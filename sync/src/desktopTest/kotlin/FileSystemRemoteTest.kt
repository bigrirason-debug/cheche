package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.engine.InMemorySyncMetaStore
import io.github.bigrirason_debug.platen.sync.engine.SyncEngine
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Drive 없이 파일시스템 원격으로 두 기기 A/B 의 왕복을 통합 테스트한다. */
class FileSystemRemoteTest {
    @Test
    fun twoDevicesRoundTripThroughFileSystem() = runTest {
        val dir = Files.createTempDirectory("platen-fsremote").toFile()
        try {
            val remoteA = FileSystemRemote(dir)
            val remoteB = FileSystemRemote(dir)
            val clock = SyncClock { 1_767_225_600_000L }
            val localA = FakeLocal(); val localB = FakeLocal()
            val engineA = SyncEngine(remoteA, localA, InMemorySyncMetaStore(), SyncConfig(deviceId = "A"), clock)
            val engineB = SyncEngine(remoteB, localB, InMemorySyncMetaStore(), SyncConfig(deviceId = "B"), clock)

            // A 가 노트를 만들어 올림 → B 첫 pull 에서 받음
            localA.add("a1", "공유노트", "hello".encodeToByteArray())
            engineA.pushAll(); engineA.pull()
            val pullB = engineB.pull()
            assertEquals(1, pullB.created)
            val bState = localB.states.values.single()
            assertContentEquals("hello".encodeToByteArray(), localB.bytes[bState.noteId])

            // B 가 수정해 올림 → A 증분 pull 로 교체
            localB.edit(bState.noteId, "hello from B".encodeToByteArray())
            engineB.pushAll()
            val pullA = engineA.pull()
            assertEquals(1, pullA.downloaded)
            assertContentEquals("hello from B".encodeToByteArray(), localA.bytes["a1"])

            // 양쪽 동시 편집 → B push 가 충돌 사본을 만들고 덮어씀, A 는 pull 로 사본 + 새 원본을 받음
            localA.edit("a1", "A edit".encodeToByteArray()); engineA.pushAll()
            localB.edit(bState.noteId, "B edit".encodeToByteArray())
            val pushB = engineB.pushAll()
            assertEquals(1, pushB.conflicts)
            val pullA2 = engineA.pull()
            assertEquals(1, pullA2.downloaded)
            assertEquals(1, pullA2.created, "B 가 만든 충돌 사본이 A 에도 생김")
            assertContentEquals("B edit".encodeToByteArray(), localA.bytes["a1"])
            assertTrue(localA.states.values.any { it.name.contains("충돌 사본 B") })

            // B 에서 삭제 → A 휴지통
            remoteB.externalDelete(bState.fileId!!)
            val pullA3 = engineA.pull()
            assertEquals(1, pullA3.trashed)
            assertEquals(listOf("a1"), localA.trashed)
        } finally {
            dir.deleteRecursively()
        }
    }
}

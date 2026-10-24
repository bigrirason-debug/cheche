package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.engine.InMemoryQueueStore
import io.github.bigrirason_debug.platen.sync.engine.InMemorySyncMetaStore
import io.github.bigrirason_debug.platen.sync.engine.SerialSyncQueue
import io.github.bigrirason_debug.platen.sync.engine.SyncEngine
import io.github.bigrirason_debug.platen.sync.engine.SyncJob
import io.github.bigrirason_debug.platen.sync.engine.SyncMeta
import io.github.bigrirason_debug.platen.sync.engine.SyncState
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SyncEngineTest {
    private val config = SyncConfig(deviceId = "dev-A")
    private val fixedNow = 1_767_225_600_000L // 2026-01-01T00:00:00Z
    private val clock = SyncClock { fixedNow }

    private fun engine(remote: FakeRemote, local: FakeLocal, meta: InMemorySyncMetaStore = InMemorySyncMetaStore()) =
        SyncEngine(remote, local, meta, config, clock)

    /** (a) 새 노트 push → create. */
    @Test
    fun newNoteIsCreated() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        local.add("n1", "메모", "v1".encodeToByteArray())
        val report = engine(remote, local).pushAll()
        assertEquals(1, report.uploaded)
        assertEquals(1, remote.creates)
        assertEquals(0, remote.updates)
        val st = local.states.getValue("n1")
        assertEquals("f1", st.fileId)
        assertEquals(md5Hex("v1".encodeToByteArray()), st.remoteMd5)
        assertEquals(st.localGen, st.syncedGen)
        assertFalse(st.dirty)
        assertEquals("메모.platen", remote.files.getValue("f1").name)
        assertEquals("dev-A", remote.files.getValue("f1").appProperties!!["deviceId"])
    }

    /** (b) 변경 없음 → 네트워크 호출 없이 skip; 내용이 같은 dirty 도 업로드 생략. */
    @Test
    fun unchangedNoteIsSkipped() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        local.add("n1", "메모", "v1".encodeToByteArray())
        val e = engine(remote, local)
        e.pushAll()
        remote.calls.clear()
        val r2 = e.pushAll()
        assertEquals(1, r2.skipped)
        assertEquals(emptyList(), remote.calls.filterNot { it == "ensureRootFolder" })

        // 저장은 했지만 바이트가 같다(결정적 zip) → get 만 하고 업로드 없음.
        local.edit("n1", "v1".encodeToByteArray())
        val r3 = e.pushAll()
        assertEquals(1, r3.skipped)
        assertEquals(0, remote.updates)
        assertFalse(local.states.getValue("n1").dirty)
    }

    /** (c) 원격 변경 + 로컬 clean → pull 교체. */
    @Test
    fun remoteChangeReplacesCleanLocal() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        val meta = InMemorySyncMetaStore()
        local.add("n1", "메모", "v1".encodeToByteArray())
        val e = engine(remote, local, meta)
        e.pushAll(); e.pull() // 첫 pull: 전체 목록 + 토큰 저장
        assertNotNull(meta.meta.startPageToken)
        remote.externalEdit("f1", "v2-remote".encodeToByteArray())
        val r = e.pull()
        assertEquals(1, r.downloaded)
        assertEquals(0, r.conflicts)
        assertContentEquals("v2-remote".encodeToByteArray(), local.bytes["n1"])
        val st = local.states.getValue("n1")
        assertEquals(md5Hex("v2-remote".encodeToByteArray()), st.remoteMd5)
        assertFalse(st.dirty)
        assertTrue(local.conflictCopies.isEmpty())
    }

    /** (d) 원격 변경 + 로컬 dirty → 충돌 사본 2곳(로컬·Drive) + 내 로컬본으로 덮어쓰기. */
    @Test
    fun conflictMakesCopiesAndLastWriteWins() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        local.add("n1", "메모", "v1".encodeToByteArray())
        val e = engine(remote, local)
        e.pushAll()
        remote.externalEdit("f1", "theirs".encodeToByteArray())
        local.edit("n1", "mine".encodeToByteArray())
        val r = e.pushAll()
        assertEquals(1, r.conflicts)
        // 로컬 충돌 사본
        assertEquals(1, local.conflictCopies.size)
        val (srcId, label) = local.conflictCopies[0]
        assertEquals("n1", srcId)
        assertEquals("메모 (충돌 사본 dev-A 20260101-000000)", label)
        val copyState = local.states.values.first { it.name == label }
        assertContentEquals("theirs".encodeToByteArray(), local.bytes[copyState.noteId])
        assertFalse(copyState.dirty, "충돌 사본은 Drive 에 올라간 상태로 기록")
        // Drive 충돌 사본 + 원본은 내 로컬본
        assertEquals(2, remote.files.size)
        val driveCopy = remote.files.values.first { it.name == "$label.platen" }
        assertContentEquals("theirs".encodeToByteArray(), remote.contents[driveCopy.id])
        assertEquals(copyState.fileId, driveCopy.id)
        assertContentEquals("mine".encodeToByteArray(), remote.contents["f1"])
        assertFalse(local.states.getValue("n1").dirty)
        assertEquals(1, e.status.value.conflictCount)
        assertEquals(SyncState.IDLE, e.status.value.state)
        assertEquals(fixedNow, e.status.value.lastSyncMillis)
    }

    /** (e) removed → 로컬 휴지통. 그리고 원격 신규 파일 → 로컬 생성. */
    @Test
    fun removedChangeTrashesLocal() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        local.add("n1", "메모", "v1".encodeToByteArray())
        val e = engine(remote, local)
        e.pushAll(); e.pull()
        remote.externalDelete("f1")
        remote.externalCreate("다른기기.platen", "other".encodeToByteArray())
        val r = e.pull()
        assertEquals(1, r.trashed)
        assertEquals(listOf("n1"), local.trashed)
        assertEquals(1, r.created)
        val created = local.states.values.single()
        assertEquals("다른기기", created.name)
        assertEquals("f2", created.fileId)
        assertFalse(created.dirty)
        assertContentEquals("other".encodeToByteArray(), local.bytes[created.noteId])
    }

    /** (f) 디바운스: 100회 연속 저장 → 업로드 1회(가상 시간). */
    @Test
    fun debounceCoalescesHundredSavesIntoOneUpload() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        local.add("n1", "메모", "v0".encodeToByteArray())
        val e = engine(remote, local)
        val queueStore = InMemoryQueueStore()
        val queue = SerialSyncQueue(e, queueStore, config, backgroundScope)
        queue.start()
        repeat(100) { i ->
            local.edit("n1", "v$i".encodeToByteArray())
            queue.noteChanged("n1")
            advanceTimeBy(100)
        }
        assertEquals(0, remote.creates, "디바운스 창 안에서는 업로드 없음")
        // advanceUntilIdle 은 backgroundScope 작업만 남으면 멈추므로 시간을 직접 전진시킨다.
        advanceTimeBy(config.debounceMillis + 1); runCurrent()
        assertEquals(1, remote.creates)
        assertEquals(0, remote.updates)
        assertContentEquals("v99".encodeToByteArray(), remote.contents["f1"])
        assertEquals(0, queue.pendingCount())
        assertTrue(queueStore.jobs.isEmpty(), "완료된 작업은 영속 큐에서 제거")
    }

    /** 영속화된 큐는 재시작 시 재개되고, 실패한 작업은 남는다. */
    @Test
    fun persistedJobsResumeAndFailuresStay() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        local.add("n1", "메모", "v0".encodeToByteArray())
        val e = engine(remote, local)
        val store = InMemoryQueueStore(listOf(SyncJob.Push(setOf("n1"))))
        val queue = SerialSyncQueue(e, store, config, backgroundScope)
        queue.start()
        runCurrent()
        assertEquals(1, remote.creates)

        // 실패하는 pull(오프라인) → 작업이 큐에 남고 상태 OFFLINE
        val failing = object : io.github.bigrirason_debug.platen.sync.drive.RemoteStore by remote {
            override suspend fun listChanges(pageToken: String) = throw IllegalStateException("no network")
        }
        val meta = InMemorySyncMetaStore(SyncMeta(startPageToken = "0", folderId = "folder1"))
        val e2 = SyncEngine(failing, local, meta, config, clock)
        val store2 = InMemoryQueueStore()
        val q2 = SerialSyncQueue(e2, store2, config, backgroundScope)
        q2.start()
        q2.enqueue(SyncJob.Pull)
        runCurrent()
        assertEquals(SyncState.OFFLINE, e2.status.value.state)
        assertEquals(1, q2.pendingCount())
        assertEquals(listOf<SyncJob>(SyncJob.Pull), store2.jobs)
    }

    /** 폴링은 pollIntervalMillis 마다 Pull 을 넣는다. */
    @Test
    fun pollingEnqueuesPull() = runTest {
        val remote = FakeRemote(); val local = FakeLocal()
        val e = engine(remote, local)
        val queue = SerialSyncQueue(e, InMemoryQueueStore(), config, backgroundScope)
        queue.start(); queue.startPolling()
        advanceTimeBy(config.pollIntervalMillis * 3 + 1); runCurrent()
        assertEquals(3, remote.calls.count { it.startsWith("changes") || it == "list" })
        queue.stop()
    }
}

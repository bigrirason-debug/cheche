package io.github.bigrirason_debug.platen.ui

import io.github.bigrirason_debug.platen.model.PlatenContainer
import io.github.bigrirason_debug.platen.ui.repo.FileNoteRepository
import io.github.bigrirason_debug.platen.ui.repo.newBlankNote
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NoteRepositoryTest {
    private fun tempDir(): File = Files.createTempDirectory("platen-ui-test").toFile()

    /** 세대 가드: 첫 저장이 느린 동안 같은 노트의 저장이 2건 더 들어오면 중간 세대는 버리고 최신만 쓴다. */
    @Test
    fun staleGenerationsAreDiscarded() = runBlocking {
        val dir = tempDir()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val gate = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        var firstWriteSeen = false
        val repo = FileNoteRepository(dir, scope, beforeWrite = { _ -> if (!firstWriteSeen) { firstWriteSeen = true; entered.complete(Unit); gate.await() } })
        val note = newBlankNote("세대")
        val d1 = repo.saveAndAwait(note.copy(title = "gen1"), 1)
        entered.await() // 워커가 gen1 쓰기에 들어간 뒤에 후속 저장을 넣는다
        repo.save(note.copy(title = "gen2"), 2)
        val d3 = repo.saveAndAwait(note.copy(title = "gen3"), 3)
        gate.complete(Unit)
        d1.await(); d3.await(); repo.flush()
        assertEquals(2, repo.writeCount, "gen1(진행 중) + gen3 만 써야 한다")
        assertEquals(1, repo.skippedCount, "gen2 는 구세대로 폐기")
        val onDisk = File(dir, "${note.id}.platen").inputStream().use { PlatenContainer.read(it) }.bundle.note
        assertEquals("gen3", onDisk.title)
        scope.cancel()
    }

    @Test
    fun outOfOrderOlderGenerationNeverOverwritesNewer() = runBlocking {
        val dir = tempDir()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = FileNoteRepository(dir, scope)
        val note = newBlankNote("순서")
        repo.saveAndAwait(note.copy(title = "new"), 5).await()
        repo.saveAndAwait(note.copy(title = "old"), 3).await() // 늦게 도착한 구세대
        repo.flush()
        val onDisk = repo.load(note.id)
        assertNotNull(onDisk)
        assertEquals("new", onDisk.first.title)
        assertEquals(1, repo.writeCount); assertEquals(1, repo.skippedCount)
        scope.cancel()
    }

    @Test
    fun createLoadDeleteRoundTrip() = runBlocking {
        val dir = tempDir()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = FileNoteRepository(dir, scope)
        val n = repo.create("라운드트립")
        repo.refresh()
        assertEquals(listOf(n.id), repo.notes.value.map { it.id })
        val loaded = repo.load(n.id)!!.first
        assertEquals(n, loaded)
        val bytes = repo.exportBytes(n.id)!!
        assertTrue(bytes.isNotEmpty())
        repo.delete(n.id)
        assertTrue(repo.notes.value.isEmpty())
        val imported = repo.importBytes(bytes)
        assertEquals(n.id, imported?.id)
        scope.cancel()
    }
}

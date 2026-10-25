package io.github.bigrirason_debug.platen.ui

import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PlatenContainer
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.ui.repo.BuiltinBrushes
import io.github.bigrirason_debug.platen.ui.repo.InMemorySettingsStore
import io.github.bigrirason_debug.platen.ui.repo.ModuleBrushRepository
import io.github.bigrirason_debug.platen.ui.repo.ModulePdfFacade
import io.github.bigrirason_debug.platen.ui.repo.NoteAttachments
import io.github.bigrirason_debug.platen.ui.repo.RepoBrushImageStore
import io.github.bigrirason_debug.platen.ui.sync.DirectoryLocalNoteStore
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** head 통합: brush·pdf·sync 모듈이 UI 파사드에 실제로 연결됐는지. */
class IntegrationWiringTest {
    private fun tmp(): File = Files.createTempDirectory("platen-int").toFile()

    private fun note(brushId: String) = Note("n1", "통합", 1, 2, listOf(Page("p1", strokes = listOf(
        Stroke("s1", brushId, 0xFF1A1A1A.toInt(), 3f, samples = (0..20).map { InputSample(40f + it * 8f, 80f + (it % 4) * 3f, 0.3f + it * 0.02f, t = it * 8f) }),
    ))))

    @Test
    fun builtinRosterAndStampTipsComeFromBrushModule() {
        val repo = ModuleBrushRepository(tmp())
        assertEquals(9, repo.brushes.value.size)
        assertNotNull(repo.tipPng(BuiltinBrushes.pencil1.id), "연필은 stamp 팁 PNG 가 있어야 한다")
        assertNull(repo.tipPng(BuiltinBrushes.pen1.id))
        val tip = RepoBrushImageStore(repo).tip(repo.get(BuiltinBrushes.pencil1.id)!!)
        assertNotNull(tip); assertTrue(tip.width > 0)
    }

    @Test
    fun embeddedBrushRoundTripsThroughNoteAttachments() {
        val a = ModuleBrushRepository(tmp())
        val zip = a.zipBytes(BuiltinBrushes.pencil2.id)!!
        val b = ModuleBrushRepository(tmp())
        val custom = mapOf("custom-x" to zip)
        b.registerEmbedded(custom)
        assertEquals("custom-x", b.get("custom-x")?.id)
        assertNotNull(b.tipPng("custom-x"))
    }

    @Test
    fun pdfExportProducesPdfWithStrokes() = runBlocking {
        val repo = ModuleBrushRepository(tmp())
        val pdf = ModulePdfFacade(repo, InMemorySettingsStore())
        val bytes = pdf.export(note(BuiltinBrushes.pen1.id), NoteAttachments())
        assertNotNull(bytes)
        assertEquals("%PDF", String(bytes, 0, 4))
        // 다시 가져오면 1페이지
        val (pages, att) = pdf.importPdf(bytes)!!
        assertEquals(1, pages.size); assertEquals(1, att.assets.size)
    }

    @Test
    fun localStoreDetectsEditsByContentHash() = runBlocking {
        val dir = tmp()
        val f = File(dir, "n1.platen")
        f.writeBytes(PlatenContainer.writeToBytes(PlatenContainer.Bundle(note("pen-fine"))))
        val store = DirectoryLocalNoteStore(dir)
        val s0 = store.listNotes().single()
        assertTrue(s0.dirty, "처음 보는 노트는 업로드 대상")
        store.readBytes("n1")
        store.updateState(s0.copy(fileId = "F1", syncedGen = s0.localGen))
        assertFalse(store.listNotes().single().dirty, "동기화 직후는 깨끗")
        f.writeBytes(PlatenContainer.writeToBytes(PlatenContainer.Bundle(note("pen-fine").copy(title = "편집"))))
        assertTrue(store.listNotes().single().dirty, "내용이 바뀌면 dirty")
        // 원격본 반영(writeBytes)은 로컬 편집이 아니다
        val s1 = store.listNotes().single()
        store.writeBytes("n1", PlatenContainer.writeToBytes(PlatenContainer.Bundle(note("pen-fine").copy(title = "원격"))))
        store.updateState(s1.copy(syncedGen = s1.localGen))
        assertFalse(store.listNotes().single().dirty)
        // 충돌 사본은 새 id
        val copy = store.writeConflictCopy("n1", f.readBytes(), "통합 (충돌 사본)")
        assertTrue(copy.noteId != "n1"); assertEquals(2, store.listNotes().size)
    }
}

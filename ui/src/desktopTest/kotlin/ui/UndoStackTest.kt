package io.github.bigrirason_debug.platen.ui

import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.ui.state.EditCommand
import io.github.bigrirason_debug.platen.ui.state.EditorController
import io.github.bigrirason_debug.platen.ui.state.UndoStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UndoStackTest {
    @Test
    fun pushUndoRedoSemantics() {
        val s = UndoStack<Int>()
        assertFalse(s.canUndo); assertFalse(s.canRedo)
        s.push(1); s.push(2); s.push(3)
        assertEquals(3, s.popUndo()); assertEquals(2, s.popUndo())
        assertTrue(s.canRedo)
        assertEquals(2, s.popRedo())
        s.push(9) // 새 push 는 redo 를 비운다
        assertFalse(s.canRedo)
        assertNull(s.popRedo())
        assertEquals(9, s.popUndo()); assertEquals(1, s.popUndo()); assertNull(s.popUndo())
    }

    @Test
    fun limitDropsOldest() {
        val s = UndoStack<Int>(limit = 3)
        (1..5).forEach { s.push(it) }
        assertEquals(3, s.undoDepth)
        assertEquals(5, s.popUndo()); assertEquals(4, s.popUndo()); assertEquals(3, s.popUndo()); assertNull(s.popUndo())
    }

    private fun stroke(id: String, x: Float) = Stroke(id, "b", 0xFF1A1A1A.toInt(), 2f, samples = listOf(InputSample(x, 10f, 0.5f), InputSample(x + 5f, 12f, 0.5f)))

    @Test
    fun controllerUndoRedoRestoresStrokesAndObjects() {
        val note = Note("n", "t", 0, 0, listOf(Page("p")))
        val c = EditorController(note)
        c.addStroke(stroke("s1", 0f)); c.addStroke(stroke("s2", 20f))
        val tb = PageObject.TextBox("t1", Rect(0f, 0f, 10f, 10f), "hi")
        c.addObject(tb)
        assertEquals(2, c.page.strokes.size); assertEquals(1, c.page.objects.size)
        assertTrue(c.undo()); assertEquals(0, c.page.objects.size)
        assertTrue(c.undo()); assertEquals(listOf("s1"), c.page.strokes.map { it.id })
        assertTrue(c.redo()); assertEquals(listOf("s1", "s2"), c.page.strokes.map { it.id })
        assertTrue(c.redo()); assertEquals(1, c.page.objects.size)
        assertFalse(c.redo())
        assertEquals(4L + 2L + 2L, c.dirtyGeneration) // 3 execute + 2 undo + 2 redo = 7 → 실제 값 검증
    }

    @Test
    fun batchIsOneUndoUnit() {
        val note = Note("n", "t", 0, 0, listOf(Page("p", strokes = listOf(stroke("a", 0f), stroke("b", 50f)))))
        val c = EditorController(note)
        c.execute(EditCommand.Batch(listOf(EditCommand.RemoveStrokes("p", listOf(c.page.strokes[0])), EditCommand.RemoveStrokes("p", listOf(c.page.strokes[1])))))
        assertEquals(0, c.page.strokes.size)
        c.undo()
        assertEquals(listOf("a", "b"), c.page.strokes.map { it.id })
    }

    @Test
    fun pageInsertRemoveUndo() {
        val c = EditorController(Note("n", "t", 0, 0, listOf(Page("p1"))))
        c.addPageAfterCurrent()
        assertEquals(2, c.pageCount); assertEquals(1, c.pageIndex)
        assertTrue(c.removeCurrentPage())
        assertEquals(1, c.pageCount); assertEquals(0, c.pageIndex)
        assertFalse(c.removeCurrentPage()) // 마지막 페이지는 삭제 불가
        c.undo(); assertEquals(2, c.pageCount)
    }
}

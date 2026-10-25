package io.github.bigrirason_debug.platen.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.model.newId
import io.github.bigrirason_debug.platen.model.nowMillis

/**
 * 노트 1개의 편집 상태: 문서 + undo/redo + 현재 페이지 + 올가미 선택. Compose 상태로 노출.
 * 모든 변경은 EditCommand 를 통해 들어오고 `dirtyGeneration` 이 올라간다(NoteRepository 세대 가드의 입력).
 */
class EditorController(initial: Note, private val history: UndoStack<EditCommand> = UndoStack()) {
    var note: Note by mutableStateOf(initial)
        private set
    var pageIndex: Int by mutableIntStateOf(0)
        private set
    var selection: LassoSelection? by mutableStateOf(null)
        private set
    /** 저장 요청마다 메인에서 스냅샷하는 세대 번호. */
    var dirtyGeneration: Long by mutableStateOf(0L)
        private set
    var canUndo: Boolean by mutableStateOf(false)
        private set
    var canRedo: Boolean by mutableStateOf(false)
        private set

    val page: Page get() = note.pages[pageIndex.coerceIn(0, note.pages.lastIndex)]
    val pageCount: Int get() = note.pages.size

    fun execute(cmd: EditCommand) {
        val prepared = cmd.prepare(note)
        note = prepared.apply(note).copy(modified = nowMillis())
        history.push(prepared)
        afterChange()
    }

    fun undo(): Boolean {
        val cmd = history.popUndo() ?: return false
        note = cmd.revert(note).copy(modified = nowMillis())
        selection = null
        afterChange(); return true
    }

    fun redo(): Boolean {
        val cmd = history.popRedo() ?: return false
        note = cmd.apply(note).copy(modified = nowMillis())
        selection = null
        afterChange(); return true
    }

    private fun afterChange() {
        canUndo = history.canUndo; canRedo = history.canRedo
        dirtyGeneration++
        if (pageIndex > note.pages.lastIndex) pageIndex = note.pages.lastIndex.coerceAtLeast(0)
        selection?.let { s -> if (note.pages.none { it.id == s.pageId }) selection = null }
    }

    // ------------------------------------------------------------ 페이지

    fun goToPage(index: Int) { pageIndex = index.coerceIn(0, note.pages.lastIndex); selection = null }
    fun nextPage() = goToPage(pageIndex + 1)
    fun previousPage() = goToPage(pageIndex - 1)

    /** 현재 페이지 뒤에 같은 크기·속지의 빈 페이지 추가. */
    fun addPageAfterCurrent(): Page {
        val cur = page
        val p = Page(id = newId(), size = cur.size, background = cur.background)
        execute(EditCommand.InsertPage(pageIndex + 1, p))
        goToPage(pageIndex + 1)
        return p
    }

    fun removeCurrentPage(): Boolean {
        if (note.pages.size <= 1) return false
        execute(EditCommand.RemovePage(pageIndex, page))
        return true
    }

    fun rename(title: String) { if (title != note.title) execute(EditCommand.Rename(note.title, title)) }

    // ------------------------------------------------------------ 획

    fun addStroke(stroke: Stroke, pageId: String = page.id) = execute(EditCommand.AddStrokes(pageId, listOf(stroke)))
    fun addStrokes(strokes: List<Stroke>, pageId: String = page.id) { if (strokes.isNotEmpty()) execute(EditCommand.AddStrokes(pageId, strokes)) }
    fun removeStrokes(strokes: List<Stroke>, pageId: String = page.id) { if (strokes.isNotEmpty()) execute(EditCommand.RemoveStrokes(pageId, strokes)) }

    // ------------------------------------------------------------ 객체

    fun addObject(obj: PageObject, pageId: String = page.id) = execute(EditCommand.AddObject(pageId, obj))
    fun updateObject(before: PageObject, after: PageObject, pageId: String = page.id) { if (before != after) execute(EditCommand.UpdateObject(pageId, before, after)) }
    fun removeObject(obj: PageObject, pageId: String = page.id) = execute(EditCommand.RemoveObjects(pageId, listOf(obj)))

    // ------------------------------------------------------------ 속지(마진 스톱)

    fun setBackground(after: Background, pageId: String = page.id) {
        val before = note.pages.first { it.id == pageId }.background
        if (before != after) execute(EditCommand.SetBackground(pageId, before, after))
    }

    // ------------------------------------------------------------ 올가미

    fun selectLasso(polygon: List<Pt>) {
        val s = LassoOps.select(page, polygon)
        selection = if (s.isEmpty) null else s
    }

    fun clearSelection() { selection = null }

    fun moveSelection(dx: Float, dy: Float) {
        val s = selection ?: return
        if (dx == 0f && dy == 0f) return
        execute(EditCommand.Translate(s.pageId, s.strokeIds, s.objectIds, dx, dy))
        val p = note.pages.first { it.id == s.pageId }
        selection = s.copy(bounds = LassoOps.bounds(p, s))
    }

    fun deleteSelection() {
        val s = selection ?: return
        val p = note.pages.first { it.id == s.pageId }
        val cmds = buildList {
            val strokes = p.strokes.filter { it.id in s.strokeIds }
            if (strokes.isNotEmpty()) add(EditCommand.RemoveStrokes(p.id, strokes))
            val objs = p.objects.filter { it.id in s.objectIds }
            if (objs.isNotEmpty()) add(EditCommand.RemoveObjects(p.id, objs))
        }
        selection = null
        if (cmds.isNotEmpty()) execute(EditCommand.Batch(cmds))
    }

    fun duplicateSelection(offset: Float = 24f) {
        val s = selection ?: return
        val p = note.pages.first { it.id == s.pageId }
        val (strokes, objects) = LassoOps.duplicate(p, s, offset, offset) { newId() }
        val cmds = buildList {
            if (strokes.isNotEmpty()) add(EditCommand.AddStrokes(p.id, strokes))
            objects.forEach { add(EditCommand.AddObject(p.id, it)) }
        }
        if (cmds.isEmpty()) return
        execute(EditCommand.Batch(cmds))
        selection = LassoSelection(p.id, strokes.map { it.id }.toSet(), objects.map { it.id }.toSet(), Rect.EMPTY).let { ns ->
            ns.copy(bounds = LassoOps.bounds(note.pages.first { it.id == p.id }, ns))
        }
    }
}

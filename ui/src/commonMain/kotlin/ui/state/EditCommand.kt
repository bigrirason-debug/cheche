package io.github.bigrirason_debug.platen.ui.state

import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Stroke

/** 노트에 대한 가역 편집 1건(획·객체 단위). apply/revert 는 순수 함수. */
sealed interface EditCommand {
    fun apply(note: Note): Note
    fun revert(note: Note): Note

    data class AddStrokes(val pageId: String, val strokes: List<Stroke>) : EditCommand {
        override fun apply(note: Note) = note.mapPage(pageId) { it.copy(strokes = it.strokes + strokes) }
        override fun revert(note: Note): Note {
            val ids = strokes.map { it.id }.toSet()
            return note.mapPage(pageId) { p -> p.copy(strokes = p.strokes.filterNot { it.id in ids }) }
        }
    }

    data class RemoveStrokes(val pageId: String, val strokes: List<Stroke>) : EditCommand {
        override fun apply(note: Note): Note {
            val ids = strokes.map { it.id }.toSet()
            return note.mapPage(pageId) { p -> p.copy(strokes = p.strokes.filterNot { it.id in ids }) }
        }
        override fun revert(note: Note) = note.mapPage(pageId) { it.copy(strokes = it.strokes + strokes) }
    }

    /** 획·객체 평행이동(올가미 이동). */
    data class Translate(val pageId: String, val strokeIds: Set<String>, val objectIds: Set<String>, val dx: Float, val dy: Float) : EditCommand {
        override fun apply(note: Note) = note.mapPage(pageId) { LassoOps.translate(it, strokeIds, objectIds, dx, dy) }
        override fun revert(note: Note) = note.mapPage(pageId) { LassoOps.translate(it, strokeIds, objectIds, -dx, -dy) }
    }

    data class AddObject(val pageId: String, val obj: PageObject) : EditCommand {
        override fun apply(note: Note) = note.mapPage(pageId) { it.copy(objects = it.objects + obj) }
        override fun revert(note: Note) = note.mapPage(pageId) { p -> p.copy(objects = p.objects.filterNot { it.id == obj.id }) }
    }

    data class RemoveObjects(val pageId: String, val objects: List<PageObject>) : EditCommand {
        override fun apply(note: Note): Note {
            val ids = objects.map { it.id }.toSet()
            return note.mapPage(pageId) { p -> p.copy(objects = p.objects.filterNot { it.id in ids }) }
        }
        override fun revert(note: Note) = note.mapPage(pageId) { it.copy(objects = it.objects + objects) }
    }

    data class UpdateObject(val pageId: String, val before: PageObject, val after: PageObject) : EditCommand {
        override fun apply(note: Note) = note.mapPage(pageId) { p -> p.copy(objects = p.objects.map { if (it.id == before.id) after else it }) }
        override fun revert(note: Note) = note.mapPage(pageId) { p -> p.copy(objects = p.objects.map { if (it.id == after.id) before else it }) }
    }

    data class SetBackground(val pageId: String, val before: Background, val after: Background) : EditCommand {
        override fun apply(note: Note) = note.mapPage(pageId) { it.copy(background = after) }
        override fun revert(note: Note) = note.mapPage(pageId) { it.copy(background = before) }
    }

    data class InsertPage(val index: Int, val page: Page) : EditCommand {
        override fun apply(note: Note) = note.copy(pages = note.pages.toMutableList().also { it.add(index.coerceIn(0, it.size), page) })
        override fun revert(note: Note) = note.copy(pages = note.pages.filterNot { it.id == page.id })
    }

    data class RemovePage(val index: Int, val page: Page) : EditCommand {
        override fun apply(note: Note) = note.copy(pages = note.pages.filterNot { it.id == page.id })
        override fun revert(note: Note) = note.copy(pages = note.pages.toMutableList().also { it.add(index.coerceIn(0, it.size), page) })
    }

    data class Rename(val before: String, val after: String) : EditCommand {
        override fun apply(note: Note) = note.copy(title = after)
        override fun revert(note: Note) = note.copy(title = before)
    }

    /** 여러 명령을 하나의 undo 단위로(예: 올가미 삭제 = 획 + 객체). */
    data class Batch(val commands: List<EditCommand>) : EditCommand {
        override fun apply(note: Note) = commands.fold(note) { n, c -> c.apply(n) }
        override fun revert(note: Note) = commands.asReversed().fold(note) { n, c -> c.revert(n) }
    }
}

fun Note.mapPage(pageId: String, f: (Page) -> Page): Note = copy(pages = pages.map { if (it.id == pageId) f(it) else it })

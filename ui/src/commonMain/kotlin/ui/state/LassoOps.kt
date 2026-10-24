package io.github.bigrirason_debug.platen.ui.state

import io.github.bigrirason_debug.platen.ink.StrokeHitTest
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke

/** 올가미 선택 결과(id 집합 + 경계). */
data class LassoSelection(val pageId: String, val strokeIds: Set<String>, val objectIds: Set<String>, val bounds: Rect) {
    val isEmpty: Boolean get() = strokeIds.isEmpty() && objectIds.isEmpty()
}

/** 올가미 선택·이동·복제 순수 함수. 선택 판정은 core-ink `StrokeHitTest.insideLasso`(샘플 과반) + 객체는 중심점 포함. */
object LassoOps {
    fun select(page: Page, lasso: List<Pt>): LassoSelection {
        if (lasso.size < 3) return LassoSelection(page.id, emptySet(), emptySet(), Rect.EMPTY)
        val strokes = page.strokes.filter { StrokeHitTest.insideLasso(it, lasso) }
        val objects = page.objects.filter { pointInPolygon(center(it.rect), lasso) }
        return LassoSelection(page.id, strokes.map { it.id }.toSet(), objects.map { it.id }.toSet(), bounds(page, strokes, objects))
    }

    fun bounds(page: Page, selection: LassoSelection): Rect =
        bounds(page, page.strokes.filter { it.id in selection.strokeIds }, page.objects.filter { it.id in selection.objectIds })

    private fun bounds(page: Page, strokes: List<Stroke>, objects: List<PageObject>): Rect {
        val rects = strokes.map { it.bounds(it.size / 2f) } + objects.map { it.rect }
        if (rects.isEmpty()) return Rect.EMPTY
        return rects.reduce { a, b -> a.union(b) }
    }

    fun translate(page: Page, strokeIds: Set<String>, objectIds: Set<String>, dx: Float, dy: Float): Page = page.copy(
        strokes = page.strokes.map { s -> if (s.id in strokeIds) translateStroke(s, dx, dy) else s },
        objects = page.objects.map { o -> if (o.id in objectIds) translateObject(o, dx, dy) else o },
    )

    fun translateStroke(s: Stroke, dx: Float, dy: Float): Stroke =
        s.copy(samples = s.samples.map { it.copy(x = it.x + dx, y = it.y + dy) })

    fun translateObject(o: PageObject, dx: Float, dy: Float): PageObject {
        val r = Rect(o.rect.left + dx, o.rect.top + dy, o.rect.right + dx, o.rect.bottom + dy)
        return when (o) {
            is PageObject.TextBox -> o.copy(rect = r)
            is PageObject.Image -> o.copy(rect = r)
            is PageObject.Table -> o.copy(rect = r)
            is PageObject.Link -> o.copy(rect = r)
        }
    }

    /** 복제: 새 id 를 부여하고 (dx,dy) 만큼 옮긴 사본. newId 는 호출자가 주입(결정적 테스트). */
    fun duplicate(page: Page, selection: LassoSelection, dx: Float, dy: Float, newId: () -> String): Pair<List<Stroke>, List<PageObject>> {
        val strokes = page.strokes.filter { it.id in selection.strokeIds }.map { translateStroke(it, dx, dy).copy(id = newId()) }
        val objects = page.objects.filter { it.id in selection.objectIds }.map { o ->
            val moved = translateObject(o, dx, dy)
            when (moved) {
                is PageObject.TextBox -> moved.copy(id = newId())
                is PageObject.Image -> moved.copy(id = newId())
                is PageObject.Table -> moved.copy(id = newId())
                is PageObject.Link -> moved.copy(id = newId())
            }
        }
        return strokes to objects
    }

    fun center(r: Rect) = Pt((r.left + r.right) / 2f, (r.top + r.bottom) / 2f)

    /** 짝수-홀수 규칙 점-폴리곤 판정. */
    fun pointInPolygon(p: Pt, poly: List<Pt>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]; val b = poly[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val x = (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
                if (p.x < x) inside = !inside
            }
            j = i
        }
        return inside
    }
}

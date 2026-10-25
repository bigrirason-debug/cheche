package io.github.bigrirason_debug.platen.ui

import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.ui.screens.PressureCurveOps
import io.github.bigrirason_debug.platen.model.CurvePoint
import io.github.bigrirason_debug.platen.ui.state.EditorController
import io.github.bigrirason_debug.platen.ui.state.LassoOps
import io.github.bigrirason_debug.platen.ui.state.MarginSide
import io.github.bigrirason_debug.platen.ui.state.MarginStops
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LassoAndMarginTest {
    private fun stroke(id: String, cx: Float, cy: Float) = Stroke(
        id, "b", 0xFF1A1A1A.toInt(), 2f,
        samples = (0 until 10).map { InputSample(cx + it, cy + (it % 3), 0.5f, t = it * 8f) },
    )
    private val square = listOf(Pt(0f, 0f), Pt(100f, 0f), Pt(100f, 100f), Pt(0f, 100f))

    @Test
    fun pointInPolygon() {
        assertTrue(LassoOps.pointInPolygon(Pt(50f, 50f), square))
        assertFalse(LassoOps.pointInPolygon(Pt(150f, 50f), square))
        val tri = listOf(Pt(0f, 0f), Pt(100f, 0f), Pt(0f, 100f))
        assertTrue(LassoOps.pointInPolygon(Pt(20f, 20f), tri)); assertFalse(LassoOps.pointInPolygon(Pt(80f, 80f), tri))
    }

    @Test
    fun selectInsideAndTranslate() {
        val inside = stroke("in", 40f, 40f); val outside = stroke("out", 300f, 300f)
        val tb = PageObject.TextBox("t", Rect(10f, 10f, 30f, 30f), "x")
        val far = PageObject.TextBox("f", Rect(200f, 200f, 230f, 230f), "y")
        val page = Page("p", strokes = listOf(inside, outside), objects = listOf(tb, far))
        val sel = LassoOps.select(page, square)
        assertEquals(setOf("in"), sel.strokeIds); assertEquals(setOf("t"), sel.objectIds)
        assertTrue(sel.bounds.left <= 10f && sel.bounds.right >= 49f)

        val moved = LassoOps.translate(page, sel.strokeIds, sel.objectIds, 15f, -5f)
        assertEquals(55f, moved.strokes[0].samples[0].x); assertEquals(35f, moved.strokes[0].samples[0].y)
        assertEquals(300f, moved.strokes[1].samples[0].x) // 비선택은 그대로
        assertEquals(Rect(25f, 5f, 45f, 25f), moved.objects[0].rect)
        assertEquals(far.rect, moved.objects[1].rect)
        // 역이동은 원복
        val back = LassoOps.translate(moved, sel.strokeIds, sel.objectIds, -15f, 5f)
        assertEquals(page, back)
    }

    @Test
    fun controllerMoveDuplicateDeleteSelectionWithUndo() {
        val page = Page("p", strokes = listOf(stroke("a", 40f, 40f)), objects = listOf(PageObject.TextBox("t", Rect(10f, 10f, 30f, 30f), "x")))
        val c = EditorController(Note("n", "t", 0, 0, listOf(page)))
        c.selectLasso(square)
        assertNotNull(c.selection); assertEquals(setOf("a"), c.selection!!.strokeIds)
        c.moveSelection(10f, 10f)
        assertEquals(50f, c.page.strokes[0].samples[0].x)
        c.duplicateSelection(offset = 100f)
        assertEquals(2, c.page.strokes.size); assertEquals(2, c.page.objects.size)
        assertEquals(setOf(c.page.strokes[1].id), c.selection!!.strokeIds)
        c.deleteSelection()
        assertEquals(1, c.page.strokes.size); assertEquals(1, c.page.objects.size); assertNull(c.selection)
        c.undo(); assertEquals(2, c.page.strokes.size)
        c.undo(); assertEquals(1, c.page.strokes.size)
        c.undo(); assertEquals(40f, c.page.strokes[0].samples[0].x)
        assertFalse(c.canUndo)
    }

    @Test
    fun marginStopDragUpdatesTemplateWithSnapAndGap() {
        val t = Background.Template("lines")
        val w = 455f
        val left = MarginStops.drag(t, MarginSide.left, 70f, w)
        assertEquals(72f, left.marginLeft) // 12pt 스냅
        val right = MarginStops.drag(left, MarginSide.right, 400f, w)
        assertEquals(w - 396f, right.marginRight)
        // 최소 간격: 오른쪽 마커를 왼쪽 마커 바로 옆으로 끌면 MIN_GAP 유지
        val squeezed = MarginStops.drag(right, MarginSide.right, 80f, w)
        assertEquals(w - (72f + MarginStops.MIN_GAP_PT), squeezed.marginRight)
        // 범위 밖은 클램프
        val neg = MarginStops.drag(t, MarginSide.left, -50f, w)
        assertEquals(0f, neg.marginLeft)
        // 컨트롤러 경유: 배경 변경이 undo 단위
        val c = EditorController(Note("n", "t", 0, 0, listOf(Page("p", background = t))))
        c.setBackground(MarginStops.drag(t, MarginSide.left, 96f, w))
        assertEquals(96f, (c.page.background as Background.Template).marginLeft)
        c.undo(); assertEquals(48f, (c.page.background as Background.Template).marginLeft)
        assertNull(MarginStops.templateOf(Background.PdfPage("a", 0)))
        assertEquals(1, MarginStops.spacingIndex(MarginStops.withSpacing(t, 1)))
    }

    @Test
    fun pressureCurveStaysMonotone() {
        val base = listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.5f), CurvePoint(1f, 1f))
        val moved = PressureCurveOps.moved(base, 1, 0.9f, 1.5f)
        assertTrue(PressureCurveOps.isMonotone(moved)); assertTrue(moved[1].y <= 1f && moved[1].x < 1f)
        val ends = PressureCurveOps.moved(base, 0, 0.5f, 0.5f)
        assertEquals(CurvePoint(0f, 0f), ends[0]) // 끝점 고정
        var pts = base
        repeat(10) { pts = PressureCurveOps.added(pts, 0.1f + it * 0.08f) }
        assertEquals(PressureCurveOps.MAX_POINTS, pts.size)
        assertTrue(PressureCurveOps.isMonotone(pts))
        assertEquals(base.size, PressureCurveOps.removed(base, 0).size)
        assertEquals(2, PressureCurveOps.removed(base, 1).size)
    }
}

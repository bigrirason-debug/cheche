package io.github.bigrirason_debug.platen.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.ui.canvas.PageLayout
import io.github.bigrirason_debug.platen.ui.canvas.Viewport
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IconsAndViewportTest {
    /** 접근성: 모든 아이콘의 라벨(contentDescription)이 비어 있지 않고 유일하다 (DESIGN §6). */
    @Test
    fun everyIconHasNonBlankUniqueLabel() {
        assertTrue(PlatenIcons.all.size >= 30)
        for (i in PlatenIcons.all) {
            assertTrue(i.label.isNotBlank(), "icon ${i.name} has blank label")
            assertTrue(i.name.isNotBlank())
        }
        assertEquals(PlatenIcons.all.size, PlatenIcons.all.map { it.label }.toSet().size, "labels must be unique")
        assertEquals(PlatenIcons.all.size, PlatenIcons.all.map { it.name }.toSet().size)
    }

    @Test
    fun layoutStacksPagesVertically() {
        val note = Note("n", "t", 0, 0, listOf(Page("a"), Page("b")))
        val l = PageLayout.of(note)
        assertEquals(0f, l.slots[0].y)
        assertEquals(note.pages[0].size.height + PageLayout.GAP_PT, l.slots[1].y)
        assertEquals(1, l.slotAt(l.slots[1].y + 10f)?.index)
        assertNull(l.slotAt(l.slots[0].page.size.height + 2f)) // 간격 영역
    }

    @Test
    fun viewportFitWidthZoomAndHit() {
        val note = Note("n", "t", 0, 0, listOf(Page("a"), Page("b")))
        val vp = Viewport().apply { marginPx = 24f }
        vp.updateLayout(PageLayout.of(note))
        vp.updateSize(IntSize(1000, 800))
        val pw = note.pages[0].size.width
        // 폭맞춤: (1000 - 48) / 455.04
        assertEquals((1000f - 48f) / pw, vp.scale, 1e-4f)
        val hit = vp.hit(Offset(24f + 10f * vp.scale, 24f + 20f * vp.scale))
        assertNotNull(hit); assertEquals(0, hit.first.index)
        assertEquals(10f, hit.second.x, 0.01f); assertEquals(20f, hit.second.y, 0.01f)
        // 줌 클램프 0.5~4
        vp.zoomBy(10f, Offset(500f, 400f)); assertEquals(Viewport.MAX_ZOOM, vp.zoom)
        vp.zoomBy(0.01f, Offset(500f, 400f)); assertEquals(Viewport.MIN_ZOOM, vp.zoom)
        vp.resetZoom(); assertEquals(1f, vp.zoom)
        // 초점 고정: 초점 아래 문서 좌표가 줌 전후 같다
        val focal = Offset(300f, 300f)
        val before = vp.toDoc(focal)
        vp.zoomBy(1.5f, focal)
        val after = vp.toDoc(focal)
        assertEquals(before.x, after.x, 0.01f); assertEquals(before.y, after.y, 0.01f)
        // 페이지 스크롤
        vp.resetZoom(); vp.scrollToPage(1)
        assertEquals(1, vp.hit(Offset(500f, 30f))!!.first.index)
        assertEquals(1, vp.centerPageIndex())
        // 팬 클램프: 위로 너무 올리면 상단 여백에서 멈춤
        vp.pan(Offset(0f, 100000f)); assertEquals(24f, vp.offset.y)
        assertEquals(Pt(0f, 0f).x, vp.toDoc(vp.toScreen(Pt(0f, 0f))).x, 0.01f)
    }
}

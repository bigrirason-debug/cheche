package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect

/** 페이지 1장의 레이아웃 좌표(문서 공간 pt: 페이지들을 세로로 쌓은 좌표계). */
data class PageSlot(val index: Int, val page: Page, val x: Float, val y: Float) {
    val rect: Rect get() = Rect(x, y, x + page.size.width, y + page.size.height)
}

/** 노트의 페이지를 세로로 쌓는다(삼성식 페이지 카드). 간격 GAP_PT, 가로는 가장 넓은 페이지 기준 가운데 정렬. */
data class PageLayout(val slots: List<PageSlot>, val width: Float, val height: Float) {
    companion object {
        const val GAP_PT = 16f
        fun of(note: Note): PageLayout {
            val maxW = note.pages.maxOfOrNull { it.size.width } ?: 0f
            var y = 0f
            val slots = note.pages.mapIndexed { i, p ->
                PageSlot(i, p, (maxW - p.size.width) / 2f, y).also { y += p.size.height + GAP_PT }
            }
            return PageLayout(slots, maxW, (y - GAP_PT).coerceAtLeast(0f))
        }
    }
    fun slotAt(docY: Float): PageSlot? = slots.firstOrNull { docY >= it.y && docY <= it.y + it.page.size.height }
    fun slotNearest(docY: Float): PageSlot? = slots.minByOrNull { kotlin.math.abs((it.y + it.page.size.height / 2f) - docY) }
}

/**
 * 뷰포트: 문서 pt ↔ 화면 px. 기본 = 폭맞춤(zoom 1.0), 핀치/휠 줌 0.5~4x. 팬은 콘텐츠 범위 + 여백으로 클램프.
 * 순수 계산이라 테스트 가능. Compose 상태는 zoom/offset 3개뿐.
 */
class Viewport {
    var zoom: Float by mutableFloatStateOf(1f); private set
    var offset: Offset by mutableStateOf(Offset.Zero); private set
    var size: IntSize by mutableStateOf(IntSize.Zero); private set
    var layout: PageLayout by mutableStateOf(PageLayout(emptyList(), 0f, 0f)); private set
    /** 페이지 바깥 여백(px). DESIGN §3: 16dp 세로 / 24dp 가로 — 호출자가 dp→px 로 준다. */
    var marginPx: Float = 24f

    companion object { const val MIN_ZOOM = 0.5f; const val MAX_ZOOM = 4f }

    /** px per pt */
    val scale: Float get() = baseScale * zoom
    private val baseScale: Float get() = if (layout.width <= 0f || size.width <= 0) 1f else (size.width - 2 * marginPx) / layout.width

    fun updateLayout(l: PageLayout) { if (l != layout) { layout = l; clamp() } }
    fun updateSize(s: IntSize) { size = s; clamp() }

    fun toScreen(p: Pt): Offset = Offset(p.x * scale + offset.x, p.y * scale + offset.y)
    fun toDoc(o: Offset): Pt = Pt((o.x - offset.x) / scale, (o.y - offset.y) / scale)

    /** 화면 좌표 → (페이지 슬롯, 페이지 로컬 pt). 페이지 밖이면 가장 가까운 슬롯에 클램프하지 않고 null. */
    fun hit(o: Offset): Pair<PageSlot, Pt>? {
        val d = toDoc(o)
        val slot = layout.slotAt(d.y) ?: return null
        val local = Pt(d.x - slot.x, d.y - slot.y)
        if (local.x < 0f || local.x > slot.page.size.width) return null
        return slot to local
    }

    /** 페이지 밖도 허용(드래그 중 연속성): 가장 가까운 슬롯 기준 로컬 pt. */
    fun hitLoose(o: Offset, slot: PageSlot): Pt {
        val d = toDoc(o)
        return Pt(d.x - slot.x, d.y - slot.y)
    }

    fun pan(delta: Offset) { offset += delta; clamp() }

    fun zoomBy(factor: Float, focal: Offset) {
        val old = scale
        zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val new = scale
        // 초점 고정: focal = doc*old + off  →  off' = focal - (focal - off) * new/old
        offset = focal - (focal - offset) * (new / old)
        clamp()
    }

    fun resetZoom() { zoom = 1f; clamp() }

    /** 페이지 index 가 보이도록 세로 스크롤. */
    fun scrollToPage(index: Int) {
        val slot = layout.slots.getOrNull(index) ?: return
        offset = Offset(offset.x, -(slot.y * scale) + marginPx)
        clamp()
    }

    /** 뷰포트 세로 중앙에 가장 가까운 페이지 index. */
    fun centerPageIndex(): Int {
        val d = toDoc(Offset(size.width / 2f, size.height / 2f))
        return layout.slotNearest(d.y)?.index ?: 0
    }

    private fun clamp() {
        if (size.width == 0 || layout.slots.isEmpty()) return
        val cw = layout.width * scale
        val ch = layout.height * scale
        val x = if (cw + 2 * marginPx <= size.width) (size.width - cw) / 2f
        else offset.x.coerceIn(size.width - cw - marginPx, marginPx)
        val y = if (ch + 2 * marginPx <= size.height) marginPx
        else offset.y.coerceIn(size.height - ch - marginPx, marginPx)
        offset = Offset(x, y)
    }

    /** 현재 보이는 문서 영역(pt, 레이아웃 좌표). */
    fun visibleDocRect(): Rect {
        val a = toDoc(Offset.Zero); val b = toDoc(Offset(size.width.toFloat(), size.height.toFloat()))
        return Rect(a.x, a.y, b.x, b.y)
    }
}

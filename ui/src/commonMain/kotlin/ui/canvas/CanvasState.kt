package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.bigrirason_debug.platen.ink.LiveStrokeBuilder
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.ink.StrokeHitTest
import io.github.bigrirason_debug.platen.model.AppSettings
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.model.newId
import io.github.bigrirason_debug.platen.model.nowMillis
import io.github.bigrirason_debug.platen.ui.repo.BrushRepository
import io.github.bigrirason_debug.platen.ui.repo.NoteAttachments
import io.github.bigrirason_debug.platen.ui.state.EditorController
import io.github.bigrirason_debug.platen.ui.state.PenSlot
import io.github.bigrirason_debug.platen.ui.state.Tool
import io.github.bigrirason_debug.platen.ui.theme.PlatenPalette

/** 레이저 포인터의 일시 점(화면 px 가 아니라 문서 pt — 줌해도 같은 자리). */
data class LaserPoint(val slotIndex: Int, val p: Pt, val t: Long)

/**
 * 캔버스 셸 상태: 도구·슬롯·진행 중 입력 세션·올가미 드래그·레이저·텍스트 편집·속지 편집 모드.
 * 입력 세션 로직은 Compose 와 무관한 메서드라 단위 테스트 가능(화면 좌표 변환은 Viewport).
 */
class CanvasState(
    val controller: EditorController,
    val viewport: Viewport,
    private val brushes: BrushRepository,
    initialSlots: List<PenSlot>,
    initialSlot: Int,
    attachments: NoteAttachments,
) {
    var settings: AppSettings by mutableStateOf(AppSettings())
    var tool: Tool by mutableStateOf(Tool.Pen)
        private set
    private var previousTool: Tool = Tool.Pen
    var slots: List<PenSlot> by mutableStateOf(initialSlots)
    var selectedSlot: Int by mutableIntStateOf(initialSlot.coerceIn(0, (initialSlots.size - 1).coerceAtLeast(0)))
    var eraserRadiusPt: Float = 6f
    var fingerDrawOverride: Boolean? by mutableStateOf(null)

    /** 속지 편집 모드(마진 스톱 드래그 허용) — PLAN §12-9 */
    var paperEditMode: Boolean by mutableStateOf(false)
    var editingTextId: String? by mutableStateOf(null)
    var selectedObjectId: String? by mutableStateOf(null)
    var editingCell: Pair<String, String>? by mutableStateOf(null)  // tableId to "r,c"

    /** 진행 중 획(젖은 잉크). liveVersion 증가 = 다시 그리기. */
    var liveBuilder: LiveStrokeBuilder? = null; private set
    var liveColor: Int = PlatenPalette.BLACK; private set
    var liveSlotIndex: Int = -1; private set
    var liveVersion: Int by mutableIntStateOf(0); private set
    /** 올가미 진행 폴리곤(페이지 로컬 pt) */
    var lassoPath: List<Pt> by mutableStateOf(emptyList()); private set
    /** 올가미 이동 드래그 미리보기(pt) */
    var dragDelta: Offset by mutableStateOf(Offset.Zero); private set
    /** 지우개로 이번 제스처에서 지운 획(커밋 전 시각적으로 숨김) */
    var erasing: Map<String, Stroke> by mutableStateOf(emptyMap()); private set
    var laser: List<LaserPoint> by mutableStateOf(emptyList()); private set

    /** 노트에 동봉되는 에셋(이미지)·브러시 zip. 저장 시 Bundle 로. */
    val assets = mutableStateMapOf<String, Pair<String, ByteArray>>().apply { putAll(attachments.assets) }
    val brushZips = HashMap<String, ByteArray>().apply { putAll(attachments.brushes) }

    val fingerDraw: Boolean get() = fingerDrawOverride ?: settings.ink.fingerDraw
    val slot: PenSlot? get() = slots.getOrNull(selectedSlot)

    fun attachments(): NoteAttachments {
        val used = controller.note.usedBrushIds()
        val zips = HashMap<String, ByteArray>()
        for (id in used) (brushZips[id] ?: brushes.zipBytes(id))?.let { zips[id] = it }
        return NoteAttachments(zips, assets.toMap())
    }

    // ------------------------------------------------------------ 도구

    fun selectTool(t: Tool) {
        if (t != tool) { previousTool = tool; tool = t }
        if (t != Tool.Lasso) controller.clearSelection()
        if (t != Tool.Text) editingTextId = null
    }
    fun previousTool() { val p = previousTool; selectTool(p) }

    /** 슬롯 탭: 다른 슬롯이면 선택, 같은 슬롯 재탭이면 true(=펜 시트 열기). */
    fun tapSlot(index: Int): Boolean {
        val reopen = tool == Tool.Pen && selectedSlot == index
        selectedSlot = index; selectTool(Tool.Pen)
        return reopen
    }

    fun updateSlot(index: Int, f: (PenSlot) -> PenSlot) { slots = slots.mapIndexed { i, s -> if (i == index) f(s) else s } }

    /** 리본 토글: 현재 펜 색 검정↔빨강(다른 색이면 검정) — PLAN §12-9 */
    fun ribbonToggle() { updateSlot(selectedSlot) { it.copy(color = PlatenPalette.ribbonToggle(it.color)) } }

    fun specFor(slot: PenSlot): BrushSpec? = brushes.get(slot.brushId)

    fun paramsFor(slot: PenSlot): ResolvedInkParams? {
        val spec = specFor(slot) ?: return null
        return ResolvedInkParams.resolve(spec, slot.size, slot.opacity, settings.ink, slot.overrides)
    }

    private val paramCache = HashMap<String, ResolvedInkParams>()
    /** 확정 획 렌더용 파라미터(브러시 id + 크기 + 오버라이드 키로 캐시). 미지의 브러시는 기본 펜으로 대체. */
    fun resolveStroke(s: Stroke): ResolvedInkParams {
        val key = "${s.brushId}|${s.size}|${s.opacity}|${s.overrides.hashCode()}|${settings.ink.hashCode()}"
        return paramCache.getOrPut(key) {
            val spec = brushes.get(s.brushId) ?: brushes.brushes.value.firstOrNull() ?: BrushSpec(id = s.brushId, name = "?")
            ResolvedInkParams.resolve(spec, s.size, s.opacity, settings.ink, s.overrides)
        }
    }
    fun invalidateParams() { paramCache.clear() }

    // ------------------------------------------------------------ 펜 세션

    private var strokeStartMs = 0L
    private var liveSlot: PageSlot? = null

    fun beginStroke(slotIdx: Int, pageSlot: PageSlot, first: InputSample, tMs: Long): Boolean {
        val s = slots.getOrNull(slotIdx) ?: return false
        val params = paramsFor(s) ?: return false
        liveBuilder = LiveStrokeBuilder.create(params)
        liveColor = s.color; liveSlotIndex = slotIdx; liveSlot = pageSlot; strokeStartMs = tMs
        liveBuilder!!.add(first.copy(t = 0f))
        liveVersion++
        return true
    }

    fun extendStroke(samples: List<InputSample>) {
        val b = liveBuilder ?: return
        b.addAll(samples)
        liveVersion++
    }

    fun strokeTime(tMs: Long): Float = (tMs - strokeStartMs).toFloat()

    fun finishStroke() {
        val b = liveBuilder ?: return
        val s = slots.getOrNull(liveSlotIndex)
        val page = liveSlot?.page
        liveBuilder = null; liveVersion++
        if (s == null || page == null) return
        val stroke = b.finish(newId(), s.brushId, s.color, nowMillis())
        if (stroke.samples.isNotEmpty()) controller.addStroke(stroke, page.id)
    }

    /** 시스템 취소(팜 등): 획 버림. */
    fun cancelStroke() { liveBuilder = null; liveVersion++ }

    // ------------------------------------------------------------ 지우개

    private var eraseSlot: PageSlot? = null
    fun beginErase(slot: PageSlot) { eraseSlot = slot; erasing = emptyMap() }

    fun eraseAt(p: Pt, radiusPt: Float = eraserRadiusPt) {
        val page = eraseSlot?.page ?: return
        val probe = Rect(p.x - radiusPt, p.y - radiusPt, p.x + radiusPt, p.y + radiusPt)
        var changed = false
        val acc = HashMap(erasing)
        for (s in page.strokes) {
            if (s.id in acc) continue
            if (!s.bounds().intersects(probe)) continue
            if (StrokeHitTest.hits(s, resolveStroke(s), p, radiusPt)) { acc[s.id] = s; changed = true }
        }
        if (changed) erasing = acc
    }

    fun finishErase() {
        val page = eraseSlot?.page
        val list = erasing.values.toList()
        erasing = emptyMap(); eraseSlot = null
        if (page != null && list.isNotEmpty()) controller.removeStrokes(list, page.id)
    }

    // ------------------------------------------------------------ 올가미

    private var lassoSlot: PageSlot? = null
    private var dragging = false

    /** 올가미 도구 다운: 선택 영역 안이면 이동 시작(true), 아니면 새 폴리곤 시작(false). */
    fun lassoDown(slot: PageSlot, p: Pt): Boolean {
        val sel = controller.selection
        if (sel != null && sel.pageId == slot.page.id && sel.bounds.inflate(8f).contains(p)) {
            dragging = true; dragDelta = Offset.Zero; return true
        }
        controller.clearSelection()
        lassoSlot = slot; lassoPath = listOf(p); dragging = false
        return false
    }

    fun lassoMove(p: Pt, deltaPt: Offset) {
        if (dragging) dragDelta += deltaPt else if (lassoPath.isNotEmpty()) lassoPath = lassoPath + p
    }

    fun lassoUp() {
        if (dragging) {
            dragging = false
            val d = dragDelta; dragDelta = Offset.Zero
            controller.moveSelection(d.x, d.y)
        } else {
            val slot = lassoSlot
            val path = lassoPath
            lassoPath = emptyList(); lassoSlot = null
            if (slot != null && path.size >= 3) {
                if (controller.pageIndex != slot.index) controller.goToPage(slot.index)
                controller.selectLasso(path)
            }
        }
    }

    // ------------------------------------------------------------ 레이저

    fun laserAdd(slotIndex: Int, p: Pt, t: Long) { laser = laser + LaserPoint(slotIndex, p, t) }
    fun laserPrune(now: Long, fadeMs: Int) { if (laser.isNotEmpty()) laser = laser.filter { now - it.t < fadeMs } }

    // ------------------------------------------------------------ 객체

    fun insertText(slot: PageSlot, p: Pt): PageObject.TextBox {
        val tb = PageObject.TextBox(newId(), Rect(p.x, p.y, p.x + 160f, p.y + 40f), "")
        controller.addObject(tb, slot.page.id)
        editingTextId = tb.id; selectedObjectId = tb.id
        return tb
    }

    fun insertTable(slot: PageSlot, p: Pt, rows: Int = 3, cols: Int = 3): PageObject.Table {
        val cw = 80f; val rh = 32f
        val t = PageObject.Table(newId(), Rect(p.x, p.y, p.x + cw * cols, p.y + rh * rows), rows, cols, List(cols) { cw }, List(rows) { rh })
        controller.addObject(t, slot.page.id)
        selectedObjectId = t.id
        return t
    }

    fun insertImage(slot: PageSlot, p: Pt, mime: String, bytes: ByteArray, aspect: Float): PageObject.Image {
        val id = newId()
        assets[id] = mime to bytes
        val w = 200f.coerceAtMost(slot.page.size.width - p.x)
        val h = w / aspect.coerceAtLeast(0.05f)
        val img = PageObject.Image(newId(), Rect(p.x, p.y, p.x + w, p.y + h), id)
        controller.addObject(img, slot.page.id)
        selectedObjectId = img.id
        return img
    }

    /** 표 행/열 ±(최소 1, 최대 50 — PLAN A7). */
    fun tableResize(t: PageObject.Table, rowDelta: Int, colDelta: Int, pageId: String) {
        val rows = (t.rows + rowDelta).coerceIn(1, 50); val cols = (t.cols + colDelta).coerceIn(1, 50)
        if (rows == t.rows && cols == t.cols) return
        val rh = t.rowHeights.lastOrNull() ?: 32f; val cw = t.colWidths.lastOrNull() ?: 80f
        val rowHeights = List(rows) { t.rowHeights.getOrNull(it) ?: rh }
        val colWidths = List(cols) { t.colWidths.getOrNull(it) ?: cw }
        val after = t.copy(rows = rows, cols = cols, rowHeights = rowHeights, colWidths = colWidths,
            rect = Rect(t.rect.left, t.rect.top, t.rect.left + colWidths.sum(), t.rect.top + rowHeights.sum()),
            cells = t.cells.filterKeys { k -> val (r, c) = k.split(','); r.toInt() < rows && c.toInt() < cols })
        controller.updateObject(t, after, pageId)
    }

    fun moveObject(obj: PageObject, dx: Float, dy: Float, pageId: String) {
        if (dx == 0f && dy == 0f) return
        controller.updateObject(obj, io.github.bigrirason_debug.platen.ui.state.LassoOps.translateObject(obj, dx, dy), pageId)
    }

    fun note(): Note = controller.note
}

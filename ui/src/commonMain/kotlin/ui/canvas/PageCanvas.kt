package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bigrirason_debug.platen.ink.PathPoint
import io.github.bigrirason_debug.platen.model.GestureAction
import io.github.bigrirason_debug.platen.model.GestureInput
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.render.InkTheme
import io.github.bigrirason_debug.platen.render.PageRenderer
import io.github.bigrirason_debug.platen.render.StrokeRenderer
import io.github.bigrirason_debug.platen.ui.diag.DiagnosticsState
import io.github.bigrirason_debug.platen.ui.input.FingerGestureRecognizer
import io.github.bigrirason_debug.platen.ui.input.GestureDispatcher
import io.github.bigrirason_debug.platen.ui.input.InputPolicy
import io.github.bigrirason_debug.platen.ui.input.PlatformPointerExtras
import io.github.bigrirason_debug.platen.ui.input.PointerRole
import io.github.bigrirason_debug.platen.ui.platform.PlatformImages
import io.github.bigrirason_debug.platen.ui.state.Tool
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import io.github.bigrirason_debug.platen.ui.theme.toComposeColor

/** 입력 세션 종류(포인터 1개가 다운~업 동안 하는 일). */
private sealed interface Session {
    data class Pen(val slotIdx: Int) : Session
    data object Erase : Session
    data object Lasso : Session
    data object Laser : Session
    /** 탭으로 생성하는 도구(텍스트·표·이미지): 업에서 처리 */
    data class TapInsert(val tool: Tool) : Session
    data class MoveObject(val obj: PageObject, val pageId: String) : Session
}

/**
 * 페이지 캔버스: 세로 페이지 스크롤 + 핀치 줌 + 도구 입력 + 렌더(PageRenderer / StrokeRenderer.drawLive).
 * 입력 정책은 InputPolicy, 제스처는 FingerGestureRecognizer → GestureDispatcher.
 */
@Composable
fun PageCanvas(
    state: CanvasState,
    pageRenderer: PageRenderer,
    strokeRenderer: StrokeRenderer,
    dispatcher: GestureDispatcher,
    diag: DiagnosticsState,
    mouseDraw: Boolean,
    onPickImage: (PageSlot, Pt) -> Unit,
    onTapObject: (PageObject?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlatenTheme.colors
    val density = LocalDensity.current
    val vp = state.viewport
    val controller = state.controller
    val inkTheme = remember(colors.dark) { if (colors.dark) InkTheme.Dark else InkTheme.Light }
    val bitmaps = remember { HashMap<String, ImageBitmap?>() }
    val textMeasurer = rememberTextMeasurer()
    val touchSlop = with(density) { 12.dp.toPx() }
    val swipeMin = with(density) { 64.dp.toPx() }
    vp.marginPx = with(density) { 24.dp.toPx() }
    vp.updateLayout(PageLayout.of(controller.note))
    // 젖은 잉크 프론트버퍼(Android). 형광펜·반투명은 겹침이 진해지므로 Compose 경로만 쓴다.
    val wetHolder = remember { arrayOfNulls<WetInkOverlay>(1) }
    // PDF 속지·플래너 배경 래스터(원본은 에셋으로 보존)
    val bgScope = androidx.compose.runtime.rememberCoroutineScope()
    val pdfBg = remember(state) { PdfBackgrounds({ id -> state.assets[id]?.second }, bgScope) }
    androidx.compose.runtime.DisposableEffect(pdfBg) { onDispose { pdfBg.close() } }
    val wetEnabled = state.settings.ink.frontBuffer && !mouseDraw

    Box(Modifier.fillMaxSize()) {
    WetInkLayer(wetEnabled, Modifier.fillMaxSize()) { wetHolder[0] = it }
    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { vp.updateSize(it) }
            .pointerInput(state, mouseDraw) {
                awaitPointerEventScope {
                    val touches = HashMap<PointerId, Offset>()
                    var drawId: PointerId? = null
                    var session: Session? = null
                    var slot: PageSlot? = null
                    var downPos = Offset.Zero
                    var lastPos = Offset.Zero
                    var moved = false
                    var buttonSession = false
                    var stylusActive = false
                    val recognizer = FingerGestureRecognizer(touchSlop, swipeMin)
                    var wetLast: Offset? = null
                    var wetArgb = 0
                    fun wetWidth(pressure: Float): Float {
                        val b = state.liveBuilder ?: return 0f
                        val lut = b.params.pressureSizeLut
                        val k = lut[(pressure.coerceIn(0f, 1f) * (lut.size - 1)).toInt()]
                        return (b.params.size * k * vp.scale).coerceAtLeast(1f)
                    }
                    fun wetTo(x: Float, y: Float, pressure: Float) {
                        val w = wetHolder[0] ?: return
                        val from = wetLast ?: Offset(x, y)
                        if (wetArgb != 0) w.segment(from.x, from.y, x, y, wetWidth(pressure), wetArgb)
                        wetLast = Offset(x, y)
                    }

                    fun endSession(canceled: Boolean, now: Long) {
                        val s = session; val sl = slot
                        when (s) {
                            is Session.Pen -> {
                                if (canceled) state.cancelStroke() else state.finishStroke()
                                if (wetLast != null) wetHolder[0]?.clearAfterDry()
                                wetLast = null
                            }
                            Session.Erase -> if (canceled) state.finishErase() else state.finishErase()
                            Session.Lasso -> state.lassoUp()
                            Session.Laser -> {}
                            is Session.TapInsert -> if (!canceled && !moved && sl != null) {
                                val p = vp.hitLoose(downPos, sl)
                                when (s.tool) {
                                    Tool.Text -> state.insertText(sl, p)
                                    Tool.Table -> state.insertTable(sl, p)
                                    Tool.Image -> onPickImage(sl, p)
                                    else -> {}
                                }
                            }
                            is Session.MoveObject -> {
                                val d = (lastPos - downPos) / vp.scale
                                if (moved) state.moveObject(s.obj, d.x, d.y, s.pageId) else onTapObject(s.obj)
                            }
                            null -> {}
                        }
                        if (buttonSession && !moved && s !is Session.Erase && s !is Session.Lasso && s !is Session.Laser) {
                            dispatcher.dispatch(GestureInput.stylusButtonClick)
                        }
                        session = null; drawId = null; slot = null; buttonSession = false
                    }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val now = event.changes.firstOrNull()?.uptimeMillis ?: 0L

                        // 휠: 스크롤 / Ctrl+휠: 줌 (마우스 전용 1차 기준, DESIGN §7)
                        if (event.type == PointerEventType.Scroll) {
                            val ch = event.changes.first()
                            val d = ch.scrollDelta
                            if (event.keyboardModifiers.isCtrlPressed) {
                                vp.zoomBy(if (d.y < 0) 1.1f else 1f / 1.1f, ch.position)
                            } else {
                                vp.pan(Offset(-d.x * 48f, -d.y * 48f))
                            }
                            ch.consume(); continue
                        }

                        for (ch in event.changes) {
                            val isStylus = ch.type == PointerType.Stylus || ch.type == PointerType.Eraser
                            if (isStylus) stylusActive = event.type != PointerEventType.Exit
                            val extra = PlatformPointerExtras.describe(event, ch)
                            diag.input.record(extra, ch.type.toString())

                            if (extra.canceled && ch.id == drawId) { endSession(canceled = true, now); ch.consume(); continue }
                            if (!ch.pressed && !ch.previousPressed) continue // hover

                            val role = InputPolicy.roleFor(
                                type = ch.type, tool = state.tool, fingerDraw = state.fingerDraw,
                                palmRejection = state.settings.ink.palmRejection,
                                stylusActive = stylusActive && ch.type == PointerType.Touch,
                                mouseDraw = mouseDraw, touchCount = touches.size + (if (ch.id in touches) 0 else 1),
                            )
                            if (role == PointerRole.Ignore) { ch.consume(); continue }

                            when {
                                ch.changedToDownIgnoreConsumed() -> {
                                    if (role == PointerRole.Draw && drawId == null) {
                                        val hit = vp.hit(ch.position) ?: continue
                                        val (sl, p) = hit
                                        slot = sl; drawId = ch.id; downPos = ch.position; lastPos = ch.position; moved = false
                                        if (controller.pageIndex != sl.index) controller.goToPage(sl.index)
                                        var tool = state.tool
                                        buttonSession = extra.stylusButton
                                        if (extra.stylusButton) {
                                            when (dispatcher.map.actionFor(GestureInput.stylusButtonDrag)) {
                                                GestureAction.eraser -> tool = Tool.Eraser
                                                GestureAction.lasso -> tool = Tool.Lasso
                                                GestureAction.laser -> tool = Tool.Laser
                                                else -> {}
                                            }
                                        }
                                        if (ch.type == PointerType.Eraser) tool = Tool.Eraser
                                        // 객체 조작(마우스 전용 기준): 비펜 도구로 객체를 누르면 이동 세션
                                        val obj = if (tool != Tool.Pen && tool != Tool.Eraser && tool != Tool.Laser) sl.page.objects.lastOrNull { it.rect.contains(p) } else null
                                        session = when {
                                            obj != null && (tool == Tool.Lasso && controller.selection == null || tool == Tool.Text || tool == Tool.Table || tool == Tool.Image) -> {
                                                state.selectedObjectId = obj.id; Session.MoveObject(obj, sl.page.id)
                                            }
                                            tool == Tool.Pen -> {
                                                val pt0 = extra.points.first()
                                                val ok = state.beginStroke(state.selectedSlot, sl, InputSample(p.x, p.y, pt0.pressure, pt0.tiltX, pt0.tiltY, 0f), pt0.timeMs)
                                                if (ok) {
                                                    val b = state.liveBuilder
                                                    val opaque = b != null && b.params.spec.role != io.github.bigrirason_debug.platen.model.BrushRole.highlighter &&
                                                        b.params.spec.blend == io.github.bigrirason_debug.platen.model.BlendMode.normal && b.params.opacity >= 0.99f
                                                    wetArgb = if (opaque) io.github.bigrirason_debug.platen.render.InkColorPolicy.remapInk(state.liveColor, b!!.params.spec.role, inkTheme) else 0
                                                    wetLast = null
                                                    if (wetArgb != 0) { wetHolder[0]?.penDown(event); wetTo(pt0.x, pt0.y, pt0.pressure) }
                                                }
                                                if (ok) Session.Pen(state.selectedSlot) else null
                                            }
                                            tool == Tool.Eraser -> { state.beginErase(sl); state.eraseAt(p); Session.Erase }
                                            tool == Tool.Lasso -> { state.lassoDown(sl, p); Session.Lasso }
                                            tool == Tool.Laser -> { state.laserAdd(sl.index, p, now); Session.Laser }
                                            else -> { state.selectedObjectId = null; Session.TapInsert(tool) }
                                        }
                                        if (session == null) drawId = null
                                        ch.consume()
                                    } else if (role == PointerRole.Navigate) {
                                        touches[ch.id] = ch.position
                                        val c = event.calculateCentroid(useCurrent = true)
                                        if (touches.size == 1) recognizer.begin(1, c.x, c.y, now) else recognizer.update(touches.size, c.x, c.y)
                                        ch.consume()
                                    }
                                }
                                ch.changedToUpIgnoreConsumed() -> {
                                    if (ch.id == drawId) {
                                        val sl = slot
                                        if (sl != null) {
                                            when (session) {
                                                is Session.Pen -> {
                                                extra.points.forEach { rp -> wetTo(rp.x, rp.y, rp.pressure) }
                                                state.extendStroke(extra.points.map { rp -> val lp = vp.hitLoose(Offset(rp.x, rp.y), sl); InputSample(lp.x, lp.y, rp.pressure, rp.tiltX, rp.tiltY, state.strokeTime(rp.timeMs)) })
                                            }
                                                else -> {}
                                            }
                                        }
                                        lastPos = ch.position
                                        if ((ch.position - downPos).getDistance() > touchSlop / 2) moved = true
                                        endSession(canceled = false, now)
                                        ch.consume()
                                    } else if (touches.remove(ch.id) != null) {
                                        if (touches.isEmpty()) recognizer.end(now)?.let { dispatcher.dispatch(it) }
                                        ch.consume()
                                    }
                                }
                                ch.pressed -> {
                                    if (ch.id == drawId) {
                                        val sl = slot ?: continue
                                        if ((ch.position - downPos).getDistance() > touchSlop / 2) moved = true
                                        when (val s = session) {
                                            is Session.Pen -> {
                                                extra.points.forEach { rp -> wetTo(rp.x, rp.y, rp.pressure) }
                                                state.extendStroke(extra.points.map { rp -> val lp = vp.hitLoose(Offset(rp.x, rp.y), sl); InputSample(lp.x, lp.y, rp.pressure, rp.tiltX, rp.tiltY, state.strokeTime(rp.timeMs)) })
                                            }
                                            Session.Erase -> extra.points.forEach { rp -> state.eraseAt(vp.hitLoose(Offset(rp.x, rp.y), sl)) }
                                            Session.Lasso -> {
                                                val p = vp.hitLoose(ch.position, sl)
                                                val d = (ch.position - lastPos) / vp.scale
                                                state.lassoMove(p, d)
                                            }
                                            Session.Laser -> extra.points.forEach { rp -> state.laserAdd(sl.index, vp.hitLoose(Offset(rp.x, rp.y), sl), rp.timeMs) }
                                            is Session.MoveObject, is Session.TapInsert, null -> {}
                                        }
                                        lastPos = ch.position
                                        ch.consume()
                                    } else if (ch.id in touches) {
                                        touches[ch.id] = ch.position
                                    }
                                }
                            }
                        }

                        // 네비게이션(손가락 팬/줌, 마우스 좌드래그 팬)
                        if (touches.isNotEmpty() && drawId == null && event.type == PointerEventType.Move) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val c = event.calculateCentroid(useCurrent = true)
                            if (zoom != 1f && c.isSpecified()) vp.zoomBy(zoom, c)
                            if (pan != Offset.Zero) vp.pan(pan)
                            if (c.isSpecified()) recognizer.update(touches.size, c.x, c.y)
                            event.changes.forEach { if (it.id in touches) it.consume() }
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val visible = vp.visibleDocRect()
            val scale = vp.scale
            val live = state.liveVersion // 구독
            for (slot in vp.layout.slots) {
                if (!slot.rect.intersects(visible)) continue
                val page = slot.page
                val origin = Offset(vp.offset.x + slot.x * scale, vp.offset.y + slot.y * scale)
                val sizePx = Size(page.size.width * scale, page.size.height * scale)
                // 페이지 피드 암시: 상단 2dp 그림자(모션 없음)
                drawRect(Color.Black.copy(alpha = if (colors.dark) 0.5f else 0.12f), topLeft = origin + Offset(0f, -2.dp.toPx()), size = Size(sizePx.width, 2.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.06f), topLeft = origin + Offset(2.dp.toPx(), 2.dp.toPx()), size = sizePx)
                withTransform({ translate(origin.x, origin.y); scale(scale, scale, pivot = Offset.Zero) }) {
                    clipRect(0f, 0f, page.size.width, page.size.height) {
                        val erasing = state.erasing
                        val renderPage = if (erasing.isEmpty()) page else page.copy(strokes = page.strokes.filterNot { it.id in erasing })
                        val localVisible = Rect(visible.left - slot.x, visible.top - slot.y, visible.right - slot.x, visible.bottom - slot.y)
                        when (val bg = page.background) {
                            is io.github.bigrirason_debug.platen.model.Background.PdfPage -> {
                                @Suppress("UNUSED_VARIABLE") val bgVersion = pdfBg.version // 래스터 완료 시 다시 그리기
                                drawRect(Color.White, size = Size(page.size.width, page.size.height))
                                pdfBg.bitmap(bg.assetId, bg.pageIndex)?.let { img ->
                                    drawImage(img, dstOffset = androidx.compose.ui.unit.IntOffset.Zero, dstSize = androidx.compose.ui.unit.IntSize(page.size.width.toInt(), page.size.height.toInt()))
                                }
                            }
                            is io.github.bigrirason_debug.platen.model.Background.Image -> {
                                drawRect(Color.White, size = Size(page.size.width, page.size.height))
                                bitmaps.getOrPut("bg:" + bg.assetId) { state.assets[bg.assetId]?.let { PlatformImages.decode(it.second) } }?.let { img ->
                                    drawImage(img, dstOffset = androidx.compose.ui.unit.IntOffset.Zero, dstSize = androidx.compose.ui.unit.IntSize(page.size.width.toInt(), page.size.height.toInt()))
                                }
                            }
                            else -> {}
                        }
                        with(pageRenderer) { drawPage(renderPage, localVisible, inkTheme) { state.resolveStroke(it) } }
                        drawObjects(page.objects, bitmaps, state, colors.ink, textMeasurer, scale)
                        if (live >= 0) state.liveBuilder?.let { b ->
                            if (state.liveSlot?.page?.id == slot.page.id) {
                                val pts: List<PathPoint> = b.committedPoints() + b.pendingPoints()
                                with(strokeRenderer) { drawLive(pts, b.params, state.liveColor, inkTheme) }
                            }
                        }
                        drawLassoAndSelection(state, slot, colors.red, scale)
                        drawLaser(state, slot, state.settings.laserColor.toComposeColor(), state.settings.laserWidth / scale, state.settings.laserFadeMillis)
                    }
                }
            }
        }
    }
    }
}

private fun DrawScope.drawObjects(objects: List<PageObject>, bitmaps: HashMap<String, ImageBitmap?>, state: CanvasState, ink: Color, measurer: androidx.compose.ui.text.TextMeasurer, scale: Float) {
    for (o in objects) {
        when (o) {
            is PageObject.Table -> {
                val c = o.lineColor.toComposeColor()
                val w = o.lineWidth
                var y = o.rect.top
                for (r in 0..o.rows) {
                    drawLine(c, Offset(o.rect.left, y), Offset(o.rect.right, y), strokeWidth = w)
                    if (r < o.rows) y += o.rowHeights.getOrElse(r) { 32f }
                }
                var x = o.rect.left
                for (col in 0..o.cols) {
                    drawLine(c, Offset(x, o.rect.top), Offset(x, o.rect.bottom), strokeWidth = w)
                    if (col < o.cols) x += o.colWidths.getOrElse(col) { 80f }
                }
                if (o.cells.isNotEmpty()) {
                    var cy = o.rect.top
                    for (r in 0 until o.rows) {
                        var cx = o.rect.left
                        for (col in 0 until o.cols) {
                            val txt = o.cells["$r,$col"]
                            if (!txt.isNullOrEmpty()) {
                                drawText(measurer, txt, topLeft = Offset(cx + 3f, cy + 3f), style = TextStyle(color = ink, fontSize = 10.sp),
                                    size = Size(o.colWidths.getOrElse(col) { 80f } - 6f, o.rowHeights.getOrElse(r) { 32f } - 6f), softWrap = true)
                            }
                            cx += o.colWidths.getOrElse(col) { 80f }
                        }
                        cy += o.rowHeights.getOrElse(r) { 32f }
                    }
                }
            }
            is PageObject.Image -> {
                val bmp = bitmaps.getOrPut(o.assetId) { state.assets[o.assetId]?.let { PlatformImages.decode(it.second) } }
                if (bmp != null) {
                    drawImage(bmp, dstOffset = IntOffset(o.rect.left.toInt(), o.rect.top.toInt()), dstSize = IntSize(o.rect.width.toInt().coerceAtLeast(1), o.rect.height.toInt().coerceAtLeast(1)))
                } else {
                    drawRect(ink.copy(alpha = 0.08f), Offset(o.rect.left, o.rect.top), Size(o.rect.width, o.rect.height))
                    drawRect(ink.copy(alpha = 0.4f), Offset(o.rect.left, o.rect.top), Size(o.rect.width, o.rect.height), style = Stroke(1f))
                }
            }
            is PageObject.TextBox -> {} // 오버레이 컴포저블이 그린다(편집 가능)
            is PageObject.Link -> drawRect(ink.copy(alpha = 0.15f), Offset(o.rect.left, o.rect.top), Size(o.rect.width, o.rect.height), style = Stroke(0.5f))
        }
        if (state.selectedObjectId == o.id) {
            drawRect(Color(0xFFC8102E).copy(alpha = 0.9f), Offset(o.rect.left, o.rect.top), Size(o.rect.width, o.rect.height), style = Stroke(1.5f / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f / scale, 3f / scale))))
        }
    }
}

private fun DrawScope.drawLassoAndSelection(state: CanvasState, slot: PageSlot, red: Color, scale: Float) {
    val path = state.lassoPath
    if (path.size >= 2 && state.controller.pageIndex == slot.index) {
        val p = Path().apply { moveTo(path[0].x, path[0].y); for (i in 1 until path.size) lineTo(path[i].x, path[i].y) }
        drawPath(p, red.copy(alpha = 0.12f))
        drawPath(p, red, style = Stroke(1.5f / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f / scale, 4f / scale)), cap = StrokeCap.Round))
    }
    val sel = state.controller.selection
    if (sel != null && sel.pageId == slot.page.id && !sel.isEmpty) {
        val b = sel.bounds.inflate(6f)
        val d = state.dragDelta
        drawRect(red.copy(alpha = 0.12f), Offset(b.left + d.x, b.top + d.y), Size(b.width, b.height))
        drawRect(red, Offset(b.left + d.x, b.top + d.y), Size(b.width, b.height), style = Stroke(1.5f / scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f / scale, 4f / scale))))
    }
}

private fun DrawScope.drawLaser(state: CanvasState, slot: PageSlot, color: Color, widthPt: Float, fadeMs: Int) {
    val pts = state.laser.filter { it.slotIndex == slot.index }
    if (pts.size < 2) return
    val now = maxOf(state.laserNow, pts.last().t)
    for (i in 1 until pts.size) {
        val a = pts[i - 1]; val b = pts[i]
        val age = (now - b.t).toFloat() / fadeMs.coerceAtLeast(1)
        val alpha = (1f - age).coerceIn(0f, 1f)
        if (alpha <= 0f) continue
        drawLine(color.copy(alpha = alpha), Offset(a.p.x, a.p.y), Offset(b.p.x, b.p.y), strokeWidth = widthPt.coerceAtLeast(1f), cap = StrokeCap.Round)
    }
}

private fun Offset.isSpecified() = this != Offset.Unspecified && this.x.isFinite() && this.y.isFinite()

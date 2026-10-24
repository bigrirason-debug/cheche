package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.BrushRole
import io.github.bigrirason_debug.platen.ui.components.PlatenTooltip
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.state.MarginSide
import io.github.bigrirason_debug.platen.ui.state.MarginStops
import io.github.bigrirason_debug.platen.ui.state.Tool
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcon
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import io.github.bigrirason_debug.platen.ui.theme.toComposeColor

/** 브러시 역할 → 슬롯 아이콘. */
fun iconForRole(role: BrushRole?): PlatenIcon = when (role) {
    BrushRole.fountain -> PlatenIcons.Fountain
    BrushRole.pencil -> PlatenIcons.Pencil
    BrushRole.highlighter -> PlatenIcons.Highlighter
    BrushRole.marker -> PlatenIcons.Monoline
    else -> PlatenIcons.Pen
}

/**
 * 페이퍼 베일 = 툴바 프레임 + 눈금자(typewriter-refs §1, DESIGN §5).
 * 1행: 뒤로 · 펜꽂이 슬롯 5 · 리본 토글 · 지우개 · 올가미 · 텍스트 · 이미지 · 표 · 레이저 · undo · redo · 페이지 추가 · 더보기
 * 2행(눈금자 8dp): 1px 눈금 + 마진 스톱 마커 2개. 마커 드래그는 속지 편집 모드에서만.
 */
@Composable
fun PaperBail(
    state: CanvasState,
    onBack: () -> Unit,
    onOpenPenSheet: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PlatenTheme.colors
    val ctrl = state.controller
    Column(modifier.fillMaxWidth().background(c.chrome)) {
        Row(
            Modifier.fillMaxWidth().height(PlatenSpacing.toolbarHeight).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolIconButton(PlatenIcons.Back, onClick = onBack)
            Spacer(Modifier.width(PlatenSpacing.xs))
            // 펜꽂이 슬롯: 선택된 슬롯 재탭 = 펜 설정 시트(PLAN §4.8)
            state.slots.forEachIndexed { i, slot ->
                val spec = state.specFor(slot)
                ToolIconButton(
                    icon = iconForRole(spec?.role),
                    label = "${spec?.name ?: "펜"} 슬롯 ${i + 1}",
                    selected = state.tool == Tool.Pen && state.selectedSlot == i,
                    badgeColor = slot.color.toComposeColor(),
                    onClick = { if (state.tapSlot(i)) onOpenPenSheet() },
                )
            }
            RibbonToggleButton(current = state.slot?.color ?: 0, onClick = { state.ribbonToggle() })
            Spacer(Modifier.width(PlatenSpacing.s))
            for (t in listOf(Tool.Eraser, Tool.Lasso, Tool.Text, Tool.Image, Tool.Table, Tool.Laser)) {
                ToolIconButton(t.icon, selected = state.tool == t, onClick = { state.selectTool(t) })
            }
            Spacer(Modifier.width(PlatenSpacing.s))
            ToolIconButton(PlatenIcons.Undo, enabled = ctrl.canUndo, onClick = { ctrl.undo() })
            ToolIconButton(PlatenIcons.Redo, enabled = ctrl.canRedo, onClick = { ctrl.redo() })
            ToolIconButton(PlatenIcons.AddPage, onClick = { ctrl.addPageAfterCurrent(); state.viewport.scrollToPage(ctrl.pageIndex) })
            Spacer(Modifier.weight(1f))
            ToolIconButton(PlatenIcons.More, onClick = onMore)
        }
        Ruler(state)
        if (state.paperEditMode) PaperEditBar(state)
    }
}

/** 2색 리본 토글(검정↔빨강, PLAN §12-9). 아이콘 자체가 두 색 띠라 tint 대신 직접 그린다. */
@Composable
private fun RibbonToggleButton(current: Int, onClick: () -> Unit) {
    val c = PlatenTheme.colors
    val isRed = current == io.github.bigrirason_debug.platen.ui.theme.PlatenPalette.RED
    PlatenTooltip(text = PlatenIcons.Ribbon.label, shortcut = PlatenIcons.Ribbon.shortcut) {
        Box(
            Modifier.width(PlatenSpacing.target).height(PlatenSpacing.target)
                .semantics { contentDescription = PlatenIcons.Ribbon.label + (if (isRed) " (빨강)" else " (검정)") }
                .pointerInput(Unit) { detectTapGestures { onClick() } },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.width(PlatenSpacing.iconSize).height(PlatenSpacing.iconSize)) {
                val w = size.width; val h = size.height
                drawRect(c.ink, topLeft = Offset(w * 0.17f, h * 0.3f), size = androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.18f))
                drawRect(c.red, topLeft = Offset(w * 0.17f, h * 0.52f), size = androidx.compose.ui.geometry.Size(w * 0.66f, h * 0.18f))
                val y = if (isRed) h * 0.61f else h * 0.39f
                drawRect(c.ink, topLeft = Offset(w * 0.06f, y - h * 0.14f), size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.28f))
                drawRect(c.ink, topLeft = Offset(w * 0.88f, y - h * 0.14f), size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.28f))
            }
        }
    }
}

/** 눈금자: 현재 페이지의 화면 x 범위에 맞춘 1px 틱(12pt 간격, 72pt 마다 긴 틱 + 모노 라벨) + 마진 스톱 마커. */
@Composable
private fun Ruler(state: CanvasState) {
    val c = PlatenTheme.colors
    val vp = state.viewport
    val ctrl = state.controller
    val page = ctrl.page
    val template = MarginStops.templateOf(page.background)
    val measurer = rememberTextMeasurer()
    var dragSide by remember { mutableStateOf<MarginSide?>(null) }
    val slot = vp.layout.slots.getOrNull(ctrl.pageIndex)
    val pageLeftPx = if (slot != null) vp.offset.x + slot.x * vp.scale else 0f
    val scale = vp.scale
    val rulerH = PlatenSpacing.rulerHeight + 8.dp

    fun docXAt(px: Float) = (px - pageLeftPx) / scale

    Box(
        Modifier.fillMaxWidth().height(rulerH)
            .semantics {
                contentDescription = if (template != null) "눈금자. 왼쪽 여백 ${template.marginLeft.toInt()}pt, 오른쪽 여백 ${template.marginRight.toInt()}pt" + (if (state.paperEditMode) " (속지 편집 중)" else "") else "눈금자"
                if (template != null) progressBarRangeInfo = ProgressBarRangeInfo(template.marginLeft, 0f..page.size.width)
            }
            .pointerInput(state.paperEditMode, template, slot) {
                detectTapGestures(
                    onLongPress = { state.paperEditMode = true },
                    onTap = { if (state.paperEditMode) state.paperEditMode = false },
                )
            }
            .pointerInput(state.paperEditMode, template, slot) {
                if (!state.paperEditMode || template == null) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos ->
                        val lx = pageLeftPx + MarginStops.markerX(template, MarginSide.left, page.size.width) * scale
                        val rx = pageLeftPx + MarginStops.markerX(template, MarginSide.right, page.size.width) * scale
                        dragSide = if (kotlin.math.abs(pos.x - lx) <= kotlin.math.abs(pos.x - rx)) MarginSide.left else MarginSide.right
                    },
                    onDragEnd = { dragSide = null },
                    onDragCancel = { dragSide = null },
                ) { change, _ ->
                    val side = dragSide ?: return@detectDragGestures
                    val cur = MarginStops.templateOf(ctrl.page.background) ?: return@detectDragGestures
                    state.controller.setBackground(MarginStops.drag(cur, side, docXAt(change.position.x), page.size.width))
                    change.consume()
                }
            },
    ) {
        Canvas(Modifier.fillMaxWidth().height(rulerH)) {
            val h = size.height
            // 상단 1px 선 = 베일 하단 테두리(rule)
            drawLine(c.rule, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
            if (slot != null) {
                val pw = page.size.width
                var x = 0f
                while (x <= pw + 0.01f) {
                    val px = pageLeftPx + x * scale
                    if (px in -2f..size.width + 2f) {
                        val major = (x / 72f).let { kotlin.math.abs(it - kotlin.math.round(it)) < 0.01f }
                        drawLine(c.rule, Offset(px, 0f), Offset(px, if (major) h * 0.55f else h * 0.3f), strokeWidth = 1f)
                        if (major && x > 0f && scale > 0.6f) {
                            drawText(measurer, (x / 72f).toInt().toString(), topLeft = Offset(px + 2f, h * 0.2f), style = MonoTextStyle.copy(color = c.mutedText, fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp)))
                        }
                    }
                    x += 12f
                }
                if (template != null) {
                    for (side in MarginSide.entries) {
                        val mx = pageLeftPx + MarginStops.markerX(template, side, pw) * scale
                        val s = if (state.paperEditMode) 12.dp.toPx() else 8.dp.toPx()
                        val tri = Path().apply { moveTo(mx - s / 2, 0f); lineTo(mx + s / 2, 0f); lineTo(mx, s * 0.85f); close() }
                        drawPath(tri, c.red)
                        if (state.paperEditMode) drawLine(c.red.copy(alpha = 0.4f), Offset(mx, 0f), Offset(mx, h), strokeWidth = 1f)
                    }
                }
            }
            if (state.paperEditMode) drawRect(c.red.copy(alpha = 0.12f), topLeft = Offset(0f, h - 2f), size = androidx.compose.ui.geometry.Size(size.width, 2f))
        }
    }
}

/** 속지 편집 모드 바: 라인 스페이스 3단(타자기 레버) + 종류 + 종료. */
@Composable
private fun PaperEditBar(state: CanvasState) {
    val c = PlatenTheme.colors
    val ctrl = state.controller
    val t = MarginStops.templateOf(ctrl.page.background)
    Row(Modifier.fillMaxWidth().background(c.chromeRaised).padding(horizontal = PlatenSpacing.m, vertical = PlatenSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text("속지", color = c.ink)
        Spacer(Modifier.width(PlatenSpacing.m))
        if (t != null) {
            SingleChoiceSegmentedButtonRow {
                listOf("줄", "모눈", "점", "빈").forEachIndexed { i, label ->
                    val kind = listOf("lines", "grid", "dots", "blank")[i]
                    SegmentedButton(selected = t.kind == kind, onClick = { ctrl.setBackground(t.copy(kind = kind)) }, shape = SegmentedButtonDefaults.itemShape(i, 4, baseShape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))) { Text(label) }
                }
            }
            Spacer(Modifier.width(PlatenSpacing.m))
            Text("줄 간격", color = c.mutedText, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(PlatenSpacing.s))
            SingleChoiceSegmentedButtonRow {
                MarginStops.LINE_SPACINGS.forEachIndexed { i, sp ->
                    SegmentedButton(selected = MarginStops.spacingIndex(t) == i, onClick = { ctrl.setBackground(MarginStops.withSpacing(t, i)) }, shape = SegmentedButtonDefaults.itemShape(i, 3, baseShape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))) {
                        Text("${sp.toInt()}", style = MonoTextStyle)
                    }
                }
            }
            Spacer(Modifier.width(PlatenSpacing.m))
            Text("여백 ${t.marginLeft.toInt()} / ${t.marginRight.toInt()} pt", style = MonoTextStyle, color = c.mutedText)
        } else {
            Text("이 페이지는 템플릿 속지가 아닙니다(PDF/이미지). ", color = c.mutedText)
            Text("템플릿으로 바꾸기", color = c.ink, modifier = Modifier.pointerInput(Unit) { detectTapGestures { ctrl.setBackground(Background.Template("lines")) } })
        }
        Spacer(Modifier.weight(1f))
        ToolIconButton(PlatenIcons.Check, label = "속지 편집 끝", onClick = { state.paperEditMode = false })
    }
}

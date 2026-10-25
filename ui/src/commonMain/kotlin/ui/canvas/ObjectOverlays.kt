package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.state.Tool
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import io.github.bigrirason_debug.platen.ui.theme.toComposeColor
import kotlin.math.roundToInt

/**
 * 캔버스 위 컴포저블 오버레이: 텍스트 상자(표시/편집), 선택 객체 액션(삭제·표 행/열 ±·셀 편집), 올가미 선택 액션(삭제·복제).
 * 편집 중이 아닌 텍스트는 포인터 핸들러가 없어 아래 캔버스로 입력이 통과한다(펜이 글자 위를 지나가도 됨).
 */
@Composable
fun ObjectOverlays(state: CanvasState, modifier: Modifier = Modifier) {
    val c = PlatenTheme.colors
    val vp = state.viewport
    val ctrl = state.controller
    val density = LocalDensity.current
    val visible = vp.visibleDocRect()
    val scale = vp.scale
    Box(modifier.fillMaxSize()) {
        for (slot in vp.layout.slots) {
            if (!slot.rect.intersects(visible)) continue
            val originX = vp.offset.x + slot.x * scale
            val originY = vp.offset.y + slot.y * scale
            for (o in slot.page.objects) {
                val ox = (originX + o.rect.left * scale).roundToInt()
                val oy = (originY + o.rect.top * scale).roundToInt()
                val w = with(density) { (o.rect.width * scale).toDp() }
                val h = with(density) { (o.rect.height * scale).toDp() }
                when (o) {
                    is PageObject.TextBox -> {
                        val editing = state.editingTextId == o.id
                        val fontPx = o.fontSize * scale
                        val style = TextStyle(color = (if (c.dark && o.color == 0xFF1A1A1A.toInt()) c.ink else o.color.toComposeColor()), fontSize = with(density) { fontPx.toSp() }, fontFamily = if (o.mono) FontFamily.Monospace else FontFamily.Default)
                        if (editing) {
                            var text by remember(o.id) { mutableStateOf(o.text) }
                            val fr = remember { FocusRequester() }
                            LaunchedEffect(o.id) { fr.requestFocus() }
                            Box(Modifier.offset { IntOffset(ox, oy) }.size(w, h).border(1.dp, c.red).background(c.paper.copy(alpha = 0.6f)).padding(2.dp)) {
                                BasicTextField(
                                    value = text, onValueChange = { text = it }, textStyle = style, cursorBrush = SolidColor(c.red),
                                    modifier = Modifier.fillMaxSize().focusRequester(fr).onFocusChanged { f ->
                                        if (!f.isFocused && state.editingTextId == o.id) {
                                            ctrl.updateObject(o, o.copy(text = text), slot.page.id)
                                            state.editingTextId = null
                                        }
                                    },
                                )
                            }
                        } else {
                            Box(Modifier.offset { IntOffset(ox, oy) }.size(w, h).padding(2.dp)) {
                                Text(o.text.ifEmpty { if (state.tool == Tool.Text) "텍스트" else "" }, style = style.copy(color = if (o.text.isEmpty()) c.mutedText else style.color))
                            }
                        }
                    }
                    else -> {}
                }
                if (state.selectedObjectId == o.id) {
                    val bx = ox; val by = (oy + with(density) { h.toPx() }).roundToInt() + 4
                    Row(Modifier.offset { IntOffset(bx, by) }.background(c.chromeRaised, PlatenTheme_panel()).border(1.dp, c.rule, PlatenTheme_panel())) {
                        if (o is PageObject.Table) {
                            ToolIconButton(PlatenIcons.RowAdd, onClick = { state.tableResize(o, 1, 0, slot.page.id) })
                            ToolIconButton(PlatenIcons.RowRemove, enabled = o.rows > 1, onClick = { state.tableResize(o, -1, 0, slot.page.id) })
                            ToolIconButton(PlatenIcons.ColAdd, onClick = { state.tableResize(o, 0, 1, slot.page.id) })
                            ToolIconButton(PlatenIcons.ColRemove, enabled = o.cols > 1, onClick = { state.tableResize(o, 0, -1, slot.page.id) })
                        }
                        if (o is PageObject.TextBox) {
                            ToolIconButton(PlatenIcons.Text, label = "텍스트 편집", onClick = { state.editingTextId = o.id })
                        }
                        ToolIconButton(PlatenIcons.Delete, onClick = { state.selectedObjectId = null; state.editingTextId = null; ctrl.removeObject(o, slot.page.id) })
                        ToolIconButton(PlatenIcons.Close, label = "선택 해제", onClick = { state.selectedObjectId = null })
                    }
                }
                if (o is PageObject.Table && state.selectedObjectId == o.id) TableCellEditor(state, o, slot, originX, originY, scale)
            }
        }
        // 올가미 선택 액션 바
        val sel = ctrl.selection
        val selSlot = sel?.let { s -> vp.layout.slots.firstOrNull { it.page.id == s.pageId } }
        if (sel != null && selSlot != null && !sel.isEmpty) {
            val bx = (vp.offset.x + (selSlot.x + sel.bounds.left) * scale).roundToInt()
            val by = (vp.offset.y + (selSlot.y + sel.bounds.bottom) * scale).roundToInt() + 8
            Row(Modifier.offset { IntOffset(bx, by) }.background(c.chromeRaised, PlatenTheme_panel()).border(1.dp, c.rule, PlatenTheme_panel()), verticalAlignment = Alignment.CenterVertically) {
                ToolIconButton(PlatenIcons.Duplicate, onClick = { ctrl.duplicateSelection() })
                ToolIconButton(PlatenIcons.Delete, label = "선택 삭제", onClick = { ctrl.deleteSelection() })
                ToolIconButton(PlatenIcons.Close, label = "선택 해제", onClick = { ctrl.clearSelection() })
            }
        }
    }
}

private fun PlatenTheme_panel() = io.github.bigrirason_debug.platen.ui.theme.PlatenShapes.panel

/** 선택된 표의 셀 탭 → 작은 입력창. 셀 좌표는 표 rect + 누적 폭/높이. */
@Composable
private fun TableCellEditor(state: CanvasState, t: PageObject.Table, slot: PageSlot, originX: Float, originY: Float, scale: Float) {
    val c = PlatenTheme.colors
    val density = LocalDensity.current
    val editing = state.editingCell?.takeIf { it.first == t.id }
    if (editing == null) {
        // 셀 탭 타깃(투명): 선택된 표에만 올라가므로 필기 방해 최소
        var cy = t.rect.top
        for (r in 0 until t.rows) {
            var cx = t.rect.left
            for (col in 0 until t.cols) {
                val cw = t.colWidths.getOrElse(col) { 80f }; val rh = t.rowHeights.getOrElse(r) { 32f }
                val key = "$r,$col"
                Box(
                    Modifier.offset { IntOffset((originX + cx * scale).roundToInt(), (originY + cy * scale).roundToInt()) }
                        .size(with(density) { (cw * scale).toDp() }, with(density) { (rh * scale).toDp() })
                        .clickable { state.editingCell = t.id to key },
                )
                cx += cw
            }
            cy += t.rowHeights.getOrElse(r) { 32f }
        }
    } else {
        val (r, col) = editing.second.split(',').map { it.toInt() }
        val cx = t.rect.left + t.colWidths.take(col).sum(); val cy = t.rect.top + t.rowHeights.take(r).sum()
        val cw = t.colWidths.getOrElse(col) { 80f }; val rh = t.rowHeights.getOrElse(r) { 32f }
        var text by remember(editing) { mutableStateOf(t.cells[editing.second] ?: "") }
        val fr = remember { FocusRequester() }
        LaunchedEffect(editing) { fr.requestFocus() }
        Box(
            Modifier.offset { IntOffset((originX + cx * scale).roundToInt(), (originY + cy * scale).roundToInt()) }
                .size(with(density) { (cw * scale).toDp() }, with(density) { (rh * scale).toDp() })
                .background(c.paper).border(1.dp, c.red).padding(2.dp),
        ) {
            BasicTextField(
                value = text, onValueChange = { text = it }, cursorBrush = SolidColor(c.red),
                textStyle = TextStyle(color = c.ink, fontSize = with(density) { (10f * scale).toSp() }),
                modifier = Modifier.fillMaxSize().focusRequester(fr).onFocusChanged { f ->
                    if (!f.isFocused && state.editingCell == editing) {
                        val cells = t.cells.toMutableMap().apply { if (text.isEmpty()) remove(editing.second) else put(editing.second, text) }
                        state.controller.updateObject(t, t.copy(cells = cells), slot.page.id)
                        state.editingCell = null
                    }
                },
            )
        }
    }
}

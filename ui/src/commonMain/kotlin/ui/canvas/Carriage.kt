package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.render.InkTheme
import io.github.bigrirason_debug.platen.render.PageRenderer
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenShapes
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme

/**
 * 캐리지 = 페이지 스트립(typewriter-refs §1): 썸네일 가로 스크롤, 좌우 이동, 현재 페이지 빨간 테두리, 번호 모노.
 */
@Composable
fun Carriage(state: CanvasState, pageRenderer: PageRenderer, modifier: Modifier = Modifier) {
    val c = PlatenTheme.colors
    val ctrl = state.controller
    val inkTheme = if (c.dark) InkTheme.Dark else InkTheme.Light
    val listState = rememberLazyListState()
    LaunchedEffect(ctrl.pageIndex) { listState.animateScrollToItem(ctrl.pageIndex) }

    Row(modifier.fillMaxWidth().height(PlatenSpacing.carriageHeight).background(c.chrome), verticalAlignment = Alignment.CenterVertically) {
        ToolIconButton(PlatenIcons.ChevronLeft, enabled = ctrl.pageIndex > 0, onClick = { ctrl.previousPage(); state.viewport.scrollToPage(ctrl.pageIndex) })
        LazyRow(
            state = listState, modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(PlatenSpacing.s), contentPadding = PaddingValues(horizontal = PlatenSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(ctrl.note.pages, key = { _, p -> p.id }) { i, page ->
                val selected = i == ctrl.pageIndex
                val thumbH = 52.dp
                val thumbW = thumbH * (page.size.width / page.size.height)
                Column(
                    Modifier.semantics { contentDescription = "페이지 ${i + 1} / ${ctrl.pageCount}" + if (selected) " (현재)" else "" }
                        .clickable { ctrl.goToPage(i); state.viewport.scrollToPage(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(thumbW, thumbH)
                            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.red else c.rule, PlatenShapes.page)
                            .background(c.paper),
                    ) {
                        Canvas(Modifier.size(thumbW, thumbH)) {
                            val s = size.height / page.size.height
                            withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                                clipRect(0f, 0f, page.size.width, page.size.height) {
                                    with(pageRenderer) { drawPage(page, Rect(0f, 0f, page.size.width, page.size.height), inkTheme) { state.resolveStroke(it) } }
                                }
                            }
                        }
                    }
                    Text("${i + 1}", style = MonoTextStyle, color = if (selected) c.ink else c.mutedText, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        Text("${ctrl.pageIndex + 1}/${ctrl.pageCount}", style = MonoTextStyle, color = c.mutedText, modifier = Modifier.width(44.dp))
        ToolIconButton(PlatenIcons.ChevronRight, enabled = ctrl.pageIndex < ctrl.pageCount - 1, onClick = { ctrl.nextPage(); state.viewport.scrollToPage(ctrl.pageIndex) })
    }
}

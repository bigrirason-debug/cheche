package io.github.bigrirason_debug.platen.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.ui.components.PlatenTooltip
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.platform.PlatformClock
import io.github.bigrirason_debug.platen.ui.repo.NoteSummary
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenShapes
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme

/**
 * 홈 = 노트북 서랍(DESIGN §5): 카드 그리드(제목·수정일·썸네일 자리), 새 노트, 열기(파일), 삭제(확인), 검색(제목), 설정.
 * 스큐어모피즘은 "서랍/카드/프레임" 구조만 — 재질 없음.
 */
@Composable
fun HomeScreen(
    notes: List<NoteSummary>,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onDelete: (String) -> Unit,
    onOpenFile: (() -> Unit)?,
    onSettings: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    val c = PlatenTheme.colors
    var query by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<NoteSummary?>(null) }
    val filtered = remember(notes, query) { if (query.isBlank()) notes else notes.filter { it.title.contains(query, ignoreCase = true) } }

    Column(Modifier.fillMaxSize().background(c.chrome)) {
        Row(Modifier.fillMaxWidth().height(PlatenSpacing.toolbarHeight + 8.dp).padding(horizontal = PlatenSpacing.s), verticalAlignment = Alignment.CenterVertically) {
            Icon(PlatenIcons.Home.vector, contentDescription = null, tint = c.ink, modifier = Modifier.padding(horizontal = PlatenSpacing.s))
            Text("Platen", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Spacer(Modifier.width(PlatenSpacing.l))
            // 검색(제목)
            Row(
                Modifier.weight(1f).height(36.dp).border(1.dp, c.rule, PlatenShapes.button).padding(horizontal = PlatenSpacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(PlatenIcons.Search.vector, contentDescription = PlatenIcons.Search.label, tint = c.muted, modifier = Modifier.width(18.dp))
                Spacer(Modifier.width(PlatenSpacing.s))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("제목 검색", color = c.mutedText, style = MaterialTheme.typography.bodyMedium)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = c.ink, fontSize = MaterialTheme.typography.bodyMedium.fontSize), cursorBrush = SolidColor(c.red),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "제목 검색" },
                    )
                }
                if (query.isNotEmpty()) ToolIconButton(PlatenIcons.Close, label = "검색 지우기", onClick = { query = "" }, modifier = Modifier.width(32.dp))
            }
            Spacer(Modifier.width(PlatenSpacing.s))
            ToolIconButton(PlatenIcons.Add, onClick = onNew)
            if (onOpenFile != null) ToolIconButton(PlatenIcons.Open, onClick = onOpenFile)
            ToolIconButton(PlatenIcons.Diagnostics, onClick = onDiagnostics)
            ToolIconButton(PlatenIcons.Settings, onClick = onSettings)
        }
        // 서랍 프레임: 상단 1px rule
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (notes.isEmpty()) "서랍이 비어 있습니다" else "검색 결과 없음", color = c.mutedText)
                    if (notes.isEmpty()) Text("＋ 로 새 노트를 만드세요", color = c.mutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = PlatenSpacing.xs))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(PlatenSpacing.m),
                horizontalArrangement = Arrangement.spacedBy(PlatenSpacing.m), verticalArrangement = Arrangement.spacedBy(PlatenSpacing.m),
            ) {
                items(filtered, key = { it.id }) { n -> NoteCard(n, onOpen = { onOpen(n.id) }, onDelete = { confirmDelete = n }) }
            }
        }
    }

    confirmDelete?.let { n ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("노트 삭제") },
            text = { Text("\"${n.title}\" (${n.pageCount}쪽)을 삭제합니다. 되돌릴 수 없습니다.") },
            confirmButton = { TextButton(onClick = { onDelete(n.id); confirmDelete = null }) { Text("삭제", color = c.red) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("취소", color = c.ink) } },
            containerColor = c.chromeRaised, shape = PlatenShapes.panel,
        )
    }
}

@Composable
private fun NoteCard(n: NoteSummary, onOpen: () -> Unit, onDelete: () -> Unit) {
    val c = PlatenTheme.colors
    Column(
        Modifier.background(c.chromeRaised, PlatenShapes.panel).border(1.dp, c.rule, PlatenShapes.panel)
            .clickable(onClick = onOpen).padding(PlatenSpacing.s)
            .semantics { contentDescription = "노트 ${n.title}, ${n.pageCount}쪽, 수정 ${PlatformClock.formatDateTime(n.modified)}" },
    ) {
        // 썸네일 자리: 종이 비율 0.773(굿노트 기본 종이), 줄 가이드만 — 실제 썸네일(thumb.png)은 후속
        Box(Modifier.fillMaxWidth().aspectRatio(0.773f).background(c.paper, PlatenShapes.page).border(1.dp, c.rule, PlatenShapes.page)) {
            Canvas(Modifier.fillMaxSize()) {
                val gap = size.height / 14f
                var y = gap * 2
                while (y < size.height - gap / 2) { drawLine(c.rule, Offset(size.width * 0.1f, y), Offset(size.width * 0.9f, y), 1f); y += gap }
                drawLine(c.red.copy(alpha = 0.5f), Offset(size.width * 0.1f, gap), Offset(size.width * 0.1f, size.height - gap), 1f)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = PlatenSpacing.s), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(n.title.ifBlank { "제목 없음" }, color = c.ink, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(PlatformClock.formatDateTime(n.modified) + " · ${n.pageCount}p", style = MonoTextStyle, color = c.mutedText)
            }
            PlatenTooltip(PlatenIcons.Delete.label) {
                ToolIconButton(PlatenIcons.Delete, onClick = onDelete, modifier = Modifier.width(PlatenSpacing.target))
            }
        }
    }
}

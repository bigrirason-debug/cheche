package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.AppSettings
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.nowMillis
import io.github.bigrirason_debug.platen.ui.AppEnvironment
import io.github.bigrirason_debug.platen.ui.diag.DiagnosticsState
import io.github.bigrirason_debug.platen.ui.diag.DiagnosticsOverlay
import io.github.bigrirason_debug.platen.ui.input.GestureActions
import io.github.bigrirason_debug.platen.ui.input.GestureDispatcher
import io.github.bigrirason_debug.platen.ui.platform.PlatformImages
import io.github.bigrirason_debug.platen.ui.screens.PenSheet
import io.github.bigrirason_debug.platen.ui.state.Tool
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcon
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 더보기 시트 항목. */
data class MoreAction(val icon: PlatenIcon, val label: String, val enabled: Boolean = true, val note: String? = null, val onClick: () -> Unit)

/**
 * 캔버스 셸: 페이퍼 베일(상단) + 캔버스 + 오버레이 + 캐리지(하단) + 펜 시트/더보기 시트 + 키보드 단축키 + 자동 저장.
 */
@Composable
fun CanvasScreen(
    env: AppEnvironment,
    state: CanvasState,
    settings: AppSettings,
    diag: DiagnosticsState,
    mouseDraw: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenImport: () -> Unit,
    onSlotsChanged: (List<io.github.bigrirason_debug.platen.ui.state.PenSlot>, Int) -> Unit,
    onToggleFingerDraw: () -> Unit,
) {
    val c = PlatenTheme.colors
    val ctrl = state.controller
    val scope = rememberCoroutineScope()
    var penSheet by remember { mutableStateOf(false) }
    var moreSheet by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    state.settings = settings
    LaunchedEffect(settings.ink) { state.invalidateParams() }
    LaunchedEffect(state.slots, state.selectedSlot) { onSlotsChanged(state.slots, state.selectedSlot) }

    val actions = remember(state) {
        object : GestureActions {
            override fun undo() { ctrl.undo() }
            override fun redo() { ctrl.redo() }
            override fun selectEraser() = state.selectTool(Tool.Eraser)
            override fun selectLasso() = state.selectTool(Tool.Lasso)
            override fun previousTool() = state.previousTool()
            override fun nextPage() { ctrl.nextPage(); state.viewport.scrollToPage(ctrl.pageIndex) }
            override fun previousPage() { ctrl.previousPage(); state.viewport.scrollToPage(ctrl.pageIndex) }
            override fun selectLaser() = state.selectTool(Tool.Laser)
            override fun toggleFingerDraw() = onToggleFingerDraw()
            override fun ribbonToggle() = state.ribbonToggle()
        }
    }
    val dispatcher = remember(state) { GestureDispatcher(settings.gestures, actions) }
    dispatcher.map = settings.gestures

    // 자동 저장: 변경 후 800ms 디바운스, 세대 번호는 메인에서 스냅샷(세대 가드).
    LaunchedEffect(ctrl.dirtyGeneration) {
        if (ctrl.dirtyGeneration == 0L) return@LaunchedEffect
        val gen = ctrl.dirtyGeneration
        val note = ctrl.note
        delay(800)
        env.notes.save(note, gen, state.attachments())
    }
    // 레이저 페이드 + 프레임 통계 루프(레이저 페이드는 reducedMotion 에서도 유지 — 기능)
    LaunchedEffect(state.laser.isNotEmpty(), diag.overlayVisible) {
        while (state.laser.isNotEmpty() || diag.overlayVisible) {
            withFrameNanos { n ->
                diag.frames.frame(n)
                val now = nowMillis()
                state.laserNow = now
                state.laserPrune(now, settings.laserFadeMillis)
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun pickImage(slot: PageSlot, p: Pt) {
        scope.launch {
            val f = env.files.pickFile("이미지 선택", listOf("png", "jpg", "jpeg", "webp")) ?: return@launch
            val bmp = PlatformImages.decode(f.bytes)
            val aspect = if (bmp != null && bmp.height > 0) bmp.width.toFloat() / bmp.height else 1f
            state.insertImage(slot, p, f.mime, f.bytes, aspect)
        }
    }

    Column(
        Modifier.fillMaxSize().background(c.chrome).focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (state.editingTextId != null || state.editingCell != null) return@onPreviewKeyEvent false
                val ctrlDown = e.isCtrlPressed
                when {
                    ctrlDown && e.key == Key.Z -> ctrl.undo()
                    ctrlDown && e.key == Key.Y -> ctrl.redo()
                    ctrlDown && (e.key == Key.Equals || e.key == Key.Plus || e.key == Key.NumPadAdd) -> state.viewport.zoomBy(1.2f, androidx.compose.ui.geometry.Offset(state.viewport.size.width / 2f, state.viewport.size.height / 2f))
                    ctrlDown && (e.key == Key.Minus || e.key == Key.NumPadSubtract) -> state.viewport.zoomBy(1 / 1.2f, androidx.compose.ui.geometry.Offset(state.viewport.size.width / 2f, state.viewport.size.height / 2f))
                    ctrlDown && e.key == Key.Zero -> state.viewport.resetZoom()
                    ctrlDown && e.key == Key.D -> ctrl.duplicateSelection()
                    e.key == Key.Delete || e.key == Key.Backspace -> {
                        if (ctrl.selection != null) ctrl.deleteSelection()
                        else state.selectedObjectId?.let { id -> ctrl.page.objects.firstOrNull { it.id == id }?.let { ctrl.removeObject(it); state.selectedObjectId = null } } ?: return@onPreviewKeyEvent false
                    }
                    e.key == Key.PageDown -> { ctrl.nextPage(); state.viewport.scrollToPage(ctrl.pageIndex) }
                    e.key == Key.PageUp -> { ctrl.previousPage(); state.viewport.scrollToPage(ctrl.pageIndex) }
                    e.key == Key.Escape -> when {
                        state.paperEditMode -> state.paperEditMode = false
                        ctrl.selection != null -> ctrl.clearSelection()
                        state.selectedObjectId != null -> state.selectedObjectId = null
                        else -> onBack()
                    }
                    e.key == Key.E -> state.selectTool(Tool.Eraser)
                    e.key == Key.L -> state.selectTool(Tool.Lasso)
                    e.key == Key.T -> state.selectTool(Tool.Text)
                    e.key == Key.I -> state.selectTool(Tool.Image)
                    e.key == Key.B -> state.selectTool(Tool.Table)
                    e.key == Key.R -> state.selectTool(Tool.Laser)
                    e.key == Key.X -> state.ribbonToggle()
                    e.key in listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five) -> {
                        val idx = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five).indexOf(e.key)
                        if (idx < state.slots.size) { if (state.tapSlot(idx)) penSheet = true }
                    }
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    ) {
        PaperBail(state, onBack = onBack, onOpenPenSheet = { penSheet = true }, onMore = { moreSheet = true })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PageCanvas(
                state = state, pageRenderer = env.pageRenderer, strokeRenderer = env.strokeRenderer, dispatcher = dispatcher, diag = diag,
                mouseDraw = mouseDraw, onPickImage = ::pickImage,
                onTapObject = { o -> state.selectedObjectId = o?.id; if (o is PageObject.TextBox && state.tool == Tool.Text) state.editingTextId = o.id },
            )
            ObjectOverlays(state)
            if (diag.overlayVisible) DiagnosticsOverlay(diag, Modifier.align(Alignment.TopEnd).padding(PlatenSpacing.s))
            message?.let { m ->
                Text(m, color = c.ink, modifier = Modifier.align(Alignment.BottomCenter).padding(PlatenSpacing.m).background(c.chromeRaised).padding(PlatenSpacing.s))
                LaunchedEffect(m) { delay(2500); message = null }
            }
        }
        Carriage(state, env.pageRenderer)
    }

    if (penSheet) {
        val slot = state.slot
        if (slot != null) {
            PenSheet(
                slot = slot, brushes = env.brushes, strokeRenderer = env.strokeRenderer, inkSettings = settings.ink,
                onChange = { state.updateSlot(state.selectedSlot) { _ -> it } },
                onDismiss = { penSheet = false },
            )
        } else penSheet = false
    }

    if (moreSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val syncState = env.sync.state.value
        val items = listOf(
            MoreAction(PlatenIcons.AddPage, "페이지 추가") { ctrl.addPageAfterCurrent(); state.viewport.scrollToPage(ctrl.pageIndex) },
            MoreAction(PlatenIcons.Delete, "이 페이지 삭제", enabled = ctrl.pageCount > 1) { ctrl.removeCurrentPage() },
            MoreAction(PlatenIcons.Paper, "속지(여백·줄 간격)") { state.paperEditMode = true },
            MoreAction(PlatenIcons.Save, "저장 (.platen)") {
                scope.launch {
                    env.notes.save(ctrl.note, ctrl.dirtyGeneration, state.attachments()); env.notes.flush()
                    val bytes = env.notes.exportBytes(ctrl.note.id)
                    if (bytes != null && env.files.saveFile("저장", ctrl.note.title.ifBlank { "note" } + ".platen", "application/vnd.platen.note+zip", bytes)) message = "저장됨" else message = "저장 취소"
                }
            },
            MoreAction(PlatenIcons.Export, "PDF 내보내기", enabled = env.pdf.available, note = if (env.pdf.available) null else "미연결") {
                scope.launch {
                    val bytes = env.pdf.export(ctrl.note, state.attachments())
                    if (bytes != null) env.files.saveFile("PDF 내보내기", ctrl.note.title + ".pdf", "application/pdf", bytes)
                }
            },
            MoreAction(PlatenIcons.Sync, "동기화", enabled = syncState.available, note = if (syncState.available) null else "미연결") { scope.launch { env.sync.syncNow() } },
            MoreAction(PlatenIcons.Import, "브러시 가져오기", onClick = onOpenImport),
            MoreAction(PlatenIcons.Diagnostics, if (diag.overlayVisible) "진단 오버레이 끄기" else "진단 오버레이 켜기") { diag.overlayVisible = !diag.overlayVisible },
            MoreAction(PlatenIcons.Diagnostics, "진단 화면", onClick = onOpenDiagnostics),
            MoreAction(PlatenIcons.Settings, "설정", onClick = onOpenSettings),
        )
        ModalBottomSheet(onDismissRequest = { moreSheet = false }, sheetState = sheetState, containerColor = c.chromeRaised, shape = io.github.bigrirason_debug.platen.ui.theme.PlatenShapes.panel) {
            Column(Modifier.padding(bottom = PlatenSpacing.l)) {
                for (it in items) {
                    ListItem(
                        headlineContent = { Text(it.label, color = if (it.enabled) c.ink else c.mutedText) },
                        supportingContent = it.note?.let { n -> { Text(n, color = c.mutedText) } },
                        leadingContent = { Icon(it.icon.vector, contentDescription = it.icon.label, tint = if (it.enabled) c.ink else c.muted) },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = c.chromeRaised),
                        modifier = Modifier.clickable(enabled = it.enabled) { moreSheet = false; it.onClick() },
                    )
                }
            }
        }
    }
}

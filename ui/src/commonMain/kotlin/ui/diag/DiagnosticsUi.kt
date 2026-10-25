package io.github.bigrirason_debug.platen.ui.diag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.model.nowMillis
import io.github.bigrirason_debug.platen.render.InkTheme
import io.github.bigrirason_debug.platen.render.PageRenderer
import io.github.bigrirason_debug.platen.ui.AppEnvironment
import io.github.bigrirason_debug.platen.ui.canvas.CanvasState
import io.github.bigrirason_debug.platen.ui.components.SectionTitle
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.platform.PlatformImages
import io.github.bigrirason_debug.platen.ui.screens.fmt
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenPalette
import io.github.bigrirason_debug.platen.ui.theme.PlatenShapes
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import kotlinx.coroutines.launch

/** 캔버스 우상단 입력/프레임 통계(모노 11sp, 크롬 80%). */
@Composable
fun DiagnosticsOverlay(diag: DiagnosticsState, modifier: Modifier = Modifier) {
    val c = PlatenTheme.colors
    val s = diag.input
    Column(modifier.background(c.chrome.copy(alpha = 0.8f), PlatenShapes.button).padding(PlatenSpacing.s)) {
        for (line in listOf(
            "in  ${s.hz.toInt()} Hz  n=${s.samplesTotal}  hist=${s.historySamples}",
            "p   ${s.distinctPressure} distinct  tilt=${s.tiltSeen}  hover=${s.hoverSeen}",
            "btn ${s.buttonSeen}  cancel=${s.cancelCount}  type=${s.lastPointerType}",
            "fr  p50 ${"%.1f".fmt(diag.frames.p50)}  p95 ${"%.1f".fmt(diag.frames.p95)} ms  drop=${diag.frames.dropped}",
        )) Text(line, style = MonoTextStyle, color = c.ink)
    }
}

/** 페이지 1장을 오프스크린 렌더(배율 scale px/pt) → PNG. 렌더 일치 검증용(Android↔Windows diff). */
fun renderPagePng(page: Page, renderer: PageRenderer, theme: InkTheme, scale: Float = 2f, resolve: (Stroke) -> io.github.bigrirason_debug.platen.ink.ResolvedInkParams): ByteArray {
    val w = (page.size.width * scale).toInt().coerceAtLeast(1); val h = (page.size.height * scale).toInt().coerceAtLeast(1)
    val bmp = ImageBitmap(w, h)
    val canvas = Canvas(bmp)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
        scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            with(renderer) { drawPage(page, Rect(0f, 0f, page.size.width, page.size.height), theme, resolve) }
        }
    }
    return PlatformImages.encodePng(bmp)
}

/**
 * 진단 화면(D-020 ④): 입력 통계, 프레임 p50/p95, 합성 1만 획 로드(시드 고정, 로드 시간), PNG 덤프, 프론트버퍼/예측 토글(Android 에서만 실효).
 */
@Composable
fun DiagnosticsScreen(
    env: AppEnvironment,
    diag: DiagnosticsState,
    canvas: CanvasState?,
    ink: InkSettings,
    onInkChange: (InkSettings) -> Unit,
    onBack: () -> Unit,
) {
    val c = PlatenTheme.colors
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.chrome)) {
        Row(Modifier.fillMaxWidth().height(PlatenSpacing.toolbarHeight), verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton(PlatenIcons.Back, onClick = onBack)
            Text("진단", style = MaterialTheme.typography.titleLarge, color = c.ink)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PlatenSpacing.l)) {
            SectionTitle("입력")
            DiagnosticsOverlay(diag, Modifier.fillMaxWidth())
            Text("합격선: 유효 샘플 ≥240Hz(히스토리 포함), 필압 고유값 ≥1,024 (PLAN §1.2). 데스크톱(마우스)에서는 측정 의미 없음.", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.padding(top = PlatenSpacing.s)) {
                OutlinedAction("통계 초기화") { diag.input.reset(); diag.frames.reset() }
                Spacer(Modifier.width(PlatenSpacing.s))
                OutlinedAction(if (diag.overlayVisible) "캔버스 오버레이 끄기" else "캔버스 오버레이 켜기") { diag.overlayVisible = !diag.overlayVisible }
            }

            SectionTitle("부하")
            Text(if (canvas == null) "노트를 먼저 열어야 합니다." else "현재 페이지에 합성 획을 넣습니다(undo 1회로 제거). 로드 시간 ≤2s 가 합격(PLAN §12-3).", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.padding(top = PlatenSpacing.s)) {
                OutlinedAction("합성 1만 획 로드", enabled = canvas != null && !busy) {
                    val cs = canvas ?: return@OutlinedAction
                    busy = true
                    val page = cs.controller.page
                    val t0 = nowMillis()
                    val strokes = SyntheticStrokes.generate(10_000, page.size.width, page.size.height, cs.slot?.brushId ?: "builtin-pen-1", PlatenPalette.BLACK)
                    cs.controller.addStrokes(strokes, page.id)
                    diag.lastLoadMillis = nowMillis() - t0
                    diag.lastMessage = "1만 획 로드 ${diag.lastLoadMillis} ms"
                    busy = false
                }
                Spacer(Modifier.width(PlatenSpacing.s))
                OutlinedAction("합성 1천 획", enabled = canvas != null && !busy) {
                    val cs = canvas ?: return@OutlinedAction
                    val page = cs.controller.page
                    val t0 = nowMillis()
                    cs.controller.addStrokes(SyntheticStrokes.generate(1_000, page.size.width, page.size.height, cs.slot?.brushId ?: "builtin-pen-1", PlatenPalette.BLACK, seed = 7L), page.id)
                    diag.lastLoadMillis = nowMillis() - t0
                    diag.lastMessage = "1천 획 로드 ${diag.lastLoadMillis} ms"
                }
            }
            if (diag.lastLoadMillis >= 0) Text("마지막 로드: ${diag.lastLoadMillis} ms", style = MonoTextStyle, color = c.ink, modifier = Modifier.padding(top = PlatenSpacing.xs))

            SectionTitle("렌더 일치")
            Text("현재 페이지를 2px/pt 로 오프스크린 렌더해 PNG 로 저장합니다(Android↔Windows 픽셀 diff ≤2% 검증 입력).", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
            OutlinedAction("PNG 덤프", enabled = canvas != null && !busy) {
                val cs = canvas ?: return@OutlinedAction
                scope.launch {
                    busy = true
                    val theme = if (c.dark) InkTheme.Dark else InkTheme.Light
                    val png = runCatching { renderPagePng(cs.controller.page, env.pageRenderer, theme) { cs.resolveStroke(it) } }.getOrNull()
                    diag.lastMessage = if (png == null) "렌더 실패" else if (env.files.saveFile("PNG 덤프", "platen-page-${cs.controller.pageIndex + 1}.png", "image/png", png)) "PNG 저장됨 (${png.size} B)" else "저장 취소"
                    busy = false
                }
            }

            SectionTitle("저지연 경로 (Android 에서만 실효)")
            ToggleRow("프론트버퍼 렌더링", ink.frontBuffer) { onInkChange(ink.copy(frontBuffer = it)) }
            ToggleRow("모션 예측", ink.motionPrediction) { onInkChange(ink.copy(motionPrediction = it)) }
            Text("프론트버퍼: Android 는 graphics-core 젖은 잉크 오버레이(형광펜·반투명 제외). 켜고/끄고 지연을 비교하세요(LOCAL_TODO S1).", color = c.mutedText, style = MaterialTheme.typography.bodySmall)

            if (diag.lastMessage.isNotEmpty()) Text(diag.lastMessage, style = MonoTextStyle, color = c.red, modifier = Modifier.padding(vertical = PlatenSpacing.m))
            Text(PlatformImages.platformName, style = MonoTextStyle, color = c.mutedText, modifier = Modifier.padding(bottom = PlatenSpacing.xl))
        }
    }
}

@Composable
fun OutlinedAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = PlatenTheme.colors
    Button(onClick = onClick, enabled = enabled, shape = PlatenShapes.button, colors = ButtonDefaults.buttonColors(containerColor = c.ink, contentColor = c.paper, disabledContainerColor = c.muted, disabledContentColor = c.chrome)) { Text(label) }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, description: String? = null, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = PlatenTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = PlatenSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) c.ink else c.mutedText)
            if (description != null) Text(description, color = c.mutedText, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

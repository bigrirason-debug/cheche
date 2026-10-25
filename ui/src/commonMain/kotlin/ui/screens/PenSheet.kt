package io.github.bigrirason_debug.platen.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.CurvePoint
import io.github.bigrirason_debug.platen.model.ExposedParam
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.model.StrokeOverrides
import io.github.bigrirason_debug.platen.render.InkTheme
import io.github.bigrirason_debug.platen.render.StrokeRenderer
import io.github.bigrirason_debug.platen.ui.canvas.iconForRole
import io.github.bigrirason_debug.platen.ui.components.SectionTitle
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.repo.BrushRepository
import io.github.bigrirason_debug.platen.ui.state.PenSlot
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenPalette
import io.github.bigrirason_debug.platen.ui.theme.PlatenShapes
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import io.github.bigrirason_debug.platen.ui.theme.toComposeColor
import kotlin.math.sin

/** 미리보기 획(결정적): 사인 곡선 + 필압 0.2→1→0.3. */
fun previewSamples(width: Float = 220f, height: Float = 40f): List<InputSample> = List(60) { i ->
    val t = i / 59f
    InputSample(x = 10f + t * width, y = height / 2f + sin(t * 6.283f * 1.5f) * height * 0.35f, pressure = (0.2f + 0.8f * sin(t * 3.1416f)).coerceIn(0.05f, 1f), t = i * 8f)
}

/**
 * 펜 설정 시트(선택 슬롯 재탭): 브러시 선택 · 굵기(spec.size.min~max) · 색(리본 토글 + 팔레트 8) ·
 * spec.exposed 에 따른 슬라이더(브러시별 오버라이드는 여기에만, PLAN §12-9) · 필압 커브 미니 에디터. 값 출처 배지(브러시/오버라이드).
 */
@Composable
fun PenSheet(
    slot: PenSlot,
    brushes: BrushRepository,
    strokeRenderer: StrokeRenderer,
    inkSettings: InkSettings,
    onChange: (PenSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PlatenTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.chromeRaised, shape = PlatenShapes.panel) {
        PenSheetContent(slot, brushes, strokeRenderer, inkSettings, onChange)
    }
}

/** 시트 본문(목업 렌더·테스트에서 ModalBottomSheet 없이 쓸 수 있게 분리). */
@Composable
fun PenSheetContent(
    slot: PenSlot,
    brushes: BrushRepository,
    strokeRenderer: StrokeRenderer,
    inkSettings: InkSettings,
    onChange: (PenSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PlatenTheme.colors
    val roster by brushes.brushes.collectAsState()
    val spec = brushes.get(slot.brushId) ?: roster.firstOrNull() ?: return
    val ov = slot.overrides ?: StrokeOverrides()
    fun setOv(f: (StrokeOverrides) -> StrokeOverrides) = onChange(slot.copy(overrides = f(ov)))

    run {
        Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = PlatenSpacing.l).padding(bottom = PlatenSpacing.xl)) {
            // 미리보기
            StrokePreview(spec, slot, inkSettings, strokeRenderer, Modifier.fillMaxWidth().height(64.dp))

            SectionTitle("브러시")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PlatenSpacing.s)) {
                items(roster, key = { it.id }) { b ->
                    val sel = b.id == spec.id
                    Row(
                        Modifier.border(if (sel) 1.5.dp else 1.dp, if (sel) c.red else c.rule, PlatenShapes.button)
                            .clickable { onChange(slot.copy(brushId = b.id, size = slot.size.coerceIn(b.size.min, b.size.max), overrides = null)) }
                            .padding(horizontal = PlatenSpacing.s, vertical = PlatenSpacing.xs)
                            .semantics { contentDescription = "브러시 ${b.name}" + if (sel) " 선택됨" else "" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(iconForRole(b.role).vector, contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(PlatenSpacing.xs))
                        Text(b.name, color = c.ink, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            LabeledSlider("굵기", slot.size, spec.size.min..spec.size.max, format = { "%.1f pt".fmt(it) }) { onChange(slot.copy(size = it)) }

            SectionTitle("색")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PlatenSpacing.s)) {
                ToolIconButton(PlatenIcons.Ribbon, onClick = { onChange(slot.copy(color = PlatenPalette.ribbonToggle(slot.color))) })
                for (col in PlatenPalette.all) {
                    val sel = col == slot.color
                    Box(
                        Modifier.size(32.dp).border(if (sel) 2.dp else 1.dp, if (sel) c.red else c.rule, PlatenShapes.button).padding(4.dp)
                            .background(col.toComposeColor(), PlatenShapes.button)
                            .clickable { onChange(slot.copy(color = col)) }
                            .semantics { contentDescription = "색 " + colorName(col) + if (sel) " 선택됨" else "" },
                    )
                }
            }

            SectionTitle("브러시 파라미터")
            Text("이 시트의 값은 이 슬롯에만 적용됩니다(전역 기본은 설정 › 필기).", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
            for (p in spec.exposed) {
                when (p) {
                    ExposedParam.size -> {}
                    ExposedParam.opacity -> LabeledSlider("불투명도", slot.opacity, 0.05f..1f, format = { "${(it * 100).toInt()}%" }) { onChange(slot.copy(opacity = it)) }
                    ExposedParam.pressureSize -> LabeledSlider("필압 민감도", ov.pressureAmount ?: spec.pressureSize.amount, 0f..1f, badge = if (ov.pressureAmount != null) "오버라이드" else "브러시", format = { "%.2f".fmt(it) }) { v -> setOv { it.copy(pressureAmount = v) } }
                    ExposedParam.streamline -> LabeledSlider("스트림라인", ov.streamline ?: spec.smoothing.streamline, 0f..1f, badge = if (ov.streamline != null) "오버라이드" else "브러시", format = { "%.2f".fmt(it) }) { v -> setOv { it.copy(streamline = v) } }
                    ExposedParam.stabilize -> LabeledSlider("안정화", ov.stabilize ?: spec.smoothing.stabilize, 0f..1f, badge = if (ov.stabilize != null) "오버라이드" else "브러시", format = { "%.2f".fmt(it) }) { v -> setOv { it.copy(stabilize = v) } }
                    ExposedParam.taperEnd -> LabeledSlider("끝 테이퍼", ov.taperEnd ?: spec.taper.endLength, 0f..60f, badge = if (ov.taperEnd != null) "오버라이드" else "브러시", format = { "${it.toInt()} pt" }) { v -> setOv { it.copy(taperEnd = v) } }
                    ExposedParam.pressureOpacity -> ReadOnlyParam("필압→불투명도", "%.2f".fmt(spec.pressureOpacity.amount))
                    ExposedParam.velocitySize -> ReadOnlyParam("속도→크기", "%.2f".fmt(spec.velocity.size))
                    ExposedParam.tiltSize -> ReadOnlyParam("틸트→크기", "%.2f".fmt(spec.tilt.size))
                    ExposedParam.grainDepth -> ReadOnlyParam("밀도(그레인)", "%.2f".fmt(spec.grain.depth))
                    ExposedParam.spacing -> ReadOnlyParam("간격", "%.2f".fmt(spec.stamp.spacing))
                }
            }
            if (slot.overrides != null) {
                Text("브러시 기본값으로 되돌리기", color = c.red, modifier = Modifier.padding(top = PlatenSpacing.s).clickable { onChange(slot.copy(overrides = null)) })
            }

            SectionTitle("필압 커브")
            Row(verticalAlignment = Alignment.Top) {
                val curve: List<CurvePoint> = ov.pressureCurve ?: spec.pressureSize.curve
                PressureCurveEditor(curve, onChange = { setOv { o -> o.copy(pressureCurve = it) } })
                Spacer(Modifier.width(PlatenSpacing.m))
                Column {
                    Text(if (ov.pressureCurve != null) "오버라이드" else "브러시", style = MonoTextStyle, color = c.mutedText)
                    Text("제어점 ${curve.size}/8 · 드래그로 이동, 빈 곳 드래그로 추가", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
                    if (ov.pressureCurve != null) Text("브러시 커브로", color = c.red, modifier = Modifier.padding(top = PlatenSpacing.xs).clickable { setOv { it.copy(pressureCurve = null) } })
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyParam(label: String, value: String) {
    val c = PlatenTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = PlatenSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.mutedText, modifier = Modifier.weight(1f))
        Text(value, style = MonoTextStyle, color = c.mutedText)
        Spacer(Modifier.width(PlatenSpacing.s))
        Text("브러시", style = MonoTextStyle, color = c.mutedText)
    }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, badge: String? = null, format: (Float) -> String = { "%.2f".fmt(it) }, enabled: Boolean = true, onChange: (Float) -> Unit) {
    val c = PlatenTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = PlatenSpacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (enabled) c.ink else c.mutedText, modifier = Modifier.weight(1f))
            Text(format(value), style = MonoTextStyle, color = c.mutedText)
            if (badge != null) { Spacer(Modifier.width(PlatenSpacing.s)); Text(badge, style = MonoTextStyle, color = if (badge == "오버라이드") c.red else c.mutedText) }
        }
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
fun StrokePreview(spec: BrushSpec, slot: PenSlot, ink: InkSettings, renderer: StrokeRenderer, modifier: Modifier = Modifier) {
    val c = PlatenTheme.colors
    val theme = if (c.dark) InkTheme.Dark else InkTheme.Light
    val samples = remember { previewSamples() }
    val stroke = remember(slot, spec.id) { Stroke("preview", spec.id, slot.color, slot.size, slot.opacity, slot.overrides, samples) }
    val params = remember(slot, spec, ink) { runCatching { ResolvedInkParams.resolve(spec, slot.size, slot.opacity, ink, slot.overrides) }.getOrNull() }
    Canvas(modifier.background(c.paper).semantics { contentDescription = "브러시 미리보기 ${spec.name}" }) {
        val s = size.width / 240f
        withTransform({ scale(s, s, pivot = Offset.Zero); translate(0f, (size.height / s - 40f) / 2f) }) {
            clipRect(0f, -40f, 240f, 80f) {
                if (params != null) with(renderer) { drawStroke(stroke, params, theme) }
            }
        }
    }
}

fun colorName(argb: Int): String = when (argb) {
    PlatenPalette.BLACK -> "검정"; PlatenPalette.RED -> "빨강"; PlatenPalette.BLUE -> "파랑"; PlatenPalette.GREEN -> "초록"
    PlatenPalette.ORANGE -> "주황"; PlatenPalette.PURPLE -> "보라"; PlatenPalette.GRAY -> "회색"; PlatenPalette.YELLOW -> "형광 노랑"; else -> "사용자 색"
}

/** commonMain 에는 String.format 이 없어 소수 자리 포맷을 직접 만든다. 패턴: "%.1f", "%.2f" */
fun String.fmt(v: Float): String {
    val digits = this.substringAfter('.').substringBefore('f').toIntOrNull() ?: 2
    val suffix = this.substringAfter('f', "")
    var scale = 1
    repeat(digits) { scale *= 10 }
    val r = kotlin.math.round(v * scale) / scale
    val intPart = r.toInt()
    var frac = kotlin.math.abs(r - intPart)
    val sb = StringBuilder()
    if (r < 0 && intPart == 0) sb.append('-')
    sb.append(intPart)
    if (digits > 0) {
        sb.append('.')
        repeat(digits) { frac *= 10; val d = frac.toInt().coerceIn(0, 9); sb.append(d); frac -= d }
    }
    return sb.toString() + suffix
}

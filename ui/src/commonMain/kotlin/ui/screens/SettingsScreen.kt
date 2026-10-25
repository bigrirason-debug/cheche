package io.github.bigrirason_debug.platen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.AppSettings
import io.github.bigrirason_debug.platen.model.GestureAction
import io.github.bigrirason_debug.platen.model.GestureInput
import io.github.bigrirason_debug.platen.model.GestureMap
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.ThemeMode
import io.github.bigrirason_debug.platen.ui.AppEnvironment
import io.github.bigrirason_debug.platen.ui.components.SectionTitle
import io.github.bigrirason_debug.platen.ui.components.ToolIconButton
import io.github.bigrirason_debug.platen.ui.diag.OutlinedAction
import io.github.bigrirason_debug.platen.ui.diag.ToggleRow
import io.github.bigrirason_debug.platen.ui.platform.PlatformImages
import io.github.bigrirason_debug.platen.ui.repo.StoredSettings
import io.github.bigrirason_debug.platen.ui.theme.MonoTextStyle
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenPalette
import io.github.bigrirason_debug.platen.ui.theme.PlatenShapes
import io.github.bigrirason_debug.platen.ui.theme.PlatenSpacing
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import io.github.bigrirason_debug.platen.ui.theme.toComposeColor
import kotlinx.coroutines.launch

fun GestureInput.label(): String = when (this) {
    GestureInput.twoFingerTap -> "두 손가락 탭"; GestureInput.threeFingerTap -> "세 손가락 탭"
    GestureInput.twoFingerDoubleTap -> "두 손가락 더블탭"; GestureInput.threeFingerDoubleTap -> "세 손가락 더블탭"
    GestureInput.twoFingerSwipeLeft -> "두 손가락 ← 스와이프"; GestureInput.twoFingerSwipeRight -> "두 손가락 → 스와이프"
    GestureInput.threeFingerSwipeLeft -> "세 손가락 ← 스와이프"; GestureInput.threeFingerSwipeRight -> "세 손가락 → 스와이프"
    GestureInput.stylusButtonDrag -> "펜 옆버튼 + 긋기"; GestureInput.stylusButtonClick -> "펜 옆버튼 클릭"; GestureInput.stylusButtonHold -> "펜 옆버튼 누른 채"
}

fun GestureAction.label(): String = when (this) {
    GestureAction.none -> "없음"; GestureAction.undo -> "실행 취소"; GestureAction.redo -> "다시 실행"; GestureAction.eraser -> "지우개"
    GestureAction.lasso -> "올가미"; GestureAction.previousTool -> "이전 도구"; GestureAction.nextPage -> "다음 페이지"; GestureAction.previousPage -> "이전 페이지"
    GestureAction.laser -> "레이저"; GestureAction.toggleFingerDraw -> "손가락 필기 전환"; GestureAction.colorBlackRed -> "검정/빨강 전환"
}

/**
 * 설정: 3그룹(필기 / 제스처 / 화면·계정) + 그룹별 "기본값 복원"(PLAN §12-9, COUNCIL 좌석5 #1).
 * 브러시별 오버라이드는 여기 없음(펜 시트 전용). 전역 필기 슬라이더는 3개 + 커브는 "고급" 접기.
 */
@Composable
fun SettingsScreen(env: AppEnvironment, stored: StoredSettings, onChange: (StoredSettings) -> Unit, onBack: () -> Unit, onDiagnostics: () -> Unit) {
    val c = PlatenTheme.colors
    val s = stored.app
    fun app(f: (AppSettings) -> AppSettings) = onChange(stored.copy(app = f(s)))
    fun ink(f: (InkSettings) -> InkSettings) = app { it.copy(ink = f(it.ink)) }
    var group by remember { mutableStateOf(0) }
    var advanced by remember { mutableStateOf(false) }
    val syncState by env.sync.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(c.chrome)) {
        Row(Modifier.fillMaxWidth().height(PlatenSpacing.toolbarHeight), verticalAlignment = Alignment.CenterVertically) {
            ToolIconButton(PlatenIcons.Back, onClick = onBack)
            Text("설정", style = MaterialTheme.typography.titleLarge, color = c.ink)
            Spacer(Modifier.weight(1f))
            SingleChoiceSegmentedButtonRow(Modifier.padding(end = PlatenSpacing.m)) {
                listOf("필기", "제스처", "화면·계정").forEachIndexed { i, label ->
                    SegmentedButton(selected = group == i, onClick = { group = i }, shape = SegmentedButtonDefaults.itemShape(i, 3, baseShape = PlatenShapes.button)) { Text(label) }
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PlatenSpacing.l)) {
            when (group) {
                0 -> {
                    SectionTitle("필기 (전역 기본)")
                    Text("브러시별 조정은 펜꽂이 슬롯을 다시 탭해 여는 펜 설정에서.", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
                    LabeledSlider("스무딩(스트림라인)", s.ink.streamline, 0f..1f) { v -> ink { it.copy(streamline = v) } }
                    LabeledSlider("안정화", s.ink.stabilize, 0f..1f) { v -> ink { it.copy(stabilize = v) } }
                    LabeledSlider("끝 테이퍼 길이", s.ink.taperEndLength, 0f..60f, format = { "${it.toInt()} pt" }) { v -> ink { it.copy(taperEndLength = v) } }
                    Text(if (advanced) "고급 접기 ▴" else "고급: 전역 필압 커브 ▾", color = c.ink, modifier = Modifier.padding(vertical = PlatenSpacing.s).clickable { advanced = !advanced })
                    if (advanced) {
                        Row(verticalAlignment = Alignment.Top) {
                            PressureCurveEditor(s.ink.globalPressureCurve, onChange = { v -> ink { it.copy(globalPressureCurve = v) } })
                            Spacer(Modifier.width(PlatenSpacing.m))
                            Text("전역 커브는 입력 보정으로 브러시 커브와 합성됩니다(brush-formats §3.4).", color = c.mutedText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        }
                    }
                    SectionTitle("입력")
                    ToggleRow("손가락 필기", s.ink.fingerDraw, "끄면 손가락은 팬/줌만") { v -> ink { it.copy(fingerDraw = v) } }
                    ToggleRow("팜 리젝션", s.ink.palmRejection, "펜 호버·접촉 중 손가락 무시") { v -> ink { it.copy(palmRejection = v) } }
                    ToggleRow("모션 예측", s.ink.motionPrediction, "Android 전용. 기본 꺼짐(밀림 체감)") { v -> ink { it.copy(motionPrediction = v) } }
                    ToggleRow("프론트버퍼 렌더링", s.ink.frontBuffer, "Android 젖은 잉크 저지연") { v -> ink { it.copy(frontBuffer = v) } }
                    if (env.desktop) ToggleRow("마우스 필기 허용 (진단용)", stored.ui.mouseDraw, "기본 꺼짐 — Windows 는 올가미·이동·편집만") { v -> onChange(stored.copy(ui = stored.ui.copy(mouseDraw = v))) }
                    LabeledSlider("지우개 반지름", stored.ui.eraserRadiusPt, 2f..24f, format = { "${it.toInt()} pt" }) { v -> onChange(stored.copy(ui = stored.ui.copy(eraserRadiusPt = v))) }
                    Spacer(Modifier.height(PlatenSpacing.m))
                    OutlinedAction("필기 기본값 복원") { app { it.copy(ink = InkSettings()) } }
                }
                1 -> {
                    SectionTitle("제스처 매핑")
                    Text("입력 → 동작. 손가락 제스처 + S펜 옆버튼(D-012). 데스크톱에서는 마우스 우드래그 = 옆버튼+긋기.", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
                    for (input in GestureInput.entries) {
                        GestureRow(input, s.gestures.actionFor(input)) { a -> app { it.copy(gestures = GestureMap(it.gestures.map + (input to a))) } }
                    }
                    Spacer(Modifier.height(PlatenSpacing.m))
                    OutlinedAction("제스처 기본값 복원") { app { it.copy(gestures = GestureMap()) } }
                }
                else -> {
                    SectionTitle("화면")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("테마", color = c.ink, modifier = Modifier.weight(1f))
                        SingleChoiceSegmentedButtonRow {
                            ThemeMode.entries.forEachIndexed { i, m ->
                                SegmentedButton(selected = s.theme == m, onClick = { app { it.copy(theme = m) } }, shape = SegmentedButtonDefaults.itemShape(i, 3, baseShape = PlatenShapes.button)) {
                                    Text(when (m) { ThemeMode.system -> "시스템"; ThemeMode.light -> "라이트"; ThemeMode.dark -> "다크" })
                                }
                            }
                        }
                    }
                    ToggleRow("감속 모션", s.reducedMotion, "장식 모션 0(레이저 페이드는 유지)") { v -> app { it.copy(reducedMotion = v) } }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = PlatenSpacing.s)) {
                        Text("UI 밀도", color = c.ink, modifier = Modifier.weight(1f))
                        SingleChoiceSegmentedButtonRow {
                            listOf(0.85f to "촘촘(40dp)", 1f to "기본(48dp)", 1.15f to "넓게").forEachIndexed { i, (d, label) ->
                                SegmentedButton(selected = kotlin.math.abs(s.uiDensity - d) < 0.01f, onClick = { app { it.copy(uiDensity = d) } }, shape = SegmentedButtonDefaults.itemShape(i, 3, baseShape = PlatenShapes.button)) { Text(label) }
                            }
                        }
                    }
                    SectionTitle("레이저 포인터")
                    Row(horizontalArrangement = Arrangement.spacedBy(PlatenSpacing.s)) {
                        for (col in PlatenPalette.all) {
                            val sel = col == s.laserColor
                            Box(Modifier.size(32.dp).border(if (sel) 2.dp else 1.dp, if (sel) c.red else c.rule, PlatenShapes.button).padding(4.dp).background(col.toComposeColor(), PlatenShapes.button).clickable { app { it.copy(laserColor = col) } }.semantics { contentDescription = "레이저 색 " + colorName(col) })
                        }
                    }
                    LabeledSlider("지속 시간", s.laserFadeMillis.toFloat(), 200f..4000f, format = { "${it.toInt()} ms" }) { v -> app { it.copy(laserFadeMillis = v.toInt()) } }
                    LabeledSlider("굵기", s.laserWidth, 1f..12f, format = { "${it.toInt()} px" }) { v -> app { it.copy(laserWidth = v) } }

                    SectionTitle("계정")
                    Text(syncState.message ?: (if (syncState.signedIn) "로그인: ${syncState.accountLabel}" else "로그아웃 상태"), color = c.mutedText, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.padding(top = PlatenSpacing.s)) {
                        OutlinedAction(if (syncState.signedIn) "로그아웃" else "Google 로그인", enabled = syncState.available && !syncState.busy) { scope.launch { if (syncState.signedIn) env.sync.signOut() else env.sync.signIn() } }
                        Spacer(Modifier.width(PlatenSpacing.s))
                        OutlinedAction("지금 동기화", enabled = syncState.available && syncState.signedIn && !syncState.busy) { scope.launch { env.sync.syncNow() } }
                    }

                    SectionTitle("정보")
                    Text("Platen 0.1.0 · ${PlatformImages.platformName}", style = MonoTextStyle, color = c.mutedText)
                    Text("디자인 B안(DESIGN.md v1) · 서체: 시스템 산세리프 + 모노(숫자·눈금·단축키)", color = c.mutedText, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.padding(vertical = PlatenSpacing.m)) {
                        OutlinedAction("진단 화면", onClick = onDiagnostics)
                        Spacer(Modifier.width(PlatenSpacing.s))
                        OutlinedAction("화면 기본값 복원") { app { it.copy(theme = ThemeMode.system, reducedMotion = false, uiDensity = 1f, laserColor = AppSettings().laserColor, laserFadeMillis = 1000, laserWidth = 4f) } }
                    }
                }
            }
            Spacer(Modifier.height(PlatenSpacing.xxl))
        }
    }
}

@Composable
private fun GestureRow(input: GestureInput, action: GestureAction, onChange: (GestureAction) -> Unit) {
    val c = PlatenTheme.colors
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = PlatenSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(input.label(), color = c.ink, modifier = Modifier.weight(1f))
        Box {
            Text(
                action.label(), color = if (action == GestureAction.none) c.mutedText else c.ink,
                modifier = Modifier.border(1.dp, c.rule, PlatenShapes.button).clickable { open = true }.padding(horizontal = PlatenSpacing.m, vertical = PlatenSpacing.s)
                    .semantics { contentDescription = "${input.label()} 동작: ${action.label()}" },
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.chromeRaised) {
                for (a in GestureAction.entries) DropdownMenuItem(text = { Text(a.label(), color = c.ink) }, onClick = { onChange(a); open = false })
            }
        }
    }
}

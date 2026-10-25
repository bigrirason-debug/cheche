package io.github.bigrirason_debug.platen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.bigrirason_debug.platen.ui.canvas.CanvasScreen
import io.github.bigrirason_debug.platen.ui.canvas.CanvasState
import io.github.bigrirason_debug.platen.ui.canvas.Viewport
import io.github.bigrirason_debug.platen.ui.diag.DiagnosticsScreen
import io.github.bigrirason_debug.platen.ui.diag.DiagnosticsState
import io.github.bigrirason_debug.platen.ui.repo.withDefaultSlots
import io.github.bigrirason_debug.platen.ui.screens.BrushImportScreen
import io.github.bigrirason_debug.platen.ui.screens.HomeScreen
import io.github.bigrirason_debug.platen.ui.screens.SettingsScreen
import io.github.bigrirason_debug.platen.ui.state.EditorController
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class Canvas(val noteId: String) : Screen
    data object Settings : Screen
    data object Diagnostics : Screen
    data object BrushImport : Screen
}

/** 단순 백스택. 플랫폼(Android 뒤로가기)은 back() 을 호출한다. */
class Navigator(start: Screen = Screen.Home) {
    val stack = mutableStateListOf<Screen>(start)
    val current: Screen get() = stack.last()
    fun push(s: Screen) { if (current != s) stack.add(s) }
    fun back(): Boolean { if (stack.size <= 1) return false; stack.removeAt(stack.lastIndex); return true }
    fun home() { while (stack.size > 1) stack.removeAt(stack.lastIndex) }
}

/** 앱 루트: 설정 → 테마, 화면 전환, 노트 로드/저장. */
@Composable
fun PlatenApp(env: AppEnvironment, nav: Navigator = remember { Navigator() }, diag: DiagnosticsState = remember { DiagnosticsState() }) {
    val stored by env.settings.settings.collectAsState()
    val notes by env.notes.notes.collectAsState()
    val scope = rememberCoroutineScope()
    var canvasState by remember { mutableStateOf<CanvasState?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { env.notes.refresh() }
    LaunchedEffect(stored.ui.slots.isEmpty()) { if (stored.ui.slots.isEmpty()) env.settings.update { it.copy(ui = it.ui.withDefaultSlots()) } }

    PlatenTheme(mode = stored.app.theme, reducedMotion = stored.app.reducedMotion, uiDensity = if (env.desktop && stored.app.uiDensity == 1f) 0.85f else stored.app.uiDensity) {
        when (val s = nav.current) {
            Screen.Home -> HomeScreen(
                notes = notes,
                onOpen = { nav.push(Screen.Canvas(it)) },
                onNew = { scope.launch { val n = env.notes.create("새 노트 ${notes.size + 1}"); nav.push(Screen.Canvas(n.id)) } },
                onDelete = { id -> scope.launch { env.notes.delete(id) } },
                onOpenFile = if (env.desktop) ({ scope.launch { env.files.pickFile("노트 열기", listOf("platen"))?.let { f -> env.notes.importBytes(f.bytes)?.let { n -> env.notes.refresh(); nav.push(Screen.Canvas(n.id)) } } } }) else null,
                onSettings = { nav.push(Screen.Settings) },
                onDiagnostics = { nav.push(Screen.Diagnostics) },
            )
            is Screen.Canvas -> {
                val cs = canvasState?.takeIf { it.controller.note.id == s.noteId }
                if (cs == null) {
                    LaunchedEffect(s.noteId) {
                        val loaded = env.notes.load(s.noteId)
                        if (loaded == null) { loadError = "노트를 열 수 없습니다"; nav.back() } else {
                            val (note, att) = loaded
                            val ui = stored.ui.withDefaultSlots()
                            canvasState = CanvasState(EditorController(note), Viewport(), env.brushes, ui.slots, ui.selectedSlot, att).also { it.eraserRadiusPt = ui.eraserRadiusPt }
                        }
                    }
                    Box(Modifier.fillMaxSize().background(PlatenTheme.colors.chrome), contentAlignment = Alignment.Center) { Text("여는 중…", color = PlatenTheme.colors.mutedText) }
                } else {
                    cs.eraserRadiusPt = stored.ui.eraserRadiusPt
                    CanvasScreen(
                        env = env, state = cs, settings = stored.app, diag = diag, mouseDraw = stored.ui.mouseDraw,
                        onBack = {
                            env.notes.save(cs.controller.note, cs.controller.dirtyGeneration, cs.attachments())
                            scope.launch { env.notes.flush(); env.notes.refresh() }
                            nav.back()
                        },
                        onOpenSettings = { nav.push(Screen.Settings) },
                        onOpenDiagnostics = { nav.push(Screen.Diagnostics) },
                        onOpenImport = { nav.push(Screen.BrushImport) },
                        onSlotsChanged = { slots, sel -> env.settings.update { it.copy(ui = it.ui.copy(slots = slots, selectedSlot = sel, lastNoteId = s.noteId)) } },
                        onToggleFingerDraw = { env.settings.update { it.copy(app = it.app.copy(ink = it.app.ink.copy(fingerDraw = !it.app.ink.fingerDraw))) } },
                    )
                }
            }
            Screen.Settings -> SettingsScreen(env, stored, onChange = { ns -> env.settings.update { ns } }, onBack = { nav.back() }, onDiagnostics = { nav.push(Screen.Diagnostics) })
            Screen.Diagnostics -> DiagnosticsScreen(env, diag, canvasState, stored.app.ink, onInkChange = { ink -> env.settings.update { it.copy(app = it.app.copy(ink = ink)) } }, onBack = { nav.back() })
            Screen.BrushImport -> BrushImportScreen(
                env, stored.app.ink,
                onAssignToSlot = { id ->
                    val cs = canvasState
                    val spec = env.brushes.get(id)
                    if (cs != null && spec != null) cs.updateSlot(cs.selectedSlot) { it.copy(brushId = id, size = spec.size.base, overrides = null) }
                    else env.settings.update { st -> st.copy(ui = st.ui.copy(slots = st.ui.slots.mapIndexed { i, sl -> if (i == st.ui.selectedSlot) sl.copy(brushId = id, size = spec?.size?.base ?: sl.size, overrides = null) else sl })) }
                    nav.back()
                },
                onBack = { nav.back() },
            )
        }
    }
}

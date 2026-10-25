package io.github.bigrirason_debug.platen.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.bigrirason_debug.platen.ui.AppEnvironment
import io.github.bigrirason_debug.platen.ui.PlatenApp
import io.github.bigrirason_debug.platen.ui.platform.AwtPlatformFiles
import io.github.bigrirason_debug.platen.ui.platform.PlatenDirs
import io.github.bigrirason_debug.platen.ui.repo.FileNoteRepository
import io.github.bigrirason_debug.platen.ui.repo.FileSettingsStore
import io.github.bigrirason_debug.platen.ui.repo.InMemoryBrushRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

/**
 * 데스크톱(Windows) 진입점. 메뉴 없음(툴바로 충분). 노트는 `~/Platen/notes`, 설정은 `~/Platen/settings.json`.
 * 마우스 전용으로 전 기능(필기 제외) — 마우스 필기는 설정의 진단용 토글.
 */
fun main() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val notes = FileNoteRepository(PlatenDirs.notes, appScope)
    val env = AppEnvironment(
        notes = notes,
        brushes = InMemoryBrushRepository(),          // TODO(head): brush 모듈 로스터·임포터 연결
        settings = FileSettingsStore(PlatenDirs.settings),
        files = AwtPlatformFiles(),
        desktop = true,
        // sync / pdf: 기본 Noop — TODO(head): sync·pdf 모듈 연결
    )
    Runtime.getRuntime().addShutdownHook(Thread { runBlocking { notes.flush() } })
    application {
        val state = remember { WindowState(width = 1280.dp, height = 820.dp) }
        Window(onCloseRequest = { runBlocking { notes.flush() }; exitApplication() }, title = "Platen", state = state) {
            PlatenApp(env)
        }
    }
}

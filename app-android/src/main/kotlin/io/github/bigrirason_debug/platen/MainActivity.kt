package io.github.bigrirason_debug.platen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.bigrirason_debug.platen.ui.AppEnvironment
import io.github.bigrirason_debug.platen.ui.Navigator
import io.github.bigrirason_debug.platen.ui.PlatenApp
import io.github.bigrirason_debug.platen.ui.Screen
import io.github.bigrirason_debug.platen.ui.platform.AndroidPlatformFiles
import io.github.bigrirason_debug.platen.ui.repo.FileNoteRepository
import io.github.bigrirason_debug.platen.ui.repo.FileSettingsStore
import io.github.bigrirason_debug.platen.ui.repo.InMemoryBrushRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Android 진입점(edge-to-edge). 노트는 `filesDir/notes`, 설정은 `filesDir/settings.json`.
 * `.platen` 열기 인텐트(VIEW)는 바이트를 저장소로 들여온 뒤 캔버스로 이동.
 * TODO: LowLatencyCanvasView(graphics-core) 프론트버퍼 호스트 — 현재 일반 Compose Canvas 경로(진단 토글은 저장만 됨).
 */
class MainActivity : ComponentActivity() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notes: FileNoteRepository
    private lateinit var env: AppEnvironment
    private val nav = Navigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notes = FileNoteRepository(File(filesDir, "notes"), appScope)
        env = AppEnvironment(
            notes = notes,
            brushes = InMemoryBrushRepository(),          // TODO(head): brush 모듈 로스터·임포터 연결
            settings = FileSettingsStore(File(filesDir, "settings.json")),
            files = AndroidPlatformFiles(this),
            desktop = false,
            // sync / pdf: Noop — TODO(head): sync(Credential Manager + Drive)·pdf 모듈 연결
        )
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (!nav.back()) { isEnabled = false; onBackPressedDispatcher.onBackPressed() } }
        })
        handleOpenIntent(intent)
        setContent { PlatenApp(env, nav) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenIntent(intent)
    }

    private fun handleOpenIntent(intent: Intent?) {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return
        appScope.launch {
            val bytes = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return@launch
            val note = notes.importBytes(bytes) ?: return@launch
            notes.refresh()
            runOnUiThread { nav.home(); nav.push(Screen.Canvas(note.id)) }
        }
    }

    override fun onStop() {
        super.onStop()
        // 백그라운드 전환 시 큐를 비운다(세대 가드가 중복 저장을 걸러 준다).
        runBlocking { notes.flush() }
    }
}

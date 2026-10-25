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
import io.github.bigrirason_debug.platen.sync.auth.AndroidGoogleAuth
import io.github.bigrirason_debug.platen.sync.auth.FileDeviceIdStore
import io.github.bigrirason_debug.platen.sync.auth.FileSyncMetaStore
import io.github.bigrirason_debug.platen.sync.auth.androidTokenStore
import io.github.bigrirason_debug.platen.sync.SyncClock
import io.github.bigrirason_debug.platen.ui.repo.ModuleBrushRepository
import io.github.bigrirason_debug.platen.ui.repo.ModulePdfFacade
import io.github.bigrirason_debug.platen.ui.repo.RepoBrushImageStore
import io.github.bigrirason_debug.platen.ui.sync.EngineSyncFacade
import io.github.bigrirason_debug.platen.ui.sync.SyncWiring
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import io.ktor.client.engine.okhttp.OkHttp
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
    private var googleAuth: AndroidGoogleAuth? = null
    /** Drive 범위 동의 화면(AuthorizationClient 해상도) 결과를 인증 객체로 돌려준다. */
    private val resolutionLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        googleAuth?.completeResolution(r.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // pdfbox-android 표준 14 폰트 메트릭 로드(PDF 내보내기 텍스트) — pdf 모듈 보고 §6
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        notes = FileNoteRepository(File(filesDir, "notes"), appScope)
        val brushes = ModuleBrushRepository(File(filesDir, "brushes"))
        val settings = FileSettingsStore(File(filesDir, "settings.json"))
        val syncDir = File(filesDir, "platen")
        // 웹 클라이언트 ID 는 빌드 시 -Pplaten.webClientId 로 주입(LOCAL_TODO G3). 비어 있으면 동기화는 안내만 한다.
        val webClientId = getString(R.string.platen_web_client_id)
        val sync = EngineSyncFacade(File(filesDir, "notes"), notes, appScope, SyncWiring(
            http = OkHttp.create(),
            auth = {
                if (webClientId.isBlank()) null
                else AndroidGoogleAuth(this, webClientId, SyncClock { System.currentTimeMillis() }, onResolutionRequired = { sender ->
                    resolutionLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }).also { googleAuth = it }
            },
            tokenStore = androidTokenStore(this),
            deviceIdStore = FileDeviceIdStore(File(syncDir, "device-id")),
            metaStore = FileSyncMetaStore(File(syncDir, "sync-meta.json")),
            missingConfigMessage = "이 빌드에는 Google 웹 클라이언트 ID 가 없습니다(LOCAL_TODO G3: -Pplaten.webClientId)",
        ))
        env = AppEnvironment(
            notes = notes,
            brushes = brushes,
            settings = settings,
            sync = sync,
            pdf = ModulePdfFacade(brushes, settings),
            files = AndroidPlatformFiles(this),
            images = RepoBrushImageStore(brushes),
            desktop = false,
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

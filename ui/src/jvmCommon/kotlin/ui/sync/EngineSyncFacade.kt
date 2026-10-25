package io.github.bigrirason_debug.platen.ui.sync

import io.github.bigrirason_debug.platen.sync.DeviceIdStore
import io.github.bigrirason_debug.platen.sync.SyncClock
import io.github.bigrirason_debug.platen.sync.SyncConfig
import io.github.bigrirason_debug.platen.sync.auth.GoogleAuth
import io.github.bigrirason_debug.platen.sync.auth.TokenManager
import io.github.bigrirason_debug.platen.sync.auth.TokenStore
import io.github.bigrirason_debug.platen.sync.drive.DriveClient
import io.github.bigrirason_debug.platen.sync.engine.SyncEngine
import io.github.bigrirason_debug.platen.sync.engine.SyncMetaStore
import io.github.bigrirason_debug.platen.ui.repo.NoteRepository
import io.github.bigrirason_debug.platen.ui.repo.SyncFacade
import io.github.bigrirason_debug.platen.ui.repo.SyncState
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** 플랫폼별 인증·저장소 조립. [auth] 가 null 이면 설정 누락(메시지 표시, 앱은 로컬로 동작). */
class SyncWiring(
    val http: HttpClientEngine,
    val auth: () -> GoogleAuth?,
    val tokenStore: TokenStore,
    val deviceIdStore: DeviceIdStore,
    val metaStore: SyncMetaStore,
    val missingConfigMessage: String,
)

/**
 * Google 로그인 + Drive 동기화 파사드(양 플랫폼 공통). 로컬 저장소는 노트 디렉터리([DirectoryLocalNoteStore]).
 * 로그인 후 60초 폴링 + "지금 동기화". 저장된 내용 해시로 변경을 감지하므로 편집 화면은 별도 신호를 보내지 않는다.
 */
class EngineSyncFacade(
    private val notesDir: File,
    private val notes: NoteRepository,
    private val scope: CoroutineScope,
    private val wiring: SyncWiring,
    private val pollMillis: Long = 60_000L,
) : SyncFacade {
    private val clock = SyncClock { System.currentTimeMillis() }
    override val state = MutableStateFlow(SyncState(available = true))

    private var tokens: TokenManager? = null
    private var engine: SyncEngine? = null
    private var drive: DriveClient? = null
    private var poller: Job? = null

    private suspend fun ensure(): Boolean {
        if (engine != null) return true
        val auth = runCatching { wiring.auth() }.getOrNull()
        if (auth == null) { state.value = state.value.copy(available = false, message = wiring.missingConfigMessage); return false }
        val tm = TokenManager(auth, wiring.tokenStore, clock)
        val config = SyncConfig(deviceId = wiring.deviceIdStore.loadOrCreate())
        val dc = DriveClient(wiring.http, tm, config)
        tokens = tm; drive = dc
        engine = SyncEngine(dc, DirectoryLocalNoteStore(notesDir), wiring.metaStore, config, clock)
        if (tm.isSignedIn()) { state.value = state.value.copy(available = true, signedIn = true); startPolling() }
        return true
    }

    init { scope.launch { runCatching { ensure() } } }

    override suspend fun signIn() {
        if (!ensure()) return
        state.value = state.value.copy(busy = true, message = "Google 로그인 진행 중…")
        runCatching {
            tokens!!.signIn()
            val who = runCatching { drive!!.about().user?.emailAddress }.getOrNull()
            state.value = state.value.copy(signedIn = true, accountLabel = who, busy = false, message = null)
            startPolling()
            syncNow()
        }.onFailure { e -> state.value = state.value.copy(busy = false, message = "로그인 실패: ${e.message}") }
    }

    override suspend fun signOut() {
        poller?.cancel(); poller = null
        runCatching { tokens?.signOut() }
        state.value = state.value.copy(signedIn = false, accountLabel = null, message = null)
    }

    override suspend fun syncNow() {
        val e = engine ?: return
        if (!state.value.signedIn) return
        notes.flush()
        state.value = state.value.copy(busy = true)
        runCatching { e.syncOnce() }
            .onSuccess { (push, pull) ->
                notes.refresh()
                val conflicts = push.conflicts + pull.conflicts
                val msg = "올림 ${push.uploaded} · 받음 ${pull.downloaded + pull.created}" + if (conflicts > 0) " · 충돌 사본 $conflicts" else ""
                state.value = state.value.copy(busy = false, lastSyncMillis = clock.nowMillis(), message = msg)
            }
            .onFailure { err -> state.value = state.value.copy(busy = false, message = "동기화 실패: ${err.message}") }
    }

    private fun startPolling() {
        if (poller?.isActive == true) return
        poller = scope.launch { while (isActive) { delay(pollMillis); runCatching { syncNow() } } }
    }
}

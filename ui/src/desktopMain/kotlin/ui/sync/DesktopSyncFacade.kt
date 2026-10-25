package io.github.bigrirason_debug.platen.ui.sync

import io.github.bigrirason_debug.platen.sync.DesktopSyncPaths
import io.github.bigrirason_debug.platen.sync.SyncClock
import io.github.bigrirason_debug.platen.sync.SyncConfig
import io.github.bigrirason_debug.platen.sync.auth.DesktopLoopbackAuth
import io.github.bigrirason_debug.platen.sync.auth.OAuthClientConfig
import io.github.bigrirason_debug.platen.sync.auth.TokenManager
import io.github.bigrirason_debug.platen.sync.drive.DriveClient
import io.github.bigrirason_debug.platen.sync.engine.SyncEngine
import io.github.bigrirason_debug.platen.sync.loadDesktop
import io.github.bigrirason_debug.platen.ui.repo.NoteRepository
import io.github.bigrirason_debug.platen.ui.repo.SyncFacade
import io.github.bigrirason_debug.platen.ui.repo.SyncState
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.net.URI

/**
 * 데스크톱 Google 로그인(루프백+PKCE) + Drive 동기화. OAuth 클라이언트는 `~/.platen/oauth-client.json`
 * (Google 콘솔에서 받은 데스크톱 client_secret JSON, LOCAL_TODO G3) — 없으면 안내 메시지만 보이고 앱은 로컬로 동작한다.
 */
class DesktopSyncFacade(
    private val notesDir: File,
    private val notes: NoteRepository,
    private val scope: CoroutineScope,
    private val paths: DesktopSyncPaths = DesktopSyncPaths(),
) : SyncFacade {
    private val clock = SyncClock { System.currentTimeMillis() }
    override val state = MutableStateFlow(SyncState(available = paths.oauthClient.exists(),
        message = if (paths.oauthClient.exists()) null else "~/.platen/oauth-client.json 이 없습니다 (LOCAL_TODO G3)"))

    private var tokens: TokenManager? = null
    private var engine: SyncEngine? = null
    private var drive: DriveClient? = null
    private var poller: Job? = null

    private suspend fun ensure(): Boolean {
        if (engine != null) return true
        val cfg = runCatching { OAuthClientConfig.loadDesktop(paths) }.getOrElse { e ->
            state.value = state.value.copy(available = false, message = e.message); return false
        }
        val http = CIO.create()
        val auth = DesktopLoopbackAuth(cfg, http, clock, openBrowser = { url -> runCatching { Desktop.getDesktop().browse(URI(url)) } })
        val tm = TokenManager(auth, paths.tokenStore(), clock)
        val config = SyncConfig(deviceId = paths.deviceIdStore().loadOrCreate())
        val dc = DriveClient(http, tm, config)
        tokens = tm; drive = dc
        engine = SyncEngine(dc, DirectoryLocalNoteStore(notesDir), paths.syncMetaStore(), config, clock)
        if (tm.isSignedIn()) { state.value = state.value.copy(available = true, signedIn = true); startPolling(config.pollIntervalMillis) }
        return true
    }

    init { scope.launch { runCatching { ensure() } } }

    override suspend fun signIn() {
        if (!ensure()) return
        state.value = state.value.copy(busy = true, message = "브라우저에서 Google 로그인을 완료하세요")
        runCatching {
            tokens!!.signIn()
            val who = runCatching { drive!!.about().user?.emailAddress }.getOrNull()
            state.value = state.value.copy(signedIn = true, accountLabel = who, busy = false, message = null)
            startPolling(60_000L)
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
                val msg = buildString {
                    append("올림 ${push.uploaded} · 받음 ${pull.downloaded + pull.created}")
                    val c = push.conflicts + pull.conflicts
                    if (c > 0) append(" · 충돌 사본 $c")
                }
                state.value = state.value.copy(busy = false, lastSyncMillis = clock.nowMillis(), message = msg)
            }
            .onFailure { err -> state.value = state.value.copy(busy = false, message = "동기화 실패: ${err.message}") }
    }

    private fun startPolling(intervalMillis: Long) {
        if (poller?.isActive == true) return
        poller = scope.launch {
            while (isActive) { delay(intervalMillis); runCatching { syncNow() } }
        }
    }
}

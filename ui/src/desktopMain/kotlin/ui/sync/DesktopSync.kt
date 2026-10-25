package io.github.bigrirason_debug.platen.ui.sync

import io.github.bigrirason_debug.platen.sync.DesktopSyncPaths
import io.github.bigrirason_debug.platen.sync.SyncClock
import io.github.bigrirason_debug.platen.sync.auth.DesktopLoopbackAuth
import io.github.bigrirason_debug.platen.sync.auth.OAuthClientConfig
import io.github.bigrirason_debug.platen.sync.loadDesktop
import io.github.bigrirason_debug.platen.ui.repo.NoteRepository
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import java.awt.Desktop
import java.io.File
import java.net.URI

/**
 * 데스크톱: 루프백+PKCE 로그인. OAuth 클라이언트는 `~/.platen/oauth-client.json`
 * (Google 콘솔의 데스크톱 client_secret JSON, LOCAL_TODO G3). 없으면 안내만 하고 앱은 로컬로 동작한다.
 */
fun desktopSyncFacade(notesDir: File, notes: NoteRepository, scope: CoroutineScope, paths: DesktopSyncPaths = DesktopSyncPaths()): EngineSyncFacade {
    val http = CIO.create()
    val clock = SyncClock { System.currentTimeMillis() }
    return EngineSyncFacade(notesDir, notes, scope, SyncWiring(
        http = http,
        auth = {
            if (!paths.oauthClient.exists()) null
            else DesktopLoopbackAuth(OAuthClientConfig.loadDesktop(paths), http, clock, openBrowser = { url -> runCatching { Desktop.getDesktop().browse(URI(url)) } })
        },
        tokenStore = paths.tokenStore(),
        deviceIdStore = paths.deviceIdStore(),
        metaStore = paths.syncMetaStore(),
        missingConfigMessage = "~/.platen/oauth-client.json 이 없습니다 — Google 콘솔에서 받은 데스크톱 클라이언트 JSON 을 두세요(LOCAL_TODO G3)",
    ))
}

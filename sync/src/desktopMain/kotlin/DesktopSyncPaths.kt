package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.auth.FileDeviceIdStore
import io.github.bigrirason_debug.platen.sync.auth.FileQueueStore
import io.github.bigrirason_debug.platen.sync.auth.FileSyncMetaStore
import io.github.bigrirason_debug.platen.sync.auth.FileTokenStore
import io.github.bigrirason_debug.platen.sync.auth.OAuthClientConfig
import io.github.bigrirason_debug.platen.sync.auth.loadFromFile
import java.io.File

/** 데스크톱 저장 위치: `~/.platen/`. */
class DesktopSyncPaths(val dir: File = File(System.getProperty("user.home"), ".platen")) {
    /** 사용자가 첫 실행 시 직접 두는 Google 콘솔 client_secret JSON. */
    val oauthClient: File get() = File(dir, "oauth-client.json")
    val tokens: File get() = File(dir, "tokens.json")
    val deviceId: File get() = File(dir, "device-id")
    val queue: File get() = File(dir, "queue.json")
    val syncMeta: File get() = File(dir, "sync-meta.json")

    fun tokenStore() = FileTokenStore(tokens)
    fun deviceIdStore() = FileDeviceIdStore(deviceId)
    fun queueStore() = FileQueueStore(queue)
    fun syncMetaStore() = FileSyncMetaStore(syncMeta)
}

/** `~/.platen/oauth-client.json` 에서 데스크톱 OAuth 클라이언트를 읽는다. 없으면 안내 메시지를 가진 예외. */
fun OAuthClientConfig.Companion.loadDesktop(paths: DesktopSyncPaths = DesktopSyncPaths()): OAuthClientConfig =
    loadFromFile(paths.oauthClient)

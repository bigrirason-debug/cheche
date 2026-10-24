package io.github.bigrirason_debug.platen.sync.engine

enum class SyncState { IDLE, SYNCING, ERROR, OFFLINE }

/** UI 에 노출되는 동기화 상태. */
data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val lastSyncMillis: Long? = null,
    val conflictCount: Int = 0,
    val message: String? = null,
)

/** `pushAll()` 결과. */
data class PushReport(val uploaded: Int = 0, val skipped: Int = 0, val conflicts: Int = 0)

/** `pull()` 결과. */
data class PullReport(val downloaded: Int = 0, val created: Int = 0, val trashed: Int = 0, val conflicts: Int = 0)

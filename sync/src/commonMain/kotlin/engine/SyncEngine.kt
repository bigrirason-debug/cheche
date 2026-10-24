package io.github.bigrirason_debug.platen.sync.engine

import io.github.bigrirason_debug.platen.sync.SyncClock
import io.github.bigrirason_debug.platen.sync.SyncConfig
import io.github.bigrirason_debug.platen.sync.UtcTime
import io.github.bigrirason_debug.platen.sync.drive.DriveException
import io.github.bigrirason_debug.platen.sync.drive.DriveFile
import io.github.bigrirason_debug.platen.sync.drive.FOLDER_MIME
import io.github.bigrirason_debug.platen.sync.drive.RemoteStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 동기화 핵심 로직(sync-google §5 그대로). 순수 로직 — 원격/로컬/시계는 주입.
 *
 * - 업로드 전 `files.get(version,md5Checksum)` 비교. md5 같으면 업로드, 다르고 dirty 면 **충돌**:
 *   원격본을 "<이름> (충돌 사본 <기기> <시각>).platen" 으로 로컬·Drive 양쪽에 저장하고 내 로컬본으로 원격을 덮어쓴다(마지막 쓰기 승리).
 * - `modifiedTime` 은 앱이 쓸 수 있는 필드라 판정에 쓰지 않는다(§4.3) — 이 클래스는 md5/version 만 본다.
 * - 첫 pull 은 `files.list` 전체, 이후는 `changes.list` 증분(startPageToken 영속화).
 *
 * `writeBytes` 는 `localGen` 을 올리지 않는다고 가정한다(원격 교체는 로컬 편집이 아님).
 */
class SyncEngine(
    private val remote: RemoteStore,
    private val local: LocalNoteStore,
    private val metaStore: SyncMetaStore,
    private val config: SyncConfig,
    private val clock: SyncClock,
) {
    private val _status = MutableStateFlow(SyncStatus())
    /** idle/syncing/error/offline + 마지막 동기화 시각 + 누적 충돌 수. */
    val status: StateFlow<SyncStatus> = _status

    private val lock = Mutex()

    /** dirty 노트 전부 업로드. */
    suspend fun pushAll(): PushReport = pushNotes(null)

    /** 지정 노트(null 이면 전부) 중 dirty 인 것만 업로드. */
    suspend fun pushNotes(noteIds: Set<String>?): PushReport = lock.withLock {
        run("push") {
            val folder = folderId()
            var uploaded = 0
            var skipped = 0
            var conflicts = 0
            for (state in local.listNotes()) {
                if (noteIds != null && state.noteId !in noteIds) continue
                if (!state.dirty) { skipped++; continue }
                when (pushOne(state, folder)) {
                    PushOutcome.UPLOADED -> uploaded++
                    PushOutcome.SKIPPED -> skipped++
                    PushOutcome.CONFLICT -> { uploaded++; conflicts++ }
                }
            }
            PushReport(uploaded, skipped, conflicts).also { addConflicts(it.conflicts) }
        }
    }

    /** 원격 변경 반영(첫 실행은 전체 목록, 이후 증분). */
    suspend fun pull(): PullReport = lock.withLock {
        run("pull") {
            val folder = folderId()
            val meta = metaStore.load()
            val byFileId = local.listNotes().filter { it.fileId != null }.associateBy { it.fileId!! }.toMutableMap()
            val report = PullCounters()
            if (meta.startPageToken == null) {
                // 토큰을 먼저 받아야 목록 조회 중 생긴 변경을 다음 증분에서 놓치지 않는다.
                val token = remote.startPageToken()
                for (f in remote.listFolder(folder)) reconcile(f, byFileId, folder, report)
                metaStore.save(metaStore.load().copy(startPageToken = token))
            } else {
                val page = remote.listChanges(meta.startPageToken)
                for (c in page.changes) {
                    val fileId = c.fileId ?: c.file?.id ?: continue
                    val known = byFileId[fileId]
                    val f = c.file
                    if (c.removed || f?.trashed == true) {
                        if (known != null) {
                            local.trash(known.noteId)
                            byFileId.remove(fileId)
                            report.trashed++
                        }
                        continue
                    }
                    if (f == null || f.mimeType == FOLDER_MIME) continue
                    val inFolder = f.parents?.contains(folder) == true
                    if (known == null && !inFolder) continue
                    reconcile(f, byFileId, folder, report)
                }
                metaStore.save(metaStore.load().copy(startPageToken = page.newStartPageToken))
            }
            report.toReport().also { addConflicts(it.conflicts) }
        }
    }

    /** push 후 pull. */
    suspend fun syncOnce(): Pair<PushReport, PullReport> = pushAll() to pull()

    // ---- 내부 ----

    private enum class PushOutcome { UPLOADED, SKIPPED, CONFLICT }

    private class PullCounters {
        var downloaded = 0; var created = 0; var trashed = 0; var conflicts = 0
        fun toReport() = PullReport(downloaded, created, trashed, conflicts)
    }

    private suspend fun pushOne(state: LocalNoteState, folder: String): PushOutcome {
        val fileId = state.fileId
        val remoteFile = fileId?.let { remote.getFile(it) }
        if (fileId == null || remoteFile == null || remoteFile.trashed == true) {
            // 원격에 없음(새 노트, 또는 원격에서 삭제됨) → 새로 생성.
            val bytes = local.readBytes(state.noteId)
            val created = remote.create(fileName(state.name), bytes, folder, props(state.localGen, state.remoteMd5))
            local.updateState(synced(state, created))
            return PushOutcome.UPLOADED
        }
        if (remoteFile.md5Checksum != null && remoteFile.md5Checksum == state.localMd5) {
            // 내용이 이미 같다(결정적 zip): 업로드 생략, 상태만 맞춤.
            local.updateState(synced(state, remoteFile))
            return PushOutcome.SKIPPED
        }
        var outcome = PushOutcome.UPLOADED
        if (remoteFile.md5Checksum != state.remoteMd5) {
            // 원격이 내가 마지막으로 본 것과 다르고 나도 dirty → 충돌.
            saveConflictCopy(state, remoteFile, folder)
            outcome = PushOutcome.CONFLICT
        }
        val bytes = local.readBytes(state.noteId)
        val updated = remote.update(fileId, bytes, props(state.localGen, remoteFile.md5Checksum))
        local.updateState(synced(state, updated))
        return outcome
    }

    private suspend fun reconcile(f: DriveFile, byFileId: MutableMap<String, LocalNoteState>, folder: String, r: PullCounters) {
        val known = byFileId[f.id]
        if (known == null) {
            val bytes = remote.download(f.id)
            val created = local.createFromRemote(f.id, stripExt(f.name ?: f.id), bytes)
            val st = created.copy(fileId = f.id, remoteVersion = f.version, remoteMd5 = f.md5Checksum, localMd5 = f.md5Checksum, syncedGen = created.localGen)
            local.updateState(st)
            byFileId[f.id] = st
            r.created++
            return
        }
        if (f.md5Checksum == known.remoteMd5) {
            if (f.version != known.remoteVersion) local.updateState(known.copy(remoteVersion = f.version))
            return
        }
        if (known.dirty) {
            saveConflictCopy(known, f, folder)
            val bytes = local.readBytes(known.noteId)
            val updated = remote.update(f.id, bytes, props(known.localGen, f.md5Checksum))
            local.updateState(synced(known, updated))
            r.conflicts++
            return
        }
        val bytes = remote.download(f.id)
        local.writeBytes(known.noteId, bytes)
        local.updateState(known.copy(remoteVersion = f.version, remoteMd5 = f.md5Checksum, localMd5 = f.md5Checksum, syncedGen = known.localGen))
        r.downloaded++
    }

    /** 원격본을 충돌 사본으로 로컬 + Drive 양쪽에 저장(§5.1). */
    private suspend fun saveConflictCopy(state: LocalNoteState, remoteFile: DriveFile, folder: String) {
        val remoteBytes = remote.download(remoteFile.id)
        val label = "${state.name} (충돌 사본 ${config.deviceId} ${UtcTime.fileStamp(clock.nowMillis())})"
        val copy = local.writeConflictCopy(state.noteId, remoteBytes, label)
        val created = remote.create(fileName(label), remoteBytes, folder, props(copy.localGen, remoteFile.md5Checksum))
        local.updateState(copy.copy(fileId = created.id, remoteVersion = created.version, remoteMd5 = created.md5Checksum, localMd5 = created.md5Checksum ?: remoteFile.md5Checksum, syncedGen = copy.localGen))
    }

    private fun synced(state: LocalNoteState, f: DriveFile): LocalNoteState = state.copy(
        fileId = f.id,
        remoteVersion = f.version,
        remoteMd5 = f.md5Checksum,
        syncedGen = state.localGen,
        localMd5 = f.md5Checksum ?: state.localMd5,
    )

    /** `appProperties{gen, deviceId, baseMd5}`(§5.1). */
    private fun props(gen: Long, baseMd5: String?): Map<String, String> =
        mapOf("gen" to gen.toString(), "deviceId" to config.deviceId, "baseMd5" to (baseMd5 ?: ""))

    private suspend fun folderId(): String {
        metaStore.load().folderId?.let { return it }
        val id = remote.ensureRootFolder()
        metaStore.save(metaStore.load().copy(folderId = id))
        return id
    }

    private fun addConflicts(n: Int) {
        if (n > 0) _status.update { it.copy(conflictCount = it.conflictCount + n) }
    }

    private suspend inline fun <T> run(what: String, block: () -> T): T {
        _status.update { it.copy(state = SyncState.SYNCING, message = null) }
        return try {
            block().also { _status.update { it.copy(state = SyncState.IDLE, lastSyncMillis = clock.nowMillis(), message = null) } }
        } catch (e: CancellationException) {
            _status.update { it.copy(state = SyncState.IDLE) }
            throw e
        } catch (e: DriveException) {
            _status.update { it.copy(state = SyncState.ERROR, message = "$what: ${e.message}") }
            throw e
        } catch (e: Exception) {
            // Drive 가 응답하지 않음(네트워크·DNS 등 플랫폼 예외) → offline 으로 본다.
            _status.update { it.copy(state = SyncState.OFFLINE, message = "$what: ${e.message}") }
            throw e
        }
    }

    companion object {
        const val EXT = ".platen"
        fun fileName(name: String): String = if (name.endsWith(EXT)) name else name + EXT
        fun stripExt(name: String): String = name.removeSuffix(EXT)
    }
}

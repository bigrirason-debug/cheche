package io.github.bigrirason_debug.platen.sync.engine

import kotlinx.serialization.Serializable

/**
 * 노트별 동기화 상태(sync-google §5 로컬 DB 스키마).
 * @param localGen 로컬 세대 번호(저장마다 +1).
 * @param syncedGen 마지막으로 업로드 성공한 세대. `dirty = localGen > syncedGen`.
 * @param localMd5 로컬 `.platen` 바이트의 md5(hex, 소문자). 결정적 zip 이라 원격 md5 와 직접 비교 가능(§5.4).
 */
@Serializable
data class LocalNoteState(
    val noteId: String,
    val name: String,
    val fileId: String? = null,
    val remoteVersion: String? = null,
    val remoteMd5: String? = null,
    val localGen: Long = 0L,
    val syncedGen: Long = 0L,
    val localMd5: String? = null,
) {
    val dirty: Boolean get() = localGen > syncedGen
}

/** 엔진이 로컬 노트 저장소에 요구하는 연산. 구현은 ui/app 모듈(테스트는 메모리). */
interface LocalNoteStore {
    suspend fun listNotes(): List<LocalNoteState>
    suspend fun readBytes(noteId: String): ByteArray

    /** 원격본으로 로컬 내용을 교체한다(상태 갱신은 엔진이 [updateState] 로 수행). */
    suspend fun writeBytes(noteId: String, bytes: ByteArray)

    /** 충돌 사본을 새 로컬 노트로 저장하고 그 상태를 돌려준다. [label] 이 노트 이름. */
    suspend fun writeConflictCopy(noteId: String, bytes: ByteArray, label: String): LocalNoteState

    /** 원격에만 있던 파일을 새 로컬 노트로 만든다. */
    suspend fun createFromRemote(fileId: String, name: String, bytes: ByteArray): LocalNoteState

    /** 원격에서 삭제/휴지통 처리된 노트를 로컬 휴지통으로. */
    suspend fun trash(noteId: String)

    suspend fun updateState(state: LocalNoteState)
}

/** 동기화 커서(startPageToken)·폴더 id 영속화. */
@Serializable
data class SyncMeta(val startPageToken: String? = null, val folderId: String? = null)

interface SyncMetaStore {
    suspend fun load(): SyncMeta
    suspend fun save(meta: SyncMeta)
}

class InMemorySyncMetaStore(initial: SyncMeta = SyncMeta()) : SyncMetaStore {
    var meta: SyncMeta = initial
        private set
    override suspend fun load(): SyncMeta = meta
    override suspend fun save(meta: SyncMeta) { this.meta = meta }
}

package io.github.bigrirason_debug.platen.ui.repo

import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.newId
import io.github.bigrirason_debug.platen.model.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** 홈 서랍 카드 1장. 썸네일은 후속(PNG 바이트 자리만). */
data class NoteSummary(val id: String, val title: String, val modified: Long, val pageCount: Int, val thumbnailPng: ByteArray? = null)

/** 노트에 동봉되는 바이너리(브러시 zip·에셋). 컨테이너 Bundle 과 1:1. */
class NoteAttachments(
    val brushes: Map<String, ByteArray> = emptyMap(),
    val assets: Map<String, Pair<String, ByteArray>> = emptyMap(),
)

/**
 * 노트 저장소 계약. 구현: jvmCommon `FileNoteRepository`(단일 직렬 큐 + 세대 가드), 테스트용 `InMemoryNoteRepository`.
 * `save` 는 비차단: 호출 시점에 (noteId, generation) 을 스냅샷하고 워커 1개가 순서대로 쓰되 구세대는 버린다(PLAN §4.5).
 */
interface NoteRepository {
    val notes: StateFlow<List<NoteSummary>>
    suspend fun refresh()
    suspend fun load(id: String): Pair<Note, NoteAttachments>?
    /** generation = 호출자(EditorController.dirtyGeneration)가 메인에서 찍은 세대. */
    fun save(note: Note, generation: Long, attachments: NoteAttachments = NoteAttachments())
    suspend fun delete(id: String)
    suspend fun create(title: String): Note
    /** 큐에 남은 저장을 모두 끝낼 때까지 대기(종료·테스트). */
    suspend fun flush()
    /** 외부 `.platen` 바이트를 저장소에 들여온다(파일 열기). */
    suspend fun importBytes(bytes: ByteArray): Note?
    /** 저장소의 노트를 `.platen` 바이트로(파일 저장·내보내기). */
    suspend fun exportBytes(id: String): ByteArray?
}

fun newBlankNote(title: String): Note {
    val now = nowMillis()
    return Note(id = newId(), title = title, created = now, modified = now, pages = listOf(Page(id = newId())))
}

/** 메모리 구현(테스트·목업). */
class InMemoryNoteRepository : NoteRepository {
    private val store = LinkedHashMap<String, Note>()
    private val attachments = HashMap<String, NoteAttachments>()
    override val notes = MutableStateFlow<List<NoteSummary>>(emptyList())
    var saveCount = 0
        private set

    private fun publish() {
        notes.update { store.values.sortedByDescending { it.modified }.map { NoteSummary(it.id, it.title, it.modified, it.pages.size) } }
    }
    override suspend fun refresh() = publish()
    override suspend fun load(id: String) = store[id]?.let { it to (attachments[id] ?: NoteAttachments()) }
    override fun save(note: Note, generation: Long, attachments: NoteAttachments) {
        store[note.id] = note; this.attachments[note.id] = attachments; saveCount++; publish()
    }
    override suspend fun delete(id: String) { store.remove(id); attachments.remove(id); publish() }
    override suspend fun create(title: String): Note = newBlankNote(title).also { store[it.id] = it; publish() }
    override suspend fun flush() {}
    override suspend fun importBytes(bytes: ByteArray): Note? = null
    override suspend fun exportBytes(id: String): ByteArray? = null
}

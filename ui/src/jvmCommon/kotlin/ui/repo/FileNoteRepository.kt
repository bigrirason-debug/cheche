package io.github.bigrirason_debug.platen.ui.repo

import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.PlatenContainer
import io.github.bigrirason_debug.platen.model.PlatenSchema
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 노트 디렉터리(`<dir>/<noteId>.platen`) 저장소.
 *
 * 저장 규율(PLAN §4.5, iwhg 데이터 손실 교훈):
 * - `save()` 는 호출 스레드(메인)에서 (noteId, generation, 불변 Note 스냅샷) 을 큐에 넣고 즉시 반환.
 * - 워커 **1개**(`Channel` 소비자)가 순서대로 처리. 처리 직전에 그 노트의 **최신 세대보다 낮으면 버린다**(구세대 저장 폐기).
 * - 쓰기는 임시 파일 → 원자적 이동(ATOMIC_MOVE). 실패해도 이전 파일은 남는다.
 */
class FileNoteRepository(
    val dir: File,
    scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    /** 테스트 훅: 실제 쓰기 직전 호출(지연 주입). */
    private val beforeWrite: suspend (SaveJob) -> Unit = {},
) : NoteRepository {
    class SaveJob(val note: Note, val generation: Long, val attachments: NoteAttachments, val done: CompletableDeferred<Unit> = CompletableDeferred())

    private val queue = Channel<SaveJob>(Channel.UNLIMITED)
    private val latestGeneration = HashMap<String, Long>()   // 메인 스레드에서만 갱신
    private val lock = Any()
    override val notes = MutableStateFlow<List<NoteSummary>>(emptyList())
    private val summaries = LinkedHashMap<String, NoteSummary>()

    /** 통계(진단·테스트): 실제 쓴 횟수 / 구세대라 버린 횟수. */
    @Volatile var writeCount: Int = 0; private set
    @Volatile var skippedCount: Int = 0; private set

    private val worker: Job = scope.launch(io) {
        for (job in queue) {
            try {
                if (job.note.id == sentinel.id) continue
                val latest = synchronized(lock) { latestGeneration[job.note.id] ?: 0L }
                if (job.generation < latest) { skippedCount++; continue }
                beforeWrite(job)
                writeAtomic(job.note, job.attachments)
                writeCount++
                synchronized(lock) { summaries[job.note.id] = NoteSummary(job.note.id, job.note.title, job.note.modified, job.note.pages.size) }
                publish()
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                // 저장 실패는 큐를 멈추지 않는다. 이전 파일은 보존됨.
                System.err.println("Platen save failed for ${job.note.id}: $t")
            } finally {
                job.done.complete(Unit)
            }
        }
    }

    private fun fileFor(id: String) = File(dir, "$id.${PlatenSchema.EXTENSION}")

    private fun writeAtomic(note: Note, att: NoteAttachments) {
        dir.mkdirs()
        val target = fileFor(note.id)
        val tmp = File(dir, "${note.id}.tmp")
        tmp.outputStream().use { PlatenContainer.write(PlatenContainer.Bundle(note, att.brushes, att.assets), it) }
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun publish() {
        val list = synchronized(lock) { summaries.values.sortedByDescending { it.modified } }
        notes.update { list }
    }

    override suspend fun refresh() = withContext(io) {
        dir.mkdirs()
        val files = dir.listFiles { f -> f.isFile && f.extension == PlatenSchema.EXTENSION }.orEmpty()
        val loaded = LinkedHashMap<String, NoteSummary>()
        for (f in files) {
            runCatching { f.inputStream().use { PlatenContainer.read(it) } }.getOrNull()?.let { r ->
                val n = r.bundle.note
                loaded[n.id] = NoteSummary(n.id, n.title, n.modified, n.pages.size, r.bundle.thumbnailPng)
            }
        }
        synchronized(lock) { summaries.clear(); summaries.putAll(loaded) }
        publish()
    }

    override suspend fun load(id: String): Pair<Note, NoteAttachments>? = withContext(io) {
        val f = fileFor(id)
        if (!f.exists()) return@withContext null
        val r = f.inputStream().use { PlatenContainer.read(it) }
        r.bundle.note to NoteAttachments(r.bundle.brushes, r.bundle.assets)
    }

    override fun save(note: Note, generation: Long, attachments: NoteAttachments) {
        synchronized(lock) {
            val cur = latestGeneration[note.id] ?: -1L
            if (generation > cur) latestGeneration[note.id] = generation
        }
        queue.trySend(SaveJob(note, generation, attachments))
    }

    /** 특정 저장의 완료까지 대기(테스트). */
    fun saveAndAwait(note: Note, generation: Long, attachments: NoteAttachments = NoteAttachments()): CompletableDeferred<Unit> {
        synchronized(lock) {
            val cur = latestGeneration[note.id] ?: -1L
            if (generation > cur) latestGeneration[note.id] = generation
        }
        val job = SaveJob(note, generation, attachments)
        queue.trySend(job)
        return job.done
    }

    override suspend fun delete(id: String) = withContext(io) {
        fileFor(id).delete()
        synchronized(lock) { summaries.remove(id) }
        publish()
    }

    override suspend fun create(title: String): Note {
        val n = newBlankNote(title)
        saveAndAwait(n, 0L).await()
        return n
    }

    override suspend fun flush() {
        val marker = CompletableDeferred<Unit>()
        // 빈 작업을 큐 끝에 넣어 그 앞의 모든 작업 완료를 보장한다.
        queue.trySend(SaveJob(sentinel, Long.MIN_VALUE, NoteAttachments(), marker))
        marker.await()
    }

    override suspend fun importBytes(bytes: ByteArray): Note? = withContext(io) {
        val r = runCatching { PlatenContainer.read(bytes.inputStream()) }.getOrNull() ?: return@withContext null
        val n = r.bundle.note
        saveAndAwait(n, (latestGeneration[n.id] ?: 0L) + 1, NoteAttachments(r.bundle.brushes, r.bundle.assets)).await()
        n
    }

    override suspend fun exportBytes(id: String): ByteArray? = withContext(io) {
        val f = fileFor(id)
        if (f.exists()) f.readBytes() else null
    }

    private companion object {
        /** flush 용 더미 노트: 워커는 세대 비교에서 반드시 건너뛴다(latest 0 > MIN_VALUE). */
        val sentinel = Note(id = "__flush__", title = "", created = 0, modified = 0, pages = emptyList())
    }
}

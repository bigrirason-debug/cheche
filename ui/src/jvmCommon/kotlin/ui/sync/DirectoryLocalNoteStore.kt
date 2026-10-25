package io.github.bigrirason_debug.platen.ui.sync

import io.github.bigrirason_debug.platen.model.PlatenContainer
import io.github.bigrirason_debug.platen.model.PlatenSchema
import io.github.bigrirason_debug.platen.model.newId
import io.github.bigrirason_debug.platen.sync.engine.LocalNoteState
import io.github.bigrirason_debug.platen.sync.engine.LocalNoteStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * sync 엔진용 로컬 저장소 = 노트 디렉터리(`<dir>/<noteId>.platen`, FileNoteRepository 와 같은 배치).
 *
 * dirty 판정은 **내용 해시**로 한다: 파일 md5 가 마지막으로 동기화된 md5 와 다르면 `localGen = syncedGen + 1`.
 * 덕분에 편집 화면이 별도 신호를 보내지 않아도 저장된 변경이 업로드된다. 상태는 `<dir>/.sync/state.json` 에 영속.
 */
class DirectoryLocalNoteStore(private val dir: File) : LocalNoteStore {
    @Serializable
    private data class Row(
        val noteId: String, val name: String, val fileId: String? = null, val remoteVersion: String? = null,
        val remoteMd5: String? = null, val localGen: Long = 0, val syncedGen: Long = 0, val localMd5: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stateFile = File(dir, ".sync/state.json")
    private val trashDir = File(dir, ".trash")
    private val lock = Mutex()
    private val rows = LinkedHashMap<String, Row>()
    /** readBytes/writeBytes 가 마지막으로 다룬 바이트의 md5 — updateState 가 "동기화된 내용"으로 기록한다. */
    private val handledMd5 = HashMap<String, String>()
    private var loaded = false

    private fun fileFor(id: String) = File(dir, "$id.${PlatenSchema.EXTENSION}")
    private fun md5(f: File) = PlatenContainer.md5Hex(f.readBytes())

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        if (stateFile.exists()) runCatching {
            json.decodeFromString(ListSerializer(Row.serializer()), stateFile.readText()).forEach { rows[it.noteId] = it }
        }
    }

    private fun persist() {
        stateFile.parentFile.mkdirs()
        val tmp = File(stateFile.parentFile, "state.json.tmp")
        tmp.writeText(json.encodeToString(ListSerializer(Row.serializer()), rows.values.toList()))
        Files.move(tmp.toPath(), stateFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun Row.toState(currentMd5: String?): LocalNoteState {
        val changed = currentMd5 != null && currentMd5 != localMd5
        val gen = if (changed) maxOf(localGen, syncedGen + 1) else localGen
        return LocalNoteState(noteId, name, fileId, remoteVersion, remoteMd5, gen, syncedGen, localMd5)
    }

    override suspend fun listNotes(): List<LocalNoteState> = lock.withLock {
        withContext(Dispatchers.IO) {
            ensureLoaded()
            dir.mkdirs()
            val files = dir.listFiles { f -> f.isFile && f.extension == PlatenSchema.EXTENSION }.orEmpty()
            files.map { f ->
                val id = f.nameWithoutExtension
                val row = rows.getOrPut(id) { Row(id, "$id.${PlatenSchema.EXTENSION}") }
                row.toState(md5(f))
            }
        }
    }

    override suspend fun readBytes(noteId: String): ByteArray = withContext(Dispatchers.IO) {
        fileFor(noteId).readBytes().also { lock.withLock { handledMd5[noteId] = PlatenContainer.md5Hex(it) } }
    }

    override suspend fun writeBytes(noteId: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        writeAtomic(fileFor(noteId), bytes)
        lock.withLock {
            ensureLoaded()
            val m = PlatenContainer.md5Hex(bytes)
            handledMd5[noteId] = m
            rows[noteId]?.let { rows[noteId] = it.copy(localMd5 = m) }
            persist()
        }
    }

    override suspend fun writeConflictCopy(noteId: String, bytes: ByteArray, label: String): LocalNoteState = withContext(Dispatchers.IO) {
        // 같은 noteId 가 두 파일에 있으면 안 되므로 사본은 새 id·제목으로 다시 쓴다.
        val r = PlatenContainer.read(bytes.inputStream())
        val copyNote = r.bundle.note.copy(id = newId(), title = label)
        val out = PlatenContainer.writeToBytes(PlatenContainer.Bundle(copyNote, r.bundle.brushes, r.bundle.assets, r.bundle.thumbnailPng))
        writeAtomic(fileFor(copyNote.id), out)
        lock.withLock {
            ensureLoaded()
            // 새 사본은 아직 원격에 없음 → dirty(엔진이 create 후 updateState)
            val row = Row(copyNote.id, "${copyNote.id}.${PlatenSchema.EXTENSION}", localGen = 1, syncedGen = 0, localMd5 = null)
            rows[copyNote.id] = row; persist()
            row.toState(PlatenContainer.md5Hex(out))
        }
    }

    override suspend fun createFromRemote(fileId: String, name: String, bytes: ByteArray): LocalNoteState = withContext(Dispatchers.IO) {
        val noteId = runCatching { PlatenContainer.read(bytes.inputStream()).bundle.note.id }.getOrNull() ?: newId()
        writeAtomic(fileFor(noteId), bytes)
        lock.withLock {
            ensureLoaded()
            val m = PlatenContainer.md5Hex(bytes)
            handledMd5[noteId] = m
            val row = Row(noteId, "$noteId.${PlatenSchema.EXTENSION}", fileId = fileId, localMd5 = m)
            rows[noteId] = row; persist()
            row.toState(m)
        }
    }

    override suspend fun trash(noteId: String) = withContext(Dispatchers.IO) {
        val f = fileFor(noteId)
        if (f.exists()) { trashDir.mkdirs(); Files.move(f.toPath(), File(trashDir, f.name).toPath(), StandardCopyOption.REPLACE_EXISTING) }
        lock.withLock { ensureLoaded(); rows.remove(noteId); persist() }
    }

    override suspend fun updateState(state: LocalNoteState) = withContext(Dispatchers.IO) {
        lock.withLock {
            ensureLoaded()
            val synced = state.syncedGen >= state.localGen
            val md5 = if (synced) handledMd5[state.noteId] ?: state.localMd5 else rows[state.noteId]?.localMd5
            rows[state.noteId] = Row(state.noteId, state.name, state.fileId, state.remoteVersion, state.remoteMd5, state.localGen, state.syncedGen, md5)
            persist()
        }
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        target.parentFile.mkdirs()
        val tmp = File(target.parentFile, target.name + ".sync.tmp")
        tmp.writeBytes(bytes)
        try { Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
        catch (_: Exception) { Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    }
}

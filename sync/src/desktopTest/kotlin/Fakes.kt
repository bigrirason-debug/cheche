package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.drive.ChangePage
import io.github.bigrirason_debug.platen.sync.drive.DriveChange
import io.github.bigrirason_debug.platen.sync.drive.DriveFile
import io.github.bigrirason_debug.platen.sync.drive.PLATEN_MIME
import io.github.bigrirason_debug.platen.sync.drive.RemoteStore
import io.github.bigrirason_debug.platen.sync.engine.LocalNoteState
import io.github.bigrirason_debug.platen.sync.engine.LocalNoteStore
import java.security.MessageDigest

fun md5Hex(bytes: ByteArray): String = MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }

/** 메모리 Drive 흉내: md5 는 실제 계산, version 단조 증가, 변경 로그로 changes.list 흉내. */
class FakeRemote : RemoteStore {
    val folderId = "folder1"
    val files = LinkedHashMap<String, DriveFile>()
    val contents = HashMap<String, ByteArray>()
    val changes = ArrayList<DriveChange>()
    val calls = ArrayList<String>()
    private var nextId = 1
    private var nextVersion = 1L

    val creates get() = calls.count { it.startsWith("create") }
    val updates get() = calls.count { it.startsWith("update") }

    /** 다른 기기가 원격을 고친 것처럼 꾸민다. */
    fun externalEdit(fileId: String, bytes: ByteArray) {
        val f = files.getValue(fileId).copy(md5Checksum = md5Hex(bytes), version = (nextVersion++).toString())
        files[fileId] = f; contents[fileId] = bytes
        changes += DriveChange(fileId, false, f)
    }

    fun externalCreate(name: String, bytes: ByteArray): DriveFile = put(null, name, bytes, emptyMap(), listOf(folderId))

    fun externalDelete(fileId: String) {
        files.remove(fileId); contents.remove(fileId)
        changes += DriveChange(fileId, removed = true)
    }

    private fun put(fileId: String?, name: String?, bytes: ByteArray, props: Map<String, String>, parents: List<String>?): DriveFile {
        val id = fileId ?: "f${nextId++}"
        val prev = files[id]
        val f = DriveFile(
            id = id, name = name ?: prev?.name, mimeType = PLATEN_MIME, md5Checksum = md5Hex(bytes),
            version = (nextVersion++).toString(), trashed = false, parents = parents ?: prev?.parents, appProperties = props,
        )
        files[id] = f; contents[id] = bytes
        changes += DriveChange(id, false, f)
        return f
    }

    override suspend fun ensureRootFolder(): String { calls += "ensureRootFolder"; return folderId }
    override suspend fun getFile(fileId: String): DriveFile? { calls += "get $fileId"; return files[fileId] }
    override suspend fun download(fileId: String): ByteArray { calls += "download $fileId"; return contents.getValue(fileId) }
    override suspend fun create(name: String, bytes: ByteArray, parentId: String, appProperties: Map<String, String>): DriveFile {
        calls += "create $name"; return put(null, name, bytes, appProperties, listOf(parentId))
    }
    override suspend fun update(fileId: String, bytes: ByteArray, appProperties: Map<String, String>): DriveFile {
        calls += "update $fileId"; return put(fileId, null, bytes, appProperties, null)
    }
    override suspend fun trash(fileId: String) {
        calls += "trash $fileId"
        val f = files.getValue(fileId).copy(trashed = true); files[fileId] = f; changes += DriveChange(fileId, false, f)
    }
    override suspend fun listFolder(folderId: String): List<DriveFile> {
        calls += "list"; return files.values.filter { it.trashed != true && it.parents?.contains(folderId) == true }
    }
    override suspend fun startPageToken(): String { calls += "startPageToken"; return changes.size.toString() }
    override suspend fun listChanges(pageToken: String): ChangePage {
        calls += "changes $pageToken"
        val from = pageToken.toInt()
        return ChangePage(changes.subList(from, changes.size).toList(), changes.size.toString())
    }
}

/** 메모리 로컬 노트 저장소. */
class FakeLocal : LocalNoteStore {
    val states = LinkedHashMap<String, LocalNoteState>()
    val bytes = HashMap<String, ByteArray>()
    val conflictCopies = ArrayList<Pair<String, String>>() // noteId → label
    val trashed = ArrayList<String>()
    val calls = ArrayList<String>()
    private var nextId = 100

    /** 로컬 편집: 바이트 교체 + localGen+1. */
    fun edit(noteId: String, newBytes: ByteArray) {
        bytes[noteId] = newBytes
        val s = states.getValue(noteId)
        states[noteId] = s.copy(localGen = s.localGen + 1, localMd5 = md5Hex(newBytes))
    }

    fun add(noteId: String, name: String, content: ByteArray, synced: DriveFile? = null): LocalNoteState {
        bytes[noteId] = content
        val s = LocalNoteState(
            noteId, name, fileId = synced?.id, remoteVersion = synced?.version, remoteMd5 = synced?.md5Checksum,
            localGen = 1, syncedGen = if (synced != null) 1 else 0, localMd5 = md5Hex(content),
        )
        states[noteId] = s
        return s
    }

    override suspend fun listNotes(): List<LocalNoteState> = states.values.toList()
    override suspend fun readBytes(noteId: String): ByteArray = bytes.getValue(noteId)
    override suspend fun writeBytes(noteId: String, bytes: ByteArray) { calls += "write $noteId"; this.bytes[noteId] = bytes }
    override suspend fun writeConflictCopy(noteId: String, bytes: ByteArray, label: String): LocalNoteState {
        calls += "conflictCopy $noteId"
        conflictCopies += noteId to label
        val id = "n${nextId++}"
        this.bytes[id] = bytes
        return LocalNoteState(id, label, localGen = 1, syncedGen = 0, localMd5 = md5Hex(bytes)).also { states[id] = it }
    }
    override suspend fun createFromRemote(fileId: String, name: String, bytes: ByteArray): LocalNoteState {
        calls += "createFromRemote $fileId"
        val id = "n${nextId++}"
        this.bytes[id] = bytes
        return LocalNoteState(id, name, fileId = fileId, localGen = 1, syncedGen = 0, localMd5 = md5Hex(bytes)).also { states[id] = it }
    }
    override suspend fun trash(noteId: String) { calls += "trash $noteId"; trashed += noteId; states.remove(noteId) }
    override suspend fun updateState(state: LocalNoteState) { states[state.noteId] = state }
}

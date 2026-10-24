package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.drive.ChangePage
import io.github.bigrirason_debug.platen.sync.drive.DriveChange
import io.github.bigrirason_debug.platen.sync.drive.DriveFile
import io.github.bigrirason_debug.platen.sync.drive.PLATEN_MIME
import io.github.bigrirason_debug.platen.sync.drive.RemoteStore
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 로컬 디렉터리를 Drive 처럼 흉내 내는 [RemoteStore](테스트 전용). 두 "기기"가 같은 디렉터리를 가리키면 Drive 없이 통합 테스트 가능.
 *
 * 레이아웃: `<root>/files/<id>.json`(메타) + `<id>.bin`(내용), `<root>/changes.json`(변경 로그 = changes.list),
 * `<root>/counter`(id·version 단조 카운터).
 */
class FileSystemRemote(private val root: File) : RemoteStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false }
    private val filesDir = File(root, "files").apply { mkdirs() }
    private val changesFile = File(root, "changes.json")
    private val counterFile = File(root, "counter")

    override suspend fun ensureRootFolder(): String = "fs-root"

    override suspend fun getFile(fileId: String): DriveFile? = metaFile(fileId).takeIf { it.isFile }?.let { readMeta(it) }

    override suspend fun download(fileId: String): ByteArray = File(filesDir, "$fileId.bin").readBytes()

    override suspend fun create(name: String, bytes: ByteArray, parentId: String, appProperties: Map<String, String>): DriveFile =
        write(DriveFile(id = "fs${nextCounter()}", name = name, mimeType = PLATEN_MIME, parents = listOf(parentId), trashed = false), bytes, appProperties)

    override suspend fun update(fileId: String, bytes: ByteArray, appProperties: Map<String, String>): DriveFile =
        write(getFile(fileId) ?: error("no such file $fileId"), bytes, appProperties)

    override suspend fun trash(fileId: String) {
        val f = (getFile(fileId) ?: return).copy(trashed = true, version = nextCounter().toString())
        metaFile(fileId).writeText(json.encodeToString(DriveFile.serializer(), f))
        appendChange(DriveChange(fileId, false, f))
    }

    override suspend fun listFolder(folderId: String): List<DriveFile> =
        filesDir.listFiles { f -> f.extension == "json" }.orEmpty().map { readMeta(it) }
            .filter { it.trashed != true && it.parents?.contains(folderId) == true }.sortedBy { it.id }

    override suspend fun startPageToken(): String = readChanges().size.toString()

    override suspend fun listChanges(pageToken: String): ChangePage {
        val all = readChanges()
        return ChangePage(all.drop(pageToken.toInt()), all.size.toString())
    }

    /** 다른 기기에서 삭제한 것처럼. */
    fun externalDelete(fileId: String) {
        metaFile(fileId).delete(); File(filesDir, "$fileId.bin").delete()
        appendChange(DriveChange(fileId, removed = true))
    }

    private fun write(base: DriveFile, bytes: ByteArray, props: Map<String, String>): DriveFile {
        val f = base.copy(md5Checksum = md5Hex(bytes), version = nextCounter().toString(), appProperties = props, trashed = false)
        File(filesDir, "${f.id}.bin").writeBytes(bytes)
        metaFile(f.id).writeText(json.encodeToString(DriveFile.serializer(), f))
        appendChange(DriveChange(f.id, false, f))
        return f
    }

    private fun metaFile(id: String) = File(filesDir, "$id.json")
    private fun readMeta(f: File) = json.decodeFromString(DriveFile.serializer(), f.readText())
    private fun readChanges(): List<DriveChange> =
        if (changesFile.isFile) json.decodeFromString(ListSerializer(DriveChange.serializer()), changesFile.readText()) else emptyList()
    private fun appendChange(c: DriveChange) =
        changesFile.writeText(json.encodeToString(ListSerializer(DriveChange.serializer()), readChanges() + c))
    private fun nextCounter(): Long {
        val n = (if (counterFile.isFile) counterFile.readText().trim().toLong() else 0L) + 1
        counterFile.writeText(n.toString())
        return n
    }
}

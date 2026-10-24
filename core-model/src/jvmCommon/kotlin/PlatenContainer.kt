package io.github.bigrirason_debug.platen.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * `.platen` 컨테이너(공개 zip, PLAN §4.5 / D-017).
 *
 * ```
 * mimetype                 첫 엔트리, STORED(무압축)
 * manifest.json
 * pages/0001.json          페이지 메타 + 객체 + 획 메타(샘플 제외)
 * pages/0001.strokes       획 샘플 float32 LE: 획 순서대로 [x,y,p,tx,ty,t]×n
 * brushes/<id>.zip         사용 브러시 동봉
 * assets/<sha1>.<ext>
 * thumb.png (선택)
 * ```
 * 결정적 출력: 엔트리 이름 정렬(mimetype 만 선두), 타임스탬프 고정, 압축 레벨 고정 → 같은 내용이면 같은 바이트/md5(Drive 변경 감지).
 */
object PlatenContainer {
    val json: Json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true // 상위 스키마가 추가한 필드 무시(하위호환)
        classDiscriminator = "type"
    }

    /** DOS 시간 하한(1980-01-01). ZipEntry.time 은 로컬 타임존 기준으로 저장되므로 고정 값만 쓴다. */
    private const val FIXED_TIME_MS = 315532800000L

    class Bundle(
        val note: Note,
        /** brushId → brush zip bytes */
        val brushes: Map<String, ByteArray> = emptyMap(),
        /** assetId → (mime, bytes) */
        val assets: Map<String, Pair<String, ByteArray>> = emptyMap(),
        val thumbnailPng: ByteArray? = null,
    )

    /** 페이지 JSON 에 들어가는 획 메타(샘플은 .strokes 로 분리). */
    @Serializable
    private data class StrokeMeta(
        val id: String,
        val brushId: String,
        val color: Int,
        val size: Float,
        val opacity: Float,
        val overrides: StrokeOverrides? = null,
        val n: Int,
        val createdAt: Long = 0L,
    )

    @Serializable
    private data class PageFile(
        val id: String,
        val size: Size,
        val background: Background,
        val objects: List<PageObject>,
        val strokes: List<StrokeMeta>,
    )

    // ---------------------------------------------------------------- write

    fun write(bundle: Bundle, out: OutputStream) {
        val note = bundle.note
        val entries = sortedMapOf<String, ByteArray>()

        val manifest = NoteManifest(
            noteId = note.id,
            title = note.title,
            created = note.created,
            modified = note.modified,
            pageIds = note.pages.map { it.id },
            brushes = bundle.brushes.keys.sorted(),
            assets = bundle.assets.entries.sortedBy { it.key }.map { (id, v) -> AssetRef(id, v.first, v.second.size.toLong()) },
            cover = note.cover,
            tags = note.tags,
        )
        entries["manifest.json"] = json.encodeToString(manifest).encodeToByteArray()

        note.pages.forEachIndexed { index, page ->
            val base = "pages/" + (index + 1).toString().padStart(4, '0')
            val metas = page.strokes.map {
                StrokeMeta(it.id, it.brushId, it.color, it.size, it.opacity, it.overrides, it.samples.size, it.createdAt)
            }
            entries["$base.json"] = json.encodeToString(PageFile(page.id, page.size, page.background, page.objects, metas)).encodeToByteArray()
            entries["$base.strokes"] = encodeSamples(page.strokes)
        }
        for ((id, bytes) in bundle.brushes) entries["brushes/$id.zip"] = bytes
        for ((id, v) in bundle.assets) entries["assets/$id.${extFor(v.first)}"] = v.second
        bundle.thumbnailPng?.let { entries["thumb.png"] = it }

        ZipOutputStream(out).use { zip ->
            zip.setLevel(6)
            // mimetype: 첫 엔트리, 무압축(ODF/EPUB 관례)
            val mime = PlatenSchema.MIME.encodeToByteArray()
            zip.putNextEntry(ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mime.size.toLong(); compressedSize = mime.size.toLong()
                crc = CRC32().apply { update(mime) }.value
                time = FIXED_TIME_MS
            })
            zip.write(mime); zip.closeEntry()
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name).apply { time = FIXED_TIME_MS })
                zip.write(bytes); zip.closeEntry()
            }
        }
    }

    fun writeToBytes(bundle: Bundle): ByteArray = ByteArrayOutputStream().also { write(bundle, it) }.toByteArray()

    // ---------------------------------------------------------------- read

    class ReadResult(val bundle: Bundle, val manifest: NoteManifest, val warnings: List<String>)

    class UnsupportedSchema(val minReader: Int) : RuntimeException("needs reader >= $minReader, have ${PlatenSchema.WRITE_VERSION}")

    fun read(input: InputStream): ReadResult {
        val files = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (!e.isDirectory) files[e.name] = zip.readBytes()
                zip.closeEntry(); e = zip.nextEntry
            }
        }
        return readFiles(files)
    }

    fun readFiles(files: Map<String, ByteArray>): ReadResult {
        val warnings = ArrayList<String>()
        val mime = files["mimetype"]?.decodeToString()
        if (mime != PlatenSchema.MIME) warnings += "mimetype 불일치: $mime"
        val manifest = json.decodeFromString<NoteManifest>(
            files["manifest.json"]?.decodeToString() ?: error("manifest.json 없음")
        )
        if (manifest.minReader > PlatenSchema.WRITE_VERSION) throw UnsupportedSchema(manifest.minReader)
        if (manifest.schema > PlatenSchema.WRITE_VERSION) warnings += "상위 스키마 ${manifest.schema}: 알 수 없는 필드는 무시됨"

        val pages = manifest.pageIds.mapIndexed { index, pid ->
            val base = "pages/" + (index + 1).toString().padStart(4, '0')
            val pf = json.decodeFromString<PageFile>(files["$base.json"]?.decodeToString() ?: error("$base.json 없음"))
            if (pf.id != pid) warnings += "페이지 id 불일치: manifest=$pid file=${pf.id}"
            val samples = decodeSamples(files["$base.strokes"] ?: ByteArray(0), pf.strokes.map { it.n })
            val strokes = pf.strokes.mapIndexed { i, m ->
                Stroke(m.id, m.brushId, m.color, m.size, m.opacity, m.overrides, samples[i], m.createdAt)
            }
            Page(pf.id, pf.size, pf.background, strokes, pf.objects)
        }
        val note = Note(manifest.noteId, manifest.title, manifest.created, manifest.modified, pages, manifest.cover, manifest.tags)
        val brushes = manifest.brushes.associateWith { id -> files["brushes/$id.zip"] ?: ByteArray(0).also { warnings += "브러시 누락: $id" } }
        val assets = manifest.assets.associate { a ->
            val bytes = files["assets/${a.id}.${extFor(a.mime)}"] ?: ByteArray(0).also { warnings += "에셋 누락: ${a.id}" }
            a.id to (a.mime to bytes)
        }
        return ReadResult(Bundle(note, brushes, assets, files["thumb.png"]), manifest, warnings)
    }

    // ---------------------------------------------------------------- samples codec

    private const val FLOATS_PER_SAMPLE = 6

    fun encodeSamples(strokes: List<Stroke>): ByteArray {
        val total = strokes.sumOf { it.samples.size }
        val buf = ByteBuffer.allocate(total * FLOATS_PER_SAMPLE * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (s in strokes) for (p in s.samples) {
            buf.putFloat(p.x); buf.putFloat(p.y); buf.putFloat(p.pressure)
            buf.putFloat(p.tiltX); buf.putFloat(p.tiltY); buf.putFloat(p.t)
        }
        return buf.array()
    }

    fun decodeSamples(bytes: ByteArray, counts: List<Int>): List<List<InputSample>> {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return counts.map { n ->
            List(n) {
                InputSample(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat())
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    fun extFor(mime: String): String = when (mime) {
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "application/pdf" -> "pdf"
        "image/webp" -> "webp"
        else -> "bin"
    }

    fun sha1Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    /** 브러시 id = zip 바이트 SHA-1 앞 12자리(중복 설치 dedup, 선례 유지). */
    fun brushIdOf(brushZip: ByteArray): String = sha1Hex(brushZip).substring(0, 12)

    fun md5Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("MD5").digest(bytes).joinToString("") { "%02x".format(it) }
}

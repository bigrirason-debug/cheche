package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.PlatenContainer
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 패키징된 브러시: id = zip 바이트 SHA-1 앞 12자리(PlatenContainer.brushIdOf, 노트 `brushes/<id>.zip` 과 동일). */
class PackagedBrush(val id: String, val bytes: ByteArray, val brush: LoadedBrush)

/**
 * 브러시 패키지 zip(`brush.json` + `tip.png` + `grain.png`, PLAN §4.4 / brush-formats §3.1).
 * 쓰기는 결정적: 엔트리 이름 정렬, 타임스탬프 고정, 압축 레벨 고정(PlatenContainer 와 같은 방식).
 */
object BrushPackage {
    const val SPEC_ENTRY = "brush.json"
    const val TIP_ENTRY = "tip.png"
    const val GRAIN_ENTRY = "grain.png"

    /** DOS 시간 하한(1980-01-01), PlatenContainer 와 동일. */
    private const val FIXED_TIME_MS = 315532800000L

    fun write(brush: LoadedBrush): ByteArray {
        val entries = sortedMapOf<String, ByteArray>()
        entries[SPEC_ENTRY] = PlatenContainer.json.encodeToString(BrushSpec.serializer(), brush.spec).encodeToByteArray()
        brush.tipPng?.let { entries[TIP_ENTRY] = it }
        brush.grainPng?.let { entries[GRAIN_ENTRY] = it }
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.setLevel(6)
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name).apply { time = FIXED_TIME_MS })
                zip.write(bytes); zip.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    fun idOf(zipBytes: ByteArray): String = PlatenContainer.brushIdOf(zipBytes)

    fun pack(brush: LoadedBrush): PackagedBrush {
        val bytes = write(brush)
        return PackagedBrush(idOf(bytes), bytes, brush)
    }

    /** zip → [LoadedBrush]. 알 수 없는 엔트리는 무시, 상위 스키마 필드는 무시(ignoreUnknownKeys). */
    fun load(zipBytes: ByteArray): LoadedBrush {
        val files = SafeZip.read(zipBytes)
        val specJson = files[SPEC_ENTRY]?.decodeToString() ?: throw BrushFormatException("$SPEC_ENTRY 없음")
        val spec = PlatenContainer.json.decodeFromString(BrushSpec.serializer(), specJson)
        return LoadedBrush(spec, files[spec.stamp.tip.image] ?: files[TIP_ENTRY], spec.grain.image?.let { files[it] } ?: files[GRAIN_ENTRY])
    }
}

/** 내장 9종 패키지(id → zip 바이트). 절차적 이미지·고정 JSON 이라 호출마다 같은 id. */
fun BuiltInBrushes.packaged(): Map<String, ByteArray> =
    all().associate { b -> BrushPackage.pack(b).let { it.id to it.bytes } }

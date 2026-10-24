package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.nowMillis

/**
 * 임포트 진입점: 확장자/매직으로 분기(.brush/.brushset/.zip → Procreate, .abr → ABR), 결과를 [BrushPackage] 로 패키징해 id 부여.
 */
object BrushImporter {
    class Result(val packages: List<PackagedBrush>, val notes: List<String>) {
        val brushes: List<LoadedBrush> get() = packages.map { it.brush }
    }

    fun import(fileName: String, bytes: ByteArray, importedAt: Long = nowMillis()): Result {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val result = when {
            ext == "abr" || (ext.isEmpty() && isAbr(bytes)) -> AbrImporter.import(fileName, bytes, importedAt)
            ext in setOf("brush", "brushset", "zip") || SafeZip.isZip(bytes) -> ProcreateImporter.import(fileName, bytes, importedAt)
            isAbr(bytes) -> AbrImporter.import(fileName, bytes, importedAt)
            else -> throw BrushFormatException("알 수 없는 브러시 파일: $fileName")
        }
        return Result(result.brushes.map { BrushPackage.pack(it) }, result.notes)
    }

    /** ABR 매직 추정: 빅엔디언 i16 버전 ∈ {1,2,6,7,9,10}, v6+ 면 8BIM 시그니처가 뒤따른다. */
    fun isAbr(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val v = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
        if (v == 1 || v == 2) return true
        if (v == 6 || v == 7 || v == 9 || v == 10) return bytes.size >= 8 && String(bytes, 4, 4, Charsets.US_ASCII) == "8BIM"
        return false
    }
}

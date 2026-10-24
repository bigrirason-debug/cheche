package io.github.bigrirason_debug.platen.brush

import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * 방어적 zip 읽기(COUNCIL-REVIEW 좌석 4, [ImportLimits]): 엔트리 수·엔트리당/총 압축 해제 바이트 상한, 경로 탐색 이름 거부.
 * 디렉터리 엔트리는 건너뛴다. 스트리밍으로 세면서 읽으므로 상한 초과 시 즉시 [ImportLimitExceeded].
 */
object SafeZip {
    /** `..` 세그먼트, 선행 `/`, 백슬래시, 드라이브 문자(`C:`), NUL 을 거부. */
    fun isSafeName(name: String): Boolean {
        if (name.isEmpty() || name.length > 1024) return false
        if (name.startsWith("/") || name.contains('\\') || name.contains('\u0000')) return false
        if (name.length >= 2 && name[1] == ':' ) return false
        return name.split('/').none { it == ".." }
    }

    fun read(bytes: ByteArray): LinkedHashMap<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        var total = 0L
        var entries = 0
        val buf = ByteArray(64 * 1024)
        ZipInputStream(bytes.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (!isSafeName(e.name)) throw ImportLimitExceeded("zip 엔트리 이름 거부: '${e.name}'")
                if (++entries > ImportLimits.ZIP_MAX_ENTRIES) throw ImportLimitExceeded("zip 엔트리 수 > ${ImportLimits.ZIP_MAX_ENTRIES}")
                if (!e.isDirectory) {
                    val bos = ByteArrayOutputStream()
                    var size = 0L
                    while (true) {
                        val n = zip.read(buf)
                        if (n < 0) break
                        size += n; total += n
                        if (size > ImportLimits.ZIP_MAX_ENTRY_BYTES) throw ImportLimitExceeded("zip 엔트리 '${e.name}' 압축 해제 > ${ImportLimits.ZIP_MAX_ENTRY_BYTES} B")
                        if (total > ImportLimits.ZIP_MAX_TOTAL_BYTES) throw ImportLimitExceeded("zip 총 압축 해제 > ${ImportLimits.ZIP_MAX_TOTAL_BYTES} B")
                        bos.write(buf, 0, n)
                    }
                    out[e.name] = bos.toByteArray()
                }
                zip.closeEntry(); e = zip.nextEntry
            }
        }
        return out
    }

    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && bytes[2].toInt() == 3 && bytes[3].toInt() == 4
}

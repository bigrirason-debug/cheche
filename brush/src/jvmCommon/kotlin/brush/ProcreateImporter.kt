package io.github.bigrirason_debug.platen.brush

/**
 * Procreate `.brush`(zip 1개) / `.brushset`(폴더 여러 개 + `brushset.plist`) 임포터(brush-formats §1).
 * - 폴더 = 브러시 1개 규칙으로 한 코드가 둘 다 처리(§5 1항). `Reset/`·`__MACOSX/` 제외(§1.1).
 * - 순서: `brushset.plist` 의 `brushes` 배열(XML/바이너리 모두 수용), 없으면 폴더명 정렬.
 * - 아카이브 해독 실패 → 해당 브러시만 "팁만 복구" + note.
 */
object ProcreateImporter {
    private const val ARCHIVE = "Brush.archive"

    private class Folder(val prefix: String, val files: Map<String, ByteArray>)

    fun import(fileName: String, bytes: ByteArray, importedAt: Long): ImportResult {
        val files = SafeZip.read(bytes)
        val notes = ArrayList<String>()
        val base = fileName.substringAfterLast('/').substringBeforeLast('.')
        val folders = groupFolders(files, notes)
        if (folders.isEmpty()) {
            // 중첩 zip 1단계: .brushset 안에 .brush zip 이 통째로 들어 있는 변형(관대)
            val nested = files.filter { it.key.endsWith(".brush", ignoreCase = true) && SafeZip.isZip(it.value) }
            if (nested.isNotEmpty()) {
                notes += "중첩 .brush zip ${nested.size}개(1단계) 처리"
                val out = ArrayList<LoadedBrush>()
                for ((name, zip) in nested.entries.sortedBy { it.key }) {
                    val inner = SafeZip.read(zip)
                    val innerFolders = groupFolders(inner, notes)
                    if (innerFolders.isEmpty()) { notes += "$name: Brush.archive 없음"; continue }
                    out += importFolder(innerFolders.first(), name.substringAfterLast('/').substringBeforeLast('.'), importedAt, notes)
                }
                return ImportResult(out, notes)
            }
            throw BrushFormatException("Brush.archive 가 없음(Procreate 브러시 아님)")
        }
        val brushes = folders.map { f -> importFolder(f, if (f.prefix.isEmpty()) base else f.prefix.trimEnd('/').substringAfterLast('/'), importedAt, notes) }
        return ImportResult(brushes, notes)
    }

    /** 브러시 폴더 수집·정렬. 루트에 Brush.archive 가 있으면 단일 브러시. */
    private fun groupFolders(files: Map<String, ByteArray>, notes: MutableList<String>): List<Folder> {
        val byPrefix = LinkedHashMap<String, MutableMap<String, ByteArray>>()
        for ((name, data) in files) {
            if (name.startsWith("__MACOSX/") || name.split('/').any { it == "Reset" || it.startsWith("._") }) continue
            val slash = name.lastIndexOf('/')
            val prefix = if (slash < 0) "" else name.substring(0, slash + 1)
            val leaf = name.substring(slash + 1)
            byPrefix.getOrPut(prefix) { LinkedHashMap() }[leaf] = data
        }
        val folders = byPrefix.filter { it.value.keys.any { k -> k.equals(ARCHIVE, ignoreCase = true) } }.map { Folder(it.key, it.value) }
        if (folders.isEmpty()) return emptyList()
        if (folders.size == 1 && folders[0].prefix.isEmpty()) return folders
        // brushset.plist 순서
        val order = files.entries.firstOrNull { it.key.substringAfterLast('/').equals("brushset.plist", ignoreCase = true) }?.let { e ->
            try {
                val plist = parsePropertyList(e.value) as? Map<*, *>
                (plist?.get("brushes") as? List<*>)?.mapNotNull { it as? String }
            } catch (ex: Exception) { notes += "brushset.plist 해석 실패: ${ex.message}"; null }
        }
        return if (order != null) {
            val byName = folders.associateBy { it.prefix.trimEnd('/').substringAfterLast('/') }
            val sorted = ArrayList<Folder>()
            for (n in order) byName[n]?.let { sorted += it }
            for (f in folders) if (f !in sorted) { sorted += f; notes += "brushset.plist 에 없는 폴더 '${f.prefix}' 뒤에 추가" }
            sorted
        } else folders.sortedBy { it.prefix }
    }

    private fun importFolder(f: Folder, fallbackName: String, importedAt: Long, notes: MutableList<String>): LoadedBrush {
        fun find(vararg names: String): ByteArray? = names.firstNotNullOfOrNull { n -> f.files.entries.firstOrNull { it.key.equals(n, ignoreCase = true) }?.value }
        val archiveBytes = find(ARCHIVE)!!
        val archive: Map<String, Any?>? = try {
            decodeNSKeyedArchive(archiveBytes)
        } catch (e: ImportLimitExceeded) {
            throw e
        } catch (e: Exception) {
            notes += "$fallbackName: Brush.archive 해독 실패(${e.message}) → 팁만 복구"; null
        }
        val shape = find("Shape.png", "shape.png", "Shape.jpg")?.let { decode(it, "$fallbackName/Shape", notes) }
        val grain = find("Grain.png", "grain.png", "Grain.jpg", "grain.jpg", "Grain.jpeg")?.let { decode(it, "$fallbackName/Grain", notes) }
        return ProcreateMapper.map(archive, shape, grain, fallbackName, importedAt)
    }

    private fun decode(bytes: ByteArray, label: String, notes: MutableList<String>): GrayImage? = try {
        ImageCodec.decodeToGray(bytes)
    } catch (e: ImportLimitExceeded) {
        throw e
    } catch (e: Exception) {
        notes += "$label 디코드 실패: ${e.message}"; null
    }
}

package io.github.bigrirason_debug.platen.brush

/** plist UID(NSKeyedArchiver 객체 참조). 플랫폼 파서가 이 타입으로 변환해 넘긴다. */
data class PlistUid(val index: Int)

/** plist 날짜(epoch ms). */
data class PlistDate(val epochMillis: Long)

/**
 * 바이너리/XML plist 를 Kotlin 값 트리로 파싱(desktop/android = dd-plist).
 * 반환 타입: Map<String, Any?> · List<Any?> · String · Long · Double · Boolean · ByteArray · [PlistUid] · [PlistDate].
 */
expect fun parsePropertyList(bytes: ByteArray): Any?

/**
 * NSKeyedArchiver 아카이브(bplist) → 평탄한 루트 객체 사전(brush-formats §1.2).
 * `$top.root` 부터 UID 를 `$objects` 로 치환한다(`$objects[1]` 하드코딩 금지).
 */
expect fun decodeNSKeyedArchive(bytes: ByteArray): Map<String, Any?>

/**
 * NSKeyedArchiver 해제(플랫폼 무관 부분). 입력은 [parsePropertyList] 의 트리.
 * - 깊이 ≤ [ImportLimits.PLIST_MAX_DEPTH], `$objects` ≤ [ImportLimits.PLIST_MAX_OBJECTS], UID 순환 감지(해석 중 스택).
 * - NSArray/NSSet(`NS.objects`) → List, NSDictionary(`NS.keys`+`NS.objects`) → Map, NSDate(`NS.time`) → Double(초),
 *   NSMutableData(`NS.data`) → ByteArray, NSMutableString(`NS.string`) → String. 그 외 객체는 `$class` 를 `$classname` 문자열로 바꾼 Map.
 * - 알 수 없는 키는 보존(§1.4 파서 정책).
 */
object NsKeyedUnarchiver {
    private const val NULL_MARKER = "\$null"

    fun unarchive(tree: Any?): Map<String, Any?> {
        val root = tree as? Map<*, *> ?: throw BrushFormatException("plist 루트가 사전이 아님")
        if (root["\$archiver"] != "NSKeyedArchiver") throw BrushFormatException("\$archiver != NSKeyedArchiver: ${root["\$archiver"]}")
        val objects = root["\$objects"] as? List<*> ?: throw BrushFormatException("\$objects 없음")
        if (objects.size > ImportLimits.PLIST_MAX_OBJECTS) throw ImportLimitExceeded("\$objects ${objects.size} > ${ImportLimits.PLIST_MAX_OBJECTS}")
        val top = root["\$top"] as? Map<*, *> ?: throw BrushFormatException("\$top 없음")
        val rootRef = top["root"] ?: throw BrushFormatException("\$top.root 없음")
        val ctx = Ctx(objects)
        val resolved = ctx.resolve(rootRef, 0)
        @Suppress("UNCHECKED_CAST")
        return resolved as? Map<String, Any?> ?: throw BrushFormatException("루트 객체가 사전이 아님: ${resolved?.let { it::class.simpleName }}")
    }

    private class Ctx(val objects: List<*>) {
        /** 현재 해석 스택의 UID(순환 감지). */
        val inProgress = HashSet<Int>()

        fun resolve(v: Any?, depth: Int): Any? {
            if (depth > ImportLimits.PLIST_MAX_DEPTH) throw ImportLimitExceeded("NSKeyedArchiver 깊이 > ${ImportLimits.PLIST_MAX_DEPTH}")
            return when (v) {
                is PlistUid -> {
                    val idx = v.index
                    if (idx < 0 || idx >= objects.size) throw BrushFormatException("UID $idx 범위 밖(objects=${objects.size})")
                    if (!inProgress.add(idx)) throw BrushFormatException("UID 순환: $idx")
                    try { resolve(objects[idx], depth + 1) } finally { inProgress.remove(idx) }
                }
                is Map<*, *> -> resolveDict(v, depth)
                is List<*> -> v.map { resolve(it, depth + 1) }
                NULL_MARKER -> null
                else -> v
            }
        }

        private fun resolveDict(d: Map<*, *>, depth: Int): Any? {
            val classRef = d["\$class"]
            val className = classNameOf(classRef)
            // 컬렉션 특수형
            if (d.containsKey("NS.keys") && d.containsKey("NS.objects")) {
                val keys = (d["NS.keys"] as? List<*> ?: emptyList<Any?>()).map { resolve(it, depth + 1) }
                val vals = (d["NS.objects"] as? List<*> ?: emptyList<Any?>()).map { resolve(it, depth + 1) }
                val out = LinkedHashMap<String, Any?>()
                for (i in keys.indices) out[keys[i].toString()] = vals.getOrNull(i)
                return out
            }
            if (d.containsKey("NS.objects") && classRef != null) return (d["NS.objects"] as? List<*> ?: emptyList<Any?>()).map { resolve(it, depth + 1) }
            if (d.containsKey("NS.time") && d.size <= 2) return d["NS.time"]
            if (d.containsKey("NS.data") && d.size <= 2) return d["NS.data"]
            if (d.containsKey("NS.string") && d.size <= 2) return d["NS.string"]
            val out = LinkedHashMap<String, Any?>()
            for ((k, value) in d) {
                val key = k.toString()
                if (key == "\$class") continue
                out[key] = resolve(value, depth + 1)
            }
            if (className != null) out["\$classname"] = className
            return out
        }

        private fun classNameOf(ref: Any?): String? {
            val dict = when (ref) {
                is PlistUid -> objects.getOrNull(ref.index) as? Map<*, *>
                is Map<*, *> -> ref
                else -> null
            } ?: return null
            return dict["\$classname"] as? String
        }
    }
}

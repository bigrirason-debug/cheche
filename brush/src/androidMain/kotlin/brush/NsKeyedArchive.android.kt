package io.github.bigrirason_debug.platen.brush

import com.dd.plist.NSArray
import com.dd.plist.NSData
import com.dd.plist.NSDate
import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSObject
import com.dd.plist.NSSet
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.dd.plist.UID

/** dd-plist(MIT) 로 bplist/XML plist 파싱 → Kotlin 값 트리. desktop 과 동일 구현(CI 전용 컴파일). */
actual fun parsePropertyList(bytes: ByteArray): Any? {
    val root = try { PropertyListParser.parse(bytes) } catch (e: Exception) { throw BrushFormatException("plist 파싱 실패: ${e.message}", e) }
    return toKotlin(root, 0)
}

actual fun decodeNSKeyedArchive(bytes: ByteArray): Map<String, Any?> = NsKeyedUnarchiver.unarchive(parsePropertyList(bytes))

private fun toKotlin(o: NSObject?, depth: Int): Any? {
    if (depth > ImportLimits.PLIST_MAX_DEPTH) throw ImportLimitExceeded("plist 깊이 > ${ImportLimits.PLIST_MAX_DEPTH}")
    return when (o) {
        null -> null
        is NSDictionary -> {
            val m = LinkedHashMap<String, Any?>()
            for ((k, v) in o.hashMap) m[k] = toKotlin(v, depth + 1)
            m
        }
        is NSArray -> o.array.map { toKotlin(it, depth + 1) }
        is NSSet -> o.allObjects().map { toKotlin(it, depth + 1) }
        is NSString -> o.content
        is NSNumber -> when {
            o.isBoolean -> o.boolValue()
            o.isInteger -> o.longValue()
            else -> o.doubleValue()
        }
        is NSData -> o.bytes()
        is NSDate -> PlistDate(o.date.time)
        is UID -> {
            var v = 0L
            for (x in o.bytes) v = (v shl 8) or (x.toLong() and 0xFF)
            if (v > Int.MAX_VALUE) throw BrushFormatException("UID 범위 초과")
            PlistUid(v.toInt())
        }
        else -> o.toString()
    }
}

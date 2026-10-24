package io.github.bigrirason_debug.platen.brush

/**
 * Photoshop Action Descriptor(버전 16) 파서 — brush-formats §2.2 "디스크립터 포맷 원리"(공식 6.0 스펙 Table 2-62 + MIT 파서 구조 참조, 코드 복사 없음).
 * 관대 정책: 길이를 아는 타입(`tdta`/`alis`/`Pth `)은 건너뛰고, 길이를 모르는 미지 OSType 은 [BrushFormatException] →
 * 호출자가 "팁만 복구" 모드(§7 축소안)로 전환한다.
 *
 * 값 표현: Objc/GlbO → [Desc], VlLs → List<Any?>, doub → Double, UntF → [UnitValue], TEXT → String, enum → [EnumValue],
 * long → Long, comp → Long, bool → Boolean, type/GlbC → [ClassValue], obj  → [Reference], tdta/alis/Pth → ByteArray.
 */
class Desc(val className: String, val classId: String, val items: LinkedHashMap<String, Any?>) {
    operator fun get(key: String): Any? = items[key] ?: items[key.trimEnd()] ?: items.entries.firstOrNull { it.key.trimEnd() == key.trimEnd() }?.value
    fun has(key: String): Boolean = get(key) != null
}

data class UnitValue(val unit: String, val value: Double)
data class EnumValue(val type: String, val value: String)
data class ClassValue(val name: String, val classId: String)
class Reference(val items: List<Any?>)

object AbrDescriptor {
    /** 디스크립터 1개(클래스명·클래스 ID·항목)를 읽는다. */
    fun read(r: ByteReader, depth: Int = 0): Desc {
        if (depth > 32) throw ImportLimitExceeded("디스크립터 깊이 > 32")
        val name = unicode(r)
        val classId = key(r)
        val count = r.i32()
        if (count < 0 || count > 100_000) throw BrushFormatException("디스크립터 항목 수 이상: $count")
        val items = LinkedHashMap<String, Any?>()
        for (i in 0 until count) {
            val k = key(r)
            val type = r.ascii(4)
            items[k] = value(r, type, depth)
        }
        return Desc(name, classId, items)
    }

    /** `u32 len` + UTF-16BE(끝 null 포함). */
    fun unicode(r: ByteReader): String {
        val n = r.i32()
        if (n < 0) throw BrushFormatException("유니코드 길이 음수")
        return r.utf16(n)
    }

    /** `u32 len`, 0 이면 4자 코드, 아니면 len 바이트 ASCII. */
    fun key(r: ByteReader): String {
        val n = r.i32()
        return when {
            n == 0 -> r.ascii(4)
            n in 1..4096 -> r.ascii(n)
            else -> throw BrushFormatException("키 길이 이상: $n")
        }
    }

    fun value(r: ByteReader, type: String, depth: Int): Any? = when (type) {
        "Objc", "GlbO" -> read(r, depth + 1)
        "VlLs" -> {
            val n = r.i32()
            if (n < 0 || n > 100_000) throw BrushFormatException("리스트 길이 이상: $n")
            List(n) { value(r, r.ascii(4), depth + 1) }
        }
        "doub" -> r.f64()
        "UntF" -> UnitValue(r.ascii(4), r.f64())
        "TEXT" -> unicode(r)
        "enum" -> EnumValue(key(r), key(r))
        "long" -> r.i32().toLong()
        "comp" -> r.i64()
        "bool" -> r.u8() != 0
        "type", "GlbC" -> ClassValue(unicode(r), key(r))
        "tdta", "alis", "Pth " -> { val n = r.i32(); if (n < 0) throw BrushFormatException("데이터 길이 음수"); r.bytes(n) }
        "UnFl" -> { val unit = r.ascii(4); val n = r.i32(); if (n < 0 || n > 100_000) throw BrushFormatException("UnFl 길이 이상"); List(n) { UnitValue(unit, r.f64()) } }
        "obj " -> reference(r, depth)
        // 추정: ObAr(object array) 등 구조를 모르는 타입은 길이를 알 수 없어 실패 → 팁만 복구
        else -> throw BrushFormatException("알 수 없는 디스크립터 타입 '$type' @${r.pos}")
    }

    /** Reference 구조(공식 6.0 스펙 Table 2-64 기준, **추정** 포함): 항목 수 + (형식 4자 + 본문). */
    private fun reference(r: ByteReader, depth: Int): Reference {
        val n = r.i32()
        if (n < 0 || n > 10_000) throw BrushFormatException("참조 항목 수 이상: $n")
        val items = ArrayList<Any?>(n)
        repeat(n) {
            when (val form = r.ascii(4)) {
                "prop" -> { unicode(r); key(r); items += key(r) }
                "Clss" -> items += ClassValue(unicode(r), key(r))
                "Enmr" -> { unicode(r); key(r); items += EnumValue(key(r), key(r)) }
                "rele" -> { unicode(r); key(r); items += r.i32().toLong() }
                "Idnt", "indx" -> { unicode(r); key(r); items += r.i32().toLong() }
                "name" -> { unicode(r); key(r); items += unicode(r) }
                else -> throw BrushFormatException("알 수 없는 참조 형식 '$form'")
            }
        }
        return Reference(items)
    }

    // ---- 값 접근 헬퍼

    fun number(v: Any?): Double? = when (v) {
        is UnitValue -> v.value
        is Double -> v
        is Long -> v.toDouble()
        is Int -> v.toDouble()
        is Boolean -> if (v) 1.0 else 0.0
        else -> null
    }

    fun text(v: Any?): String? = v as? String

    fun bool(v: Any?): Boolean = when (v) { is Boolean -> v; is Number -> v.toInt() != 0; else -> false }
}

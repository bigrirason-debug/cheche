package io.github.bigrirason_debug.platen.brush

/** 빅엔디언 바이트 리더(ABR). 범위를 벗어나면 [BrushFormatException]. */
class ByteReader(val data: ByteArray, var pos: Int = 0, val limit: Int = data.size) {
    val remaining: Int get() = limit - pos

    fun need(n: Int) {
        if (n < 0 || pos + n > limit) throw BrushFormatException("읽기 범위 초과: pos=$pos need=$n limit=$limit")
    }

    fun u8(): Int { need(1); return data[pos++].toInt() and 0xFF }
    fun i8(): Int = u8().toByte().toInt()
    fun u16(): Int { need(2); val v = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF); pos += 2; return v }
    fun i16(): Int = u16().toShort().toInt()
    fun i32(): Int {
        need(4)
        val v = ((data[pos].toInt() and 0xFF) shl 24) or ((data[pos + 1].toInt() and 0xFF) shl 16) or ((data[pos + 2].toInt() and 0xFF) shl 8) or (data[pos + 3].toInt() and 0xFF)
        pos += 4; return v
    }
    fun u32(): Long = i32().toLong() and 0xFFFFFFFFL
    fun i64(): Long = (u32() shl 32) or u32()
    fun f64(): Double = Double.fromBits(i64())
    fun bytes(n: Int): ByteArray { need(n); val out = data.copyOfRange(pos, pos + n); pos += n; return out }
    fun ascii(n: Int): String { need(n); val sb = StringBuilder(n); repeat(n) { sb.append((data[pos++].toInt() and 0xFF).toChar()) }; return sb.toString() }
    fun skip(n: Int) { need(n); pos += n }
    fun peekAscii(n: Int): String? = if (pos + n <= limit) String(CharArray(n) { (data[pos + it].toInt() and 0xFF).toChar() }) else null

    /** UTF-16BE 문자열([units] 코드 유닛), 끝 null 제거. */
    fun utf16(units: Int): String {
        if (units < 0 || units > remaining / 2) throw BrushFormatException("UTF-16 길이 이상: $units")
        val chars = CharArray(units) { u16().toChar() }
        var end = units
        while (end > 0 && chars[end - 1] == '\u0000') end--
        return chars.concatToString(0, end)
    }

    /** 하위 리더(현재 위치부터 [len] 바이트). */
    fun sub(len: Int): ByteReader { need(len); val r = ByteReader(data, pos, pos + len); pos += len; return r }
}

/** PackBits(Apple/TIFF RLE) 디코드. 출력이 [expected] 바이트에 못 미치면 0 으로 채운다(관대). */
object PackBits {
    fun decode(r: ByteReader, expected: Int): ByteArray {
        val out = ByteArray(expected)
        var o = 0
        while (o < expected && r.remaining > 0) {
            val n = r.i8()
            when {
                n >= 0 -> { val len = minOf(n + 1, expected - o); val avail = minOf(len, r.remaining); for (i in 0 until avail) out[o++] = r.data[r.pos + i]; r.skip(avail); if (avail < len) break }
                n == -128 -> {}
                else -> { val v = r.u8().toByte(); val len = minOf(1 - n, expected - o); for (i in 0 until len) out[o++] = v }
            }
        }
        return out
    }
}

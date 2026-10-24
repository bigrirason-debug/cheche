package io.github.bigrirason_debug.platen.brush

/**
 * 순수 Kotlin PNG 인코더(그레이 8-bit, zlib stored 블록). 압축하지 않는 이유: 플랫폼 zlib 구현 차이 없이
 * 바이트가 결정적이어야 내장 브러시 id(zip SHA-1)가 Android/데스크톱에서 같다(COUNCIL-REVIEW 좌석 1 #7 와 같은 우려).
 * 브러시 zip 자체가 다시 deflate 되므로 패키지 크기 손해는 없다.
 */
object PngEncoder {
    private val SIGNATURE = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)

    private val crcTable: IntArray = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else (c ushr 1) }
        c
    }

    fun crc32(bytes: ByteArray, from: Int = 0, to: Int = bytes.size): Int {
        var c = -1
        for (i in from until to) c = crcTable[(c xor bytes[i].toInt()) and 0xFF] xor (c ushr 8)
        return c.inv()
    }

    fun adler32(bytes: ByteArray): Int {
        var a = 1; var b = 0
        for (x in bytes) { a = (a + (x.toInt() and 0xFF)) % 65521; b = (b + a) % 65521 }
        return (b shl 16) or a
    }

    fun encodeGray(img: GrayImage): ByteArray {
        val raw = ByteArray((img.width + 1) * img.height)
        for (y in 0 until img.height) {
            raw[y * (img.width + 1)] = 0 // 필터 None
            img.alpha.copyInto(raw, y * (img.width + 1) + 1, y * img.width, (y + 1) * img.width)
        }
        val out = ByteWriter()
        out.bytes(SIGNATURE)
        val ihdr = ByteWriter()
        ihdr.u32(img.width); ihdr.u32(img.height)
        ihdr.u8(8); ihdr.u8(0); ihdr.u8(0); ihdr.u8(0); ihdr.u8(0)
        chunk(out, "IHDR", ihdr.toByteArray())
        chunk(out, "IDAT", zlibStored(raw))
        chunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun zlibStored(data: ByteArray): ByteArray {
        val w = ByteWriter()
        w.u8(0x78); w.u8(0x01)
        var off = 0
        do {
            val len = minOf(65535, data.size - off)
            val last = off + len >= data.size
            w.u8(if (last) 1 else 0)
            w.u8(len and 0xFF); w.u8((len ushr 8) and 0xFF)
            val nlen = len.inv() and 0xFFFF
            w.u8(nlen and 0xFF); w.u8((nlen ushr 8) and 0xFF)
            w.bytes(data, off, len)
            off += len
        } while (!last)
        w.u32(adler32(data))
        return w.toByteArray()
    }

    private fun chunk(out: ByteWriter, type: String, data: ByteArray) {
        out.u32(data.size)
        val typeAndData = ByteArray(4 + data.size)
        for (i in 0 until 4) typeAndData[i] = type[i].code.toByte()
        data.copyInto(typeAndData, 4)
        out.bytes(typeAndData)
        out.u32(crc32(typeAndData))
    }
}

/** 간단한 성장 바이트 버퍼(빅엔디언 정수). */
class ByteWriter {
    private var buf = ByteArray(256)
    var size = 0
        private set

    private fun ensure(n: Int) {
        if (size + n > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, size + n))
    }

    fun u8(v: Int) { ensure(1); buf[size++] = v.toByte() }
    fun u16(v: Int) { u8(v ushr 8); u8(v) }
    fun u32(v: Int) { u8(v ushr 24); u8(v ushr 16); u8(v ushr 8); u8(v) }
    fun i64(v: Long) { u32((v ushr 32).toInt()); u32(v.toInt()) }
    fun f64(v: Double) { i64(v.toBits()) }
    fun bytes(b: ByteArray, off: Int = 0, len: Int = b.size) { ensure(len); b.copyInto(buf, size, off, off + len); size += len }
    fun ascii(s: String) { for (c in s) u8(c.code) }
    fun toByteArray(): ByteArray = buf.copyOf(size)
}

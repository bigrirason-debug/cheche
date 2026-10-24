package io.github.bigrirason_debug.platen.brush

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.sqrt

/** 합성 픽스처(실제 상용 파일 없음). */
object TestFixtures {
    /** 흰 원(255) on 검정(0) 그레이 이미지. */
    fun circle(size: Int, radiusFrac: Double = 0.4, value: Int = 255): GrayImage {
        val out = ByteArray(size * size)
        val c = (size - 1) / 2.0; val r = size * radiusFrac
        for (y in 0 until size) for (x in 0 until size) {
            val d = sqrt((x - c) * (x - c) + (y - c) * (y - c))
            out[y * size + x] = (if (d <= r) value else 0).toByte()
        }
        return GrayImage(size, size, out)
    }

    fun zip(entries: Map<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            for ((n, b) in entries) { z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() }
        }
        return bos.toByteArray()
    }

    /** PackBits 인코더(행 단위): 상수 행은 반복 청크, 그 외 리터럴 청크. */
    fun packBits(row: ByteArray): ByteArray {
        val w = ByteWriter()
        if (row.all { it == row[0] }) {
            var left = row.size
            while (left > 0) { val n = minOf(128, left); w.u8((1 - n) and 0xFF); w.u8(row[0].toInt() and 0xFF); left -= n }
        } else {
            var off = 0
            while (off < row.size) { val n = minOf(128, row.size - off); w.u8(n - 1); w.bytes(row, off, n); off += n }
        }
        return w.toByteArray()
    }

    /** RLE 이미지 본문: 행별 u16 길이표 + PackBits 데이터(ABR v1/v6 공통). */
    fun rleBody(img: GrayImage): ByteArray {
        val rows = (0 until img.height).map { y -> packBits(img.alpha.copyOfRange(y * img.width, (y + 1) * img.width)) }
        val w = ByteWriter()
        for (r in rows) w.u16(r.size)
        for (r in rows) w.bytes(r)
        return w.toByteArray()
    }

    // ---- 디스크립터 인코딩 헬퍼(테스트 전용 미니 라이터)
    fun unicode(w: ByteWriter, s: String) { w.u32(s.length + 1); for (c in s) w.u16(c.code); w.u16(0) }
    fun key(w: ByteWriter, k: String) { if (k.length == 4) { w.u32(0); w.ascii(k) } else { w.u32(k.length); w.ascii(k) } }

    class DescWriter(val className: String = "", val classId: String = "null") {
        val items = ArrayList<Pair<String, (ByteWriter) -> Unit>>()
        fun text(k: String, v: String) = apply { items += k to { w -> w.ascii("TEXT"); unicode(w, v) } }
        fun untf(k: String, unit: String, v: Double) = apply { items += k to { w -> w.ascii("UntF"); w.ascii(unit); w.f64(v) } }
        fun doub(k: String, v: Double) = apply { items += k to { w -> w.ascii("doub"); w.f64(v) } }
        fun long(k: String, v: Int) = apply { items += k to { w -> w.ascii("long"); w.u32(v) } }
        fun bool(k: String, v: Boolean) = apply { items += k to { w -> w.ascii("bool"); w.u8(if (v) 1 else 0) } }
        fun enum(k: String, type: String, v: String) = apply { items += k to { w -> w.ascii("enum"); key(w, type); key(w, v) } }
        fun objc(k: String, d: DescWriter) = apply { items += k to { w -> w.ascii("Objc"); d.writeTo(w) } }
        fun list(k: String, vararg ds: DescWriter) = apply { items += k to { w -> w.ascii("VlLs"); w.u32(ds.size); for (d in ds) { w.ascii("Objc"); d.writeTo(w) } } }
        /** 알 수 없는 타입(길이 불명) 삽입. */
        fun unknown(k: String) = apply { items += k to { w -> w.ascii("ObAr"); w.u32(16); w.u32(0) } }
        fun writeTo(w: ByteWriter) {
            unicode(w, className); key(w, classId); w.u32(items.size)
            for ((k, f) in items) { key(w, k); f(w) }
        }
    }

    fun block(key: String, data: ByteArray): ByteArray {
        val w = ByteWriter(); w.ascii("8BIM"); w.ascii(key); w.u32(data.size); w.bytes(data)
        var pad = (4 - data.size % 4) % 4; while (pad-- > 0) w.u8(0)
        return w.toByteArray()
    }

    const val UUID_A = "11111111-2222-3333-4444-555555555555"

    /** v6 samp 레코드(서브버전 2 → 264 B 미지 필드). */
    fun sampRecord(id: String, img: GrayImage, subversion: Int = 2, right: Int = img.width, depth: Int = 8): ByteArray {
        val body = ByteWriter()
        body.u8(id.length); body.ascii(id); repeat(36 - id.length) { body.u8(0) }
        repeat(if (subversion == 1) 10 else 264) { body.u8(0) }
        body.u32(0); body.u32(0); body.u32(img.height); body.u32(right)
        body.u16(depth); body.u8(1)
        body.bytes(rleBody(img))
        val b = body.toByteArray()
        val w = ByteWriter(); w.u32(b.size); w.bytes(b)
        var pad = (4 - b.size % 4) % 4; while (pad-- > 0) w.u8(0)
        return w.toByteArray()
    }

    fun descBlock(root: DescWriter): ByteArray {
        val w = ByteWriter(); w.u32(16); root.writeTo(w)
        return block("desc", w.toByteArray())
    }
}

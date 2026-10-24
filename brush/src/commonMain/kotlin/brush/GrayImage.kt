package io.github.bigrirason_debug.platen.brush

/**
 * 8-bit 단일 채널 이미지(brush-formats §3.5: 팁·그레인은 alpha 만 가진다). alpha[y*width+x], 0 = 투명, 255 = 칠함.
 */
class GrayImage(val width: Int, val height: Int, val alpha: ByteArray) {
    init {
        require(width > 0 && height > 0) { "빈 이미지: ${width}x$height" }
        require(alpha.size == width * height) { "alpha 크기 불일치: ${alpha.size} != ${width * height}" }
    }

    /** 픽셀 값 0..255 */
    fun get(x: Int, y: Int): Int = alpha[y * width + x].toInt() and 0xFF

    fun copy(): GrayImage = GrayImage(width, height, alpha.copyOf())

    companion object {
        fun filled(width: Int, height: Int, value: Int = 0): GrayImage =
            GrayImage(width, height, ByteArray(width * height) { value.toByte() })
    }
}

/** 플랫폼 디코더가 돌려주는 중간 표현: ARGB 8888(Int) 픽셀. hasAlpha = 원본에 알파 채널이 있었는가. */
class ArgbImage(val width: Int, val height: Int, val argb: IntArray, val hasAlpha: Boolean)

/** 플랫폼 이미지 디코더(desktop = javax.imageio, android = BitmapFactory). 크기 검사는 [ImageCodec] 이 먼저 한다. */
expect fun platformDecodeArgb(bytes: ByteArray): ArgbImage

/**
 * PNG/JPEG 바이트 ↔ [GrayImage]. 인코딩은 순수 Kotlin(결정적 바이트 → 내장 브러시 id 가 플랫폼 간 동일).
 */
object ImageCodec {
    /** PNG IHDR / JPEG SOF 에서 (폭, 높이)를 읽는다. 모르는 포맷이면 null. */
    fun probeSize(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size >= 24 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()) {
            if (bytes[12] == 'I'.code.toByte() && bytes[13] == 'H'.code.toByte() && bytes[14] == 'D'.code.toByte() && bytes[15] == 'R'.code.toByte()) {
                return u32be(bytes, 16) to u32be(bytes, 20)
            }
            return null
        }
        if (bytes.size >= 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
            // JPEG: 마커를 걸어가며 SOF0/1/2(FFC0/C1/C2) 의 높이·폭을 읽는다.
            var p = 2
            while (p + 9 < bytes.size) {
                if (bytes[p] != 0xFF.toByte()) { p++; continue }
                val marker = bytes[p + 1].toInt() and 0xFF
                if (marker == 0xFF) { p++; continue }
                if (marker == 0xD8 || marker in 0xD0..0xD7 || marker == 0x01) { p += 2; continue }
                val len = ((bytes[p + 2].toInt() and 0xFF) shl 8) or (bytes[p + 3].toInt() and 0xFF)
                if (marker == 0xC0 || marker == 0xC1 || marker == 0xC2) {
                    val h = ((bytes[p + 5].toInt() and 0xFF) shl 8) or (bytes[p + 6].toInt() and 0xFF)
                    val w = ((bytes[p + 7].toInt() and 0xFF) shl 8) or (bytes[p + 8].toInt() and 0xFF)
                    return w to h
                }
                if (marker == 0xDA) return null
                p += 2 + len
            }
            return null
        }
        return null
    }

    /** 헤더 크기 검사(디코드 전) → 플랫폼 디코드 → 그레이 변환(§3.5 1항). */
    fun decodeToGray(bytes: ByteArray): GrayImage {
        if (bytes.size > ImportLimits.IMAGE_MAX_BYTES) throw ImportLimitExceeded("이미지 바이트 ${bytes.size} > ${ImportLimits.IMAGE_MAX_BYTES}")
        probeSize(bytes)?.let { (w, h) ->
            if (w > ImportLimits.IMAGE_MAX_SIDE || h > ImportLimits.IMAGE_MAX_SIDE || w <= 0 || h <= 0) {
                throw ImportLimitExceeded("이미지 헤더 ${w}x$h > ${ImportLimits.IMAGE_MAX_SIDE}")
            }
        }
        val img = platformDecodeArgb(bytes)
        if (img.width > ImportLimits.IMAGE_MAX_SIDE || img.height > ImportLimits.IMAGE_MAX_SIDE) {
            throw ImportLimitExceeded("디코드 크기 ${img.width}x${img.height} > ${ImportLimits.IMAGE_MAX_SIDE}")
        }
        return toGray(img)
    }

    /**
     * ARGB → alpha. 규칙(§3.5 1항): alpha = luminance × srcAlpha. 색 채널이 거의 평탄(휘도 범위 < 16)하고
     * 알파가 있으면 알파 채널을 그대로 채택(단색 PNG 셰이프 — chahui 휴리스틱).
     */
    fun toGray(img: ArgbImage): GrayImage {
        val n = img.width * img.height
        val out = ByteArray(n)
        var lumMin = 255; var lumMax = 0
        val lum = IntArray(n)
        for (i in 0 until n) {
            val p = img.argb[i]
            val r = (p ushr 16) and 0xFF; val g = (p ushr 8) and 0xFF; val b = p and 0xFF
            val l = (r * 299 + g * 587 + b * 114 + 500) / 1000
            lum[i] = l
            val a = (p ushr 24) and 0xFF
            if (!img.hasAlpha || a > 0) { if (l < lumMin) lumMin = l; if (l > lumMax) lumMax = l }
        }
        val flatColor = img.hasAlpha && (lumMax - lumMin) < 16
        for (i in 0 until n) {
            val a = if (img.hasAlpha) (img.argb[i] ushr 24) and 0xFF else 255
            out[i] = (if (flatColor) a else (lum[i] * a + 127) / 255).toByte()
        }
        return GrayImage(img.width, img.height, out)
    }

    /** 8-bit 그레이 PNG(색상 타입 0). 결정적 바이트. */
    fun encodePng(img: GrayImage): ByteArray = PngEncoder.encodeGray(img)

    private fun u32be(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)
}

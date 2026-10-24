package io.github.bigrirason_debug.platen.brush

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 셰이프·그레인 정규화(brush-formats §3.5). 모든 연산은 [GrayImage] 를 입력받아 새 이미지를 돌려준다.
 * 임포트 시점에 반전·밝기/대비·트림·리사이즈를 전부 구워 넣어 런타임은 alpha 팁/타일만 알게 한다(§5 5항).
 */
object ImageNormalizer {
    /** 펜류 팁 긴 변 상한(§3.5 3항). */
    const val TIP_MAX_PEN = 256
    /** 질감류 팁(size.max > 64) 긴 변 상한(§3.5 3항, §3.7 성능 상한). */
    const val TIP_MAX_TEXTURE = 512
    /** 그레인 타일 한 변 상한(§3.5 4항). */
    const val GRAIN_MAX = 512
    /** 트림 후 패딩 px(§3.5 3항). */
    const val TIP_PADDING = 1

    /** size.max(pt) 에 따른 팁 상한. */
    fun tipLimitFor(sizeMax: Float): Int = if (sizeMax > 64f) TIP_MAX_TEXTURE else TIP_MAX_PEN

    /** 반전 굽기: 255 − v(§3.5 2항). */
    fun invert(img: GrayImage): GrayImage {
        val out = ByteArray(img.alpha.size) { (255 - (img.alpha[it].toInt() and 0xFF)).toByte() }
        return GrayImage(img.width, img.height, out)
    }

    /**
     * 밝기/대비 굽기. brightness ∈ −0.75..0.75, contrast ∈ −1..1(Procreate textureBrightness/Contrast 범위, §1.3).
     * 공식은 **추정**(§3.3 "대비 공식 추정"): v' = (v−0.5)·(1+contrast) + 0.5 + brightness, 0..1 클램프.
     */
    fun brightnessContrast(img: GrayImage, brightness: Float, contrast: Float): GrayImage {
        if (brightness == 0f && contrast == 0f) return img
        val gain = 1f + contrast.coerceIn(-1f, 1f)
        val lut = ByteArray(256) { v ->
            val f = ((v / 255f - 0.5f) * gain + 0.5f + brightness).coerceIn(0f, 1f)
            (f * 255f + 0.5f).toInt().toByte()
        }
        return GrayImage(img.width, img.height, ByteArray(img.alpha.size) { lut[img.alpha[it].toInt() and 0xFF] })
    }

    /** alpha > threshold 인 바운딩 박스. 전부 투명이면 null. */
    fun bounds(img: GrayImage, threshold: Int = 0): IntArray? {
        var x0 = img.width; var y0 = img.height; var x1 = -1; var y1 = -1
        for (y in 0 until img.height) for (x in 0 until img.width) {
            if (img.get(x, y) > threshold) {
                if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
            }
        }
        return if (x1 < 0) null else intArrayOf(x0, y0, x1 - x0 + 1, y1 - y0 + 1)
    }

    fun crop(img: GrayImage, x: Int, y: Int, w: Int, h: Int): GrayImage {
        val out = ByteArray(w * h)
        for (yy in 0 until h) img.alpha.copyInto(out, yy * w, (y + yy) * img.width + x, (y + yy) * img.width + x + w)
        return GrayImage(w, h, out)
    }

    /** 바운딩 박스로 트림 + 사방 [pad] px 투명 패딩. 전부 투명이면 입력 반환. */
    fun trim(img: GrayImage, pad: Int = TIP_PADDING): GrayImage {
        val b = bounds(img) ?: return img
        val inner = crop(img, b[0], b[1], b[2], b[3])
        return place(inner, inner.width + 2 * pad, inner.height + 2 * pad)
    }

    /** [w]×[h] 투명 캔버스 중앙에 배치(캔버스가 더 작으면 중앙 크롭). */
    fun place(img: GrayImage, w: Int, h: Int): GrayImage {
        val out = ByteArray(w * h)
        val dx = (w - img.width) / 2; val dy = (h - img.height) / 2
        for (y in 0 until img.height) {
            val ty = y + dy
            if (ty !in 0 until h) continue
            for (x in 0 until img.width) {
                val tx = x + dx
                if (tx in 0 until w) out[ty * w + tx] = img.alpha[y * img.width + x]
            }
        }
        return GrayImage(w, h, out)
    }

    /** 정사각 캔버스(긴 변) 중앙 배치. */
    fun squareCenter(img: GrayImage): GrayImage {
        val s = max(img.width, img.height)
        return if (img.width == s && img.height == s) img else place(img, s, s)
    }

    /** 중앙 정사각 크롭(짧은 변). */
    fun squareCrop(img: GrayImage): GrayImage {
        val s = min(img.width, img.height)
        return if (img.width == s && img.height == s) img else crop(img, (img.width - s) / 2, (img.height - s) / 2, s, s)
    }

    /**
     * 박스 필터(면적 평균) 리사이즈. 축소 전용으로 설계했으나 확대 시에도 동작(겹치는 소스 영역 평균 = 최근접에 가깝다).
     * 정수/실수 연산만 써서 플랫폼 간 결정적.
     */
    fun resizeBox(img: GrayImage, newW: Int, newH: Int): GrayImage {
        require(newW > 0 && newH > 0)
        if (newW == img.width && newH == img.height) return img
        val out = ByteArray(newW * newH)
        val sx = img.width.toDouble() / newW
        val sy = img.height.toDouble() / newH
        for (oy in 0 until newH) {
            val y0 = oy * sy; val y1 = (oy + 1) * sy
            val iy0 = y0.toInt(); val iy1 = min(img.height - 1, (y1 - 1e-9).toInt())
            for (ox in 0 until newW) {
                val x0 = ox * sx; val x1 = (ox + 1) * sx
                val ix0 = x0.toInt(); val ix1 = min(img.width - 1, (x1 - 1e-9).toInt())
                var sum = 0.0; var area = 0.0
                for (y in iy0..iy1) {
                    val wy = min(y + 1.0, y1) - max(y.toDouble(), y0)
                    if (wy <= 0) continue
                    for (x in ix0..ix1) {
                        val wx = min(x + 1.0, x1) - max(x.toDouble(), x0)
                        if (wx <= 0) continue
                        sum += img.get(x, y) * wx * wy; area += wx * wy
                    }
                }
                out[oy * newW + ox] = (if (area > 0) (sum / area + 0.5).toInt() else 0).coerceIn(0, 255).toByte()
            }
        }
        return GrayImage(newW, newH, out)
    }

    /** 긴 변이 [maxSide] 를 넘으면 비율 유지 축소. */
    fun fitLongSide(img: GrayImage, maxSide: Int): GrayImage {
        val long = max(img.width, img.height)
        if (long <= maxSide) return img
        val scale = maxSide.toDouble() / long
        return resizeBox(img, max(1, (img.width * scale).roundToInt()), max(1, (img.height * scale).roundToInt()))
    }

    /** 셰이프 규격(§3.5 3항): 트림+1px 패딩 → 정사각 중앙 → 긴 변 ≤ [maxSide]. */
    fun normalizeTip(img: GrayImage, maxSide: Int): GrayImage = fitLongSide(squareCenter(trim(img)), maxSide)

    /** 그레인 규격(§3.5 4항): 중앙 정사각 크롭 → ≤ [GRAIN_MAX]. */
    fun normalizeGrain(img: GrayImage, maxSide: Int = GRAIN_MAX): GrayImage = fitLongSide(squareCrop(img), maxSide)

    /**
     * 원형·경계 선명도 추정(brush-formats §3.1 "90%/50% 반경 비교", chahui 방식).
     * 중심에서 16방향으로 alpha 가 최대의 50%/90% 아래로 떨어지는 반경을 재서
     * (a) r50 의 방향별 편차가 작고(min/max ≥ 0.85) (b) r90/r50 ≥ 0.6(가장자리가 비교적 단단) 이면 true.
     */
    fun isRoundHardTip(img: GrayImage): Boolean {
        val cx = (img.width - 1) / 2.0; val cy = (img.height - 1) / 2.0
        var peak = 0
        for (v in img.alpha) peak = max(peak, v.toInt() and 0xFF)
        if (peak < 32) return false
        val t50 = peak * 0.5; val t90 = peak * 0.9
        val maxR = min(img.width, img.height) / 2.0
        var r50Min = Double.MAX_VALUE; var r50Max = 0.0; var r90Sum = 0.0; var r50Sum = 0.0
        val dirs = 16
        for (d in 0 until dirs) {
            val ang = d * 2.0 * kotlin.math.PI / dirs
            val dx = kotlin.math.cos(ang); val dy = kotlin.math.sin(ang)
            var r50 = -1.0; var r90 = -1.0
            var r = 0.0
            while (r <= maxR) {
                val x = (cx + dx * r).roundToInt(); val y = (cy + dy * r).roundToInt()
                if (x !in 0 until img.width || y !in 0 until img.height) break
                val v = img.get(x, y)
                if (r90 < 0 && v < t90) r90 = r
                if (v < t50) { r50 = r; break }
                r += 0.5
            }
            if (r50 < 0) r50 = r
            if (r90 < 0) r90 = r50
            r50Min = min(r50Min, r50); r50Max = max(r50Max, r50); r90Sum += r90; r50Sum += r50
        }
        if (r50Max <= 0.0) return false
        val uniform = r50Min / r50Max >= 0.85
        val hard = (r90Sum / dirs) / (r50Sum / dirs) >= 0.6
        return uniform && hard
    }

    /** 평균 밝기 0..1(그레인 진단용). */
    fun mean(img: GrayImage): Float {
        var s = 0L
        for (v in img.alpha) s += (v.toInt() and 0xFF)
        return s.toFloat() / (img.alpha.size * 255f)
    }

    /** 거리 유틸(팁 합성에서 사용). */
    internal fun dist(x: Double, y: Double): Double = sqrt(x * x + y * y)
}

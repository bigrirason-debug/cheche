package io.github.bigrirason_debug.platen.brush

import kotlin.math.sqrt

/**
 * 절차적 팁/그레인(내장 브러시·번들 셰이프 대체용). 초월함수 없이 정수 해시 + 사칙연산 + sqrt 만 써서
 * 어떤 플랫폼에서도 같은 바이트가 나온다(내장 브러시 id 결정성).
 */
object ProceduralImages {
    /**
     * 원형(타원) 팁. [hardness] 0..1: 1 = 경계 선명, 0 = 중심부터 페더. [roundness] 0.01..1 세로 비율.
     * 페더 구간은 smoothstep(3t²−2t³) 으로 감쇠.
     */
    fun roundTip(size: Int = 64, hardness: Float = 0.9f, roundness: Float = 1f): GrayImage {
        val out = ByteArray(size * size)
        val c = (size - 1) / 2.0
        val radius = size / 2.0 - 1.0
        val h = hardness.coerceIn(0f, 0.999f).toDouble()
        val ry = roundness.coerceIn(0.01f, 1f).toDouble()
        for (y in 0 until size) for (x in 0 until size) {
            val dx = (x - c) / radius
            val dy = (y - c) / (radius * ry)
            val r = sqrt(dx * dx + dy * dy)
            val a = when {
                r <= h -> 1.0
                r >= 1.0 -> 0.0
                else -> { val t = 1.0 - (r - h) / (1.0 - h); t * t * (3.0 - 2.0 * t) }
            }
            out[y * size + x] = (a * 255.0 + 0.5).toInt().coerceIn(0, 255).toByte()
        }
        return GrayImage(size, size, out)
    }

    /** 정수 해시(xorshift·곱셈 혼합). 시드 고정 → 결정적. */
    private fun hash(x: Int, y: Int, seed: Int): Int {
        var h = x * 374761393 + y * 668265263 + seed * 1274126177
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return h and 0xFFFF
    }

    /**
     * 값 노이즈 그레인 타일(주기적 → 타일링 가능). 4 옥타브, 격자 8·16·32·64 셀. 결과 평균 ≈ 0.5, 범위 0..1 로 정규화.
     */
    fun noiseGrain(size: Int = 256, seed: Int = 1, octaves: Int = 4): GrayImage {
        val acc = DoubleArray(size * size)
        var amp = 1.0; var totalAmp = 0.0
        var cells = 8
        for (o in 0 until octaves) {
            val cell = size / cells
            for (y in 0 until size) for (x in 0 until size) {
                val gx = x / cell; val gy = y / cell
                val fx = (x - gx * cell).toDouble() / cell; val fy = (y - gy * cell).toDouble() / cell
                val sx = fx * fx * (3 - 2 * fx); val sy = fy * fy * (3 - 2 * fy)
                val gx1 = (gx + 1) % cells; val gy1 = (gy + 1) % cells
                val v00 = hash(gx, gy, seed + o) / 65535.0; val v10 = hash(gx1, gy, seed + o) / 65535.0
                val v01 = hash(gx, gy1, seed + o) / 65535.0; val v11 = hash(gx1, gy1, seed + o) / 65535.0
                val v = (v00 * (1 - sx) + v10 * sx) * (1 - sy) + (v01 * (1 - sx) + v11 * sx) * sy
                acc[y * size + x] += v * amp
            }
            totalAmp += amp; amp *= 0.5; cells *= 2
            if (size / cells < 1) break
        }
        var mn = Double.MAX_VALUE; var mx = -Double.MAX_VALUE
        for (v in acc) { if (v < mn) mn = v; if (v > mx) mx = v }
        val range = if (mx > mn) mx - mn else 1.0
        val out = ByteArray(size * size) { i -> (((acc[i] - mn) / range) * 255.0 + 0.5).toInt().coerceIn(0, 255).toByte() }
        return GrayImage(size, size, out)
    }
}

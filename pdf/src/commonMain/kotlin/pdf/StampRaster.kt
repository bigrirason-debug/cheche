package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.ink.StampPlan
import io.github.bigrirason_debug.platen.model.Rect
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** 8-bit 알파 팁 이미지(brush 모듈이 정규화해 제공, brush-formats §3.5). 행 우선, 0=투명 255=불투명. */
class GrayTip(val width: Int, val height: Int, val alpha: ByteArray) {
    init { require(alpha.size == width * height) }
}

/** stamp 엔진 획을 투명 ARGB 래스터로 합성한다(PDF 이미지 XObject 용, PLAN §4.7 래스터 레이어). */
internal object StampRaster {
    /** 메모리 상한: 이 픽셀 수를 넘으면 해상도를 절반씩 낮춘다(4096² ≈ 64MB ARGB). */
    private const val MAX_PIXELS = 4096L * 4096L

    /** 소프트 디스크 가장자리 폭 = max(0.75px, 반지름의 25%) — 경도 ~0.75 의 둥근 기본 팁 근사(팁 이미지 없을 때). */
    private fun softEdge(r: Float) = max(0.75f, r * 0.25f)

    class Result(val image: RasterImage, val rect: Rect)

    fun render(plan: StampPlan, color: Int, pxPerPt: Float, tip: GrayTip?): Result? {
        if (plan.stamps.isEmpty()) return null
        // 경계: 회전된 정사각 팁까지 덮도록 반지름×√2 + 1pt 여유
        var l = Float.POSITIVE_INFINITY; var t = Float.POSITIVE_INFINITY
        var r = Float.NEGATIVE_INFINITY; var b = Float.NEGATIVE_INFINITY
        for (s in plan.stamps) {
            val h = s.diameter * 0.5f * sqrt(2f) + 1f
            l = min(l, s.x - h); t = min(t, s.y - h); r = max(r, s.x + h); b = max(b, s.y + h)
        }
        if (!(l.isFinite() && t.isFinite() && r.isFinite() && b.isFinite())) return null
        var scale = pxPerPt
        var x0: Int; var y0: Int; var w: Int; var h: Int
        while (true) {
            x0 = floor(l * scale).toInt(); y0 = floor(t * scale).toInt()
            w = ceil(r * scale).toInt() - x0; h = ceil(b * scale).toInt() - y0
            if (w.toLong() * h.toLong() <= MAX_PIXELS || scale <= 0.0625f) break
            scale *= 0.5f
        }
        if (w <= 0 || h <= 0) return null
        val acc = FloatArray(w * h)
        for (s in plan.stamps) {
            val cx = s.x * scale - x0; val cy = s.y * scale - y0
            val rad = s.diameter * 0.5f * scale
            if (rad <= 0f || s.alpha <= 0f) continue
            if (tip == null) disc(acc, w, h, cx, cy, rad, s.alpha)
            else stampTip(acc, w, h, cx, cy, rad, s.alpha, s.angleRad, tip)
        }
        val rgb = color and 0x00FFFFFF
        val argb = IntArray(w * h) { i ->
            val a = (acc[i].coerceIn(0f, 1f) * 255f).roundToInt()
            (a shl 24) or rgb
        }
        return Result(RasterImage(w, h, argb), Rect(x0 / scale, y0 / scale, (x0 + w) / scale, (y0 + h) / scale))
    }

    private fun blend(acc: FloatArray, idx: Int, a: Float) {
        if (a <= 0f) return
        val d = acc[idx]; acc[idx] = d + a - d * a // source-over(같은 색이므로 알파만)
    }

    private fun disc(acc: FloatArray, w: Int, h: Int, cx: Float, cy: Float, rad: Float, alpha: Float) {
        val edge = softEdge(rad)
        val outer = rad + 0.5f
        val inner = rad - edge
        val px0 = max(0, floor(cx - outer).toInt()); val px1 = min(w - 1, ceil(cx + outer).toInt())
        val py0 = max(0, floor(cy - outer).toInt()); val py1 = min(h - 1, ceil(cy + outer).toInt())
        for (py in py0..py1) for (px in px0..px1) {
            val dx = px + 0.5f - cx; val dy = py + 0.5f - cy
            val dist = sqrt(dx * dx + dy * dy)
            val cov = when {
                dist <= inner -> 1f
                dist >= outer -> 0f
                else -> { val u = (outer - dist) / (outer - inner); u * u * (3f - 2f * u) } // smoothstep
            }
            blend(acc, py * w + px, cov * alpha)
        }
    }

    private fun stampTip(acc: FloatArray, w: Int, h: Int, cx: Float, cy: Float, rad: Float, alpha: Float, angle: Float, tip: GrayTip) {
        val half = rad * sqrt(2f) + 1f
        val px0 = max(0, floor(cx - half).toInt()); val px1 = min(w - 1, ceil(cx + half).toInt())
        val py0 = max(0, floor(cy - half).toInt()); val py1 = min(h - 1, ceil(cy + half).toInt())
        val c = cos(-angle); val s = sin(-angle)
        for (py in py0..py1) for (px in px0..px1) {
            val dx = px + 0.5f - cx; val dy = py + 0.5f - cy
            val u = dx * c - dy * s; val v = dx * s + dy * c // 역회전 → 팁 로컬 좌표
            if (u < -rad || u >= rad || v < -rad || v >= rad) continue
            val tx = ((u / rad + 1f) * 0.5f * tip.width).toInt().coerceIn(0, tip.width - 1)
            val ty = ((v / rad + 1f) * 0.5f * tip.height).toInt().coerceIn(0, tip.height - 1)
            val ta = (tip.alpha[ty * tip.width + tx].toInt() and 255) / 255f
            blend(acc, py * w + px, ta * alpha)
        }
    }
}

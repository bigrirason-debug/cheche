package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Stroke
import kotlin.math.sqrt

/** [StrokeHitTest] 기본 구현. */
object DefaultHitTest {
    /** 점 p 가 중심선 세그먼트까지의 거리 ≤ (보간 반지름 + tolerance) 인가. */
    fun hits(stroke: Stroke, params: ResolvedInkParams, p: Pt, tolerance: Float, geometry: StrokeGeometry): Boolean {
        if (stroke.samples.isEmpty()) return false
        // 반지름은 속도·틸트 배율(각 ≤2)로 최대 2·size 까지 커질 수 있다 → 보수적 사전 거부.
        if (!stroke.bounds(2f * params.size + tolerance).contains(p)) return false
        val center = geometry.smooth(stroke.samples, params)
        if (center.isEmpty()) return false
        if (center.size == 1) {
            val c = center[0]
            val d = dist(p.x, p.y, c.x, c.y)
            return d <= c.radius + tolerance
        }
        for (i in 0 until center.size - 1) {
            val a = center[i]; val b = center[i + 1]
            val vx = b.x - a.x; val vy = b.y - a.y
            val len2 = vx * vx + vy * vy
            val u = if (len2 > 0f) (((p.x - a.x) * vx + (p.y - a.y) * vy) / len2).coerceIn(0f, 1f) else 0f
            val qx = a.x + vx * u; val qy = a.y + vy * u
            val r = a.radius + (b.radius - a.radius) * u
            if (dist(p.x, p.y, qx, qy) <= r + tolerance) return true
        }
        return false
    }

    /** 짝수-홀수 규칙으로 샘플의 과반(> 50%)이 올가미 안에 있는가. */
    fun insideLasso(stroke: Stroke, lasso: List<Pt>): Boolean {
        if (lasso.size < 3 || stroke.samples.isEmpty()) return false
        var inside = 0
        for (s in stroke.samples) if (pointInPolygon(s.x, s.y, lasso)) inside++
        return inside * 2 > stroke.samples.size
    }

    private fun pointInPolygon(x: Float, y: Float, poly: List<Pt>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val pi = poly[i]; val pj = poly[j]
            if ((pi.y > y) != (pj.y > y)) {
                val xi = pj.x + (y - pj.y) * (pi.x - pj.x) / (pi.y - pj.y)
                if (x < xi) inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun dist(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val dx = x1 - x0; val dy = y1 - y0
        return sqrt(dx * dx + dy * dy)
    }
}

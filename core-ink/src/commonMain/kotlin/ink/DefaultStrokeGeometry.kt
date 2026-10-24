package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.StrokeCap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 결정적 xorshift64 — 플랫폼 RNG 에 의존하지 않아 Android/Windows 에서 같은 스탬프 배치를 낸다. */
internal class XorShift(seed: Long) {
    private var s: Long = if (seed == 0L) 0x9E3779B97F4A7C15uL.toLong() else seed

    fun next(): Float {
        s = s xor (s shl 13)
        s = s xor (s ushr 7)
        s = s xor (s shl 17)
        return ((s ushr 40).toInt() and 0xFFFFFF).toFloat() / 16777216f
    }

    fun nextInt(bound: Int): Int = (next() * bound).toInt().coerceIn(0, bound - 1)
}

/** [StrokeGeometry] 기본 구현(순수 함수, 상태 없음). */
object DefaultStrokeGeometry : StrokeGeometry {
    /** 둥근 캡 반원 분할 수(캡당 점 CAP_SEGMENTS−1 개 추가). */
    const val CAP_SEGMENTS = 8
    /** 점 하나짜리 획(dot) 폴리곤 분할 수. */
    const val DOT_SEGMENTS = 16
    /** 스탬프 간격 하한(지름 비율) — brush-formats §3.7 성능 상한. */
    const val MIN_SPACING = 0.03f
    /** 획 하나당 스탬프 수 상한(메모리 방어; 초과 시 간격을 늘린다). */
    const val MAX_STAMPS = 100_000
    private const val DEG = (PI / 180).toFloat()
    private const val TWO_PI = (2 * PI).toFloat()

    override fun smooth(samples: List<InputSample>, params: ResolvedInkParams): List<PathPoint> {
        if (samples.isEmpty()) return emptyList()
        val streamlined = StrokePipeline.streamlineAll(samples, params.streamline)
        val cl = StrokePipeline.buildCenterline(streamlined, params.stabilize)
        val n = cl.size
        val total = cl.total()
        val es = StrokePipeline.endSpeed(cl, total, params.taperEndLength)
        return List(n) { StrokePipeline.finalize(cl, it, params, total, es) }
    }

    override fun outline(center: List<PathPoint>, params: ResolvedInkParams): Outline {
        val n = center.size
        if (n == 0) return Outline(center, FloatArray(0), Rect.EMPTY)
        val cap = params.spec.cap
        if (n == 1) return dotOutline(center, cap)

        val capExtra = when (cap) { StrokeCap.round -> CAP_SEGMENTS - 1; StrokeCap.square -> 2; StrokeCap.flat -> 0 }
        val poly = FloatArray((2 * n + 2 * capExtra) * 2)
        var w = 0
        fun put(x: Float, y: Float) { poly[w++] = x; poly[w++] = y }

        val first = center[0]
        val last = center[n - 1]
        // 시작 캡: 우측(−n) → 뒤쪽(−dir) → 좌측(+n)
        when (cap) {
            StrokeCap.round -> {
                val nx = -first.dirY; val ny = first.dirX
                for (k in 1 until CAP_SEGMENTS) {
                    val th = PI.toFloat() * k / CAP_SEGMENTS
                    val c = cos(th); val s = sin(th)
                    put(first.x + first.radius * (-nx * c - first.dirX * s), first.y + first.radius * (-ny * c - first.dirY * s))
                }
            }
            StrokeCap.square -> {
                val nx = -first.dirY; val ny = first.dirX
                put(first.x - nx * first.radius - first.dirX * first.radius, first.y - ny * first.radius - first.dirY * first.radius)
                put(first.x + nx * first.radius - first.dirX * first.radius, first.y + ny * first.radius - first.dirY * first.radius)
            }
            StrokeCap.flat -> {}
        }
        // 좌측 순방향
        for (i in 0 until n) {
            val p = center[i]
            put(p.x - p.dirY * p.radius, p.y + p.dirX * p.radius)
        }
        // 끝 캡: 좌측(+n) → 앞쪽(+dir) → 우측(−n)
        when (cap) {
            StrokeCap.round -> {
                val nx = -last.dirY; val ny = last.dirX
                for (k in 1 until CAP_SEGMENTS) {
                    val th = PI.toFloat() * k / CAP_SEGMENTS
                    val c = cos(th); val s = sin(th)
                    put(last.x + last.radius * (nx * c + last.dirX * s), last.y + last.radius * (ny * c + last.dirY * s))
                }
            }
            StrokeCap.square -> {
                val nx = -last.dirY; val ny = last.dirX
                put(last.x + nx * last.radius + last.dirX * last.radius, last.y + ny * last.radius + last.dirY * last.radius)
                put(last.x - nx * last.radius + last.dirX * last.radius, last.y - ny * last.radius + last.dirY * last.radius)
            }
            StrokeCap.flat -> {}
        }
        // 우측 역방향
        for (i in n - 1 downTo 0) {
            val p = center[i]
            put(p.x + p.dirY * p.radius, p.y - p.dirX * p.radius)
        }
        return Outline(center, poly, boundsOf(poly, center))
    }

    private fun dotOutline(center: List<PathPoint>, cap: StrokeCap): Outline {
        val p = center[0]
        val r = p.radius
        val poly: FloatArray
        if (cap == StrokeCap.round) {
            poly = FloatArray(DOT_SEGMENTS * 2)
            for (k in 0 until DOT_SEGMENTS) {
                val th = TWO_PI * k / DOT_SEGMENTS
                poly[2 * k] = p.x + r * cos(th); poly[2 * k + 1] = p.y + r * sin(th)
            }
        } else {
            val nx = -p.dirY; val ny = p.dirX
            poly = floatArrayOf(
                p.x - p.dirX * r - nx * r, p.y - p.dirY * r - ny * r,
                p.x - p.dirX * r + nx * r, p.y - p.dirY * r + ny * r,
                p.x + p.dirX * r + nx * r, p.y + p.dirY * r + ny * r,
                p.x + p.dirX * r - nx * r, p.y + p.dirY * r - ny * r,
            )
        }
        return Outline(center, poly, boundsOf(poly, center))
    }

    private fun boundsOf(poly: FloatArray, center: List<PathPoint>): Rect {
        var l = Float.POSITIVE_INFINITY; var t = Float.POSITIVE_INFINITY
        var r = Float.NEGATIVE_INFINITY; var b = Float.NEGATIVE_INFINITY
        var i = 0
        while (i < poly.size) {
            val x = poly[i]; val y = poly[i + 1]
            if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > b) b = y
            i += 2
        }
        for (p in center) {
            if (p.x - p.radius < l) l = p.x - p.radius; if (p.x + p.radius > r) r = p.x + p.radius
            if (p.y - p.radius < t) t = p.y - p.radius; if (p.y + p.radius > b) b = p.y + p.radius
        }
        return if (l.isFinite()) Rect(l, t, r, b) else Rect.EMPTY
    }

    override fun stampPlan(center: List<PathPoint>, params: ResolvedInkParams, seed: Long): StampPlan {
        val n = center.size
        if (n == 0) return StampPlan(emptyList(), Rect.EMPTY)
        val st = params.spec.stamp
        val tip = st.tip
        val rng = XorShift(seed)
        val startAngle = if (tip.randomStart) rng.next() * TWO_PI else 0f
        val count = tip.count.coerceIn(1, 4)
        val flow = params.spec.flow.coerceIn(0f, 1f)
        val jitterAmp = st.jitter.coerceAtLeast(0f) * params.size * 0.5f
        val sizeJ = st.sizeJitter.coerceIn(0f, 1f)
        val opJ = st.opacityJitter.coerceIn(0f, 1f)
        val baseAngle = tip.angle * DEG
        val scatter = tip.scatterAngle * DEG

        val out = ArrayList<StampInstance>()
        var l = Float.POSITIVE_INFINITY; var t = Float.POSITIVE_INFINITY
        var r = Float.NEGATIVE_INFINITY; var b = Float.NEGATIVE_INFINITY

        fun emit(x0: Float, y0: Float, radius: Float, alpha: Float, dx: Float, dy: Float) {
            val nx = -dy; val ny = dx
            repeat(count) {
                val rj = rng.next(); val rs = rng.next(); val ro = rng.next(); val ra = rng.next(); val rv = rng.nextInt(8)
                val lat = (rj * 2f - 1f) * jitterAmp
                val x = x0 + nx * lat; val y = y0 + ny * lat
                val d = 2f * radius * (1f - sizeJ * rs)
                val a = (flow * alpha * (1f - opJ * ro)).coerceIn(0f, 1f)
                val ang = tip.followStroke * atan2(dy, dx) + baseAngle + scatter * (ra * 2f - 1f) + startAngle
                out.add(StampInstance(x, y, d, a, ang, rv))
                val h = d * 0.5f
                if (x - h < l) l = x - h; if (x + h > r) r = x + h; if (y - h < t) t = y - h; if (y + h > b) b = y + h
            }
        }

        if (n == 1) {
            val p = center[0]
            emit(p.x, p.y, p.radius, p.alpha, p.dirX, p.dirY)
            return StampPlan(out, Rect(l, t, r, b))
        }

        // 누적 호장
        val cum = FloatArray(n)
        for (i in 1 until n) {
            val dx = center[i].x - center[i - 1].x; val dy = center[i].y - center[i - 1].y
            cum[i] = cum[i - 1] + sqrt(dx * dx + dy * dy)
        }
        val total = cum[n - 1]
        // 간격 = spacing × base 지름(현재 반지름이 아님 → 스탬프 수 상한 고정, §3.7)
        var spacingPx = maxOf(st.spacing, MIN_SPACING) * params.size
        if (!(spacingPx > 1e-3f)) spacingPx = 1e-3f
        if (total / spacingPx > MAX_STAMPS) spacingPx = total / MAX_STAMPS

        var s = 0f
        var i = 0
        val eps = spacingPx * 1e-3f
        while (s <= total + eps) {
            while (i < n - 2 && cum[i + 1] < s) i++
            val a0 = center[i]; val a1 = center[i + 1]
            val seg = cum[i + 1] - cum[i]
            val u = if (seg > 0f) ((s - cum[i]) / seg).coerceIn(0f, 1f) else 0f
            val x = a0.x + (a1.x - a0.x) * u
            val y = a0.y + (a1.y - a0.y) * u
            val rad = a0.radius + (a1.radius - a0.radius) * u
            val al = a0.alpha + (a1.alpha - a0.alpha) * u
            var dx = a0.dirX + (a1.dirX - a0.dirX) * u
            var dy = a0.dirY + (a1.dirY - a0.dirY) * u
            val len = sqrt(dx * dx + dy * dy)
            if (len > 1e-6f) { dx /= len; dy /= len } else { dx = a0.dirX; dy = a0.dirY }
            emit(x, y, rad, al, dx, dy)
            s += spacingPx
        }
        return StampPlan(out, if (l.isFinite()) Rect(l, t, r, b) else Rect.EMPTY)
    }
}

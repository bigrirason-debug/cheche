package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.CurvePoint
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * 커브 보간·LUT 유틸(public — brush 모듈 임포터가 재사용).
 * 제어점 커브(brush-formats §3.4: 2~8점, 단조, 끝점 (0,0)/(1,1) 고정)를 모노톤 큐빅(Fritsch–Carlson)으로
 * 보간해 256-엔트리 LUT 로 샘플링한다.
 */
object Interp {
    /** 런타임 LUT 크기(brush-formats §3.4). */
    const val LUT_SIZE: Int = 256

    /** 같은 x 로 간주하는 제어점 간격(이보다 가까우면 뒤 점이 앞 점을 대체). */
    private const val X_EPS = 1e-4f

    /**
     * 제어점 단조 보정: x 로 정렬 → 0..1 클램프 → x 중복 제거 → y 단조 증가 강제 → 끝점 (0,0)/(1,1) 고정.
     * 입력이 비거나 전부 범위 밖이면 선형 [(0,0),(1,1)] 을 돌려준다.
     */
    fun sanitizeCurve(points: List<CurvePoint>): List<CurvePoint> {
        val interior = points
            .asSequence()
            .filter { it.x.isFinite() && it.y.isFinite() }
            .map { CurvePoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
            .filter { it.x > X_EPS && it.x < 1f - X_EPS }
            .sortedBy { it.x }
            .toList()
        val out = ArrayList<CurvePoint>(interior.size + 2)
        out.add(CurvePoint(0f, 0f))
        for (p in interior) {
            val last = out.last()
            val y = maxOf(p.y, last.y)
            if (p.x - last.x < X_EPS) {
                if (out.size > 1) out[out.lastIndex] = CurvePoint(p.x, y)
            } else {
                out.add(CurvePoint(p.x, y))
            }
        }
        out.add(CurvePoint(1f, 1f))
        return out
    }

    /** 단조 보정된 제어점으로 모노톤 큐빅 보간기를 만든다. */
    fun monotoneCubic(points: List<CurvePoint>): MonotoneCubic {
        val s = sanitizeCurve(points)
        return MonotoneCubic(FloatArray(s.size) { s[it].x }, FloatArray(s.size) { s[it].y })
    }

    /** 제어점 → [size]-엔트리 LUT(입력 0..1 균등 샘플). */
    fun buildLut(points: List<CurvePoint>, size: Int = LUT_SIZE): FloatArray {
        require(size >= 2) { "LUT size must be >= 2" }
        val f = monotoneCubic(points)
        val inv = 1f / (size - 1)
        return FloatArray(size) { f.eval(it * inv).coerceIn(0f, 1f) }
    }

    /** 항등 LUT. */
    fun identityLut(size: Int = LUT_SIZE): FloatArray = FloatArray(size) { it.toFloat() / (size - 1) }

    /** LUT 조회(엔트리 사이 선형 보간, 입력은 0..1 로 클램프). */
    fun lookup(lut: FloatArray, p: Float): Float {
        val n = lut.size
        if (n == 0) return p
        if (n == 1) return lut[0]
        val x = (if (p.isNaN()) 0f else p.coerceIn(0f, 1f)) * (n - 1)
        val i = floor(x).toInt().coerceIn(0, n - 2)
        val t = x - i
        return lut[i] + (lut[i + 1] - lut[i]) * t
    }

    /** 합성 LUT: result(p) = outer(inner(p)). 전역 입력 보정(inner) 뒤에 브러시 커브(outer)를 적용할 때 쓴다. */
    fun composeLut(outer: FloatArray, inner: FloatArray): FloatArray =
        FloatArray(inner.size) { lookup(outer, inner[it]) }

    /** amount 반영: 1 − amount + amount·curve(p) (brush-formats §3.2 "최소폭 = base×(1−amount)"). */
    fun applyAmount(lut: FloatArray, amount: Float): FloatArray {
        val a = amount.coerceIn(0f, 1f)
        return FloatArray(lut.size) { 1f - a + a * lut[it] }
    }
}

/**
 * Fritsch–Carlson 모노톤 큐빅 에르미트 보간. xs 는 엄격 증가, ys 는 단조여야 한다(Interp.sanitizeCurve 가 보장).
 * 구간 밖 x 는 끝 값으로 클램프.
 */
class MonotoneCubic(private val xs: FloatArray, private val ys: FloatArray) {
    private val tangents: FloatArray

    init {
        require(xs.size == ys.size && xs.size >= 2) { "need >= 2 points" }
        val n = xs.size
        val h = FloatArray(n - 1) { xs[it + 1] - xs[it] }
        val delta = FloatArray(n - 1) { (ys[it + 1] - ys[it]) / h[it] }
        val m = FloatArray(n)
        m[0] = delta[0]
        m[n - 1] = delta[n - 2]
        for (k in 1 until n - 1) {
            val d0 = delta[k - 1]
            val d1 = delta[k]
            m[k] = if (d0 * d1 <= 0f) 0f else (d0 + d1) * 0.5f
        }
        // 단조성 보장: α²+β² > 9 이면 접선 축소
        for (k in 0 until n - 1) {
            val d = delta[k]
            if (d == 0f) {
                m[k] = 0f; m[k + 1] = 0f
            } else {
                val a = m[k] / d
                val b = m[k + 1] / d
                val s = a * a + b * b
                if (s > 9f) {
                    val tau = 3f / sqrt(s)
                    m[k] = tau * a * d
                    m[k + 1] = tau * b * d
                }
            }
        }
        tangents = m
    }

    /** x 에서의 값. */
    fun eval(x: Float): Float {
        val n = xs.size
        if (x.isNaN()) return ys[0]
        if (x <= xs[0]) return ys[0]
        if (x >= xs[n - 1]) return ys[n - 1]
        // 이진 탐색으로 구간 찾기
        var lo = 0
        var hi = n - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (xs[mid] <= x) lo = mid else hi = mid
        }
        val h = xs[hi] - xs[lo]
        val t = (x - xs[lo]) / h
        val t2 = t * t
        val t3 = t2 * t
        val h00 = 2f * t3 - 3f * t2 + 1f
        val h10 = t3 - 2f * t2 + t
        val h01 = -2f * t3 + 3f * t2
        val h11 = t3 - t2
        return h00 * ys[lo] + h10 * h * tangents[lo] + h01 * ys[hi] + h11 * h * tangents[hi]
    }
}

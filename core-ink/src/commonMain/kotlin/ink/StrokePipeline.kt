package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.InputSample
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 스무딩 중간 표현(스트림라인/안정화 뒤의 중심선 점). tilt = |tilt| 라디안. */
internal class CenterPt(val x: Float, val y: Float, val p: Float, val tilt: Float, val t: Float)

/** 가변 Float 배열(commonMain 에서 박싱 없이 누적). */
internal class FloatList(initial: Int = 64) {
    private var a = FloatArray(maxOf(initial, 4))
    var size = 0
        private set

    operator fun get(i: Int): Float = a[i]
    fun add(v: Float) {
        if (size == a.size) a = a.copyOf(a.size * 2)
        a[size++] = v
    }
    fun truncate(n: Int) { if (n < size) size = maxOf(n, 0) }
    fun last(): Float = a[size - 1]
}

/**
 * 안정화된 중심선 + 누적 호장(cum, pt) + 점별 속도(speed, pt/s).
 * append 가 순차 의존(Δt≤0 → 이전 속도 유지)을 처리하므로 이후 단계는 인덱스별 순수 함수가 된다.
 */
internal class Centerline {
    val pts = ArrayList<CenterPt>()
    val cum = FloatList()
    val speed = FloatList()
    val size: Int get() = pts.size

    fun append(c: CenterPt) {
        if (pts.isEmpty()) {
            cum.add(0f); speed.add(0f)
        } else {
            val prev = pts[pts.size - 1]
            val dx = c.x - prev.x
            val dy = c.y - prev.y
            val d = sqrt(dx * dx + dy * dy)
            val dt = c.t - prev.t
            cum.add(cum.last() + d)
            // 샘플 t 는 ms → pt/s. Δt≤0(동일 타임스탬프·히스토리 역순)이면 이전 속도 유지.
            speed.add(if (dt > 0f) d / dt * 1000f else speed.last())
        }
        pts.add(c)
    }

    fun truncate(n: Int) {
        while (pts.size > n) pts.removeAt(pts.size - 1)
        cum.truncate(n); speed.truncate(n)
    }

    fun total(): Float = if (pts.isEmpty()) 0f else cum.last()
}

/**
 * smooth 파이프라인의 공통 단계. [DefaultStrokeGeometry.smooth](일괄)와 [DefaultLiveStrokeBuilder](증분)가
 * 같은 함수를 쓰므로 두 결과가 일치한다.
 */
internal object StrokePipeline {
    /** 정지/중복 샘플 제거 거리(pt). */
    const val DEDUPE_MIN_DIST = 0.05f
    /** 스트림라인 추종 계수: 새 점 = 이전 + (입력 − 이전)·(1 − streamline·GAIN). */
    const val STREAMLINE_GAIN = 0.9f
    /** stabilize 1.0 에서 이동평균 윈도 = 1 + 8. */
    const val STABILIZE_MAX_EXTRA = 8
    /** 증분 빌더가 pending 으로 두는 꼬리 길이 K = round(streamline·6)+1 의 계수. */
    const val PENDING_TAIL_PER_STREAMLINE = 6
    /**
     * taperAuto 끝 반지름 하한: 느린 끝 0.85, 빠른 끝 0.58 — 0 까지 가지 않음
     * (rea-samsung-notes §"기본 펜" 이전 프로젝트 실측 0.58~0.85).
     */
    const val AUTO_TAPER_SLOW_FLOOR = 0.85f
    const val AUTO_TAPER_FAST_FLOOR = 0.58f
    /** 속도·틸트 배율의 안전 범위(퇴화 방지). */
    private const val FACTOR_MIN = 0.15f
    private const val FACTOR_MAX = 2f
    private const val HALF_PI = (PI / 2).toFloat()

    fun keep(lastKept: InputSample?, s: InputSample): Boolean {
        if (lastKept == null) return true
        val dx = s.x - lastKept.x
        val dy = s.y - lastKept.y
        return dx * dx + dy * dy >= DEDUPE_MIN_DIST * DEDUPE_MIN_DIST
    }

    fun streamlineStep(prev: CenterPt?, s: InputSample, streamline: Float): CenterPt {
        val tilt = sqrt(s.tiltX * s.tiltX + s.tiltY * s.tiltY)
        val p = if (s.pressure.isNaN()) 0f else s.pressure.coerceIn(0f, 1f)
        if (prev == null || streamline <= 0f) return CenterPt(s.x, s.y, p, tilt, s.t)
        val k = 1f - streamline.coerceIn(0f, 1f) * STREAMLINE_GAIN
        return CenterPt(prev.x + (s.x - prev.x) * k, prev.y + (s.y - prev.y) * k, p, tilt, s.t)
    }

    fun stabilizeWindow(stabilize: Float): Int = 1 + (stabilize.coerceIn(0f, 1f) * STABILIZE_MAX_EXTRA).roundToInt()

    fun pendingTail(streamline: Float): Int = (streamline.coerceIn(0f, 1f) * PENDING_TAIL_PER_STREAMLINE).roundToInt() + 1

    /** 중심 이동평균(x,y 만; 윈도는 양 끝에서 잘림). half = window/2. */
    fun stabilizeAt(src: List<CenterPt>, i: Int, half: Int): CenterPt {
        val c = src[i]
        if (half <= 0) return c
        val lo = maxOf(0, i - half)
        val hi = minOf(src.size - 1, i + half)
        var sx = 0f; var sy = 0f
        for (j in lo..hi) { sx += src[j].x; sy += src[j].y }
        val n = (hi - lo + 1).toFloat()
        return CenterPt(sx / n, sy / n, c.p, c.tilt, c.t)
    }

    /** 원시 샘플 → 스트림라인 적용 점열(중복 제거 포함). */
    fun streamlineAll(samples: List<InputSample>, streamline: Float): ArrayList<CenterPt> {
        val out = ArrayList<CenterPt>(samples.size)
        var lastKept: InputSample? = null
        for (s in samples) {
            if (!keep(lastKept, s)) continue
            lastKept = s
            out.add(streamlineStep(out.lastOrNull(), s, streamline))
        }
        return out
    }

    fun buildCenterline(streamlined: List<CenterPt>, stabilize: Float): Centerline {
        val half = stabilizeWindow(stabilize) / 2
        val cl = Centerline()
        for (i in streamlined.indices) cl.append(stabilizeAt(streamlined, i, half))
        return cl
    }

    /** 끝 테이퍼 구간(마지막 taperEndLength pt)의 평균 속도(pt/s). */
    fun endSpeed(cl: Centerline, totalLen: Float, taperEndLength: Float): Float {
        val n = cl.size
        if (n < 2) return 0f
        val target = totalLen - taperEndLength
        var j = n - 2
        while (j > 0 && cl.cum[j] > target) j--
        val dt = cl.pts[n - 1].t - cl.pts[j].t
        return if (dt > 0f) (totalLen - cl.cum[j]) / dt * 1000f else cl.speed[n - 1]
    }

    /** 인덱스 i 의 최종 PathPoint. totalLen/endSpeed 는 끝 테이퍼 구간 안의 점에만 영향을 준다. */
    fun finalize(cl: Centerline, i: Int, params: ResolvedInkParams, totalLen: Float, endSpeed: Float): PathPoint {
        val n = cl.size
        val c = cl.pts[i]
        val spec = params.spec
        val tiltNorm = (c.tilt / HALF_PI).coerceIn(0f, 1f)

        var r = params.size * 0.5f * Interp.lookup(params.pressureSizeLut, c.p)
        if (spec.tilt.size != 0f) r *= (1f + spec.tilt.size * tiltNorm).coerceIn(FACTOR_MIN, FACTOR_MAX)
        if (n > 1) {
            // 속도: refSpeed(px/s)에서 효과 100% (brush-formats §3.2), 그 이상은 포화.
            if (spec.velocity.size != 0f && spec.velocity.refSpeed > 0f) {
                val norm = (cl.speed[i] / spec.velocity.refSpeed).coerceIn(0f, 1f)
                r *= (1f + spec.velocity.size * norm).coerceIn(FACTOR_MIN, FACTOR_MAX)
            }
            val s = cl.cum[i]
            // 시작 테이퍼: 선형 램프, 최소 비율 taper.startSize
            if (params.taperStartLength > 0f && s < params.taperStartLength) {
                val m = spec.taper.startSize.coerceIn(0f, 1f)
                r *= m + (1f - m) * (s / params.taperStartLength)
            }
            // 끝 테이퍼: 누적 호장 기준 램프. auto 면 끝 속도에 따라 0.58~0.85 하한.
            val remaining = totalLen - s
            if (params.taperEndLength > 0f && remaining < params.taperEndLength) {
                var m = spec.taper.endSize.coerceIn(0f, 1f)
                if (params.taperAuto) {
                    val ref = if (spec.velocity.refSpeed > 0f) spec.velocity.refSpeed else 1500f
                    val sn = (endSpeed / ref).coerceIn(0f, 1f)
                    val floor = AUTO_TAPER_SLOW_FLOOR + (AUTO_TAPER_FAST_FLOOR - AUTO_TAPER_SLOW_FLOOR) * sn
                    m = maxOf(m, floor)
                }
                r *= m + (1f - m) * (remaining.coerceAtLeast(0f) / params.taperEndLength)
            }
        }

        var a = params.opacity * Interp.lookup(params.pressureOpacityLut, c.p)
        // 틸트→불투명: 기울일수록 옅게(연필 셰이딩 근사; 가정).
        if (spec.tilt.opacity != 0f) a *= (1f - spec.tilt.opacity.coerceIn(0f, 1f) * tiltNorm).coerceAtLeast(0f)

        var dx: Float; var dy: Float
        if (n == 1) { dx = 1f; dy = 0f } else {
            val a0 = cl.pts[maxOf(0, i - 1)]
            val a1 = cl.pts[minOf(n - 1, i + 1)]
            dx = a1.x - a0.x; dy = a1.y - a0.y
            var len = sqrt(dx * dx + dy * dy)
            if (len <= 1e-9f) {
                val b = cl.pts[if (i > 0) i - 1 else minOf(n - 1, 1)]
                dx = if (i > 0) c.x - b.x else b.x - c.x
                dy = if (i > 0) c.y - b.y else b.y - c.y
                len = sqrt(dx * dx + dy * dy)
            }
            if (len <= 1e-9f) { dx = 1f; dy = 0f } else { dx /= len; dy /= len }
        }
        return PathPoint(c.x, c.y, r.coerceAtLeast(0f), a.coerceIn(0f, 1f), dx, dy, c.t)
    }
}

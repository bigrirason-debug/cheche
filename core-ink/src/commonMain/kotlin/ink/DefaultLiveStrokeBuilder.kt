package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.model.StrokeOverrides

/**
 * 증분 [LiveStrokeBuilder]. [DefaultStrokeGeometry.smooth] 와 같은 파이프라인을 점 단위로 진행한다.
 * - committed: 더 이상 변하지 않는 앞부분 = 마지막 K=round(streamline·6)+1 점, 안정화 윈도 반폭, 끝 테이퍼 구간을 제외한 것.
 * - pending: 나머지 꼬리. [pullToInput] 이 true 면 현재 원시 입력점까지 당긴 점을 하나 덧붙여 지연이 보이지 않게 한다.
 * - [finish] 는 원시 샘플을 그대로 보존한 [Stroke] 를 만든다(모델은 원시 보존, PLAN §4.3).
 * 다른 [StrokeGeometry] 구현이 주어지면 증분 없이 매번 그 구현의 smooth 를 호출한다(committed 는 비어 있음).
 */
class DefaultLiveStrokeBuilder(
    override val params: ResolvedInkParams,
    private val geometry: StrokeGeometry = DefaultStrokeGeometry,
) : LiveStrokeBuilder {
    /** pending 끝에 현재 입력점을 덧붙일지(젖은 잉크가 펜 끝에 닿게). */
    var pullToInput: Boolean = true

    /** [finish] 가 Stroke 에 기록할 사용자 오버라이드(ResolvedInkParams 는 원본 오버라이드를 들고 있지 않다). */
    var overrides: StrokeOverrides? = null

    private val raw = ArrayList<InputSample>()
    private var lastKept: InputSample? = null
    private val streamlined = ArrayList<CenterPt>()
    private val cl = Centerline()
    private val half = StrokePipeline.stabilizeWindow(params.stabilize) / 2
    private val tailK = StrokePipeline.pendingTail(params.streamline)
    private val committed = ArrayList<PathPoint>()
    private val incremental = geometry === DefaultStrokeGeometry

    override fun add(sample: InputSample) {
        raw.add(sample)
        if (!incremental) return
        if (!StrokePipeline.keep(lastKept, sample)) return
        lastKept = sample
        streamlined.add(StrokePipeline.streamlineStep(streamlined.lastOrNull(), sample, params.streamline))
        // 안정화: 인덱스 i 는 i+half ≤ n−1 일 때 확정. 꼬리 half 개만 다시 계산한다.
        val n = streamlined.size
        cl.truncate(maxOf(0, n - 1 - half))
        for (i in cl.size until n) cl.append(StrokePipeline.stabilizeAt(streamlined, i, half))
    }

    /** 확정 가능한 점 개수(단조 증가). */
    private fun commitCount(): Int {
        val n = cl.size
        if (n < 2) return 0
        var c = minOf(n - tailK, n - 1 - half)
        val total = cl.total()
        while (c > 0 && total - cl.cum[c - 1] < params.taperEndLength) c--
        return maxOf(c, committed.size)
    }

    private fun syncCommitted() {
        if (!incremental) return
        val target = commitCount()
        if (target <= committed.size) return
        val total = cl.total()
        val es = StrokePipeline.endSpeed(cl, total, params.taperEndLength)
        for (i in committed.size until target) committed.add(StrokePipeline.finalize(cl, i, params, total, es))
    }

    override fun committedPoints(): List<PathPoint> {
        syncCommitted()
        return committed
    }

    override fun pendingPoints(): List<PathPoint> {
        if (!incremental) return geometry.smooth(raw, params)
        syncCommitted()
        val n = cl.size
        val c = committed.size
        val total = cl.total()
        val es = StrokePipeline.endSpeed(cl, total, params.taperEndLength)
        val out = ArrayList<PathPoint>(n - c + 1)
        for (i in c until n) out.add(StrokePipeline.finalize(cl, i, params, total, es))
        if (pullToInput && n > 0 && raw.isNotEmpty()) {
            val last = cl.pts[n - 1]
            val tip = raw[raw.size - 1]
            val dx = tip.x - last.x; val dy = tip.y - last.y
            val d2 = dx * dx + dy * dy
            if (d2 >= StrokePipeline.DEDUPE_MIN_DIST * StrokePipeline.DEDUPE_MIN_DIST) {
                val ref = out.lastOrNull() ?: committed.last()
                val len = kotlin.math.sqrt(d2)
                out.add(PathPoint(tip.x, tip.y, ref.radius, ref.alpha, dx / len, dy / len, tip.t))
            }
        }
        return out
    }

    override fun bounds(): Rect {
        var l = Float.POSITIVE_INFINITY; var t = Float.POSITIVE_INFINITY
        var r = Float.NEGATIVE_INFINITY; var b = Float.NEGATIVE_INFINITY
        fun acc(p: PathPoint) {
            if (p.x - p.radius < l) l = p.x - p.radius; if (p.x + p.radius > r) r = p.x + p.radius
            if (p.y - p.radius < t) t = p.y - p.radius; if (p.y + p.radius > b) b = p.y + p.radius
        }
        committedPoints().forEach(::acc)
        pendingPoints().forEach(::acc)
        return if (l.isFinite()) Rect(l, t, r, b) else Rect.EMPTY
    }

    override fun finish(id: String, brushId: String, color: Int, createdAt: Long): Stroke = Stroke(
        id = id,
        brushId = brushId,
        color = color,
        size = params.size,
        opacity = params.opacity,
        overrides = overrides,
        samples = raw.toList(),
        createdAt = createdAt,
    )
}

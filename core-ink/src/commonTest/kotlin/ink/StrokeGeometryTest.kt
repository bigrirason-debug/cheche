package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Stamp
import io.github.bigrirason_debug.platen.model.StrokeCap
import io.github.bigrirason_debug.platen.model.StrokeOverrides
import io.github.bigrirason_debug.platen.model.Taper
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

internal fun line(n: Int, length: Float, pressure: Float = 1f, dtMs: Float = 8f, y: Float = 0f): List<InputSample> =
    List(n) { InputSample(length * it / (n - 1), y, pressure, t = dtMs * it) }

class StrokeGeometryTest {
    private val geo = DefaultStrokeGeometry
    private val spec = BrushSpec(id = "t", name = "t")
    private fun params(s: BrushSpec = spec, size: Float = 4f, ov: StrokeOverrides? = null, settings: InkSettings = InkSettings()) =
        ResolvedInkParams.resolve(s, size, 1f, settings, ov)

    @Test
    fun streamlineZeroPreservesInput() {
        val input = line(20, 19f)
        val out = geo.smooth(input, params(ov = StrokeOverrides(streamline = 0f)))
        assertEquals(input.size, out.size)
        for (i in input.indices) {
            assertEquals(input[i].x, out[i].x, 1e-6f)
            assertEquals(input[i].y, out[i].y, 1e-6f)
        }
    }

    @Test
    fun streamlineRoundsCorners() {
        // L 자: (0,0)→(10,0)→(10,10), 꼭짓점 (10,0)
        val samples = ArrayList<InputSample>()
        for (i in 0..10) samples.add(InputSample(i.toFloat(), 0f, 1f, t = i * 8f))
        for (i in 1..10) samples.add(InputSample(10f, i.toFloat(), 1f, t = (10 + i) * 8f))
        fun minCornerDist(streamline: Float) =
            geo.smooth(samples, params(ov = StrokeOverrides(streamline = streamline))).minOf { hypot(it.x - 10f, it.y - 0f) }
        assertEquals(0f, minCornerDist(0f), 1e-6f)
        assertTrue(minCornerDist(0.8f) > 1f, "corner should be cut: ${minCornerDist(0.8f)}")
    }

    @Test
    fun endTaperIsThinnerButNotZero() {
        val out = geo.smooth(line(40, 100f), params(ov = StrokeOverrides(streamline = 0f)))
        val first = out.first().radius
        val last = out.last().radius
        assertEquals(2f, first, 1e-5f)
        assertTrue(last < first, "last=$last first=$first")
        assertTrue(last > 0f)
        // 테이퍼 구간(끝 12pt) 밖은 전폭
        val mid = out[out.size / 2].radius
        assertEquals(first, mid, 1e-5f)
    }

    @Test
    fun autoTaperFastEndIsThinnerThanSlowEnd() {
        val p = params(ov = StrokeOverrides(streamline = 0f))
        val slow = geo.smooth(line(40, 100f, dtMs = 40f), p).last().radius
        val fast = geo.smooth(line(40, 100f, dtMs = 1f), p).last().radius // ≈2560 pt/s > refSpeed 1500 → 포화
        assertTrue(fast < slow, "fast=$fast slow=$slow")
        assertEquals(2f * StrokePipeline.AUTO_TAPER_SLOW_FLOOR, slow, 0.05f)
        assertEquals(2f * StrokePipeline.AUTO_TAPER_FAST_FLOOR, fast, 0.05f)
    }

    @Test
    fun manualTaperReachesEndSize() {
        val s = spec.copy(taper = Taper(startLength = 10f, endLength = 12f, startSize = 0.25f, endSize = 0f, auto = false))
        val out = geo.smooth(line(41, 100f), params(s, ov = StrokeOverrides(streamline = 0f)))
        assertEquals(0.5f, out.first().radius, 1e-5f) // 2 × 0.25
        assertEquals(0f, out.last().radius, 1e-5f)
    }

    @Test
    fun pressureAndDotHandling() {
        val p = params(ov = StrokeOverrides(streamline = 0f, taperEnd = 0f))
        val heavy = geo.smooth(line(10, 20f, pressure = 1f), p)[5].radius
        val light = geo.smooth(line(10, 20f, pressure = 0.2f), p)[5].radius
        assertTrue(light < heavy)
        val dot = geo.smooth(listOf(InputSample(3f, 4f, 0.5f)), p)
        assertEquals(1, dot.size)
        assertTrue(dot[0].radius > 0f)
        // 정지 샘플은 제거된다
        val still = geo.smooth(List(5) { InputSample(1f, 1f, 1f, t = it * 8f) }, p)
        assertEquals(1, still.size)
    }

    @Test
    fun outlinePolygonCountBoundsAndWidth() {
        val p = params(ov = StrokeOverrides(streamline = 0f, taperEnd = 0f))
        val center = geo.smooth(line(25, 60f), p)
        val n = center.size
        val round = geo.outline(center, p)
        assertEquals((2 * n + 2 * (DefaultStrokeGeometry.CAP_SEGMENTS - 1)) * 2, round.polygon.size)
        val flat = geo.outline(center, params(spec.copy(cap = StrokeCap.flat), ov = StrokeOverrides(streamline = 0f, taperEnd = 0f)))
        assertEquals(2 * n * 2, flat.polygon.size)
        val square = geo.outline(center, params(spec.copy(cap = StrokeCap.square), ov = StrokeOverrides(streamline = 0f, taperEnd = 0f)))
        assertEquals((2 * n + 4) * 2, square.polygon.size)

        for (o in listOf(round, flat, square)) {
            var i = 0
            var minY = Float.POSITIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
            while (i < o.polygon.size) {
                val x = o.polygon[i]; val y = o.polygon[i + 1]
                assertTrue(x >= o.bounds.left - 1e-4f && x <= o.bounds.right + 1e-4f && y >= o.bounds.top - 1e-4f && y <= o.bounds.bottom + 1e-4f)
                if (y < minY) minY = y; if (y > maxY) maxY = y
                i += 2
            }
            // 직선 획의 폭 = 2r (±1%)
            assertEquals(4f, maxY - minY, 0.04f)
        }
        // 반지름 포함 bounds
        assertTrue(round.bounds.left <= -2f + 1e-4f && round.bounds.right >= 62f - 1e-4f)
        assertTrue(flat.bounds.top <= -2f + 1e-4f && flat.bounds.bottom >= 2f - 1e-4f)
        // 점 하나
        val dot = geo.outline(geo.smooth(listOf(InputSample(0f, 0f, 1f)), p), p)
        assertEquals(DefaultStrokeGeometry.DOT_SEGMENTS * 2, dot.polygon.size)
    }

    @Test
    fun stampPlanCountDeterminismAndBaseDiameter() {
        val s = spec.copy(stamp = Stamp(spacing = 0.15f, jitter = 0.5f, sizeJitter = 0.3f, opacityJitter = 0.3f))
        val p = params(s, size = 10f, ov = StrokeOverrides(streamline = 0f, taperEnd = 0f))
        val length = 100f
        val expected = (length / (0.15f * 10f)).roundToInt()
        val planA = geo.stampPlan(geo.smooth(line(50, length), p), p, seed = 42L)
        assertTrue(abs(planA.stamps.size - expected) <= 1, "count=${planA.stamps.size} expected≈$expected")
        val planB = geo.stampPlan(geo.smooth(line(50, length), p), p, seed = 42L)
        assertEquals(planA.stamps, planB.stamps)
        assertEquals(planA.bounds, planB.bounds)
        val planC = geo.stampPlan(geo.smooth(line(50, length), p), p, seed = 7L)
        assertTrue(planA.stamps != planC.stamps)
        // 필압이 낮아도 개수 불변(base 지름 기준)
        val light = geo.stampPlan(geo.smooth(line(50, length, pressure = 0.2f), p), p, seed = 42L)
        assertEquals(planA.stamps.size, light.stamps.size)
        assertTrue(light.stamps[10].diameter < planA.stamps[10].diameter)
        // 값 범위
        for (st in planA.stamps) {
            assertTrue(st.alpha in 0f..1f)
            assertTrue(st.variant in 0..7)
            assertTrue(st.diameter > 0f)
            assertTrue(planA.bounds.contains(io.github.bigrirason_debug.platen.model.Pt(st.x, st.y)))
        }
        // 간격 하한 0.03
        val tiny = params(s.copy(stamp = Stamp(spacing = 0.001f)), size = 10f, ov = StrokeOverrides(streamline = 0f, taperEnd = 0f))
        val tinyPlan = geo.stampPlan(geo.smooth(line(50, length), tiny), tiny, 1L)
        assertTrue(abs(tinyPlan.stamps.size - (length / (0.03f * 10f)).roundToInt()) <= 1)
    }

    @Test
    fun outlinePerformanceSoft() {
        val p = params()
        val strokes = List(100) { k -> line(30, 80f, y = k.toFloat()) }
        val mark = TimeSource.Monotonic.markNow()
        var acc = 0L
        repeat(10_000) { i ->
            val o = geo.outline(geo.smooth(strokes[i % strokes.size], p), p)
            acc += o.polygon.size
        }
        val elapsed = mark.elapsedNow()
        println("core-ink perf: 30pt × 10,000 strokes smooth+outline = $elapsed (polygon floats=$acc)")
        assertTrue(elapsed.inWholeMilliseconds < 4000, "outline too slow: $elapsed")
    }
}

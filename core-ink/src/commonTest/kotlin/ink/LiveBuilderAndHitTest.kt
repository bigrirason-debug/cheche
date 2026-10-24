package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.StrokeOverrides
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveBuilderAndHitTest {
    private val geo = DefaultStrokeGeometry
    private val spec = BrushSpec(id = "t", name = "t")

    private fun wave(n: Int): List<InputSample> = List(n) { i ->
        val x = i * 0.6f
        InputSample(x, 20f * sin(x * 0.05f), 0.5f + 0.5f * cos(i * 0.013f), tiltX = 0.2f, t = i * 8f + (if (i % 7 == 0) 0f else 0.5f))
    }

    private fun assertSamePoints(expected: List<PathPoint>, actual: List<PathPoint>, tol: Float = 1e-3f) {
        assertEquals(expected.size, actual.size, "point count")
        for (i in expected.indices) {
            val e = expected[i]; val a = actual[i]
            assertEquals(e.x, a.x, tol, "x@$i"); assertEquals(e.y, a.y, tol, "y@$i")
            assertEquals(e.radius, a.radius, tol, "r@$i"); assertEquals(e.alpha, a.alpha, tol, "a@$i")
            assertEquals(e.dirX, a.dirX, tol, "dx@$i"); assertEquals(e.dirY, a.dirY, tol, "dy@$i")
        }
    }

    @Test
    fun incrementalEqualsBatch() {
        for ((sl, st) in listOf(0f to 0f, 0.5f to 0.5f, 0.9f to 1f)) {
            val p = ResolvedInkParams.resolve(spec, 5f, 0.8f, InkSettings(), StrokeOverrides(streamline = sl, stabilize = st))
            val samples = wave(1000)
            val b = DefaultLiveStrokeBuilder(p).apply { pullToInput = false }
            var lastCommitted = 0
            for ((i, s) in samples.withIndex()) {
                b.add(s)
                if (i % 50 == 0) {
                    // 매 프레임 조회해도 committed 는 단조 증가하고 committed+pending 이 연속이다
                    val c = b.committedPoints().size
                    assertTrue(c >= lastCommitted); lastCommitted = c
                    b.pendingPoints()
                }
            }
            val batch = geo.smooth(samples, p)
            val committed = b.committedPoints()
            val pending = b.pendingPoints()
            assertTrue(committed.isNotEmpty(), "committed empty (sl=$sl st=$st)")
            assertTrue(pending.isNotEmpty())
            assertSamePoints(batch, committed + pending)
            // 중간 시점의 committed 점은 최종 결과와 같다(확정 불변)
            val mid = DefaultLiveStrokeBuilder(p).apply { pullToInput = false }
            mid.addAll(samples.subList(0, 600))
            val midCommitted = mid.committedPoints().toList()
            assertSamePoints(batch.subList(0, midCommitted.size), midCommitted)
        }
    }

    @Test
    fun pullToInputAndFinish() {
        val p = ResolvedInkParams.resolve(spec, 5f, 1f, InkSettings(), StrokeOverrides(streamline = 0.8f))
        val samples = wave(200)
        val b = DefaultLiveStrokeBuilder(p)
        b.addAll(samples)
        val tip = b.pendingPoints().last()
        assertEquals(samples.last().x, tip.x, 1e-6f)
        assertEquals(samples.last().y, tip.y, 1e-6f)
        val bounds = b.bounds()
        assertTrue(bounds.contains(Pt(tip.x, tip.y)))
        b.overrides = StrokeOverrides(streamline = 0.8f)
        val stroke = b.finish("s1", "brush", 0xFF000000.toInt(), 123L)
        assertEquals(samples, stroke.samples) // 원시 샘플 보존
        assertEquals(5f, stroke.size)
        assertEquals(StrokeOverrides(streamline = 0.8f), stroke.overrides)
        assertEquals(123L, stroke.createdAt)
    }

    @Test
    fun hitTestPositiveAndNegative() {
        val p = ResolvedInkParams.resolve(spec, 4f, 1f, InkSettings(), StrokeOverrides(streamline = 0f, taperEnd = 0f))
        val stroke = DefaultLiveStrokeBuilder(p).apply { addAll(line(30, 100f, y = 10f)) }.finish("s", "b", 0, 0L)
        assertTrue(StrokeHitTest.hits(stroke, p, Pt(50f, 11.5f), 0f)) // 반지름 2 안
        assertTrue(StrokeHitTest.hits(stroke, p, Pt(50f, 13.5f), 2f)) // tolerance 로 포함
        assertFalse(StrokeHitTest.hits(stroke, p, Pt(50f, 13.5f), 0f)) // 반지름 밖
        assertFalse(StrokeHitTest.hits(stroke, p, Pt(120f, 10f), 1f)) // 끝 너머
        val dot = DefaultLiveStrokeBuilder(p).apply { add(InputSample(5f, 5f, 1f)) }.finish("d", "b", 0, 0L)
        assertTrue(StrokeHitTest.hits(dot, p, Pt(6f, 5f), 0f))
        assertFalse(StrokeHitTest.hits(dot, p, Pt(9f, 5f), 0f))
    }

    @Test
    fun lassoMajorityRule() {
        val stroke = DefaultLiveStrokeBuilder(ResolvedInkParams.resolve(spec, 4f, 1f, InkSettings(), null))
            .apply { addAll(line(11, 100f, y = 10f)) }.finish("s", "b", 0, 0L)
        val coversMost = listOf(Pt(-5f, 0f), Pt(70f, 0f), Pt(70f, 20f), Pt(-5f, 20f)) // x 0..70 → 8/11
        val coversFew = listOf(Pt(-5f, 0f), Pt(30f, 0f), Pt(30f, 20f), Pt(-5f, 20f)) // x 0..30 → 4/11
        val away = listOf(Pt(0f, 50f), Pt(100f, 50f), Pt(100f, 80f), Pt(0f, 80f))
        assertTrue(StrokeHitTest.insideLasso(stroke, coversMost))
        assertFalse(StrokeHitTest.insideLasso(stroke, coversFew))
        assertFalse(StrokeHitTest.insideLasso(stroke, away))
        assertFalse(StrokeHitTest.insideLasso(stroke, coversMost.take(2)))
    }
}

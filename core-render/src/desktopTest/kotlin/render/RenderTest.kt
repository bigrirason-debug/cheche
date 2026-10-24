package io.github.bigrirason_debug.platen.render

import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.bigrirason_debug.platen.ink.Outline
import io.github.bigrirason_debug.platen.ink.PathPoint
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.ink.StampInstance
import io.github.bigrirason_debug.platen.ink.StampPlan
import io.github.bigrirason_debug.platen.ink.StrokeGeometry
import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.BrushRole
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size
import io.github.bigrirason_debug.platen.model.Stroke
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 오프스크린 렌더(Skiko 래스터 백엔드, 디스플레이 불필요)로 픽셀을 읽어 검증한다.
 * 지오메트리는 가짜(직선 사각 outline)를 주입 — core-ink 구현과 독립.
 */
class RenderTest {

    // ---- 픽스처 ----

    /** 사각 outline 을 돌려주는 가짜 지오메트리: 중심선 bbox ± 반지름. */
    private class FakeGeometry : StrokeGeometry {
        override fun smooth(samples: List<InputSample>, params: ResolvedInkParams): List<PathPoint> =
            samples.map { PathPoint(it.x, it.y, params.size / 2f, 1f, 1f, 0f, it.t) }

        override fun outline(center: List<PathPoint>, params: ResolvedInkParams): Outline {
            val r = center.maxOf { it.radius }
            val b = Rect.ofPoints(center.map { Pt(it.x, it.y) }).inflate(r)
            val poly = floatArrayOf(b.left, b.top, b.right, b.top, b.right, b.bottom, b.left, b.bottom)
            return Outline(center, poly, b)
        }

        override fun stampPlan(center: List<PathPoint>, params: ResolvedInkParams, seed: Long): StampPlan =
            StampPlan(center.map { StampInstance(it.x, it.y, it.radius * 2f, it.alpha, 0f, 0) }, outline(center, params).bounds)
    }

    private class CountingRenderer(private val inner: StrokeRenderer) : StrokeRenderer {
        var count = 0
        override fun DrawScope.drawStroke(stroke: Stroke, params: ResolvedInkParams, theme: InkTheme) {
            count++
            with(inner) { drawStroke(stroke, params, theme) }
        }
        override fun DrawScope.drawLive(points: List<PathPoint>, params: ResolvedInkParams, color: Int, theme: InkTheme) {
            with(inner) { drawLive(points, params, color, theme) }
        }
    }

    private val noImages = object : BrushImageStore {
        override fun tip(spec: BrushSpec) = null
        override fun grain(spec: BrushSpec) = null
    }

    private val penSpec = BrushSpec(id = "pen", name = "pen")
    private val hlSpec = BrushSpec(id = "hl", name = "hl", blend = BlendMode.multiply, role = BrushRole.highlighter)

    private fun params(spec: BrushSpec, size: Float) = ResolvedInkParams(
        spec, size, opacity = 1f, streamline = 0f, stabilize = 0f, taperStartLength = 0f, taperEndLength = 0f, taperAuto = false,
        pressureSizeLut = FloatArray(256) { it / 255f }, pressureOpacityLut = FloatArray(256) { 1f },
    )

    private fun hStroke(id: String, color: Int, brush: String = "pen", y: Float = 128f, size: Float = 20f) = Stroke(
        id = id, brushId = brush, color = color, size = size,
        samples = listOf(InputSample(40f, y, 0.5f), InputSample(216f, y, 0.5f, t = 100f)),
    )

    private fun vStroke(id: String, color: Int, brush: String, x: Float = 128f, size: Float = 20f) = Stroke(
        id = id, brushId = brush, color = color, size = size,
        samples = listOf(InputSample(x, 20f, 0.5f), InputSample(x, 236f, 0.5f, t = 100f)),
    )

    private fun resolver(): (Stroke) -> ResolvedInkParams = { s -> params(if (s.brushId == "hl") hlSpec else penSpec, s.size) }

    private fun render(w: Int = 256, h: Int = 256, block: DrawScope.() -> Unit): PixelMap {
        val bmp = ImageBitmap(w, h)
        val canvas = Canvas(bmp)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, ComposeSize(w.toFloat(), h.toFloat())) { block() }
        return bmp.toPixelMap()
    }

    private fun PixelMap.argb(x: Int, y: Int): Int {
        val c = this[x, y]
        fun ch(v: Float) = (v * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (ch(c.alpha) shl 24) or (ch(c.red) shl 16) or (ch(c.green) shl 8) or ch(c.blue)
    }

    private fun assertColorNear(expected: Int, actual: Int, tol: Int = 2, msg: String = "") {
        for (shift in intArrayOf(16, 8, 0)) {
            val e = (expected shr shift) and 0xFF
            val a = (actual shr shift) and 0xFF
            assertTrue(abs(e - a) <= tol, "$msg expected ${expected.toUInt().toString(16)} got ${actual.toUInt().toString(16)} (channel shift $shift)")
        }
    }

    private fun lum(argb: Int) = InkPalette.relativeLuminance(argb)

    private val visibleAll = Rect(0f, 0f, 256f, 256f)
    private fun page(vararg strokes: Stroke, bg: Background = Background.Solid(InkPalette.paperLight)) =
        Page(id = "p", size = Size(256f, 256f), background = bg, strokes = strokes.toList())

    // ---- 테스트 ----

    @Test
    fun profileStrokeFillsOutlineOnPaper() {
        val sr = DefaultStrokeRenderer(noImages, FakeGeometry())
        val pm = render {
            drawRect(Color(InkTheme.Light.paper))
            with(sr) { drawStroke(hStroke("s", 0xFF1A1A1A.toInt()), params(penSpec, 20f), InkTheme.Light) }
        }
        assertColorNear(0xFF1A1A1A.toInt(), pm.argb(128, 128), msg = "center")
        assertColorNear(InkTheme.Light.paper, pm.argb(128, 60), msg = "outside")
        assertColorNear(InkTheme.Light.paper, pm.argb(20, 128), msg = "left of stroke")
    }

    @Test
    fun darkThemeRemapsBlackAndRedSemanticColors() {
        val sr = DefaultStrokeRenderer(noImages, FakeGeometry())
        val pm = render {
            drawRect(Color(InkTheme.Dark.paper))
            with(sr) {
                drawStroke(hStroke("black", 0xFF1A1A1A.toInt(), y = 64f), params(penSpec, 20f), InkTheme.Dark)
                drawStroke(hStroke("red", 0xFFC8102E.toInt(), y = 160f), params(penSpec, 20f), InkTheme.Dark)
            }
        }
        assertColorNear(InkTheme.Dark.inkDefaultDark, pm.argb(128, 64), msg = "black→inkDefaultDark")
        assertColorNear(InkTheme.Dark.redDark, pm.argb(128, 160), msg = "red→redDark")

        // 그 외 어두운 색은 명도만 올라간다(색상 유지)
        val navy = 0xFF102040.toInt()
        val lifted = InkColorPolicy.remapInk(navy, BrushRole.pen, InkTheme.Dark)
        val (h0, _, l0) = InkColorPolicy.toHsl(navy)
        val (h1, _, l1) = InkColorPolicy.toHsl(lifted)
        assertTrue(l1 > l0 + 0.3f, "lightness lifted: $l0 → $l1")
        assertTrue(abs(h0 - h1) < 3f, "hue kept: $h0 vs $h1")
        // 라이트는 그대로, 형광펜은 다크에서도 색 유지
        assertEquals(navy, InkColorPolicy.remapInk(navy, BrushRole.pen, InkTheme.Light))
        assertEquals(InkPalette.hlYellow, InkColorPolicy.remapInk(InkPalette.hlYellow, BrushRole.highlighter, InkTheme.Dark))
    }

    @Test
    fun highlighterMultipliesInLightAndScreensInDark() {
        val sr = DefaultStrokeRenderer(noImages, FakeGeometry())
        fun frame(theme: InkTheme) = render {
            drawRect(Color(theme.paper))
            with(sr) {
                drawStroke(hStroke("ink", 0xFF1A1A1A.toInt()), params(penSpec, 20f), theme)
                drawStroke(vStroke("hl", InkPalette.hlYellow, "hl"), params(hlSpec, 20f), theme) // 잉크 위에 덮어 혼합 모드 확인
            }
        }
        val light = frame(InkTheme.Light)
        val dark = frame(InkTheme.Dark)
        val lightCross = lum(light.argb(128, 128))
        val darkCross = lum(dark.argb(128, 128))
        // 라이트: 검정 × 노랑(multiply) = 어둡다. 다크: 밝은 잉크 ⊕ 노랑(screen) = 밝다(§7.2 — 그래서 실제 페이지는 belowInk).
        assertTrue(lightCross < 0.05, "light multiply crossing should stay dark: $lightCross")
        assertTrue(darkCross > 0.8, "dark screen crossing should be bright: $darkCross")
        // 형광펜 단독 영역: 라이트는 종이보다 어둡고(multiply), 다크는 종이보다 밝다(screen).
        assertTrue(lum(light.argb(128, 40)) < lum(InkTheme.Light.paper), "multiply darkens paper")
        assertTrue(lum(dark.argb(128, 40)) > lum(InkTheme.Dark.paper) + 0.3, "screen brightens dark paper")
        assertColorNear(0xFFF6D746.toInt(), light.argb(128, 40), tol = 3, msg = "§7.2 #FFE44D × #F6F1E7")
        assertColorNear(0xFFFFE75E.toInt(), dark.argb(128, 40), tol = 3, msg = "§7.2 #FFE44D ⊕ #1C1B19")
    }

    @Test
    fun pageRendererPutsHighlighterBelowInk() {
        val pr = DefaultPageRenderer(DefaultStrokeRenderer(noImages, FakeGeometry()))
        // 목록 순서는 형광펜이 나중이지만 렌더는 잉크 아래 → 교차 픽셀은 순수 잉크색
        val pg = page(hStroke("ink", 0xFF1A1A1A.toInt()), vStroke("hl", InkPalette.hlYellow, "hl"))
        val light = render { with(pr) { drawPage(pg, visibleAll, InkTheme.Light, resolver()) } }
        assertColorNear(0xFF1A1A1A.toInt(), light.argb(128, 128), msg = "light crossing = ink")
        val dark = render { with(pr) { drawPage(pg, visibleAll, InkTheme.Dark, resolver()) } }
        assertColorNear(InkTheme.Dark.inkDefaultDark, dark.argb(128, 128), msg = "dark crossing = remapped ink")
        assertColorNear(InkTheme.Dark.paper, dark.argb(20, 20), msg = "dark paper")
    }

    @Test
    fun templateLinesAndMarginStops() {
        val pr = DefaultPageRenderer(DefaultStrokeRenderer(noImages, FakeGeometry()))
        val tpl = Background.Template("lines", spacing = 24f, marginLeft = 48f, marginRight = 48f, marginTop = 64f)
        val pg = page(bg = tpl)
        val pm = render { with(pr) { drawPage(pg, visibleAll, InkTheme.Light, resolver()) } }
        val paper = tpl.paper
        assertColorNear(paper, pm.argb(150, 100), msg = "between lines is paper")
        // 첫 줄 y = marginTop + spacing = 88 (hairline 은 87/88 행에 걸칠 수 있음)
        val lineHit = pm.argb(150, 88) != paper || pm.argb(150, 87) != paper
        assertTrue(lineHit, "rule line expected near y=88")
        // 마진 스톱 x = 48: 빨강 성분 유지, 초록/파랑 감소
        val m = listOf(pm.argb(48, 150), pm.argb(47, 150)).maxByOrNull { ((it shr 16) and 0xFF) - ((it shr 8) and 0xFF) }!!
        val r = (m shr 16) and 0xFF; val g = (m shr 8) and 0xFF; val b = m and 0xFF
        assertTrue(g < ((paper shr 8) and 0xFF) - 8 && r > g && r > b, "margin stop should be reddish: ${m.toUInt().toString(16)}")
        // 상단 여백선 y = 64
        val t = listOf(pm.argb(150, 64), pm.argb(150, 63)).maxByOrNull { ((it shr 16) and 0xFF) - ((it shr 8) and 0xFF) }!!
        assertTrue(((t shr 8) and 0xFF) < ((paper shr 8) and 0xFF) - 8, "top margin should be reddish: ${t.toUInt().toString(16)}")
    }

    @Test
    fun tileCacheSkipsRedrawUntilInvalidated() {
        val counting = CountingRenderer(DefaultStrokeRenderer(noImages, FakeGeometry()))
        val cache = PageCache(tileSize = 256)
        val pr = DefaultPageRenderer(counting, cache)
        val stroke = hStroke("ink", 0xFF1A1A1A.toInt())
        val pg = page(stroke)

        val f1 = render { with(pr) { drawPage(pg, visibleAll, InkTheme.Light, resolver()) } }
        val n1 = counting.count
        assertTrue(n1 > 0, "first frame renders tiles")
        assertEquals(1, cache.tileCount)

        val f2 = render { with(pr) { drawPage(pg, visibleAll, InkTheme.Light, resolver()) } }
        assertEquals(n1, counting.count, "second frame must not call drawStroke")
        for (y in 0 until 256 step 8) for (x in 0 until 256 step 8) assertEquals(f1.argb(x, y), f2.argb(x, y), "pixel $x,$y")
        assertColorNear(0xFF1A1A1A.toInt(), f2.argb(128, 128), msg = "cached ink")
        assertColorNear(InkPalette.paperLight, f2.argb(20, 20), msg = "cached paper")

        // 획 영역과 교차하지 않는 dirty → 그대로
        cache.invalidate("other-page")
        render { with(pr) { drawPage(pg, visibleAll, InkTheme.Light, resolver()) } }
        assertEquals(n1, counting.count, "unrelated invalidate keeps tiles")

        cache.invalidate(pg.id, stroke.bounds())
        assertEquals(0, cache.tileCount)
        render { with(pr) { drawPage(pg, visibleAll, InkTheme.Light, resolver()) } }
        assertTrue(counting.count > n1, "dirty invalidation re-renders")

        // 캐시 결과 = 직접 렌더 결과(줌 1, 타일 정렬)
        val direct = render { with(DefaultPageRenderer(counting)) { drawPage(pg, visibleAll, InkTheme.Light, resolver()) } }
        for (y in 0 until 256 step 8) for (x in 0 until 256 step 8) assertColorNear(direct.argb(x, y), f2.argb(x, y), tol = 1, msg = "cache vs direct $x,$y")
    }

    @Test
    fun contrastRatiosMatchResearchTable() {
        val ink = InkPalette.contrastRatio(0xFF1A1A1A.toInt(), 0xFFF6F1E7.toInt())
        assertTrue(abs(ink - 15.5) <= 0.2, "ink/paper light = $ink")
        val red = InkPalette.contrastRatio(0xFFF26B5E.toInt(), 0xFF1C1B19.toInt())
        assertTrue(abs(red - 5.8) <= 0.2, "red/paper dark = $red")
        assertEquals(ink, InkPalette.contrastRatio(0xFFF6F1E7.toInt(), 0xFF1A1A1A.toInt()), "symmetric")
        assertEquals(1.0, InkPalette.contrastRatio(0xFF808080.toInt(), 0xFF808080.toInt()), 1e-9)
    }
}

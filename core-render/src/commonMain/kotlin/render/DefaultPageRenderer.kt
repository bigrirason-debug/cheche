package io.github.bigrirason_debug.platen.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.BlendMode as ComposeBlend
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeStyle
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke

/**
 * 기본 페이지 렌더러: 종이 → 속지(줄/격자/점) → 마진 스톱(PLAN §4.8) → 객체 → 형광펜(belowInk) → 잉크.
 * 텍스트 글자는 그리지 않는다(TextBox 는 테두리만; 글자 조판은 ui 모듈의 Compose Text 가 같은 rect 위에 얹는다).
 *
 * @param cache 있으면 마른 잉크를 포함한 페이지 전체를 타일 캐시로 그린다(젖은 잉크는 UI 가 drawLive 로 위에 얹음).
 * @param imageStore 있으면 PageObject.Image 의 실제 비트맵을 그린다. 없으면 회색 플레이스홀더 + 대각선.
 */
class DefaultPageRenderer(
    private val strokeRenderer: StrokeRenderer,
    private val cache: PageCache? = null,
    private val imageStore: ImageStore? = null,
) : PageRenderer {

    override fun DrawScope.drawPage(page: Page, visible: Rect, theme: InkTheme, resolve: (Stroke) -> ResolvedInkParams) {
        if (cache != null) {
            cache.drawTiles(this, page, visible, theme) { tileRect -> drawPageDirect(page, tileRect, theme, resolve) }
        } else {
            drawPageDirect(page, visible, theme, resolve)
        }
    }

    /** 캐시 없이 visible 영역을 직접 그린다(타일 렌더의 본체). */
    fun DrawScope.drawPageDirect(page: Page, visible: Rect, theme: InkTheme, resolve: (Stroke) -> ResolvedInkParams) {
        val tokens = InkPalette.of(theme)
        drawPaper(page, visible, theme)
        (page.background as? Background.Template)?.let { drawTemplate(page, it, visible, tokens) }
        drawObjects(page, visible, tokens)
        drawStrokes(page, visible, theme, resolve)
    }

    // ---- 종이 ----

    private fun DrawScope.drawPaper(page: Page, visible: Rect, theme: InkTheme) {
        val bg = page.background
        // 라이트: 속지가 지정한 종이색(Template.paper / Solid.color). 다크: 항상 theme.paper(§7.1 종이 다크는 테마 토큰).
        val paper = when {
            theme.dark -> theme.paper
            bg is Background.Template -> bg.paper
            bg is Background.Solid -> bg.color
            else -> theme.paper // PdfPage/Image 배경: 실제 비트맵은 pdf/ui 모듈이 아래에 깐다
        }
        val l = maxOf(0f, visible.left); val t = maxOf(0f, visible.top)
        val r = minOf(page.size.width, visible.right); val b = minOf(page.size.height, visible.bottom)
        if (r <= l || b <= t) return
        drawRect(Color(paper), topLeft = Offset(l, t), size = ComposeSize(r - l, b - t))
    }

    // ---- 속지 + 마진 스톱 ----

    private fun DrawScope.drawTemplate(page: Page, tpl: Background.Template, visible: Rect, tokens: InkPalette.Tokens) {
        val w = page.size.width
        val h = page.size.height
        val rule = Color(tokens.rule) // ink @20% (§8.3 rule 토큰)
        val spacing = tpl.spacing.takeIf { it > 1f } ?: 24f
        val vl = maxOf(0f, visible.left); val vr = minOf(w, visible.right)
        val vt = maxOf(0f, visible.top); val vb = minOf(h, visible.bottom)

        when (tpl.kind) {
            "lines" -> {
                // 줄은 상단 여백(마진 스톱 top) 아래부터 시작(PLAN §4.8 줄 가이드)
                var y = tpl.marginTop + spacing
                while (y < h) {
                    if (y >= vt - 1f && y <= vb + 1f) drawLine(rule, Offset(0f, y), Offset(w, y), strokeWidth = StrokeStyle.HairlineWidth)
                    y += spacing
                }
            }
            "grid" -> {
                var y = spacing
                while (y < h) { if (y >= vt - 1f && y <= vb + 1f) drawLine(rule, Offset(0f, y), Offset(w, y), strokeWidth = StrokeStyle.HairlineWidth); y += spacing }
                var x = spacing
                while (x < w) { if (x >= vl - 1f && x <= vr + 1f) drawLine(rule, Offset(x, 0f), Offset(x, h), strokeWidth = StrokeStyle.HairlineWidth); x += spacing }
            }
            "dots" -> {
                var y = spacing
                while (y < h) {
                    if (y >= vt - DOT_RADIUS && y <= vb + DOT_RADIUS) {
                        var x = spacing
                        while (x < w) {
                            if (x >= vl - DOT_RADIUS && x <= vr + DOT_RADIUS) drawCircle(rule, radius = DOT_RADIUS, center = Offset(x, y))
                            x += spacing
                        }
                    }
                    y += spacing
                }
            }
            else -> {} // blank
        }

        // 마진 스톱: 타자기 페이퍼 베일의 좌/우 마진 스톱 + 상단 여백선(PLAN §4.8). 빨강 토큰, 1px(hairline), 알파 60%.
        val red = Color(tokens.red).copy(alpha = InkPalette.MARGIN_ALPHA)
        val xl = tpl.marginLeft
        val xr = w - tpl.marginRight
        val yt = tpl.marginTop
        if (xl > 0f) drawLine(red, Offset(xl, 0f), Offset(xl, h), strokeWidth = StrokeStyle.HairlineWidth)
        if (tpl.marginRight > 0f) drawLine(red, Offset(xr, 0f), Offset(xr, h), strokeWidth = StrokeStyle.HairlineWidth)
        if (yt > 0f) drawLine(red, Offset(0f, yt), Offset(w, yt), strokeWidth = StrokeStyle.HairlineWidth)
    }

    // ---- 객체 ----

    private fun DrawScope.drawObjects(page: Page, visible: Rect, tokens: InkPalette.Tokens) {
        for (obj in page.objects) {
            if (!obj.rect.intersects(visible)) continue
            when (obj) {
                is PageObject.TextBox -> drawTextBoxFrame(obj, tokens)
                is PageObject.Image -> drawImageObject(obj, tokens)
                is PageObject.Table -> drawTable(obj, tokens)
                is PageObject.Link -> drawLinkFrame(obj, tokens)
            }
        }
    }

    /** 글자는 그리지 않는다 — 박스 테두리만(muted, hairline). 텍스트 조판은 ui 모듈 Compose Text 담당. */
    private fun DrawScope.drawTextBoxFrame(o: PageObject.TextBox, tokens: InkPalette.Tokens) {
        drawRect(Color(tokens.muted), topLeft = o.rect.topLeft(), size = o.rect.size(), alpha = 0.6f, style = StrokeStyle(StrokeStyle.HairlineWidth))
    }

    private fun DrawScope.drawImageObject(o: PageObject.Image, tokens: InkPalette.Tokens) {
        val bmp = imageStore?.image(o.assetId)
        val r = o.rect
        rotate(o.rotationDeg, pivot = Offset(r.left + r.width / 2f, r.top + r.height / 2f)) {
            if (bmp != null) {
                drawImage(bmp, dstOffset = r.topLeftInt(), dstSize = r.sizeInt())
            } else {
                // 플레이스홀더: 회색 사각 + 대각선 2개
                val gray = Color(tokens.muted)
                drawRect(gray, topLeft = r.topLeft(), size = r.size(), alpha = 0.35f)
                drawRect(gray, topLeft = r.topLeft(), size = r.size(), style = StrokeStyle(StrokeStyle.HairlineWidth))
                drawLine(gray, Offset(r.left, r.top), Offset(r.right, r.bottom), strokeWidth = StrokeStyle.HairlineWidth)
                drawLine(gray, Offset(r.right, r.top), Offset(r.left, r.bottom), strokeWidth = StrokeStyle.HairlineWidth)
            }
        }
    }

    /** 표: 바깥 테두리 + 열/행 선. colWidths/rowHeights 합이 rect 와 다르면 비례로 맞춘다(셀 크기 조절 중 일관성). */
    private fun DrawScope.drawTable(o: PageObject.Table, tokens: InkPalette.Tokens) {
        val r = o.rect
        val color = Color(o.lineColor)
        val lw = o.lineWidth.coerceAtLeast(StrokeStyle.HairlineWidth)
        val colSum = o.colWidths.sum().takeIf { it > 0f } ?: r.width
        val rowSum = o.rowHeights.sum().takeIf { it > 0f } ?: r.height
        val sx = r.width / colSum
        val sy = r.height / rowSum
        drawRect(color, topLeft = r.topLeft(), size = r.size(), style = StrokeStyle(lw))
        var x = r.left
        for (i in 0 until (o.cols - 1).coerceAtLeast(0)) {
            x += (o.colWidths.getOrNull(i) ?: (colSum / o.cols)) * sx
            drawLine(color, Offset(x, r.top), Offset(x, r.bottom), strokeWidth = lw)
        }
        var y = r.top
        for (i in 0 until (o.rows - 1).coerceAtLeast(0)) {
            y += (o.rowHeights.getOrNull(i) ?: (rowSum / o.rows)) * sy
            drawLine(color, Offset(r.left, y), Offset(r.right, y), strokeWidth = lw)
        }
        // 셀 텍스트(o.cells)는 TextBox 와 같은 이유로 ui 모듈이 조판한다.
    }

    /** 하이퍼링크 영역: 점선 테두리, 알파 30%(편집 모드 힌트; 보기 모드에서는 ui 가 숨길 수 있음). */
    private fun DrawScope.drawLinkFrame(o: PageObject.Link, tokens: InkPalette.Tokens) {
        drawRect(
            Color(tokens.ink), topLeft = o.rect.topLeft(), size = o.rect.size(), alpha = 0.3f,
            style = StrokeStyle(width = StrokeStyle.HairlineWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))),
        )
    }

    // ---- 획 ----

    private fun DrawScope.drawStrokes(page: Page, visible: Rect, theme: InkTheme, resolve: (Stroke) -> ResolvedInkParams) {
        val below = ArrayList<Pair<Stroke, ResolvedInkParams>>()
        val ink = ArrayList<Pair<Stroke, ResolvedInkParams>>()
        var needsLayer = false
        for (s in page.strokes) {
            if (s.samples.isEmpty() || !s.bounds().intersects(visible)) continue
            val p = resolve(s)
            if (InkColorPolicy.isBelowInk(p.spec)) below += s to p
            else {
                if (p.spec.blend == BlendMode.erase) needsLayer = true
                ink += s to p
            }
        }
        // 1) 형광펜: 잉크 아래(§7.2 belowInk). 종이와 직접 Multiply/Screen 혼합되어야 하므로 레이어 없이 그린다.
        with(strokeRenderer) { for ((s, p) in below) drawStroke(s, p, theme) }
        // 2) 잉크. erase(Clear) 획이 있으면 잉크만 별도 레이어에 그려 종이·속지가 지워지지 않게 한다.
        if (needsLayer) {
            val l = maxOf(0f, visible.left); val t = maxOf(0f, visible.top)
            val r = minOf(page.size.width, visible.right); val b = minOf(page.size.height, visible.bottom)
            val canvas = drawContext.canvas
            canvas.saveLayer(ComposeRect(l, t, r, b), Paint().apply { blendMode = ComposeBlend.SrcOver })
            with(strokeRenderer) { for ((s, p) in ink) drawStroke(s, p, theme) }
            canvas.restore()
        } else {
            with(strokeRenderer) { for ((s, p) in ink) drawStroke(s, p, theme) }
        }
    }

    private companion object {
        /** 점 속지 반지름(pt). 24pt 간격에서 보이되 잉크와 혼동되지 않는 크기. */
        const val DOT_RADIUS = 1f
    }
}

private fun Rect.topLeft() = Offset(left, top)
private fun Rect.size() = ComposeSize(width, height)
private fun Rect.topLeftInt() = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt())
private fun Rect.sizeInt() = androidx.compose.ui.unit.IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1))

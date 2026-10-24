package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.ink.StrokeGeometry
import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size
import io.github.bigrirason_debug.platen.model.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** 내보내기 결과. warnings = 생략·대체된 항목(한글 텍스트 등). */
class ExportResult(val bytes: ByteArray, val warnings: List<String>)

/**
 * 내보내기 옵션.
 * stampPxPerPt: stamp 엔진 래스터 해상도. 기본 2 px/pt(144 dpi) = 100% 줌 화면 2× 밀도(PLAN §4.7 래스터 레이어).
 */
data class PdfExportOptions(val stampPxPerPt: Float = 2f, val title: String? = null)

/**
 * 노트 → PDF. 배경이 PdfPage 면 원본 페이지를 그대로 복사(링크 주석 보존), Template 은 벡터 속지, Solid 는 색면.
 * 획: profile = Outline.polygon 벡터 fill(nonzero) + ExtGState 알파, stamp = 투명 PNG 래스터 XObject,
 * multiply 블렌드(형광펜) = ExtGState BM/Multiply. 텍스트 = Helvetica/Courier(표준 14; 비 WinAnsi 문자는 생략+경고).
 */
object PdfExporter {
    private const val PRODUCER = "Platen"

    /** Helvetica ascender 718/1000(AFM) — 첫 줄 베이스라인 오프셋 */
    private const val ASCENT = 0.718f
    /** 줄 간격 1.2em(일반 본문 관례) */
    private const val LINE_HEIGHT = 1.2f
    /** 속지 괘선 두께 0.5pt(rea-goodnotes §8 Essentials 수치) */
    private const val RULE_WIDTH = 0.5f
    /** 점 속지 점 반지름(pt) — 0.5pt 선과 비슷한 시각 밀도 */
    private const val DOT_RADIUS = 0.9f
    /** 표 셀 텍스트 크기·패딩(pt) — PLAN A7 에 수치 없음, 본문 14pt 대비 작은 캡션 크기 */
    private const val CELL_FONT = 10f
    private const val CELL_PAD = 3f

    fun export(
        note: Note,
        assetBytes: (String) -> ByteArray?,
        geometry: StrokeGeometry,
        resolve: (Stroke) -> ResolvedInkParams,
        images: (BrushSpec) -> GrayTip? = { null },
        options: PdfExportOptions = PdfExportOptions(),
    ): ExportResult {
        val warnings = ArrayList<String>()
        val writer = createPdfWriter()
        try {
            val pageIndexById = HashMap<String, Int>()
            note.pages.forEachIndexed { i, p -> pageIndexById[p.id] = i }
            val importMap = HashMap<Pair<String, Int>, Int>()
            val sinks = ArrayList<PdfPageSink>()

            for ((i, page) in note.pages.withIndex()) {
                var sink: PdfPageSink? = null
                val bg = page.background
                if (bg is Background.PdfPage) {
                    val bytes = assetBytes(bg.assetId)
                    if (bytes == null) {
                        warnings += "page $i: PDF asset '${bg.assetId}' missing; exported without background"
                    } else {
                        try {
                            sink = writer.importPage(bytes, bg.assetId, bg.pageIndex)
                            val key = bg.assetId to bg.pageIndex
                            if (key !in importMap) importMap[key] = i
                            if (abs(sink.size.width - page.size.width) > 0.5f || abs(sink.size.height - page.size.height) > 0.5f) {
                                warnings += "page $i: note size ${page.size} differs from PDF page ${sink.size}; using PDF size"
                            }
                        } catch (e: Exception) {
                            warnings += "page $i: cannot import PDF page ${bg.pageIndex} of '${bg.assetId}': ${e.message}"
                        }
                    }
                }
                sinks += sink ?: writer.addPage(page.size)
            }

            for ((i, page) in note.pages.withIndex()) {
                val sink = sinks[i]
                val ctx = Ctx(i, sink.size, assetBytes, geometry, resolve, images, options, warnings)
                sink.draw { canvas ->
                    ctx.drawBackground(canvas, page)
                    ctx.drawStrokes(canvas, page)
                    ctx.drawObjects(canvas, page)
                }
                for (obj in page.objects) if (obj is PageObject.Link) {
                    val target = parseLinkTarget(obj.target, pageIndexById, note.pages.size)
                    if (target == null) warnings += "page $i: link '${obj.id}' target '${obj.target}' unresolved; omitted"
                    else sink.addLink(obj.rect, target)
                }
            }

            writer.setInfo(PRODUCER, options.title ?: note.title, note.created, note.modified, fnv1a64(note.id))
            val bytes = writer.finish { key, src -> importMap[key to src] }
            return ExportResult(bytes, warnings)
        } finally {
            writer.close()
        }
    }

    internal fun parseLinkTarget(target: String, pageIndexById: Map<String, Int>, pageCount: Int): LinkTarget? = when {
        target.startsWith("index:") -> target.removePrefix("index:").toIntOrNull()?.takeIf { it in 0 until pageCount }?.let { LinkTarget.PageIndex(it) }
        target.startsWith("page:") -> pageIndexById[target.removePrefix("page:")]?.let { LinkTarget.PageIndex(it) }
        target.isNotBlank() -> LinkTarget.Url(target)
        else -> null
    }

    private class Ctx(
        val pageNo: Int,
        val size: Size,
        val assetBytes: (String) -> ByteArray?,
        val geometry: StrokeGeometry,
        val resolve: (Stroke) -> ResolvedInkParams,
        val images: (BrushSpec) -> GrayTip?,
        val options: PdfExportOptions,
        val warnings: MutableList<String>,
    ) {
        private var eraserWarned = false

        fun drawBackground(c: PdfCanvas, page: Page) {
            when (val bg = page.background) {
                is Background.Solid -> fillRect(c, Rect(0f, 0f, size.width, size.height), bg.color, 1f)
                is Background.Template -> drawTemplate(c, bg)
                is Background.Image -> {
                    val bytes = assetBytes(bg.assetId)
                    if (bytes == null) warnings += "page $pageNo: background image '${bg.assetId}' missing"
                    else drawImageBytes(c, bytes, Rect(0f, 0f, size.width, size.height), 0f, "background '${bg.assetId}'")
                }
                is Background.PdfPage -> {} // 임포트된 원본 페이지가 곧 배경
            }
        }

        fun drawTemplate(c: PdfCanvas, t: Background.Template) {
            val w = size.width; val h = size.height
            fillRect(c, Rect(0f, 0f, w, h), t.paper, colorA(t.paper))
            val alpha = colorA(t.color)
            val spacing = if (t.spacing > 1f) t.spacing else 24f
            val x0 = t.marginLeft; val x1 = w - t.marginRight
            when (t.kind) {
                "lines" -> {
                    var y = t.marginTop
                    while (y < h - RULE_WIDTH) { c.strokeLine(x0, y, x1, y, RULE_WIDTH, t.color, alpha); y += spacing }
                    // 좌측 마진 스톱(노트 여백선): 전체 높이
                    if (t.marginLeft > 0f) c.strokeLine(x0, 0f, x0, h, RULE_WIDTH, t.color, alpha)
                }
                "grid" -> {
                    var y = t.marginTop
                    while (y < h - RULE_WIDTH) { c.strokeLine(x0, y, x1, y, RULE_WIDTH, t.color, alpha); y += spacing }
                    var x = x0
                    while (x <= x1 + 0.01f) { c.strokeLine(x, t.marginTop, x, h, RULE_WIDTH, t.color, alpha); x += spacing }
                }
                "dots" -> {
                    var y = t.marginTop
                    while (y < h - RULE_WIDTH) {
                        var x = x0
                        while (x <= x1 + 0.01f) { fillCircle(c, x, y, DOT_RADIUS, t.color, alpha); x += spacing }
                        y += spacing
                    }
                }
                else -> {} // blank
            }
        }

        fun drawStrokes(c: PdfCanvas, page: Page) {
            for (stroke in page.strokes) {
                if (stroke.samples.isEmpty()) continue
                val params = resolve(stroke)
                val spec = params.spec
                if (spec.blend == BlendMode.erase) {
                    if (!eraserWarned) { warnings += "page $pageNo: pixel-eraser strokes cannot be represented in PDF; skipped"; eraserWarned = true }
                    continue
                }
                val blend = if (spec.blend == BlendMode.multiply) PdfBlend.multiply else PdfBlend.normal
                val alpha = params.opacity.coerceIn(0f, 1f)
                if (alpha <= 0f) continue
                val center = geometry.smooth(stroke.samples, params)
                if (center.isEmpty()) continue
                when (spec.engine) {
                    BrushEngine.profile -> {
                        val outline = geometry.outline(center, params)
                        if (outline.polygon.size >= 6) c.fillPolygon(outline.polygon, stroke.color, alpha, blend)
                    }
                    BrushEngine.stamp -> {
                        val plan = geometry.stampPlan(center, params, stroke.id.hashCode().toLong())
                        val r = StampRaster.render(plan, stroke.color, options.stampPxPerPt, images(spec)) ?: continue
                        c.drawRaster(r.image, imageMatrix(r.rect, 0f), alpha, blend)
                    }
                }
            }
        }

        fun drawObjects(c: PdfCanvas, page: Page) {
            for (obj in page.objects) when (obj) {
                is PageObject.TextBox -> drawTextBox(c, obj)
                is PageObject.Image -> {
                    val bytes = assetBytes(obj.assetId)
                    if (bytes == null) warnings += "page $pageNo: image '${obj.id}' asset '${obj.assetId}' missing"
                    else drawImageBytes(c, bytes, obj.rect, obj.rotationDeg, "image '${obj.id}'")
                }
                is PageObject.Table -> drawTable(c, obj)
                is PageObject.Link -> {} // 주석으로 추가(그릴 것 없음)
            }
        }

        private fun sanitize(c: PdfCanvas, text: String, mono: Boolean, what: String): String {
            var dropped = 0
            val sb = StringBuilder(text.length)
            for (ch in text) {
                if (ch == '\n' || c.canEncode(ch, mono)) sb.append(ch) else dropped++
            }
            if (dropped > 0) warnings += "page $pageNo: $what: $dropped character(s) omitted (no embedded font for non-Latin text, e.g. Korean)"
            return sb.toString()
        }

        fun drawTextBox(c: PdfCanvas, tb: PageObject.TextBox) {
            val text = sanitize(c, tb.text, tb.mono, "text '${tb.id}'")
            if (text.isBlank()) return
            val fs = tb.fontSize
            val lines = TextLayout.wrap(text, tb.rect.width) { c.textWidth(it, fs, tb.mono) }
            var top = tb.rect.top
            for ((k, line) in lines.withIndex()) {
                if (k > 0 && top + fs > tb.rect.bottom + 0.01f) break
                if (line.isNotEmpty()) c.drawText(line, tb.rect.left, top + fs * ASCENT, fs, tb.color, tb.mono)
                top += fs * LINE_HEIGHT
            }
        }

        fun drawTable(c: PdfCanvas, t: PageObject.Table) {
            if (t.rows <= 0 || t.cols <= 0) return
            val cw = if (t.colWidths.size == t.cols && t.colWidths.all { it > 0f }) t.colWidths else List(t.cols) { t.rect.width / t.cols }
            val rh = if (t.rowHeights.size == t.rows && t.rowHeights.all { it > 0f }) t.rowHeights else List(t.rows) { t.rect.height / t.rows }
            val xs = FloatArray(t.cols + 1); xs[0] = t.rect.left
            for (i in 0 until t.cols) xs[i + 1] = xs[i] + cw[i]
            val ys = FloatArray(t.rows + 1); ys[0] = t.rect.top
            for (i in 0 until t.rows) ys[i + 1] = ys[i] + rh[i]
            val alpha = colorA(t.lineColor)
            for (x in xs) c.strokeLine(x, ys[0], x, ys[t.rows], t.lineWidth, t.lineColor, alpha)
            for (y in ys) c.strokeLine(xs[0], y, xs[t.cols], y, t.lineWidth, t.lineColor, alpha)
            for ((key, raw) in t.cells) {
                val parts = key.split(',')
                val r = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: continue
                val col = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: continue
                if (r !in 0 until t.rows || col !in 0 until t.cols) continue
                var text = sanitize(c, raw.replace('\n', ' '), false, "table '${t.id}' cell $key")
                val maxW = cw[col] - 2 * CELL_PAD
                while (text.isNotEmpty() && c.textWidth(text, CELL_FONT, false) > maxW) text = text.dropLast(1)
                if (text.isEmpty()) continue
                c.drawText(text, xs[col] + CELL_PAD, ys[r] + CELL_PAD + CELL_FONT * ASCENT, CELL_FONT, t.lineColor, false)
            }
        }

        private fun drawImageBytes(c: PdfCanvas, bytes: ByteArray, rect: Rect, rotationDeg: Float, what: String) {
            val mime = sniffImageMime(bytes)
            if (mime == null) { warnings += "page $pageNo: $what: unsupported image format (PNG/JPEG only)"; return }
            if (!c.drawEncodedImage(bytes, mime, imageMatrix(rect, rotationDeg), 1f)) warnings += "page $pageNo: $what: image could not be embedded"
        }

        /** 이미지 단위 정사각형 → rect(시각 공간, y 뒤집기 포함), 중심 기준 시계 방향 회전. */
        private fun imageMatrix(rect: Rect, rotationDeg: Float): PdfMat {
            val base = PdfMat(rect.width, 0f, 0f, -rect.height, rect.left, rect.bottom)
            if (rotationDeg == 0f) return base
            val cx = (rect.left + rect.right) / 2f; val cy = (rect.top + rect.bottom) / 2f
            return base.then(PdfMat.translate(-cx, -cy)).then(PdfMat.rotate((rotationDeg * PI / 180.0).toFloat())).then(PdfMat.translate(cx, cy))
        }

        private fun fillRect(c: PdfCanvas, r: Rect, color: Int, alpha: Float) =
            c.fillPolygon(floatArrayOf(r.left, r.top, r.right, r.top, r.right, r.bottom, r.left, r.bottom), color, alpha)

        private fun fillCircle(c: PdfCanvas, cx: Float, cy: Float, radius: Float, color: Int, alpha: Float) {
            val n = 12
            val pts = FloatArray(n * 2)
            for (i in 0 until n) { val a = (2.0 * PI * i / n).toFloat(); pts[2 * i] = cx + radius * cos(a); pts[2 * i + 1] = cy + radius * sin(a) }
            c.fillPolygon(pts, color, alpha)
        }
    }

    /** 괘선 수(테스트·미리보기용): marginTop 부터 spacing 간격으로 높이 안에 들어가는 줄 수. */
    internal fun ruleCount(height: Float, marginTop: Float, spacing: Float): Int =
        if (height - RULE_WIDTH <= marginTop) 0 else floor((height - RULE_WIDTH - marginTop - 1e-4f) / spacing).toInt() + 1
}

/** 매직 바이트로 PNG/JPEG 판별. */
internal fun sniffImageMime(b: ByteArray): String? = when {
    b.size >= 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte() && b[3] == 'G'.code.toByte() -> "image/png"
    b.size >= 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> "image/jpeg"
    else -> null
}

/** 단어 단위 줄바꿈(너무 긴 단어는 글자 단위로 끊는다). '\n' 은 강제 개행. */
internal object TextLayout {
    fun wrap(text: String, maxWidth: Float, measure: (String) -> Float): List<String> {
        val lines = ArrayList<String>()
        for (para in text.split('\n')) {
            var line = ""
            for (word in para.split(' ')) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (measure(candidate) <= maxWidth) { line = candidate; continue }
                if (line.isNotEmpty()) { lines.add(line); line = "" }
                var chunk = ""
                for (ch in word) {
                    if (chunk.isNotEmpty() && measure(chunk + ch) > maxWidth) { lines.add(chunk); chunk = "" }
                    chunk += ch
                }
                line = chunk
            }
            lines.add(line)
        }
        return lines
    }
}

package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.ink.Outline
import io.github.bigrirason_debug.platen.ink.PathPoint
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.ink.StampInstance
import io.github.bigrirason_debug.platen.ink.StampPlan
import io.github.bigrirason_debug.platen.ink.StrokeGeometry
import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushRole
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.cos.COSNumber
import org.apache.pdfbox.pdfparser.PDFStreamParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** 테스트 픽스처: PDFBox 로 만든 3페이지 PDF + core-ink 를 흉내 내는 가짜 StrokeGeometry. */
object PdfFixtures {
    const val W = 400f
    const val H = 600f
    const val URL = "https://example.com/"

    /** 페이지 2(index 1)에 GoTo → 페이지 3, URI 링크 1개. 각 페이지에 "Fixture page N" 텍스트. */
    fun threePagePdf(): ByteArray = PDDocument().use { doc ->
        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val pages = (0 until 3).map { PDPage(PDRectangle(W, H)).also { doc.addPage(it) } }
        pages.forEachIndexed { i, p ->
            PDPageContentStream(doc, p).use { cs ->
                cs.beginText(); cs.setFont(font, 12f); cs.newLineAtOffset(40f, 560f); cs.showText("Fixture page ${i + 1}"); cs.endText()
            }
        }
        val goTo = PDAnnotationLink().apply {
            rectangle = PDRectangle(100f, 500f, 100f, 50f)
            action = PDActionGoTo().apply { destination = PDPageXYZDestination().also { it.page = pages[2] } }
        }
        val uri = PDAnnotationLink().apply {
            rectangle = PDRectangle(50f, 50f, 100f, 30f)
            action = PDActionURI().apply { uri = URL }
        }
        pages[1].annotations = listOf(goTo, uri)
        doc.documentId = 1L
        ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
    }

    /** 400×600 페이지 1장에 /Rotate 90, 사용자 공간 (100,500)-(200,550) URI 링크. */
    fun rotatedPdf(): ByteArray = PDDocument().use { doc ->
        val p = PDPage(PDRectangle(W, H)).also { it.rotation = 90; doc.addPage(it) }
        PDPageContentStream(doc, p).use { cs ->
            cs.beginText(); cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 14f); cs.newLineAtOffset(40f, 560f); cs.showText("Rotated"); cs.endText()
        }
        p.annotations = listOf(PDAnnotationLink().apply {
            rectangle = PDRectangle(100f, 500f, 100f, 50f)
            action = PDActionURI().apply { uri = URL }
        })
        doc.documentId = 2L
        ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
    }

    fun pngAsset(): ByteArray {
        val img = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until 8) for (x in 0 until 8) img.setRGB(x, y, 0xFFCC0000.toInt())
        return ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
    }

    val pen = BrushSpec(id = "pen", name = "Pen", engine = BrushEngine.profile)
    val pencil = BrushSpec(id = "pencil", name = "Pencil", engine = BrushEngine.stamp)
    val highlighter = BrushSpec(id = "hl", name = "Highlighter", engine = BrushEngine.profile, blend = BlendMode.multiply, role = BrushRole.highlighter)
    private val specs = listOf(pen, pencil, highlighter).associateBy { it.id }

    private val lut = FloatArray(256) { it / 255f }
    fun params(spec: BrushSpec, size: Float, opacity: Float = 1f) =
        ResolvedInkParams(spec, size, opacity, 0f, 0f, 0f, 0f, false, lut, lut)

    val resolve: (Stroke) -> ResolvedInkParams = { s -> params(specs.getValue(s.brushId), s.size, s.opacity) }

    fun stroke(id: String, brushId: String, color: Int, size: Float, vararg pts: Pair<Float, Float>, opacity: Float = 1f) = Stroke(
        id = id, brushId = brushId, color = color, size = size, opacity = opacity,
        samples = pts.mapIndexed { i, (x, y) -> InputSample(x, y, 0.5f, t = i * 8f) },
    )

    /** 가짜 지오메트리: outline = 중심선 경계 사각형, stampPlan = 처음·중간·끝 3개 스탬프. */
    val fakeGeometry = object : StrokeGeometry {
        override fun smooth(samples: List<InputSample>, params: ResolvedInkParams): List<PathPoint> =
            samples.map { PathPoint(it.x, it.y, params.size / 2f, 1f, 1f, 0f, it.t) }

        override fun outline(center: List<PathPoint>, params: ResolvedInkParams): Outline {
            val r = Rect.ofPoints(center.map { Pt(it.x, it.y) }).inflate(params.size / 2f)
            return Outline(center, floatArrayOf(r.left, r.top, r.right, r.top, r.right, r.bottom, r.left, r.bottom), r)
        }

        override fun stampPlan(center: List<PathPoint>, params: ResolvedInkParams, seed: Long): StampPlan {
            val picks = listOf(center.first(), center[center.size / 2], center.last())
            val stamps = picks.map { StampInstance(it.x, it.y, params.size, 1f, 0f, 0) }
            return StampPlan(stamps, Rect.ofPoints(center.map { Pt(it.x, it.y) }).inflate(params.size / 2f))
        }
    }

    /** 콘텐츠 스트림 토큰. */
    fun tokens(page: PDPage): List<Any> = PDFStreamParser(page).parse()

    fun operatorNames(page: PDPage): List<String> = tokens(page).filterIsInstance<Operator>().map { it.name }

    /** `m x y l x y S` 패턴 중 수평선(y 같음, x 다름)의 개수. */
    fun horizontalStrokeCount(page: PDPage): Int {
        var count = 0
        val nums = ArrayList<Float>()
        var m: Pair<Float, Float>? = null
        var l: Pair<Float, Float>? = null
        for (t in tokens(page)) {
            when (t) {
                is COSNumber -> nums += t.floatValue()
                is Operator -> {
                    when (t.name) {
                        "m" -> if (nums.size >= 2) m = nums[nums.size - 2] to nums[nums.size - 1]
                        "l" -> if (nums.size >= 2) l = nums[nums.size - 2] to nums[nums.size - 1]
                        "S" -> { val a = m; val b = l; if (a != null && b != null && a.second == b.second && a.first != b.first) count++; m = null; l = null }
                    }
                    nums.clear()
                }
                else -> nums.clear()
            }
        }
        return count
    }
}

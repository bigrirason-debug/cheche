package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size
import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSInteger
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.util.Matrix
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/** PDFBox 3 쓰기 백엔드. */
internal actual fun createPdfWriter(): PdfWriter = DesktopPdfWriter()

internal fun PdfMat.toPdfbox() = Matrix(a, b, c, d, e, f)

private fun utc(ms: Long): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply { timeInMillis = ms }

private class DesktopPdfWriter : PdfWriter {
    val doc = PDDocument()
    private val sources = HashMap<String, PDDocument>()
    private val pages = ArrayList<DesktopPageSink>()
    private class Imported(val sink: DesktopPageSink, val key: String, val src: PDDocument)
    private val imported = ArrayList<Imported>()
    private val gsCache = HashMap<Pair<Int, PdfBlend>, PDExtendedGraphicsState>()

    val helvetica: PDFont by lazy { PDType1Font(Standard14Fonts.FontName.HELVETICA) }
    val courier: PDFont by lazy { PDType1Font(Standard14Fonts.FontName.COURIER) }

    fun pageAt(i: Int): PDPage? = pages.getOrNull(i)?.page

    /** 알파를 1/1000 단위로 양자화해 ExtGState 를 재사용한다. */
    fun gs(alpha: Float, blend: PdfBlend): PDExtendedGraphicsState {
        val q = (alpha.coerceIn(0f, 1f) * 1000f).roundToInt()
        return gsCache.getOrPut(q to blend) {
            PDExtendedGraphicsState().apply {
                nonStrokingAlphaConstant = q / 1000f
                strokingAlphaConstant = q / 1000f
                blendMode = if (blend == PdfBlend.multiply) BlendMode.MULTIPLY else BlendMode.NORMAL
            }
        }
    }

    override fun importPage(sourceBytes: ByteArray, sourceKey: String, pageIndex: Int): PdfPageSink {
        val src = sources.getOrPut(sourceKey) { Loader.loadPDF(sourceBytes) }
        require(pageIndex in 0 until src.numberOfPages) { "page index $pageIndex out of range (${src.numberOfPages})" }
        val srcPage = src.getPage(pageIndex)
        val page = doc.importPage(srcPage) // 콘텐츠·리소스·주석 배열을 공유 복사
        // 주석 사전은 복제해 /P 를 새 페이지로 돌리고, 같은 원본 페이지를 두 번 임포트해도 서로 간섭하지 않게 한다.
        val cloned = ArrayList<PDAnnotation>()
        for (a in srcPage.annotations) {
            val c = PDAnnotation.createAnnotation(COSDictionary(a.cosObject))
            c.page = page
            cloned += c
        }
        page.annotations = cloned
        val sink = DesktopPageSink(this, page, srcPage.frame(), pages.size)
        pages += sink
        imported += Imported(sink, sourceKey, src)
        return sink
    }

    override fun addPage(size: Size): PdfPageSink {
        val page = PDPage(PDRectangle(size.width, size.height))
        doc.addPage(page)
        val sink = DesktopPageSink(this, page, PdfPageFrame.plain(size), pages.size)
        pages += sink
        return sink
    }

    override fun setInfo(producer: String, title: String?, createdMillis: Long, modifiedMillis: Long, documentId: Long) {
        doc.documentId = documentId
        val info = doc.documentInformation
        info.producer = producer
        if (!title.isNullOrBlank()) info.title = title
        info.creationDate = utc(createdMillis)
        info.modificationDate = utc(modifiedMillis)
    }

    override fun finish(remap: (String, Int) -> Int?): ByteArray {
        for (imp in imported) {
            val page = imp.sink.page
            val kept = ArrayList<PDAnnotation>()
            for (a in page.annotations) {
                if (a is PDAnnotationLink) {
                    val dest = sourcePageDestination(a, imp.src)
                    if (dest != null) {
                        val out = pageIndexOf(dest, imp.src)?.let { remap(imp.key, it) }
                        if (out == null) continue // 출력에 포함되지 않은 페이지로 가는 내부 링크는 제거
                        retarget(a, dest, pages[out].page)
                    }
                }
                kept += a
            }
            page.annotations = kept
        }
        for (s in pages) s.flushLinks()
        val out = ByteArrayOutputStream()
        doc.save(out)
        return out.toByteArray()
    }

    private fun retarget(link: PDAnnotationLink, dest: PDPageDestination, target: PDPage) {
        val nd: PDPageDestination = try {
            val arr = COSArray()
            (dest.cosObject as? COSArray)?.let { arr.addAll(it) }
            PDDestination.create(arr) as PDPageDestination
        } catch (e: Exception) {
            PDPageXYZDestination()
        }
        nd.page = target
        link.action = PDActionGoTo().apply { destination = nd }
        link.cosObject.removeItem(COSName.DEST)
    }

    override fun close() {
        pages.clear()
        for (s in sources.values) runCatching { s.close() }
        sources.clear()
        runCatching { doc.close() }
    }
}

private class DesktopPageSink(
    private val writer: DesktopPdfWriter,
    val page: PDPage,
    private val frame: PdfPageFrame,
    override val index: Int,
) : PdfPageSink {
    override val size: Size = frame.visualSize
    private val pending = ArrayList<PDAnnotationLink>()

    override fun draw(block: (PdfCanvas) -> Unit) {
        PDPageContentStream(writer.doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            cs.saveGraphicsState()
            cs.transform(frame.visualToUser.toPdfbox())
            block(DesktopCanvas(writer, cs))
            cs.restoreGraphicsState()
        }
    }

    override fun addLink(rect: Rect, target: LinkTarget) {
        val link = PDAnnotationLink()
        val ur = frame.visualRectToUser(rect)
        link.rectangle = PDRectangle(ur.left, ur.top, ur.width, ur.height)
        link.borderStyle = PDBorderStyleDictionary().apply { width = 0f }
        link.border = COSArray().apply { add(COSInteger.ZERO); add(COSInteger.ZERO); add(COSInteger.ZERO) }
        when (target) {
            is LinkTarget.Url -> link.action = PDActionURI().apply { uri = target.url }
            is LinkTarget.PageIndex -> {
                val tp = writer.pageAt(target.index) ?: return
                link.action = PDActionGoTo().apply { destination = PDPageXYZDestination().also { it.page = tp } }
            }
        }
        link.page = page
        pending += link
    }

    /** 임포트 페이지의 원본 링크 재매핑이 끝난 뒤에 붙인다(새 링크가 재매핑 대상으로 오인되지 않도록). */
    fun flushLinks() {
        if (pending.isEmpty()) return
        val all = ArrayList<PDAnnotation>(page.annotations)
        all.addAll(pending)
        page.annotations = all
        pending.clear()
    }
}

private class DesktopCanvas(private val w: DesktopPdfWriter, private val cs: PDPageContentStream) : PdfCanvas {
    private fun state(alpha: Float, blend: PdfBlend) = cs.setGraphicsStateParameters(w.gs(alpha, blend))
    private fun font(mono: Boolean): PDFont = if (mono) w.courier else w.helvetica

    override fun fillPolygon(points: FloatArray, color: Int, alpha: Float, blend: PdfBlend) {
        if (points.size < 6) return
        state(alpha, blend)
        cs.setNonStrokingColor(colorR(color), colorG(color), colorB(color))
        cs.moveTo(points[0], points[1])
        var i = 2
        while (i + 1 < points.size) { cs.lineTo(points[i], points[i + 1]); i += 2 }
        cs.closePath()
        cs.fill()
    }

    override fun strokeLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int, alpha: Float) {
        state(alpha, PdfBlend.normal)
        cs.setStrokingColor(colorR(color), colorG(color), colorB(color))
        cs.setLineWidth(width)
        cs.moveTo(x1, y1)
        cs.lineTo(x2, y2)
        cs.stroke()
    }

    override fun drawRaster(image: RasterImage, matrix: PdfMat, alpha: Float, blend: PdfBlend) {
        val bi = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        bi.setRGB(0, 0, image.width, image.height, image.argb, 0, image.width)
        val x = LosslessFactory.createFromImage(w.doc, bi)
        state(alpha, blend)
        cs.drawImage(x, matrix.toPdfbox())
    }

    override fun drawEncodedImage(bytes: ByteArray, mime: String, matrix: PdfMat, alpha: Float): Boolean {
        val x = try {
            PDImageXObject.createFromByteArray(w.doc, bytes, "img")
        } catch (e: IOException) { return false } catch (e: IllegalArgumentException) { return false }
        state(alpha, PdfBlend.normal)
        cs.drawImage(x, matrix.toPdfbox())
        return true
    }

    override fun canEncode(ch: Char, mono: Boolean): Boolean = try {
        font(mono).encode(ch.toString()); true
    } catch (e: IllegalArgumentException) { false } catch (e: IOException) { false }

    override fun textWidth(text: String, fontSize: Float, mono: Boolean): Float =
        try { font(mono).getStringWidth(text) / 1000f * fontSize } catch (e: Exception) { 0f }

    override fun drawText(text: String, x: Float, baseline: Float, fontSize: Float, color: Int, mono: Boolean) {
        if (text.isEmpty()) return
        state(1f, PdfBlend.normal)
        cs.setNonStrokingColor(colorR(color), colorG(color), colorB(color))
        cs.beginText()
        cs.setFont(font(mono), fontSize)
        cs.setTextMatrix(Matrix(1f, 0f, 0f, -1f, x, baseline)) // CTM 이 y 를 뒤집으므로 글자를 다시 세운다
        cs.showText(text)
        cs.endText()
    }
}

package io.github.bigrirason_debug.platen.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSInteger
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.blend.BlendMode
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import com.tom_roush.pdfbox.util.Matrix
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/*
 * CI 전용 컴파일. 데스크톱(PdfWriter.desktop.kt)과 같은 로직을 pdfbox-android 2.0 API 로 옮긴 것.
 * 차이점: PDDocument.load(바이트), PDType1Font.HELVETICA 상수, Bitmap 기반 LosslessFactory, /P 는 COSName.P 로 직접 설정.
 * 주의: 표준 14 폰트 메트릭(AFM)을 쓰려면 앱 시작 시 PDFBoxResourceLoader.init(applicationContext) 가 필요하다(TODO 앱 모듈).
 */
internal actual fun createPdfWriter(): PdfWriter = AndroidPdfWriter()

internal fun PdfMat.toPdfbox() = Matrix(a, b, c, d, e, f)

private fun utc(ms: Long): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply { timeInMillis = ms }

private class AndroidPdfWriter : PdfWriter {
    val doc = PDDocument()
    private val sources = HashMap<String, PDDocument>()
    private val pages = ArrayList<AndroidPageSink>()
    private class Imported(val sink: AndroidPageSink, val key: String, val src: PDDocument)
    private val imported = ArrayList<Imported>()
    private val gsCache = HashMap<Pair<Int, PdfBlend>, PDExtendedGraphicsState>()

    val helvetica: PDFont get() = PDType1Font.HELVETICA
    val courier: PDFont get() = PDType1Font.COURIER

    fun pageAt(i: Int): PDPage? = pages.getOrNull(i)?.page

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
        val src = sources.getOrPut(sourceKey) { PDDocument.load(sourceBytes) }
        require(pageIndex in 0 until src.numberOfPages) { "page index $pageIndex out of range (${src.numberOfPages})" }
        val srcPage = src.getPage(pageIndex)
        val page = doc.importPage(srcPage)
        val cloned = ArrayList<PDAnnotation>()
        for (a in srcPage.annotations) {
            val c = PDAnnotation.createAnnotation(COSDictionary(a.cosObject))
            c.cosObject.setItem(COSName.P, page)
            cloned += c
        }
        page.annotations = cloned
        val sink = AndroidPageSink(this, page, srcPage.frame(), pages.size)
        pages += sink
        imported += Imported(sink, sourceKey, src)
        return sink
    }

    override fun addPage(size: Size): PdfPageSink {
        val page = PDPage(PDRectangle(size.width, size.height))
        doc.addPage(page)
        val sink = AndroidPageSink(this, page, PdfPageFrame.plain(size), pages.size)
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
                        if (out == null) continue
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

private class AndroidPageSink(
    private val writer: AndroidPdfWriter,
    val page: PDPage,
    private val frame: PdfPageFrame,
    override val index: Int,
) : PdfPageSink {
    override val size: Size = frame.visualSize
    private val pending = ArrayList<PDAnnotationLink>()

    override fun draw(block: (PdfCanvas) -> Unit) {
        val cs = PDPageContentStream(writer.doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
        try {
            cs.saveGraphicsState()
            cs.transform(frame.visualToUser.toPdfbox())
            block(AndroidCanvas(writer, cs))
            cs.restoreGraphicsState()
        } finally {
            cs.close()
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
        link.cosObject.setItem(COSName.P, page)
        pending += link
    }

    fun flushLinks() {
        if (pending.isEmpty()) return
        val all = ArrayList<PDAnnotation>(page.annotations)
        all.addAll(pending)
        page.annotations = all
        pending.clear()
    }
}

private class AndroidCanvas(private val w: AndroidPdfWriter, private val cs: PDPageContentStream) : PdfCanvas {
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
        val bmp = Bitmap.createBitmap(image.argb, image.width, image.height, Bitmap.Config.ARGB_8888)
        val x: PDImageXObject = LosslessFactory.createFromImage(w.doc, bmp)
        bmp.recycle()
        state(alpha, blend)
        cs.drawImage(x, matrix.toPdfbox())
    }

    override fun drawEncodedImage(bytes: ByteArray, mime: String, matrix: PdfMat, alpha: Float): Boolean {
        val x: PDImageXObject = try {
            when (mime) {
                "image/jpeg" -> JPEGFactory.createFromStream(w.doc, ByteArrayInputStream(bytes))
                else -> {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
                    LosslessFactory.createFromImage(w.doc, bmp).also { bmp.recycle() }
                }
            }
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
        cs.setTextMatrix(Matrix(1f, 0f, 0f, -1f, x, baseline))
        cs.showText(text)
        cs.endText()
    }
}

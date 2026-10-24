package io.github.bigrirason_debug.platen.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import io.github.bigrirason_debug.platen.model.Size
import java.io.File
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.max

/*
 * CI 전용 컴파일(이 컨테이너에는 Android SDK 가 없다). pdfbox-android 2.0.27.0 = PDFBox 2.0 API.
 * 렌더 = 플랫폼 PdfRenderer(pdfium), 크기·링크 = pdfbox-android. 두 라이브러리 모두 crop box + /Rotate 를
 * 적용한 시각 크기를 보고하므로 좌표계가 일치한다고 가정한다(TODO 실기기 검증).
 */

/** Android: PdfRenderer(임시 파일 + ParcelFileDescriptor) 렌더 + pdfbox-android 링크 추출. */
actual fun openPdf(bytes: ByteArray): PdfDocumentSource = AndroidPdfSource(bytes)

internal fun PDPage.frame(): PdfPageFrame {
    val cb = cropBox
    return PdfPageFrame(cb.lowerLeftX, cb.lowerLeftY, cb.width, cb.height, rotation)
}

internal fun sourcePageDestination(link: PDAnnotationLink, doc: PDDocument): PDPageDestination? {
    var dest: PDDestination? = try {
        when (val a = link.action) {
            is PDActionGoTo -> a.destination
            null -> link.destination
            else -> return null
        }
    } catch (e: IOException) { return null }
    if (dest is PDNamedDestination) {
        dest = try { doc.documentCatalog.findNamedDestinationPage(dest) } catch (e: IOException) { null }
    }
    return dest as? PDPageDestination
}

internal fun pageIndexOf(dest: PDPageDestination, doc: PDDocument): Int? {
    val n = dest.retrievePageNumber()
    if (n >= 0) return n
    val p = dest.page ?: return null
    val i = doc.pages.indexOf(p)
    return if (i >= 0) i else null
}

internal fun linkTargetOf(link: PDAnnotationLink, doc: PDDocument): LinkTarget? {
    val a = link.action
    if (a is PDActionURI) return a.uri?.takeIf { it.isNotBlank() }?.let { LinkTarget.Url(it) }
    val dest = sourcePageDestination(link, doc) ?: return null
    return pageIndexOf(dest, doc)?.let { LinkTarget.PageIndex(it) }
}

private class AndroidPdfSource(bytes: ByteArray) : PdfDocumentSource {
    // java.io.tmpdir 는 Android 에서 앱 캐시 디렉터리를 가리킨다.
    private val file: File = File.createTempFile("platen-pdf-", ".pdf").apply { deleteOnExit(); writeBytes(bytes) }
    private val pfd: ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val doc: PDDocument = PDDocument.load(bytes)

    override val pageCount: Int get() = renderer.pageCount

    override fun pageSize(index: Int): Size = doc.getPage(index).frame().visualSize

    override fun render(index: Int, scalePxPerPt: Float): RasterImage {
        require(scalePxPerPt > 0f) { "scale must be > 0" }
        val sz = pageSize(index)
        val w = max(1, ceil(sz.width * scalePxPerPt).toInt())
        val h = max(1, ceil(sz.height * scalePxPerPt).toInt())
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE) // PdfRenderer 는 종이색을 칠하지 않는다
        val page = renderer.openPage(index)
        try {
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        } finally {
            page.close()
        }
        val argb = IntArray(w * h)
        bmp.getPixels(argb, 0, w, 0, 0, w, h)
        bmp.recycle()
        return RasterImage(w, h, argb)
    }

    override fun links(index: Int): List<PdfLink> {
        val page = doc.getPage(index)
        val frame = page.frame()
        return page.annotations.filterIsInstance<PDAnnotationLink>().mapNotNull { link ->
            val target = linkTargetOf(link, doc) ?: return@mapNotNull null
            val r = link.rectangle ?: return@mapNotNull null
            PdfLink(frame.userRectToVisual(r.lowerLeftX, r.lowerLeftY, r.upperRightX, r.upperRightY), target)
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { pfd.close() }
        runCatching { doc.close() }
        runCatching { file.delete() }
    }
}

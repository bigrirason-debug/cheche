package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Size
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.max

/** PDFBox 3: 렌더 = PDFRenderer, 링크 = PDAnnotationLink(GoTo/Dest/URI). */
actual fun openPdf(bytes: ByteArray): PdfDocumentSource = DesktopPdfSource(Loader.loadPDF(bytes))

internal fun PDPage.frame(): PdfPageFrame {
    val cb = cropBox
    return PdfPageFrame(cb.lowerLeftX, cb.lowerLeftY, cb.width, cb.height, rotation)
}

/** 링크의 GoTo 액션 또는 /Dest 를 원본 문서의 페이지 목적지로 푼다(이름 목적지 포함). 없으면 null. */
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

/** 목적지 → 문서 안 페이지 인덱스(0 기반). */
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

private class DesktopPdfSource(private val doc: PDDocument) : PdfDocumentSource {
    private val renderer = PDFRenderer(doc)

    override val pageCount: Int get() = doc.numberOfPages

    override fun pageSize(index: Int): Size = doc.getPage(index).frame().visualSize

    override fun render(index: Int, scalePxPerPt: Float): RasterImage {
        require(scalePxPerPt > 0f) { "scale must be > 0" }
        val sz = pageSize(index)
        val w = max(1, ceil(sz.width * scalePxPerPt).toInt())
        val h = max(1, ceil(sz.height * scalePxPerPt).toInt())
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, w, h)
            // renderImage() 는 floor 크기를 쓰므로 ceil 크기 캔버스에 직접 그린다(회전·crop box 는 PDFBox 가 처리).
            renderer.renderPageToGraphics(index, g, scalePxPerPt)
        } finally {
            g.dispose()
        }
        return RasterImage(w, h, img.getRGB(0, 0, w, h, null, 0, w))
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

    override fun close() = doc.close()
}

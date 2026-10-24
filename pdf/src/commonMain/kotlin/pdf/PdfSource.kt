package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size

/** 렌더된 래스터: 비프리멀티플라이 ARGB, 행 우선(width×height). */
class RasterImage(val width: Int, val height: Int, val argb: IntArray) {
    init { require(argb.size == width * height) { "argb size ${argb.size} != $width*$height" } }
}

/** 링크 목적지: 같은 문서의 페이지 인덱스(0 기반) 또는 URL. */
sealed class LinkTarget {
    data class PageIndex(val index: Int) : LinkTarget()
    data class Url(val url: String) : LinkTarget()
}

/** PDF 링크 주석. rect 는 시각 좌표(crop box·회전 적용 후, 좌상단 원점, pt). */
data class PdfLink(val rect: Rect, val target: LinkTarget)

/**
 * 열린 PDF(읽기 전용). 모든 페이지 좌표는 "시각 좌표" = crop box 와 /Rotate 를 적용한 뒤 좌상단 원점 pt.
 * 원본 바이트는 절대 수정하지 않는다(하이퍼링크 플래너는 읽기 전용 배경, rea-goodnotes §8).
 */
interface PdfDocumentSource : AutoCloseable {
    val pageCount: Int
    /** 페이지 시각 크기(pt). */
    fun pageSize(index: Int): Size
    /** 페이지를 scale px/pt 로 렌더. 크기 = ceil(pt×scale), 종이색은 흰색(불투명). */
    fun render(index: Int, scalePxPerPt: Float): RasterImage
    /** 페이지의 링크 주석(GoTo → PageIndex, URI → Url; 그 밖의 액션은 생략). */
    fun links(index: Int): List<PdfLink>
    override fun close()
}

/** 바이트에서 PDF 를 연다(desktop = PDFBox 3, android = PdfRenderer 렌더 + pdfbox-android 링크). */
expect fun openPdf(bytes: ByteArray): PdfDocumentSource

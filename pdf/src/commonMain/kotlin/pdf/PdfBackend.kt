package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size

/*
 * 플랫폼 PDF 쓰기 백엔드. PDFBox 3(org.apache.pdfbox)와 pdfbox-android 2.0(com.tom_roush.pdfbox)은
 * 패키지가 달라 공유 소스가 불가능하므로, 내보내기 로직(PdfExporter)은 이 얇은 인터페이스만 쓴다.
 * 모든 좌표는 시각 공간(좌상단 원점, pt). 백엔드가 CTM(PdfPageFrame.visualToUser)을 건다.
 */

internal enum class PdfBlend { normal, multiply }

internal interface PdfCanvas {
    /** 닫힌 폴리곤 fill(nonzero). points = [x0,y0,x1,y1,...] */
    fun fillPolygon(points: FloatArray, color: Int, alpha: Float, blend: PdfBlend = PdfBlend.normal)
    fun strokeLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int, alpha: Float)
    /** 단위 정사각형을 matrix 로 사상해 래스터를 그린다(호출자가 y 뒤집기를 matrix 에 넣는다). */
    fun drawRaster(image: RasterImage, matrix: PdfMat, alpha: Float, blend: PdfBlend = PdfBlend.normal)
    /** PNG/JPEG 바이트 임베드. 지원하지 않는 포맷이면 false. */
    fun drawEncodedImage(bytes: ByteArray, mime: String, matrix: PdfMat, alpha: Float): Boolean
    /** 표준 14 폰트(Helvetica/Courier)로 인코딩 가능한 문자인가(한글 등 비 WinAnsi 는 false). */
    fun canEncode(ch: Char, mono: Boolean): Boolean
    fun textWidth(text: String, fontSize: Float, mono: Boolean): Float
    /** baseline 은 시각 y. 글자는 바로 선다(백엔드가 텍스트 행렬을 뒤집는다). */
    fun drawText(text: String, x: Float, baseline: Float, fontSize: Float, color: Int, mono: Boolean)
}

internal interface PdfPageSink {
    /** 출력 문서 안의 페이지 인덱스 */
    val index: Int
    /** 시각 크기(pt) */
    val size: Size
    /** 콘텐츠 스트림을 append 모드로 열고(q/Q 로 격리, CTM 설정) block 을 실행한다. */
    fun draw(block: (PdfCanvas) -> Unit)
    /** 링크 주석 추가. PageIndex 는 출력 문서의 페이지 인덱스. */
    fun addLink(rect: Rect, target: LinkTarget)
}

internal interface PdfWriter {
    /** 원본 PDF 페이지를 주석 포함 그대로 복사한다. 같은 sourceKey 는 한 번만 파싱된다. */
    fun importPage(sourceBytes: ByteArray, sourceKey: String, pageIndex: Int): PdfPageSink
    fun addPage(size: Size): PdfPageSink
    /** 결정적 출력: 날짜·문서 ID 를 입력에서 유도한다. */
    fun setInfo(producer: String, title: String?, createdMillis: Long, modifiedMillis: Long, documentId: Long)
    /** 임포트된 페이지의 내부 GoTo 를 remap(sourceKey, 원본 페이지) → 출력 페이지로 바꾸고(null 이면 주석 제거) 저장. */
    fun finish(remap: (sourceKey: String, sourcePageIndex: Int) -> Int?): ByteArray
    fun close()
}

internal expect fun createPdfWriter(): PdfWriter

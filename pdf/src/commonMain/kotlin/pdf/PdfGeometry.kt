package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 2×3 아핀 행렬, PDF 규약(x' = a·x + c·y + e, y' = b·x + d·y + f; 행벡터 곱). */
data class PdfMat(val a: Float, val b: Float, val c: Float, val d: Float, val e: Float, val f: Float) {
    fun apply(x: Float, y: Float): Pt = Pt(a * x + c * y + e, b * x + d * y + f)

    /** this 를 먼저 적용하고 o 를 적용하는 합성(= this × o). */
    fun then(o: PdfMat): PdfMat = PdfMat(
        a * o.a + b * o.c, a * o.b + b * o.d,
        c * o.a + d * o.c, c * o.b + d * o.d,
        e * o.a + f * o.c + o.e, e * o.b + f * o.d + o.f,
    )

    fun inverse(): PdfMat {
        val det = a * d - b * c
        require(det != 0f) { "singular matrix" }
        val ia = d / det; val ib = -b / det; val ic = -c / det; val id = a / det
        return PdfMat(ia, ib, ic, id, -(e * ia + f * ic), -(e * ib + f * id))
    }

    /** 사각형의 네 꼭짓점을 변환한 뒤 축 정렬 경계. */
    fun mapRect(r: Rect): Rect {
        val ps = listOf(apply(r.left, r.top), apply(r.right, r.top), apply(r.right, r.bottom), apply(r.left, r.bottom))
        return Rect.ofPoints(ps)
    }

    companion object {
        val IDENTITY = PdfMat(1f, 0f, 0f, 1f, 0f, 0f)
        fun translate(tx: Float, ty: Float) = PdfMat(1f, 0f, 0f, 1f, tx, ty)
        fun scale(sx: Float, sy: Float) = PdfMat(sx, 0f, 0f, sy, 0f, 0f)
        /** y-down(시각) 좌표계에서 시계 방향 회전. */
        fun rotate(rad: Float): PdfMat { val c = cos(rad); val s = sin(rad); return PdfMat(c, s, -s, c, 0f, 0f) }
    }
}

/**
 * 원본 PDF 페이지의 사용자 공간(하단 원점, crop box 오프셋, /Rotate) ↔ Platen 시각 공간(회전 적용 후 좌상단 원점).
 * 렌더러(PDFBox PDFRenderer / pdfium)가 보여주는 그대로의 좌표계가 시각 공간이다.
 */
data class PdfPageFrame(val cropX: Float, val cropY: Float, val cropW: Float, val cropH: Float, val rotation: Int) {
    val rot: Int = ((rotation % 360) + 360) % 360
    val visualSize: Size = if (rot == 90 || rot == 270) Size(cropH, cropW) else Size(cropW, cropH)

    /** 사용자 공간 → 시각 공간. */
    val userToVisual: PdfMat = run {
        val unrotated = PdfMat.translate(-cropX, -cropY).then(PdfMat(1f, 0f, 0f, -1f, 0f, cropH))
        val r = when (rot) {
            90 -> PdfMat(0f, 1f, -1f, 0f, cropH, 0f)      // (X,Y) → (H−Y, X)
            180 -> PdfMat(-1f, 0f, 0f, -1f, cropW, cropH) // (X,Y) → (W−X, H−Y)
            270 -> PdfMat(0f, -1f, 1f, 0f, 0f, cropW)     // (X,Y) → (Y, W−X)
            else -> PdfMat.IDENTITY
        }
        unrotated.then(r)
    }

    /** 시각 공간 → 사용자 공간(콘텐츠 스트림 CTM 으로 쓴다). */
    val visualToUser: PdfMat = userToVisual.inverse()

    /** 사용자 공간 사각형(llx,lly,urx,ury) → 시각 Rect. */
    fun userRectToVisual(llx: Float, lly: Float, urx: Float, ury: Float): Rect =
        userToVisual.mapRect(Rect(min(llx, urx), min(lly, ury), max(llx, urx), max(lly, ury)))

    /** 시각 Rect → 사용자 공간 축 정렬 사각형(left=llx, top=lly, right=urx, bottom=ury 의미로 반환). */
    fun visualRectToUser(r: Rect): Rect = visualToUser.mapRect(r)

    companion object {
        fun plain(size: Size) = PdfPageFrame(0f, 0f, size.width, size.height, 0)
    }
}

internal fun fnv1a64(s: String): Long {
    var h = -0x340d631b7bdddcdbL // FNV offset basis 14695981039346656037
    for (ch in s) { h = h xor ch.code.toLong(); h *= 0x100000001b3L }
    return h
}

internal fun colorR(c: Int) = ((c shr 16) and 255) / 255f
internal fun colorG(c: Int) = ((c shr 8) and 255) / 255f
internal fun colorB(c: Int) = (c and 255) / 255f
internal fun colorA(c: Int) = ((c ushr 24) and 255) / 255f

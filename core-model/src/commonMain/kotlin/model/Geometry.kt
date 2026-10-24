package io.github.bigrirason_debug.platen.model

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/** 문서 좌표계: 포인트(1/72 in). 페이지 좌상단 원점. */
@Serializable
data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(k: Float) = Pt(x * k, y * k)
    fun length() = sqrt(x * x + y * y)
    fun distanceTo(o: Pt) = (this - o).length()
}

@Serializable
data class Size(val width: Float, val height: Float)

@Serializable
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    fun contains(p: Pt) = p.x in left..right && p.y in top..bottom
    fun intersects(o: Rect) = !(o.left > right || o.right < left || o.top > bottom || o.bottom < top)
    fun union(o: Rect) = Rect(minOf(left, o.left), minOf(top, o.top), maxOf(right, o.right), maxOf(bottom, o.bottom))
    fun inflate(d: Float) = Rect(left - d, top - d, right + d, bottom + d)

    companion object {
        val EMPTY = Rect(0f, 0f, 0f, 0f)
        fun ofPoints(points: Iterable<Pt>): Rect {
            var l = Float.POSITIVE_INFINITY; var t = Float.POSITIVE_INFINITY
            var r = Float.NEGATIVE_INFINITY; var b = Float.NEGATIVE_INFINITY
            for (p in points) { if (p.x < l) l = p.x; if (p.y < t) t = p.y; if (p.x > r) r = p.x; if (p.y > b) b = p.y }
            return if (l.isFinite()) Rect(l, t, r, b) else EMPTY
        }
    }
}

/** 기본 종이 크기(pt). 굿노트 Standard 455.04×588.45(비율 0.773) [rea-goodnotes], A4 595×842, 16:10 플래너 등. */
object PaperSizes {
    val GOODNOTES_STANDARD = Size(455.04f, 588.45f)
    val A4 = Size(595.28f, 841.89f)
    val LETTER = Size(612f, 792f)
    val PLANNER_16_10 = Size(576f, 360f)
    val PLANNER_10_16 = Size(360f, 576f)
    val DEFAULT = GOODNOTES_STANDARD
}

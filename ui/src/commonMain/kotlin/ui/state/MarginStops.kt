package io.github.bigrirason_debug.platen.ui.state

import io.github.bigrirason_debug.platen.model.Background

enum class MarginSide { left, right }

/**
 * 마진 스톱(눈금자 위 빨간 마커 2개) ↔ Background.Template 의 marginLeft/Right.
 * 드래그는 "속지 편집 모드"에서만 호출된다(PLAN §12-9). 순수 함수라 테스트 가능.
 */
object MarginStops {
    /** 좌·우 마커 사이 최소 간격(pt). 굿노트 기본 종이 455pt 의 약 1/4 — 줄 가이드가 의미를 갖는 최소 폭(추정). */
    const val MIN_GAP_PT = 96f
    /** 눈금 스냅 간격(pt). 12pt = 1/6 inch(타자기 피카 1/10", 엘리트 1/12" 사이 값 — 추정). */
    const val SNAP_PT = 12f

    /** 템플릿이 아니면 null(PDF·이미지 배경은 마진 없음). */
    fun templateOf(bg: Background): Background.Template? = bg as? Background.Template

    /** side 마커를 문서 x(pt) 로 옮긴 템플릿. 눈금 스냅, 0..pageWidth 클램프, 최소 간격 유지. */
    fun drag(t: Background.Template, side: MarginSide, docX: Float, pageWidth: Float, snap: Boolean = true): Background.Template {
        val x = (if (snap) (docX / SNAP_PT).let { kotlin.math.round(it) } * SNAP_PT else docX).coerceIn(0f, pageWidth)
        return when (side) {
            MarginSide.left -> {
                val rightX = pageWidth - t.marginRight
                val left = x.coerceAtMost(rightX - MIN_GAP_PT).coerceAtLeast(0f)
                t.copy(marginLeft = left)
            }
            MarginSide.right -> {
                val minRightX = t.marginLeft + MIN_GAP_PT
                val rightX = x.coerceAtLeast(minRightX).coerceAtMost(pageWidth)
                t.copy(marginRight = (pageWidth - rightX).coerceAtLeast(0f))
            }
        }
    }

    /** 마커의 문서 x 위치. */
    fun markerX(t: Background.Template, side: MarginSide, pageWidth: Float): Float =
        if (side == MarginSide.left) t.marginLeft else pageWidth - t.marginRight

    /** 라인 스페이스 3단(typewriter-refs §1 라인 스페이스 레버): 줄 간격 pt. 6/7/8mm ≈ 17/20/23pt. */
    val LINE_SPACINGS = listOf(17f, 20f, 23f)

    fun withSpacing(t: Background.Template, index: Int): Background.Template =
        t.copy(spacing = LINE_SPACINGS[index.coerceIn(0, LINE_SPACINGS.lastIndex)])

    fun spacingIndex(t: Background.Template): Int =
        LINE_SPACINGS.indices.minByOrNull { kotlin.math.abs(LINE_SPACINGS[it] - t.spacing) } ?: 1
}

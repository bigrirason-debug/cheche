package io.github.bigrirason_debug.platen.render

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.bigrirason_debug.platen.ink.PathPoint
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke

/*
 * core-render 공개 계약(PLAN §4.1). Compose Canvas(DrawScope) 위에 그린다.
 * 좌표 변환(문서 pt → 화면 px)은 호출자가 DrawScope 의 transform 으로 적용한다.
 */

/** 다크 모드 색 재매핑·형광펜 혼합 정책(typewriter-refs §7: 반전이 아니라 의미색 재매핑). */
data class InkTheme(
    val dark: Boolean,
    val paper: Int,
    /** 잉크 "검정"으로 간주할 색(라이트 #1A1A1A)을 다크에서 무엇으로 바꿀지 */
    val inkDefaultLight: Int = 0xFF1A1A1A.toInt(),
    val inkDefaultDark: Int = 0xFFE8E4DC.toInt(),
    val redLight: Int = 0xFFC8102E.toInt(),
    val redDark: Int = 0xFFF26B5E.toInt(),
) {
    companion object {
        val Light = InkTheme(dark = false, paper = 0xFFF6F1E7.toInt())
        val Dark = InkTheme(dark = true, paper = 0xFF1C1B19.toInt())
    }
}

/** 브러시 이미지(팁/그레인)를 렌더러에 공급. 프리컴포즈 변형 8장은 구현이 캐시한다. */
interface BrushImageStore {
    fun tip(spec: BrushSpec): ImageBitmap?
    fun grain(spec: BrushSpec): ImageBitmap?
}

interface StrokeRenderer {
    /** 확정 획(마른 잉크) */
    fun DrawScope.drawStroke(stroke: Stroke, params: ResolvedInkParams, theme: InkTheme)
    /** 진행 중 획(젖은 잉크): 빌더의 committed+pending 점열 */
    fun DrawScope.drawLive(points: List<PathPoint>, params: ResolvedInkParams, color: Int, theme: InkTheme)
}

interface PageRenderer {
    /** 배경(종이·속지 줄·마진 스톱 여백) + 객체 + 획 전체. visible = 문서 좌표의 가시 영역(컬링·타일 캐시 힌트). */
    fun DrawScope.drawPage(page: Page, visible: Rect, theme: InkTheme, resolve: (Stroke) -> ResolvedInkParams)
}

object Renderers {
    fun stroke(images: BrushImageStore): StrokeRenderer = DefaultStrokeRenderer(images)
    fun page(strokeRenderer: StrokeRenderer): PageRenderer = DefaultPageRenderer(strokeRenderer)
}

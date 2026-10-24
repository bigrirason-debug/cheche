package io.github.bigrirason_debug.platen.render

import androidx.compose.ui.graphics.ImageBitmap
import io.github.bigrirason_debug.platen.model.BrushSpec

/**
 * [BrushImageStore] 선택 확장: 팁 × 그레인 오프셋을 미리 합성한 변형(brush-formats §3.7 "프리컴포즈 8장").
 * 구현이 캐시하며, 렌더러는 [StampInstance.variant] 로 고른다. 미구현이면 렌더러는 [BrushImageStore.tip] 1장을 쓴다.
 */
interface PrecomposedTips : BrushImageStore {
    /** 프리컴포즈 변형 수(기본 8) */
    val variantCount: Int get() = 8
    /** variant(0 until variantCount)에 해당하는 합성 팁. null 이면 기본 팁으로 폴백. */
    fun tipVariant(spec: BrushSpec, variant: Int): ImageBitmap?
}

/** 페이지 객체(PageObject.Image)의 실제 비트맵 공급. null 이면 플레이스홀더를 그린다. */
interface ImageStore {
    fun image(assetId: String): ImageBitmap?
}

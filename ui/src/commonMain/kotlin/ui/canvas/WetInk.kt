package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent

/**
 * 젖은 잉크 저지연 오버레이(PLAN §4.2). Android 는 graphics-core 프론트버퍼(CanvasFrontBufferedRenderer)로
 * 펜 아래 획 조각을 곧바로 그리고, 펜을 떼면 Compose 가 마른 잉크를 그린 뒤(2프레임 후) 지운다.
 * 데스크톱은 없음(null). 좌표는 캔버스 Box 로컬 px.
 */
interface WetInkOverlay {
    /** 펜 다운: 입력을 vsync 배칭 없이 받도록(Android requestUnbufferedDispatch). */
    fun penDown(event: PointerEvent)
    fun segment(x0: Float, y0: Float, x1: Float, y1: Float, widthPx: Float, argb: Int)
    /** 마른 잉크가 그려진 뒤 지운다(구현이 프레임 지연을 둔다). */
    fun clearAfterDry()
}

/** 캔버스와 같은 크기로 겹쳐 놓는 오버레이. [enabled] 가 false 이거나 플랫폼이 지원하지 않으면 onOverlay(null). */
@Composable
expect fun WetInkLayer(enabled: Boolean, modifier: Modifier, onOverlay: (WetInkOverlay?) -> Unit)

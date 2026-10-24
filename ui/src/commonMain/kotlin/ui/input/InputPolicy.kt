package io.github.bigrirason_debug.platen.ui.input

import androidx.compose.ui.input.pointer.PointerType
import io.github.bigrirason_debug.platen.ui.state.Tool

/** 포인터 1개의 역할. */
enum class PointerRole { Draw, Navigate, Ignore }

/**
 * 기본 입력 정책(BRIEF 2b): 스타일러스 = 그리기 / 손가락 = 팬·줌(+손가락 필기 토글) / 펜 호버·접촉 중 손가락 무시 /
 * 마우스 = 도구 조작(올가미·텍스트·표·이미지·지우개)만, 펜 도구에선 팬. `mouseDraw` 는 데스크톱 진단용.
 */
object InputPolicy {
    fun roleFor(
        type: PointerType,
        tool: Tool,
        fingerDraw: Boolean,
        palmRejection: Boolean,
        stylusActive: Boolean,
        mouseDraw: Boolean,
        touchCount: Int,
    ): PointerRole = when (type) {
        PointerType.Stylus, PointerType.Eraser -> PointerRole.Draw
        PointerType.Touch -> when {
            palmRejection && stylusActive -> PointerRole.Ignore
            touchCount >= 2 -> PointerRole.Navigate
            fingerDraw -> PointerRole.Draw
            else -> PointerRole.Navigate
        }
        PointerType.Mouse -> when {
            mouseDraw -> PointerRole.Draw
            tool == Tool.Pen -> PointerRole.Navigate
            else -> PointerRole.Draw
        }
        else -> PointerRole.Navigate
    }
}

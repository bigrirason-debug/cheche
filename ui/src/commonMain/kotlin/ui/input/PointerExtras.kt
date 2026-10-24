package io.github.bigrirason_debug.platen.ui.input

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed

/** 화면 좌표의 원시 입력점(히스토리 포함). 문서 좌표 변환은 호출자. */
data class RawPoint(val x: Float, val y: Float, val pressure: Float, val tiltX: Float, val tiltY: Float, val timeMs: Long)

/** 포인터 변경 1건에서 끌어낸 플랫폼 확장 정보. */
data class PointerExtra(
    val points: List<RawPoint>,
    /** 시스템이 취소한 입력(Android FLAG_CANCELED/ACTION_CANCEL) → 진행 중 획 롤백 */
    val canceled: Boolean,
    /** S펜 옆버튼(BUTTON_STYLUS_PRIMARY) 또는 마우스 보조 버튼 */
    val stylusButton: Boolean,
    val hovering: Boolean,
    val historyCount: Int,
    val hasTilt: Boolean,
)

/** commonMain 에서 가능한 범위의 기본 구현(PointerInputChange.historical + pressure). */
object CommonPointerExtras {
    fun describe(event: PointerEvent, change: PointerInputChange): PointerExtra {
        val hist = change.historical
        val points = ArrayList<RawPoint>(hist.size + 1)
        for (h in hist) points += RawPoint(h.position.x, h.position.y, change.pressure, 0f, 0f, h.uptimeMillis)
        points += RawPoint(change.position.x, change.position.y, change.pressure, 0f, 0f, change.uptimeMillis)
        val stylusLike = change.type == PointerType.Stylus || change.type == PointerType.Eraser || change.type == PointerType.Mouse
        return PointerExtra(
            points = points,
            canceled = false,
            stylusButton = stylusLike && event.buttons.isSecondaryPressed,
            hovering = stylusLike && !change.pressed,
            historyCount = hist.size,
            hasTilt = false,
        )
    }
}

/** 플랫폼별 확장(Android: MotionEvent 필압·틸트·옆버튼·FLAG_CANCELED·히스토리; 데스크톱: 공통 구현). */
expect object PlatformPointerExtras {
    fun describe(event: PointerEvent, change: PointerInputChange): PointerExtra
}

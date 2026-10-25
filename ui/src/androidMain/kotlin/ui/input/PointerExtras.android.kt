package io.github.bigrirason_debug.platen.ui.input

import android.view.MotionEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.motionEvent
import kotlin.math.cos
import kotlin.math.sin

/**
 * Android 입력 어댑터(PLAN §4.2): MotionEvent 에서 필압·틸트·방위·호버·옆버튼(BUTTON_STYLUS_PRIMARY)·FLAG_CANCELED·히스토리 샘플을
 * 모두 꺼내 RawPoint 로 만든다. 좌표는 Compose 의 change.position 기준(히스토리는 현재 위치와 MotionEvent 위치의 차로 보정).
 *
 * 컨테이너에서는 컴파일되지 않는다(CI 전용, D-020 ⑤) — API 레퍼런스: developer.android.com MotionEvent.
 * 젖은 잉크 프론트버퍼는 ui/canvas/WetInk.android.kt(D-026).
 */
actual object PlatformPointerExtras {
    actual fun describe(event: PointerEvent, change: PointerInputChange): PointerExtra {
        val me: MotionEvent = event.motionEvent ?: run {
            // 합성 이벤트(Compose 가 만든 취소 등)에는 MotionEvent 가 없다. 눌린 상태에서 들어온 합성 "업"은 취소로 본다.
            val base = CommonPointerExtras.describe(event, change)
            return base.copy(canceled = change.previousPressed && !change.pressed)
        }
        val stylusLike = change.type == PointerType.Stylus || change.type == PointerType.Eraser
        // Compose PointerId 와 Android pointerId 매핑이 공개되지 않아 도구 타입으로 인덱스를 찾는다(스타일러스는 1개).
        var idx = -1
        for (i in 0 until me.pointerCount) {
            val tt = me.getToolType(i)
            val isStylusTool = tt == MotionEvent.TOOL_TYPE_STYLUS || tt == MotionEvent.TOOL_TYPE_ERASER
            if (isStylusTool == stylusLike) { idx = i; break }
        }
        if (idx < 0) idx = me.actionIndex.coerceIn(0, me.pointerCount - 1)

        val action = me.actionMasked
        val canceled = action == MotionEvent.ACTION_CANCEL || (me.flags and MotionEvent.FLAG_CANCELED) != 0
        val hovering = action == MotionEvent.ACTION_HOVER_MOVE || action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_EXIT
        val button = (me.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0 || (me.buttonState and MotionEvent.BUTTON_SECONDARY) != 0

        // MotionEvent 좌표계 → Compose 로컬 좌표계 보정(현재 샘플의 차이를 히스토리에 동일 적용)
        val dx = change.position.x - me.getX(idx)
        val dy = change.position.y - me.getY(idx)
        val n = me.historySize
        val points = ArrayList<RawPoint>(n + 1)
        var hasTilt = false
        for (h in 0 until n) {
            val tilt = me.getHistoricalAxisValue(MotionEvent.AXIS_TILT, idx, h)
            val orient = me.getHistoricalAxisValue(MotionEvent.AXIS_ORIENTATION, idx, h)
            if (tilt != 0f) hasTilt = true
            points += RawPoint(
                me.getHistoricalX(idx, h) + dx, me.getHistoricalY(idx, h) + dy,
                me.getHistoricalPressure(idx, h).coerceIn(0f, 1f),
                tiltX(tilt, orient), tiltY(tilt, orient), me.getHistoricalEventTime(h),
            )
        }
        val tilt = me.getAxisValue(MotionEvent.AXIS_TILT, idx)
        val orient = me.getAxisValue(MotionEvent.AXIS_ORIENTATION, idx)
        if (tilt != 0f) hasTilt = true
        points += RawPoint(change.position.x, change.position.y, me.getPressure(idx).coerceIn(0f, 1f), tiltX(tilt, orient), tiltY(tilt, orient), me.eventTime)
        return PointerExtra(points, canceled, button, hovering, n, hasTilt)
    }

    /** 틸트 크기(라디안, 수직=0)와 방위(0 = 화면 위쪽, 시계방향 +) → x/y 성분. */
    private fun tiltX(tilt: Float, orientation: Float): Float = tilt * sin(orientation)
    private fun tiltY(tilt: Float, orientation: Float): Float = -tilt * cos(orientation)
}

package io.github.bigrirason_debug.platen.ui.input

import io.github.bigrirason_debug.platen.model.GestureAction
import io.github.bigrirason_debug.platen.model.GestureInput
import io.github.bigrirason_debug.platen.model.GestureMap

/** 제스처 동작의 수신자(캔버스 셸이 구현). */
interface GestureActions {
    fun undo()
    fun redo()
    fun selectEraser()
    fun selectLasso()
    fun previousTool()
    fun nextPage()
    fun previousPage()
    fun selectLaser()
    fun toggleFingerDraw()
    fun ribbonToggle()
}

/**
 * 손가락 제스처 인식 결과 → GestureMap → GestureActions. 인식기(터치 수·탭/스와이프 판정)와 분리된 순수 디스패처.
 * 반환값 = 실행된 동작(none 이면 아무 것도 안 함). 테스트 대상.
 */
class GestureDispatcher(var map: GestureMap, private val actions: GestureActions) {
    fun dispatch(input: GestureInput): GestureAction {
        val a = map.actionFor(input)
        when (a) {
            GestureAction.none -> {}
            GestureAction.undo -> actions.undo()
            GestureAction.redo -> actions.redo()
            GestureAction.eraser -> actions.selectEraser()
            GestureAction.lasso -> actions.selectLasso()
            GestureAction.previousTool -> actions.previousTool()
            GestureAction.nextPage -> actions.nextPage()
            GestureAction.previousPage -> actions.previousPage()
            GestureAction.laser -> actions.selectLaser()
            GestureAction.toggleFingerDraw -> actions.toggleFingerDraw()
            GestureAction.colorBlackRed -> actions.ribbonToggle()
        }
        return a
    }
}

/**
 * 멀티터치 제스처 인식기(순수 상태 기계). 손가락 수 2~3, 탭 = 이동 < slop 이고 지속 < tapMs, 스와이프 = 수평 이동 > swipeMin.
 * 더블탭 = 같은 손가락 수 탭이 doubleTapMs 안에 2회.
 */
class FingerGestureRecognizer(
    private val tapSlopPx: Float,
    private val swipeMinPx: Float,
    private val tapMs: Long = 300,
    private val doubleTapMs: Long = 350,
) {
    private var maxFingers = 0
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var startT = 0L
    private var lastTapT = 0L
    private var lastTapFingers = 0
    private var active = false

    fun begin(fingers: Int, cx: Float, cy: Float, t: Long) {
        active = true; maxFingers = fingers; startX = cx; startY = cy; lastX = cx; lastY = cy; startT = t
    }

    fun update(fingers: Int, cx: Float, cy: Float) {
        if (!active) return
        if (fingers > maxFingers) { maxFingers = fingers; startX = cx; startY = cy }
        lastX = cx; lastY = cy
    }

    /** 모든 손가락이 떨어졌을 때. 인식된 입력 또는 null. */
    fun end(t: Long): GestureInput? {
        if (!active) return null
        active = false
        if (maxFingers < 2) return null
        val dx = lastX - startX; val dy = lastY - startY
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val dur = t - startT
        if (dist < tapSlopPx && dur < tapMs) {
            val isDouble = (t - lastTapT) < doubleTapMs && lastTapFingers == maxFingers
            lastTapT = if (isDouble) 0L else t
            lastTapFingers = maxFingers
            return when (maxFingers) {
                2 -> if (isDouble) GestureInput.twoFingerDoubleTap else GestureInput.twoFingerTap
                else -> if (isDouble) GestureInput.threeFingerDoubleTap else GestureInput.threeFingerTap
            }
        }
        if (kotlin.math.abs(dx) > swipeMinPx && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f) {
            val left = dx < 0
            return when (maxFingers) {
                2 -> if (left) GestureInput.twoFingerSwipeLeft else GestureInput.twoFingerSwipeRight
                else -> if (left) GestureInput.threeFingerSwipeLeft else GestureInput.threeFingerSwipeRight
            }
        }
        return null
    }
}

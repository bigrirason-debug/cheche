package io.github.bigrirason_debug.platen.model

import kotlinx.serialization.Serializable

/** 제스처 입력(설정 가능한 범위 = 손가락 제스처 + S펜 옆버튼, D-012 (b)). */
@Serializable
enum class GestureInput {
    twoFingerTap, threeFingerTap, twoFingerDoubleTap, threeFingerDoubleTap,
    twoFingerSwipeLeft, twoFingerSwipeRight, threeFingerSwipeLeft, threeFingerSwipeRight,
    stylusButtonDrag, stylusButtonClick, stylusButtonHold,
}

@Serializable
enum class GestureAction {
    none, undo, redo, eraser, lasso, previousTool, nextPage, previousPage, laser, toggleFingerDraw, colorBlackRed,
}

/** 입력 → 동작 매핑 테이블. 기본값 = 이전 앱 관례(손가락 팬/줌, 옆버튼+긋기 = 지우개). */
@Serializable
data class GestureMap(val map: Map<GestureInput, GestureAction> = DEFAULT) {
    fun actionFor(input: GestureInput): GestureAction = map[input] ?: GestureAction.none

    companion object {
        val DEFAULT: Map<GestureInput, GestureAction> = mapOf(
            GestureInput.twoFingerTap to GestureAction.undo,
            GestureInput.threeFingerTap to GestureAction.redo,
            GestureInput.twoFingerSwipeLeft to GestureAction.nextPage,
            GestureInput.twoFingerSwipeRight to GestureAction.previousPage,
            GestureInput.stylusButtonDrag to GestureAction.eraser,
            GestureInput.stylusButtonClick to GestureAction.previousTool,
            GestureInput.stylusButtonHold to GestureAction.lasso,
        )
    }
}

/** 전역 필기 파라미터(브러시 커브와 합성, brush-formats §3.4). 사용자 설정 화면의 1층. */
@Serializable
data class InkSettings(
    val globalPressureCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val streamline: Float = 0.3f,
    val stabilize: Float = 0f,
    val taperEndLength: Float = 12f,
    /** 모션 예측: 이전 실측에서 기본 OFF 가 체감상 나았다(밀림) */
    val motionPrediction: Boolean = false,
    val frontBuffer: Boolean = true,
    val fingerDraw: Boolean = false,
    val palmRejection: Boolean = true,
)

@Serializable
enum class ThemeMode { system, light, dark }

@Serializable
data class AppSettings(
    val ink: InkSettings = InkSettings(),
    val gestures: GestureMap = GestureMap(),
    val theme: ThemeMode = ThemeMode.system,
    val reducedMotion: Boolean = false,
    val uiDensity: Float = 1f,
    val defaultPaper: Int = 0xFFF6F1E7.toInt(),
    val laserColor: Int = 0xFFC8102E.toInt(),
    val laserFadeMillis: Int = 1000,
    val laserWidth: Float = 4f,
)

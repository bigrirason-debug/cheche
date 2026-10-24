package io.github.bigrirason_debug.platen.ui.state

import io.github.bigrirason_debug.platen.model.StrokeOverrides
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcon
import io.github.bigrirason_debug.platen.ui.theme.PlatenIcons
import io.github.bigrirason_debug.platen.ui.theme.PlatenPalette
import kotlinx.serialization.Serializable

/** 캔버스 도구. 펜은 슬롯 인덱스로 구분(Tool.Pen 은 "현재 선택 슬롯"). */
enum class Tool(val icon: PlatenIcon) {
    Pen(PlatenIcons.Pen), Eraser(PlatenIcons.Eraser), Lasso(PlatenIcons.Lasso), Text(PlatenIcons.Text),
    Image(PlatenIcons.Image), Table(PlatenIcons.Table), Laser(PlatenIcons.Laser),
}

/** 펜꽂이 슬롯(5개). 브러시 id + 색 + 굵기 + 획 오버라이드(펜 시트 전용, PLAN §12-9). */
@Serializable
data class PenSlot(
    val brushId: String,
    val color: Int = PlatenPalette.BLACK,
    val size: Float = 3f,
    val opacity: Float = 1f,
    val overrides: StrokeOverrides? = null,
)

/** UI 전용 환경설정(core-model AppSettings 밖): 펜꽂이·마우스 필기·마지막 노트. */
@Serializable
data class UiPrefs(
    val slots: List<PenSlot> = emptyList(),
    val selectedSlot: Int = 0,
    /** 데스크톱 진단용. 기본 off(BRIEF 2b: Windows 마우스 필기 없음). */
    val mouseDraw: Boolean = false,
    val lastNoteId: String? = null,
    val eraserRadiusPt: Float = 6f,
)

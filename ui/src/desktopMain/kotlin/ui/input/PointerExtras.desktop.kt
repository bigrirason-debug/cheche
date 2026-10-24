package io.github.bigrirason_debug.platen.ui.input

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange

/** 데스크톱: 펜 없음(마우스/터치) → 공통 구현 그대로. */
actual object PlatformPointerExtras {
    actual fun describe(event: PointerEvent, change: PointerInputChange): PointerExtra = CommonPointerExtras.describe(event, change)
}

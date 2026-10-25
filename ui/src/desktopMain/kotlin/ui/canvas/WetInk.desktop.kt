package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/** 데스크톱은 마우스 편집 전용(필기 없음) — 프론트버퍼 없음. */
@Composable
actual fun WetInkLayer(enabled: Boolean, modifier: Modifier, onOverlay: (WetInkOverlay?) -> Unit) {
    LaunchedEffect(Unit) { onOverlay(null) }
}

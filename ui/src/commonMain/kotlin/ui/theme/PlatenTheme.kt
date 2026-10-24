package io.github.bigrirason_debug.platen.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bigrirason_debug.platen.model.ThemeMode

/**
 * 디자인 토큰(docs/design/DESIGN.md §1, typewriter-refs §8.3 B 표). 값은 DESIGN.md 가 원본이다.
 * `red` 는 마진 마커·활성 상태·빨강 잉크에만 쓴다(화면당 1% 미만).
 */
@Immutable
data class PlatenColors(
    val dark: Boolean,
    val paper: Color,
    val chrome: Color,
    val chromeRaised: Color,
    val ink: Color,
    val red: Color,
    /** 비텍스트 전용(눈금 틱·비활성 아이콘) — 라이트 크롬 위 2.98:1 */
    val muted: Color,
    /** 보조 텍스트 전용(AA ≥4.5:1, DESIGN §1) */
    val mutedText: Color,
    val highlighter: Color,
) {
    /** 줄 가이드·눈금 틱 = 잉크 20% (typewriter-refs §8.3) */
    val rule: Color get() = ink.copy(alpha = 0.20f)
    val selectionFill: Color get() = red.copy(alpha = 0.12f)
    val scrim: Color get() = Color.Black.copy(alpha = if (dark) 0.48f else 0.32f)

    companion object {
        val Light = PlatenColors(
            dark = false,
            paper = Color(0xFFF6F1E7),
            chrome = Color(0xFFECE7DC),
            chromeRaised = Color(0xFFF2EEE5),
            ink = Color(0xFF1A1A1A),
            red = Color(0xFFC8102E),
            muted = Color(0xFF8A857B),
            mutedText = Color(0xFF66615A),
            highlighter = Color(0xFFFFE44D),
        )
        val Dark = PlatenColors(
            dark = true,
            paper = Color(0xFF1C1B19),
            chrome = Color(0xFF121212),
            chromeRaised = Color(0xFF1A1A1A),
            ink = Color(0xFFE8E4DC),
            red = Color(0xFFF26B5E),
            muted = Color(0xFF8E8A82),
            mutedText = Color(0xFFA39E95),
            highlighter = Color(0xFFFFE44D),
        )
    }
}

/** 간격 스케일 4·8·16·24·32·48 dp (DESIGN §3). */
object PlatenSpacing {
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 16.dp
    val l: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val toolbarHeight: Dp = 48.dp
    val rulerHeight: Dp = 8.dp
    val iconSize: Dp = 24.dp
    val target: Dp = 48.dp
    val carriageHeight: Dp = 72.dp
}

/** 라운드: 버튼 4 · 패널 8 · 페이지 0, 필 금지 (DESIGN §3). */
object PlatenShapes {
    val button = RoundedCornerShape(4.dp)
    val panel = RoundedCornerShape(8.dp)
    val page = RoundedCornerShape(0.dp)
}

/** 펜 설정 시트 팔레트 8색 (DESIGN §5.1). ARGB Int = 문서 모델 색 표현. */
object PlatenPalette {
    const val BLACK: Int = 0xFF1A1A1A.toInt()
    const val RED: Int = 0xFFC8102E.toInt()
    const val BLUE: Int = 0xFF2458B3.toInt()
    const val GREEN: Int = 0xFF2E7D4F.toInt()
    const val ORANGE: Int = 0xFFD9731A.toInt()
    const val PURPLE: Int = 0xFF6D3FB3.toInt()
    const val GRAY: Int = 0xFF6B665C.toInt()
    const val YELLOW: Int = 0xFFFFE44D.toInt()
    val all: List<Int> = listOf(BLACK, RED, BLUE, GREEN, ORANGE, PURPLE, GRAY, YELLOW)

    /** 리본 토글: 빨강이면 검정, 검정이면 빨강, 그 외 색이면 검정 (PLAN §12-9 의미 고정). */
    fun ribbonToggle(current: Int): Int = when (current) {
        RED -> BLACK
        BLACK -> RED
        else -> BLACK
    }
}

val LocalPlatenColors = staticCompositionLocalOf { PlatenColors.Light }
val LocalReducedMotion = staticCompositionLocalOf { false }
/** 1.0 = 48dp 타깃(Android), 0.85 ≈ 40dp(Windows) — DESIGN §7 */
val LocalUiDensity = staticCompositionLocalOf { 1f }

object PlatenTheme {
    val colors: PlatenColors @Composable get() = LocalPlatenColors.current
    val reducedMotion: Boolean @Composable get() = LocalReducedMotion.current
}

/** reducedMotion 이면 snap, 아니면 짧은 tween(≤150ms, DESIGN §4). */
@Composable
fun <T> platenMotion(durationMillis: Int = 150): AnimationSpec<T> =
    if (LocalReducedMotion.current) snap() else tween(durationMillis)

fun Int.toComposeColor(): Color = Color(this.toLong() and 0xFFFFFFFFL)

private fun colorScheme(c: PlatenColors): ColorScheme {
    val base = if (c.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.ink,
        onPrimary = c.paper,
        secondary = c.mutedText,
        onSecondary = c.paper,
        tertiary = c.red,
        background = c.chrome,
        onBackground = c.ink,
        surface = c.chrome,
        onSurface = c.ink,
        surfaceVariant = c.chromeRaised,
        onSurfaceVariant = c.mutedText,
        surfaceContainer = c.chromeRaised,
        surfaceContainerHigh = c.chromeRaised,
        surfaceContainerHighest = c.chromeRaised,
        surfaceContainerLow = c.chrome,
        surfaceContainerLowest = c.chrome,
        outline = c.muted,
        outlineVariant = c.rule,
        error = c.red,
        onError = c.paper,
        scrim = Color.Black,
    )
}

/** 모노는 숫자·눈금·단축키에만(DESIGN §2). */
val MonoTextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)

private val typography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = MonoTextStyle,
)

/** 테마 루트. mode=system 이면 플랫폼 다크 설정을 따른다. */
@Composable
fun PlatenTheme(
    mode: ThemeMode = ThemeMode.system,
    reducedMotion: Boolean = false,
    uiDensity: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.system -> isSystemInDarkTheme()
        ThemeMode.light -> false
        ThemeMode.dark -> true
    }
    val colors = if (dark) PlatenColors.Dark else PlatenColors.Light
    CompositionLocalProvider(
        LocalPlatenColors provides colors,
        LocalReducedMotion provides reducedMotion,
        LocalUiDensity provides uiDensity,
    ) {
        MaterialTheme(
            colorScheme = colorScheme(colors),
            typography = typography,
            shapes = Shapes(
                extraSmall = PlatenShapes.button,
                small = PlatenShapes.button,
                medium = PlatenShapes.panel,
                large = PlatenShapes.panel,
                extraLarge = PlatenShapes.panel,
            ),
            content = content,
        )
    }
}

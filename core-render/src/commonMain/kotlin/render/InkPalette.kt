package io.github.bigrirason_debug.platen.render

import kotlin.math.pow

/**
 * B안 디자인 토큰(typewriter-refs §8.3 "방향 B — 모던 미니멀 + 타자기 암시") + WCAG 2.x 대비 계산.
 * 색은 ARGB Int. 라이트/다크는 반전이 아니라 의미색별 2값 토큰(§7.1).
 */
object InkPalette {
    // §8.3 B안 표 — paper
    val paperLight: Int = 0xFFF6F1E7.toInt()
    val paperDark: Int = 0xFF1C1B19.toInt()

    // §8.3 B안 표 — chrome(크롬 다크 + 종이 라이트 조합 허용)
    val chromeLight: Int = 0xFFECE7DC.toInt()
    val chromeDark: Int = 0xFF121212.toInt()

    // §8.3 B안 표 — ink.default (대비 15.46 / 13.57, §7.3)
    val inkLight: Int = 0xFF1A1A1A.toInt()
    val inkDark: Int = 0xFFE8E4DC.toInt()

    // §8.3 B안 표 — ink.red (대비 5.23 / 5.77, §7.3). 마진 마커·활성 상태·빨강 잉크에만.
    val redLight: Int = 0xFFC8102E.toInt()
    val redDark: Int = 0xFFF26B5E.toInt()

    // §8.3 B안 표 — muted(눈금·비활성 아이콘)
    val mutedLight: Int = 0xFF8A857B.toInt()
    val mutedDark: Int = 0xFF8E8A82.toInt()

    /** §8.3 B안 표 — rule = ink.default @20% (줄 가이드·눈금 틱) */
    const val RULE_ALPHA: Float = 0.20f
    val ruleLight: Int = withAlpha(inkLight, RULE_ALPHA)
    val ruleDark: Int = withAlpha(inkDark, RULE_ALPHA)

    /** §8.3 B안 표 — hl.yellow: 라이트 multiply / 다크 screen + belowInk(§7.2) */
    val hlYellow: Int = 0xFFFFE44D.toInt()
    /** §7.2 구현 메모: 형광펜 레이어는 테마와 무관하게 잉크 아래. */
    const val HL_BELOW_INK: Boolean = true

    /** 마진 스톱 여백선 알파(PLAN §4.8; 1px 빨강 가이드가 잉크와 경쟁하지 않도록 60%) */
    const val MARGIN_ALPHA: Float = 0.60f

    /** 테마에 맞는 토큰 묶음. */
    data class Tokens(val paper: Int, val chrome: Int, val ink: Int, val red: Int, val muted: Int, val rule: Int, val hlYellow: Int, val hlScreen: Boolean)

    val light: Tokens = Tokens(paperLight, chromeLight, inkLight, redLight, mutedLight, ruleLight, hlYellow, hlScreen = false)
    val dark: Tokens = Tokens(paperDark, chromeDark, inkDark, redDark, mutedDark, ruleDark, hlYellow, hlScreen = true)

    /** [InkTheme] 에 맞는 토큰(테마의 ink/red 재정의를 반영). */
    fun of(theme: InkTheme): Tokens =
        if (theme.dark) dark.copy(paper = theme.paper, ink = theme.inkDefaultDark, red = theme.redDark, rule = withAlpha(theme.inkDefaultDark, RULE_ALPHA))
        else light.copy(paper = theme.paper, ink = theme.inkDefaultLight, red = theme.redLight, rule = withAlpha(theme.inkDefaultLight, RULE_ALPHA))

    /** ARGB 의 알파만 교체(0..1). */
    fun withAlpha(argb: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (a shl 24) or (argb and 0x00FFFFFF)
    }

    /** WCAG 2.x 상대 휘도(sRGB → 선형, 알파 무시). */
    fun relativeLuminance(argb: Int): Double {
        fun lin(c8: Int): Double {
            val c = c8 / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = lin((argb shr 16) and 0xFF)
        val g = lin((argb shr 8) and 0xFF)
        val b = lin(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG 2.x 대비비(≥1). 순서 무관. §7.3 표의 수치(#1A1A1A/#F6F1E7 = 15.46)를 재현한다. */
    fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }
}

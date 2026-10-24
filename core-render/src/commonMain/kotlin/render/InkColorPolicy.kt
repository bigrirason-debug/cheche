package io.github.bigrirason_debug.platen.render

import androidx.compose.ui.graphics.BlendMode as ComposeBlend
import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.BrushRole
import io.github.bigrirason_debug.platen.model.BrushSpec
import kotlin.math.abs

/**
 * 다크 모드 색 재매핑·혼합 모드 정책(typewriter-refs §7).
 * 저장된 획 색은 그대로 두고 렌더 시점에만 바꾼다(픽셀 반전 금지 — 빨강이 청록으로 뒤집히는 문제, §7.1).
 */
object InkColorPolicy {
    /** "검정 계열" 판정: HSL 명도 < 0.25 이고 채도 낮음(§7.1 ink.default 재매핑 대상) */
    private const val BLACK_MAX_L = 0.25f
    private const val BLACK_MAX_S = 0.25f

    /** "빨강 계열" 판정: 색상 345°~15°, 채도 높음(§7.3 ink.red 재매핑 대상) */
    private const val RED_HUE_LO = 345f
    private const val RED_HUE_HI = 15f
    private const val RED_MIN_S = 0.5f

    /** 그 외 어두운 색: 다크 종이에서 사라지지 않도록 명도만 끌어올린다(L < 0.35 → L += 0.4, 클램프). */
    private const val LIFT_MAX_L = 0.35f
    private const val LIFT_AMOUNT = 0.4f

    /**
     * 획 색을 테마에 맞게 재매핑. 라이트 테마면 그대로. 형광펜(role=highlighter)은 색을 유지하고 혼합 모드만 바뀐다(§7.2).
     * 알파는 항상 보존한다.
     */
    fun remapInk(color: Int, role: BrushRole, theme: InkTheme): Int {
        if (!theme.dark) return color
        if (role == BrushRole.highlighter) return color
        val a = color ushr 24
        val hsl = toHsl(color)
        val (h, s, l) = hsl
        val rgb: Int = when {
            l < BLACK_MAX_L && s < BLACK_MAX_S -> theme.inkDefaultDark
            s >= RED_MIN_S && (h >= RED_HUE_LO || h <= RED_HUE_HI) -> theme.redDark
            l < LIFT_MAX_L -> fromHsl(h, s, (l + LIFT_AMOUNT).coerceAtMost(1f))
            else -> color
        }
        return (a shl 24) or (rgb and 0x00FFFFFF)
    }

    /**
     * brush.json 혼합 모드 → Compose 혼합 모드.
     * multiply(또는 형광펜 역할)는 라이트 Multiply / 다크 Screen(§7.2: 다크 종이에서 Multiply 는 보이지 않음),
     * behind → DstOver, erase → Clear(PageRenderer 가 잉크 레이어 안에서만 적용).
     */
    fun blendModeFor(spec: BrushSpec, theme: InkTheme): ComposeBlend = when {
        spec.blend == BlendMode.erase -> ComposeBlend.Clear
        spec.blend == BlendMode.behind -> ComposeBlend.DstOver
        spec.blend == BlendMode.multiply || spec.role == BrushRole.highlighter ->
            if (theme.dark) ComposeBlend.Screen else ComposeBlend.Multiply
        else -> ComposeBlend.SrcOver
    }

    /** 형광펜 역할 또는 multiply 혼합이면 잉크 아래 레이어에 그린다(§7.2 hl.zOrder = belowInk). */
    fun isBelowInk(spec: BrushSpec): Boolean = spec.role == BrushRole.highlighter || spec.blend == BlendMode.multiply

    /** RGB(0..255) → HSL(h 0..360, s 0..1, l 0..1) */
    internal fun toHsl(argb: Int): Triple<Float, Float, Float> {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        val d = max - min
        if (d < 1e-6f) return Triple(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        var h = when (max) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        if (h < 0f) h += 360f
        return Triple(h, s, l)
    }

    /** HSL → 불투명 RGB(알파 FF) */
    internal fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = (h % 360f) / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val (r1, g1, b1) = when {
            hp < 1f -> Triple(c, x, 0f)
            hp < 2f -> Triple(x, c, 0f)
            hp < 3f -> Triple(0f, c, x)
            hp < 4f -> Triple(0f, x, c)
            hp < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        fun ch(v: Float) = ((v + m).coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (0xFF shl 24) or (ch(r1) shl 16) or (ch(g1) shl 8) or ch(b1)
    }
}

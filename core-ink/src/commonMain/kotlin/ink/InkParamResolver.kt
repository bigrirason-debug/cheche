package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.ExposedParam
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.StrokeOverrides

/**
 * 브러시 spec + 전역 설정 + 획 오버라이드 → [ResolvedInkParams] (PLAN §4.3 2층 구조).
 *
 * 우선순위: 획 오버라이드 > 브러시 spec > 전역 settings.
 * - streamline / stabilize / taperEnd: `overrides ?: (브러시가 ExposedParam 으로 노출하면 spec 값, 아니면 전역값)`.
 * - 필압 커브: 전역 커브(입력 보정, brush-formats §3.4)를 먼저 적용하고 그 위에 브러시 커브 → LUT[p] = brush(global(p)).
 *   amount 반영: 비율 = 1 − amount + amount·curve(p). 오버라이드 pressureAmount/pressureCurve 는 크기 커브에만 적용.
 */
object InkParamResolver {
    fun resolve(spec: BrushSpec, size: Float, opacity: Float, settings: InkSettings, overrides: StrokeOverrides?): ResolvedInkParams {
        val exposed = spec.exposed
        val streamline = (overrides?.streamline
            ?: if (ExposedParam.streamline in exposed) spec.smoothing.streamline else settings.streamline).coerceIn(0f, 1f)
        val stabilize = (overrides?.stabilize
            ?: if (ExposedParam.stabilize in exposed) spec.smoothing.stabilize else settings.stabilize).coerceIn(0f, 1f)
        val taperEnd = (overrides?.taperEnd
            ?: if (ExposedParam.taperEnd in exposed) spec.taper.endLength else settings.taperEndLength).coerceAtLeast(0f)

        val globalLut = Interp.buildLut(settings.globalPressureCurve)

        val sizeAmount = overrides?.pressureAmount ?: spec.pressureSize.amount
        val sizeCurve = overrides?.pressureCurve ?: spec.pressureSize.curve
        val sizeLut = Interp.applyAmount(Interp.composeLut(Interp.buildLut(sizeCurve), globalLut), sizeAmount)

        val opacityLut = Interp.applyAmount(
            Interp.composeLut(Interp.buildLut(spec.pressureOpacity.curve), globalLut),
            spec.pressureOpacity.amount,
        )

        return ResolvedInkParams(
            spec = spec,
            size = if (size.isFinite()) size.coerceAtLeast(MIN_SIZE) else spec.size.base,
            opacity = if (opacity.isFinite()) opacity.coerceIn(0f, 1f) else spec.opacity,
            streamline = streamline,
            stabilize = stabilize,
            taperStartLength = spec.taper.startLength.coerceAtLeast(0f),
            taperEndLength = taperEnd,
            taperAuto = spec.taper.auto,
            pressureSizeLut = sizeLut,
            pressureOpacityLut = opacityLut,
        )
    }

    /** 지름 하한(pt) — 0 지름은 지오메트리 퇴화를 일으키므로 막는다. */
    private const val MIN_SIZE = 0.01f
}

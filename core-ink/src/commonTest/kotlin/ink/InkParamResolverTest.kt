package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.CurvePoint
import io.github.bigrirason_debug.platen.model.ExposedParam
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.PressureMapping
import io.github.bigrirason_debug.platen.model.Smoothing
import io.github.bigrirason_debug.platen.model.StrokeOverrides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InkParamResolverTest {
    private val spec = BrushSpec(id = "t", name = "t")
    private val sCurve = listOf(CurvePoint(0f, 0f), CurvePoint(0.3f, 0.1f), CurvePoint(0.6f, 0.8f), CurvePoint(1f, 1f))

    private fun assertMonotone(lut: FloatArray) {
        for (i in 1 until lut.size) assertTrue(lut[i] >= lut[i - 1] - 1e-6f, "lut[$i]=${lut[i]} < lut[${i - 1}]=${lut[i - 1]}")
    }

    @Test
    fun lutIsMonotoneWithFixedEndpoints() {
        val lut = Interp.buildLut(sCurve)
        assertEquals(256, lut.size)
        assertEquals(0f, lut[0], 1e-6f)
        assertEquals(1f, lut[255], 1e-6f)
        assertMonotone(lut)
        // 제어점을 정확히 지난다
        assertEquals(0.1f, Interp.monotoneCubic(sCurve).eval(0.3f), 1e-5f)
        assertEquals(0.8f, Interp.monotoneCubic(sCurve).eval(0.6f), 1e-5f)
        // 합성(전역 ∘ 브러시)도 단조·끝점 유지
        val composed = Interp.composeLut(lut, Interp.buildLut(listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.7f), CurvePoint(1f, 1f))))
        assertEquals(0f, composed[0], 1e-6f)
        assertEquals(1f, composed[255], 1e-6f)
        assertMonotone(composed)
    }

    @Test
    fun sanitizeFixesOrderMonotonicityAndEndpoints() {
        val messy = listOf(CurvePoint(0.7f, 0.2f), CurvePoint(0.2f, 0.5f), CurvePoint(1.5f, 0.9f), CurvePoint(-1f, 0.4f))
        val s = Interp.sanitizeCurve(messy)
        assertEquals(CurvePoint(0f, 0f), s.first())
        assertEquals(CurvePoint(1f, 1f), s.last())
        for (i in 1 until s.size) {
            assertTrue(s[i].x > s[i - 1].x)
            assertTrue(s[i].y >= s[i - 1].y)
        }
        assertEquals(4, s.size)
        assertEquals(listOf(0f, 0f, 0.5f, 0.5f, 1f), listOf(s[0].y, s[0].x, s[1].y, s[2].y, s[3].y).map { it })
        // 빈 입력 → 선형
        assertEquals(listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)), Interp.sanitizeCurve(emptyList()))
    }

    @Test
    fun resolvedLutReflectsAmount() {
        val p = ResolvedInkParams.resolve(spec.copy(pressureSize = PressureMapping(amount = 0.7f, curve = sCurve)), 4f, 1f, InkSettings(), null)
        assertEquals(1f - 0.7f, p.pressureSizeLut[0], 1e-6f)
        assertEquals(1f, p.pressureSizeLut[255], 1e-6f)
        assertMonotone(p.pressureSizeLut)
        // amount = 0 → 상수 1
        val zero = ResolvedInkParams.resolve(spec.copy(pressureSize = PressureMapping(amount = 0f, curve = sCurve)), 4f, 1f, InkSettings(), null)
        assertTrue(zero.pressureSizeLut.all { it == 1f })
        // 기본 pressureOpacity amount=0 → 상수 1
        assertTrue(p.pressureOpacityLut.all { it == 1f })
    }

    @Test
    fun globalCurveIsAppliedAsInputCalibration() {
        val settings = InkSettings(globalPressureCurve = listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.9f), CurvePoint(1f, 1f)))
        val a = ResolvedInkParams.resolve(spec.copy(pressureSize = PressureMapping(amount = 1f)), 4f, 1f, InkSettings(), null)
        val b = ResolvedInkParams.resolve(spec.copy(pressureSize = PressureMapping(amount = 1f)), 4f, 1f, settings, null)
        // 가벼운 필압이 전역 커브로 끌어올려진다
        assertTrue(Interp.lookup(b.pressureSizeLut, 0.5f) > Interp.lookup(a.pressureSizeLut, 0.5f) + 0.3f)
        assertEquals(1f, b.pressureSizeLut[255], 1e-6f)
    }

    @Test
    fun overridePriority() {
        val settings = InkSettings(streamline = 0.5f, stabilize = 0.6f, taperEndLength = 20f)
        val brush = spec.copy(
            smoothing = Smoothing(streamline = 0.3f, stabilize = 0.1f),
            exposed = listOf(ExposedParam.size, ExposedParam.streamline), // stabilize·taperEnd 는 노출 안 함 → 전역
        )
        val none = ResolvedInkParams.resolve(brush, 4f, 1f, settings, null)
        assertEquals(0.3f, none.streamline) // 브러시가 노출 → spec 값
        assertEquals(0.6f, none.stabilize) // 노출 안 함 → 전역
        assertEquals(20f, none.taperEndLength) // 노출 안 함 → 전역
        assertEquals(brush.taper.auto, none.taperAuto)

        val ov = ResolvedInkParams.resolve(
            brush, 4f, 1f, settings,
            StrokeOverrides(streamline = 0.9f, stabilize = 0.2f, taperEnd = 3f, pressureAmount = 0f),
        )
        assertEquals(0.9f, ov.streamline) // 획 오버라이드 > 브러시
        assertEquals(0.2f, ov.stabilize) // 획 오버라이드 > 전역
        assertEquals(3f, ov.taperEndLength)
        assertTrue(ov.pressureSizeLut.all { it == 1f })

        val curveOv = ResolvedInkParams.resolve(brush, 4f, 1f, settings, StrokeOverrides(pressureCurve = sCurve, pressureAmount = 1f))
        assertEquals(0.1f, Interp.lookup(curveOv.pressureSizeLut, 0.3f), 1e-3f)
    }
}

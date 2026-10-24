package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushRole
import io.github.bigrirason_debug.platen.model.BrushSize
import io.github.bigrirason_debug.platen.model.BrushSource
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.CurvePoint
import io.github.bigrirason_debug.platen.model.ExposedParam
import io.github.bigrirason_debug.platen.model.Grain
import io.github.bigrirason_debug.platen.model.GrainMode
import io.github.bigrirason_debug.platen.model.PressureMapping
import io.github.bigrirason_debug.platen.model.Smoothing
import io.github.bigrirason_debug.platen.model.Stamp
import io.github.bigrirason_debug.platen.model.StampTip
import io.github.bigrirason_debug.platen.model.StrokeCap
import io.github.bigrirason_debug.platen.model.Taper
import io.github.bigrirason_debug.platen.model.Tilt
import io.github.bigrirason_debug.platen.model.Velocity

/**
 * 1차 내장 로스터 9종(PLAN A8: 펜 2·만년필 2·연필 2·모노라인 1·형광펜 2).
 * 노출 파라미터(`exposed`)는 스타노트 관찰(PLAN §4.3: 만년필 = 압감·두께·안정화 / 연필 = 압감·두께·밀도)을 따른다.
 * 모든 값·이미지는 결정적이어서 패키지 id(zip SHA-1)가 빌드·플랫폼에 관계없이 같다.
 */
object BuiltInBrushes {
    /** 절차적 팁 크기(px). 64² = §3.7 성능 기준 표의 전제. */
    const val PENCIL_TIP_SIZE = 64
    /** 절차적 그레인 타일 한 변(px). */
    const val PENCIL_GRAIN_SIZE = 256
    /** 노이즈 시드(고정). */
    const val GRAIN_SEED = 7

    private val native = BrushSource(format = "native")
    private val linear = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
    /** 만년필용 완만한 S 커브(가벼운 필압에서 얇게 유지). */
    private val fountainCurve = listOf(CurvePoint(0f, 0f), CurvePoint(0.35f, 0.2f), CurvePoint(0.7f, 0.65f), CurvePoint(1f, 1f))

    val penFine = BrushSpec(
        id = "pen-fine", name = "Pen Fine", source = native, engine = BrushEngine.profile, role = BrushRole.pen,
        size = BrushSize(base = 2f, min = 0.5f, max = 12f),
        pressureSize = PressureMapping(amount = 0.7f, curve = linear),
        taper = Taper(startLength = 0f, endLength = 12f),
        smoothing = Smoothing(streamline = 0.3f),
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline),
    )

    val penBold = penFine.copy(
        id = "pen-bold", name = "Pen Bold",
        size = BrushSize(base = 4f, min = 1f, max = 20f),
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline, ExposedParam.taperEnd),
    )

    val fountainSoft = BrushSpec(
        id = "fountain-soft", name = "Fountain Soft", source = native, engine = BrushEngine.profile, role = BrushRole.fountain,
        size = BrushSize(base = 3f, min = 0.5f, max = 16f),
        pressureSize = PressureMapping(amount = 0.8f, curve = fountainCurve),
        pressureOpacity = PressureMapping(amount = 0.2f, curve = linear),
        velocity = Velocity(size = -0.2f),
        taper = Taper(startLength = 6f, endLength = 16f),
        smoothing = Smoothing(streamline = 0.4f, stabilize = 0.1f),
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline, ExposedParam.stabilize),
    )

    val fountainFlex = BrushSpec(
        id = "fountain-flex", name = "Fountain Flex", source = native, engine = BrushEngine.profile, role = BrushRole.fountain,
        size = BrushSize(base = 3f, min = 0.5f, max = 18f),
        pressureSize = PressureMapping(amount = 0.9f, curve = fountainCurve),
        pressureOpacity = PressureMapping(amount = 0.3f, curve = linear),
        velocity = Velocity(size = -0.3f),
        taper = Taper(startLength = 6f, endLength = 18f),
        smoothing = Smoothing(streamline = 0.4f),
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline),
    )

    private fun pencil(id: String, name: String, grainDepth: Float, base: Float, opacity: Float) = BrushSpec(
        id = id, name = name, source = native, engine = BrushEngine.stamp, role = BrushRole.pencil,
        size = BrushSize(base = base, min = 1f, max = 24f),
        opacity = opacity, flow = 0.8f,
        pressureSize = PressureMapping(amount = 0.5f, curve = linear),
        pressureOpacity = PressureMapping(amount = 0.6f, curve = linear),
        tilt = Tilt(size = 0.4f, opacity = 0.1f),
        taper = Taper(startLength = 0f, endLength = 6f),
        smoothing = Smoothing(streamline = 0.25f),
        stamp = Stamp(spacing = 0.08f, jitter = 0.02f, tip = StampTip(image = "tip.png", followStroke = 0f, randomStart = true)),
        grain = Grain(image = "grain.png", depth = grainDepth, scale = 1f, mode = GrainMode.textured),
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.grainDepth),
    )

    val pencilHb = pencil("pencil-hb", "Pencil HB", grainDepth = 0.6f, base = 2.5f, opacity = 0.85f)
    val pencil6b = pencil("pencil-6b", "Pencil 6B", grainDepth = 0.8f, base = 4f, opacity = 1f)

    val monoline = BrushSpec(
        id = "monoline", name = "Monoline", source = native, engine = BrushEngine.profile, role = BrushRole.pen,
        size = BrushSize(base = 3f, min = 0.5f, max = 24f),
        pressureSize = PressureMapping(amount = 0f, curve = linear),
        taper = Taper(startLength = 0f, endLength = 0f, auto = false),
        smoothing = Smoothing(streamline = 0.35f),
        exposed = listOf(ExposedParam.size, ExposedParam.streamline),
    )

    val highlighterFlat = BrushSpec(
        id = "highlighter-flat", name = "Highlighter Flat", source = native, engine = BrushEngine.profile, role = BrushRole.highlighter,
        size = BrushSize(base = 18f, min = 6f, max = 60f),
        opacity = 0.35f, cap = StrokeCap.flat, blend = BlendMode.multiply,
        pressureSize = PressureMapping(amount = 0f, curve = linear),
        taper = Taper(startLength = 0f, endLength = 0f, auto = false),
        smoothing = Smoothing(streamline = 0.5f, stabilize = 0.3f),
        exposed = listOf(ExposedParam.size, ExposedParam.opacity, ExposedParam.stabilize),
    )

    val highlighterRound = highlighterFlat.copy(
        id = "highlighter-round", name = "Highlighter Round", cap = StrokeCap.round,
        size = BrushSize(base = 14f, min = 4f, max = 48f),
    )

    val specs: List<BrushSpec> = listOf(penFine, penBold, fountainSoft, fountainFlex, pencilHb, pencil6b, monoline, highlighterFlat, highlighterRound)

    /** 연필 공용 팁(소프트 원 64px) PNG — 결정적. */
    val pencilTipPng: ByteArray by lazy { ImageCodec.encodePng(ProceduralImages.roundTip(PENCIL_TIP_SIZE, hardness = 0.55f)) }

    /** 연필 공용 그레인(노이즈 256px, 시드 고정) PNG — 결정적. */
    val pencilGrainPng: ByteArray by lazy { ImageCodec.encodePng(ProceduralImages.noiseGrain(PENCIL_GRAIN_SIZE, GRAIN_SEED)) }

    /** 9종 전부. stamp 엔진만 이미지를 가진다. */
    fun all(): List<LoadedBrush> = specs.map { spec ->
        if (spec.engine == BrushEngine.stamp) LoadedBrush(spec, pencilTipPng, pencilGrainPng) else LoadedBrush(spec, null, null)
    }

    fun byId(id: String): LoadedBrush? = all().firstOrNull { it.spec.id == id }
}

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
import io.github.bigrirason_debug.platen.model.Taper
import io.github.bigrirason_debug.platen.model.Tilt
import io.github.bigrirason_debug.platen.model.Velocity
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Procreate `Brush.archive`(평탄 사전) + Shape/Grain 이미지 → [LoadedBrush]. 키 매핑은 brush-formats §3.3 손실표 그대로.
 * 플랫폼 무관(이미지는 이미 [GrayImage] 로 디코드되어 들어온다).
 */
object ProcreateMapper {
    /**
     * 크기 단위 K: `maxSize`/`minSize` 1.0 = K pt. **가정**(§1.3 "단위 미확인", 후보 100/120/200) — LOCAL_TODO B1(실기 샘플 L1/L3)로 보정.
     */
    const val SIZE_UNIT_K = 100f

    /**
     * 테이퍼 길이 단위 L: `pencilTaper*Length` 1.0 = L pt. **가정**(§1.3 "길이 1.0이 몇 px인지 미확인", NekoPhoto 300 추측) — LOCAL_TODO B1 로 보정.
     */
    const val TAPER_UNIT_L = 300f

    /** 문서 크기 클램프(§3.2 size.base 0.5–200). */
    private const val SIZE_MIN = 0.5f
    private const val SIZE_MAX = 200f

    /** 임포터가 소비한 키(이 밖의 키는 lossNotes 에 "버림" 으로 기록). */
    private val consumed = setOf(
        "name", "authorName", "plotSpacing", "plotJitter", "plotSmoothing", "plotMovingAverageStabilization",
        "maxSize", "minSize", "paintSize", "maxOpacity", "minOpacity", "paintOpacity", "dynamicsGlazedFlow", "dynamicsGlazeFlow",
        "dynamicsPressureSize", "dynamicsPressureSizeCurve", "dynamicsPressureOpacity", "dynamicsPressureOpacityCurve",
        "dynamicsSpeedSize", "dynamicsTiltSize", "dynamicsTiltOpacity", "dynamicsJitterSize", "dynamicsJitterOpacity",
        "pencilTaperStartLength", "pencilTaperEndLength", "pencilTaperSize", "pencilTaperOpacity",
        "shapeRotation", "shapeAngle", "shapeScatter", "shapeRandomise", "shapeFlipXJitter", "shapeFlipYJitter", "shapeRoundness",
        "shapeCount", "shapeAzimuth", "shapeInverted", "bundledShapePath",
        "textureApplication", "textureMovement", "textureScale", "grainDepth", "textureInverted", "textureBrightness", "textureContrast", "bundledGrainPath",
        "blendMode", "extendedBlend", "version", "plotSpacingVersion", "taperVersion", "\$classname",
    )

    /** 메타·앱 상태 키: 버림으로 기록하지 않는다. */
    private val silent = setOf(
        "creationDate", "previewSize", "preview", "previewRotation", "previewSizeOffset", "color", "signedID", "resetSignedID",
        "eraseSize", "eraseOpacity", "smudgeSize", "smudgeOpacity", "savedSizes", "savedOpacities", "stamp", "importedFromABR", "maxPressureSizeClamped",
    )

    class Images(val shape: GrayImage?, val grain: GrainImage?)
    class GrainImage(val image: GrayImage)

    /**
     * @param archive [decodeNSKeyedArchive] 결과(없으면 "팁만 복구" 모드)
     * @param shape 디코드된 Shape.png(없으면 bundledShapePath 휴리스틱)
     * @param grain 디코드된 Grain.png/jpg(없으면 bundledGrainPath → 무그레인)
     */
    fun map(archive: Map<String, Any?>?, shape: GrayImage?, grain: GrayImage?, fallbackName: String, importedAt: Long): LoadedBrush {
        val notes = ArrayList<String>()
        val a = archive ?: emptyMap()
        if (archive == null) notes += "Brush.archive 해독 실패 → 팁만 복구(이름·이미지·기본 동역학)"

        val name = (a["name"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackName
        val author = a["authorName"] as? String

        // ---- 크기 (K 가정)
        val maxRaw = num(a["maxSize"]) ?: 1f
        val minRaw = num(a["minSize"]) ?: 0.01f
        var sizeMax = (maxRaw * SIZE_UNIT_K).coerceIn(SIZE_MIN, SIZE_MAX)
        var sizeMin = (minRaw * SIZE_UNIT_K).coerceIn(SIZE_MIN, sizeMax)
        if (sizeMin > sizeMax) sizeMin = sizeMax
        val paintSize = num(a["paintSize"])?.let { if (it > 1f) { notes += "paintSize=$it (>1, 구형 px 저장 추정) → 비율 0.5 로 대체"; 0.5f } else it } ?: 0.5f
        val base = (sizeMin + paintSize * (sizeMax - sizeMin)).coerceIn(SIZE_MIN, SIZE_MAX)
        if (a.containsKey("maxSize") || a.containsKey("minSize")) notes += "크기 단위 K=${SIZE_UNIT_K.toInt()} 가정(maxSize=$maxRaw)"

        // ---- 불투명/플로우
        val maxOp = num(a["maxOpacity"]) ?: 1f
        val minOp = num(a["minOpacity"]) ?: 0f
        val paintOp = num(a["paintOpacity"]) ?: 1f
        val opacity = (minOp + paintOp.coerceIn(0f, 1f) * (maxOp - minOp)).coerceIn(0.02f, 1f)
        val flow = (num(a["dynamicsGlazedFlow"]) ?: num(a["dynamicsGlazeFlow"]) ?: 1f).coerceIn(0.02f, 1f)

        // ---- 필압
        val pSize = PressureMapping(
            amount = (num(a["dynamicsPressureSize"]) ?: 0f).coerceIn(0f, 1f),
            curve = curveOf(a["dynamicsPressureSizeCurve"]),
        )
        val pOpacity = PressureMapping(
            amount = (num(a["dynamicsPressureOpacity"]) ?: 0f).coerceIn(0f, 1f),
            curve = curveOf(a["dynamicsPressureOpacityCurve"]),
        )
        if (a.containsKey("dynamicsPressureOpacity")) notes += "dynamicsPressureOpacity 는 Procreate 플로우 기준 → pressure.opacity 로 근사"

        // ---- 속도/틸트/지터
        val velocity = Velocity(size = (num(a["dynamicsSpeedSize"]) ?: 0f).coerceIn(-1f, 1f))
        val tilt = Tilt(size = (num(a["dynamicsTiltSize"]) ?: 0f).coerceIn(-1f, 1f), opacity = (num(a["dynamicsTiltOpacity"]) ?: 0f).coerceIn(0f, 1f))

        // ---- 테이퍼 (L 가정)
        val taperStart = num(a["pencilTaperStartLength"]) ?: 0f
        val taperEnd = num(a["pencilTaperEndLength"]) ?: 0f
        val taperSize = (num(a["pencilTaperSize"]) ?: 0f).coerceIn(0f, 1f)
        val taper = Taper(
            startLength = (taperStart * TAPER_UNIT_L).coerceIn(0f, 300f),
            endLength = (taperEnd * TAPER_UNIT_L).coerceIn(0f, 300f),
            startSize = taperSize, endSize = taperSize, auto = true,
        )
        if (a.containsKey("pencilTaperEndLength") || a.containsKey("pencilTaperStartLength")) notes += "테이퍼 길이 단위 L=${TAPER_UNIT_L.toInt()} 가정"
        if (a.keys.any { it.startsWith("taper") && it != "taperVersion" }) notes += "터치용 taper* 세트 버림(S펜 전용)"

        // ---- 스무딩
        val smoothing = Smoothing(
            streamline = (num(a["plotSmoothing"]) ?: 0.3f).coerceIn(0f, 1f),
            stabilize = (num(a["plotMovingAverageStabilization"]) ?: 0f).coerceIn(0f, 1f),
        )

        // ---- 스탬프 / 팁
        val spacingRaw = num(a["plotSpacing"])
        val spacing = if (spacingRaw == null) 0.15f else SpecUtil.clampSpacing(spacingRaw)
        if (spacingRaw != null && spacingRaw < SpecUtil.MIN_SPACING) notes += "plotSpacing=$spacingRaw → 하한 ${SpecUtil.MIN_SPACING} 로 클램프(성능 상한)"
        val angleRaw = num(a["shapeAngle"]) ?: 0f
        var angleDeg = ((angleRaw * 180f / PI.toFloat()) % 360f + 360f) % 360f // 라디안 가정(§1.3 shapeAngle 단위 추정)
        if (angleRaw != 0f) notes += "shapeAngle=$angleRaw 라디안으로 해석(단위 추정)"
        val countRaw = num(a["shapeCount"]) ?: 0f
        var count = (countRaw * 16f).roundToInt().coerceIn(1, 4)
        if ((countRaw * 16f).roundToInt() > 4) notes += "shapeCount ${(countRaw * 16f).roundToInt()} → 4 로 클램프"
        val tip = StampTip(
            image = "tip.png",
            roundness = (num(a["shapeRoundness"]) ?: 1f).coerceIn(0.01f, 1f),
            angle = angleDeg,
            followStroke = (num(a["shapeRotation"]) ?: 0f).coerceIn(-1f, 1f),
            scatterAngle = ((num(a["shapeScatter"]) ?: 0f) * 180f).coerceIn(0f, 180f),
            randomStart = bool(a["shapeRandomise"]),
            flipX = bool(a["shapeFlipXJitter"]), flipY = bool(a["shapeFlipYJitter"]),
            count = count,
            azimuth = bool(a["shapeAzimuth"]),
        )
        val stamp = Stamp(
            spacing = spacing,
            jitter = (num(a["plotJitter"]) ?: 0f).coerceIn(0f, 2f),
            sizeJitter = (num(a["dynamicsJitterSize"]) ?: 0f).coerceIn(0f, 1f),
            opacityJitter = (num(a["dynamicsJitterOpacity"]) ?: 0f).coerceIn(0f, 1f),
            tip = tip,
        )

        // ---- 블렌드
        val blendRaw = (num(a["blendMode"]) ?: 0f).roundToInt()
        val blend = when (blendRaw) {
            0 -> BlendMode.normal; 1 -> BlendMode.multiply; 18 -> BlendMode.behind
            else -> { notes += "blendMode=$blendRaw 미지원 → normal"; BlendMode.normal }
        }

        // ---- 셰이프 이미지
        val shapeInverted = bool(a["shapeInverted"])
        val tipLimit = ImageNormalizer.tipLimitFor(sizeMax)
        var tipImage: GrayImage = if (shape != null) {
            ImageNormalizer.normalizeTip(if (shapeInverted) ImageNormalizer.invert(shape) else shape, tipLimit)
        } else {
            val bundled = a["bundledShapePath"] as? String
            val synth = synthesizeTip(bundled)
            notes += if (bundled != null) "내장 셰이프 '$bundled' 는 동봉 불가 → 합성 팁(${synth.second})" else "Shape.png 없음 → 합성 원형 팁"
            synth.first
        }

        // ---- 그레인 이미지
        val grainDepthRaw = (num(a["grainDepth"]) ?: 0f).coerceIn(0f, 1f)
        var grainPng: ByteArray? = null
        var grainSpec = Grain()
        if (grain != null) {
            var g = grain
            if (bool(a["textureInverted"])) g = ImageNormalizer.invert(g)
            g = ImageNormalizer.brightnessContrast(g, num(a["textureBrightness"]) ?: 0f, num(a["textureContrast"]) ?: 0f)
            g = ImageNormalizer.normalizeGrain(g)
            grainPng = ImageCodec.encodePng(g)
            val mode = if ((num(a["textureApplication"]) ?: 1f).roundToInt() == 0) GrainMode.moving else GrainMode.textured
            val scaleRaw = num(a["textureScale"])
            val scale = if (scaleRaw == null || scaleRaw <= 0f) 1f else sqrt(scaleRaw).coerceIn(0.25f, 4f) // √ 관계 추정(§3.3)
            if (scaleRaw != null) notes += "textureScale=$scaleRaw → grain.scale=√raw 추정"
            grainSpec = Grain(image = "grain.png", depth = if (grainDepthRaw > 0f) grainDepthRaw else 0.5f, scale = scale, mode = mode, inverted = false)
            if (a.containsKey("textureBrightness") || a.containsKey("textureContrast")) notes += "textureBrightness/Contrast 를 이미지에 구움(공식 추정)"
        } else {
            val bundled = a["bundledGrainPath"] as? String
            if (bundled != null && !bundled.contains("Blank", ignoreCase = true)) {
                notes += "내장 그레인 '$bundled' 는 동봉 불가 → 그레인 없음(depth=$grainDepthRaw 보존)"
                grainSpec = Grain(image = null, depth = grainDepthRaw)
            }
        }

        // ---- 엔진 승격: 원형·단단한 팁 + 그레인 없음 → profile(§3.1)
        var engine = BrushEngine.stamp
        if (grainPng == null && tip.count == 1 && ImageNormalizer.isRoundHardTip(tipImage)) {
            engine = BrushEngine.profile
            notes += "원형 팁·그레인 없음 → profile 엔진으로 승격"
        }

        // ---- 버린 키 기록
        val dropped = a.keys.filter { it !in consumed && it !in silent }.sorted()
        if (dropped.isNotEmpty()) notes += "버림(${dropped.size}): " + dropped.joinToString(", ")
        (a["version"] as? Number)?.let { notes += "archive version=$it" }

        val role = when {
            grainPng != null -> BrushRole.pencil
            blend == BlendMode.multiply && opacity < 0.6f -> BrushRole.highlighter
            else -> BrushRole.pen
        }
        val exposed = if (grainPng != null) listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.grainDepth)
        else listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline)

        val spec = BrushSpec(
            id = "procreate-" + SpecUtil.slug(name),
            name = name, author = author,
            source = BrushSource(format = "procreate", originalName = name, importedAt = importedAt, lossNotes = notes),
            engine = engine,
            size = BrushSize(base = base, min = sizeMin, max = sizeMax),
            opacity = opacity, flow = flow,
            pressureSize = pSize, pressureOpacity = pOpacity,
            velocity = velocity, tilt = tilt, taper = taper, smoothing = smoothing,
            blend = blend, stamp = stamp, grain = grainSpec, exposed = exposed, role = role,
        )
        return LoadedBrush(spec, ImageCodec.encodePng(tipImage), grainPng)
    }

    /** 번들 셰이프 이름 휴리스틱(§3.3): round/hard → 선명한 원, soft → 페더 원, oval → 타원, 그 외 기본 원. */
    fun synthesizeTip(bundledName: String?): Pair<GrayImage, String> {
        val n = bundledName?.lowercase() ?: ""
        return when {
            n.contains("oval") -> ProceduralImages.roundTip(128, hardness = 0.8f, roundness = 0.6f) to "oval"
            n.contains("soft") || n.contains("airbrush") -> ProceduralImages.roundTip(128, hardness = 0.2f) to "soft"
            n.contains("hard") || n.contains("round") || n.contains("medium") -> ProceduralImages.roundTip(128, hardness = 0.9f) to "hard-round"
            else -> ProceduralImages.roundTip(128, hardness = 0.85f) to "default-round"
        }
    }

    /** `ValkyrieMagnitudinalCurve` → 제어점. `points` 는 "{x, y}" 문자열 리스트(§1.2). 없으면 선형. */
    fun curveOf(v: Any?): List<CurvePoint> {
        val m = v as? Map<*, *> ?: return SpecUtil.monotoneCurve(emptyList())
        val pts = (m["points"] as? List<*>)?.mapNotNull { parsePoint(it) } ?: emptyList()
        return SpecUtil.monotoneCurve(pts)
    }

    private val pointRegex = Regex("""\{\s*([-+0-9.eE]+)\s*,\s*([-+0-9.eE]+)\s*\}""")

    fun parsePoint(s: Any?): CurvePoint? {
        val str = s as? String ?: return null
        val mr = pointRegex.find(str) ?: return null
        val x = mr.groupValues[1].toFloatOrNull() ?: return null
        val y = mr.groupValues[2].toFloatOrNull() ?: return null
        return CurvePoint(x, y)
    }

    private fun num(v: Any?): Float? = when (v) {
        is Number -> v.toFloat().takeIf { it.isFinite() }
        is Boolean -> if (v) 1f else 0f
        else -> null
    }

    private fun bool(v: Any?): Boolean = when (v) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        else -> false
    }
}

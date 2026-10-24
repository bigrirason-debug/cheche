package io.github.bigrirason_debug.platen.model

import kotlinx.serialization.Serializable

/**
 * 브러시 정의(brush.json v1) — docs/research/brush-formats.md §3.2 채택(D-018).
 * 렌더 엔진 2종: profile(폭·불투명 프로필 스트로크) / stamp(팁 이미지 간격 배치 + 그레인).
 * 이미지(tip.png, grain.png)는 임포트 시 정규화되어 8-bit alpha로만 존재한다(§3.5).
 */
@Serializable
enum class BrushEngine { profile, stamp }

@Serializable
enum class StrokeCap { round, flat, square }

@Serializable
enum class BlendMode { normal, multiply, behind, erase }

@Serializable
enum class GrainMode { moving, textured }

@Serializable
data class CurvePoint(val x: Float, val y: Float)

/** 필압 커브 = 제어점 2~8(단조, 끝점 (0,0)/(1,1) 고정). 런타임은 256-LUT로 샘플링(§3.4). */
@Serializable
data class PressureMapping(
    val amount: Float = 0.7f,
    val curve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
)

@Serializable
data class BrushSize(val base: Float = 3f, val min: Float = 0.5f, val max: Float = 60f)

@Serializable
data class Taper(
    val startLength: Float = 0f,
    val endLength: Float = 12f,
    val startSize: Float = 0f,
    val endSize: Float = 0f,
    /** 펜을 뗄 때 속도 기반 자동 끝 테이퍼 */
    val auto: Boolean = true,
)

@Serializable
data class Smoothing(
    /** 지연 보간(스트림라인) 0~1 */
    val streamline: Float = 0.3f,
    /** 이동평균 안정화 0~1 */
    val stabilize: Float = 0f,
)

@Serializable
data class Velocity(val size: Float = 0f, val refSpeed: Float = 1500f)

@Serializable
data class Tilt(val size: Float = 0f, val opacity: Float = 0f)

@Serializable
data class StampTip(
    val image: String = "tip.png",
    val roundness: Float = 1f,
    val angle: Float = 0f,
    val followStroke: Float = 0f,
    val scatterAngle: Float = 0f,
    val randomStart: Boolean = false,
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val count: Int = 1,
    val azimuth: Boolean = false,
)

@Serializable
data class Stamp(
    val spacing: Float = 0.15f,
    val jitter: Float = 0f,
    val sizeJitter: Float = 0f,
    val opacityJitter: Float = 0f,
    val tip: StampTip = StampTip(),
)

@Serializable
data class Grain(
    val image: String? = null,
    val depth: Float = 0f,
    val scale: Float = 1f,
    val mode: GrainMode = GrainMode.textured,
    val inverted: Boolean = false,
    val rotateWithStroke: Float = 0f,
)

@Serializable
data class BrushSource(
    val format: String = "native",
    val originalName: String? = null,
    val importedAt: Long? = null,
    val lossNotes: List<String> = emptyList(),
)

/** 설정 화면에 노출할 슬라이더(스타노트처럼 펜마다 다른 파라미터를 보여준다 — D-012). */
@Serializable
enum class ExposedParam { size, opacity, pressureSize, pressureOpacity, streamline, stabilize, taperEnd, velocitySize, tiltSize, grainDepth, spacing }

@Serializable
data class BrushSpec(
    val schema: Int = 1,
    val id: String,
    val name: String,
    val author: String? = null,
    val source: BrushSource = BrushSource(),
    val engine: BrushEngine = BrushEngine.profile,
    val size: BrushSize = BrushSize(),
    val opacity: Float = 1f,
    val flow: Float = 1f,
    val pressureSize: PressureMapping = PressureMapping(),
    val pressureOpacity: PressureMapping = PressureMapping(amount = 0f),
    val velocity: Velocity = Velocity(),
    val tilt: Tilt = Tilt(),
    val taper: Taper = Taper(),
    val smoothing: Smoothing = Smoothing(),
    val cap: StrokeCap = StrokeCap.round,
    val blend: BlendMode = BlendMode.normal,
    val stamp: Stamp = Stamp(),
    val grain: Grain = Grain(),
    val exposed: List<ExposedParam> = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline),
    /** 형광펜 등 "다크 모드에서도 보이게" 렌더러가 특별 취급해야 하는 역할 */
    val role: BrushRole = BrushRole.pen,
)

@Serializable
enum class BrushRole { pen, fountain, pencil, highlighter, marker, eraser, laser }

/** 획 단위 사용자 오버라이드(브러시 기본값 위에 얹힘). null = 브러시 값 사용. */
@Serializable
data class StrokeOverrides(
    val streamline: Float? = null,
    val stabilize: Float? = null,
    val taperEnd: Float? = null,
    val pressureAmount: Float? = null,
    val pressureCurve: List<CurvePoint>? = null,
)

package io.github.bigrirason_debug.platen.ink

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.InkSettings
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Pt
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.model.StrokeOverrides

/*
 * core-ink 공개 계약(PLAN §4.3). 구현은 이 파일의 타입을 바꾸지 않는 범위에서 자유.
 * 좌표는 문서 pt. 렌더러(core-render)는 이 모듈의 출력(Outline/StampPlan)만 소비한다.
 */

/** 획 하나를 렌더하기 위해 합성된 최종 파라미터(브러시 + 전역 설정 + 획 오버라이드). */
data class ResolvedInkParams(
    val spec: BrushSpec,
    /** 100% 줌 기준 지름(pt) */
    val size: Float,
    val opacity: Float,
    val streamline: Float,
    val stabilize: Float,
    val taperStartLength: Float,
    val taperEndLength: Float,
    val taperAuto: Boolean,
    /** 256-엔트리 LUT: 입력 필압 0..1 → 크기 비율 0..1 (전역 커브 ∘ 브러시 커브, amount 반영) */
    val pressureSizeLut: FloatArray,
    val pressureOpacityLut: FloatArray,
) {
    companion object {
        fun resolve(spec: BrushSpec, size: Float, opacity: Float, settings: InkSettings, overrides: StrokeOverrides?): ResolvedInkParams =
            InkParamResolver.resolve(spec, size, opacity, settings, overrides)
    }
}

/** 스무딩이 끝난 중심선 점. radius = 반지름(pt), alpha = 0..1, dir = 진행 방향 단위벡터. */
data class PathPoint(val x: Float, val y: Float, val radius: Float, val alpha: Float, val dirX: Float, val dirY: Float, val t: Float)

/** profile 엔진 출력: 중심선 + 외곽선 폴리곤(좌측 점열 + 우측 점열 역순으로 닫힘). */
data class Outline(
    val center: List<PathPoint>,
    /** 닫힌 폴리곤(시계 방향 무관). 렌더러는 fill(evenOdd 아님, nonZero)로 그린다. */
    val polygon: FloatArray,
    val bounds: Rect,
)

/** stamp 엔진 출력: 각 스탬프의 위치·지름·알파·각도. */
data class StampInstance(val x: Float, val y: Float, val diameter: Float, val alpha: Float, val angleRad: Float, val variant: Int)
data class StampPlan(val stamps: List<StampInstance>, val bounds: Rect)

/**
 * 획 지오메트리 생성기(순수 함수, 스레드 안전).
 * - [smooth]: 원시 샘플 → 스트림라인/안정화 적용 + 필압·속도·틸트·테이퍼로 반지름/알파 결정.
 * - [outline]: profile 엔진용 외곽선.
 * - [stampPlan]: stamp 엔진용 배치. 간격은 base 지름 기준(성능 상한 고정, brush-formats §3.7).
 */
interface StrokeGeometry {
    fun smooth(samples: List<InputSample>, params: ResolvedInkParams): List<PathPoint>
    fun outline(center: List<PathPoint>, params: ResolvedInkParams): Outline
    fun stampPlan(center: List<PathPoint>, params: ResolvedInkParams, seed: Long): StampPlan

    companion object {
        val Default: StrokeGeometry get() = DefaultStrokeGeometry
    }
}

/**
 * 진행 중 획(젖은 잉크)을 증분으로 받는 빌더. UI 입력 레이어가 샘플을 밀어넣고,
 * 매 프레임 [committedPoints] + [pendingPoints] 를 그린다. [finish] 가 최종 Stroke 를 돌려준다.
 */
interface LiveStrokeBuilder {
    val params: ResolvedInkParams
    fun add(sample: InputSample)
    fun addAll(samples: List<InputSample>) = samples.forEach(::add)
    /** 스무딩이 확정된(더 이상 변하지 않는) 점들 */
    fun committedPoints(): List<PathPoint>
    /** 스트림라인 지연 때문에 아직 움직일 수 있는 꼬리 점들 */
    fun pendingPoints(): List<PathPoint>
    fun bounds(): Rect
    fun finish(id: String, brushId: String, color: Int, createdAt: Long): Stroke

    companion object {
        fun create(params: ResolvedInkParams, geometry: StrokeGeometry = StrokeGeometry.Default): LiveStrokeBuilder =
            DefaultLiveStrokeBuilder(params, geometry)
    }
}

/** 지우개·올가미용 히트 테스트. */
object StrokeHitTest {
    /** 점 p 가 획의 외곽선(반지름 + tolerance) 안에 있는가 */
    fun hits(stroke: Stroke, params: ResolvedInkParams, p: Pt, tolerance: Float, geometry: StrokeGeometry = StrokeGeometry.Default): Boolean =
        DefaultHitTest.hits(stroke, params, p, tolerance, geometry)

    /** 올가미 폴리곤 안에 획의 샘플 과반이 들어가는가 */
    fun insideLasso(stroke: Stroke, lasso: List<Pt>): Boolean = DefaultHitTest.insideLasso(stroke, lasso)
}

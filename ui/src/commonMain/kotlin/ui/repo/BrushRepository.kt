package io.github.bigrirason_debug.platen.ui.repo

import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushRole
import io.github.bigrirason_debug.platen.model.BrushSize
import io.github.bigrirason_debug.platen.model.BrushSource
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.CurvePoint
import io.github.bigrirason_debug.platen.model.ExposedParam
import io.github.bigrirason_debug.platen.model.Grain
import io.github.bigrirason_debug.platen.model.PressureMapping
import io.github.bigrirason_debug.platen.model.Smoothing
import io.github.bigrirason_debug.platen.model.StrokeCap
import io.github.bigrirason_debug.platen.model.Taper
import io.github.bigrirason_debug.platen.model.Tilt
import io.github.bigrirason_debug.platen.model.Velocity
import io.github.bigrirason_debug.platen.model.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** 임포트 결과: 변환된 스펙(실패 시 null) + 손실 메모(brush-formats §3.3 lossNotes) + 오류. */
data class BrushImportResult(val spec: BrushSpec?, val lossNotes: List<String>, val error: String? = null)

/**
 * 브러시 로스터 계약. 실 구현은 brush 모듈(임포터·zip 로더)이 연결한다 — head 가 교체.
 * UI 는 이 인터페이스와 core-model `BrushSpec` 만 본다.
 */
interface BrushRepository {
    val brushes: StateFlow<List<BrushSpec>>
    fun get(id: String): BrushSpec?
    /** 사용자 파일(.brush/.brushset/.abr) 가져오기. */
    suspend fun import(name: String, bytes: ByteArray): BrushImportResult
    /** 노트에 동봉할 브러시 zip 바이트(없으면 null → 동봉 생략). */
    fun zipBytes(id: String): ByteArray?
}

/**
 * 내장 로스터 9종(PLAN A8: 펜 2·만년필 2·연필 2·모노라인 1·형광펜 2) — 메모리 구현.
 * 수치는 brush-formats §3.2 기본값 범위 안에서 역할별로 고른 자체 값(타사 수치 아님). 단위 pt(PLAN §12-2).
 */
class InMemoryBrushRepository(initial: List<BrushSpec> = BuiltinBrushes.all) : BrushRepository {
    override val brushes = MutableStateFlow(initial)
    private val zips = HashMap<String, ByteArray>()
    override fun get(id: String): BrushSpec? = brushes.value.firstOrNull { it.id == id }

    override suspend fun import(name: String, bytes: ByteArray): BrushImportResult {
        if (bytes.isEmpty()) return BrushImportResult(null, emptyList(), "빈 파일")
        // 임포터 미연결: 이름·크기만 보존한 profile 브러시를 만들고 손실을 전부 기록한다(brush 모듈 연결 시 교체).
        val id = "imp" + bytes.size.toString(16).padStart(9, '0').take(9)
        val spec = BrushSpec(
            id = id, name = name.substringBeforeLast('.'), engine = BrushEngine.profile,
            source = BrushSource(format = name.substringAfterLast('.', "unknown"), originalName = name, importedAt = nowMillis(),
                lossNotes = listOf("임포터 미연결(brush 모듈 대기): 팁 이미지·간격·동역학 미복원", "profile 엔진 기본값으로 대체")),
            exposed = listOf(ExposedParam.size, ExposedParam.opacity, ExposedParam.pressureSize, ExposedParam.streamline),
        )
        brushes.update { list -> list.filterNot { it.id == id } + spec }
        zips[id] = bytes
        return BrushImportResult(spec, spec.source.lossNotes)
    }

    override fun zipBytes(id: String): ByteArray? = zips[id]
}

object BuiltinBrushes {
    private val linear = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
    /** 가벼운 S 커브(제어점 4): 약한 필압은 더 가늘게, 강한 필압은 빨리 굵어짐 — 스타노트 기준 "필압 반응"의 출발점(추정, 실측 후 조정). */
    private val soft = listOf(CurvePoint(0f, 0f), CurvePoint(0.3f, 0.2f), CurvePoint(0.7f, 0.8f), CurvePoint(1f, 1f))

    val pen1 = BrushSpec(
        id = "builtin-pen-1", name = "펜", role = BrushRole.pen,
        size = BrushSize(base = 2.5f, min = 0.5f, max = 12f), pressureSize = PressureMapping(0.6f, linear),
        smoothing = Smoothing(streamline = 0.35f), taper = Taper(endLength = 10f),
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.streamline, ExposedParam.taperEnd),
    )
    val pen2 = pen1.copy(id = "builtin-pen-2", name = "볼펜", size = BrushSize(base = 2f, min = 0.5f, max = 8f), pressureSize = PressureMapping(0.3f, linear), velocity = Velocity(size = -0.15f))
    val fountain1 = BrushSpec(
        id = "builtin-fountain-1", name = "만년필", role = BrushRole.fountain,
        size = BrushSize(base = 3f, min = 1f, max = 14f), pressureSize = PressureMapping(0.85f, soft),
        smoothing = Smoothing(streamline = 0.45f, stabilize = 0.2f), taper = Taper(startLength = 6f, endLength = 16f), tilt = Tilt(size = 0.3f),
        cap = StrokeCap.round,
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.stabilize, ExposedParam.streamline, ExposedParam.taperEnd),
    )
    val fountain2 = fountain1.copy(id = "builtin-fountain-2", name = "만년필 F", size = BrushSize(base = 2.2f, min = 0.8f, max = 10f), pressureSize = PressureMapping(0.7f, soft))
    val pencil1 = BrushSpec(
        id = "builtin-pencil-1", name = "연필", role = BrushRole.pencil, engine = BrushEngine.profile,
        size = BrushSize(base = 2f, min = 0.5f, max = 10f), opacity = 0.85f, pressureSize = PressureMapping(0.4f, linear),
        pressureOpacity = PressureMapping(0.6f, linear), grain = Grain(image = null, depth = 0.5f), tilt = Tilt(size = 0.5f, opacity = 0.3f),
        smoothing = Smoothing(streamline = 0.2f), taper = Taper(endLength = 4f),
        exposed = listOf(ExposedParam.size, ExposedParam.pressureSize, ExposedParam.grainDepth, ExposedParam.tiltSize),
    )
    val pencil2 = pencil1.copy(id = "builtin-pencil-2", name = "연필 4B", size = BrushSize(base = 3.5f, min = 1f, max = 14f), opacity = 0.95f, grain = Grain(image = null, depth = 0.7f))
    val monoline = BrushSpec(
        id = "builtin-monoline-1", name = "모노라인", role = BrushRole.marker,
        size = BrushSize(base = 3f, min = 0.5f, max = 20f), pressureSize = PressureMapping(0f, linear), smoothing = Smoothing(streamline = 0.5f),
        taper = Taper(endLength = 0f, auto = false), exposed = listOf(ExposedParam.size, ExposedParam.streamline),
    )
    val highlighter1 = BrushSpec(
        id = "builtin-highlighter-1", name = "형광펜", role = BrushRole.highlighter,
        size = BrushSize(base = 14f, min = 6f, max = 40f), opacity = 0.9f, pressureSize = PressureMapping(0f, linear), blend = BlendMode.multiply,
        cap = StrokeCap.flat, smoothing = Smoothing(streamline = 0.6f, stabilize = 0.3f), taper = Taper(endLength = 0f, auto = false),
        exposed = listOf(ExposedParam.size, ExposedParam.opacity, ExposedParam.stabilize),
    )
    val highlighter2 = highlighter1.copy(id = "builtin-highlighter-2", name = "형광펜 가는", size = BrushSize(base = 8f, min = 4f, max = 24f), cap = StrokeCap.round)

    val all: List<BrushSpec> = listOf(pen1, pen2, fountain1, fountain2, pencil1, pencil2, monoline, highlighter1, highlighter2)

    /** 기본 펜꽂이 5슬롯: 펜·만년필·연필·모노라인·형광펜(노랑). */
    val defaultSlotBrushIds: List<String> = listOf(pen1.id, fountain1.id, pencil1.id, monoline.id, highlighter1.id)
}

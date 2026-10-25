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
    /** 노트를 열 때 동봉 브러시를 로스터에 등록(다른 기기에서 임포트한 브러시도 그대로 렌더). */
    fun registerEmbedded(zips: Map<String, ByteArray>) {}
    /** stamp 엔진 팁/그레인 PNG(8-bit gray = 커버리지). 없으면 null. */
    fun tipPng(id: String): ByteArray? = null
    fun grainPng(id: String): ByteArray? = null
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
    override fun tipPng(id: String): ByteArray? = BuiltinBrushes.tipPng(id)
    override fun grainPng(id: String): ByteArray? = BuiltinBrushes.grainPng(id)
}

/**
 * 내장 로스터 = brush 모듈 `BuiltInBrushes`(D-018, PLAN A8 9종)에 한국어 표시 이름만 입힌 것.
 * 연필 2종은 stamp 엔진(절차적 팁+그레인 PNG)이다. 필드 이름은 UI·테스트 호환용 별칭.
 */
object BuiltinBrushes {
    private val m = io.github.bigrirason_debug.platen.brush.BuiltInBrushes
    private val koNames = mapOf(
        "pen-fine" to "펜", "pen-bold" to "굵은 펜", "fountain-soft" to "만년필", "fountain-flex" to "만년필 플렉스",
        "pencil-hb" to "연필 HB", "pencil-6b" to "연필 6B", "monoline" to "모노라인",
        "highlighter-flat" to "형광펜", "highlighter-round" to "형광펜 둥근",
    )
    private fun ko(spec: BrushSpec) = spec.copy(name = koNames[spec.id] ?: spec.name)

    val pen1: BrushSpec = ko(m.penFine)
    val pen2: BrushSpec = ko(m.penBold)
    val fountain1: BrushSpec = ko(m.fountainSoft)
    val fountain2: BrushSpec = ko(m.fountainFlex)
    val pencil1: BrushSpec = ko(m.pencilHb)
    val pencil2: BrushSpec = ko(m.pencil6b)
    val monoline: BrushSpec = ko(m.monoline)
    val highlighter1: BrushSpec = ko(m.highlighterFlat)
    val highlighter2: BrushSpec = ko(m.highlighterRound)

    val all: List<BrushSpec> = listOf(pen1, pen2, fountain1, fountain2, pencil1, pencil2, monoline, highlighter1, highlighter2)

    /** stamp 엔진 내장 브러시의 팁/그레인 PNG. */
    fun tipPng(id: String): ByteArray? = if (id.startsWith("pencil-")) m.pencilTipPng else null
    fun grainPng(id: String): ByteArray? = if (id.startsWith("pencil-")) m.pencilGrainPng else null

    /** 기본 펜꽂이 5슬롯: 펜·만년필·연필·모노라인·형광펜(노랑). */
    val defaultSlotBrushIds: List<String> = listOf(pen1.id, fountain1.id, pencil1.id, monoline.id, highlighter1.id)
}

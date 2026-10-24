package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.CurvePoint

/**
 * 메모리에 올린 브러시 = spec + 정규화된 PNG 바이트(§3.5 규격, 8-bit 그레이).
 * `spec.id` 는 사람이 읽는 안정 슬러그(내장: `pen-fine`, 임포트: `procreate-<slug>`)이고,
 * 노트가 참조하는 **패키지 id** 는 zip 바이트 SHA-1 앞 12자리(PlatenContainer.brushIdOf)로 패키징 시 결정된다
 * (brush.json 이 자기 zip 의 해시를 담을 수 없으므로 둘을 분리).
 */
class LoadedBrush(val spec: BrushSpec, val tipPng: ByteArray?, val grainPng: ByteArray?)

/** 임포트 결과: 브러시 목록 + 파일 수준 메모(브러시 개별 손실은 spec.source.lossNotes). */
class ImportResult(val brushes: List<LoadedBrush>, val notes: List<String>)

/** 스펙 보정 유틸(커브 단조화·슬러그·클램프). */
object SpecUtil {
    /** 성능 상한(brush-formats §3.7, PLAN §4.4): 간격 하한. */
    const val MIN_SPACING = 0.03f
    const val MAX_SPACING = 2f

    /**
     * 커브 제어점 정규화(§3.4): x 로 정렬, 0..1 클램프, y 비감소 강제, 끝점 (0,0)/(1,1) 고정, 최대 8점.
     * 입력이 비었거나 전부 무효면 선형 2점.
     */
    fun monotoneCurve(points: List<CurvePoint>): List<CurvePoint> {
        val inner = points.asSequence()
            .filter { it.x.isFinite() && it.y.isFinite() }
            .map { CurvePoint(it.x.coerceIn(0f, 1f), it.y.coerceIn(0f, 1f)) }
            .filter { it.x > 0f && it.x < 1f }
            .sortedBy { it.x }
            .toMutableList()
        while (inner.size > 6) inner.removeAt(inner.size / 2) // 중간점부터 솎아 8점 이내로
        val out = ArrayList<CurvePoint>(inner.size + 2)
        out += CurvePoint(0f, 0f)
        var lastY = 0f
        for (p in inner) {
            val y = maxOf(p.y, lastY).coerceAtMost(1f)
            out += CurvePoint(p.x, y); lastY = y
        }
        out += CurvePoint(1f, 1f)
        return out
    }

    fun isMonotone(points: List<CurvePoint>): Boolean {
        if (points.size < 2) return false
        if (points.first() != CurvePoint(0f, 0f) || points.last() != CurvePoint(1f, 1f)) return false
        for (i in 1 until points.size) if (points[i].x < points[i - 1].x || points[i].y < points[i - 1].y) return false
        return true
    }

    fun clampSpacing(v: Float): Float = if (v.isFinite()) v.coerceIn(MIN_SPACING, MAX_SPACING) else 0.15f

    /** 파일/브러시 이름 → id 슬러그(ascii 소문자·숫자·하이픈). 비면 fallback. */
    fun slug(name: String, fallback: String = "brush"): String {
        val sb = StringBuilder()
        var dash = false
        for (ch in name.lowercase()) {
            if (ch in 'a'..'z' || ch in '0'..'9') { sb.append(ch); dash = false }
            else if (!dash && sb.isNotEmpty()) { sb.append('-'); dash = true }
        }
        val s = sb.toString().trimEnd('-')
        return if (s.isEmpty()) fallback else s.take(48)
    }
}

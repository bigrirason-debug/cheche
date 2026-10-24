package io.github.bigrirason_debug.platen.ui.diag

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.ui.input.PointerExtra
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 입력 통계(PLAN §1.2 합격선: 유효 샘플 ≥240Hz, 필압 고유값 ≥1,024). 캔버스 포인터 루프가 기록하고 진단 화면/오버레이가 읽는다.
 * D-020 ④: 스파이크 대신 제품 안의 측정 도구.
 */
class InputStats {
    private val times = ArrayDeque<Long>()           // 최근 1초 창의 샘플 시각(ms)
    private val pressures = HashSet<Int>()          // 필압 고유값(1/65536 양자화)
    var samplesTotal: Long by mutableStateOf(0L); private set
    var historySamples: Long by mutableStateOf(0L); private set
    var hz: Float by mutableStateOf(0f); private set
    var distinctPressure: Int by mutableStateOf(0); private set
    var tiltSeen: Boolean by mutableStateOf(false); private set
    var hoverSeen: Boolean by mutableStateOf(false); private set
    var buttonSeen: Boolean by mutableStateOf(false); private set
    var cancelCount: Int by mutableStateOf(0); private set
    var lastPointerType: String by mutableStateOf("—"); private set

    fun record(extra: PointerExtra, pointerType: String) {
        lastPointerType = pointerType
        if (extra.hovering) hoverSeen = true
        if (extra.stylusButton) buttonSeen = true
        if (extra.hasTilt) tiltSeen = true
        if (extra.canceled) cancelCount++
        if (extra.hovering) return
        historySamples += extra.historyCount
        for (p in extra.points) {
            samplesTotal++
            times.addLast(p.timeMs)
            pressures += (p.pressure * 65535f).toInt()
        }
        val now = extra.points.lastOrNull()?.timeMs ?: return
        while (times.isNotEmpty() && now - times.first() > 1000) times.removeFirst()
        hz = times.size.toFloat()
        distinctPressure = pressures.size
    }

    fun reset() { times.clear(); pressures.clear(); samplesTotal = 0; historySamples = 0; hz = 0f; distinctPressure = 0; tiltSeen = false; hoverSeen = false; buttonSeen = false; cancelCount = 0 }
}

/** 프레임 간격 p50/p95(ms). withFrameNanos 루프가 기록. */
class FrameStats(private val window: Int = 240) {
    private val deltas = ArrayDeque<Float>()
    var p50: Float by mutableStateOf(0f); private set
    var p95: Float by mutableStateOf(0f); private set
    var dropped: Int by mutableStateOf(0); private set
    private var last = 0L

    fun frame(nanos: Long) {
        if (last != 0L) {
            val d = (nanos - last) / 1_000_000f
            deltas.addLast(d); if (deltas.size > window) deltas.removeFirst()
            if (d > 16.7f * 1.5f) dropped++
            val sorted = deltas.sorted()
            p50 = sorted[sorted.size / 2]
            p95 = sorted[(sorted.size * 0.95f).toInt().coerceAtMost(sorted.lastIndex)]
        }
        last = nanos
    }
    fun reset() { deltas.clear(); p50 = 0f; p95 = 0f; dropped = 0; last = 0L }
}

class DiagnosticsState {
    val input = InputStats()
    val frames = FrameStats()
    var overlayVisible: Boolean by mutableStateOf(false)
    var lastLoadMillis: Long by mutableStateOf(-1L)
    var lastMessage: String by mutableStateOf("")
}

/** 합성 획 생성기(시드 고정 → 결정적). 1만 획 로드 테스트(PLAN §12-3: 로드 ≤2s). */
object SyntheticStrokes {
    fun generate(count: Int, pageWidth: Float, pageHeight: Float, brushId: String, color: Int, seed: Long = 42L, samplesPer: Int = 24): List<Stroke> {
        val rnd = Random(seed)
        return List(count) { i ->
            val cx = rnd.nextFloat() * pageWidth
            val cy = rnd.nextFloat() * pageHeight
            val r = 6f + rnd.nextFloat() * 18f
            val phase = rnd.nextFloat() * 6.283f
            val samples = List(samplesPer) { k ->
                val a = phase + k * 0.35f
                InputSample(
                    x = (cx + cos(a) * r * (1f + k * 0.02f)).coerceIn(0f, pageWidth),
                    y = (cy + sin(a) * r * 0.6f).coerceIn(0f, pageHeight),
                    pressure = 0.3f + 0.5f * (0.5f + 0.5f * sin(a * 1.3f)),
                    t = k * 8f,
                )
            }
            Stroke(id = "syn-$seed-$i", brushId = brushId, color = color, size = 2.5f, samples = samples, createdAt = i.toLong())
        }
    }
}

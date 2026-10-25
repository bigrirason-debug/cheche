package io.github.bigrirason_debug.platen.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.bigrirason_debug.platen.model.CurvePoint
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme

/** 필압 커브 제어점 편집 순수 로직(제어점 2~8, 끝점 고정, 단조 보정) — D-018. */
object PressureCurveOps {
    const val MAX_POINTS = 8

    /** index 점을 (x,y) 로 옮긴다. 끝점은 x 고정(0/1), 중간점은 이웃 x 사이로 클램프, y 는 단조(이웃 y 사이). */
    fun moved(points: List<CurvePoint>, index: Int, x: Float, y: Float): List<CurvePoint> {
        if (index !in points.indices) return points
        val list = points.toMutableList()
        val isFirst = index == 0; val isLast = index == points.lastIndex
        val nx = when {
            isFirst -> 0f
            isLast -> 1f
            else -> x.coerceIn(list[index - 1].x + 0.01f, list[index + 1].x - 0.01f)
        }
        val minY = if (isFirst) 0f else list[index - 1].y
        val maxY = if (isLast) 1f else list[index + 1].y
        val ny = if (isFirst) 0f else if (isLast) 1f else y.coerceIn(minY, maxY)
        list[index] = CurvePoint(nx, ny)
        return list
    }

    /** x 위치에 점 추가(최대 8). */
    fun added(points: List<CurvePoint>, x: Float): List<CurvePoint> {
        if (points.size >= MAX_POINTS) return points
        val cx = x.coerceIn(0.02f, 0.98f)
        val y = evaluate(points, cx)
        return (points + CurvePoint(cx, y)).sortedBy { it.x }
    }

    fun removed(points: List<CurvePoint>, index: Int): List<CurvePoint> =
        if (index == 0 || index == points.lastIndex || points.size <= 2) points else points.filterIndexed { i, _ -> i != index }

    /** 선형 보간 평가. */
    fun evaluate(points: List<CurvePoint>, x: Float): Float {
        if (points.isEmpty()) return x
        if (x <= points.first().x) return points.first().y
        for (i in 1 until points.size) {
            val a = points[i - 1]; val b = points[i]
            if (x <= b.x) { val t = if (b.x > a.x) (x - a.x) / (b.x - a.x) else 1f; return a.y + (b.y - a.y) * t }
        }
        return points.last().y
    }

    fun isMonotone(points: List<CurvePoint>): Boolean = points.zipWithNext().all { (a, b) -> b.x >= a.x && b.y >= a.y }
}

/** 필압 커브 미니 에디터: 제어점 드래그, 빈 곳 탭 = 점 추가(최대 8), 점 더블탭 = 삭제(끝점 제외). */
@Composable
fun PressureCurveEditor(points: List<CurvePoint>, onChange: (List<CurvePoint>) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = PlatenTheme.colors
    var dragging by remember { mutableIntStateOf(-1) }
    Canvas(
        modifier.width(160.dp).height(120.dp).background(c.paper)
            .semantics { contentDescription = "필압 커브 편집기, 제어점 ${points.size}개" }
            .pointerInput(points, enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos ->
                        val w = size.width.toFloat(); val h = size.height.toFloat()
                        val near = points.indices.minByOrNull { i -> val p = points[i]; (Offset(p.x * w, (1 - p.y) * h) - pos).getDistance() }
                        val d = near?.let { i -> (Offset(points[i].x * w, (1 - points[i].y) * h) - pos).getDistance() } ?: Float.MAX_VALUE
                        dragging = if (d < 24.dp.toPx()) near!! else {
                            val np = PressureCurveOps.added(points, pos.x / w)
                            onChange(np); np.indexOfFirst { kotlin.math.abs(it.x - pos.x / w) < 0.011f }
                        }
                    },
                    onDragEnd = { dragging = -1 }, onDragCancel = { dragging = -1 },
                ) { change, _ ->
                    if (dragging >= 0) {
                        onChange(PressureCurveOps.moved(points, dragging, change.position.x / size.width, 1f - change.position.y / size.height))
                        change.consume()
                    }
                }
            },
    ) {
        val w = size.width; val h = size.height
        for (i in 1..3) {
            drawLine(c.rule, Offset(w * i / 4f, 0f), Offset(w * i / 4f, h), 1f)
            drawLine(c.rule, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1f)
        }
        drawLine(c.muted, Offset(0f, h), Offset(w, 0f), 1f)
        val path = Path()
        var x = 0f
        while (x <= 1f) { val y = PressureCurveOps.evaluate(points, x); if (x == 0f) path.moveTo(0f, (1 - y) * h) else path.lineTo(x * w, (1 - y) * h); x += 0.02f }
        drawPath(path, c.ink, style = Stroke(2f))
        points.forEachIndexed { i, p ->
            drawCircle(if (i == dragging) c.red else c.ink, radius = 5.dp.toPx(), center = Offset(p.x * w, (1 - p.y) * h))
            drawCircle(c.paper, radius = 2.5.dp.toPx(), center = Offset(p.x * w, (1 - p.y) * h))
        }
    }
}

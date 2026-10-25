package io.github.bigrirason_debug.platen.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.SurfaceView
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.motionEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer

/** 프론트버퍼에 그릴 획 조각 1개(화면 px). */
private class WetSegment(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val width: Float, val argb: Int)

/**
 * graphics-core 1.0.x `CanvasFrontBufferedRenderer` 로 젖은 잉크를 그린다(API 29+, Tab S9+ 충족).
 * SurfaceView 는 Compose 입력을 가로채지 않도록 캔버스 **아래** 형제로 두고 `setZOrderOnTop(true)` 로 화면 위에 합성한다.
 * 멀티버퍼 레이어는 쓰지 않는다(마른 잉크는 Compose 렌더러 몫) — 실기기 검증 전이라 설정의 "프론트버퍼" 토글로 끌 수 있다.
 */
private class FrontBufferWetInk(private val surface: SurfaceView, private val host: View) : WetInkOverlay,
    CanvasFrontBufferedRenderer.Callback<WetSegment> {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val renderer = CanvasFrontBufferedRenderer(surface, this)

    override fun onDrawFrontBufferedLayer(canvas: Canvas, bufferWidth: Int, bufferHeight: Int, param: WetSegment) {
        paint.color = param.argb
        paint.strokeWidth = param.width
        canvas.drawLine(param.x0, param.y0, param.x1, param.y1, paint)
    }

    override fun onDrawMultiBufferedLayer(canvas: Canvas, bufferWidth: Int, bufferHeight: Int, params: Collection<WetSegment>) {
        // 마른 잉크는 Compose 가 그린다 — 멀티버퍼 레이어는 비워 둔다.
    }

    override fun penDown(event: PointerEvent) {
        // View.requestUnbufferedDispatch(MotionEvent) 는 API 30+(Tab S9+ 는 Android 14+)
        if (android.os.Build.VERSION.SDK_INT >= 30) event.motionEvent?.let { host.requestUnbufferedDispatch(it) }
    }

    override fun segment(x0: Float, y0: Float, x1: Float, y1: Float, widthPx: Float, argb: Int) {
        renderer.renderFrontBufferedLayer(WetSegment(x0, y0, x1, y1, widthPx, argb))
    }

    override fun clearAfterDry() {
        // 펜 업 직후 Compose 가 마른 잉크를 그릴 시간을 2프레임 준다(깜빡임 방지).
        host.postOnAnimation { host.postOnAnimation { renderer.clear() } }
    }

    fun release() = renderer.release(true)
}

@Composable
actual fun WetInkLayer(enabled: Boolean, modifier: Modifier, onOverlay: (WetInkOverlay?) -> Unit) {
    if (!enabled) {
        DisposableEffect(Unit) { onOverlay(null); onDispose { } }
        return
    }
    val host = LocalView.current
    val holder = remember { arrayOfNulls<FrontBufferWetInk>(1) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceView(ctx).apply {
                setZOrderOnTop(true)
                this.holder.setFormat(PixelFormat.TRANSLUCENT)
                isClickable = false
                isFocusable = false
            }.also { sv ->
                val wet = FrontBufferWetInk(sv, host)
                holder[0] = wet
                onOverlay(wet)
            }
        },
    )
    DisposableEffect(Unit) {
        onDispose {
            onOverlay(null)
            holder[0]?.release()
            holder[0] = null
        }
    }
}

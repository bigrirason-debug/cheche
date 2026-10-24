package io.github.bigrirason_debug.platen.brush

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** BitmapFactory 디코드(PNG/JPEG). 크기 상한은 [ImageCodec] 이 헤더에서 먼저 검사한다. (CI 전용 컴파일) */
actual fun platformDecodeArgb(bytes: ByteArray): ArgbImage {
    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inScaled = false }
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: throw BrushFormatException("이미지 디코드 실패(지원하지 않는 포맷)")
    val w = bmp.width; val h = bmp.height
    val argb = IntArray(w * h)
    bmp.getPixels(argb, 0, w, 0, 0, w, h)
    val hasAlpha = bmp.hasAlpha()
    bmp.recycle()
    return ArgbImage(w, h, argb, hasAlpha)
}

package io.github.bigrirason_debug.platen.brush

import javax.imageio.ImageIO

/**
 * javax.imageio 디코드(PNG/JPEG). 크기 상한은 [ImageCodec] 이 헤더에서 먼저 검사한다.
 * 그레이(1 색 성분) 이미지는 `getRGB` 가 선형 gray → sRGB 감마 변환을 끼워 넣으므로 래스터 샘플을 직접 읽는다.
 */
actual fun platformDecodeArgb(bytes: ByteArray): ArgbImage {
    val img = ImageIO.read(bytes.inputStream()) ?: throw BrushFormatException("이미지 디코드 실패(지원하지 않는 포맷)")
    val w = img.width; val h = img.height
    val cm = img.colorModel
    if (cm.numColorComponents == 1 && img.raster.numBands <= 2) {
        val raster = img.raster
        val bits = cm.componentSize[0]
        val gray = raster.getSamples(0, 0, w, h, 0, null as IntArray?)
        val alpha = if (cm.hasAlpha() && raster.numBands >= 2) raster.getSamples(0, 0, w, h, 1, null as IntArray?) else null
        val maxIn = (1 shl bits) - 1
        val argb = IntArray(w * h) { i ->
            val g = if (bits == 8) gray[i] else if (bits > 8) gray[i] ushr (bits - 8) else gray[i] * 255 / maxIn
            val a = if (alpha == null) 255 else if (bits == 8) alpha[i] else if (bits > 8) alpha[i] ushr (bits - 8) else alpha[i] * 255 / maxIn
            (a shl 24) or (g shl 16) or (g shl 8) or g
        }
        return ArgbImage(w, h, argb, alpha != null)
    }
    val argb = img.getRGB(0, 0, w, h, null, 0, w)
    return ArgbImage(w, h, argb, cm.hasAlpha())
}

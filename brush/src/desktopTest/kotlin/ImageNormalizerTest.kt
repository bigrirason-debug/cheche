package io.github.bigrirason_debug.platen.brush

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImageNormalizerTest {
    @Test
    fun trimPadsOnePixel() {
        // 100×60 캔버스, (10..29, 20..34) 사각형 → 트림 20×15 + 패딩 → 22×17
        val img = GrayImage.filled(100, 60, 0)
        for (y in 20..34) for (x in 10..29) img.alpha[y * 100 + x] = 255.toByte()
        val t = ImageNormalizer.trim(img)
        assertEquals(22, t.width); assertEquals(17, t.height)
        assertEquals(0, t.get(0, 0)); assertEquals(255, t.get(1, 1)); assertEquals(0, t.get(21, 16))
    }

    @Test
    fun normalizeTipIsSquareAndLimited() {
        val img = GrayImage.filled(900, 300, 0)
        for (y in 100 until 200) for (x in 0 until 900) img.alpha[y * 900 + x] = 200.toByte()
        val pen = ImageNormalizer.normalizeTip(img, ImageNormalizer.tipLimitFor(10f))
        assertEquals(256, pen.width); assertEquals(256, pen.height)
        val tex = ImageNormalizer.normalizeTip(img, ImageNormalizer.tipLimitFor(100f))
        assertEquals(512, tex.width); assertEquals(512, tex.height)
        // 작은 이미지는 확대하지 않는다
        val small = ImageNormalizer.normalizeTip(TestFixtures.circle(40), 256)
        assertTrue(small.width <= 40 && small.width == small.height)
    }

    @Test
    fun grainIsCenterCroppedSquare() {
        val img = GrayImage.filled(1200, 700, 0)
        for (y in 0 until 700) for (x in 0 until 1200) img.alpha[y * 1200 + x] = (x % 256).toByte()
        val g = ImageNormalizer.normalizeGrain(img)
        assertEquals(512, g.width); assertEquals(512, g.height)
    }

    @Test
    fun boxDownsampleAveragesExactly() {
        // 4×4 체커보드(0/255) → 2×2 는 전부 128 근처
        val img = GrayImage(4, 4, ByteArray(16) { if ((it % 4 + it / 4) % 2 == 0) 255.toByte() else 0 })
        val r = ImageNormalizer.resizeBox(img, 2, 2)
        for (v in r.alpha) assertTrue((v.toInt() and 0xFF) in 127..128)
        // 비정수 비율(5→2)도 합이 보존되는 범위
        val row = GrayImage(5, 1, byteArrayOf(0, 0, 0, 255.toByte(), 255.toByte()))
        val r2 = ImageNormalizer.resizeBox(row, 2, 1)
        // 왼쪽 = src 0..2.5 → 0, 오른쪽 = src 2.5..5 → (0×0.5 + 255 + 255)/2.5 = 204
        assertEquals(0, r2.get(0, 0)); assertEquals(204, r2.get(1, 0))
    }

    @Test
    fun invertAndBrightnessContrast() {
        val img = GrayImage.filled(2, 2, 200)
        assertEquals(55, ImageNormalizer.invert(img).get(0, 0))
        assertEquals(255, ImageNormalizer.brightnessContrast(img, 0.5f, 0f).get(0, 0))
        val c = ImageNormalizer.brightnessContrast(GrayImage.filled(1, 1, 128), 0f, 1f).get(0, 0)
        assertTrue(c in 127..129, "contrast keeps midpoint: $c")
    }

    @Test
    fun roundHardTipDetection() {
        assertTrue(ImageNormalizer.isRoundHardTip(TestFixtures.circle(64)))
        assertTrue(ImageNormalizer.isRoundHardTip(ProceduralImages.roundTip(64, 0.9f)))
        assertTrue(!ImageNormalizer.isRoundHardTip(ProceduralImages.roundTip(64, 0.9f, roundness = 0.4f)))
        assertTrue(!ImageNormalizer.isRoundHardTip(ImageNormalizer.invert(TestFixtures.circle(64))))
    }

    @Test
    fun rejectsOversizeHeaderAndAcceptsRgba() {
        val big = BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)
        val png = ByteArrayOutputStream().also { ImageIO.write(big, "png", it) }.toByteArray()
        // IHDR 폭을 4097 로 패치 → 디코드 전에 거부
        val patched = png.copyOf()
        patched[16] = 0; patched[17] = 0; patched[18] = 0x10; patched[19] = 0x01
        assertFailsWith<ImportLimitExceeded> { ImageCodec.decodeToGray(patched) }

        // RGBA: 단색 흰 원 + 알파 → 알파 채택(§3.5 1항)
        val rgba = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until 50) for (x in 0 until 50) {
            val d = Math.hypot(x - 24.5, y - 24.5)
            rgba.setRGB(x, y, if (d < 18) 0xFFFFFFFF.toInt() else 0x00FFFFFF)
        }
        val bytes = ByteArrayOutputStream().also { ImageIO.write(rgba, "png", it) }.toByteArray()
        val g = ImageCodec.decodeToGray(bytes)
        assertEquals(255, g.get(25, 25)); assertEquals(0, g.get(0, 0))
        assertEquals(50 to 50, ImageCodec.probeSize(bytes))

        // RGB(알파 없음): 휘도 채택
        val rgb = BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB)
        rgb.setRGB(0, 0, 0xFFFFFF); rgb.setRGB(1, 0, 0x000000)
        val g2 = ImageCodec.decodeToGray(ByteArrayOutputStream().also { ImageIO.write(rgb, "png", it) }.toByteArray())
        assertEquals(255, g2.get(0, 0)); assertEquals(0, g2.get(1, 0))
    }
}

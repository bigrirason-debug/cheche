package io.github.bigrirason_debug.platen.ui.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual object PlatformImages {
    actual fun encodePng(bitmap: ImageBitmap): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(bitmap.toAwtImage(), "png", it) }.toByteArray()

    actual fun fromArgb(width: Int, height: Int, argb: IntArray): ImageBitmap =
        java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            .also { it.setRGB(0, 0, width, height, argb, 0, width) }.toComposeImageBitmap()

    actual fun decode(bytes: ByteArray): ImageBitmap? =
        runCatching { ImageIO.read(bytes.inputStream())?.toComposeImageBitmap() }.getOrNull()

    actual val platformName: String = "desktop/" + System.getProperty("os.name")
}

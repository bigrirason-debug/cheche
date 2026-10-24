package io.github.bigrirason_debug.platen.ui.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual object PlatformImages {
    actual fun encodePng(bitmap: ImageBitmap): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(bitmap.toAwtImage(), "png", it) }.toByteArray()

    actual fun decode(bytes: ByteArray): ImageBitmap? =
        runCatching { ImageIO.read(bytes.inputStream())?.toComposeImageBitmap() }.getOrNull()

    actual val platformName: String = "desktop/" + System.getProperty("os.name")
}

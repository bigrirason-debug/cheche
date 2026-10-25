package io.github.bigrirason_debug.platen.ui.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

actual object PlatformImages {
    actual fun encodePng(bitmap: ImageBitmap): ByteArray =
        ByteArrayOutputStream().also { bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    actual fun decode(bytes: ByteArray): ImageBitmap? =
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()

    actual val platformName: String = "android/" + android.os.Build.VERSION.SDK_INT + " " + android.os.Build.MODEL
}

package io.github.bigrirason_debug.platen.ui.platform

import androidx.compose.ui.graphics.ImageBitmap

/** 이미지 코덱은 expect/actual (PLAN §12-8: jvmCommon 에서 java.awt/javax.imageio 금지). */
expect object PlatformImages {
    /** ImageBitmap → PNG 바이트(진단 PNG 덤프·썸네일). */
    fun encodePng(bitmap: ImageBitmap): ByteArray
    /** PNG/JPEG 바이트 → ImageBitmap. 실패 시 null. */
    fun decode(bytes: ByteArray): ImageBitmap?
    /** 플랫폼 이름(설정 > 정보, 밀도 기본값). */
    val platformName: String
}

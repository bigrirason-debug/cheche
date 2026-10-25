package io.github.bigrirason_debug.platen.ui.repo

import androidx.compose.ui.graphics.ImageBitmap
import io.github.bigrirason_debug.platen.brush.ImageCodec
import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.render.BrushImageStore
import io.github.bigrirason_debug.platen.ui.platform.PlatformImages

/**
 * 브러시 팁/그레인 PNG(8-bit 커버리지) → 흰색 + 알파 ImageBitmap. 렌더러가 SrcIn 으로 획 색을 입힌다.
 * 디코드 결과는 브러시 id 별로 캐시한다.
 */
class RepoBrushImageStore(private val repo: BrushRepository) : BrushImageStore {
    private val tips = HashMap<String, ImageBitmap?>()
    private val grains = HashMap<String, ImageBitmap?>()

    override fun tip(spec: BrushSpec): ImageBitmap? =
        if (spec.engine != BrushEngine.stamp) null else tips.getOrPut(spec.id) { repo.tipPng(spec.id)?.let(::alphaBitmap) }

    override fun grain(spec: BrushSpec): ImageBitmap? =
        if (spec.grain.depth <= 0f) null else grains.getOrPut(spec.id) { repo.grainPng(spec.id)?.let(::alphaBitmap) }

    private fun alphaBitmap(png: ByteArray): ImageBitmap? = runCatching {
        val g = ImageCodec.decodeToGray(png)
        val argb = IntArray(g.width * g.height) { i -> ((g.alpha[i].toInt() and 0xFF) shl 24) or 0x00FFFFFF }
        PlatformImages.fromArgb(g.width, g.height, argb)
    }.getOrNull()
}

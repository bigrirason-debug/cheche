package io.github.bigrirason_debug.platen.ui.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.github.bigrirason_debug.platen.pdf.PdfDocumentSource
import io.github.bigrirason_debug.platen.pdf.openPdf
import io.github.bigrirason_debug.platen.ui.platform.PlatformImages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * PDF 배경 페이지 비트맵 캐시(속지·플래너). 원본 PDF 는 노트 에셋으로 보존되고, 화면용으로만 래스터화한다.
 * 렌더는 백그라운드 한 줄(PDF 라이브러리는 스레드 안전하지 않음), 결과가 생기면 [version] 이 올라 캔버스가 다시 그린다.
 * 해상도 2 px/pt(= 100% 줌에서 고밀도 화면 대응), 최근 [maxPages] 장만 보관.
 */
class PdfBackgrounds(private val assets: (String) -> ByteArray?, private val scope: CoroutineScope, private val maxPages: Int = 8) {
    var version by mutableIntStateOf(0); private set
    private val lock = Mutex()
    private val docs = HashMap<String, PdfDocumentSource>()
    private val bitmaps = LinkedHashMap<String, ImageBitmap>(16, 0.75f, true)
    private val pending = HashSet<String>()
    private val failed = HashSet<String>()

    fun bitmap(assetId: String, pageIndex: Int): ImageBitmap? {
        val key = "$assetId#$pageIndex"
        bitmaps[key]?.let { return it }
        if (key in failed || !pending.add(key)) return null
        scope.launch(Dispatchers.Default) {
            val img = runCatching {
                lock.withLock {
                    val doc = docs.getOrPut(assetId) { openPdf(assets(assetId) ?: error("PDF 에셋 없음: $assetId")) }
                    val r = doc.render(pageIndex, RENDER_PX_PER_PT)
                    PlatformImages.fromArgb(r.width, r.height, r.argb)
                }
            }.getOrNull()
            launch(Dispatchers.Main) {
                pending.remove(key)
                if (img == null) failed += key else {
                    bitmaps[key] = img
                    while (bitmaps.size > maxPages) bitmaps.remove(bitmaps.keys.first())
                }
                version++
            }
        }
        return null
    }

    fun close() { docs.values.forEach { runCatching { it.close() } }; docs.clear(); bitmaps.clear() }

    companion object { const val RENDER_PX_PER_PT = 2f }
}

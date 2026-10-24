package io.github.bigrirason_debug.platen.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.Rect
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 마른 잉크 타일 캐시. 페이지를 [tileSize]px 타일(기본 256)로 쪼개 오프스크린 [ImageBitmap] 에 그려 두고,
 * 이후 프레임은 타일을 drawImage 로 붙인다. 키 = (페이지 id, 테마, 줌 레벨, 타일 좌표).
 *
 * - 줌 레벨: [zoom] 을 반옥타브(√2 단위)로 양자화 — 핀치 중 매 프레임 재렌더를 막고 레벨당 타일 수를 제한.
 * - 무효화: [invalidate] (dirty 영역과 교차하는 타일만), [invalidateAll].
 * - 메모리: LRU, [maxTiles] 초과 시 가장 오래 쓰인 타일 제거(256² ARGB ≈ 256KB → 기본 128장 ≈ 32MB).
 *
 * UI 는 매 프레임 현재 화면 배율을 [zoom] 에 넣어 준다(DrawScope 는 현재 변환 행렬을 노출하지 않으므로).
 */
class PageCache(val tileSize: Int = 256, val maxTiles: Int = 128) {
    /** 현재 문서 pt → 화면 px 배율. 호출자가 drawPage 전에 갱신한다. */
    var zoom: Float = 1f

    private data class Key(val pageId: String, val dark: Boolean, val level: Float, val tx: Int, val ty: Int)
    private class Tile(val bitmap: ImageBitmap, val docRect: Rect)

    private val tiles = LinkedHashMap<Key, Tile>()

    /** 캐시된 타일 수(테스트·디버그) */
    val tileCount: Int get() = tiles.size

    /** 줌을 반옥타브 단위로 양자화한 렌더 배율(0.25 ~ 8). */
    fun levelFor(zoom: Float): Float {
        val z = zoom.coerceIn(0.25f, 8f)
        val halfOctaves = (ln(z.toDouble()) / ln(2.0) * 2.0).roundToInt()
        return 2.0.pow(halfOctaves / 2.0).toFloat()
    }

    /** dirty(문서 좌표)와 교차하는 해당 페이지의 타일 제거. dirty = null 이면 페이지 전체. */
    fun invalidate(pageId: String, dirty: Rect? = null) {
        val it = tiles.entries.iterator()
        while (it.hasNext()) {
            val (k, t) = it.next()
            if (k.pageId != pageId) continue
            if (dirty == null || t.docRect.intersects(dirty)) it.remove()
        }
    }

    fun invalidateAll() = tiles.clear()

    /**
     * visible 과 교차하는 타일을 (없으면 [render] 로 만들어) 그린다.
     * render 는 "타일의 문서 영역"을 받아 그 영역의 페이지 전체(종이·속지·객체·마른 잉크)를 문서 좌표로 그려야 한다.
     */
    fun drawTiles(scope: DrawScope, page: Page, visible: Rect, theme: InkTheme, render: DrawScope.(Rect) -> Unit) {
        val level = levelFor(zoom)
        val tilePt = tileSize / level
        val pageW = page.size.width
        val pageH = page.size.height
        val left = maxOf(visible.left, 0f)
        val top = maxOf(visible.top, 0f)
        val right = minOf(visible.right, pageW)
        val bottom = minOf(visible.bottom, pageH)
        if (right <= left || bottom <= top) return
        val tx0 = floor(left / tilePt).toInt()
        val ty0 = floor(top / tilePt).toInt()
        val tx1 = ceil(right / tilePt).toInt() - 1
        val ty1 = ceil(bottom / tilePt).toInt() - 1

        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            val key = Key(page.id, theme.dark, level, tx, ty)
            val tile = tiles.remove(key) ?: renderTile(scope, key, tilePt, level, render)
            tiles[key] = tile // 재삽입 = LRU 갱신
            val r = tile.docRect
            scope.withTransform({
                translate(r.left, r.top)
                scale(1f / level, 1f / level, pivot = Offset.Zero)
            }) {
                drawImage(tile.bitmap)
            }
        }
        evict()
    }

    private fun renderTile(scope: DrawScope, key: Key, tilePt: Float, level: Float, render: DrawScope.(Rect) -> Unit): Tile {
        val docRect = Rect(key.tx * tilePt, key.ty * tilePt, (key.tx + 1) * tilePt, (key.ty + 1) * tilePt)
        val bitmap = ImageBitmap(tileSize, tileSize)
        val canvas = Canvas(bitmap)
        CanvasDrawScope().draw(scope, scope.layoutDirection, canvas, ComposeSize(tileSize.toFloat(), tileSize.toFloat())) {
            withTransform({
                scale(level, level, pivot = Offset.Zero)
                translate(-docRect.left, -docRect.top)
            }) {
                render(docRect)
            }
        }
        return Tile(bitmap, docRect)
    }

    private fun evict() {
        if (tiles.size <= maxTiles) return
        val it = tiles.keys.iterator()
        while (tiles.size > maxTiles && it.hasNext()) {
            it.next(); it.remove()
        }
    }
}

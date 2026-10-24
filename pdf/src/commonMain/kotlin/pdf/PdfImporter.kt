package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject

/** PDF → 노트 페이지 변환. 원본 PDF 는 에셋(assetId)으로 그대로 보존되고 페이지는 배경으로만 참조한다. */
object PdfImporter {
    /**
     * 각 PDF 페이지 → `Page(size = 시각 pt, background = PdfPage(assetId, i), objects = 링크 → PageObject.Link)`.
     * 링크 target: 내부 이동은 "index:<n>"(가져온 페이지 묶음 안의 인덱스), URI 는 URL 문자열.
     */
    fun toPages(bytes: ByteArray, assetId: String): List<Page> = openPdf(bytes).use { src ->
        (0 until src.pageCount).map { i ->
            val links = src.links(i).mapIndexed { k, link ->
                val target = when (val t = link.target) {
                    is LinkTarget.PageIndex -> "index:${t.index}"
                    is LinkTarget.Url -> t.url
                }
                PageObject.Link(id = "$assetId-p$i-l$k", rect = link.rect, target = target)
            }
            Page(
                id = "$assetId-p$i",
                size = src.pageSize(i),
                background = Background.PdfPage(assetId, i),
                objects = links,
            )
        }
    }
}

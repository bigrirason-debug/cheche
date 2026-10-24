package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.PageObject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PdfImporterTest {
    private val bytes = PdfFixtures.threePagePdf()

    @Test
    fun importsPagesLinksAndSizes() {
        val pages = PdfImporter.toPages(bytes, "asset1")
        assertEquals(3, pages.size)
        pages.forEachIndexed { i, p ->
            assertEquals(PdfFixtures.W, p.size.width, 0.01f)
            assertEquals(PdfFixtures.H, p.size.height, 0.01f)
            assertEquals(Background.PdfPage("asset1", i), p.background)
        }
        assertTrue(pages[0].objects.isEmpty())
        assertTrue(pages[2].objects.isEmpty())
        val links = pages[1].objects.filterIsInstance<PageObject.Link>()
        assertEquals(2, links.size)
        val goTo = assertNotNull(links.find { it.target == "index:2" })
        // 사용자 공간 (100,500)-(200,550) → 좌상단 원점: top = 600-550 = 50, bottom = 100
        assertTrue(abs(goTo.rect.left - 100f) < 0.01f && abs(goTo.rect.top - 50f) < 0.01f)
        assertTrue(abs(goTo.rect.right - 200f) < 0.01f && abs(goTo.rect.bottom - 100f) < 0.01f)
        assertNotNull(links.find { it.target == PdfFixtures.URL })
        assertEquals(pages.map { it.id }.toSet().size, 3, "page ids unique")
    }

    @Test
    fun renderSizeIsCeilOfPtTimesScale() {
        openPdf(bytes).use { src ->
            assertEquals(3, src.pageCount)
            val a = src.render(0, 1.5f)
            assertEquals(600, a.width); assertEquals(900, a.height)
            assertEquals(a.width * a.height, a.argb.size)
            assertEquals(0xFFFFFFFF.toInt(), a.argb[0], "paper is opaque white")
            val s = 0.37f
            val b = src.render(1, s)
            assertEquals(ceil(PdfFixtures.W * s).toInt(), b.width)
            assertEquals(ceil(PdfFixtures.H * s).toInt(), b.height)
            // 텍스트가 있는 페이지는 흰색이 아닌 픽셀이 존재
            assertTrue(a.argb.any { it != 0xFFFFFFFF.toInt() }, "rendered content present")
        }
    }
}

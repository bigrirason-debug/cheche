package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.PaperSizes
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.text.PDFTextStripper
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfExporterTest {
    private val pdf = PdfFixtures.threePagePdf()
    private val png = PdfFixtures.pngAsset()
    private val assets: (String) -> ByteArray? = { id -> when (id) { "planner" -> pdf; "sticker" -> png; else -> null } }

    private fun compositeNote(): Note {
        val size = Size(PdfFixtures.W, PdfFixtures.H)
        val p0 = Page("p0", size, Background.PdfPage("planner", 0))
        val p1 = Page(
            "p1", size, Background.PdfPage("planner", 1),
            strokes = listOf(
                PdfFixtures.stroke("s1", "pen", 0xFF000000.toInt(), 4f, 50f to 200f, 150f to 220f, 250f to 210f),
                PdfFixtures.stroke("s2", "pencil", 0xFF336699.toInt(), 6f, 60f to 300f, 160f to 320f, 260f to 310f),
                PdfFixtures.stroke("s3", "hl", 0xFFFFEB3B.toInt(), 12f, 40f to 560f, 200f to 560f, opacity = 0.5f),
            ),
            objects = listOf(
                PageObject.TextBox("t1", Rect(40f, 40f, 300f, 100f), "hello world from Platen"),
                PageObject.Table("tb1", Rect(40f, 380f, 240f, 440f), 2, 2, listOf(100f, 100f), listOf(30f, 30f), cells = mapOf("0,0" to "alpha", "1,1" to "beta")),
                PageObject.Image("im1", Rect(300f, 300f, 360f, 360f), "sticker", rotationDeg = 15f),
                PageObject.Link("l1", Rect(40f, 120f, 140f, 150f), "index:0"),
                PageObject.Link("l2", Rect(40f, 160f, 140f, 190f), "https://platen.example/"),
                PageObject.Link("l3", Rect(40f, 200f, 140f, 230f), "page:p2"),
            ),
        )
        val p2 = Page("p2", size, Background.PdfPage("planner", 2))
        return Note("note-1", "Composite", created = 1_700_000_000_000L, modified = 1_700_000_100_000L, pages = listOf(p0, p1, p2))
    }

    @Test
    fun exportsOverOriginalPdfAndIsDeterministic() {
        val note = compositeNote()
        val a = PdfExporter.export(note, assets, PdfFixtures.fakeGeometry, PdfFixtures.resolve)
        val b = PdfExporter.export(note, assets, PdfFixtures.fakeGeometry, PdfFixtures.resolve)
        assertContentEquals(a.bytes, b.bytes, "same input must give identical bytes")
        assertTrue(a.warnings.isEmpty(), "unexpected warnings: ${a.warnings}")

        Loader.loadPDF(a.bytes).use { doc ->
            assertEquals(3, doc.numberOfPages)
            assertEquals("Platen", doc.documentInformation.producer)
            for (i in 0 until 3) {
                val page = doc.getPage(i)
                assertTrue(page.hasContents(), "page $i has content stream")
                assertTrue(PdfFixtures.operatorNames(page).isNotEmpty(), "page $i content non-empty")
            }
            val page = doc.getPage(1)
            val links = page.annotations.filterIsInstance<PDAnnotationLink>()
            assertEquals(5, links.size, "2 original + 3 new links")
            val uris = links.mapNotNull { (it.action as? PDActionURI)?.uri }
            assertTrue(PdfFixtures.URL in uris, "original URI preserved")
            assertTrue("https://platen.example/" in uris, "new URI link")
            val goToPages = links.mapNotNull { ((it.action as? PDActionGoTo)?.destination as? PDPageDestination)?.retrievePageNumber() }.sorted()
            assertEquals(listOf(0, 2, 2), goToPages, "original GoTo remapped to page 3 (index 2), new links → 0 and p2")

            val text = PDFTextStripper().getText(doc)
            assertTrue("hello" in text, "text box exported: $text")
            assertTrue("Fixture page 2" in text, "original page content kept")
            assertTrue("alpha" in text && "beta" in text, "table cell text")

            val ops = PdfFixtures.operatorNames(page)
            assertTrue("f" in ops || "f*" in ops, "profile stroke fill operator present")
            assertTrue("Do" in ops, "image XObject drawn (stamp raster + png)")
            val xobjects = page.resources.xObjectNames.count()
            assertTrue(xobjects >= 2, "stamp raster + sticker image, got $xobjects")
            val multiply = page.resources.extGStateNames.any { page.resources.getExtGState(it).blendMode == BlendMode.MULTIPLY }
            assertTrue(multiply, "highlighter uses BM/Multiply")
        }
    }

    @Test
    fun templateLinesCountMatchesSpacing() {
        val size = PaperSizes.DEFAULT
        val tpl = Background.Template("lines", spacing = 24f)
        val note = Note("n2", "Lines", 0L, 0L, listOf(Page("p", size, tpl)))
        val r = PdfExporter.export(note, { null }, PdfFixtures.fakeGeometry, PdfFixtures.resolve)
        Loader.loadPDF(r.bytes).use { doc ->
            val count = PdfFixtures.horizontalStrokeCount(doc.getPage(0))
            val expected = (size.height - tpl.marginTop) / tpl.spacing
            assertTrue(abs(count - expected) <= 1.5f, "lines=$count expected≈$expected")
            assertEquals(PdfExporter.ruleCount(size.height, tpl.marginTop, tpl.spacing), count)
        }
    }

    @Test
    fun koreanTextIsOmittedWithWarning() {
        val page = Page("p", PaperSizes.A4, Background.Solid(0xFFFFFFFF.toInt()), objects = listOf(PageObject.TextBox("t", Rect(50f, 50f, 400f, 120f), "안녕하세요 hello")))
        val r = PdfExporter.export(Note("n3", "KR", 0L, 0L, listOf(page)), { null }, PdfFixtures.fakeGeometry, PdfFixtures.resolve)
        assertTrue(r.warnings.any { "omitted" in it }, "warning expected: ${r.warnings}")
        Loader.loadPDF(r.bytes).use { doc -> assertTrue("hello" in PDFTextStripper().getText(doc)) }
    }

    @Test
    fun internalLinkToUnexportedPageIsRemovedWithWarning() {
        // 플래너 2페이지만 내보내면 원본 GoTo(→3페이지)는 제거되고 URI 는 남는다
        val page = Page("only", Size(PdfFixtures.W, PdfFixtures.H), Background.PdfPage("planner", 1))
        val r = PdfExporter.export(Note("n5", "Partial", 0L, 0L, listOf(page)), assets, PdfFixtures.fakeGeometry, PdfFixtures.resolve)
        assertTrue(r.warnings.any { "removed" in it }, "warnings=${r.warnings}")
        Loader.loadPDF(r.bytes).use { doc ->
            val links = doc.getPage(0).annotations.filterIsInstance<PDAnnotationLink>()
            assertEquals(1, links.size)
            assertEquals(PdfFixtures.URL, (links[0].action as PDActionURI).uri)
        }
    }

    @Test
    fun missingAssetFallsBackToBlankPageWithWarning() {
        val page = Page("p", Size(300f, 300f), Background.PdfPage("nope", 0))
        val r = PdfExporter.export(Note("n4", "Missing", 0L, 0L, listOf(page)), { null }, PdfFixtures.fakeGeometry, PdfFixtures.resolve)
        assertTrue(r.warnings.any { "missing" in it })
        Loader.loadPDF(r.bytes).use { doc ->
            assertEquals(1, doc.numberOfPages)
            assertEquals(300f, doc.getPage(0).mediaBox.width)
        }
    }
}

package io.github.bigrirason_debug.platen.pdf

import io.github.bigrirason_debug.platen.model.Background
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Size
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test

class TmpDumpTest {
    private val dir = File("/tmp/claude-0/-home-user-cheche/3c46f7dd-2ab6-58f6-a8d8-1e065f99cfe9/scratchpad/probe")

    @Test
    fun dump() {
        dir.mkdirs()
        val pdf = PdfFixtures.threePagePdf()
        val png = PdfFixtures.pngAsset()
        val assets: (String) -> ByteArray? = { id -> when (id) { "planner" -> pdf; "sticker" -> png; "rot" -> rotatedPdf(); else -> null } }
        val size = Size(PdfFixtures.W, PdfFixtures.H)
        val p1 = Page(
            "p1", size, Background.PdfPage("planner", 1),
            strokes = listOf(
                PdfFixtures.stroke("s1", "pen", 0xFF000000.toInt(), 4f, 50f to 200f, 150f to 220f, 250f to 210f),
                PdfFixtures.stroke("s2", "pencil", 0xFF336699.toInt(), 6f, 60f to 300f, 160f to 320f, 260f to 310f),
                PdfFixtures.stroke("s3", "hl", 0xFFFFEB3B.toInt(), 12f, 40f to 560f, 200f to 560f, opacity = 0.5f),
            ),
            objects = listOf(
                PageObject.TextBox("t1", Rect(40f, 40f, 300f, 100f), "hello world from Platen, wrapping onto a second line maybe"),
                PageObject.Table("tb1", Rect(40f, 380f, 240f, 440f), 2, 2, listOf(100f, 100f), listOf(30f, 30f), cells = mapOf("0,0" to "alpha", "1,1" to "beta")),
                PageObject.Image("im1", Rect(300f, 300f, 360f, 360f), "sticker", rotationDeg = 15f),
                PageObject.Link("l2", Rect(40f, 160f, 140f, 190f), "https://platen.example/"),
            ),
        )
        val tpl = Page("p2", size, Background.Template("grid", spacing = 24f))
        val dots = Page("p3", size, Background.Template("dots", spacing = 16f, marginTop = 40f))
        val rot = Page("p4", Size(600f, 400f), Background.PdfPage("rot", 0),
            strokes = listOf(PdfFixtures.stroke("r1", "pen", 0xFFCC0000.toInt(), 4f, 20f to 20f, 300f to 60f)),
            objects = listOf(PageObject.TextBox("rt", Rect(20f, 100f, 400f, 140f), "rotated page text (top-left region)")))
        val note = Note("n", "dump", 0L, 0L, listOf(p1, tpl, dots, rot))
        val r = PdfExporter.export(note, assets, PdfFixtures.fakeGeometry, PdfFixtures.resolve)
        File(dir, "out.pdf").writeBytes(r.bytes)
        println("WARNINGS=" + r.warnings)
        openPdf(rotatedPdf()).use { s -> println("ROT size=" + s.pageSize(0) + " links=" + s.links(0)) }
    }

    private fun rotatedPdf(): ByteArray = PDDocument().use { doc ->
        val p = PDPage(PDRectangle(400f, 600f)); p.rotation = 90; doc.addPage(p)
        PDPageContentStream(doc, p).use { cs ->
            cs.beginText(); cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 14f); cs.newLineAtOffset(40f, 560f); cs.showText("ROT user-space top-left"); cs.endText()
        }
        val uri = PDAnnotationLink().apply { rectangle = PDRectangle(100f, 500f, 100f, 50f); action = PDActionURI().apply { uri = "https://r.example/" } }
        p.annotations = listOf(uri)
        doc.documentId = 2L
        ByteArrayOutputStream().also { doc.save(it) }.toByteArray()
    }
}

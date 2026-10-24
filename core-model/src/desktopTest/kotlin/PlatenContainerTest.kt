package io.github.bigrirason_debug.platen.model

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatenContainerTest {
    private fun sampleNote(): Note {
        val s1 = Stroke(
            id = "s1", brushId = "abc123def456", color = 0xFF1A1A1A.toInt(), size = 3f,
            samples = (0 until 50).map { InputSample(10f + it, 20f + (it % 5), 0.3f + 0.01f * it, 0.1f, -0.2f, it * 8f) },
        )
        val s2 = s1.copy(id = "s2", color = 0xFFC8102E.toInt(), overrides = StrokeOverrides(streamline = 0.5f))
        val page1 = Page(
            id = "p1", strokes = listOf(s1, s2),
            objects = listOf(
                PageObject.TextBox("t1", Rect(10f, 10f, 200f, 60f), "hello"),
                PageObject.Table("tb1", Rect(20f, 100f, 300f, 220f), 3, 2, listOf(140f, 140f), listOf(40f, 40f, 40f), cells = mapOf("0,0" to "a")),
                PageObject.Link("l1", Rect(0f, 0f, 50f, 50f), "page:p2"),
            ),
        )
        val page2 = Page(id = "p2", background = Background.PdfPage("asset1", 0))
        return Note("n1", "테스트 노트", 1000L, 2000L, listOf(page1, page2))
    }

    @Test
    fun roundTripPreservesEverything() {
        val note = sampleNote()
        val bundle = PlatenContainer.Bundle(
            note,
            brushes = mapOf("abc123def456" to byteArrayOf(1, 2, 3)),
            assets = mapOf("asset1" to ("application/pdf" to byteArrayOf(9, 9, 9))),
        )
        val bytes = PlatenContainer.writeToBytes(bundle)
        val result = PlatenContainer.read(bytes.inputStream())
        assertEquals(note, result.bundle.note)
        assertContentEquals(byteArrayOf(1, 2, 3), result.bundle.brushes["abc123def456"])
        assertContentEquals(byteArrayOf(9, 9, 9), result.bundle.assets["asset1"]!!.second)
        assertTrue(result.warnings.isEmpty(), result.warnings.toString())
    }

    @Test
    fun outputIsDeterministic() {
        val bundle = PlatenContainer.Bundle(sampleNote())
        val a = PlatenContainer.writeToBytes(bundle)
        val b = PlatenContainer.writeToBytes(bundle)
        assertContentEquals(a, b)
        assertEquals(PlatenContainer.md5Hex(a), PlatenContainer.md5Hex(b))
    }

    @Test
    fun mimetypeIsFirstAndStored() {
        val bytes = PlatenContainer.writeToBytes(PlatenContainer.Bundle(sampleNote()))
        // 로컬 파일 헤더: 시그니처(4) 버전(2) 플래그(2) 메서드(2) ... 이름 길이@26, 이름@30
        assertEquals(0x04034b50, java.nio.ByteBuffer.wrap(bytes, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int)
        val method = java.nio.ByteBuffer.wrap(bytes, 8, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).short.toInt()
        assertEquals(0, method, "mimetype must be STORED")
        val nameLen = java.nio.ByteBuffer.wrap(bytes, 26, 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).short.toInt()
        assertEquals("mimetype", String(bytes, 30, nameLen))
    }

    @Test
    fun unknownFieldsAreIgnoredForForwardCompat() {
        val files = HashMap<String, ByteArray>()
        files["mimetype"] = PlatenSchema.MIME.encodeToByteArray()
        files["manifest.json"] = """{"schema":1,"minReader":1,"noteId":"n","title":"t","created":1,"modified":2,"pageIds":["p"],"futureField":42}""".encodeToByteArray()
        files["pages/0001.json"] = """{"id":"p","size":{"width":100,"height":100},"background":{"type":"io.github.bigrirason_debug.platen.model.Background.Solid","color":-1},"objects":[],"strokes":[],"extra":true}""".encodeToByteArray()
        files["pages/0001.strokes"] = PlatenContainer.encodeSamples(emptyList())
        val r = PlatenContainer.readFiles(files)
        assertEquals("p", r.bundle.note.pages.single().id)
    }

    @Test
    fun strokesHeaderAndWiderStrideForwardCompat() {
        val s = Stroke("s", "b", 0, 1f, samples = listOf(InputSample(1f, 2f, 0.5f, 0f, 0f, 3f)))
        val enc = PlatenContainer.encodeSamples(listOf(s))
        assertEquals("PSTK", String(enc, 0, 4))
        // stride 8 로 쓰인(미래 버전) 데이터도 앞 6개만 읽는다
        val wide = java.nio.ByteBuffer.allocate(12 + 8 * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        wide.put(enc, 0, 4); wide.putShort(2); wide.putShort(8); wide.putInt(0)
        floatArrayOf(1f, 2f, 0.5f, 0f, 0f, 3f, 9f, 9f).forEach { wide.putFloat(it) }
        val dec = PlatenContainer.decodeSamples(wide.array(), listOf(1))
        assertEquals(InputSample(1f, 2f, 0.5f, 0f, 0f, 3f), dec.single().single())
    }

    @Test
    fun rejectsPathTraversalEntries() {
        val bout = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(bout).use { z ->
            z.putNextEntry(java.util.zip.ZipEntry("../evil")); z.write(1); z.closeEntry()
        }
        val ex = kotlin.runCatching { PlatenContainer.read(bout.toByteArray().inputStream()) }.exceptionOrNull()
        assertTrue(ex is IllegalArgumentException, "expected rejection, got $ex")
    }
}

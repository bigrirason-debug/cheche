package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.Grain
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrushPackageTest {
    private fun sample(): LoadedBrush {
        val spec = BrushSpec(id = "t", name = "T", engine = BrushEngine.stamp, grain = Grain(image = "grain.png", depth = 0.5f))
        val tip = ImageCodec.encodePng(TestFixtures.circle(32))
        val grain = ImageCodec.encodePng(ProceduralImages.noiseGrain(64, 3))
        return LoadedBrush(spec, tip, grain)
    }

    @Test
    fun roundTrip() {
        val b = sample()
        val bytes = BrushPackage.write(b)
        val loaded = BrushPackage.load(bytes)
        assertEquals(b.spec, loaded.spec)
        assertContentEquals(b.tipPng, loaded.tipPng)
        assertContentEquals(b.grainPng, loaded.grainPng)
    }

    @Test
    fun deterministicId() {
        val a = BrushPackage.pack(sample())
        val b = BrushPackage.pack(sample())
        assertContentEquals(a.bytes, b.bytes)
        assertEquals(a.id, b.id)
        assertEquals(12, a.id.length)
    }

    @Test
    fun pngEncoderIsDecodableByImageIo() {
        val img = TestFixtures.circle(40)
        val back = ImageCodec.decodeToGray(ImageCodec.encodePng(img))
        assertEquals(40, back.width); assertEquals(40, back.height)
        assertContentEquals(img.alpha, back.alpha)
    }

    // ---- 방어 상한(COUNCIL-REVIEW 좌석 4)

    @Test
    fun rejectsPathTraversalNames() {
        for (bad in listOf("../evil.json", "/abs/brush.json", "a\\b.png", "x/../../y")) {
            val z = TestFixtures.zip(mapOf(bad to byteArrayOf(1)))
            val ex = assertFailsWith<ImportLimitExceeded>(bad) { SafeZip.read(z) }
            assertTrue(ex.message!!.contains("엔트리 이름"), ex.message)
        }
    }

    @Test
    fun rejectsTooManyEntries() {
        val entries = (0 until 600).associate { "e$it.bin" to byteArrayOf(0) }
        assertFailsWith<ImportLimitExceeded> { SafeZip.read(TestFixtures.zip(entries)) }
    }

    @Test
    fun rejectsHugeIhdrBeforeDecoding() {
        // 폭 5000 IHDR 만 가진 가짜 PNG(본문 없음) → 디코드 시도 전에 거부
        val w = ByteWriter()
        w.bytes(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A))
        w.u32(13); w.ascii("IHDR"); w.u32(5000); w.u32(10); w.u8(8); w.u8(0); w.u8(0); w.u8(0); w.u8(0); w.u32(0)
        val ex = assertFailsWith<ImportLimitExceeded> { ImageCodec.decodeToGray(w.toByteArray()) }
        assertTrue(ex.message!!.contains("헤더"), ex.message)
        assertNotNull(ImageCodec.probeSize(w.toByteArray()))
    }
}

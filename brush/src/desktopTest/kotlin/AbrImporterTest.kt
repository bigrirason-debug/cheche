package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.BrushEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/** ABR 바이트를 직접 구성(§2.2 구조)해 파싱을 검증한다. */
class AbrImporterTest {
    private val tip = TestFixtures.circle(40, 0.45)

    /** v2: 헤더(2,count) + 샘플 레코드(type 2, UCS-2 이름, bounds, depth 8, RLE). */
    private fun v2File(): ByteArray {
        val body = ByteWriter()
        body.u32(0)              // misc
        body.u16(25)             // spacing %
        TestFixtures.unicode(body, "Dot")
        body.u8(1)               // antialias
        body.u16(0); body.u16(0); body.u16(tip.height); body.u16(tip.width)
        body.u32(0); body.u32(0); body.u32(tip.height); body.u32(tip.width)
        body.u16(8)              // depth
        body.u8(1)               // compression RLE
        body.bytes(TestFixtures.rleBody(tip))
        val rec = body.toByteArray()
        val w = ByteWriter()
        w.u16(2); w.u16(1)
        w.u16(2); w.u32(rec.size); w.bytes(rec)
        return w.toByteArray()
    }

    @Test
    fun v2SampledTip() {
        val r = BrushImporter.import("old.abr", v2File())
        val b = r.brushes.single()
        assertEquals("Dot", b.spec.name)
        assertEquals("abr", b.spec.source.format)
        assertEquals(0.25f, b.spec.stamp.spacing)
        assertEquals(40f, b.spec.size.max)
        val img = ImageCodec.decodeToGray(assertNotNull(b.tipPng))
        // 원 지름 ≈ 36 → 트림 + 1px 패딩 → 정사각
        assertEquals(img.width, img.height)
        assertTrue(img.width in 36..40, "tip ${img.width}")
        assertEquals(255, img.get(img.width / 2, img.height / 2))
        assertEquals(0, img.get(0, 0))
        // 원형·그레인 없음 → profile 승격
        assertEquals(BrushEngine.profile, b.spec.engine)
    }

    private fun preset(uuid: String = TestFixtures.UUID_A, mutate: (TestFixtures.DescWriter) -> Unit = {}) = TestFixtures.DescWriter("", "sampledBrush")
        .text("Nm  ", "My Brush")
        .untf("Dmtr", "#Pxl", 40.0)
        .untf("Spcn", "#Prc", 20.0)
        .untf("Angl", "#Ang", 30.0)
        .untf("Rndn", "#Prc", 80.0)
        .bool("Intr", true)
        .bool("useTipDynamics", true)
        .objc("szVr", TestFixtures.DescWriter("", "null").long("bVTy", 2).untf("jitter", "#Prc", 10.0).long("fStp", 0).untf("Mnm", "#Prc", 0.0))
        .untf("minimumDiameter", "#Prc", 30.0)
        .objc("angleDynamics", TestFixtures.DescWriter("", "null").long("bVTy", 6).untf("jitter", "#Prc", 0.0))
        .bool("usePaintDynamics", true)
        .objc("opVr", TestFixtures.DescWriter("", "null").long("bVTy", 2).untf("Mnm", "#Prc", 25.0))
        .enum("textureBlendMode", "BlnM", "Mltp")
        .text("sampledData", uuid)
        .also(mutate)

    private fun v6File(presetWriter: TestFixtures.DescWriter = preset(), withDesc: Boolean = true, samp: ByteArray = TestFixtures.sampRecord(TestFixtures.UUID_A, tip)): ByteArray {
        val w = ByteWriter()
        w.u16(6); w.u16(2)
        w.bytes(TestFixtures.block("samp", samp))
        if (withDesc) w.bytes(TestFixtures.descBlock(TestFixtures.DescWriter("", "null").list("Brsh", presetWriter)))
        return w.toByteArray()
    }

    @Test
    fun v62DescriptorMapping() {
        val r = BrushImporter.import("modern.abr", v6File())
        assertEquals(1, r.brushes.size, r.notes.toString())
        val s = r.brushes[0].spec
        assertEquals("My Brush", s.name)
        assertEquals(0.2f, s.stamp.spacing)                       // Spcn 20% → 0.2
        assertEquals(40f, s.size.base); assertEquals(40f, s.size.max)
        assertEquals(30f, s.stamp.tip.angle); assertEquals(0.8f, s.stamp.tip.roundness)
        assertEquals(0.7f, s.pressureSize.amount)                 // 1 − minimumDiameter 30%
        assertEquals(0.1f, s.stamp.sizeJitter)
        assertEquals(1f, s.stamp.tip.followStroke)                // bVTy 6 → followStroke
        assertEquals(0.75f, s.pressureOpacity.amount)             // opVr Mnm 25%
        assertTrue(SpecUtil.isMonotone(s.pressureSize.curve))
        val img = ImageCodec.decodeToGray(assertNotNull(r.brushes[0].tipPng))
        assertTrue(img.width in 36..40 && img.width == img.height)
        assertTrue(r.notes.any { it.contains("ABR v6.2") }, r.notes.toString())
    }

    @Test
    fun unknownDescriptorTypeFallsBackToTipOnly() {
        val r = BrushImporter.import("weird.abr", v6File(preset { it.unknown("mystery") }))
        assertEquals(1, r.brushes.size)
        assertNotNull(r.brushes[0].tipPng)
        assertTrue(r.notes.any { it.contains("desc 해석 실패") }, r.notes.toString())
        assertTrue(r.brushes[0].spec.source.lossNotes.any { it.contains("팁만 복구") })
        assertEquals(0.15f, r.brushes[0].spec.stamp.spacing)
    }

    @Test
    fun missingDescIsTipOnly() {
        val r = BrushImporter.import("tips.abr", v6File(withDesc = false))
        assertEquals(1, r.brushes.size)
        assertEquals("tips", r.brushes[0].spec.name)
        assertTrue(r.notes.any { it.contains("팁만 복구") })
    }

    @Test
    fun sixteenBitTakesHighByte() {
        val w16 = GrayImage(4, 2, ByteArray(8) { 0 })
        // 16-bit 원본 행: 상위 바이트 200, 하위 7 → raw 로 구성(압축 0)
        val body = ByteWriter()
        body.u8(TestFixtures.UUID_A.length); body.ascii(TestFixtures.UUID_A); repeat(264) { body.u8(0) }
        body.u32(0); body.u32(0); body.u32(2); body.u32(4)
        body.u16(16); body.u8(0)
        repeat(8) { body.u8(200); body.u8(7) }
        val b = body.toByteArray()
        val rec = ByteWriter(); rec.u32(b.size); rec.bytes(b); var pad = (4 - b.size % 4) % 4; while (pad-- > 0) rec.u8(0)
        val r = BrushImporter.import("deep.abr", v6File(withDesc = false, samp = rec.toByteArray()))
        val img = ImageCodec.decodeToGray(assertNotNull(r.brushes.single().tipPng))
        assertTrue(img.alpha.any { (it.toInt() and 0xFF) == 200 })
        assertTrue(w16.width == 4)
    }

    @Test
    fun lengthBeyondEofAndHugeBoundsAreHandled() {
        // samp 레코드 길이가 블록을 넘음 → 팁 없음 + note, 예외 없음
        val bad = ByteWriter(); bad.u32(99999); bad.u32(0)
        val r = BrushImporter.import("trunc.abr", v6File(withDesc = false, samp = bad.toByteArray()))
        assertEquals(0, r.brushes.size)
        assertTrue(r.notes.any { it.contains("넘음") }, r.notes.toString())
        // bounds 한 변 9000 → 레코드 건너뜀
        val huge = TestFixtures.sampRecord(TestFixtures.UUID_A, tip, right = 9000)
        val r2 = BrushImporter.import("huge.abr", v6File(withDesc = false, samp = huge))
        assertEquals(0, r2.brushes.size)
        assertTrue(r2.notes.any { it.contains("bounds") }, r2.notes.toString())
        // 8BIM 블록 길이가 파일 끝을 넘음 → 중단 + note
        val w = ByteWriter(); w.u16(6); w.u16(2); w.ascii("8BIM"); w.ascii("samp"); w.u32(1_000_000); w.u32(0)
        val r3 = AbrImporter.import("eof.abr", w.toByteArray(), 0L)
        assertTrue(r3.notes.any { it.contains("파일 끝") })
        // 지원하지 않는 버전
        val v5 = ByteWriter(); v5.u16(5); v5.u16(0); v5.u32(0)
        assertFailsWith<BrushFormatException> { AbrImporter.import("v5.abr", v5.toByteArray(), 0L) }
    }

    @Test
    fun magicDetectionWithoutExtension() {
        assertTrue(BrushImporter.isAbr(v2File()))
        assertTrue(BrushImporter.isAbr(v6File()))
        assertTrue(!BrushImporter.isAbr(TestFixtures.zip(mapOf("a" to byteArrayOf(1)))))
        assertEquals(1, BrushImporter.import("noext", v6File()).brushes.size)
    }
}

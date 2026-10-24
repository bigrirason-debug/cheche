package io.github.bigrirason_debug.platen.brush

import com.dd.plist.BinaryPropertyListWriter
import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSNumber
import com.dd.plist.NSObject
import com.dd.plist.NSString
import com.dd.plist.UID
import io.github.bigrirason_debug.platen.model.BlendMode
import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.CurvePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * NSKeyedArchiver 형식 bplist 를 dd-plist 로 **직접 생성**해 임포트한다(§1.2 구조: $archiver/$top/$objects, SilicaBrush 클래스).
 * 루트는 일부러 `$objects[2]` 에 두어 `$objects[1]` 하드코딩이 없음을 확인한다.
 */
class ProcreateImporterTest {
    private class ArchiveBuilder {
        val objects = ArrayList<NSObject>().apply { add(NSString("\$null")) }
        fun uid(o: NSObject): UID { objects += o; return UID("", byteArrayOf((objects.size - 1).toByte())) }
        fun classDict(vararg names: String): UID = uid(NSDictionary().apply {
            put("\$classname", NSString(names[0])); put("\$classes", NSArray(*names.map { NSString(it) }.toTypedArray()))
        })
        fun curve(cls: UID, arrayCls: UID, vararg pts: String): UID {
            val arr = NSDictionary().apply { put("\$class", arrayCls); put("NS.objects", NSArray(*pts.map { uid(NSString(it)) }.toTypedArray())) }
            return uid(NSDictionary().apply { put("\$class", cls); put("points", uid(arr)); put("resolution", NSNumber(1024)) })
        }
        fun build(root: UID): ByteArray {
            val top = NSDictionary().apply {
                put("\$archiver", NSString("NSKeyedArchiver")); put("\$version", NSNumber(100000))
                put("\$top", NSDictionary().apply { put("root", root) })
                put("\$objects", NSArray(*objects.toTypedArray()))
            }
            return BinaryPropertyListWriter.writeToArray(top)
        }
    }

    /** §1.3 키 10개 이상을 가진 SilicaBrush 아카이브. */
    private fun archive(name: String, spacing: Double = 0.12, inverted: Boolean = true, extra: Map<String, NSObject> = emptyMap()): ByteArray {
        val b = ArchiveBuilder()
        val brushCls = b.classDict("SilicaBrush", "ValkyrieBrush", "NSObject") // index 1 (루트 아님)
        val curveCls = b.classDict("ValkyrieMagnitudinalCurve", "NSObject")
        val arrayCls = b.classDict("NSArray", "NSObject")
        val root = NSDictionary()
        val rootUid = b.uid(root)
        root.put("\$class", brushCls)
        root.put("name", b.uid(NSString(name)))
        root.put("authorName", NSString("tester"))
        root.put("version", NSNumber(2))
        root.put("plotSpacing", NSNumber(spacing))
        root.put("plotJitter", NSNumber(0.2))
        root.put("plotSmoothing", NSNumber(0.45))
        root.put("maxSize", NSNumber(0.5))
        root.put("minSize", NSNumber(0.02))
        root.put("paintSize", NSNumber(0.5))
        root.put("maxOpacity", NSNumber(1.0))
        root.put("paintOpacity", NSNumber(0.9))
        root.put("dynamicsPressureSize", NSNumber(0.8))
        root.put("dynamicsPressureSizeCurve", b.curve(curveCls, arrayCls, "{0, 0}", "{0.3, 0.15}", "{0.7, 0.8}", "{1, 1}"))
        root.put("dynamicsPressureOpacity", NSNumber(0.4))
        root.put("dynamicsPressureOpacityCurve", b.curve(curveCls, arrayCls, "{0, 0}", "{1, 1}"))
        root.put("pencilTaperStartLength", NSNumber(0.05))
        root.put("pencilTaperEndLength", NSNumber(0.1))
        root.put("pencilTaperSize", NSNumber(0.2))
        root.put("shapeInverted", NSNumber(inverted))
        root.put("shapeRotation", NSNumber(1.0))
        root.put("shapeScatter", NSNumber(0.5))
        root.put("shapeRandomise", NSNumber(true))
        root.put("textureInverted", NSNumber(true))
        root.put("grainDepth", NSNumber(0.7))
        root.put("textureApplication", NSNumber(1))
        root.put("blendMode", NSNumber(1))
        root.put("dynamicsMix", NSNumber(0.3)) // 웻믹스 → 버림
        root.put("dynamicsJitterHue", NSNumber(0.1)) // 컬러 → 버림
        root.put("taperStartLength", NSNumber(0.3)) // 터치용 → 버림
        for ((k, v) in extra) root.put(k, v)
        return b.build(rootUid)
    }

    private val shapePng = ImageCodec.encodePng(TestFixtures.circle(64, 0.4))
    private val grainPng = ImageCodec.encodePng(GrayImage.filled(80, 80, 200))

    @Test
    fun singleBrushMapping() {
        val zip = TestFixtures.zip(mapOf("Brush.archive" to archive("Test Ink"), "Shape.png" to shapePng, "Grain.png" to grainPng, "QuickLook/Thumbnail.png" to byteArrayOf(1)))
        val r = BrushImporter.import("Test Ink.brush", zip, importedAt = 1234L)
        assertEquals(1, r.packages.size, r.notes.toString())
        val b = r.brushes[0]; val s = b.spec
        assertEquals("Test Ink", s.name); assertEquals("tester", s.author)
        assertEquals("procreate", s.source.format); assertEquals("Test Ink", s.source.originalName); assertEquals(1234L, s.source.importedAt)
        assertEquals(0.12f, s.stamp.spacing)                                  // plotSpacing 그대로
        assertEquals(0.2f, s.stamp.jitter)
        assertEquals(0.45f, s.smoothing.streamline)
        assertEquals(50f, s.size.max); assertEquals(2f, s.size.min); assertEquals(26f, s.size.base) // K=100
        assertEquals(0.9f, s.opacity)
        assertEquals(0.8f, s.pressureSize.amount)
        assertEquals(listOf(CurvePoint(0f, 0f), CurvePoint(0.3f, 0.15f), CurvePoint(0.7f, 0.8f), CurvePoint(1f, 1f)), s.pressureSize.curve) // 커브 점 보존
        assertEquals(0.4f, s.pressureOpacity.amount)
        assertEquals(30f, s.taper.endLength); assertEquals(15f, s.taper.startLength); assertEquals(0.2f, s.taper.endSize) // L=300
        assertEquals(1f, s.stamp.tip.followStroke); assertEquals(90f, s.stamp.tip.scatterAngle); assertTrue(s.stamp.tip.randomStart)
        assertEquals(BlendMode.multiply, s.blend)
        assertEquals(0.7f, s.grain.depth); assertEquals("grain.png", s.grain.image); assertEquals(false, s.grain.inverted)
        assertEquals(BrushEngine.stamp, s.engine)
        assertEquals("procreate-test-ink", s.id)
        // 반전이 이미지에 구워졌는가: 셰이프(흰 원) → 중앙 0, 모서리 255 / 그레인 200 → 55
        val tip = ImageCodec.decodeToGray(assertNotNull(b.tipPng))
        assertTrue(tip.get(tip.width / 2, tip.height / 2) < 8, "center ${tip.get(tip.width / 2, tip.height / 2)}")
        assertEquals(255, tip.get(1, 1))
        val grain = ImageCodec.decodeToGray(assertNotNull(b.grainPng))
        assertEquals(80, grain.width); assertEquals(55, grain.get(3, 3))
        // 버린 키 기록
        val dropped = s.source.lossNotes.first { it.startsWith("버림") }
        assertTrue(dropped.contains("dynamicsMix") && dropped.contains("dynamicsJitterHue"), dropped)
        assertTrue(s.source.lossNotes.any { it.contains("K=100") } && s.source.lossNotes.any { it.contains("L=300") })
        assertTrue(s.source.lossNotes.any { it.contains("taper*") })
        // 패키지 라운드트립
        val loaded = BrushPackage.load(r.packages[0].bytes)
        assertEquals(s, loaded.spec)
    }

    @Test
    fun roundTipWithoutGrainIsPromotedToProfile() {
        val zip = TestFixtures.zip(mapOf("Brush.archive" to archive("Plain", inverted = false), "Shape.png" to shapePng))
        val b = BrushImporter.import("plain.brush", zip).brushes.single()
        assertEquals(BrushEngine.profile, b.spec.engine)
        assertNull(b.spec.grain.image)
        assertTrue(b.spec.source.lossNotes.any { it.contains("profile") })
    }

    @Test
    fun bundledShapeUsesNameHeuristic() {
        val zip = TestFixtures.zip(mapOf("Brush.archive" to archive("Bundled", inverted = false, extra = mapOf(
            "bundledShapePath" to NSString("Brush-Preset-Soft.png"), "bundledGrainPath" to NSString("Grain-Paper.png"),
        ))))
        val b = BrushImporter.import("bundled.brush", zip).brushes.single()
        assertNotNull(b.tipPng); assertNull(b.grainPng)
        assertTrue(b.spec.source.lossNotes.any { it.contains("Brush-Preset-Soft.png") && it.contains("soft") })
        assertTrue(b.spec.source.lossNotes.any { it.contains("Grain-Paper.png") })
        assertEquals(0.7f, b.spec.grain.depth) // depth 보존
        assertEquals(BrushEngine.stamp, b.spec.engine) // 소프트 팁은 승격 안 함
    }

    @Test
    fun brushsetPreservesPlistOrderAndSkipsResetAndMacosx() {
        val plist = """<?xml version="1.0" encoding="UTF-8"?>
            <plist version="1.0"><dict><key>name</key><string>Set</string>
            <key>brushes</key><array><string>B-UUID</string><string>A-UUID</string></array></dict></plist>""".trimIndent().encodeToByteArray()
        val zip = TestFixtures.zip(linkedMapOf(
            "brushset.plist" to plist,
            "A-UUID/Brush.archive" to archive("Alpha"), "A-UUID/Shape.png" to shapePng, "A-UUID/Grain.png" to grainPng,
            "A-UUID/Reset/Brush.archive" to archive("Alpha Reset"),
            "B-UUID/Brush.archive" to archive("Beta", spacing = 0.2), "B-UUID/Shape.png" to shapePng,
            "__MACOSX/A-UUID/._Brush.archive" to byteArrayOf(0, 1, 2),
        ))
        val r = BrushImporter.import("set.brushset", zip)
        assertEquals(listOf("Beta", "Alpha"), r.brushes.map { it.spec.name })
        assertEquals(0.2f, r.brushes[0].spec.stamp.spacing)
        assertEquals(2, r.packages.map { it.id }.toSet().size)
    }

    @Test
    fun brokenArchiveRecoversTipOnly() {
        val zip = TestFixtures.zip(mapOf("Brush.archive" to byteArrayOf(0x62, 0x70, 0x6c, 0x69, 0x73, 0x74, 0x30, 0x30, 9, 9, 9), "Shape.png" to shapePng))
        val r = BrushImporter.import("Broken Pen.brush", zip)
        val b = r.brushes.single()
        assertEquals("Broken Pen", b.spec.name)
        assertNotNull(b.tipPng)
        assertTrue(r.notes.any { it.contains("해독 실패") }, r.notes.toString())
        assertTrue(b.spec.source.lossNotes.any { it.contains("팁만 복구") })
    }

    @Test
    fun archiveWithoutTopRootOrWithCycleIsRejected() {
        // $top.root 없음
        val noTop = NSDictionary().apply {
            put("\$archiver", NSString("NSKeyedArchiver")); put("\$objects", NSArray(NSString("\$null"), NSDictionary()))
            put("\$top", NSDictionary())
        }
        assertFailsWith<BrushFormatException> { decodeNSKeyedArchive(BinaryPropertyListWriter.writeToArray(noTop)) }
        // UID 순환: objects[1].self → UID(1)
        val cyc = NSDictionary().apply {
            put("\$archiver", NSString("NSKeyedArchiver"))
            put("\$objects", NSArray(NSString("\$null"), NSDictionary().apply { put("self", UID("", byteArrayOf(1))) }))
            put("\$top", NSDictionary().apply { put("root", UID("", byteArrayOf(1))) })
        }
        assertFailsWith<BrushFormatException> { decodeNSKeyedArchive(BinaryPropertyListWriter.writeToArray(cyc)) }
    }
}

package io.github.bigrirason_debug.platen.brush

import io.github.bigrirason_debug.platen.model.BrushEngine
import io.github.bigrirason_debug.platen.model.BrushRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuiltInBrushesTest {
    @Test
    fun rosterHasNine() {
        val all = BuiltInBrushes.all()
        assertEquals(9, all.size)
        assertEquals(
            listOf("pen-fine", "pen-bold", "fountain-soft", "fountain-flex", "pencil-hb", "pencil-6b", "monoline", "highlighter-flat", "highlighter-round"),
            all.map { it.spec.id },
        )
        assertEquals(9, all.map { it.spec.id }.toSet().size)
    }

    @Test
    fun packagedIdsAreDeterministic() {
        val a = BuiltInBrushes.packaged()
        val b = BuiltInBrushes.packaged()
        assertEquals(a.keys, b.keys)
        assertEquals(9, a.size)
        for ((id, bytes) in a) {
            assertEquals(id, BrushPackage.idOf(bytes))
            val loaded = BrushPackage.load(bytes)
            assertNotNull(BuiltInBrushes.byId(loaded.spec.id))
        }
    }

    @Test
    fun specsAreValid() {
        for (b in BuiltInBrushes.all()) {
            val s = b.spec
            assertTrue(SpecUtil.isMonotone(s.pressureSize.curve), "${s.id} pressureSize curve")
            assertTrue(SpecUtil.isMonotone(s.pressureOpacity.curve), "${s.id} pressureOpacity curve")
            assertTrue(s.stamp.spacing >= 0.03f, "${s.id} spacing")
            assertTrue(s.size.min <= s.size.base && s.size.base <= s.size.max, "${s.id} size range")
            assertTrue(s.exposed.isNotEmpty(), "${s.id} exposed")
            assertEquals("native", s.source.format)
            if (s.engine == BrushEngine.stamp) {
                assertNotNull(b.tipPng, "${s.id} tip"); assertNotNull(b.grainPng, "${s.id} grain")
                assertTrue(s.grain.depth > 0f)
            } else {
                assertEquals(null, b.tipPng)
            }
        }
        assertEquals(BrushRole.highlighter, BuiltInBrushes.highlighterFlat.role)
        assertEquals(0f, BuiltInBrushes.monoline.pressureSize.amount)
        assertEquals(0.9f, BuiltInBrushes.fountainFlex.pressureSize.amount)
    }

    @Test
    fun proceduralImagesAreDeterministicAndSized() {
        val t1 = ImageCodec.decodeToGray(BuiltInBrushes.pencilTipPng)
        val t2 = ImageCodec.decodeToGray(ImageCodec.encodePng(ProceduralImages.roundTip(64, hardness = 0.55f)))
        assertEquals(64, t1.width)
        assertTrue(t1.alpha.contentEquals(t2.alpha))
        val g = ImageCodec.decodeToGray(BuiltInBrushes.pencilGrainPng)
        assertEquals(256, g.width)
        val mean = ImageNormalizer.mean(g)
        assertTrue(mean in 0.3f..0.7f, "grain mean $mean")
        assertTrue(ProceduralImages.noiseGrain(256, 7).alpha.contentEquals(ProceduralImages.noiseGrain(256, 7).alpha))
        assertTrue(!ProceduralImages.noiseGrain(256, 8).alpha.contentEquals(ProceduralImages.noiseGrain(256, 7).alpha))
    }
}

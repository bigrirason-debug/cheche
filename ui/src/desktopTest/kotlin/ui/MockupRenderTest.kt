package io.github.bigrirason_debug.platen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import io.github.bigrirason_debug.platen.model.AppSettings
import io.github.bigrirason_debug.platen.model.InputSample
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PageObject
import io.github.bigrirason_debug.platen.model.Rect
import io.github.bigrirason_debug.platen.model.Stroke
import io.github.bigrirason_debug.platen.model.ThemeMode
import io.github.bigrirason_debug.platen.ui.canvas.CanvasScreen
import io.github.bigrirason_debug.platen.ui.canvas.CanvasState
import io.github.bigrirason_debug.platen.ui.canvas.Viewport
import io.github.bigrirason_debug.platen.ui.diag.DiagnosticsState
import io.github.bigrirason_debug.platen.ui.repo.BuiltinBrushes
import io.github.bigrirason_debug.platen.ui.repo.NoteAttachments
import io.github.bigrirason_debug.platen.ui.repo.NoteSummary
import io.github.bigrirason_debug.platen.ui.repo.UiPrefsDefaults
import io.github.bigrirason_debug.platen.ui.screens.HomeScreen
import io.github.bigrirason_debug.platen.ui.screens.PenSheetContent
import io.github.bigrirason_debug.platen.ui.state.EditorController
import io.github.bigrirason_debug.platen.ui.theme.PlatenPalette
import io.github.bigrirason_debug.platen.ui.theme.PlatenShapes
import io.github.bigrirason_debug.platen.ui.theme.PlatenTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 홈·캔버스·펜 설정 × 라이트/다크 목업 PNG → `ui/build/mockups/`(head 가 docs/design/mockups 로 복사).
 * 헤드리스 렌더가 불가능한 환경이면 실패 대신 보고(println)만 한다.
 */
class MockupRenderTest {
    private val outDir = File(System.getProperty("platen.mockups.dir") ?: "build/mockups")

    private fun handwriting(seed: Int, page: Page): List<Stroke> {
        val out = ArrayList<Stroke>()
        var y = 90f
        var id = 0
        for (line in 0 until 6) {
            var x = 60f
            while (x < page.size.width - 80f) {
                val len = 18f + ((seed + id) % 5) * 6f
                val samples = (0..14).map { k ->
                    val t = k / 14f
                    InputSample(x + t * len, y + sin(t * 6.283f + id) * 7f, 0.35f + 0.5f * sin(t * 3.1416f), t = k * 8f)
                }
                out += Stroke("hw-$seed-$id", if (line == 2) BuiltinBrushes.fountain1.id else BuiltinBrushes.pen1.id, if (line == 4) PlatenPalette.RED else PlatenPalette.BLACK, if (line == 2) 3f else 2.5f, samples = samples)
                id++; x += len + 8f
            }
            y += 46f
        }
        out += Stroke("hl", BuiltinBrushes.highlighter1.id, PlatenPalette.YELLOW, 14f, 0.9f, samples = (0..10).map { InputSample(60f + it * 20f, 136f, 0.8f, t = it * 8f) })
        return out
    }

    private fun sampleNote(): Note {
        val p1 = Page("p1")
        val p2 = Page("p2")
        return Note("mock", "강의 노트 — 열역학 3장", 0, 1_761_300_000_000L, listOf(
            p1.copy(strokes = handwriting(1, p1), objects = listOf(PageObject.TextBox("t1", Rect(260f, 400f, 420f, 440f), "텍스트 상자"), PageObject.Table("tb", Rect(60f, 420f, 240f, 500f), 2, 3, listOf(60f, 60f, 60f), listOf(40f, 40f), cells = mapOf("0,0" to "T", "0,1" to "P")))),
            p2.copy(strokes = handwriting(2, p2).take(20)),
        ))
    }

    private fun render(name: String, w: Int = 1280, h: Int = 820, content: @Composable () -> Unit): Boolean = try {
        ImageComposeScene(width = w, height = h, density = Density(1.25f)).use { scene ->
            scene.setContent(content)
            scene.render(0L)
            scene.render(16_000_000L)
            val img = scene.render(32_000_000L)
            val bytes = img.encodeToData(EncodedImageFormat.PNG)!!.bytes
            outDir.mkdirs()
            File(outDir, "$name.png").writeBytes(bytes)
            println("mockup: ${File(outDir, "$name.png").absolutePath} (${bytes.size} B)")
        }
        true
    } catch (t: Throwable) {
        println("mockup $name skipped: headless render failed — $t")
        false
    }

    @Test
    fun renderHomeCanvasPenSheetLightDark() {
        var any = false
        for (mode in listOf(ThemeMode.light, ThemeMode.dark)) {
            val tag = mode.name
            any = render("home-$tag") {
                PlatenTheme(mode = mode, uiDensity = 0.85f) {
                    HomeScreen(
                        notes = listOf(
                            NoteSummary("a", "강의 노트 — 열역학 3장", 1_761_300_000_000L, 12),
                            NoteSummary("b", "회의 2026-10", 1_761_200_000_000L, 3),
                            NoteSummary("c", "스케치", 1_761_100_000_000L, 7),
                            NoteSummary("d", "플래너(PDF)", 1_761_000_000_000L, 120),
                        ),
                        onOpen = {}, onNew = {}, onDelete = {}, onOpenFile = {}, onSettings = {}, onDiagnostics = {},
                    )
                }
            } || any

            val env = AppEnvironment.inMemory(desktop = true)
            val note = sampleNote()
            val prefs = UiPrefsDefaults.slots()
            val state = CanvasState(EditorController(note), Viewport(), env.brushes, prefs, 0, NoteAttachments())
            any = render("canvas-$tag") {
                PlatenTheme(mode = mode, uiDensity = 0.85f) {
                    CanvasScreen(env, state, AppSettings(theme = mode), DiagnosticsState(), mouseDraw = false, onBack = {}, onOpenSettings = {}, onOpenDiagnostics = {}, onOpenImport = {}, onSlotsChanged = { _, _ -> }, onToggleFingerDraw = {})
                }
            } || any

            any = render("pen-$tag") {
                PlatenTheme(mode = mode, uiDensity = 0.85f) {
                    Box(Modifier.fillMaxSize()) {
                        CanvasScreen(env, state, AppSettings(theme = mode), DiagnosticsState(), mouseDraw = false, onBack = {}, onOpenSettings = {}, onOpenDiagnostics = {}, onOpenImport = {}, onSlotsChanged = { _, _ -> }, onToggleFingerDraw = {})
                        Box(Modifier.fillMaxSize().background(PlatenTheme.colors.scrim))
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.7f).background(PlatenTheme.colors.chromeRaised, PlatenShapes.panel)) {
                            PenSheetContent(prefs[1], env.brushes, env.strokeRenderer, AppSettings().ink, onChange = {})
                        }
                    }
                }
            } || any
        }
        // 헤드리스 불가 환경에서는 소프트 통과(보고만). 하나라도 렌더됐으면 파일 존재를 확인한다.
        if (any) assertTrue(File(outDir, "home-light.png").exists())
    }
}

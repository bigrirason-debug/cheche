package io.github.bigrirason_debug.platen.ui.repo

import io.github.bigrirason_debug.platen.brush.ImageCodec
import io.github.bigrirason_debug.platen.ink.ResolvedInkParams
import io.github.bigrirason_debug.platen.ink.StrokeGeometry
import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import io.github.bigrirason_debug.platen.model.PlatenContainer
import io.github.bigrirason_debug.platen.pdf.GrayTip
import io.github.bigrirason_debug.platen.pdf.PdfExportOptions
import io.github.bigrirason_debug.platen.pdf.PdfExporter
import io.github.bigrirason_debug.platen.pdf.PdfImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * pdf 모듈 연결: PDF 가져오기(원본 바이트를 에셋으로 보존, 페이지 = PdfPage 배경 + 링크 객체)와
 * 벡터 획 PDF 내보내기. 획 파라미터는 화면과 같은 규칙(브러시 + 전역 설정 + 획 오버라이드)으로 해석한다.
 */
class ModulePdfFacade(private val brushes: BrushRepository, private val settings: SettingsStore) : PdfFacade {
    override val available = true

    /** 마지막 내보내기 경고(한글 텍스트 생략 등) — UI 가 토스트로 보여줄 수 있다. */
    @Volatile var lastWarnings: List<String> = emptyList(); private set

    override suspend fun export(note: Note, attachments: NoteAttachments): ByteArray? = withContext(Dispatchers.Default) {
        val ink = settings.settings.value.app.ink
        val fallback = brushes.brushes.value.firstOrNull() ?: BrushSpec(id = "fallback", name = "fallback")
        val result = PdfExporter.export(
            note = note,
            assetBytes = { id -> attachments.assets[id]?.second },
            geometry = StrokeGeometry.Default,
            resolve = { s -> ResolvedInkParams.resolve(brushes.get(s.brushId) ?: fallback, s.size, s.opacity, ink, s.overrides) },
            images = { spec -> brushes.tipPng(spec.id)?.let { png -> runCatching { ImageCodec.decodeToGray(png) }.getOrNull()?.let { GrayTip(it.width, it.height, it.alpha) } } },
            options = PdfExportOptions(title = note.title),
        )
        lastWarnings = result.warnings
        result.bytes
    }

    override suspend fun importPdf(bytes: ByteArray): Pair<List<Page>, NoteAttachments>? = withContext(Dispatchers.Default) {
        val assetId = PlatenContainer.sha1Hex(bytes).substring(0, 16)
        val pages = runCatching { PdfImporter.toPages(bytes, assetId) }.getOrNull() ?: return@withContext null
        pages to NoteAttachments(assets = mapOf(assetId to ("application/pdf" to bytes)))
    }
}

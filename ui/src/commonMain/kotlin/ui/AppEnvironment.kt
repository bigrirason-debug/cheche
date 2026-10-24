package io.github.bigrirason_debug.platen.ui

import io.github.bigrirason_debug.platen.model.BrushSpec
import io.github.bigrirason_debug.platen.render.BrushImageStore
import io.github.bigrirason_debug.platen.render.PageRenderer
import io.github.bigrirason_debug.platen.render.Renderers
import io.github.bigrirason_debug.platen.render.StrokeRenderer
import io.github.bigrirason_debug.platen.ui.repo.BrushRepository
import io.github.bigrirason_debug.platen.ui.repo.InMemoryBrushRepository
import io.github.bigrirason_debug.platen.ui.repo.InMemoryNoteRepository
import io.github.bigrirason_debug.platen.ui.repo.InMemorySettingsStore
import io.github.bigrirason_debug.platen.ui.repo.NoopPdfFacade
import io.github.bigrirason_debug.platen.ui.repo.NoopPlatformFiles
import io.github.bigrirason_debug.platen.ui.repo.NoopSyncFacade
import io.github.bigrirason_debug.platen.ui.repo.NoteRepository
import io.github.bigrirason_debug.platen.ui.repo.PdfFacade
import io.github.bigrirason_debug.platen.ui.repo.PlatformFiles
import io.github.bigrirason_debug.platen.ui.repo.SettingsStore
import io.github.bigrirason_debug.platen.ui.repo.SyncFacade

/** 앱 전역 의존성 묶음. 플랫폼 진입점(Main.kt / MainActivity)이 조립한다. */
class AppEnvironment(
    val notes: NoteRepository,
    val brushes: BrushRepository,
    val settings: SettingsStore,
    val sync: SyncFacade = NoopSyncFacade(),
    val pdf: PdfFacade = NoopPdfFacade(),
    val files: PlatformFiles = NoopPlatformFiles,
    val images: BrushImageStore = EmptyBrushImageStore,
    /** 데스크톱(마우스 전용)이면 true — 툴바 48dp·타깃 40epx·마우스 정책 */
    val desktop: Boolean = false,
) {
    val strokeRenderer: StrokeRenderer by lazy { Renderers.stroke(images) }
    val pageRenderer: PageRenderer by lazy { Renderers.page(strokeRenderer) }

    companion object {
        /** 메모리 전용 환경(목업·테스트). */
        fun inMemory(desktop: Boolean = true) = AppEnvironment(InMemoryNoteRepository(), InMemoryBrushRepository(), InMemorySettingsStore(), desktop = desktop)
    }
}

/** 팁/그레인 이미지 미공급(profile 엔진만) — brush 모듈 연결 전 자리. */
object EmptyBrushImageStore : BrushImageStore {
    override fun tip(spec: BrushSpec) = null
    override fun grain(spec: BrushSpec) = null
}

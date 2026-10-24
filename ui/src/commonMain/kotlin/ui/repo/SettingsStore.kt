package io.github.bigrirason_debug.platen.ui.repo

import io.github.bigrirason_debug.platen.model.AppSettings
import io.github.bigrirason_debug.platen.ui.state.PenSlot
import io.github.bigrirason_debug.platen.ui.state.UiPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** 디스크에 저장되는 설정 묶음(settings.json). */
@Serializable
data class StoredSettings(val app: AppSettings = AppSettings(), val ui: UiPrefs = UiPrefs())

interface SettingsStore {
    val settings: StateFlow<StoredSettings>
    fun update(f: (StoredSettings) -> StoredSettings)
}

class InMemorySettingsStore(initial: StoredSettings = StoredSettings()) : SettingsStore {
    override val settings = MutableStateFlow(initial)
    override fun update(f: (StoredSettings) -> StoredSettings) { settings.value = f(settings.value) }
}

/** 슬롯이 비어 있으면 내장 로스터로 채운다(첫 실행). */
fun UiPrefs.withDefaultSlots(): UiPrefs = if (slots.isNotEmpty()) this else copy(
    slots = BuiltinBrushes.defaultSlotBrushIds.map { id ->
        val spec = BuiltinBrushes.all.first { it.id == id }
        PenSlot(brushId = id, size = spec.size.base, color = if (spec.role == io.github.bigrirason_debug.platen.model.BrushRole.highlighter) io.github.bigrirason_debug.platen.ui.theme.PlatenPalette.YELLOW else io.github.bigrirason_debug.platen.ui.theme.PlatenPalette.BLACK)
    },
)

package io.github.bigrirason_debug.platen.ui.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** `<dir>/settings.json`. 쓰기는 동기(작은 파일) + 임시 파일 교체. */
class FileSettingsStore(private val file: File) : SettingsStore {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    override val settings: MutableStateFlow<StoredSettings> = MutableStateFlow(loadOrDefault())

    private fun loadOrDefault(): StoredSettings =
        runCatching { if (file.exists()) json.decodeFromString<StoredSettings>(file.readText()) else StoredSettings() }
            .getOrDefault(StoredSettings())
            .let { it.copy(ui = it.ui.withDefaultSlots()) }

    override fun update(f: (StoredSettings) -> StoredSettings) {
        val next = f(settings.value)
        settings.value = next
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(next))
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
    }
}

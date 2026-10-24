package io.github.bigrirason_debug.platen.ui.platform

import io.github.bigrirason_debug.platen.ui.repo.PickedFile
import io.github.bigrirason_debug.platen.ui.repo.PlatformFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** 데스크톱 노트 디렉터리 = `~/Platen` (설정은 `~/Platen/settings.json`). */
object PlatenDirs {
    val root: File get() = File(System.getProperty("user.home"), "Platen")
    val notes: File get() = File(root, "notes")
    val settings: File get() = File(root, "settings.json")
}

/** AWT FileDialog 기반 파일 선택/저장(메뉴 없음, 툴바에서 호출). Swing 디스패처에서 실행. */
class AwtPlatformFiles(private val owner: () -> Frame? = { null }) : PlatformFiles {
    override suspend fun pickFile(title: String, extensions: List<String>): PickedFile? = withContext(Dispatchers.Main) {
        val dialog = FileDialog(owner(), title, FileDialog.LOAD)
        if (extensions.isNotEmpty()) dialog.setFilenameFilter { _, name -> extensions.any { name.lowercase().endsWith(".$it") } }
        dialog.isVisible = true
        val dir = dialog.directory ?: return@withContext null
        val name = dialog.file ?: return@withContext null
        val f = File(dir, name)
        PickedFile(name, mimeFor(name), f.readBytes())
    }

    override suspend fun saveFile(title: String, suggestedName: String, mime: String, bytes: ByteArray): Boolean = withContext(Dispatchers.Main) {
        val dialog = FileDialog(owner(), title, FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true
        val dir = dialog.directory ?: return@withContext false
        val name = dialog.file ?: return@withContext false
        runCatching { File(dir, name).writeBytes(bytes) }.isSuccess
    }

    private fun mimeFor(name: String) = when (name.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"; "webp" -> "image/webp"; "pdf" -> "application/pdf"
        "platen" -> "application/vnd.platen.note+zip"; else -> "application/octet-stream"
    }
}

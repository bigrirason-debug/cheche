package io.github.bigrirason_debug.platen.ui.platform

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.github.bigrirason_debug.platen.ui.repo.PickedFile
import io.github.bigrirason_debug.platen.ui.repo.PlatformFiles
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SAF(OpenDocument / CreateDocument) 기반 파일 선택·저장. Activity 생성 시(onCreate 전/중) 등록해야 한다.
 * 확장자 필터는 SAF 가 MIME 기준이라 "*\/*" 로 열고 이름으로 거른다.
 */
class AndroidPlatformFiles(private val activity: ComponentActivity) : PlatformFiles {
    private var pickResult: CompletableDeferred<Uri?>? = null
    private var saveResult: CompletableDeferred<Uri?>? = null
    private var pendingSaveBytes: ByteArray? = null

    private val picker: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pickResult?.complete(uri) }
    private val saver: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> saveResult?.complete(uri) }

    override suspend fun pickFile(title: String, extensions: List<String>): PickedFile? {
        val d = CompletableDeferred<Uri?>()
        pickResult = d
        picker.launch(arrayOf("*/*"))
        val uri = d.await() ?: return null
        return withContext(Dispatchers.IO) {
            val name = queryName(uri) ?: "file"
            if (extensions.isNotEmpty() && extensions.none { name.lowercase().endsWith(".$it") }) return@withContext null
            val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            PickedFile(name, activity.contentResolver.getType(uri) ?: "application/octet-stream", bytes)
        }
    }

    override suspend fun saveFile(title: String, suggestedName: String, mime: String, bytes: ByteArray): Boolean {
        val d = CompletableDeferred<Uri?>()
        saveResult = d; pendingSaveBytes = bytes
        saver.launch(suggestedName)
        val uri = d.await() ?: return false
        return withContext(Dispatchers.IO) {
            runCatching { activity.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } != null }.getOrDefault(false)
        }
    }

    private fun queryName(uri: Uri): String? =
        activity.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}

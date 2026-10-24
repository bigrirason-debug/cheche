package io.github.bigrirason_debug.platen.ui.repo

import io.github.bigrirason_debug.platen.model.Note
import io.github.bigrirason_debug.platen.model.Page
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 동기화 상태(설정 화면 계정 섹션·더보기 시트 표시용). */
data class SyncState(
    val available: Boolean,
    val signedIn: Boolean = false,
    val accountLabel: String? = null,
    val lastSyncMillis: Long? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

/** Google 로그인 + Drive 동기화 파사드. 실 구현은 sync 모듈(head 연결). */
interface SyncFacade {
    val state: StateFlow<SyncState>
    suspend fun signIn()
    suspend fun signOut()
    suspend fun syncNow()
}

class NoopSyncFacade : SyncFacade {
    override val state = MutableStateFlow(SyncState(available = false, message = "동기화 미연결(sync 모듈 대기)"))
    override suspend fun signIn() {}
    override suspend fun signOut() {}
    override suspend fun syncNow() {}
}

/** PDF 가져오기/내보내기 파사드. 실 구현은 pdf 모듈(head 연결). */
interface PdfFacade {
    val available: Boolean
    /** 노트 → PDF 바이트(벡터 획). null = 미연결. */
    suspend fun export(note: Note, attachments: NoteAttachments): ByteArray?
    /** PDF 바이트 → 배경 페이지 목록 + 에셋. null = 미연결. */
    suspend fun importPdf(bytes: ByteArray): Pair<List<Page>, NoteAttachments>?
}

class NoopPdfFacade : PdfFacade {
    override val available = false
    override suspend fun export(note: Note, attachments: NoteAttachments): ByteArray? = null
    override suspend fun importPdf(bytes: ByteArray): Pair<List<Page>, NoteAttachments>? = null
}

/** 선택한 파일. */
data class PickedFile(val name: String, val mime: String, val bytes: ByteArray)

/** 플랫폼 파일 다이얼로그(데스크톱 AWT FileDialog / Android SAF). UI 는 콜백만 쓴다. */
interface PlatformFiles {
    suspend fun pickFile(title: String, extensions: List<String>): PickedFile?
    suspend fun saveFile(title: String, suggestedName: String, mime: String, bytes: ByteArray): Boolean
}

object NoopPlatformFiles : PlatformFiles {
    override suspend fun pickFile(title: String, extensions: List<String>): PickedFile? = null
    override suspend fun saveFile(title: String, suggestedName: String, mime: String, bytes: ByteArray): Boolean = false
}

package io.github.bigrirason_debug.platen.sync.drive

import kotlinx.serialization.Serializable

/** Drive `File` 리소스 중 Platen 이 쓰는 필드만(Discovery rev.20261020). `version` 은 int64 를 문자열로 준다. */
@Serializable
data class DriveFile(
    val id: String,
    val name: String? = null,
    val mimeType: String? = null,
    val md5Checksum: String? = null,
    val version: String? = null,
    val trashed: Boolean? = null,
    val parents: List<String>? = null,
    val appProperties: Map<String, String>? = null,
)

@Serializable
data class DriveFileList(val nextPageToken: String? = null, val files: List<DriveFile> = emptyList())

/** `Change` 리소스: `removed` = 삭제 또는 접근 상실, `file` = 삭제되지 않았을 때 최신 상태. */
@Serializable
data class DriveChange(val fileId: String? = null, val removed: Boolean = false, val file: DriveFile? = null)

@Serializable
data class DriveChangeList(
    val nextPageToken: String? = null,
    val newStartPageToken: String? = null,
    val changes: List<DriveChange> = emptyList(),
)

@Serializable
data class StartPageToken(val startPageToken: String)

@Serializable
data class DriveUser(val emailAddress: String? = null, val displayName: String? = null)

@Serializable
data class DriveAbout(val user: DriveUser? = null)

/** 업로드/생성 요청 메타데이터. */
@Serializable
data class DriveFileMetadata(
    val name: String? = null,
    val mimeType: String? = null,
    val parents: List<String>? = null,
    val appProperties: Map<String, String>? = null,
    val trashed: Boolean? = null,
)

@Serializable
data class DriveErrorBody(val error: DriveErrorDetail? = null)

@Serializable
data class DriveErrorDetail(val code: Int? = null, val message: String? = null, val errors: List<DriveErrorItem>? = null)

@Serializable
data class DriveErrorItem(val reason: String? = null, val message: String? = null)

/** HTTP 오류(재시도 소진 또는 비재시도 상태). */
class DriveException(val status: Int, val reason: String?, message: String) : Exception("Drive HTTP $status${reason?.let { " ($it)" } ?: ""}: $message")

/** 한 번의 증분 조회 결과(모든 페이지를 합친 것). */
data class ChangePage(val changes: List<DriveChange>, val newStartPageToken: String)

/** Platen 노트 파일의 MIME 타입(`.platen` zip 컨테이너). */
const val PLATEN_MIME = "application/vnd.platen.note+zip"

/** 루트 폴더 이름(sync-google §3.5). */
const val PLATEN_FOLDER_NAME = "Platen"

const val FOLDER_MIME = "application/vnd.google-apps.folder"

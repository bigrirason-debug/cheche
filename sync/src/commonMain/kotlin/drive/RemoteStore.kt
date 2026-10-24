package io.github.bigrirason_debug.platen.sync.drive

/**
 * 동기화 엔진이 원격에 요구하는 최소 연산. 구현: [DriveClient](실제 Drive), 테스트용 파일시스템 구현.
 * `md5Checksum`/`version` 의미는 Drive 와 같아야 한다(내용 해시 · 단조 증가 버전).
 */
interface RemoteStore {
    /** 루트 폴더 "Platen" 을 찾거나 만들고 id 를 돌려준다. */
    suspend fun ensureRootFolder(): String

    /** 메타데이터(version, md5Checksum, trashed …). 없으면 null. */
    suspend fun getFile(fileId: String): DriveFile?

    /** 내용 다운로드. */
    suspend fun download(fileId: String): ByteArray

    /** 새 파일 생성(multipart 또는 resumable). */
    suspend fun create(name: String, bytes: ByteArray, parentId: String, appProperties: Map<String, String>): DriveFile

    /** 내용 교체(multipart 또는 resumable). */
    suspend fun update(fileId: String, bytes: ByteArray, appProperties: Map<String, String>): DriveFile

    /** 휴지통으로 이동(`trashed=true`). */
    suspend fun trash(fileId: String)

    /** 폴더 안의 휴지통 제외 파일 전체 목록(첫 동기화용). */
    suspend fun listFolder(folderId: String): List<DriveFile>

    /** `changes.getStartPageToken`. */
    suspend fun startPageToken(): String

    /** `changes.list` 를 끝까지 따라가 모은 결과. */
    suspend fun listChanges(pageToken: String): ChangePage
}

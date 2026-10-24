package io.github.bigrirason_debug.platen.sync.drive

import io.github.bigrirason_debug.platen.sync.SyncConfig
import io.github.bigrirason_debug.platen.sync.auth.AccessTokenSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Google Drive API v3 의 Platen 이 쓰는 엔드포인트만 Ktor 로 직접 호출한다(sync-google §6 (b) 권장).
 *
 * - 401 → 토큰 갱신 후 1회 재시도.
 * - 429 / 403(rate limit 사유) / 5xx → 지수 백오프, 최대 [maxRetries]회(§4.6: "User rate limit exceeded → 지수 백오프").
 * - [SyncConfig.simpleUploadMaxBytes] 이하 multipart, 초과 resumable(세션 시작 → [SyncConfig.chunkBytes] 청크 PUT)(§4.5).
 * - 모든 본문은 ByteArray 라 재시도 시 재전송이 안전하다.
 *
 * @param sleeper 백오프 대기(테스트는 가짜 대기 주입).
 * @param random 백오프 지터·multipart 경계 생성(테스트는 시드 고정).
 */
class DriveClient(
    engine: HttpClientEngine,
    private val tokens: AccessTokenSource,
    private val config: SyncConfig,
    private val baseUrl: String = "https://www.googleapis.com",
    private val maxRetries: Int = 5,
    private val baseBackoffMillis: Long = 1_000L,
    private val random: Random = Random.Default,
    private val sleeper: suspend (Long) -> Unit = { delay(it) },
) : RemoteStore {
    private val http = HttpClient(engine) {
        expectSuccess = false
        followRedirects = false // resumable 308 을 리다이렉트로 오해하지 않도록
    }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false }

    /** `about.get(fields=user)` — 로그인 계정 확인용. */
    suspend fun about(): DriveAbout = execute {
        method = HttpMethod.Get
        url("$baseUrl/drive/v3/about")
        parameter("fields", "user")
    }.decode(DriveAbout.serializer())

    override suspend fun ensureRootFolder(): String {
        val q = "name = '$PLATEN_FOLDER_NAME' and mimeType = '$FOLDER_MIME' and 'root' in parents and trashed = false"
        val found = listFiles(q).firstOrNull()
        if (found != null) return found.id
        val meta = DriveFileMetadata(name = PLATEN_FOLDER_NAME, mimeType = FOLDER_MIME, parents = listOf("root"))
        return execute {
            method = HttpMethod.Post
            url("$baseUrl/drive/v3/files")
            parameter("fields", "id")
            setBody(TextContent(json.encodeToString(DriveFileMetadata.serializer(), meta), ContentType.Application.Json))
        }.decode(DriveFile.serializer()).id
    }

    override suspend fun getFile(fileId: String): DriveFile? {
        val r = executeOrNull404 {
            method = HttpMethod.Get
            url("$baseUrl/drive/v3/files/$fileId")
            parameter("fields", FILE_FIELDS)
        } ?: return null
        return r.decode(DriveFile.serializer())
    }

    override suspend fun download(fileId: String): ByteArray = execute {
        method = HttpMethod.Get
        url("$baseUrl/drive/v3/files/$fileId")
        parameter("alt", "media")
    }.body<ByteArray>()

    override suspend fun create(name: String, bytes: ByteArray, parentId: String, appProperties: Map<String, String>): DriveFile {
        val meta = DriveFileMetadata(name = name, mimeType = PLATEN_MIME, parents = listOf(parentId), appProperties = appProperties)
        return upload(fileId = null, meta = meta, bytes = bytes)
    }

    override suspend fun update(fileId: String, bytes: ByteArray, appProperties: Map<String, String>): DriveFile {
        val meta = DriveFileMetadata(appProperties = appProperties)
        return upload(fileId = fileId, meta = meta, bytes = bytes)
    }

    override suspend fun trash(fileId: String) {
        execute {
            method = HttpMethod.Patch
            url("$baseUrl/drive/v3/files/$fileId")
            parameter("fields", "id,trashed")
            setBody(TextContent(json.encodeToString(DriveFileMetadata.serializer(), DriveFileMetadata(trashed = true)), ContentType.Application.Json))
        }
    }

    override suspend fun listFolder(folderId: String): List<DriveFile> =
        listFiles("'$folderId' in parents and trashed = false")

    override suspend fun startPageToken(): String = execute {
        method = HttpMethod.Get
        url("$baseUrl/drive/v3/changes/startPageToken")
    }.decode(StartPageToken.serializer()).startPageToken

    override suspend fun listChanges(pageToken: String): ChangePage {
        val all = ArrayList<DriveChange>()
        var token = pageToken
        while (true) {
            val page = execute {
                method = HttpMethod.Get
                url("$baseUrl/drive/v3/changes")
                parameter("pageToken", token)
                parameter("pageSize", 1000)
                parameter("includeRemoved", true)
                parameter("spaces", "drive")
                parameter("fields", "nextPageToken,newStartPageToken,changes(fileId,removed,file($FILE_FIELDS))")
            }.decode(DriveChangeList.serializer())
            all += page.changes
            when {
                page.nextPageToken != null -> token = page.nextPageToken
                page.newStartPageToken != null -> return ChangePage(all, page.newStartPageToken)
                else -> return ChangePage(all, token)
            }
        }
    }

    // ---- 내부 ----

    private suspend fun listFiles(q: String): List<DriveFile> {
        val all = ArrayList<DriveFile>()
        var pageToken: String? = null
        do {
            val page = execute {
                method = HttpMethod.Get
                url("$baseUrl/drive/v3/files")
                parameter("q", q)
                parameter("spaces", "drive")
                parameter("pageSize", 1000)
                parameter("fields", "nextPageToken,files($FILE_FIELDS)")
                pageToken?.let { parameter("pageToken", it) }
            }.decode(DriveFileList.serializer())
            all += page.files
            pageToken = page.nextPageToken
        } while (pageToken != null)
        return all
    }

    private suspend fun upload(fileId: String?, meta: DriveFileMetadata, bytes: ByteArray): DriveFile =
        if (bytes.size.toLong() <= config.simpleUploadMaxBytes) uploadMultipart(fileId, meta, bytes)
        else uploadResumable(fileId, meta, bytes)

    private suspend fun uploadMultipart(fileId: String?, meta: DriveFileMetadata, bytes: ByteArray): DriveFile {
        val boundary = "platen_" + random.nextLong().toULong().toString(16)
        val body = multipartRelated(boundary, json.encodeToString(DriveFileMetadata.serializer(), meta), bytes)
        return execute {
            method = if (fileId == null) HttpMethod.Post else HttpMethod.Patch
            url(uploadUrl(fileId))
            parameter("uploadType", "multipart")
            parameter("fields", FILE_FIELDS)
            setBody(ByteArrayContent(body, ContentType.parse("multipart/related; boundary=$boundary")))
        }.decode(DriveFile.serializer())
    }

    private suspend fun uploadResumable(fileId: String?, meta: DriveFileMetadata, bytes: ByteArray): DriveFile {
        val session = execute {
            method = if (fileId == null) HttpMethod.Post else HttpMethod.Patch
            url(uploadUrl(fileId))
            parameter("uploadType", "resumable")
            parameter("fields", FILE_FIELDS)
            header("X-Upload-Content-Type", PLATEN_MIME)
            header("X-Upload-Content-Length", bytes.size.toString())
            setBody(TextContent(json.encodeToString(DriveFileMetadata.serializer(), meta), ContentType.Application.Json))
        }
        val location = session.headers[HttpHeaders.Location]
            ?: throw DriveException(session.status.value, null, "resumable 세션 응답에 Location 이 없음")
        val total = bytes.size
        var offset = 0
        while (true) {
            val end = minOf(offset + config.chunkBytes, total)
            val chunk = bytes.copyOfRange(offset, end)
            val r = execute {
                method = HttpMethod.Put
                url(location)
                header(HttpHeaders.ContentRange, "bytes $offset-${end - 1}/$total")
                setBody(ByteArrayContent(chunk, ContentType.parse(PLATEN_MIME)))
            }
            if (r.status.value == 308) {
                // Range: bytes=0-N → 서버가 받은 마지막 바이트. 없으면 우리가 보낸 만큼 전진.
                val range = r.headers["Range"]
                offset = range?.substringAfter('-', "")?.toIntOrNull()?.plus(1) ?: end
                if (offset >= total) throw DriveException(308, null, "서버가 전체 수신 후에도 308 을 반환")
            } else {
                return r.decode(DriveFile.serializer())
            }
        }
    }

    private fun uploadUrl(fileId: String?): String =
        if (fileId == null) "$baseUrl/upload/drive/v3/files" else "$baseUrl/upload/drive/v3/files/$fileId"

    private suspend fun executeOrNull404(block: HttpRequestBuilder.() -> Unit): HttpResponse? =
        try { execute(block) } catch (e: DriveException) { if (e.status == 404) null else throw e }

    /** 인증 헤더 부착 + 401 갱신 1회 + 429/403/5xx 지수 백오프. */
    private suspend fun execute(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        var refreshed = false
        var attempt = 0
        var token = tokens.accessToken()
        while (true) {
            val response = http.request {
                block()
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            val code = response.status.value
            if (code in 200..299 || code == 308) return response
            val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
            val reason = parseReason(bodyText)
            when {
                code == 401 && !refreshed -> {
                    refreshed = true
                    token = tokens.refreshAccessToken()
                }
                isRetryable(code, reason) && attempt < maxRetries -> {
                    sleeper(backoffMillis(attempt))
                    attempt++
                }
                else -> throw DriveException(code, reason, bodyText.take(500))
            }
        }
    }

    private fun isRetryable(code: Int, reason: String?): Boolean = when {
        code == 429 -> true
        code == 403 -> reason != null && RATE_LIMIT_REASONS.any { it.equals(reason, ignoreCase = true) }
        code in 500..599 -> true
        else -> false
    }

    /** 1s·2s·4s·8s·16s + 0~1s 지터(sync-google §4.6 지수 백오프). */
    private fun backoffMillis(attempt: Int): Long = baseBackoffMillis * (1L shl attempt) + random.nextLong(0L, 1_000L)

    private fun parseReason(body: String): String? = runCatching {
        json.decodeFromString(DriveErrorBody.serializer(), body).error?.errors?.firstOrNull()?.reason
    }.getOrNull()

    private suspend fun <T> HttpResponse.decode(serializer: kotlinx.serialization.KSerializer<T>): T =
        json.decodeFromString(serializer, bodyAsText())

    companion object {
        const val FILE_FIELDS = "id,name,mimeType,md5Checksum,version,trashed,parents,appProperties"
        private val RATE_LIMIT_REASONS = listOf("userRateLimitExceeded", "rateLimitExceeded", "dailyLimitExceeded", "sharingRateLimitExceeded")

        /** `multipart/related` 본문: JSON 메타 파트 + 미디어 파트. */
        fun multipartRelated(boundary: String, metadataJson: String, media: ByteArray, mediaType: String = PLATEN_MIME): ByteArray {
            val head = (
                "--$boundary\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                    metadataJson + "\r\n" +
                    "--$boundary\r\n" +
                    "Content-Type: $mediaType\r\n\r\n"
                ).encodeToByteArray()
            val tail = "\r\n--$boundary--".encodeToByteArray()
            return head + media + tail
        }
    }
}

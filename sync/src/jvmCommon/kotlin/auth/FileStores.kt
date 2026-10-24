package io.github.bigrirason_debug.platen.sync.auth

import io.github.bigrirason_debug.platen.sync.DeviceIdStore
import io.github.bigrirason_debug.platen.sync.engine.QueueStore
import io.github.bigrirason_debug.platen.sync.engine.SyncJob
import io.github.bigrirason_debug.platen.sync.engine.SyncMeta
import io.github.bigrirason_debug.platen.sync.engine.SyncMetaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID

private val fileJson = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

/** 소유자 전용(600) 으로 파일을 쓴다. POSIX 미지원(Windows)이면 Java File API 로 소유자 전용 권한을 흉내 낸다. */
internal fun writeOwnerOnly(file: File, text: String) {
    file.parentFile?.mkdirs()
    val tmp = File(file.parentFile, file.name + ".tmp")
    tmp.writeText(text)
    restrictToOwner(tmp)
    if (!tmp.renameTo(file)) {
        file.writeText(text)
        tmp.delete()
    }
    restrictToOwner(file)
}

internal fun restrictToOwner(file: File) {
    try {
        Files.setPosixFilePermissions(file.toPath(), setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
    } catch (_: UnsupportedOperationException) {
        file.setReadable(false, false); file.setReadable(true, true)
        file.setWritable(false, false); file.setWritable(true, true)
    } catch (_: Exception) {
        // 권한 설정 실패는 치명적이지 않다(예: 일부 네트워크 FS).
    }
}

/** 토큰 파일 포맷. `storage` 는 암호화 방식 표시("plain" = 평문). */
@Serializable
internal data class TokenFile(val storage: String = "plain", val tokens: OAuthTokens)

/**
 * JSON 파일 토큰 저장소(desktop: `~/.platen/tokens.json`, android: 앱 내부 파일). 권한 600, 내용에 `"storage":"plain"` 표시.
 *
 * TODO(보안): Windows DPAPI / macOS Keychain / Android EncryptedSharedPreferences 로 감싸기 — 의존성 미승인, 후속.
 */
class FileTokenStore(private val file: File) : TokenStore {
    override suspend fun load(): OAuthTokens? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        runCatching { fileJson.decodeFromString(TokenFile.serializer(), file.readText()).tokens }.getOrNull()
    }

    override suspend fun save(tokens: OAuthTokens) = withContext(Dispatchers.IO) {
        writeOwnerOnly(file, fileJson.encodeToString(TokenFile.serializer(), TokenFile(tokens = tokens)))
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        if (file.exists()) file.delete()
        Unit
    }
}

/** `device-id` 파일: 설치 시 1회 UUID 생성. */
class FileDeviceIdStore(private val file: File) : DeviceIdStore {
    override suspend fun loadOrCreate(): String = withContext(Dispatchers.IO) {
        val existing = if (file.isFile) file.readText().trim() else ""
        if (existing.isNotEmpty()) existing
        else UUID.randomUUID().toString().also { writeOwnerOnly(file, it) }
    }
}

private class JsonFileStore<T>(private val file: File, private val serializer: KSerializer<T>, private val default: () -> T) {
    suspend fun load(): T = withContext(Dispatchers.IO) {
        if (!file.isFile) default()
        else runCatching { fileJson.decodeFromString(serializer, file.readText()) }.getOrElse { default() }
    }

    suspend fun save(value: T) = withContext(Dispatchers.IO) { writeOwnerOnly(file, fileJson.encodeToString(serializer, value)) }
}

/** 큐 영속화 파일(`queue.json`). */
class FileQueueStore(file: File) : QueueStore {
    private val store = JsonFileStore(file, ListSerializer(SyncJob.serializer())) { emptyList() }
    override suspend fun load(): List<SyncJob> = store.load()
    override suspend fun save(jobs: List<SyncJob>) = store.save(jobs)
}

/** 동기화 커서 파일(`sync-meta.json`). */
class FileSyncMetaStore(file: File) : SyncMetaStore {
    private val store = JsonFileStore(file, SyncMeta.serializer()) { SyncMeta() }
    override suspend fun load(): SyncMeta = store.load()
    override suspend fun save(meta: SyncMeta) = store.save(meta)
}

/** 누락 시 사용자 안내가 담긴 예외. */
class OAuthClientConfigMissingException(message: String) : Exception(message)

/**
 * Google 콘솔에서 받은 client_secret JSON(`{"installed":{"client_id":…,"client_secret":…}}`) 을 파싱한다.
 * `"web"` 키도 허용(웹 클라이언트 ID 를 Credential Manager 용으로 둘 때).
 */
fun OAuthClientConfig.Companion.parseClientSecretJson(text: String): OAuthClientConfig {
    val root = fileJson.parseToJsonElement(text).jsonObject
    val node: JsonObject = (root["installed"] ?: root["web"])?.jsonObject
        ?: root.takeIf { it.containsKey("client_id") }
        ?: throw OAuthClientConfigMissingException("oauth-client.json 에 \"installed\" 항목이 없습니다. Google Cloud 콘솔 → 사용자 인증 정보 → 데스크톱 앱 클라이언트의 JSON 을 그대로 저장하세요.")
    val clientId = node["client_id"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        ?: throw OAuthClientConfigMissingException("oauth-client.json 에 client_id 가 없습니다.")
    val secret = node["client_secret"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
    return OAuthClientConfig(clientId = clientId, clientSecret = secret)
}

/** 파일에서 클라이언트 설정을 읽는다. 없으면 어디에 무엇을 둘지 안내하는 예외. */
fun OAuthClientConfig.Companion.loadFromFile(file: File): OAuthClientConfig {
    if (!file.isFile) throw OAuthClientConfigMissingException(
        "Google OAuth 클라이언트 파일이 없습니다: ${file.absolutePath}\n" +
            "Google Cloud 콘솔(API 및 서비스 → 사용자 인증 정보)에서 '데스크톱 앱' OAuth 클라이언트를 만들고 " +
            "다운로드한 client_secret_*.json 을 위 경로에 저장한 뒤 다시 로그인하세요(sync-google §8.2 L3/L5).",
    )
    return parseClientSecretJson(file.readText())
}

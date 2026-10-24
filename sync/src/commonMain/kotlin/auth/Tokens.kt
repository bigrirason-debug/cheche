package io.github.bigrirason_debug.platen.sync.auth

import kotlinx.serialization.Serializable

/** Platen 이 요청하는 OAuth 범위(sync-google §3.5: `drive.file` + openid email profile, 전부 비민감). */
object OAuthScopes {
    const val DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    val DEFAULT: List<String> = listOf(DRIVE_FILE, "openid", "email", "profile")
}

/**
 * OAuth 토큰 묶음. `toString()` 은 토큰 값을 마스킹한다(로그 유출 방지).
 * @param expiresAtMillis 액세스 토큰 만료 시각(epoch millis).
 */
@Serializable
data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtMillis: Long,
    val scopes: List<String> = emptyList(),
    val idToken: String? = null,
) {
    /** 만료 여부(기본 60초 여유). */
    fun isExpired(nowMillis: Long, skewMillis: Long = 60_000L): Boolean = nowMillis + skewMillis >= expiresAtMillis

    override fun toString(): String =
        "OAuthTokens(accessToken=${mask(accessToken)}, refreshToken=${refreshToken?.let(::mask)}, " +
            "expiresAtMillis=$expiresAtMillis, scopes=$scopes, idToken=${idToken?.let(::mask)})"

    private companion object {
        fun mask(s: String): String = if (s.length <= 8) "***" else s.take(4) + "…" + s.takeLast(2) + "(${s.length})"
    }
}

/** 토큰 영속화. desktop: `~/.platen/tokens.json`(600), android: 앱 내부 파일. */
interface TokenStore {
    suspend fun load(): OAuthTokens?
    suspend fun save(tokens: OAuthTokens)
    suspend fun clear()
}

/** 플랫폼별 로그인/갱신/로그아웃. */
interface GoogleAuth {
    /** 대화형 로그인(브라우저/Credential Manager). */
    suspend fun signIn(): OAuthTokens
    /** 리프레시 토큰으로 액세스 토큰 갱신. 응답에 refresh_token 이 없으면 기존 값을 유지한다. */
    suspend fun refresh(tokens: OAuthTokens): OAuthTokens
    suspend fun signOut()
}

/** 선택 구현: 로그아웃 시 원격 토큰 폐기. */
interface TokenRevoker {
    suspend fun revoke(tokens: OAuthTokens)
}

/**
 * OAuth 클라이언트 식별자(리포에 넣지 않음, sync-google §8.2 L5).
 * 데스크톱 앱 클라이언트는 `client_secret` 값이 필요하지만 기밀로 취급되지 않는다(§1.2).
 */
data class OAuthClientConfig(
    val clientId: String,
    val clientSecret: String? = null,
    val authEndpoint: String = "https://accounts.google.com/o/oauth2/v2/auth",
    val tokenEndpoint: String = "https://oauth2.googleapis.com/token",
    val revokeEndpoint: String = "https://oauth2.googleapis.com/revoke",
) {
    override fun toString(): String = "OAuthClientConfig(clientId=$clientId, clientSecret=${if (clientSecret == null) "null" else "***"})"

    companion object
}

/** 로그인되지 않았거나 토큰을 복구할 수 없을 때. */
class NotSignedInException(message: String = "Google 계정에 로그인되어 있지 않습니다.") : Exception(message)

/** 인가 흐름 실패(state 불일치, 사용자 거부, 토큰 교환 오류). */
class OAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** 메모리 토큰 저장소(테스트·일회성). */
class InMemoryTokenStore(initial: OAuthTokens? = null) : TokenStore {
    private var tokens: OAuthTokens? = initial
    override suspend fun load(): OAuthTokens? = tokens
    override suspend fun save(tokens: OAuthTokens) { this.tokens = tokens }
    override suspend fun clear() { tokens = null }
}

package io.github.bigrirason_debug.platen.sync.auth

import io.github.bigrirason_debug.platen.sync.SyncClock
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `https://oauth2.googleapis.com/token` 응답(필요 필드만). */
@Serializable
data class TokenResponse(
    val access_token: String,
    val expires_in: Long = 3600,
    val refresh_token: String? = null,
    val scope: String? = null,
    val token_type: String? = null,
    val id_token: String? = null,
)

/**
 * 설치형 앱(authorization code + PKCE) 토큰 교환/갱신을 Ktor 로 직접 호출한다(sync-google §1.2).
 * google-auth-library 대신 직접 호출: 필요한 요청이 2개뿐이고 MockEngine 으로 결정적 테스트가 가능하다.
 */
class OAuthTokenClient(
    engine: HttpClientEngine,
    private val config: OAuthClientConfig,
    private val clock: SyncClock,
) {
    private val http = HttpClient(engine) { expectSuccess = false }
    private val json = Json { ignoreUnknownKeys = true }

    /** 인가 URL(브라우저로 열 주소). `access_type=offline` 로 리프레시 토큰을 요청한다. */
    fun authorizationUrl(redirectUri: String, scopes: List<String>, state: String, codeChallenge: String): String =
        URLBuilder(config.authEndpoint).apply {
            parameters.append("client_id", config.clientId)
            parameters.append("redirect_uri", redirectUri)
            parameters.append("response_type", "code")
            parameters.append("scope", scopes.joinToString(" "))
            parameters.append("state", state)
            parameters.append("code_challenge", codeChallenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("access_type", "offline")
            parameters.append("prompt", "consent")
        }.buildString()

    /** 인가 코드 → 토큰. */
    suspend fun exchangeCode(code: String, codeVerifier: String, redirectUri: String, requestedScopes: List<String>): OAuthTokens {
        val params = Parameters.build {
            append("grant_type", "authorization_code")
            append("code", code)
            append("code_verifier", codeVerifier)
            append("redirect_uri", redirectUri)
            append("client_id", config.clientId)
            config.clientSecret?.let { append("client_secret", it) }
        }
        return toTokens(post(params), previous = null, requestedScopes = requestedScopes)
    }

    /** 리프레시 토큰으로 갱신. 응답에 refresh_token 이 없으면 기존 값을 유지한다. */
    suspend fun refresh(previous: OAuthTokens): OAuthTokens {
        val refreshToken = previous.refreshToken ?: throw NotSignedInException("리프레시 토큰이 없어 재로그인이 필요합니다.")
        val params = Parameters.build {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
            append("client_id", config.clientId)
            config.clientSecret?.let { append("client_secret", it) }
        }
        return toTokens(post(params), previous = previous, requestedScopes = previous.scopes)
    }

    /** 토큰 폐기(실패해도 로컬 로그아웃은 진행하므로 예외를 던지지 않는다). */
    suspend fun revoke(token: String): Boolean = runCatching {
        http.submitForm(config.revokeEndpoint, Parameters.build { append("token", token) }).status.isSuccess()
    }.getOrDefault(false)

    private suspend fun post(params: Parameters): TokenResponse {
        val response = http.submitForm(config.tokenEndpoint, params)
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw OAuthException("토큰 엔드포인트 오류 ${response.status.value}: ${body.take(300)}")
        return try {
            json.decodeFromString(TokenResponse.serializer(), body)
        } catch (e: Exception) {
            throw OAuthException("토큰 응답 파싱 실패", e)
        }
    }

    private fun toTokens(r: TokenResponse, previous: OAuthTokens?, requestedScopes: List<String>): OAuthTokens = OAuthTokens(
        accessToken = r.access_token,
        refreshToken = r.refresh_token ?: previous?.refreshToken,
        expiresAtMillis = clock.nowMillis() + r.expires_in * 1000L,
        scopes = r.scope?.split(' ')?.filter { it.isNotBlank() } ?: requestedScopes,
        idToken = r.id_token ?: previous?.idToken,
    )
}

package io.github.bigrirason_debug.platen.sync.auth

import io.github.bigrirason_debug.platen.sync.SyncClock
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.ContentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** 루프백 콜백 쿼리 해석(순수 함수, 테스트 가능). */
object LoopbackCallback {
    /** 기본 콜백 경로(.NET `LocalServerCodeReceiver` 와 동일, sync-google §1.2). */
    const val PATH = "/authorize/"

    /** `code` 를 돌려주거나, state 불일치·오류·누락이면 실패. */
    fun parse(param: (String) -> String?, expectedState: String): Result<String> {
        param("error")?.let { return Result.failure(OAuthException("인가 거부/오류: $it")) }
        val state = param("state")
        if (state == null || state != expectedState) return Result.failure(OAuthException("state 불일치 — 콜백을 신뢰할 수 없습니다."))
        val code = param("code")?.takeIf { it.isNotBlank() } ?: return Result.failure(OAuthException("콜백에 code 가 없습니다."))
        return Result.success(code)
    }
}

/**
 * 데스크톱 설치형 앱 OAuth(sync-google §1.2): `http://127.0.0.1:{임의 포트}/authorize/` 루프백 1회성 서버(ktor-server-cio)
 * + PKCE S256 + state 검증. 토큰 교환/갱신은 [OAuthTokenClient](Ktor 직접 호출). google-auth-library 는 쓰지 않는다.
 *
 * @param openBrowser 인가 URL 을 시스템 브라우저로 여는 콜백(호출자가 `java.awt.Desktop` 등으로 구현).
 * @param timeoutMillis 사용자가 브라우저에서 동의를 마칠 때까지 기다리는 상한(기본 5분).
 */
class DesktopLoopbackAuth(
    config: OAuthClientConfig,
    engine: HttpClientEngine,
    clock: SyncClock,
    private val openBrowser: (String) -> Unit,
    private val scopes: List<String> = OAuthScopes.DEFAULT,
    private val timeoutMillis: Long = 5 * 60_000L,
    private val random: Random = SecureRandom().asKotlinRandom(),
) : GoogleAuth, TokenRevoker {
    private val tokenClient = OAuthTokenClient(engine, config, clock)

    override suspend fun signIn(): OAuthTokens {
        val verifier = Pkce.generateVerifier(random = random)
        val challenge = Pkce.challenge(verifier)
        val state = Pkce.generateState(random)
        val codeResult = CompletableDeferred<String>()

        val server = embeddedServer(CIO, host = "127.0.0.1", port = 0) {
            routing {
                get(LoopbackCallback.PATH) {
                    val r = LoopbackCallback.parse({ call.request.queryParameters[it] }, state)
                    r.fold(
                        onSuccess = {
                            call.respondText(SUCCESS_HTML, ContentType.Text.Html)
                            codeResult.complete(it)
                        },
                        onFailure = {
                            call.respondText("<html><body><h3>Platen 로그인 실패</h3><p>${it.message}</p></body></html>", ContentType.Text.Html)
                            codeResult.completeExceptionally(it)
                        },
                    )
                }
            }
        }
        server.start(wait = false)
        try {
            val port = server.engine.resolvedConnectors().first().port
            val redirectUri = "http://127.0.0.1:$port${LoopbackCallback.PATH}"
            openBrowser(tokenClient.authorizationUrl(redirectUri, scopes, state, challenge))
            val code = withTimeoutOrNull(timeoutMillis) { codeResult.await() }
                ?: throw OAuthException("브라우저 로그인 대기 시간 초과(${timeoutMillis / 1000}s)")
            return tokenClient.exchangeCode(code, verifier, redirectUri, scopes)
        } finally {
            server.stop(gracePeriodMillis = 100, timeoutMillis = 1_000)
        }
    }

    override suspend fun refresh(tokens: OAuthTokens): OAuthTokens = tokenClient.refresh(tokens)

    /** 로컬 상태는 [TokenManager] 가 지운다. 원격 폐기는 [revoke]. */
    override suspend fun signOut() = Unit

    override suspend fun revoke(tokens: OAuthTokens) {
        tokenClient.revoke(tokens.refreshToken ?: tokens.accessToken)
    }

    private companion object {
        const val SUCCESS_HTML = "<html><body><h3>Platen 로그인 완료</h3><p>이 탭을 닫고 앱으로 돌아가세요.</p></body></html>"
    }
}

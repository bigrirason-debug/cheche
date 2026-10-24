package io.github.bigrirason_debug.platen.sync

import io.github.bigrirason_debug.platen.sync.auth.DesktopLoopbackAuth
import io.github.bigrirason_debug.platen.sync.auth.LoopbackCallback
import io.github.bigrirason_debug.platen.sync.auth.OAuthClientConfig
import io.github.bigrirason_debug.platen.sync.auth.OAuthException
import io.github.bigrirason_debug.platen.sync.auth.OAuthTokenClient
import io.github.bigrirason_debug.platen.sync.auth.OAuthTokens
import io.github.bigrirason_debug.platen.sync.auth.Pkce
import io.github.bigrirason_debug.platen.sync.auth.parseClientSecretJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.parseUrlEncodedParameters
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PkceAndOAuthTest {
    private val config = OAuthClientConfig(clientId = "cid.apps.googleusercontent.com", clientSecret = "sec")
    private val clock = SyncClock { 1_700_000_000_000L }

    @Test
    fun verifierLengthAndCharset() {
        val r = Random(42)
        for (len in listOf(43, 64, 128)) {
            val v = Pkce.generateVerifier(len, r)
            assertEquals(len, v.length)
            assertTrue(v.all { it.isLetterOrDigit() || it in "-._~" }, v)
        }
        val a = Pkce.generateVerifier(random = Random(1))
        val b = Pkce.generateVerifier(random = Random(1))
        assertEquals(a, b, "seeded → deterministic")
    }

    @Test
    fun challengeMatchesRfc7636AppendixB() {
        // RFC 7636 Appendix B 의 공개 테스트 벡터.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
        assertFalse(Pkce.challenge("x".repeat(43)).contains('='), "no padding")
    }

    @Test
    fun callbackRejectsStateMismatchAndErrors() {
        val ok = LoopbackCallback.parse({ mapOf("code" to "c1", "state" to "s1")[it] }, "s1")
        assertEquals("c1", ok.getOrThrow())
        val bad = LoopbackCallback.parse({ mapOf("code" to "c1", "state" to "other")[it] }, "s1")
        assertTrue(bad.isFailure && bad.exceptionOrNull() is OAuthException)
        val err = LoopbackCallback.parse({ mapOf("error" to "access_denied", "state" to "s1")[it] }, "s1")
        assertTrue(err.isFailure)
        val noCode = LoopbackCallback.parse({ mapOf("state" to "s1")[it] }, "s1")
        assertTrue(noCode.isFailure)
    }

    @Test
    fun exchangeParsesTokensAndComputesExpiry() = runTest {
        var form: Map<String, String> = emptyMap()
        val engine = MockEngine { req ->
            form = String(req.body.toByteArray()).parseUrlEncodedParameters().entries().associate { it.key to it.value.first() }
            respond(
                """{"access_token":"at1","expires_in":3600,"refresh_token":"rt1","scope":"openid email","token_type":"Bearer"}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = OAuthTokenClient(engine, config, clock)
        val t = client.exchangeCode("code1", "verifier1", "http://127.0.0.1:1234/authorize/", listOf("a"))
        assertEquals("at1", t.accessToken)
        assertEquals("rt1", t.refreshToken)
        assertEquals(1_700_000_000_000L + 3600_000L, t.expiresAtMillis)
        assertEquals(listOf("openid", "email"), t.scopes)
        assertEquals("authorization_code", form["grant_type"])
        assertEquals("verifier1", form["code_verifier"])
        assertEquals("sec", form["client_secret"])
        assertTrue(t.isExpired(t.expiresAtMillis - 30_000L))
        assertFalse(t.isExpired(t.expiresAtMillis - 120_000L))
    }

    @Test
    fun refreshKeepsRefreshTokenWhenAbsent() = runTest {
        val engine = MockEngine {
            respond("""{"access_token":"at2","expires_in":1800}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = OAuthTokenClient(engine, config, clock)
        val prev = OAuthTokens("at1", "rt-keep", 0L, listOf("s"))
        val t = client.refresh(prev)
        assertEquals("at2", t.accessToken)
        assertEquals("rt-keep", t.refreshToken)
        assertEquals(listOf("s"), t.scopes)
        assertEquals(1_700_000_000_000L + 1_800_000L, t.expiresAtMillis)
    }

    @Test
    fun authorizationUrlCarriesPkceAndScopes() {
        val client = OAuthTokenClient(MockEngine { respond("") }, config, clock)
        val url = Url(client.authorizationUrl("http://127.0.0.1:5/authorize/", listOf("a", "b"), "st", "ch"))
        assertEquals("S256", url.parameters["code_challenge_method"])
        assertEquals("ch", url.parameters["code_challenge"])
        assertEquals("st", url.parameters["state"])
        assertEquals("a b", url.parameters["scope"])
        assertEquals("offline", url.parameters["access_type"])
    }

    @Test
    fun tokensToStringMasksSecrets() {
        val s = OAuthTokens("ya29.secret-access-token-value", "1//refresh-token-value", 1L).toString()
        assertFalse(s.contains("secret-access"))
        assertFalse(s.contains("refresh-token-value"))
    }

    @Test
    fun parsesGoogleClientSecretJson() {
        val c = OAuthClientConfig.parseClientSecretJson("""{"installed":{"client_id":"abc","client_secret":"xyz","redirect_uris":["http://localhost"]}}""")
        assertEquals("abc", c.clientId)
        assertEquals("xyz", c.clientSecret)
        assertFalse(c.toString().contains("xyz"))
    }

    /** 실제 루프백 서버(127.0.0.1 임의 포트) + 가짜 브라우저 + MockEngine 토큰 교환. */
    @Test
    fun loopbackSignInEndToEnd() = runBlocking {
        var exchangedForm: Map<String, String> = emptyMap()
        val tokenEngine = MockEngine { req ->
            exchangedForm = String(req.body.toByteArray()).parseUrlEncodedParameters().entries().associate { it.key to it.value.first() }
            respond("""{"access_token":"at","expires_in":3600,"refresh_token":"rt"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val opened = CompletableDeferred<String>()
        val auth = DesktopLoopbackAuth(config, tokenEngine, clock, openBrowser = { opened.complete(it) }, timeoutMillis = 20_000, random = Random(7))
        val browser = launch(Dispatchers.IO) {
            val authUrl = Url(opened.await())
            val redirect = authUrl.parameters["redirect_uri"]!!
            val state = authUrl.parameters["state"]!!
            assertEquals("S256", authUrl.parameters["code_challenge_method"])
            HttpClient(CIO).use { it.get("$redirect?code=the-code&state=$state") }
        }
        val tokens = withTimeout(20_000) { auth.signIn() }
        browser.join()
        assertEquals("at", tokens.accessToken)
        assertEquals("the-code", exchangedForm["code"])
        assertNotNull(exchangedForm["code_verifier"])
        assertTrue(Url(opened.await()).parameters["redirect_uri"]!!.startsWith("http://127.0.0.1:"))
    }
}

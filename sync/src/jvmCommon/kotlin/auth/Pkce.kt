package io.github.bigrirason_debug.platen.sync.auth

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * PKCE(RFC 7636) S256 + state 생성. verifier 는 unreserved 문자 43~128자, challenge = base64url(sha256(verifier)) 패딩 없음.
 */
object Pkce {
    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private val secure: Random by lazy { SecureRandom().asKotlinRandom() }
    private val b64url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    /** code_verifier(기본 64자). */
    fun generateVerifier(length: Int = 64, random: Random = secure): String {
        require(length in 43..128) { "verifier length must be 43..128" }
        return buildString(length) { repeat(length) { append(UNRESERVED[random.nextInt(UNRESERVED.length)]) } }
    }

    /** code_challenge = BASE64URL(SHA256(ASCII(verifier))). */
    fun challenge(verifier: String): String =
        b64url.encode(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    /** CSRF 방지용 state(128비트). */
    fun generateState(random: Random = secure): String = b64url.encode(random.nextBytes(16))
}

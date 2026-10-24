package io.github.bigrirason_debug.platen.sync.auth

import io.github.bigrirason_debug.platen.sync.SyncClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** DriveClient 가 요구하는 최소 인터페이스: 현재 액세스 토큰과 강제 갱신. */
interface AccessTokenSource {
    /** 유효한 액세스 토큰(만료 임박 시 갱신). */
    suspend fun accessToken(): String
    /** 401 수신 후 호출: 캐시를 버리고 갱신한 토큰. */
    suspend fun refreshAccessToken(): String
}

/** GoogleAuth + TokenStore 를 묶어 토큰 수명을 관리한다. 갱신은 Mutex 로 직렬화. */
class TokenManager(
    private val auth: GoogleAuth,
    private val store: TokenStore,
    private val clock: SyncClock,
    private val skewMillis: Long = 60_000L,
) : AccessTokenSource {
    private val lock = Mutex()

    suspend fun isSignedIn(): Boolean = store.load() != null

    suspend fun signIn(): OAuthTokens = lock.withLock {
        auth.signIn().also { store.save(it) }
    }

    suspend fun signOut() = lock.withLock {
        try {
            store.load()?.let { (auth as? TokenRevoker)?.revoke(it) }
            auth.signOut()
        } finally {
            store.clear()
        }
    }

    override suspend fun accessToken(): String = lock.withLock {
        val t = store.load() ?: throw NotSignedInException()
        if (!t.isExpired(clock.nowMillis(), skewMillis)) t.accessToken else refreshLocked(t).accessToken
    }

    override suspend fun refreshAccessToken(): String = lock.withLock {
        val t = store.load() ?: throw NotSignedInException()
        refreshLocked(t).accessToken
    }

    private suspend fun refreshLocked(t: OAuthTokens): OAuthTokens = auth.refresh(t).also { store.save(it) }
}

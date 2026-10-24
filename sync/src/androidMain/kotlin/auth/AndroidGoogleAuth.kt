package io.github.bigrirason_debug.platen.sync.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.bigrirason_debug.platen.sync.SyncClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android 인증·인가(sync-google §1.1, D-023):
 * 1. **인증** — Credential Manager + `GetGoogleIdOption(serverClientId = 웹 클라이언트 ID)` → ID 토큰.
 * 2. **인가** — `Identity.getAuthorizationClient(context).authorize(scopes=[drive.file])` → 액세스 토큰(play-services-auth 21.4.0).
 *    사용자 동의가 필요하면(`hasResolution()`) [onResolutionRequired] 로 `IntentSender` 를 호출자(Activity)에 넘기고,
 *    호출자가 Activity 결과를 [completeResolution] 으로 돌려주면 이어서 토큰을 만든다.
 *
 * 리프레시 토큰은 Play 서비스가 보관하므로 앱은 갖지 않는다 → [refresh] 는 `authorize` 를 다시 호출한다(보통 UI 없이 즉시 반환).
 *
 * 이 컨테이너에서는 androidMain 이 컴파일되지 않는다. 사용한 API 는 play-services-auth 21.x 공개 레퍼런스 기준:
 * `Identity.getAuthorizationClient(Context)`, `AuthorizationRequest.builder().setRequestedScopes(List<Scope>).build()`,
 * `AuthorizationClient.authorize(AuthorizationRequest): Task<AuthorizationResult>`, `AuthorizationResult.hasResolution()/getPendingIntent()/getAccessToken()`,
 * `AuthorizationClient.getAuthorizationResultFromIntent(Intent)`. [불확실] `getAccessToken()` 의 만료 시각은 결과에 없음 → 55분으로 가정.
 *
 * @param context 시트를 띄울 수 있는 Activity 컨텍스트.
 * @param serverClientId Google 콘솔의 **웹 애플리케이션** 클라이언트 ID(§1.1).
 * @param onResolutionRequired 동의 UI 가 필요할 때 호출. 호출자는 `startIntentSenderForResult`/`ActivityResultContracts.StartIntentSenderForResult` 로 띄운 뒤 결과를 [completeResolution] 로 전달.
 */
class AndroidGoogleAuth(
    private val context: Context,
    private val serverClientId: String,
    private val clock: SyncClock,
    private val onResolutionRequired: (IntentSender) -> Unit,
    private val scopes: List<String> = listOf(OAuthScopes.DRIVE_FILE),
) : GoogleAuth {
    private var pendingResolution: CompletableDeferred<Intent?>? = null
    private var lastIdToken: String? = null

    override suspend fun signIn(): OAuthTokens {
        lastIdToken = fetchIdToken()
        return authorize()
    }

    /** 리프레시 토큰이 없으므로 `authorize` 재호출(이미 동의했으면 UI 없이 새 액세스 토큰). */
    override suspend fun refresh(tokens: OAuthTokens): OAuthTokens = authorize().copy(idToken = tokens.idToken ?: lastIdToken)

    override suspend fun signOut() {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        lastIdToken = null
    }

    /**
     * 호출자가 동의 Activity 결과를 돌려준다. [data] 가 null 이면 취소로 처리.
     * 대기 중인 [signIn]/[refresh] 가 이어서 진행된다.
     */
    fun completeResolution(data: Intent?) {
        pendingResolution?.complete(data)
    }

    /** Credential Manager 로 Google ID 토큰을 받는다. */
    suspend fun fetchIdToken(): String {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw OAuthException("예상치 못한 자격 증명 유형: ${credential.type}")
    }

    /** AuthorizationClient 로 drive.file 액세스 토큰을 받는다(필요 시 동의 UI 왕복). */
    private suspend fun authorize(): OAuthTokens {
        val client = Identity.getAuthorizationClient(context)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes.map { Scope(it) })
            .build()
        var result: AuthorizationResult = client.authorize(request).await()
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent ?: throw OAuthException("동의가 필요하지만 PendingIntent 가 없습니다.")
            val deferred = CompletableDeferred<Intent?>()
            pendingResolution = deferred
            try {
                onResolutionRequired(pendingIntent.intentSender)
                val data = deferred.await() ?: throw OAuthException("사용자가 Drive 권한 동의를 취소했습니다.")
                result = client.getAuthorizationResultFromIntent(data)
            } finally {
                pendingResolution = null
            }
        }
        val accessToken = result.accessToken?.takeIf { it.isNotBlank() }
            ?: throw OAuthException("AuthorizationClient 가 액세스 토큰을 돌려주지 않았습니다.")
        return OAuthTokens(
            accessToken = accessToken,
            refreshToken = null, // Play 서비스가 보관. 만료 시 authorize 재호출.
            expiresAtMillis = clock.nowMillis() + ASSUMED_LIFETIME_MILLIS,
            scopes = scopes,
            idToken = lastIdToken,
        )
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

    private companion object {
        /** Google 액세스 토큰 수명은 보통 3600s. 결과에 만료가 없으므로 55분으로 보수 가정 [불확실]. */
        const val ASSUMED_LIFETIME_MILLIS = 55 * 60_000L
    }
}

/**
 * Android 토큰 저장소: 앱 내부 파일(`filesDir/platen/tokens.json`).
 * EncryptedSharedPreferences 는 의존성 미승인, 후속. (리프레시 토큰은 저장되지 않으므로 노출 범위는 1시간짜리 액세스 토큰뿐.)
 */
fun androidTokenStore(context: Context): TokenStore = FileTokenStore(File(File(context.filesDir, "platen"), "tokens.json"))

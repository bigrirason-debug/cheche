package io.github.bigrirason_debug.platen.sync.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.io.File

/**
 * Android 인증(sync-google §1.1): Credential Manager + `GetGoogleIdOption(serverClientId = 웹 클라이언트 ID)` 로 ID 토큰.
 *
 * TODO(의존성 승인 필요): Drive 범위 액세스 토큰은 `Identity.getAuthorizationClient()`(com.google.android.gms:play-services-auth)
 * 가 발급한다. 카탈로그에 없어 이번 구현은 ID 토큰까지만 받고 명확한 예외를 던진다.
 *
 * @param context 사용자에게 시트를 띄울 수 있는 Activity 컨텍스트.
 */
class AndroidGoogleAuth(
    private val context: Context,
    private val serverClientId: String,
) : GoogleAuth {
    override suspend fun signIn(): OAuthTokens {
        val idToken = fetchIdToken()
        throw UnsupportedOperationException(
            "Drive 액세스 토큰 발급 불가: AuthorizationClient(play-services-auth) 의존성 승인 필요. " +
                "Credential Manager 인증은 성공(ID 토큰 ${idToken.length}자).",
        )
    }

    override suspend fun refresh(tokens: OAuthTokens): OAuthTokens =
        throw UnsupportedOperationException("AuthorizationClient 의존성 승인 필요(토큰 갱신은 Play 서비스가 담당).")

    override suspend fun signOut() {
        CredentialManager.create(context).clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
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
}

/**
 * Android 토큰 저장소: 앱 내부 파일(`filesDir/platen/tokens.json`).
 * EncryptedSharedPreferences 는 의존성 미승인, 후속.
 */
fun androidTokenStore(context: Context): TokenStore = FileTokenStore(File(File(context.filesDir, "platen"), "tokens.json"))

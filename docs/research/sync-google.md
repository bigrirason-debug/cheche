# 리서치 — Google 로그인 + Google Drive 동기화 타당성

> 대상 결정: D-005 / D-011 Q3. 열람일 **2026-10-24**. 작성 환경: 클라우드 컨테이너(실기기·Cloud 콘솔 접근 불가).
>
> **중요한 환경 제약**: 이 세션의 송신 프록시가 `developers.google.com`, `developers.google.cn`, `support.google.com`, `cloud.google.com`(일부), `firebase.google.com`, `groups.google.com`, `discuss.google.dev`, `web.archive.org`, `ietf.org`, `iana.org` 를 **정책 차단**했다(CONNECT 403 / DNS 미해결). 따라서 Google OAuth·Drive 가이드 페이지 **본문을 직접 열지 못했다**. 대신 다음을 사용했다.
> - **Drive API v3 Discovery 문서**(`https://www.googleapis.com/discovery/v1/apis/drive/v3/rest`, revision `20261020`) — Google이 기계가 읽도록 배포하는 공식 API 명세. 필드·메서드·파라미터 설명 원문은 여기서 인용한다. → 등급 **공식 문서(Discovery)**
> - **검색 결과 스니펫**(developers.google.com / support.google.com 페이지에서 발췌된 문장) — 전문을 열지 못했으므로 등급을 **공식 문서(스니펫 경유·전문 미열람)** 로 낮춰 표기한다. 로컬에서 원문 재확인이 필요하다.
> - 열린 1차 자료: `developer.android.com`, `pub.dev`, `nuget.org`, `repo1.maven.org`, `github.com`/`raw.githubusercontent.com`(Google 공식 라이브러리 소스), `microsoft.com`.
>
> 등급 표기: **[공식]** 공식 문서 직접 열람 / **[공식·Discovery]** / **[공식·스니펫]** 전문 미열람 / **[1차]** 1차 관찰(소스·레지스트리 직접 확인) / **[2차]** 2차 글 / **[추정]** 근거 있는 추정(원문 미확인). 자료가 없으면 "자료 부족".

## 0. 요약(결론 먼저)

| 질문 | 결론 | 등급 |
|---|---|---|
| 테스트(Testing) 상태에서 리프레시 토큰이 7일 만에 죽는가 | **그렇다.** support.google.com 발췌: "Consent granted by a test user lapses after seven days, and any refresh token from an offline request expires with it." 즉 외부(External) + Testing 조합이면 **범위(scope) 종류와 무관하게** 7일 후 재동의가 필요하다고 읽힌다. 비민감 범위 예외 문구(이름·이메일·프로필만 요청할 때 예외)는 developers.google.com 페이지에 있었다고 기억하지만 **이번에 원문을 열지 못해 미검증**. Drive 범위는 어차피 그 예외에 해당하지 않으므로 Platen에는 **7일 규칙이 적용된다고 가정**해야 한다. | 공식·스니펫 + 추정 |
| 그러면 어떻게 운영하나 | **"In production"으로 전환**(Publish app). 비민감 범위(`drive.file`, `drive.appdata`)만 쓰면 **검증(verification) 없이도** 운영 가능하고 사용자 수 제한·7일 만료가 사라진다. 대신 동의 화면에 "확인되지 않은 앱" 경고가 붙을 수 있다(브랜드 검증 전). 개인 Gmail 개발자는 Internal 유형을 쓸 수 없다(조직 필요). | 공식·스니펫 |
| 권장 scope | **`drive.file`** (비민감, 검증 불필요, 사용자가 Drive 웹에서 파일을 볼 수 있음, 두 플랫폼이 **같은 Cloud 프로젝트** 안의 클라이언트면 서로가 만든 파일 접근 가능 — 단 "앱=프로젝트" 단위라는 점은 원문 미열람으로 **추정**). `drive.appdata`는 숨김·백업 전용 느낌이라 "노트북에서 바로 연다"는 요구와 어긋남. `drive`(전체)는 **제한(restricted)** 범위라 보안 평가 대상 → 제외. | 공식·Discovery + 스니펫 + 추정 |
| Windows 로그인 경로 | **루프백 리다이렉트(`http://127.0.0.1:{port}/…`) + PKCE** 데스크톱 앱 클라이언트. .NET이면 `Google.Apis.Auth`(1.77.0, Apache-2.0)의 `GoogleWebAuthorizationBroker` — 기본 코드 수신기가 `LocalServerCodeReceiver`, PKCE 기본 on. JVM(KMP)이면 `google-auth-library-oauth2-http`(1.54.0, BSD-3)의 `UserAuthorizer`(PKCE 지원). Flutter면 `google_sign_in`이 **Windows 미지원** → `googleapis_auth`(2.3.5, Windows 지원, 루프백 리다이렉트)로 직접 구현. | 1차(소스·레지스트리) |
| 동기화 방식 | `changes.getStartPageToken` → 주기적 `changes.list`(토큰은 만료되지 않음) + `files.update`(resumable). **v3에는 ETag/If-Match 조건부 쓰기가 없다**(Discovery에 etag 필드·etagRequired 없음) → 낙관적 잠금은 `version`/`md5Checksum` 비교로 앱이 직접 구현. 푸시 알림은 HTTPS 서버 필요 → **폴링**. | 공식·Discovery |

---

## 1. OAuth 클라이언트 유형과 플랫폼별 흐름

### 1.1 Android 클라이언트
- **사용할 것**: Credential Manager(인증) + `AuthorizationClient`(Drive 범위 인가). developer.android.com 레거시 마이그레이션 문서 원문: 레거시 Google Sign-In SDK(`com.google.android.gms:play-services-auth` 일부)는 "**is deprecated and will be removed from the Google Play services Auth SDK in a future release**". 인증과 인가는 "distinct actions": "**Authentication:** Use Credential Manager to sign the user into your app. **Authorization:** Use the AuthorizationClient API for specific actions, such as accessing Google Drive." — 출처 https://developer.android.com/identity/sign-in/legacy-gsi-migration (Last updated 2026-03-06) **[공식]**
- 레거시 GSI 페이지 원문: "**The Google Sign-In for Android API is outdated and no longer supported.** To ensure the continued security and usability of your app, migrate your Sign in with Google implementation to Credential Manager today." 제거 일정은 명시 없음. — https://developer.android.com/identity/legacy/gsi (Last updated 2026-02-26) **[공식]**
- **클라이언트 ID 구성(사이드로드 관점)**: Sign in with Google 문서는 "You use the client ID configured in the Google Auth Platform while implementing the Credential Manager APIs" 라고만 쓰고, 구현 페이지는 `GetGoogleIdOption.Builder().setServerClientId(WEB_CLIENT_ID)` 즉 **웹 애플리케이션 유형 클라이언트 ID**를 요구한다. — https://developer.android.com/identity/sign-in/credential-manager-siwg (2026-10-23), https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation (2026-10-23) **[공식]**
- **Android 유형 클라이언트(패키지명 + 서명 SHA-1)**: 이번에 Google Auth Platform 도움말(support.google.com/cloud/answer/6158849, /15544987)을 열지 못했다. 종전 지식으로는 Android 클라이언트는 `applicationId` + 서명 인증서 SHA-1로 식별되며 **클라이언트 시크릿이 없다**. 디버그 키(Android Studio가 `$HOME/.android/debug.keystore`에 생성, "insecure by design" — https://developer.android.com/studio/publish/app-signing, 2026-03-06 **[공식]**)와 릴리스 키는 서명 인증서가 다르므로 **각 키의 SHA-1을 별도 Android 클라이언트로 등록해야 한다** **[추정 — 원문 미열람, 로컬 숙제에서 확인]**. 사이드로드이므로 Play App Signing 재서명은 없고, **릴리스 keystore 하나를 사용자가 직접 보관**하면 SHA-1은 영구 고정이다.
- SHA-1 추출: Android Studio Gradle `signingReport` 태스크(공식 문서가 권장하는 방법) 또는 `keytool -list -v -keystore <keystore> -alias <alias>` (keytool 명령 자체는 공식 페이지에 SHA-1용으로 실려 있지 않음 — **[추정]**, JDK 표준 도구).
- 라이브러리: `androidx.credentials:credentials` 안정판 **1.6.0**(2026-04-08) — https://developer.android.com/jetpack/androidx/releases/credentials **[공식]**; 구현 가이드는 `1.7.0-alpha03` 예시 + `com.google.android.libraries.identity.googleid:googleid:<latest>` **[공식]**. `play-services-auth`·`googleid` 정확한 최신 버전은 `dl.google.com`/`maven.google.com` 차단으로 **자료 부족**.

### 1.2 데스크톱(Windows) 클라이언트
- **공식 가이드(developers.google.com/identity/protocols/oauth2/native-app) 원문 미열람.** 다음은 Google 공식 라이브러리 소스로 확인한 사실이다.
- `Google.Apis.Auth`(.NET)의 `LocalServerCodeReceiver.cs`: `IPAddress.Loopback` 포트 0 바인딩으로 **임의 포트**를 얻고, 콜백 경로는 `internal const string LoopbackCallbackPath = "/authorize/";`, 기본 템플릿은 `http://127.0.0.1:{port}/authorize/` → 실패 시 `http://localhost:{port}/authorize/`. Windows에서는 `cmd /c start`로 시스템 브라우저를 연다. 주석: "127.0.0.1 is recommended, but can't be done in non-admin Windows 7 and 8 at least." — https://raw.githubusercontent.com/googleapis/google-api-dotnet-client/main/Src/Support/Google.Apis.Auth/OAuth2/LocalServerCodeReceiver.cs **[1차]**
- `GoogleWebAuthorizationBroker.cs`: `AuthorizeAsync(ClientSecrets, scopes, user, ct, IDataStore dataStore = null, ICodeReceiver codeReceiver = null)`; `codeReceiver ?? new LocalServerCodeReceiver()`; `dataStore ?? new FileDataStore(Folder)`; ClientSecrets 오버로드는 **`usePkce: true`** 로 호출 → PKCE 기본 사용. 클래스 설명: "only suitable for client-side use" (로컬 브라우저 필요). **[1차]**
- `AuthorizationCodeFlow.cs`: 토큰 요청에 `request.ClientSecret = ClientSecrets.ClientSecret;` 를 넣는다 → 데스크톱 앱 클라이언트는 **client_secret 값 자체는 필요**하다. **[1차]**
- **클라이언트 시크릿 취급 공식 입장**: Google 가이드 원문("installed apps… cannot keep secrets" 취지)과 RFC 8252 §8.5 모두 **이번에 열지 못함** → **[추정]**: 데스크톱 클라이언트의 시크릿은 기밀로 취급되지 않으며(앱에 내장되어 추출 가능), 보안은 PKCE + 루프백에서 나온다. 그래도 리포에 커밋하지 않는 운영은 §8 LOCAL_TODO 참고.
- JVM 경로: `com.google.auth:google-auth-library-oauth2-http` **1.54.0**(Maven Central 2026-09-30) **[1차]**. `UserAuthorizer.java`: `PKCEProvider` 필드, `setPKCEProvider()`, 인가 URL에 `code_challenge`/`code_challenge_method`, 토큰 교환에 `code_verifier`; 토큰 저장은 `TokenStore`(기본 `MemoryTokensStorage`); 클라이언트 인증 방식 `CLIENT_SECRET_POST`(기본)/`CLIENT_SECRET_BASIC`/`NONE`. — https://raw.githubusercontent.com/googleapis/google-auth-library-java/main/oauth2_http/java/com/google/auth/oauth2/UserAuthorizer.java **[1차]**. 단, 이 GitHub 리포는 2026-05-12 보관(archived)되었고 내용은 `googleapis/google-cloud-java/tree/main/google-auth-library-java` 로 이동(README 고지) **[1차]** — 소스 위치만 바뀌었고 Maven 아티팩트는 계속 발행 중.
- 구세대 `com.google.oauth-client:google-oauth-client(-jetty)` 1.39.0(2025-05-16) **[1차]** — `LocalServerReceiver` 제공. GitHub 리포 `google-oauth-client-java`는 **404**(이동/비공개 추정) → 신규 채택 비권장 **[추정]**.

### 1.3 리프레시 토큰 저장 원칙
- 공식 가이드 원문 미열람. 라이브러리 동작으로 본 관례: .NET `FileDataStore`(사용자 프로필 폴더에 JSON 평문 저장), Java `TokenStore` 인터페이스. **[1차]**
- 권장(**[추정]**): Windows는 DPAPI(`ProtectedData`) 또는 Windows Credential Manager로 감싸서 저장, Android는 `EncryptedSharedPreferences`/Keystore — 단 Android에서 `AuthorizationClient`를 쓰면 **리프레시 토큰을 앱이 직접 들고 있지 않고 Play 서비스가 액세스 토큰을 발급**한다(마이그레이션 문서 취지) **[추정 — AuthorizationClient 레퍼런스 미열람]**.

## 2. OAuth 동의 화면 "테스트(Testing)" 상태의 제약 (핵심)

support.google.com/cloud/answer/10311615, /15549945("Manage App Audience"), /15544987 **전문 미열람**. 검색 스니펫으로 확보한 원문 문장:

| 항목 | 발췌 원문 | 출처 |
|---|---|---|
| 테스트 사용자 한도 | "Projects configured with a publishing status of Testing are limited to up to 100 test users listed in the OAuth consent screen." | support.google.com/cloud/answer/15549945 **[공식·스니펫]** |
| **7일 만료** | "Consent granted by a test user lapses after seven days, and any refresh token from an offline request expires with it." | 동일 **[공식·스니펫]** |
| 프로덕션 전환 | "Choosing the Publish app button switches the status to In production"; "Once in production, the app is available to any user with a Google Account." | 동일 **[공식·스니펫]** |
| 검증 필요 조건 | 프로덕션 앱은 "may need verification before their name and logo appear on the consent screen or before they can request sensitive or restricted scopes"; 미검증 상태에서 민감/제한 범위를 요청하면 "users see an unverified-app warning". | support.google.com/cloud/answer/10311615 **[공식·스니펫]** |
| 미검증 앱 사용자 캡 | 민감/제한 범위를 요청하는 미검증 앱에는 별도 사용자 캡이 있고 "it applies over the entire lifetime of the project, and it cannot be reset or changed." | 동일 **[공식·스니펫]** |
| Internal 유형 | "Projects associated with a Google Cloud Organization can configure Internal users to limit authorization requests to members of the organization." 조직 밖 사용자는 `org_internal` 오류. | support.google.com/cloud/answer/15549945 **[공식·스니펫]** |
| External 유형 | "Projects configured with a user type of External are available to any user with a Google Account." | 동일 **[공식·스니펫]** |

**7일 규칙의 적용 범위 판정**
- 위 스니펫 문장은 **범위 종류를 조건으로 달지 않는다**("Testing" 상태의 테스트 사용자 동의 자체가 7일 후 소멸).
- developers.google.com/identity/protocols/oauth2 의 "Refresh token expiration" 절에는 종전에 "external user type + publishing status Testing → 7일 만료, 단 요청 범위가 이름·이메일·프로필의 부분집합이면 예외"라는 문장이 있었다고 기억한다. **이번 세션에서 원문 열람 실패(차단) → 미검증 [추정]**. 검색 스니펫(2차 포럼 인용)도 7일 규칙 본문은 반복해서 인용하지만 예외 문구는 검색에 걸리지 않았다.
- **Platen에 대한 결론**: Drive 범위를 요청하는 이상 예외 조항(있더라도)에 해당하지 않는다. 따라서 **Testing 상태로 두면 두 기기 모두 7일마다 재로그인**이 필요하다. 개인용이라 해도 "오프라인 우선 + 자동 동기화" 목표와 충돌하므로 **In production 전환이 사실상 필수**.
- 2차 자료 경고(검색 스니펫, 포럼): 프로덕션 전환 후에도 **Testing 때 발급된 토큰은 그대로 7일에 죽는 사례**가 보고됨 → 전환 후 **기존 토큰을 지우고 재동의** 한 번 수행. Google 계정당 OAuth 클라이언트 ID당 리프레시 토큰 50개 한도(초과 시 가장 오래된 것 무효화). **[2차]**

**개인 Gmail 계정 개발자의 현실적 선택지**
1. External + **In production** + 비민감 범위만(`drive.file`/`drive.appdata`/`openid email profile`) → 검증 제출 없이 운영 가능(동의 화면에 앱 이름 대신 프로젝트 ID/미검증 경고가 보일 수 있음 — **[추정]**). 사용자 1명이므로 캡 무관. **← 권장**
2. External + Testing + 테스트 사용자에 본인 등록 → 7일마다 재동의. 스파이크 단계에서만.
3. Internal → Google Cloud Organization(Workspace 또는 Cloud Identity) 필요. 개인 Gmail만으로는 불가 **[공식·스니펫 기반 추정]**.
4. 브랜드 검증(앱 이름·로고 표시용)은 선택. Android SIWG 문서는 "Your brand must be verified for your app name to be visible to users on the Sign in with Google consent screen" — https://developer.android.com/identity/sign-in/credential-manager-siwg **[공식]**. 로그인 자체를 막지는 않는다는 해석 **[추정]**.

## 3. Drive 범위(scope) 선택

### 3.1 Discovery 문서의 범위 설명 원문 **[공식·Discovery, revision 20261020]**
| scope | 설명 원문 |
|---|---|
| `…/auth/drive.appdata` | "See, create, and delete its own configuration data in your Google Drive" |
| `…/auth/drive.file` | "See, edit, create, and delete only the specific Google Drive files you use with this app" |
| `…/auth/drive` | "See, edit, create, and delete all of your Google Drive files" |
| `…/auth/drive.readonly` | "See and download all your Google Drive files" |
| `…/auth/drive.metadata(.readonly)` | 메타데이터 조회/관리 |

### 3.2 민감도 분류 **[공식·스니펫 — developers.google.com/workspace/drive/api/guides/api-specific-auth 전문 미열람]**
- `drive.appdata`: **Recommended, Non-sensitive**
- `drive.file`: **Recommended, Non-sensitive**
- `drive`, `drive.readonly`: **Restricted** ("only specific application types are permitted to use restricted scopes" — backup/sync, productivity/education, reporting/security; 가능하면 비민감 범위로 이전 권고)
- `drive.apps.readonly`: Sensitive
- 일반 정의(Slides scopes 페이지 스니펫): 비민감 → 기본 앱 검증만; 제한 → "restricted scope verification process".

### 3.3 사용자가 Drive 웹에서 볼 수 있는가
- `drive.file`: 앱이 만든 파일은 **일반 Drive 파일**이므로 Drive 웹/데스크톱 클라이언트에서 보이고, 사용자가 폴더로 정리할 수 있다 **[추정 — 가이드 미열람이나 Discovery의 `spaces` 설명이 `drive`와 `appDataFolder`를 구분하는 것과 일치]**.
- `drive.appdata`: `spaces=appDataFolder`에 저장되며 Discovery의 `restrictToMyDrive` 설명 "omits changes to files such as those in the Application Data folder" 처럼 My Drive 계층 밖. 공식 가이드(appdata)는 미열람이지만 종전 지식으로 **사용자에게 숨겨지고 해당 앱만 접근** **[추정]**. 사용자 요구("노트북에서 바로 열기", 굿노트 느낌)와는 맞지만 "내 파일을 내가 Drive에서 본다"는 투명성은 없다.

### 3.4 Windows 앱과 Android 앱이 같은 파일에 접근하려면
- `drive.file`의 "this app"이 **OAuth 클라이언트 단위인지 Cloud 프로젝트 단위인지** — 공식 가이드 원문 미열람 → **[추정]**: Drive의 "앱" 식별은 Cloud **프로젝트** 단위이고(appProperties 설명도 "private to the requesting app"), 같은 프로젝트 안의 Android 클라이언트와 데스크톱 클라이언트는 같은 앱으로 취급된다. **LOCAL_TODO**: 같은 프로젝트의 두 클라이언트로 각각 로그인해 한쪽이 만든 파일을 `files.list`로 보이는지 실측. 실측이 실패하면 B안은 (a) 두 플랫폼 모두 같은 "데스크톱" 클라이언트 + 루프백(Android도 가능하나 비권장) 또는 (b) `drive.appdata`도 동일 문제이므로 결국 `drive`(restricted)로 가야 하는데 이는 비권장 → (c) 폴더 동기화 대안(§7).
- `appProperties`(앱 비공개 키-값, OAuth 클라이언트로만 조회) vs `properties`(모든 앱에 보임) — Discovery 원문 **[공식·Discovery]**. 노트 메타(로컬 세대 번호 등)는 `appProperties`에 두면 같은 프로젝트의 두 클라이언트가 공유 **[추정]**.

### 3.5 권장
- **1차 선택: `drive.file`** + `openid email profile`. 루트에 `Platen/` 폴더를 앱이 생성하고 그 아래 `*.platen` 저장.
- `drive.appdata`는 **설정·기기 레지스트리·동기화 커서(startPageToken) 보관용으로 병행** 가능(둘 다 비민감).
- `drive` 전체 범위는 제외.

## 4. 동기화 메커니즘

### 4.1 증분 동기화 **[공식·Discovery]**
- `changes.getStartPageToken`: "Gets the starting pageToken for listing future changes." `StartPageToken.startPageToken`: "**The page token doesn't expire.**"
- `changes.list`: `pageToken` = 이전 응답의 `nextPageToken` 또는 `getStartPageToken` 결과; `pageSize` 기본 100, 최대 1000; `spaces`에 `'drive'`와 `'appDataFolder'` 지원; `includeRemoved`로 삭제/접근상실 변경 포함; `restrictToMyDrive`.
- `Change` 리소스: `fileId`, `time`, `removed`("removed from this list of changes, for example by deletion or loss of access"), `file`(삭제되지 않았을 때 최신 상태), `changeType`(`file`/`drive`).
- 첫 동기화: `files.list(q="'<PlatenFolderId>' in parents and trashed=false", fields=...)` 전체 열거 후 `getStartPageToken` 저장 **[추정 — 가이드 manage-changes 미열람, 표준 패턴]**.
- 주의: `drive.file` 범위에서는 `changes.list`가 **앱이 접근 가능한 파일의 변경만** 돌려준다 **[추정]**.

### 4.2 조건부 쓰기(ETag/If-Match) **[공식·Discovery]**
- v3 `File` 스키마에 **`etag` 필드 없음**, 어느 스키마에도 `etag` 없음, `files.update`에 `etagRequired` 플래그 없음. `files.update` 파라미터: `addParents`, `removeParents`, `keepRevisionForever`, `useContentAsIndexableText`, `ocrLanguage`, `supportsAllDrives`, `includeLabels`, `includePermissionsForView` 등 — **조건부 쓰기 파라미터 없음**.
- 따라서 **서버측 낙관적 잠금은 불가**. 앱이 "업로드 직전 `files.get(fields=version,md5Checksum,headRevisionId)` → 로컬에 기억한 값과 비교 → 다르면 충돌 처리 → 같으면 업로드" 순으로 구현해야 하며, 그 사이의 경쟁은 막을 수 없다(1인 2기기 가정에서는 실질 위험 낮음). 업로드 후 응답의 `version`을 저장.

### 4.3 비교 필드 신뢰성 **[공식·Discovery 원문]**
- `md5Checksum`: "Output only. The MD5 checksum for the content of the file. This is only applicable to files with binary content in Google Drive." (zip은 binary → 제공)
- `sha1Checksum` / `sha256Checksum`: "Output only… only populated for files with content stored in Google Drive" → 사용 가능.
- `version`: "Output only. A monotonically increasing version number for the file. This reflects every change made to the file on the server, even those not visible to the user." → **메타데이터 변경(이름·부모 이동)에도 증가** → 내용 변경 판별은 `md5Checksum` 우선, `version`은 "무언가 바뀜" 신호.
- `modifiedTime`: "The last time the file was modified by anyone (RFC 3339 date-time)… setting modifiedTime will also update modifiedByMeTime" → **쓰기 가능 필드**(앱이 설정 가능) → 충돌 판정의 근거로 쓰지 말 것.
- `headRevisionId`: "Output only… only available for files with binary content".
- `Revision.md5Checksum`, `Revision.keepForever` 존재.

### 4.4 리비전 보존 **[공식·Discovery + 공식·스니펫]**
- Discovery `Revision.keepForever`: "If not set, the revision will be automatically purged **30 days** after newer content is uploaded. This can be set on a maximum of **200 revisions** for a file. This field is only applicable to files with binary content in Drive."
- `files.update.keepRevisionForever`: 새 head 리비전에 keepForever 설정, "Only 200 revisions for the file can be kept forever."
- 스니펫(manage-revisions): 보존 대상 아닌 리비전은 "typically preserved for 30 days, but can be purged earlier if a file has **100 revisions** that aren't designated as Keep Forever and a new revision is uploaded"; head 리비전은 자동 삭제되지 않음; keepForever 리비전은 저장 용량에 계산.
- 함의: 자동 저장을 자주 올리면 100개 초과분이 조기 삭제 → 최근 30일 내 복구 가능 리비전은 사실상 **최근 100개**. 중요한 시점(수동 "스냅샷")만 `keepRevisionForever=true`.

### 4.5 업로드 방식 **[공식·Discovery + 1차]**
- Discovery `mediaUpload`: `simple`(`/upload/drive/v3/files[/{fileId}]`, multipart 지원) / `resumable`(`/resumable/upload/drive/v3/files[/{fileId}]`), `accept: */*`, **`maxSize: 5497558138880`** 바이트(= 5 TiB = 5,120 GiB).
- simple·multipart의 **5 MB 권장 상한**은 가이드(manage-uploads) 원문 미열람 → **[추정]**. 청크 크기: google-api-python-client 문서 "Files larger than 256 KB (256 * 1024 B) must have chunk sizes that are multiples of 256 KB." — https://raw.githubusercontent.com/googleapis/google-api-python-client/main/docs/media.md **[1차·Google 공식 라이브러리 문서]**.
- Platen 노트(zip, 이미지·PDF 포함 시 수십 MB)는 **resumable** 기본, 메타+소형은 multipart.

### 4.6 할당량 **[공식·스니펫 — 상충 있음]**
- 한 버전의 Usage limits 페이지: "Queries per 60 seconds 12,000 / per 60 seconds per user 12,000" (developers.google.cn 미러 2025-08-04 갱신 표기).
- 다른 버전: "quota units" 모델, "Per minute per user per project 325,000 quota units", 2026년 중 과금 변경을 90일 전 고지 예정.
- → **현재 수치 확정 불가(자료 부족)**. 어느 쪽이든 1인 2기기 폴링(§4.7)은 자릿수 여유. 초과 시 `403 User rate limit exceeded` → 지수 백오프 **[공식·스니펫]**.

### 4.7 변경 알림과 폴링 설계
- `changes.watch`는 "Notifications for resource changes" 가이드로 연결되며 **HTTPS 수신 엔드포인트가 필요**(서버 없음 → 불가) **[공식·Discovery + 추정]**.
- 폴링 제안(**[추정]**): 앱 포그라운드 진입 시 1회 + 활성 중 60초 간격 `changes.list`(요청 1회/분/기기 = 하루 ≤1,440회) + 저장 큐 flush 직후 1회. 백그라운드(Android WorkManager 15분 주기, 네트워크 조건부)는 선택. 호출당 비용이 작으므로 할당량 문제 없음.

## 5. 충돌 전략 제안 (한 시점 한 기기 편집 + 마지막 쓰기 승리 + 충돌 사본) **[추정·설계]**

로컬 DB(노트별): `fileId`, `remoteVersion`, `remoteMd5`, `localGen`(로컬 세대 번호, 저장마다 +1), `syncedGen`(마지막으로 업로드 성공한 세대), `dirty = localGen > syncedGen`.

1. **업로드 전 검사**: `files.get(fileId, fields="version,md5Checksum,headRevisionId")`.
   - `md5Checksum == remoteMd5` → 원격 변화 없음 → resumable `files.update`(본문 + `appProperties{gen, deviceId, baseMd5}`) → 응답의 `version`,`md5Checksum`을 `remoteVersion/remoteMd5`에, `syncedGen = localGen`.
   - `md5Checksum != remoteMd5` 이고 `dirty` → **충돌**. 원격본을 `"<이름> (충돌 사본 <기기> <시각>).platen"` 으로 다운로드해 새 파일로 Drive에 `files.create`(원본 폴더), 내 로컬본으로 원격을 덮어씀(마지막 쓰기 승리). UI에 충돌 사본 알림.
   - `md5Checksum != remoteMd5` 이고 `!dirty` → 단순 다운로드.
2. **다운로드(changes.list 결과)**: `removed` → 로컬 휴지통. `file.md5Checksum != remoteMd5` → `dirty`면 1의 충돌 분기, 아니면 교체.
3. **md5 신뢰**: Drive가 서버에서 계산한 콘텐츠 해시이므로 **내용 동일성 판정에 충분**; `version`은 메타 변경에도 오르므로 보조. `modifiedTime`은 앱이 설정 가능한 필드라 **판정에 사용 금지**(§4.3).
4. **zip 결정성**: 같은 내용이면 같은 md5가 나오도록 zip 엔트리 순서·타임스탬프 고정(안 그러면 "변경 없음"을 해시로 못 잡음) — 포맷 리서치에 전달.
5. **단일 직렬 큐**: 로컬 저장 → 큐 → 업로드는 한 워커만. 앱 종료 시 큐 영속화(D-005 교훈).

## 6. 플랫폼별 구현 라이브러리

| 플랫폼/스택 | 로그인·인가 | Drive 호출 | 버전·라이선스·근거 |
|---|---|---|---|
| **Android (Kotlin)** | `androidx.credentials:credentials` + `credentials-play-services-auth` **1.6.0**(안정, 2026-04-08) + `googleid`(버전 미확인) → 인증. `play-services-auth`의 `Identity.getAuthorizationClient()` → Drive 범위 액세스 토큰 **[공식 developer.android.com]** | (a) `com.google.api-client:google-api-client-android` **2.9.2**(2026-10-22) + `com.google.apis:google-api-services-drive` **v3-rev20261011-2.0.0**(2026-10-18) **[1차 Maven Central]**, Apache-2.0(google-api-java-client 리포 LICENSE **[1차]**), Java 7+ / Android 1.6+ 표기 **[1차]**; (b) **직접 HTTP**(OkHttp/Ktor + kotlinx.serialization) — 필요한 엔드포인트가 6~7개라 **(b) 권장 [추정]**(의존성·메서드 수 절감, resumable 직접 구현 필요) | 레거시 `GoogleSignIn` 사용 금지(§1.1) |
| **Windows .NET (WinUI/C#)** | `Google.Apis.Auth` **1.77.0**(2026-09-28, Apache-2.0, net6.0/netstandard2.0/net462) `GoogleWebAuthorizationBroker.AuthorizeAsync` — 루프백 + PKCE 기본, `FileDataStore` **[1차 nuget + 소스]** | `Google.Apis.Drive.v3` **1.77.0.4301**(2026-10-20, Apache-2.0) **[1차 nuget]** | 리포 README: "supported but in maintenance mode only" **[1차]** — 유지보수 모드지만 계속 발행 |
| **KMP / Compose Desktop (JVM)** | `com.google.auth:google-auth-library-oauth2-http` **1.54.0**(2026-09-30, BSD-3) `UserAuthorizer` + `PKCEProvider` **[1차]**; 루프백 서버는 직접(Ktor CIO 수 줄) 또는 구 `google-oauth-client-jetty` 1.39.0 `LocalServerReceiver` | Android와 같은 직접 HTTP 클라이언트(Ktor)를 commonMain에 → **코드 공유 이점** **[추정]** | Android 쪽은 Credential Manager(expect/actual) |
| **Flutter** | `google_sign_in` **7.2.0**(BSD-3): 플랫폼 **Android, iOS, macOS, Web — Windows 없음** **[1차 pub.dev]**. 대안: `googleapis_auth` **2.3.5**(BSD-3, Android/iOS/Linux/macOS/web/**Windows**), 설치형 앱 흐름 `clientViaUserConsent`가 "listens on localhost for the redirect"(브라우저는 앱이 `url_launcher`로 열어야 함; PKCE 여부 페이지에 명시 없음 → **자료 부족**) **[1차]**; `google_sign_in_all_platforms` 2.0.3(서드파티, Windows 지원, `http://localhost:<port>` 리다이렉트) **[1차]**; `flutter_appauth` 12.1.0은 **Windows 미지원** **[1차]**; `oauth2_client` 4.3.3(BSD-2, Windows 포함, PKCE 명시 없음) **[1차]** | `googleapis` **17.0.0**(`drive/v3` 포함) **[1차]** | Flutter 채택 시 Windows 로그인은 **직접 구현 비용 발생**(스파이크 축에 반영) |

## 7. 대안 비교(개인 1인·기기 2대 관점) **[추정 — Drive 데스크톱 클라이언트·OneDrive 문서 미열람]**

| 안 | 장점 | 단점 |
|---|---|---|
| **Drive API `drive.file`** (권장) | 비민감·검증 불필요, 파일이 Drive에 보임, 양 플랫폼 동일 코드, 리비전 30일/100개 무료 백업, 충돌 제어를 앱이 소유 | OAuth 구현·프로덕션 전환 숙제, 루프백 로그인 UX, ETag 없음 |
| Drive API `drive.appdata` | 가장 좁은 권한, 폴더 구조 고민 없음 | 사용자가 Drive에서 못 봄 → "노트를 노트북에서 바로 연다"는 체감과 거리, 앱 삭제·권한 회수 시 유실 위험(추정) |
| 폴더 동기화(Windows: Drive 데스크톱 클라이언트가 동기화하는 폴더를 앱이 직접 읽기 / Android: SAF `ACTION_OPEN_DOCUMENT_TREE`로 Drive 제공자 폴더 선택 — SAF는 "cloud or local storage services can participate… by implementing a DocumentsProvider" 예시로 Google Drive 명시 **[공식 developer.android.com, 2026-10-23]**) | OAuth·Cloud 프로젝트 **불필요**, 로그인 숙제 0 | Android Drive 제공자는 온디맨드·단일 파일 중심이라 폴더 전체 오프라인·변경 감지가 불안정(추정), 충돌 처리를 Drive 클라이언트에 맡김, "구글 로그인" 요구와 어긋남 |
| OneDrive(Microsoft Graph) | Windows에 기본 내장, delta 쿼리·ETag 지원(추정) | Android에서 Microsoft 로그인 필요, 사용자 요구(구글 계정)와 불일치 → 제외 |

## 8. 리스크 표 · LOCAL_TODO 초안

### 8.1 리스크
| # | 리스크 | 영향 | 완화 |
|---|---|---|---|
| R1 | Testing 상태 7일 토큰 만료 → 주기적 재로그인, 백그라운드 동기화 실패 | 높음 | In production 전환(비민감 범위만), 전환 후 토큰 재발급 |
| R2 | `drive.file`의 "앱" 경계가 프로젝트가 아니라 클라이언트 단위일 가능성 | 높음(두 기기가 서로 파일을 못 봄) | LOCAL_TODO 실측 선행; B안 = 한 데스크톱 클라이언트 공용 또는 폴더 동기화 |
| R3 | 사이드로드 디버그/릴리스 SHA-1 불일치 → Android 인가 실패 | 중 | 두 키 모두 등록, 릴리스 keystore 백업 |
| R4 | ETag 부재 → 경쟁 쓰기 시 덮어씀 | 중(1인이라 낮음) | 업로드 전 get-compare, 충돌 사본, 리비전 복구 |
| R5 | 자동 저장 폭주 → 리비전 100개 조기 삭제·할당량 | 낮 | 디바운스 업로드(예: 마지막 편집 후 30초), 스냅샷만 keepForever |
| R6 | 할당량 모델 변경(quota units·과금 고지) | 낮 | 2026 공지 추적, 폴링 간격 설정화 |
| R7 | 미검증 앱 경고 화면이 사용자 불안 유발 | 낮 | 개인용이므로 수용; 원하면 브랜드 검증 제출 |
| R8 | Flutter 채택 시 Windows 로그인 직접 구현 | 중 | 스파이크에서 `googleapis_auth` 루프백 PoC 1일 |
| R9 | 구글 정책 페이지 원문 미확인(이 문서의 [스니펫]·[추정] 항목) | 중 | LOCAL_TODO L0에서 원문 재확인 후 본 문서 갱신 |

### 8.2 LOCAL_TODO 초안(사용자 로컬 숙제)
- **L0 원문 확인(30분)**: 아래 URL을 열어 본 문서의 [공식·스니펫]/[추정] 항목을 확정하고 "최종 업데이트" 표기를 기록 — `developers.google.com/identity/protocols/oauth2#expiration`, `…/oauth2/native-app`, `…/workspace/drive/api/guides/api-specific-auth`, `…/appdata`, `…/manage-uploads`, `…/limits`, `support.google.com/cloud/answer/15549945`, `/10311615`, `/6158849`.
- **L1 Google Cloud 프로젝트 생성**: 콘솔 → 새 프로젝트(예: `platen-sync`) → API 및 서비스 → **Google Drive API 사용 설정**.
- **L2 OAuth 동의 화면(Google Auth Platform)**: 사용자 유형 **External**, 앱 이름 `Platen`, 지원 이메일, 범위에 `…/auth/drive.file`(+ 필요 시 `drive.appdata`, `openid`, `email`, `profile`) 추가. 처음엔 Testing + 테스트 사용자에 본인 Gmail 추가 → 스파이크 끝나면 **"앱 게시(Publish app)" → In production**(비민감 범위만이면 검증 제출 없이 진행; 경고 문구 캡처).
- **L3 클라이언트 생성**: ① **Android** 클라이언트 ×2(디버그 SHA-1, 릴리스 SHA-1; 패키지명은 naming.md 확정값) ② **웹 애플리케이션** 클라이언트 1개(Credential Manager `serverClientId`용) ③ **데스크톱 앱** 클라이언트 1개(Windows; client_id + client_secret JSON 다운로드).
- **L4 SHA-1 추출**: 디버그 `keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android` (기본 자격증명은 관례값 **[추정]**; 안 맞으면 Android Studio Gradle `signingReport`로 확인 **[공식]**), 릴리스 `keytool -list -v -keystore <release.jks> -alias <alias>`. 릴리스 keystore는 비밀번호와 함께 **오프라인 백업**(분실 시 SHA-1 변경 → 클라이언트 재등록).
- **L5 값의 리포 반영 방식**:
  - 리포에 넣어도 되는 것: `applicationId`, 웹 클라이언트 ID(공개 식별자), Android 클라이언트 ID. 그래도 **로컬 파일로 분리**해 관리: Android `local.properties`/`secrets.properties`(gitignore) → `BuildConfig`; Windows `appsettings.Local.json`(gitignore) 또는 사용자 환경변수.
  - 리포에 넣지 않는 것: 데스크톱 클라이언트의 `client_secret`(기밀은 아니지만 커밋 금지), keystore 파일·비밀번호.
  - CI(GitHub Actions)에서 빌드할 때는 **GitHub Secrets**(`PLATEN_DESKTOP_CLIENT_ID`, `PLATEN_DESKTOP_CLIENT_SECRET`, `PLATEN_WEB_CLIENT_ID`, `PLATEN_RELEASE_KEYSTORE_BASE64`, `PLATEN_KEYSTORE_PASSWORD`)로 주입 → 빌드 스텝에서 파일로 복원. 리포에는 `*.example` 템플릿만.
- **L6 실측**: 같은 프로젝트의 Android·데스크톱 클라이언트로 각각 로그인 → 한쪽에서 `files.create`한 파일이 다른 쪽 `files.list`/`changes.list`에 보이는지(R2). 7일 뒤 Testing 토큰이 실제로 죽는지, 프로덕션 전환 후 재발급 토큰이 유지되는지(R1) 기록.

## 9. 출처 목록(열람일 2026-10-24, 실제로 연 URL과 열람 실패 URL 구분)

**열람 성공**
- https://www.googleapis.com/discovery/v1/apis/drive/v3/rest — Drive API v3 Discovery, revision 20261020 [공식·Discovery]
- https://developer.android.com/identity/sign-in/legacy-gsi-migration — Last updated 2026-03-06 [공식]
- https://developer.android.com/identity/legacy/gsi — Last updated 2026-02-26 [공식]
- https://developer.android.com/identity/sign-in/credential-manager-siwg — Last updated 2026-10-23 [공식]
- https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation — Last updated 2026-10-23 [공식]
- https://developer.android.com/jetpack/androidx/releases/credentials — 1.6.0 (2026-04-08) [공식]
- https://developer.android.com/studio/publish/app-signing — Last updated 2026-03-06 [공식]
- https://developer.android.com/guide/topics/providers/document-provider — Last updated 2026-10-23 [공식]
- https://raw.githubusercontent.com/googleapis/google-api-dotnet-client/main/Src/Support/Google.Apis.Auth/OAuth2/LocalServerCodeReceiver.cs [1차]
- https://raw.githubusercontent.com/googleapis/google-api-dotnet-client/main/Src/Support/Google.Apis.Auth/OAuth2/GoogleWebAuthorizationBroker.cs [1차]
- https://raw.githubusercontent.com/googleapis/google-api-dotnet-client/main/Src/Support/Google.Apis.Auth/OAuth2/Flows/AuthorizationCodeFlow.cs [1차]
- https://github.com/googleapis/google-api-dotnet-client/blob/main/README.md — "maintenance mode only" [1차]
- https://www.nuget.org/packages/Google.Apis.Auth — 1.77.0 (2026-09-28), Apache-2.0 [1차]
- https://www.nuget.org/packages/Google.Apis.Drive.v3 — 1.77.0.4301 (2026-10-20), Apache-2.0 [1차]
- https://raw.githubusercontent.com/googleapis/google-auth-library-java/main/oauth2_http/java/com/google/auth/oauth2/UserAuthorizer.java [1차]
- https://github.com/googleapis/google-auth-library-java (및 /blob/main/README.md) — archived 2026-05-12, moved to google-cloud-java, BSD-3 [1차]
- https://github.com/googleapis/google-api-java-client — Apache-2.0, Java 7+ [1차]
- https://repo1.maven.org/maven2/com/google/auth/google-auth-library-oauth2-http/maven-metadata.xml — 1.54.0 (2026-09-30) [1차]
- https://repo1.maven.org/maven2/com/google/oauth-client/google-oauth-client/maven-metadata.xml — 1.39.0 (2025-05-16) [1차]
- https://repo1.maven.org/maven2/com/google/api-client/google-api-client-android/maven-metadata.xml — 2.9.2 (2026-10-22) [1차]
- https://repo1.maven.org/maven2/com/google/apis/google-api-services-drive/maven-metadata.xml — v3-rev20261011-2.0.0 (2026-10-18) [1차]
- https://raw.githubusercontent.com/googleapis/google-api-python-client/main/docs/media.md — 256 KB 청크 규칙 [1차]
- https://pub.dev/packages/google_sign_in — 7.2.0, BSD-3, Windows 미지원 [1차]
- https://pub.dev/packages/googleapis_auth — 2.3.5, BSD-3, Windows 지원 [1차]
- https://pub.dev/packages/google_sign_in_all_platforms — 2.0.3 [1차]
- https://pub.dev/packages/flutter_appauth — 12.1.0, Windows 미지원 [1차]
- https://pub.dev/packages/oauth2_client — 4.3.3 [1차]
- https://pub.dev/packages/googleapis — 17.0.0, drive/v3 포함 [1차]

**열람 실패(프록시 차단) — 검색 스니펫으로만 인용, 로컬 재확인 필요**
- https://developers.google.com/identity/protocols/oauth2 (Refresh token expiration)
- https://developers.google.com/identity/protocols/oauth2/native-app
- https://developers.google.com/workspace/drive/api/guides/api-specific-auth — 스니펫: drive.appdata/drive.file Non-sensitive, drive/drive.readonly Restricted
- https://developers.google.com/workspace/drive/api/guides/limits — 스니펫: 12,000/60s vs 325,000 quota units/min (상충)
- https://developers.google.com/workspace/drive/api/guides/manage-revisions — 스니펫: 30일/100개/200 keepForever
- https://developers.google.com/workspace/drive/api/guides/manage-uploads, /manage-changes, /appdata, /push
- https://developers.google.com/identity/authorization/android
- https://support.google.com/cloud/answer/15549945 (Manage App Audience) — 스니펫: 100 test users, 7일, Internal=Cloud Organization
- https://support.google.com/cloud/answer/10311615 — 스니펫: Publish app, verification, unverified-app warning, 사용자 캡
- https://support.google.com/cloud/answer/15544987, /6158849, /13463073
- 2차(검색 스니펫만): forum.rclone.org, groups.google.com/g/adwords-api, discuss.google.dev, nango.dev, unipile.com — 7일 규칙 인용·전환 후 토큰 재발급 필요·50개 한도

# 리서치 — "Platen" 이름 충돌 확인

> 대상 결정: D-002 / D-011 Q4. 열람일 **2026-10-24**. 작성 환경: 클라우드 컨테이너.
> 환경 제약: 프록시가 `play.google.com`, `apps.microsoft.com`, `apps.apple.com`, `itunes.apple.com`, RDAP/WHOIS 서버(`rdap.org`, `rdap.nic.google`, `rdap.verisign.com`, `rdap.nic.io`, `lookup.icann.org`), DNS-over-HTTPS, 상표 DB(`tmsearch.uspto.gov`, `euipo.europa.eu`, `kpat.kipris.or.kr`, `branddb.wipo.int`), 확장자 DB(`fileinfo.com`, `filext.com`, `file-extensions.org`), `flathub.org`, `wikipedia.org`, `ietf.org`, `iana.org` 를 차단했다. 직접 열람 실패 항목은 **"확인 불가"** 로 표기하고 검색 스니펫만 있는 경우 **[2차·스니펫]** 으로 등급을 낮췄다. 웹 검색 예산도 중간에 소진되어 추가 검색은 못 했다.
>
> 등급: **[1차]** 직접 조회(레지스트리 API·페이지) / **[2차]** 2차 글·검색 스니펫 / **[추정]** / "확인 불가".

## 0. 판정 요약

**조건부 사용 가능.** 소프트웨어 영역에서 "Platen"은 이미 여러 소규모 프로젝트가 쓰는 이름이고, 특히 **동일 장르(타자기 콘셉트 노트앱)에 "Platen: Paper & Ink"(iOS 전용) 가 존재한다는 2차 보고**가 있다. 스토어 배포 없는 개인용 앱이라 법적·스토어 충돌 위험은 낮지만, (1) 역도메인 식별자는 `platen.*` 도메인 소유를 전제하지 말고, (2) 상표·도메인은 로컬 숙제로 확인하며, (3) 만약 향후 공개 배포로 바뀌면 이름 재고를 권고한다.

## 1. 충돌 조사 결과

### 1.1 Google Play
- `play.google.com` 직접 열람 **불가**(차단).
- 검색 스니펫 **[2차·스니펫]**: `"platen" app site:play.google.com` 결과에 **"Platen" 이라는 이름의 Play 앱 없음**. 관련 없는 개발자 페이지(Plenqo, Platuni)와 사전 항목만. 한국어 앱 리뷰 뉴스레터(maily.so/whattheapp, 열람 실패)가 **"Platen: Paper & Ink — 타자기 타이핑을 흉내 내는 노트앱, iOS Only"** 를 소개했다는 스니펫 있음 → iOS App Store에 동명·동장르 앱 존재 가능성 높음. `apps.apple.com`/iTunes Search API 차단으로 **직접 확인 불가**.
- 로컬 숙제: Play 스토어와 App Store에서 "Platen" 검색, "Platen: Paper & Ink" 개발사·출시일 확인.

### 1.2 Microsoft Store
- `apps.microsoft.com` 차단. 대신 **https://www.microsoft.com/en-us/search/shop/apps?q=platen** 열람 성공 **[1차]**: 1,235건 중 첫 페이지 90건에 **"Platen" 정확 일치 앱 없음**. 유사 철자: "Patens", "Platin FaultPath", "Plate Catcher", 각종 "Plate…" 앱.
- 검색 스니펫(`site:apps.microsoft.com`)도 Microsoft Store 결과 0건 **[2차·스니펫]**.
- 참고: Linux Flathub에 `page.wisha.platen`(Typst/LaTeX 수식 편집기 "Platen") 존재 **[2차·스니펫, flathub 열람 실패]**.

### 1.3 GitHub
- https://github.com/search?q=platen&type=repositories **[1차]**: 359개 리포. 상위:
  - `platenio/platen` — 테이블탑 게임용 인터랙티브 디지털 북 툴킷, MIT(코드)/CC-BY-4.0(문서), 51★, **웹사이트 `platen.io`** (https://github.com/platenio/platen **[1차]**) → **`platen.io` 도메인은 이 프로젝트가 사용 중일 가능성 높음 [추정]**
  - `winchxyz/platen` — 브라우저 3D 수동 타자기(종이·리본·기계음), MIT, 15★, https://winchxyz.github.io/platen/ **[1차]** → **타자기 콘셉트 동명**
  - `dandacompany/platen` — 한국어 타자기 테마 메모 웹앱("밤 램프 아래 타자기로 쓰는 메모"), MIT, 1★/52 forks, 대학 강의 과제용, memo.dante-labs.com **[1차]** → **타자기 메모 콘셉트 동명**
  - `AlexTatiyants/platen` — WordPress용 Markdown 편집기(Chrome 패키지 앱), 12★
  - 나머지는 `platenumber`(번호판 인식) 등 부분 일치.
- GitHub 사용자 `platen` 존재(개인, CS:GO 플러그인 6개 리포) — https://github.com/platen **[1차]** → **`github.com/platen` 핸들은 사용 불가**.

### 1.4 도메인
| 도메인 | 결과 |
|---|---|
| platen.app | **확인 불가**(RDAP·DoH 차단). `.app` 은 HTTPS 필수 TLD(HSTS preload) — 참고 [추정] |
| platen.io | **확인 불가**. 단 `platenio/platen` 리포가 웹사이트로 `platen.io`를 적시 → **사용 중으로 가정** |
| getplaten.com / platen.com / platen.dev | **확인 불가** |
- 로컬 숙제: 레지스트라 또는 `whois`/RDAP(`https://rdap.org/domain/platen.app`)로 5개 조회.

### 1.5 패키지 레지스트리(참고용) **[1차, 레지스트리 API 직접 조회]**
| 레지스트리 | 결과 |
|---|---|
| npm `platen` | **점유**. v0.0.0 "Project WIP Placeholder"(2017, MIT, Jose Orihuela) — https://registry.npmjs.org/platen |
| PyPI `platen` | **점유**. 활성 프로젝트: "presses structured data into documents"(Jinja 템플릿 CLI, Cariad Eccleston, Python ≥3.11) — https://pypi.org/pypi/platen/json |
| crates.io `platen` | **점유**. v0.0.1 (2026-05-05, MIT OR Apache-2.0) "Graph-aware composition surface for the Mere browser" — https://crates.io/api/v1/crates/platen |
| pub.dev `platen` | 미조회(자료 부족) |
- 함의: 패키지명으로 `platen`을 쓸 필요는 없지만(앱이지 라이브러리가 아님), 인쇄 은유 때문에 **개발자들이 즐겨 쓰는 이름**이라는 신호.

## 2. 상표
- USPTO(TESS/TSDR), EUIPO eSearch, KIPRIS, WIPO Global Brand DB **모두 접근 불가(차단)** → **"확인 불가, 로컬 숙제"**.
- 검색 스니펫 **[2차·스니펫]**: "platen trademark USPTO/EUIPO/KIPRIS software" 검색에서 **"PLATEN" 이라는 소프트웨어 상표는 검색되지 않음**(결과는 "PLATINUM…", "ITALIAN PLATE", Heidelberg "Smartsetter" 등 무관). 이는 부재의 증거가 아니라 미검색임.
- 일반론 **[추정]**: "platen"은 인쇄기·타자기·스캐너 부품을 가리키는 **보통명사**(사전 등재)이므로, 인쇄·사무기기류(니스분류 7·9·16류)에서는 기술적(descriptive) 표장으로 식별력이 약하고, 반대로 "노트앱"(9류 소프트웨어/42류 SaaS)에서는 임의선택 표장(arbitrary)에 가까워 등록 가능성 자체는 있다. 다만 이는 상표 리서치가 아니라 일반 원칙이며, **개인용·비배포 앱에는 상표 이슈가 실질적으로 없다** [추정].
- 로컬 숙제: KIPRIS(한국) "PLATEN"/"플래튼" 9·42류, USPTO 9·42류 1회 조회.

## 3. 식별자 제안

### 3.1 Android `applicationId`
- 규칙(공식 원문, https://developer.android.com/build/configure-app-module, Last updated 2026-02-26): "It must have at least two segments (one or more dots). Each segment must start with a letter. All characters must be alphanumeric or an underscore [a-zA-Z0-9_]." 그리고 "Once you publish your app, you should never change the application ID." **[1차 공식]**
- 역도메인 관례는 소유 도메인을 전제한다. `platen.io`는 타 프로젝트가 쓰는 것으로 보이고 `platen.app`은 미확인 → **소유하지 않은 도메인의 역순은 피한다** [추정].
- 후보(우선순위):
  1. **`dev.<사용자핸들>.platen`** — 사용자가 이미 소유한 도메인(또는 GitHub Pages `<핸들>.github.io` → `io.github.<핸들>.platen`)의 역순. 충돌 위험 최소. ← 권장
  2. `app.platen` — `platen.app`을 **취득한 뒤에만**. 두 세그먼트 규칙 충족.
  3. `io.platen.notes` — `platen.io` 미소유 가능성 높음 → **비권장**.
  4. `com.platen.notes` — 동일 사유 비권장.
- 사이드로드 전용이므로 Play 고유성 검사는 없지만, OAuth Android 클라이언트가 `applicationId`+SHA-1로 묶이므로 **스파이크 전에 확정**(변경 시 클라이언트 재등록).

### 3.2 Windows 패키지/식별자
- MSIX 패키지 ID(공식 문서, MicrosoftDocs/windows-dev-docs `package-identity-overview.md`, ms.date 2023-01-10 **[1차·공식 원본 리포]**): Identity = Name·Version·Architecture·ResourceId·Publisher; Package Family Name = `<Name>_<PublisherId>`; **Name 제약: 3~50자, ASCII 영숫자·점·대시, 예약 장치명(con, prn, aux, nul, com1-9, lpt1-9) 금지, `xn--` 시작 금지, 점으로 끝나기 금지, 대소문자 무시 비교**. "The Microsoft Store enforces name uniqueness among its own apps, but names aren't guaranteed to be unique across the broader ecosystem."
- 후보: Package Name **`Platen.Notes`** 또는 `<핸들>.Platen`; Publisher는 자체 서명 인증서 CN(예: `CN=<사용자 이름>`)이므로 PFN 해시가 붙어 전역 충돌이 사실상 없음 [추정]. 비패키지(exe) 배포라면 AppUserModelID `Platen.Notes` + 레지스트리 `HKCU\Software\Platen\` 권장 [추정].
- 파일 연결 ProgID 후보: `Platen.Note.1` [추정, Windows 관례].

### 3.3 파일 확장자
- `.platen` 충돌: fileinfo/filext/file-extensions.org **접근 불가**. 검색 스니펫 **[2차·스니펫]**: `".platen" file extension` 검색에서 **정의된 .platen 확장자 없음**(유사: `.plt` AutoCAD 플롯, `.plat` Platinum Web Package, `.pln` ArchiCAD).
- 후보: **`.platen`**(가독성·충돌 미발견, 6자) — 권장. 대안 `.pltn`(4자, 미조사). 내부는 zip 컨테이너이므로 매직 바이트 `PK`이며 `mimetype` 엔트리를 첫 항목·무압축으로 두는 ODF/EPUB 관례를 따르면 식별이 쉬움 [추정].

### 3.4 MIME 타입
- RFC 6838 §3.2 벤더 트리(`vnd.`)·§4.2.8 구조화 구문 접미사 원문은 **열람 실패(ietf.org 차단)**. 종전 지식 **[추정]**: 벤더 트리는 `application/vnd.<producer>.<name>` 형식이며 IANA 등록은 권장이지만 미등록 사용도 관행상 흔함; `+zip` 접미사는 IANA 구조화 구문 접미사 레지스트리에 등록되어 있음(원문 미확인).
- 후보: **`application/vnd.platen.note+zip`**(권장, 생산자 `platen` + 문서 종류 `note`) 또는 `application/vnd.platen+zip`. 미등록 상태이면 Android 인텐트 필터·Windows 레지스트리 내부용으로만 사용하고, Drive 업로드 시에도 이 값을 `mimeType`으로 지정(Drive는 임의 MIME 허용, Discovery `accept: */*`) [1차·Discovery + 추정].

## 4. 판정

| 축 | 결과 | 근거 등급 |
|---|---|---|
| Google Play 동명 앱 | 미발견(스니펫) / 직접 확인 불가 | 2차 |
| Microsoft Store 동명 앱 | 미발견(1페이지 90건) | 1차 |
| iOS App Store | **동명·동장르("Platen: Paper & Ink", 타자기 노트, iOS 전용) 존재 보고** | 2차 |
| GitHub | 동명 리포 다수(타자기 메모 웹앱 2건 포함), `github.com/platen` 핸들 점유 | 1차 |
| 도메인 | 확인 불가; `platen.io`는 타 프로젝트 사용 추정 | 추정 |
| 패키지 레지스트리 | npm·PyPI·crates 모두 점유(무관 프로젝트) | 1차 |
| 상표 | 확인 불가(로컬 숙제); 보통명사라 분쟁 가능성 낮음 | 추정 |
| 확장자 `.platen` | 충돌 미발견 | 2차 |

**최종: 조건부 사용 가능.**
- 유지 조건: (a) 개인용·비배포 유지, (b) `applicationId`는 소유 도메인/GitHub 핸들 역순(`io.github.<핸들>.platen` 등)으로 확정, (c) 로컬 숙제에서 App Store "Platen: Paper & Ink"·도메인·KIPRIS/USPTO 확인 결과를 D-0xx로 기록.
- 재고 트리거: 공개 배포(스토어·웹사이트) 계획이 생기면 iOS 동장르 앱과의 혼동 가능성 때문에 **이름 재고 권고**(후보 풀은 D-002의 Escapement/Typebar/Pica 등 재활용).

## 5. 로컬 숙제(LOCAL_TODO 반영용)
1. Play 스토어·App Store(iOS)에서 "Platen" 검색 → "Platen: Paper & Ink" 개발사·출시일·카테고리 캡처.
2. WHOIS/RDAP: platen.app, platen.io, getplaten.com, platen.dev, platen.com.
3. KIPRIS(한글 "플래튼"·영문 "PLATEN", 9·42류), USPTO Trademark Search(9·42류) 각 1회.
4. 확장자 DB(fileinfo.com/extension/platen) 1회 확인.
5. `applicationId` 최종값과 Windows Package Name을 DECISIONS에 기록(OAuth 클라이언트 생성 전).

## 6. 출처 목록(열람일 2026-10-24)

**열람 성공 [1차]**
- https://github.com/search?q=platen&type=repositories — 359 results
- https://github.com/platenio/platen — MIT/CC-BY-4.0, 51★, website platen.io
- https://github.com/winchxyz/platen — MIT, 15★
- https://github.com/dandacompany/platen — MIT, 1★/52 forks
- https://github.com/platen — 개인 사용자 프로필
- https://www.microsoft.com/en-us/search/shop/apps?q=platen — 1,235건, 정확 일치 없음
- https://registry.npmjs.org/platen — platen@0.0.0 (MIT)
- https://pypi.org/pypi/platen/json — platen (Cariad Eccleston)
- https://crates.io/api/v1/crates/platen — platen 0.0.1 (2026-05-05)
- https://developer.android.com/build/configure-app-module — applicationId 규칙, Last updated 2026-02-26
- https://raw.githubusercontent.com/MicrosoftDocs/windows-dev-docs/docs/hub/apps/desktop/modernize/package-identity-overview.md — ms.date 2023-01-10
- https://www.googleapis.com/discovery/v1/apis/drive/v3/rest — mediaUpload accept `*/*`

**검색 스니펫만 [2차] / 열람 실패**
- https://maily.so/whattheapp/posts/32z836kvon4 — "Platen: Paper & Ink" iOS 노트앱 소개(열람 실패, 스니펫)
- https://flathub.org/ko/apps/page.wisha.platen — Linux "Platen" 수식 편집기(열람 실패, 스니펫)
- play.google.com, apps.microsoft.com, apps.apple.com, itunes.apple.com — 차단
- rdap.org, rdap.nic.google, rdap.verisign.com, rdap.nic.io, lookup.icann.org, cloudflare-dns.com — 차단
- tmsearch.uspto.gov, euipo.europa.eu, kpat.kipris.or.kr, branddb.wipo.int — 차단
- fileinfo.com, filext.com, file-extensions.org, en.wikipedia.org, ietf.org(RFC 6838), iana.org(+zip) — 차단

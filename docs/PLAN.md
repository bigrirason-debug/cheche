# PLAN — Platen 구현계획서

> 상태: **v1 초안 — 승인 대기** (2026-10-24). 승인되면 §9의 의존성과 §10의 확인 지점이 잠긴다.
> 근거 문서: `docs/BRIEF.md`(v3) · `docs/research/*.md`(8건 + feature-matrix) · `docs/DECISIONS.md`(D-001~D-013)
> 표기: **[사실]** 리서치에서 출처로 확인 · **[가정]** 가역적 빈칸, 틀리면 바꾼다 · **[미결]** 사용자 답 필요

---

## 1. 목표 · 완료 정의 · 제약

### 1.1 목표
굿노트·스타노트·삼성노트·Notes+를 분해해 원리를 가져온, 타자기 구조 은유의 손글씨 노트앱. 차별점은 **Procreate·Photoshop 브러시 파일을 내 브러시로 변환**하는 것.

### 1.2 1차 출하 완료 정의 (BRIEF §3 그대로)
> 갤럭시 탭 S9+에서 Google 로그인 후, 지연은 삼성노트·필압/매끈함/끝처리는 스타노트 수준(사용자 조절 가능)으로 빈 속지나 PDF 위에 필기하고(펜·만년필·연필·형광펜·지우개·올가미·되돌리기), Procreate·Photoshop 브러시 파일을 가져와 내 브러시로 쓸 수 있으며, 그 노트가 Drive로 동기화되어 Windows 앱에서 동일하게 보이고 올가미 이동·삭제·페이지 관리·텍스트·이미지 편집이 되며, PDF로 내보낼 수 있다. (+ 표 삽입 · 레이저 포인터 · 설정 화면)

**수치 합격선**(각 마일스톤 게이트에서 측정):

| 축 | 합격선 | 기준 출처 |
|---|---|---|
| 펜 다운→첫 픽셀 지연 | 같은 방법으로 잰 **삼성노트 중앙값 + 8ms 이내**, 절대 ≤30ms(프론트버퍼 on) | stack.md §5-3 M1 — 절대값 근거 문헌은 미열람이라 **상대값이 1차 기준** |
| 유효 입력 샘플 | 히스토리 포함 ≥240Hz, 필압 고유값 ≥1,024단계 | stack.md M2·M3 |
| 팜 리젝션 | 손바닥 접촉 20회 중 오획 잔존 0 | stack.md M6 |
| 1만 획 로드 후 팬/줌 | 프레임 p95 ≤8.3ms, 드롭 ≤5% (Android 120Hz) / p95 ≤16.7ms (Windows) | stack.md M7·M10 |
| Android↔Windows 렌더 일치 | 동일 획 → PNG 픽셀 diff ≤2%, 구조적 차이 0 | stack.md M11 |
| 필기 파라미터 | 필압 커브(제어점 2~8) · 스무딩 0~1 · 테이퍼 길이 px 가 설정 화면에서 조절되고 브러시별 오버라이드 가능 | D-012, brush-formats §3.6 |
| 브러시 임포트 | 사용자 제공 샘플 세트에서 Procreate·.abr 각각 **팁 이미지·간격·이름 100% 복원**, 동역학은 손실표대로 복원하고 `lossNotes`에 기록 | brush-formats §3.3 |
| 동기화 | 기기 A 저장 → 기기 B 폴링 1회(≤60s) 내 반영, 충돌 시 "충돌 사본" 생성, 데이터 손실 0 | sync-google §5 |
| 다크 모드 | 검정 잉크 자동 재매핑, 형광펜 가시(다크에서 Screen 혼합), 빨강 포인트 대비 ≥4.5:1 | typewriter-refs §7 |
| 접근성 | 보이는 아이콘 전부 툴팁+접근성 이름, 터치 타깃 ≥48dp / Windows ≥40epx | BRIEF §7 |

### 1.3 제약
- 클라우드 환경: 실기기·로컬 파일 접근 불가, **KVM 없음**(에뮬레이터 불가), **네트워크 정책이 대부분 외부 호스트 차단**(열림: developer.android.com, raw.githubusercontent.com, Maven Central/pub.dev/nuget). Windows 산출물은 이 컨테이너에서 불가 → GitHub Actions `windows-latest`. APK는 Android cmdline-tools 설치 승인 후 가능 **[사실, stack.md §1·§3-7]**.
- 타사 코드·에셋 복사 금지. GPL/AGPL/CC-NC 파서는 원리만. 디컴파일 유래 정보는 설계 근거로 쓰지 않는다(§2.2).
- 의존성·설치·스펙 변경은 승인 후에만.

## 2. 가정과 미결 (사실과 분리)

### 2.1 가정 [가정] — 승인 시 함께 채택, 틀리면 DECISIONS로 수정
| # | 가정 | 근거/영향 |
|---|---|---|
| A1 | Notes+ 기준 앱 = **Singfish "Notes+ : Notes for Pencil"**(id1609361989). Viet Tran의 Notes Plus X에는 연필·특수선이 없다 | rea-notesplus §0 |
| A2 | Android `applicationId` = `io.github.bigrirason_debug.platen` (GitHub 핸들 역순; 하이픈은 밑줄로). Windows 패키지명 `Platen.Notes`. 확장자 `.platen`, MIME `application/vnd.platen.note+zip` | naming.md §3. **OAuth 클라이언트 생성 전에 확정 필요** |
| A3 | Drive scope = `drive.file`(+`drive.appdata` 선택). 같은 Cloud 프로젝트의 Android·데스크톱 클라이언트가 서로 만든 파일을 볼 수 있다 | sync-google §3. 틀리면 R2 완화책 |
| A4 | OAuth는 Testing으로 시작 → M0 끝나면 **In production 전환**(비민감 범위라 검증 불필요) | sync-google §0 |
| A5 | 한 시점 한 기기 편집. 충돌 = 마지막 쓰기 승리 + 충돌 사본 | D-005 |
| A6 | Procreate 크기 상수 K=100px, 테이퍼 상수 L=300px — 샘플 1회 측정으로 보정 | brush-formats §7 |
| A7 | 표 = 행·열·선 스타일·셀 크기 조절 가능한 벡터 객체(1~50행·열, 스타노트 기준). 레이저 포인터 = 선/점 모드·굵기·지속시간(기본 1s)·색 조절 | D-013, rea-starnote |
| A8 | 1차 내장 브러시 로스터: 펜 2·만년필 2·연필 2·모노라인 1·형광펜 2(총 9) + 사용자 임포트. 특수선·수채는 후속 | Notes+ 구조 참고, 범위 절제 |
| A9 | Windows 노트북은 터치스크린 **없을 수도 있음** → Windows 앱은 마우스만으로 전 기능 동작, 터치는 보너스 | D-003 |
| A10 | "내 맘대로 조정 가능한 설정" = 앱 전체 설정 화면(필기 파라미터·제스처 매핑·UI 밀도·종이 색·기본 펜) | D-013 |

### 2.2 미결 [미결] — 답을 기다리는 중 (HANDOFF 참조)
1. 네트워크 정책 개방 여부 → 개방 시 "(스니펫)" 항목 2차 검증 패스.
2. A1 확인.
3. 스타노트·삼성노트 문서의 **디컴파일 유래 항목** 삭제 여부(권장: 삭제). 어느 쪽이든 설계 근거로는 쓰지 않는다 — 본 계획서는 공식 API·공개 스펙·스토어/리뷰만 근거로 작성했다.
4. A2 `applicationId` 확정.

## 3. 스택 결정 (스파이크 조건부)

**추천: B. Kotlin Multiplatform + Compose Multiplatform** (Android + Desktop/JVM Windows) — stack.md §4 가중 80/100.
**대안: C. 네이티브 2개**(Kotlin/Compose + androidx.ink ↔ WinUI 3 C# + SkiaSharp) — 76/100.

근거 **[사실]**:
1. 최대 가중 축(입력·저지연)에서 B는 `PointerEvent.motionEvent`로 필압·틸트·방위·호버·옆버튼·`FLAG_CANCELED`·히스토리 샘플 전부 접근 + `AndroidView`로 `graphics-core` 프론트버퍼 직접 사용. Flutter는 히스토리 샘플·팜 플래그가 엔진 수준에서 막혀 있고 S펜 버튼 이슈(#42846)가 2019년부터 열려 있음.
2. androidx.ink 렌더러는 Windows JVM 타깃이 없어 어차피 자체 렌더러가 필요 → B는 commonMain 1벌로 양 플랫폼 동일 코드 경로(렌더 일치 요구 D-009 충족). 코드 공유율 추정 60~75%.
3. 브러시 파서(zip+bplist+abr)·동기화·포맷·UI를 Kotlin 1벌로.

**리스크**: Compose Desktop **Windows 터치** 제스처 결함 이력(#5278), Android/Desktop Skia 버전 상이 → M0에서 측정.

**전환 규칙**(stack.md §5-4): M1·M2·M6 중 하나라도 불합격 → 측정 오류 배제 후 스택 재검토. **M10(Windows 편집) 불합격 → C로 전환**(Android 코드 유지, Windows만 WinUI 3 + SkiaSharp). M11(렌더 일치) 불합격 → 공통 렌더러 설계 수정(스택 유지).

## 4. 아키텍처

### 4.1 모듈 (Gradle, KMP)
```
platen/
  core-model/      commonMain  문서 모델(노트·페이지·획·객체), .platen 컨테이너 읽기/쓰기, 스키마 버전
  core-ink/        commonMain  입력 샘플 → 스트로크 지오메트리: 스무딩(streamline/stabilize), 필압 커브(LUT), 속도·틸트 매핑, 테이퍼, 폭 프로필 → 외곽선/메시; 스탬프 배치
  core-render/     commonMain  Compose Canvas 렌더러(profile/stamp 엔진), 셰이더 expect/actual(AGSL/SkSL), 타일 캐시, 다크 모드 색 재매핑, 형광펜 혼합
  brush/           commonMain  brush.json 모델, 내장 로스터, 임포터(procreate/, abr/), 이미지 정규화, 프리컴포즈
  input/           androidMain 펜·손가락·팜 정책, 제스처 매핑 테이블 엔진(commonMain) + Android MotionEvent 어댑터, 프론트버퍼 호스트(graphics-core), 예측(motionprediction)
  pdf/             expect/actual  Android: PdfRenderer(렌더) + pdfbox-android(내보내기) / Desktop: PDFBox 3
  sync/            commonMain  Drive REST 클라이언트(Ktor), 변경 폴링, 충돌 처리, 직렬 업로드 큐 / auth expect/actual(Android Credential Manager+AuthorizationClient, Desktop 루프백+PKCE)
  ui/              commonMain  Compose Multiplatform 화면: 홈(노트북 서랍), 캔버스 셸(페이퍼 베일 툴바·마진 스톱·캐리지 페이지 스트립), 펜 설정, 설정, 임포트; 디자인 토큰
  app-android/     APK
  app-desktop/     Windows exe/msi (jpackage)
  spike-android/, spike-desktop/   M0 전용, M0 후 보존(회귀 측정용)
  tools/           골든 렌더 비교 스크립트, 합성 획 생성기
```
**파일 소유 경계**: 모듈 = 소유 단위. `core-model` 스키마 변경은 head만. 교차 변경은 PR 단위로 head가 병합.

### 4.2 입력 레이어
- 펜: `MotionEvent` 히스토리 샘플 포함 수집 → `(x, y, pressure, tiltX, tiltY, t)` → `core-ink`. `requestUnbufferedDispatch` + 프론트버퍼 레이어에 "젖은 잉크", 확정 시 `core-render`로 "마른 잉크"(stack.md §4 설계 전제). 예측은 **기본 OFF**, 설정에서 on(이전 실측: 밀림 체감).
- 손가락: 팬/줌 전용 + "손가락 필기" 토글. 펜 호버/접촉 중 손가락 이벤트 무시. `FLAG_CANCELED`/`ACTION_CANCEL` 수신 시 해당 획 롤백.
- 제스처 매핑 테이블: `{입력: 두손가락탭|세손가락탭|두손가락스와이프L/R|옆버튼+긋기|옆버튼클릭|옆버튼누른채} → {동작: undo|redo|지우개|올가미|이전도구|페이지이동|레이저|없음}`. 기본값 = 이전 앱 관례. 굿노트엔 이런 테이블이 없음 **[사실]** → 차별점.

### 4.3 잉크 코어
- 스트로크 = 원시 샘플(보존, 손글씨 인식 후속 대비) + 파라미터 스냅샷(브러시 id + 사용자 오버라이드) → 렌더 시 재계산 가능. 지오메트리 캐시는 파생물.
- 사용자 파라미터 3종(필압 커브·스무딩·테이퍼)은 **앱 전역 기본 + 브러시별 오버라이드** 2층(brush-formats §3.4: 전역 필압 커브는 입력 보정으로 합성).
- 스타노트 관찰 **[사실, 스토어/리뷰 등급]**: 펜마다 노출 파라미터가 다름(만년필 = 압감·두께·안정화 / 연필 = 압감·두께·밀도). 우리도 브러시 정의가 "노출 슬라이더 목록"을 가진다.

### 4.4 브러시 엔진 + 임포터
brush-formats §3 초안 채택: `engine ∈ {profile, stamp}`, zip(`brush.json` + `tip.png` + `grain.png`), id = SHA-1 앞 12자, 노트에 사용 브러시 동봉. 임포터는 Procreate(zip → NSKeyedArchiver 해석 → 키 매핑 → 이미지 정규화)와 ABR(v1/v2 공식 경로, v6+ `8BIM` 워킹 → `samp`+`desc`). 손실은 `source.lossNotes`에 기록하고 임포트 미리보기에서 표시. 성능 상한: 팁 ≤512px, spacing ≥0.03, base-D 기준 간격. **축소안(B)**: 해독 실패 시 팁+간격+이름만.

### 4.5 문서 모델 · `.platen` 포맷 (공개 zip)
```
mimetype                      (첫 엔트리, 무압축: application/vnd.platen.note+zip)
manifest.json                 schema, minReader, noteId, title, created/modified, pages[], brushes[], assets[]
pages/0001.json               페이지 크기(pt)·배경(색|템플릿|pdf asset+pageIndex)·객체 목록(획 참조·텍스트·이미지·표·링크)
pages/0001.strokes            바이너리: 획 헤더 + float32 샘플 배열(x,y,p,tx,ty,t) — JSON 대비 크기 1/4 [가정]
brushes/<id>.zip              사용 브러시 동봉(자기완결)
assets/<sha1>.png|.pdf        이미지·원본 PDF
thumb.png
```
- **결정적 zip**(엔트리 정렬·타임스탬프 고정) → 같은 내용 = 같은 md5 → Drive 변경 감지에 사용(sync-google §5-4).
- 버전: `schema`(쓰기 버전)·`minReader`(읽기 최소). 모르는 필드 보존.
- 저장: **단일 직렬 큐 + 메인에서 대상 스냅샷 + 세대 가드**(iwhg 데이터 손실 교훈).
- 내부 좌표: pt(1/72in) 기준, 굿노트 기본 종이 455×588pt·비율 0.773 **[사실]** 을 기본 템플릿 중 하나로.

### 4.6 동기화
`drive.file` · `changes.getStartPageToken` → 60s 폴링(포그라운드) · resumable `files.update` · 업로드 전 `md5Checksum` 비교 → 충돌 사본 · 디바운스 30s · 리비전은 Drive 기본(30일/100개). Android 인증 = Credential Manager(1.6.0) + AuthorizationClient, Desktop = 루프백+PKCE(`google-auth-library-oauth2-http`). Drive 호출은 Ktor 직접 HTTP(엔드포인트 6~7개) **[가정, sync-google §6 권장]**.

### 4.7 내보내기
PDF: 원본 PDF 페이지 위에 획을 **벡터 경로(profile 엔진)** 또는 **래스터 레이어(stamp 엔진)** 로 합성. Desktop = PDFBox 3, Android = pdfbox-android(정체 플래그) — 공통 인터페이스 `PdfExporter` expect/actual. 이미지(PNG) 내보내기는 렌더러 공용.

### 4.8 UI 셸 (타자기 구조 은유, typewriter-refs §8)
- 페이퍼 베일 = 툴바 프레임 + 눈금자, 그 위 **마진 스톱 마커 2개**(줄 가이드·속지 여백), 라인 스페이스 3단.
- 캐리지 = 페이지 스트립(좌우 페이지·노트 이동). 페이지 피드는 페이지 전환 시 시각 암시만(모션 짧게, reduced-motion 예외).
- 2색 리본 = 색 시스템(검정 잉크 + 빨강 단일 포인트) + 펜 검정/빨강 즉시 토글.
- 크롬 다크 ↔ 종이 다크 분리. 장식은 크롬, 종이는 깨끗.
- 펜 설정 동선: 선택 도구 아이콘 재탭 = 세부 설정(굿노트·삼성 공통 관례 **[사실]**). 펜꽂이 슬롯 + "기본 펜 고르기" 시트 2층(Notes+ 구조).

### 4.9 디자인 토큰
`docs/design/DESIGN.md` — typewriter-refs §8.3 초안을 출발점으로: A안 Courier Prime + Pretendard + D2Coding / B안 IBM Plex Mono + IBM Plex Sans KR + D2Coding(전부 OFL). 빨강 `#B3261E`/`#C8102E`(라이트)·`#F26B5E`(다크). 간격 4 기반, 아이콘 24, 타깃 48dp.

## 5. 마일스톤 (실행 모드: 혼합 — M0·M1 manual, M2~ auto, 실기기 판정은 항상 멈춤)

각 M의 끝 = 빌드 산출물 1개 + 측정 기록 + 사용자 실기기 체크리스트. 산출물은 GitHub Actions Artifacts(APK: ubuntu 러너 또는 컨테이너, Windows: windows-latest).

| M | 이름 | 산출물 | 완료 기준(측정 가능) | 검증 방법 | 사용자 체크리스트 |
|---|---|---|---|---|---|
| **M0** | 스택 스파이크 **[manual]** | `spike-android.apk`, `spike-desktop.msi/exe`, CI 워크플로 2개 | stack.md §5-3 M1~M11 합격선. 자동 통과 항목: 빌드 성공, 1만 획 합성 로드, 렌더 일치 diff 스크립트 동작 | CI 로그 + 사용자 측정표(§5-4 양식) | APK 설치 → 토글 4종(FB/예측/ink/1만획) 테스트, 240fps 촬영 10회, CSV·영상 업로드; Windows: 올가미·핀치·1만 획 |
| **M1** | 디자인 **[manual]** | `DESIGN.md` v1, 시안 A/B × 3화면(홈·캔버스·펜 설정) PNG(+HTML 소스) `docs/design/mockups/` | 토큰 전수(색·서체·간격·라운드·컴포넌트) + 근거, 다크 변형 포함, 대비값 표 | 대비 계산 스크립트, 사용자 선택 → 정제 1회 → 잠금 | 시안 비교 후 A/B 선택, 수정 요청 1라운드 |
| **M2** | 잉크 코어 + 문서 모델 | `platen-m2.apk` | 펜·지우개·undo/redo 동작, 필압 커브·스무딩·테이퍼 설정 반영, `.platen` 저장/로드 라운드트립(바이트 동일), 골든 렌더 테스트 통과, 1만 획 p95 ≤8.3ms(실기기) | 단위 테스트(지오메트리·포맷), 골든 PNG, 실기기 프레임 통계 | 필기감 1차 판정: 스타노트와 같은 문장 나란히 쓰고 스크린샷(필압 3단계·지그재그) |
| **M3** | 브러시 엔진 + 임포터 | `platen-m3.apk` | 내장 로스터 9종 렌더, Procreate·ABR 샘플 세트 임포트(팁·간격·이름 100%, 손실표 일치), 임포트 미리보기·lossNotes 표시, 성능 상한 동작(512px·2% 팁에서 드롭 ≤5%) | 샘플 기반 테스트(사용자 샘플 없으면 합성 샘플로 구조 테스트 + 플래그), 프레임 측정 | 본인 브러시 파일 임포트 → 획 비교 스크린샷 |
| **M4** | UI 셸 + 입력 정책 + 제스처 + 설정 | `platen-m4.apk` | DESIGN 토큰 적용 셸(툴바·마진 스톱·페이지 스트립·노트북 서랍), 손가락 정책·옆버튼·팜 롤백 동작, 제스처 매핑 설정 UI, 설정 화면(필기 파라미터·제스처·UI), 툴팁·접근성 이름 100%, 다크 모드 | 접근성 스캐너(Compose semantics 테스트), 수동 체크리스트 | 제스처 바꿔보기, 팜 20회, 다크 전환 |
| **M5** | 객체 도구 + PDF + 템플릿 | `platen-m5.apk` | 올가미(선택·이동·복사·삭제·색/크기), 텍스트 박스, 이미지, **표**(행열·선·셀 조절), **레이저 포인터**(모드·지속·색), PDF 가져오기(배경 렌더, 하이퍼링크 탭 이동), PDF 내보내기(벡터 획 유지), 내장 속지 ≥6 + 표지 ≥3, 스티커(PNG 알파) | 내보낸 PDF를 PDFBox로 재파싱해 획 수·링크 검증, 플래너 PDF 링크 테스트 | 하이퍼링크 플래너 1개 열어 링크 동작, 내보낸 PDF를 노트북에서 열기 |
| **M6** | Windows 앱 | `platen-m6.msi` (+ `platen-m6.apk` 동기 버전) | 같은 `.platen`이 Android 렌더와 diff ≤2%, 올가미 이동·삭제, 페이지 추가·삭제·순서, 텍스트·이미지 삽입, PDF 내보내기, 마우스만으로 전 기능 | 골든 diff CI(양 플랫폼 PNG), 수동 | 노트북에 설치(SmartScreen 경고 캡처), Android 노트 파일 수동 복사해 열기 |
| **M7** | Google 로그인 + Drive 동기화 | `platen-m7.apk` + `.msi` | 양 플랫폼 로그인, 노트 업로드/다운로드, A→B 반영 ≤60s, 충돌 사본 생성, 오프라인 편집 후 복귀 동기화, 직렬 큐 영속화, 데이터 손실 0(100회 저장 폭주 테스트) | 통합 테스트(모의 Drive 서버) + 실계정 | OAuth 로컬 숙제 선행. 두 기기 교차 편집 시나리오 5개 |
| **M8** | 마감 · 1차 출하 | `platen-1.0.apk` + `.msi` + 릴리스 노트 | §1.2 합격선 전수 재측정, 크래시 0(1시간 사용), 접근성 100%, reduced-motion, 성능 회귀 없음 | 전 게이트 재실행 | 1차 출하 판정 체크리스트 서명 |

후속(2차): .sdocx 읽기/쓰기, .goodnotes 읽기(부분), 손글씨 인식, 특수선·수채, 탭 스톱, 펜 더블탭 제스처, 스탬프 엔진 확장(컬러 다이내믹스 제외 유지).

## 6. 디자인 단계 (M1 상세)
1. `DESIGN.md` v0: typewriter-refs §8.3 토큰 + 컴포넌트 목록(툴바·펜꽂이·마진 스톱·페이지 스트립·서랍 카드·설정 행·시트) + 근거(대비·타깃·서체 라이선스).
2. 시안: A(실사 — 재질은 **임시 플레이스홀더 색면**으로, 실제 이미지 에셋은 사용자 촬영/구매 전제이므로 "에셋 슬롯" 표시) / B(미니멀). 각 홈·캔버스·펜 설정 3화면 × 라이트/다크. HTML/SVG로 작성 → Playwright(기설치)로 PNG 렌더.
3. 사용자 선택 → 정제 1라운드 → `DESIGN.md` v1 잠금. 이후 변경은 DECISIONS.

> A안 주의: "재질은 진짜 이미지 에셋이 아니면 넣지 않는다" 규칙상, A안 채택 시 **에셋 제작·구매가 사용자 숙제**가 된다. 시안에서 이 비용을 명시한다.

## 7. 리스크 레지스터

| # | 리스크 | 영향 | 징후 | 완화 / B안 |
|---|---|---|---|---|
| R1 | Compose Desktop Windows 터치/편집 결함 | 높 | M0 M10 불합격 | C안(WinUI 3 + SkiaSharp)로 Windows만 전환, 공통 모듈 유지 |
| R2 | 프론트버퍼/히스토리 샘플이 기대와 다름 | 높 | M0 M1·M2 불합격 | 측정 재현 → ink-authoring 경로 대조 → 스택 재검토 |
| R3 | Procreate/ABR 해독 실패·단위 오차 | 중 | M3 샘플 테스트 실패 | 축소안(팁+간격+이름), K·L 상수 사용자 보정 UI |
| R4 | `drive.file` 앱 경계가 클라이언트 단위 | 높 | M7에서 B가 A 파일을 못 봄 | 데스크톱 클라이언트 공용 또는 `drive.appdata` 병행 |
| R5 | OAuth Testing 7일 만료 | 중 | 주기적 재로그인 | In production 전환(A4) |
| R6 | 컨테이너 네트워크 차단으로 리서치 등급 저하 | 중 | "(스니펫)" 항목 오류 | 개방 시 2차 검증; 설계는 공식 API 등급 항목에만 의존 |
| R7 | APK를 컨테이너에서 못 만듦 | 중 | SDK 설치 거부 | GitHub Actions ubuntu 러너(SDK 기설치)로 전부 빌드 |
| R8 | 스탬프 엔진 성능(큰 팁·작은 간격) | 중 | M3 드롭 >5% | 상한 강제 + GPU 셰이더 경로 |
| R9 | 이름 충돌(iOS "Platen: Paper & Ink") | 낮(개인용) | 공개 배포 시 | 배포 계획 생기면 재명명 |
| R10 | pdfbox-android 정체(2023) | 낮 | Android 내보내기 버그 | Android도 PDF 내보내기를 Desktop/공통 코드로 이관 검토, 또는 androidx.pdf 성숙 대기 |
| R11 | 디컴파일 유래 정보 오염 | 낮 | — | 설계 근거에서 배제(§2.2), 사용자 판정 후 문서 정리 |

## 8. 사용자 확인 지점
1. **본 계획서 승인**(§2.1 가정·§9 의존성 포함)
2. M0 결과로 **스택 확정**
3. M1 **디자인 방향 선택** + 잠금
4. 각 M의 **실기기 판정**(M2~M8)
5. 의존성 추가(§9 밖의 것)
6. A2 `applicationId` 확정(OAuth 클라이언트 생성 전, M7 이전)

## 9. 의존성 목록 (승인용)

### 9.1 컨테이너 설치
| 항목 | 용도 | 비고 |
|---|---|---|
| Android cmdline-tools + platform-tools + platforms;android-35 + build-tools;35.x | APK 빌드(스파이크·M2~) | 약 1~2GB. 거부 시 R7 경로(CI 전용) |

### 9.2 라이브러리 (전부 Apache-2.0/MIT/BSD, 버전은 stack.md §6·sync-google §6 기준)
| 이름 | 버전 | 라이선스 | 용도 |
|---|---|---|---|
| Kotlin | 2.4.21 | Apache-2.0 | 언어 |
| Compose Multiplatform | 1.12.2 | Apache-2.0 | UI |
| androidx.compose.* / activity-compose / lifecycle | 최신 안정 | Apache-2.0 | Android UI |
| androidx.graphics:graphics-core | 1.0.4 | Apache-2.0 | 프론트버퍼 |
| androidx.input:input-motionprediction | 1.0.0 | Apache-2.0 | 예측(옵션) |
| androidx.ink:* | 1.0.0 | Apache-2.0 | M0 대조군(제품 채택은 M0 후 결정) |
| androidx.credentials + credentials-play-services-auth + googleid | 1.6.0 / 최신 | Apache-2.0 | Android 로그인 |
| play-services-auth (AuthorizationClient) | 최신 | Android SDK 라이선스 | Drive 토큰 |
| com.google.auth:google-auth-library-oauth2-http | 1.54.0 | BSD-3 | Desktop 루프백+PKCE |
| Ktor client (cio/okhttp) + kotlinx.serialization + kotlinx.coroutines | 최신 안정 | Apache-2.0 | HTTP/JSON/비동기 |
| Apache PDFBox | 3.0.8 | Apache-2.0 | Desktop PDF |
| pdfbox-android | 2.0.27.0 | Apache-2.0 | Android PDF 내보내기(정체 플래그) |
| dd-plist | 1.30+ | MIT | Procreate bplist |
| kotlin.test / JUnit / Compose UI test | — | — | 테스트 |
| Skiko(CMP 포함) | 0.156.x | Apache-2.0 | Desktop Skia |
| 폰트: IBM Plex Mono · IBM Plex Sans KR · Courier Prime · Pretendard · D2Coding | — | OFL 1.1 | 시안·UI(방향 확정 후 하나만 번들) |

**참고 전용(의존 아님)**: ag-psd `abr.ts`(MIT), aumlette 스키마(MIT). **열람만·코드 금지**: GIMP·Krita(GPL).
**CI**: GitHub Actions `ubuntu-24.04`(APK) · `windows-latest`(msi/exe). Secrets: `PLATEN_WEB_CLIENT_ID`, `PLATEN_DESKTOP_CLIENT_ID/SECRET`, `PLATEN_RELEASE_KEYSTORE_BASE64`, `PLATEN_KEYSTORE_PASSWORD`.

## 10. 작업 분할 (병렬 단위 · 파일 소유)

| 단위 | 소유 디렉터리 | 선행 | 병렬 가능 |
|---|---|---|---|
| U1 빌드·CI 골격 + 스파이크 | `/`, `.github/`, `spike-*` | 승인 | — |
| U2 잉크 코어·렌더러 | `core-ink/`, `core-render/`, `tools/` | M0 | U3·U4·U6과 |
| U3 문서 모델·포맷 | `core-model/` | 승인 | U2와 |
| U4 브러시 임포터 | `brush/` | U3 스키마 | U2와 |
| U5 UI 셸·디자인 | `ui/`, `docs/design/` | M1 | U2~U4와 |
| U6 동기화·인증 | `sync/` | U3 | U2·U4·U5와 |
| U7 PDF | `pdf/` | U3 | U5·U6과 |
| U8 Windows 앱 | `app-desktop/` | U2·U3·U5 | U6·U7과 |

교차 규칙: `core-model` 스키마는 head만 변경. 각 단위는 자기 모듈의 테스트를 소유. 공통 테스트 벡터(`tools/golden/`)는 U2가 소유하고 U8이 소비.

## 11. 승인 요청
이 계획서를 승인하시면: §9.1 설치 → U1(빌드 골격·CI·스파이크) 착수 → M0 산출물. §2.1 가정은 승인과 함께 채택되며, §2.2 미결 4개는 답이 오는 대로 DECISIONS에 기록합니다.

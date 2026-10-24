# REA — GoodNotes(굿노트) 공개 자료 리버스 엔지니어링 분석

> 프로젝트: Platen · 작성일/열람일: **2026-10-24** · 범위: BRIEF §2 REA ①~④ (기능·UX / 필기감·잉크 / 파일 포맷 / 템플릿 생태계)
> 규칙: 공개 자료만. 코드·에셋 복사 없음 — 가져가는 것은 **원리와 수치**.

## 0. 읽기 전에 — 열람 방식 고지와 신뢰 등급

**열람 방식 고지 (네트워크 정책, 2026-10-24 확인)**
- **차단된 호스트(WebFetch 실패, 재시도 안 함)**: support.goodnotes.com · goodnotes.com(블로그·가격·파트너) · feedback.goodnotes.com · goodnotes-team.notion.site · apps.apple.com · play.google.com · apps.microsoft.com · apkmirror.com · reddit.com · youtube · namu 등 대부분의 외부 호스트.
- **열람 가능**: WebSearch(검색엔진이 반환한 원문 **스니펫**) · github.com 리포 페이지·위키 · **raw.githubusercontent.com**(README·FORMAT.md·wiki 원문).
- 따라서 Goodnotes 공식 도움말·블로그·포럼 항목은 전부 **스니펫 기반**이다: 인용된 문장 범위 안에서만 유효하며, 원문 전체(스크린샷·표·수치 목록)는 보지 못했다. 오픈소스 파서의 포맷 문서는 **원문을 직접 읽었다**.
- 웹 검색 예산(세션당 200회)을 소진해 일부 항목은 **"자료 부족"**으로 남겼다. 후속 세션에서 보강 가능.

**신뢰 등급 표기**
- **공식 문서** = Goodnotes 도움말·블로그·포럼의 Goodnotes 직원 답변 / **1차 관찰** = 오픈소스 파서가 실제 파일을 분석해 문서화한 것, 또는 사용자가 직접 기기에서 본 보고 / **2차 글** = 리뷰·블로그·APK 미러 / **추정** = 자료에서 논리적으로 유도.
- 열람 방식: **직접** = 원문을 열어 읽음(GitHub) / **(스니펫)** = 검색 결과 발췌로만 확인.

---

## 1. 앱 정체·버전

| 플랫폼 | 관찰 | 수치 | 출처·등급 |
|---|---|---|---|
| iPadOS/iOS | 2025년 9~10월 "차세대(next-generation) Goodnotes"로 메이저 개편. 공식 버전 명칭 없이 출시("Goodnotes 7" 표기는 쓰지 않음), 사용자는 7.0.x로 부름. 플로팅 툴바·새 오브젝트 메뉴·Shapes·Whiteboard·Text Documents·AI 추가 | 툴바 커스터마이즈 추가 = **7.0.14** | AlternativeTo 2025-10 기사 [S-31] 2차·(스니펫) / feedback 포럼 Goodnotes 공지 [S-27] 공식·(스니펫) |
| iOS 현재 버전 번호 | **자료 부족** (App Store 직접 열람 불가) | — | — |
| Android | 스토어 명칭 "Goodnotes: AI Notes, Docs, PDF", 패키지 `com.goodnotes.android.app`. 2026-01-13 **2.0.0** 업로드 이후 2.0.x 연속 배포 | 최신 **2.0.9 (v320533)** 2026-08-25 업로드, arm64-v8a / **Android 9.0+**; 구형 기기용 2.0.3(v290970) 최소 Android 7.0. Play 스토어 "업데이트 2026-08-19" | APKMirror [S-29][S-30] 2차·(스니펫) |
| Windows | Microsoft Store 배포. 기술적으로 **PWA**(웹앱 패키징). Windows 11 **Ink API**로 포인트를 OS 컴포지터에 직접 전달해 브라우저 렌더 루프를 우회(지연 감소) | — | PWABuilder 블로그 2024-01 [S-33] 2차·(스니펫) / productivitybrief 2026 가이드 [S-34] 2차·(스니펫) |
| Windows 현재 버전 번호 | **자료 부족** | — | — |
| 공식 입장 | "Android·Windows·Web은 iPad의 Goodnotes 6 전체 기능을 제공하지 않음, 계속 추가 중". 기능 가용성은 플랫폼·기기·계정·플랜에 따라 다름 | — | goodnotes.com/windows [S-35], 공식 FAQ 7378735557519 [S-02] 공식·(스니펫) |

### 1.1 Android/Windows판이 iOS 대비 빠진 것 (Platen 1차 플랫폼 관점)

| 항목 | 상태 (2026-10 기준 공개 자료) | 출처·등급 |
|---|---|---|
| 레이저 포인터 | **iOS 전용**. macOS·Android·Windows·Web 미지원 | Presentation Mode 도움말 [S-09] 공식·(스니펫) / 교육용 도움말 [S-10] 공식·(스니펫) |
| 줌 윈도우(Zoom Window) | iOS 전용 | 도움말 7353756826383 [S-28] 공식·(스니펫) |
| 전체 라이브러리 백업 | A/W/W에 없음 → 노트별 PDF·.goodnotes 수동 내보내기 | FAQ [S-02] 공식·(스니펫) |
| 커스텀 템플릿 크기·테마 생성 | Apple 전용(A/W/W는 Apple에서 만든 Dynamic Template 노트북 열기만 가능) | FAQ [S-02] 공식·(스니펫) |
| Apple 기기 노트 동기화 | 라이브러리가 iCloud Sync가 아닌 **Goodnotes Cloud**일 때만 A/W/W에 보임 | FAQ [S-02] 공식·(스니펫) |
| PDF 하이퍼링크 | iOS는 탭으로 바로 이동, A/W/W는 **Read-only 모드 진입 후 탭** (베타 시절엔 아예 미지원) | 도움말 4406218761231 [S-20] 공식·(스니펫) |
| 툴바 커스터마이즈 | 2025 Q4 Apple 선행 → **2026 1~3월 A/W/W 도입** | 블로그 2026-03 라운드업 [S-23] 공식·(스니펫) |
| 아웃라인(Outline) | 2026 1~3월 A/W/W 도입 | [S-23] 공식·(스니펫) |
| 멀티탭·Smart Ink(손글씨 리플로우)·텍스트박스 스타일·Whiteboard 템플릿 가져오기 | 2026 4~6월 A/W/W 도입 | 블로그 2026-07-06 라운드업 [S-22] 공식·(스니펫) |
| 필압 | 가격 페이지에 "handwriting sensitivity: iOS only" 표기가 있다는 2차 보고와, Windows에서 Surface Pen 필압·틸트가 동작한다는 리뷰가 **상충** | [S-34] 2차·(스니펫) / everytalkin 리뷰 [S-36] 2차·(스니펫) → **LOCAL_TODO-1** |
| S Pen 옆버튼 | **Galaxy Store 빌드**: 버튼 길게 누른 채 긋기 = 지우개 / **Play Store 빌드**: 한 번 탭 = 지우개 전환, 다시 탭 = 이전 도구 복귀 (빌드별 동작 상이, 사용자 신뢰성 불만 다수) | feedback 포럼 Goodnotes 답변 [S-25] 공식·(스니펫) / goodnotes.com/partners/samsung [S-37] 공식·(스니펫) |
| 지우개 크기 | 조절 불가, **3단계** 고정(사용자 보고) | feedback [S-26] 1차 관찰·(스니펫) |
| Android 베타 시절 제약(참고) | PWA, Samsung 8"↑·RAM 3GB↑, 노트북 20개 제한, 세로모드 문제 — 현재는 네이티브 2.0.x로 전환된 것으로 보이나 **확인 자료 부족** | mynexttablet [S-38] 2차·(스니펫) |

---

## 2. 기능·UX 동선

### 2.1 툴바 배치
- **관찰**: 2025-09 개편으로 플로팅 툴바. 7.0.14(2026)부터 커스터마이즈: 메인 툴바 도구 숨김/표시, 드래그로 순서 변경, "Writing Tools" 패널 구성 변경, 메인 툴바에 Zoom Window·Time Keeper·Ruler 추가 가능, Shapes·Writing Tools 플로팅 메뉴를 화면 가장자리에 드래그하면 스냅, "기본 레이아웃 복원" 제공. A/W/W도 2026 1~3월부터 동일 기능.
- **레이저 포인터는 툴바 최우측 아이콘**, 가려지면 툴바를 스와이프해 노출(iPad 세로에서 숨겨질 수 있음).
- **기본 도구 순서(좌→우) 공식 기록 없음 → 자료 부족 (LOCAL_TODO-2)**.
- 출처: feedback 공지 [S-27] 공식·(스니펫), 레이저 도움말 7451632333327 [S-08] 공식·(스니펫), [S-23] 공식·(스니펫).

### 2.2 펜 설정 동선 — "선택된 도구 아이콘 재탭 = 세부 설정" 확인
- **관찰**: 형광펜 도움말 "Select the Highlighter tool and tap on the icon again to open the Highlighter settings". 올가미 도움말 "tap the tool twice and enable only the relevant option". 지우개(GN5) "double-tapping the eraser icon". 펜: 스트로크 프리셋(굵기 슬롯)을 선택한 뒤 **다시 탭**하면 굵기 값 조절. → **재탭 = 세부 설정 패턴이 전 도구에 일관**. 한 사용자는 개편 후 "펜 굵기가 두 번 탭 뒤로 들어갔다"고 불만.
- 출처: [S-05][S-06][S-07][S-03] 공식·(스니펫), feedback [S-39] 1차 관찰·(스니펫).

### 2.3 펜 종류·프리셋 수
| 항목 | 관찰 | 수치 | 출처·등급 |
|---|---|---|---|
| 펜 스타일 | **Fountain Pen**(필압 반응, 일상 필기) / **Ball Pen**(필압 무시, 일정 굵기, 끝 항상 둥긂) / **Brush Pen**(필압 강하게 반응, 레터링·드로잉) | 3종 | 도움말 7353756785679 [S-03] 공식·(스니펫) |
| 연필 | 별도 **Pencil tool** (흑연 질감; 펜 도구로는 불가). 사용자 보고: 틸트로 음영, 세울수록 가늘고 눕힐수록 넓고 옅음, 진하기 조절 불가 불만 | — | [S-03] 공식·(스니펫) / feedback [S-40] 1차 관찰·(스니펫) / 포맷상 연필 포인트에 `azimuth, altitude, force` 저장 [G-02] 1차 관찰·직접 |
| 선 스타일 | Solid / Dashed / Dotted | 3종 | [S-03] 공식·(스니펫) |
| 굵기 프리셋 | 컨텍스트 영역에 굵기 슬롯. 형광펜 도움말 "the first 3 slots save your desired stroke thickness" → 굵기 슬롯 **3개**(펜도 동일 구조로 보임: 추정) | 3 | [S-05] 공식·(스니펫) |
| 색 프리셋 | 기본 **3** 슬롯, 최대 **12** 슬롯까지 확장 ("3색 슬롯만 있을 땐 확장 옵션 없음") | 3→12 | [S-03] 공식·(스니펫) |
| 굵기 단위·범위 | mm 단위 표시(사용자 설정 예: 0.4/0.6/1.0 mm). **최소·최대값 공식 기록 없음 → LOCAL_TODO-3** | — | Lemon8 사용자 글 [S-41] 2차·(스니펫) |
| 설정 동기화 | 커스텀 색·펜 설정은 로그아웃/다른 기기 로그인 시 초기화(구 도움말) | — | [S-03] 공식·(스니펫) |

### 2.4 페이지·노트북·폴더 구조
- 라이브러리 = 컴퓨터 파일시스템식 계층. GN6 "Colored Folders": **폴더·하위폴더 무제한**, 색·아이콘 지정.
- 노트북 **내부에는 폴더 없음**(공식 답변: 여러 문서로 나누거나 북마크/아웃라인 사용). Quick Notes = 폴더 레벨에 노트북 밖으로 생성(GN5 리뷰, GN6 유지 여부 미확인).
- 2026 Q2: 문서가 **탭**으로 열리고 백그라운드 문서는 위치 기억.
- 포맷 관점: 페이지 순서는 `index.notes.pb` 나열 순서가 아니라 **이벤트 로그의 order key(ASCII 문자열 비교)** 로 결정됨(§7).
- 출처: Notion 도움말 [S-42] 공식·(스니펫), feedback 공식 답변 [S-43] 공식·(스니펫), MacStories GN5 리뷰 [S-44] 2차·(스니펫), [S-22] 공식·(스니펫).

### 2.5 올가미(Lasso)
- **선택 방식**: 기본 자유곡선, 사각 올가미 전환 가능. 대상 객체에 **살짝 닿기만 해도 선택**(인접 객체 오선택 방지 팁).
- **선택 필터**: 기본은 페이지의 모든 객체 유형 선택. 도구 **재탭** → 유형별 켜고 끄기(손글씨·이미지·텍스트박스·스티키노트 등).
- **오브젝트 메뉴**: Resize(크기·회전) / Color(편집 가능한 선택일 때만 표시) / Convert(손글씨 → 타이핑 텍스트 또는 수식; 변환 글자 크기 ≈ 원본, 색 = 첫 글자 색) / Crop(이미지만) / add element(Elements 컬렉션에 저장) / cut·copy·delete.
- **펜 제스처**: "Circle to Lasso"(펜으로 원을 그려 도구 전환 없이 선택), "Scribble to Erase"(Android 포함).
- 포맷 관점: 올가미 이동은 포인트를 다시 쓰지 않고 **트레일러에 (dx, dy) 오프셋**을 기록(Kaih1825 관찰) → 이동 비용 O(1).
- 출처: 도움말 7353695644175·10779390143247 [S-06][S-45] 공식·(스니펫), Notion Pen Gestures [S-46] 공식·(스니펫), [G-04] 1차 관찰·직접.

### 2.6 텍스트 박스 · 이미지
- 텍스트: 이동 가능한 박스. 서식 = 색·크기·폰트·스타일·정렬/간격·목록·박스 스타일. 대량 텍스트는 **페이지 경계를 넘지 않고 잘려 숨겨짐**. 2025-09 개편 후 폰트 툴바가 플로팅 팝업으로 이동, iOS 설치 커스텀 폰트가 피커에 안 뜨는 보고. 스타일이 박스 전체에 적용된다는 불만(부분 선택 서식 불가 보고). 텍스트박스 스타일링은 2026 Q2에 A/W/W 도입.
- 이미지: Photos/Files/카메라 삽입. 이동·잘라내기·복사·삭제·스케일/회전. 크롭은 **새 첨부를 기록**(비파괴 아님, 포맷 관찰). 사용자 불만: 리사이즈 핸들이 중심 기준, 회전만 따로 불가, 크롭 비율 프리셋 없음.
- 출처: Notion Text Tool·Image Tool [S-47][S-48] 공식·(스니펫), feedback [S-49][S-50] 1차 관찰·(스니펫), [G-01] 1차 관찰·직접.

### 2.7 도형 보정(동작 조건)
- **Draw and Hold**: Pen·Pencil·Highlighter로 **한 획**으로 그린 뒤 **끝에서 펜을 멈춰 누르고 있으면** 스냅. 누른 상태에서 **크기·각도 조절** 가능. 선 끝점끼리 스냅되어 **여러 획 삼각형도 한 도형**으로 인식. 형광펜 도형도 가능. 설정에서 끄기 가능(펜 설정 팝업).
- **Shapes 도구**(2025 개편): "Require Hold to Snap" 토글 — 끄면 홀드 없이도 수평/수직 직선. Fill Color는 Shapes 툴바 우측 Color 메뉴. 기존 도형을 올가미로 선택 후 Color 슬롯 탭 → 외곽선+채움 동시 변경.
- 사용자 보고: 빨리 그릴 때 오인식, 도형 종류 사전 선택 요구.
- 포맷 관점(어떤 도형이 "객체"로 존재하는가): 폴리라인 / 사각형(중심·크기, **모서리 반경은 짧은 변의 절반으로 클램프** → 큰 값이면 알약 모양) / 타원(중심·반지름·각도) / 직선·곡선(중간 핸들이 곡선 위에 놓임) / 엘보 커넥터(knee) / 화살촉 open·filled / 대시 (dash, gap). 도형 도구 스트로크는 **빈 경로 + 디스크립터**로 저장.
- 출처: 블로그 shape-recognition [S-51] 공식·(스니펫), 도움말 13682939148943·7353718215951 [S-52][S-53] 공식·(스니펫), feedback [S-54] 1차 관찰·(스니펫), [G-01] 1차 관찰·직접.

### 2.8 템플릿·표지 (요약; 생태계는 §8)
- 가져오기: 템플릿 크기·방향을 먼저 고르고 Files/Photos에서 추가. **이미지보다 PDF 권장**("A4 PDF를 넣어라, A4 픽셀 치수 이미지 말고"). 다중 페이지 PDF는 **첫 페이지만** 표지/속지로 들어감.
- GN6 템플릿 관리자: 그룹 생성 → 크기·색(가능한 경우)·방향 선택, "Custom" 크기(Apple 전용). 3자 가이드: 커스텀은 **inch/mm 입력**.
- 포맷 관점: 내장 속지도 **PDF 첨부**로 저장되며 PDF 크기는 **페이지 단위의 6/11**(= 72/132). Goodnotes Standard 예: PDF **455.04 × 588.45 pt**, 페이지 단위 **834.24 × 1078.825**. 색 코드 1=yellow, 2=white, 3=black.
- 출처: 도움말 360000115295·7353728093199 [S-55][S-56] 공식·(스니펫), Notion Custom Templates [S-57] 공식·(스니펫), omnisizes [S-58] 2차·(스니펫), [G-01] 1차 관찰·직접.

### 2.9 내부 링크
- **GN6 이상에서만 생성**, GN5는 열기만. 대상: 같은/다른 노트북·화이트보드의 **페이지**(페이지 내 위치·섹션 불가). **타이핑 텍스트에만** 링크 부착(손글씨는 변환 후). Text Documents·Study Sets 미지원.
- 포맷 관찰: 링크 URL 스킴 `https://app.goodnotes.com/documents/?anchor=<base64 "page-<page id>">#<base64 doc id>` — 즉 **웹 URL 형태로 문서 ID·페이지 ID를 인코딩**(앱 밖에서도 의미 있는 식별자).
- 출처: 도움말 7353743248143 [S-59] 공식·(스니펫), [G-01] 1차 관찰·직접.

### 2.10 검색(손글씨 OCR)
- 라이브러리 전체 검색 탭 + 문서 내 검색. 인덱스 대상: Pen 도구 손글씨, 타이핑 텍스트, **OCR 레이어가 있는 PDF 텍스트**(Goodnotes는 PDF OCR을 수행하지 않음), 스캔 문서, 폴더/문서 제목. 결과 섹션: Titles / Written Notes / PDFs / Document Outlines / Typed Notes.
- Apple 기기 인식 언어: 중국어(번체·간체)·네덜란드어·영어(US/UK)·프랑스어·독일어·이탈리아어·일본어·한국어·포르투갈어(PT/BR)·러시아어·스페인어·태국어·터키어 (**한국어 포함**). A/W/W는 별도 가이드(목록 미확인).
- 포맷 관찰: 인식 결과는 `index.search.pb` + `search/<id>`에 **페이지별 인식 그룹(필드 6) → 후보 텍스트(필드 3) + 위치** 형태로 캐시. 즉 **스트로크 원본과 별개 파일**에 보관, 스트로크 자체는 건드리지 않음 → Platen의 "1차 제외, 스트로크 보존" 방침과 호환되는 구조.
- 출처: 도움말 7353743594127·7353727932047 [S-60][S-61] 공식·(스니펫), Notion Search [S-62] 공식·(스니펫), [G-05] 1차 관찰·직접.

### 2.11 내보내기
- 형식: **.goodnotes / PDF / 이미지**. PDF는 **Flattened**(평면화; 인식된 손글씨 텍스트 레이어 포함 가능 = 검색 가능; 아웃라인·하이퍼링크 **유실** 보고) vs **Editable**(주석 레이어 유지, 용량 큼, 아웃라인 유지).
- 썸네일 뷰에서 여러 페이지 선택 → 내보내기(페이지 선택 내보내기).
- A/W/W: 전체 백업 없음 → 노트북 단위 PDF/.goodnotes 내보내기. 가져온 플래너를 Editable PDF로 내보내면 **하이퍼링크 대부분 누락** 버그 보고(iOS·A/W/W 모두).
- 출처: Notion Export [S-63] 공식·(스니펫), feedback [S-64][S-65] 1차 관찰·(스니펫), [S-02] 공식·(스니펫).

### 2.12 다크 모드의 잉크·형광펜 처리
- **관찰**: 앱 UI 다크 모드는 있으나 **종이·잉크는 자동 반전 안 함**(사용자: "인터페이스는 바뀌는데 페이지는 그대로 하얗다", "다크 모드로 바꾸면 펜 색이 틀어진다"). 2013년부터 요청 누적("True dark mode" 스레드). 공식 "잉크 자동 반전" 문서 **없음**. 해법은 검정 속지 선택(템플릿 색 코드 3=black) — 포맷상 검정 속지의 괘선 색은 **0.345/0.349/0.357 on 0.2 base**(밝은 종이 흰색 괘선 0.816/0.824/0.827).
- 형광펜 z-order: 사용자 요청 "형광펜이 항상 펜 아래로 가게 해달라(밝은 펜색이 탁해짐)" → **항상 뒤로 그리지는 않는 것으로 추정**. PDF 텍스트 롱프레스 하이라이트는 **항상 노랑**. 형광펜은 별도 펜 종류가 아니라 **스트로크 플래그 + 알파**(알파 < 0.95, 예 0.5)로 저장.
- 판정: Platen 요구(검정 잉크 자동 반전 + 형광펜 다크 가시성)는 **GoodNotes에 없는 것** → 차별 요소. 다만 공식 문서 직접 열람 불가 → 2차 확인은 **LOCAL_TODO-4**.
- 출처: feedback [S-66][S-67][S-68] 1차 관찰·(스니펫), 도움말 7353718215823 [S-05] 공식·(스니펫), [G-01][G-03] 1차 관찰·직접.

---

## 3. 제스처 설정 UX (중요)

### 3.1 설정 가능한 제스처 목록 — 결론: **매핑 테이블이 없다**
| 입력 | 동작 | 설정 가능? | 출처·등급 |
|---|---|---|---|
| 두 손가락 **더블탭** | Undo | **고정**. "현재 끄는 토글 없음"(공식 FAQ). iPad·Android·Windows 터치 공통 | 도움말 14069931315983 [S-01] 공식·(스니펫) |
| 세 손가락 **더블탭** | Redo | 고정. Windows "3·4손가락 제스처" 시스템 설정과 충돌 → OS 쪽 끄기 권고. 일부 Android 빌드도 자체 멀티핑거 제스처와 충돌 | [S-01] 공식·(스니펫) |
| 한 번 탭(2/3손가락) | 요청 다수(Procreate식) | 미도입 | feedback [S-69] 1차 관찰·(스니펫) |
| 손가락/펜 **롱프레스** | 팝업 메뉴(Zoom 등) | — | [S-28] 공식·(스니펫) |
| 펜 **Circle to Lasso** / **Scribble to Erase** | 올가미 선택 / 지우기 | 펜 제스처 섹션에 존재(온오프 세부는 **자료 부족**) | [S-46] 공식·(스니펫) |
| **S Pen 옆버튼** (Android) | 지우개 전환(길게 누름 또는 토글, 빌드별 상이) | 사용자 설정 항목 **확인 안 됨** | [S-25] 공식·(스니펫) |
| Apple Pencil **더블탭** | 사용자 댓글: iOS 설정의 Apple Pencil 항목에서 선택 | 앱 내 선택지 목록 **자료 부족** | feedback 댓글 [S-70] 2차·(스니펫) |
| Apple Pencil Pro **스퀴즈 팔레트** | 도구·색 팔레트 표시 | 팔레트 내 도구 커스터마이즈 불가(색 3개) 불만 | feedback [S-71] 1차 관찰·(스니펫) |

### 3.2 설정 화면 구조 (입력 → 동작 선택지)
- 진입: 문서 뷰 **More(…) → "Stylus & Palm Rejection"**, 또는 라이브러리 **Settings**.
- 항목(확인된 것): **Palm Rejection** = 필기 자세(writing posture) 선택 + 민감도 **Low(권장)/Medium/High**(Low = 손바닥 같은 큰 터치만 거부, High = 스타일러스 팁 외 전부 거부) / **Smart Stylus** = None으로 두면 손가락·비블루투스 스타일러스로 필기 / **Draw and Hold** 토글 / 펜 설정(Tip Sharpness·Pressure Sensitivity·Stroke Stabilization — §6).
- 구조적 특징: **"입력 → 동작"을 고르는 매핑 UI가 아니라, 기능별 토글·슬라이더의 나열**. 제스처 자체는 하드코딩.
- 출처: 도움말 7353727026959·360000444675 [S-11][S-12] 공식·(스니펫), [S-03] 공식·(스니펫).

### 3.3 기본값
- Undo/Redo 제스처: 항상 켜짐. Palm Rejection 민감도 기본: Low(권장 표기). Tip Sharpness 50%, Pressure Sensitivity 50%(Fountain).
- 손가락 필기 허용 여부 기본값(Android): **자료 부족 → LOCAL_TODO-2**.

**Platen 시사점**: BRIEF의 "손가락 제스처 + S펜 옆버튼을 매핑 테이블로 설정"은 GoodNotes에 **없는** UX다. 가져올 것은 (a) 2/3손가락 **더블**탭(오작동 방지용 이중 탭) 기본값, (b) 팜 리젝션 3단계 + 자세 선택, (c) OS 멀티핑거 제스처 충돌 안내문.

---

## 4. 레이저 포인터

| 항목 | 관찰 | 수치 | 출처·등급 |
|---|---|---|---|
| 모드 | **Dot**(빨간 점이 손가락/펜을 따라감) / **Trail**(그린 궤적이 잠시 남았다 사라짐 — 큰 영역 강조·임시 주석용) | 2종, 컨텍스트 영역에서 선택 | 도움말 7451632333327 [S-08] 공식·(스니펫) |
| 사라지는 시간 | Trail: "fades after about **one second**"(도움말), 포럼 공지 "disappears after 1 second"; 리뷰는 "a few seconds" | ≈ **1 s** | [S-08] 공식·(스니펫) / feedback [S-72] 공식·(스니펫) / beingpaperless [S-73] 2차·(스니펫) |
| 색·굵기·지속시간 설정 | **없음**(빨강 고정). 색·굵기·선명도·지속시간 커스터마이즈 요청 스레드 존재 | 0개 설정 | feedback [S-13] 1차 관찰·(스니펫) |
| 위치 | 툴바 **최우측** 아이콘, 가려지면 스와이프/가로 회전 | — | [S-08] 공식·(스니펫) |
| 도입 | Trail 스타일은 **v5.2**에서 추가 | — | [S-72] 공식·(스니펫) |
| 플랫폼 | **iOS 전용** (macOS·Android·Windows·Web 미지원) | — | [S-09][S-10] 공식·(스니펫) |
| 기타 | Presentation Mode(AirPlay 미러링·화면공유)와 세트. 키보드 단축키 없음(요청), 화이트보드에서 미동작 보고, 블루투스 프레젠터 미지원(요청) | — | feedback [S-74][S-75] 1차 관찰·(스니펫) |

**Platen 시사점**: 1초 페이드·점/궤적 2모드가 기준선. BRIEF `[가정]`인 색·지속시간 조절은 GoodNotes 사용자들의 **미충족 요청**이므로 넣을 가치가 있다. A/W에 없다는 점이 그대로 우리 기회.

---

## 5. 표(Table) 삽입

- **없음.** 공식 도움말·블로그 어디에도 표 삽입 기능 없음. feedback "Tables in Notebooks, Whiteboard" 스레드 **3,231표**(스니펫 시점), "몇 년째 미구현" 불만. 워크어라운드: 타 앱에서 표 만들어 이미지로 삽입, 도형 도구로 격자 수작업.
- 포맷 측 교차검증: 페이지 요소 종류는 잉크(7)·스티키(20)·도형/텍스트박스(21)·선/커넥터(22)·이미지(1)·수식(11)·구형 텍스트(8)뿐 — **표 객체 없음**. (goodnotes-codec이 "graph widgets"를 라운드트립 전용으로 언급하나 상세 불명.)
- 요청된 사양(사용자 요구 = Platen 설계 입력): 행·열 수 조절, 셀이 텍스트에 맞춰 늘어남, 키보드로 셀 입력, 표 템플릿, 타 프로그램 import/export.
- 출처: feedback [S-14][S-15] 1차 관찰·(스니펫), [G-01] 1차 관찰·직접.

---

## 6. 필기감·잉크 (공개 정보)

### 6.1 펜 종류별 필압 반응
| 펜 | 필압 | 끝처리(테이퍼) | 저장 형식(포맷 관찰) |
|---|---|---|---|
| Ball Pen | **무시** (일정 굵기) | 항상 둥근 끝, 팁 설정 없음 | pen type **0**: 중심선 quad 커브 + **포인트별 width f32**(압력을 "렌더 폭"으로 구워 저장; 가는 펜 약 0.5~1.4, 굵은 고정펜 약 3~4 단위) |
| Fountain Pen | 반응 (Pressure Sensitivity 조절) | Tip Sharpness로 Round↔Sharp | pen type **1**: 중심선(포인트별 w) + **미리 계산된 외곽선**(CGPath move/cubic/arc/elliptical arc) |
| Brush Pen | **강하게** 반응 | Tip Sharpness 조절 | pen type **4**: Fountain 레이아웃 + thickness |
| Pencil | 필압 + **틸트**(azimuth·altitude) | 눕히면 넓고 옅은 음영 | pen type **5**: 포인트별 `x y azimuth altitude force` + quad별 텍스처 시드 |
| Highlighter | — | 직선 보정 옵션 | 별도 타입 아님: **플래그(필드 5) + 알파<0.95** |
출처: 도움말 [S-03] 공식·(스니펫), goodnotes-codec FORMAT.md [G-01] 1차 관찰·직접, goodparse README [G-02] 1차 관찰·직접, Kaih1825 wiki [G-03][G-04] 1차 관찰·직접.

### 6.2 사용자 조절 파라미터와 UI
| 파라미터 | 적용 펜 | UI | 범위/기본 | 출처 |
|---|---|---|---|---|
| **Tip Sharpness** | Fountain·Brush (Ball 없음) | 슬라이더 | Round(**Min**)↔Sharp(**Max**), 기본 **50%** | [S-03] 공식·(스니펫) |
| **Pressure Sensitivity** | Fountain·Brush | 슬라이더 | Min~Max, "Min으로 내려도 효과가 사라지진 않음", 기본 **50%**(Fountain) | [S-03] 공식·(스니펫) |
| **Tip Flatness** | 지원 Apple 기기에서만 노출 | 슬라이더 | 자료 부족 | [S-03] 공식·(스니펫) |
| **Stroke Stabilization** | 펜·형광펜 | 슬라이더 | "작은 떨림을 매끈하게" — 수치 범위 **자료 부족** | [S-03][S-05] 공식·(스니펫) |
| **Draw and Hold** | 펜·연필·형광펜 | 토글 | 기본 켜짐(추정) | [S-03][S-51] |
| **Draw in Straight Line** | 형광펜 | 토글 | 꺼짐(추정). 켜면 살짝 굽은 선이 곡선으로 변환되는 부작용 보고 | [S-05] 공식·(스니펫), feedback [S-76] 1차 관찰·(스니펫) |
| 굵기 | 전부 | 슬롯 재탭 → 슬라이더(mm) | 범위 **자료 부족 → LOCAL_TODO-3** | [S-03] |
| 사용자 요청(미충족) | Ball Pen에 안정화 / Fountain 필압 더 낮추기 / 연필 진하기·틸트 민감도 / 커스텀 필압 커브 | — | — | feedback [S-77][S-40] 1차 관찰·(스니펫) |

### 6.3 저지연 관련 공식/개발자 언급
- Goodnotes 채용 공고(Senior Graphics Engineer – Ink Rendering): "GPU-accelerated ink rendering", **Metal and/or OpenGL/WebGL**, "real-time rendering of ink or vector graphics at high resolution and **low latency**" — 자체 GPU 잉크 렌더러 보유 시사. [S-78] 2차·(스니펫)
- Windows(PWA): **Windows 11 Ink API**로 포인트를 OS 컴포지터에 넘겨 브라우저 애플리케이션 루프 밖에서 추가 잉크를 그림(지연 단축). [S-33] 2차·(스니펫)
- Apple Pencil Pro "Dynamic Ink"·Palette 기능 보도자료 존재(내용 미열람). [S-79] 공식·미열람(제목만)
- 포맷상 시사점(**추정**): 만년필·브러시는 **외곽선을 저장 시점에 굽는다**(fountain/brush outline precomputed). 즉 렌더 시 매번 필압→폭→외곽을 계산하지 않고 채움 경로만 그린다. 지우개 분할(v9)도 새 외곽 경로를 만든다. Stroke Stabilization은 원시 입력을 보관하지 않고 **보정 결과만 저장**(원시 포인트 필드 없음).
- 실측 수치(ms)는 공개 자료에 없음. BRIEF상 지연 기준점은 삼성노트이므로 GoodNotes 지연 실측은 **선택적 LOCAL_TODO-5**.

---

## 7. 파일 포맷 .goodnotes

### 7.1 컨테이너 구조 (오픈소스 파서들의 교차 관찰 — 1차 관찰·직접)
```
<doc>.goodnotes  = 표준 ZIP (암호화 없음)
├─ schema.pb            # protobuf: field1 = 스키마 번호 (현재 35; macOS GN6 수출본에서 24 관찰)
├─ document.info.pb     # 관찰된 파일들에선 빈 파일
├─ thumbnail.jpg        # 미리보기(전체 렌더 아님)
├─ index.notes.pb       # [varint len][msg]* : f1=note id, f2="notes/<id>"  (생성 순, 페이지 순 아님)
├─ notes/<id>           # 페이지별 요소 스트림 (빈 페이지는 0바이트)
├─ index.attachments.pb # f1=attachment id, f2="attachments/<id>"
├─ attachments/<id>     # 원본 PDF/PNG/JPEG 바이트 (확장자 없음, 매직바이트로 판별)
├─ index.events.pb      # 편집 이벤트 로그(문서·페이지·속지·첨부·북마크·아웃라인·오디오…)
└─ index.search.pb, search/<id>  # 손글씨 인식/PDF 검색 캐시
```
- 모든 `.pb`는 **길이 접두(varint) 레코드 스트림**. 스키마 `.proto` 없이 와이어 타입(0/1/2/5)으로 해석; 그룹(3/4)은 미사용.
- **페이지 요소 = 메타 레코드 + 페이로드 레코드 쌍.** 메타: f1 id, f2 버전스탬프{편집 카운터, 랜덤 태그}, **f3 deleted 플래그**(삭제 요소는 파일에 남고 플래그만 1 = 툼스톤), f4 attachment id, f6 오디오 링크, f8 device id, f9 생성 순번, f16 요소별 스키마 번호, f20 group id. **레코드 순서 = z-order.**
- 페이로드 최상위 필드 번호 = 요소 종류: **7 잉크 / 20 스티키 / 21 도형·텍스트박스 / 22 선·커넥터 / 1 이미지·스티커 / 11 수식 변환 / 8 구형 텍스트**.
- **잉크(7)**: f1 id, **f2 지오메트리 blob**, f3 펜 타입(0 Ball, 1 Fountain, 4 Brush, 5 Pencil), f4 색 {r,g,b,a float32 0~1; proto3라 0값 생략}, f5 형광펜 플래그, f9 도형도구 디스크립터, f20 테이프.
- **지오메트리 blob = Apple framed LZ4**: `"bv41"` + u32 raw size + u32 payload size + LZ4 block + `"bv4$"` (비압축 블록은 `"bv4-"`), 32 KiB 초과 시 다중 청크, 히스토리 64 KiB. 해제하면 **TPL**(`"tpl\0"` + flags + u32 size + 포맷 문자열 + 값): 토큰 `v`=u16, `u`=u32(float 비트), `f`=f32/f64, `A()`=count+items, `S()`=struct.
  - Ball: `vuA(v)A(S(uu))A(S(uuuu))vA(f)` = 버전, 굵기, 명령(0 move/1 quad), 시작점, quad(cx,cy,x,y), 상수, **폭 샘플 배열**.
  - Fountain/Brush: 중심선 + 서브패스별 명령(move/cubic/arc(cx cy r a0 a1)/elliptical arc) 외곽선.
  - Pencil: `(x y azimuth altitude force)` 포인트 + 11값 텍스처 시드.
  - 올가미 이동: LZ4 뒤 protobuf 트레일러 f6 {dx, dy} 오프셋.
- **좌표 단위**: 페이지 단위는 **132 dpi 기준**(PDF pt × 11/6). 속지 PDF는 페이지 크기의 6/11. SVG 변환 비율 72/132. (goodparse만 "72dpi pt, A4 595.28×841.89"로 기술 — 자체 가정으로 보이며 다른 셋과 상충: **추정**.)
- **이벤트 로그**: 각 이벤트 `{1 target id, N body}`; body에 f10 시각(ms double), f11 event id, f13 device, f14 **hybrid logical clock**. 속성 이벤트는 **last-writer-wins**. 종류: 30/31 문서(제목) / **2 속지 레이어**(f4 배경 첨부, f5 PDF 페이지(1-based), f8 페이지 크기, f9 템플릿명, f7 줄간격, f18 필기영역·줄폭·라이트/다크) / **54 페이지**(f2 page id, f3 layer id, **f4 order key — ASCII 문자열 비교로 정렬**, 예 `4HRnd001`…) / 104·102·10 노트파일 생성·연결·삽입 / 105 검색 인덱스(PDF만) / **6 첨부 추가**(f5 바이트 크기, f12 pageRange{first,count}; PDF만 범위, **이 이벤트 없으면 앱이 첨부 무시**) / 56 삭제 페이지 / 57 북마크 / 63 회전(쿼터턴) / 65 아웃라인(order key, title, parent) / 160·161·210·213 오디오·단어별 타임스탬프. 오디오 클록 ≈ **31.25 kHz**, AAC 22.05 kHz, 정렬 오차 < 50 ms.
- **텍스트(21/f32)**: bv41 압축된 리치텍스트(런: text, 문자속성{3 색, 4 링크, 30 폰트, 40 크기, 50 이탤릭, 60 weight(-30=bold)}, 문단속성). 구형은 RTF(`{\rtf`). 콘텐츠 해시(8바이트) 알고리즘 미상이나 앱은 아무 값이나 수용.
- **이미지(1)**: f2 AABB, f3 {center,size,angle rad}, f4 attachment id, f6 포맷(0 PNG/1 JPEG/2 JP2/3 PDF/4 HEIC/5 GIF). 스티커·요소형 스티키는 **벡터 PDF 첨부**.
- 속지 수치(Essentials, pt, 좌상단 기준): 점 r0.5 간격 16 / 모눈 0.5 간격 16 / 좁은 줄 0.5 간격 17.5 시작 50 / 넓은 줄 간격 25 시작 49.5.
- 출처: [G-01] goodnotes-codec docs/FORMAT.md, [G-02] goodparse README, [G-03][G-04][G-06][G-07] Kaih1825 wiki 02·03·04·09, [G-05] goodnotes-toolkit references/format.md, [G-08] oblyx README, [G-09] attachment-extractor README — 모두 직접 열람.

### 7.2 공개 분석·오픈소스 파서 (2026-10-24 GitHub API/페이지 기준, 모두 직접 열람)
| 리포 | 언어 | 라이선스 | 마지막 push | 파싱 범위 | 비고 |
|---|---|---|---|---|---|
| **Taylor-Nilsen/goodnotes-codec** [G-10] | Python(stdlib) | **MIT** | 2026-10-20 | 읽기+**쓰기**, 무손실 라운드트립(206문서·4,720페이지·307,070획 검증). Ball·형광펜 쓰기 앱 확인, Fountain/Brush **쓰기 시 앱 크래시**, 도형·텍스트·스티키·선·이미지·페이지·PDF 배경·북마크·회전·아웃라인·링크, 구형 RTF·수식 읽기 전용, 오디오·그래프 위젯·댓글·Text Docs 라운드트립만 | 가장 상세한 FORMAT.md(스키마 24, macOS GN6 수출본). 0★ |
| **Coobyk/oblyx** [G-08] | Rust + Web(WASM) | MIT | 2026-10-13 | → SVG/PDF/PNG(기본 144 dpi). 획(폭·색·알파, Catmull-Rom), JPEG/PNG, 스티키·텍스트·수식, PDF 배경 벡터, 툼스톤 제외 옵션 | 스키마 35. 브라우저 로컬 변환 데모 |
| **Kaih1825/parser-for-goodnotes** [G-11] | Python | MIT | 2026-08-26 | **GN5+GN6**. 획(펜·브러시·형광펜·지우개 컷·도형·올가미 오프셋·RGBA)·텍스트·스티키·이미지·PDF MediaBox·페이지 순서·오디오(획 타임스탬프 동기 MP4 재생) → JSON/SVG/PDF/M4A/MP4/HTML | 9★. 연필 필압 부정확·틸트 미지원, 화살표 불안정. "vibe coding" 자인 |
| **franzthiemann/goodparse** [G-12] | Python | **GPL-3.0** | 2026-10-02 | → Xournal++/Excalidraw/PDF. 획(RGBA·포인트별 폭), 다중 페이지, 테이프·사진·PDF 스티커, 속지, 텍스트(런별 색·크기·폰트) | **GPL → 코드 참조 금지(원리만)**. 페이지 순서 어긋날 수 있음 자인 |
| **ou0826/goodnotes-attachment-extractor** [G-09] | Python | MIT | 2026-10-18 | 첨부(오디오 M4A·이미지·PDF)만 추출, 매직바이트 판별 | GN5 수출본 위주 테스트 |
| **Gitman236/goodnotes-toolkit-skill** [G-05] | Python | MIT | 2026-10-10 | 인스펙션, PDF 배경 교체, PDF 북마크→네이티브 아웃라인(이벤트 합성), 인식 후보 추출. 획 의미는 미해독 | 스키마 35 노트북 1건에서 837개 아웃라인 쓰기 성공 보고 |
| LambertAlpha/natural-ink [G-13] | Python | MIT(+OFL) | 2026-10-16 | 실험적 **쓰기**(스키마 24 Ball 형식) | 1인 테스트 |
| Loligplayer33/goodnotes-cli [G-14] | Python | MIT | 2026-10-24 | macOS 로컬 라이브러리(`Library/Databases/`, `Attachments/`) 읽기, 인식 후보·PDF 텍스트 검색 | 잉크 렌더 안 함 |
| Just8it/Goodtype-Goodnodes-importer [G-15] | Python | Apache-2.0(디코더 MIT) | 2026-10-21 | 폴더·PDF 배경·이미지·손글씨·도형 → 타앱 가져오기(편집 가능 유지). 텍스트·스티키·오디오·부분 지우개 미지원 | Windows x64 CLI |

- GoodNotes 공식 "Open Data Format" 요청 스레드가 feedback에 존재(사용자 "암호화 바이너리"라 주장 — 위 분석들로 **반증됨: 평문 ZIP**). [S-16] 2차·(스니펫)
- 포맷 문서는 모두 **비공식·관찰 기반**이며 GoodNotes 측 스펙은 없다.

### 7.3 GoodNotes 5 vs 6 차이 (Kaih1825 wiki 01 [G-16] 직접 + codec [G-01])
| 기준 | GN5 | GN6(현재) |
|---|---|---|
| 컨테이너 | `.goodnotes` ZIP | 동일 |
| 스키마 번호 | 미기록(자료 부족) | `schema.pb` **35**(2026 수출본), macOS GN6 일부 **24** 관찰, 요소별 f16(아웃라인 26 관찰) |
| 레코드 프레이밍 | varint 길이 접두 | 동일 또는 **Tag 7 래핑** |
| 획 좌표 | TPL 4-tuple(uuuu)·11-tuple | + 고밀도 6-float·3-float·5-float(brush: x y p w angle) |
| 도형 | Tag 9 기본 손그림 도형 | + Tag 21(Type 35)·Tag 22(Type 31) 복합 도형·화살촉·둥근 사각형 |
| 텍스트 | **RTF** 또는 UTF-8 블록 | bv41 캡슐화 리치텍스트(폰트·색·목록·정렬) |
| 페이지 순서 | `index.notes.pb` 순 | **이벤트 로그 order key**(최신 이벤트 기준) |
| 링크 | 열기만 | 생성 가능(§2.9) |

### 7.4 가져오기 판정: **부분 가능 (읽기 전용, 1차 범위 제한)**
근거:
1. **가능한 것(근거 충분)**: ZIP 열기 → protobuf 와이어 디코드 → Apple LZ4 해제 → TPL 파싱으로 **Ball 펜 중심선+포인트별 폭, 색/알파, 형광펜 플래그, 이미지(PNG/JPEG + 변환), PDF 배경(첨부 + 페이지 범위), 페이지 순서(이벤트 order key), 삭제 요소 제외(f3), 텍스트박스(bv41 리치텍스트 또는 RTF), 도형 디스크립터**. 복수 독립 구현(MIT 3종 + GPL 1종)이 서로 다른 샘플에서 같은 구조를 확인했고, 한 구현은 206문서 무손실 라운드트립을 주장한다.
2. **부분적인 것**: Fountain/Brush는 중심선 폭 + **미리 구운 외곽선**이 저장됨 → 외곽선을 **채움 경로로 그대로 렌더**하면 모양은 재현되나, Platen 브러시 엔진으로 "다시 편집 가능한 획"으로 되살리려면 중심선·폭만 취해 **우리 펜으로 재렌더**(모양 차이 발생). Pencil은 azimuth/altitude/force가 있으나 텍스처 시드 의미 미해독(Kaih1825도 "필압 부정확"). 지우개로 잘린 획(v9)은 외곽 경로만 남아 중심선 복원 불가.
3. **불가/제외**: 스키마 드리프트(24↔35, Android 수출본 미검증 → **LOCAL_TODO-6**) 대비 공식 스펙 없음; 오디오 동기·Smart Ink·댓글·Study Set·Text Document는 1차 제외; **쓰기(.goodnotes 내보내기)는 제외**(Fountain 쓰기 시 앱 크래시 보고 = 검증 불가 영역).
4. 법적: 평문 ZIP이라 DRM 우회 없음. GPL 리포(goodparse) 코드는 보지 않고 원리만 사용; MIT 리포도 **코드 복사 없이 재구현**(BRIEF 규칙).
5. 공수 추정(참고): 와이어 디코더 + LZ4 프레임 해제 + TPL 포맷 문자열 해석 + 이벤트 리플레이(페이지 순서·첨부 바인딩) 4개 모듈. 외부 의존 없이 구현 가능(모든 파서가 stdlib로 해냄).

**결론**: `.goodnotes` **읽기 = 부분 가능(후속 마일스톤 후보, 1차 MVP 밖)**. 1차에서는 사용자가 GoodNotes에서 **PDF(Editable)로 내보낸 파일 가져오기**로 대체(하이퍼링크 누락 가능성 안내).

---

## 8. 템플릿·속지 생태계

| 항목 | 관찰 | 수치 | 출처·등급 |
|---|---|---|---|
| 하이퍼링크 PDF 플래너 — 동작 조건 | 링크는 **Read-only 모드에서 탭**(iOS는 편집 모드 탭도 가능), 편집 모드는 **롱프레스 → Open Link**. A/W/W는 Read-only 필수. 링크 안 되면 외부 PDF 뷰어에서 먼저 확인하라(공식). 플랫폼별 동작이 달라 판매자는 **모든 플랫폼에서 테스트** 권고 | — | 도움말 4406218761231·7353757120655 [S-20][S-21] 공식·(스니펫) / 판매자 가이드 [S-80] 2차·(스니펫) |
| 링크 유지 조건 | **내부 링크(문서 내 이동)만 신뢰**, 웹 링크는 깨질 수 있음(2차·저품질). **내보내기 시 하이퍼링크 유실**(Editable PDF도 대부분 누락, 특정 월만 살아남는 식) — 플랫폼 공통 버그 보고 | — | [S-81] 2차·(스니펫) / feedback [S-64][S-82] 1차 관찰·(스니펫) |
| 권장 비율·크기 | **Goodnotes Standard = 455.04 × 588.45 pt** (≈ 6.32 × 8.17 in, 비율 **0.773**, 4:3=0.75에 근접; 내부 페이지 단위 834.24 × 1078.825). 플래너 제작 가이드류가 제시하는 픽셀값(예 A4 2480×3508 @300dpi)은 저품질 SEO 페이지라 **채택 안 함** | 0.773 | [G-01] 1차 관찰·직접 / omnisizes [S-58] 2차·(스니펫) |
| 템플릿 크기 단위 | 앱 내부·PDF는 **pt**(72 dpi) + 페이지 단위(132 dpi). 커스텀 크기 입력은 inch/mm(3자, Apple 전용). 픽셀 입력 **근거 없음** | — | [G-01] 직접 / [S-58] 2차·(스니펫) / [S-02] 공식·(스니펫) |
| 표지 | PDF 권장(첫 페이지만 사용), 크기·방향을 템플릿 그룹과 일치시켜 가져오기. 가져올 때 이미지 스케일링 불만 스레드 | — | [S-55][S-56] 공식·(스니펫), feedback [S-83] 1차 관찰·(스니펫) |
| 스티커(PNG 알파) | **Elements** 도구: 컬렉션 단위 관리, Photos(다중 선택)·Files(**한 번에 1개**, 3자 보고) 가져오기, .jpg/.png 지원; 페이지 객체를 올가미/이미지 도구로 선택 → "add element"(여러 객체 선택 시 1개 요소로 묶임). 판매자 테스트 항목: 투명 PNG를 **밝은/어두운 속지 양쪽에서 가장자리 깨끗한지** 확인. 포맷: 스티커는 PNG 또는 **벡터 PDF 첨부** | — | 도움말 360003097995 [S-17] 공식·(스니펫) / wendaful [S-84] 2차·(스니펫) / [S-80] 2차·(스니펫) / [G-01] 직접 |
| "Apply to all" 함정 | GoodNotes에는 **템플릿 일괄 변경(모든 페이지 적용) 기능이 없음**(페이지별 More → Change template → 속지·색·크기·방향; "모든 페이지에 적용" 요청 스레드 다수). 따라서 GoodNotes 쪽 함정은 "없어서 불편"이고, **Platen이 일괄 적용을 넣는다면** PDF 배경이 있는 플래너 페이지에 적용 시 **배경 PDF·링크·페이지 크기가 바뀐다**는 경고/제외 규칙이 필요(추정) | — | 도움말 7353743758991 [S-18] 공식·(스니펫), feedback [S-19] 1차 관찰·(스니펫) |
| Dynamic Templates(GN6) | 앱 안에서 크기·색을 바꿀 수 있는 템플릿. A/W/W는 **열기만** 가능. 마켓플레이스에 "Dynamic Essentials" 류 상품 존재 | — | [S-02] 공식·(스니펫), Gumroad [S-85] 2차·(스니펫) |
| 속지 괘선 규격(참고값) | §7.1 Essentials 수치(점 16pt, 모눈 16pt, 좁은 줄 17.5pt 시작 50, 넓은 줄 25pt 시작 49.5; 선 0.5pt) | — | [G-01] 직접 |

---

## 가져올 원리 (3~5개)

1. **"선택된 도구 재탭 = 세부 설정" + 굵기 3슬롯·색 3→12슬롯** — 도구 바가 글자 없이 아이콘만으로도 동선이 일관되고(BRIEF UI 제약과 일치), 슬롯 수의 상한이 명시돼 설계가 단순해진다.
2. **Undo/Redo = 2/3손가락 "더블"탭을 기본값으로** — 단일 탭은 팜 터치와 충돌해 오작동한다는 것이 GoodNotes가 고정값으로 택한 이유로 보이며(공식: 끄기 토글조차 없음), Platen 매핑 테이블의 **기본 행**으로 채택하되 사용자 변경 허용.
3. **필압은 "렌더 폭"으로 구워 저장하고, 만년필/브러시는 외곽선을 저장 시점에 확정** — 렌더 루프에서 필압→폭 변환을 반복하지 않아 재생·스크롤이 가볍다. 단 Platen은 편집 가능성을 위해 **원시 입력(x, y, p, tilt, t) + 렌더 폭 둘 다** 보관하는 것이 상위 호환(§6.3 추정의 교훈).
4. **삭제 = 툼스톤 플래그, 이동 = 오프셋, 페이지 순서 = 이벤트 로그 order key(LWW·HLC)** — Drive 동기화·오프라인 우선(BRIEF)에 바로 맞는 충돌 해결 모델. 페이지 재정렬이 문자열 키 하나로 끝난다.
5. **인식 결과는 별도 인덱스 파일(search/)에 캐시, 스트로크는 불변** — Platen의 "손글씨 인식 1차 제외·스트로크 보존" 방침과 충돌 없이 나중에 붙일 수 있는 구조.

## 버릴 것
- **제스처 하드코딩**(매핑 UI 없음, 끄기 토글 없음) → Platen은 매핑 테이블.
- **레이저 포인터 색·지속시간 고정, iOS 전용** → 색·시간 조절 + Android 1차.
- **다크 모드에서 잉크 미반전**(종이는 흰색 유지, 형광펜 z-order 불명) → 검정 잉크 자동 반전 + 형광펜 가시성 보장.
- **표 없음**(수년간 최다 요청) → 1차 표 삽입 유지.
- **템플릿 일괄 변경 없음** → 일괄 적용 제공하되 PDF 배경 페이지 제외/경고.
- **Apple 전용 기능 분기**(커스텀 템플릿 크기, 줌 윈도우, 필압 표기 상충) → Android/Windows에서 동일 기능 세트.
- **.goodnotes 쓰기 호환** 시도(크래시 보고, 스펙 없음) → 읽기만, 그것도 후속.

## LOCAL_TODO 후보 (갤럭시 탭 S9+에 GoodNotes Android 설치 가능 — 최소화)

| ID | 무엇을 | 실행 방법 | 측정/기록 양식 |
|---|---|---|---|
| **LOCAL_TODO-1** | Android/Windows 필압·틸트 실제 동작 | Fountain·Brush·Pencil 선택 → 약/중/강 필압·수직/45° 틸트로 각 1획. Windows 노트북은 펜 없음 → 마우스 굵기 일정 여부만 | 스크린샷 + 표: 펜 × (필압 반응 Y/N, 틸트 반응 Y/N, 빌드 Play/Galaxy, 앱 버전) |
| **LOCAL_TODO-2** | Android 기본 툴바 순서 + 설정 트리 + 손가락 필기 기본값 + S Pen 버튼 동작 | 새 설치 상태(초기화 후)에서 문서 열고 툴바 좌→우 캡처; More(…)·Settings 전 화면 캡처; 손가락으로 긋기 | 순서 리스트(아이콘명), 설정 트리(들여쓰기 텍스트), 손가락 필기 기본 ON/OFF, S Pen: 탭/길게 중 어느 쪽, 복귀 여부 |
| **LOCAL_TODO-3** | 펜 굵기 슬라이더 범위·기본 슬롯값, Tip Sharpness/Pressure/Stabilization 슬라이더 표시 단위 | 각 슬롯 재탭 → 최소/최대로 밀어 표시값 기록 | 표: 도구 × (최소, 최대, 기본 3슬롯 값, 단위 mm/%) |
| **LOCAL_TODO-4** | 다크 모드 잉크·형광펜 처리(Android) | 시스템 다크 전환 → 흰 속지·검정 속지 각각에서 검정 펜·노랑 형광펜 1획, 겹쳐 긋기(펜 위 형광펜 / 형광펜 위 펜) | 스크린샷 4장 + "잉크 반전 Y/N, 형광펜이 펜 아래로 내려가는지 Y/N" |
| **LOCAL_TODO-5 (선택)** | 지연 체감 비교(기준점은 삼성노트이므로 참고용) | 240fps 슬로모 촬영, 펜촉-잉크 끝 간격 프레임 수 | 프레임 수 → ms(1프레임=4.17ms), 삼성노트 동일 조건 병기 |
| **LOCAL_TODO-6** | Android 수출 `.goodnotes` 샘플 1개 확보(개인 데이터 없는 테스트 노트: Ball/Fountain/Brush/Pencil/형광펜 각 1획 + 이미지 1 + PDF 1쪽 + 텍스트 1) | 문서 → 내보내기 → Goodnotes 형식 → PC로 복사 | `unzip -l` 목록, `schema.pb` 바이트(hex 2바이트), 파일 크기. 리포 `docs/research/samples/`에 두지 말고 로컬 보관(개인정보·약관) |

## 출처 목록 (URL · 열람일 2026-10-24 · 등급 · 열람 방식)

### GitHub (직접 열람)
- [G-01] https://raw.githubusercontent.com/Taylor-Nilsen/goodnotes-codec/main/docs/FORMAT.md · 1차 관찰 · 직접
- [G-02] https://raw.githubusercontent.com/franzthiemann/goodparse/main/README.md · 1차 관찰 · 직접 (GPL-3.0 — 원리만 참조)
- [G-03] https://raw.githubusercontent.com/Kaih1825/parser-for-goodnotes/main/wiki/03-compression-and-tpl-binary.md · 1차 관찰 · 직접
- [G-04] https://raw.githubusercontent.com/Kaih1825/parser-for-goodnotes/main/wiki/09-current-re-findings.md · 1차 관찰 · 직접
- [G-05] https://raw.githubusercontent.com/Gitman236/goodnotes-toolkit-skill/main/references/format.md · 1차 관찰 · 직접
- [G-06] https://raw.githubusercontent.com/Kaih1825/parser-for-goodnotes/main/wiki/02-archive-and-wire-format.md · 1차 관찰 · 직접
- [G-07] https://raw.githubusercontent.com/Kaih1825/parser-for-goodnotes/main/wiki/04-stroke-geometry-and-rendering.md · 1차 관찰 · 직접
- [G-08] https://github.com/Coobyk/oblyx · 1차 관찰 · 직접
- [G-09] https://github.com/ou0826/goodnotes-attachment-extractor · 1차 관찰 · 직접
- [G-10] https://github.com/Taylor-Nilsen/goodnotes-codec · 1차 관찰 · 직접
- [G-11] https://github.com/Kaih1825/parser-for-goodnotes (+ /wiki) · 1차 관찰 · 직접
- [G-12] https://github.com/franzthiemann/goodparse · 1차 관찰 · 직접
- [G-13] https://github.com/LambertAlpha/natural-ink · 1차 관찰 · 직접
- [G-14] https://github.com/Loligplayer33/goodnotes-cli · 1차 관찰 · 직접
- [G-15] https://github.com/Just8it/Goodtype-Goodnodes-importer · 1차 관찰 · 직접
- [G-16] https://raw.githubusercontent.com/Kaih1825/parser-for-goodnotes/main/wiki/01-architecture-overview.md · 1차 관찰 · 직접
- 리포 메타(라이선스·push 일자): GitHub Search API 응답(2026-10-24) · 직접

### Goodnotes 공식 (스니펫으로 확인 — 직접 열람 차단)
- [S-01] https://support.goodnotes.com/hc/en-us/articles/14069931315983-Quick-gestures-for-Undo-and-Redo · 공식 · (스니펫)
- [S-02] https://support.goodnotes.com/hc/en-us/articles/7378735557519-Goodnotes-for-Android-Windows-and-Web-general-FAQs · 공식 · (스니펫)
- [S-03] https://support.goodnotes.com/hc/en-us/articles/7353756785679-Write-and-customize-ink-with-the-Pen-tool · 공식 · (스니펫)
- [S-05] https://support.goodnotes.com/hc/en-us/articles/7353718215823-Using-the-Highlighter-tool · 공식 · (스니펫)
- [S-06] https://support.goodnotes.com/hc/en-us/articles/7353695644175 (Lasso) · 공식 · (스니펫)
- [S-07] https://support.goodnotes.com/hc/en-us/articles/7353718249231-Erase-content-with-the-Eraser-tool · 공식 · (스니펫)
- [S-08] https://support.goodnotes.com/hc/en-us/articles/7451632333327-Focus-Attention-with-Laser-Pointer · 공식 · (스니펫)
- [S-09] https://support.goodnotes.com/hc/en-us/articles/7353727934223-Presentation-Mode · 공식 · (스니펫)
- [S-10] https://goodnoteseducation.zendesk.com/hc/en-us/articles/13879694731791-Presentation-Mode · 공식 · (스니펫)
- [S-11] https://support.goodnotes.com/hc/en-us/articles/7353727026959-Configure-palm-rejection-for-comfortable-writing · 공식 · (스니펫)
- [S-12] https://support.goodnotes.com/hc/en-us/articles/360000444675 (Smart Stylus) · 공식 · (스니펫)
- [S-17] https://support.goodnotes.com/hc/en-us/articles/360003097995-Elements-Tool-Enrich-and-reuse-your-note · 공식 · (스니펫)
- [S-18] https://support.goodnotes.com/hc/en-us/articles/7353743758991-Change-a-page-template-in-Goodnotes · 공식 · (스니펫)
- [S-20] https://support.goodnotes.com/hc/en-us/articles/4406218761231 (PDF hyperlinks) · 공식 · (스니펫)
- [S-21] https://support.goodnotes.com/hc/en-us/articles/7353757120655-Use-Read-Only-Mode-to-navigate-without-editing · 공식 · (스니펫)
- [S-22] https://www.goodnotes.com/blog/whats-new-in-goodnotes-april-june-2026 · 공식 · (스니펫)
- [S-23] https://www.goodnotes.com/blog/features-fixes-updates-march-2026 · 공식 · (스니펫)
- [S-24] https://goodnotes.com/blog/features-fixes-updates-december-2025 · 공식 · (스니펫)
- [S-28] https://support.goodnotes.com/hc/en-us/articles/7353756826383 (Zoom Window) · 공식 · (스니펫)
- [S-35] https://www.goodnotes.com/de/windows · 공식 · (스니펫)
- [S-37] https://goodnotes.com/partners/samsung · 공식 · (스니펫)
- [S-42] https://goodnotes-team.notion.site/Folders-and-documents-e5b36c71891f4350a5ee4c1a199bbe92 · 공식 · (스니펫)
- [S-45] https://support.goodnotes.com/hc/en-us/articles/10779390143247 (Convert handwriting) · 공식 · (스니펫)
- [S-46] https://goodnotes-team.notion.site/Pen-Gestures-26fb740273d281e4b3f6cb20cad4590e · 공식 · (스니펫)
- [S-47] https://goodnotes-team.notion.site/Text-Tool-26fb740273d2811983cbe906be1f5520 · 공식 · (스니펫)
- [S-48] https://goodnotes-team.notion.site/Image-Tool-18fc0e30c81647cbb2b1071bb1b946c1 · 공식 · (스니펫)
- [S-51] https://www.goodnotes.com/blog/shape-recognition · 공식 · (스니펫)
- [S-52] https://support.goodnotes.com/hc/en-us/articles/13682939148943-Draw-shapes-and-build-diagrams-in-Goodnotes · 공식 · (스니펫)
- [S-53] https://support.goodnotes.com/hc/ko/articles/7353718215951/ · 공식 · (스니펫)
- [S-55] https://support.goodnotes.com/hc/en-us/articles/360000115295-Add-custom-templates-to-the-template-library · 공식 · (스니펫)
- [S-56] https://support.goodnotes.com/hc/en-us/articles/7353728093199-Add-external-templates-to-the-template-library · 공식 · (스니펫)
- [S-57] https://goodnotes-team.notion.site/Custom-Templates-ac6648f8915841f78cb95d75ad10332d · 공식 · (스니펫)
- [S-59] https://support.goodnotes.com/hc/en-us/articles/7353743248143-Connect-your-notes-with-Links · 공식 · (스니펫)
- [S-60] https://support.goodnotes.com/hc/en-us/articles/7353743594127 (Search) · 공식 · (스니펫)
- [S-61] https://support.goodnotes.com/hc/en-us/articles/7353727932047 (Handwriting languages) · 공식 · (스니펫)
- [S-62] https://goodnotes-team.notion.site/Search-Handwriting-and-Math-Recognition-26fb740273d281b5beb8e60130afb556 · 공식 · (스니펫)
- [S-63] https://goodnotes-team.notion.site/Export-documents-d5738094122f4bd095af7d0f8d04748b · 공식 · (스니펫)
- [S-79] https://www.goodnotes.com/press/new-features-apple-pencil-pro-palette-dynamic-ink · 공식 · 제목만(미열람)

### Goodnotes feedback 포럼 (Goodnotes 직원 답변 = 공식, 사용자 글 = 1차 관찰; 모두 (스니펫))
- [S-13] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/45975667--laser-pointer-more-customizations-colors-thick
- [S-14] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/44822932-tables-in-notebooks-whiteboard
- [S-15] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/3638695-implement-the-ability-to-add-blank-tables-and-grap
- [S-16] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/40953457-open-data-format-for-goodnotes
- [S-19] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/40095070-change-template-for-all-pages-in-document
- [S-25] https://feedback.goodnotes.com/forums/950440-how-can-we-improve-goodnotes-beta/suggestions/46341553-s-pen-side-button-should-be-used-to-change-to-era (Goodnotes 답변)
- [S-26] https://feedback.goodnotes.com/forums/950440-customer-suggestions-for-goodnotes-android-windo/suggestions/47596142--comment-function-activate-the-on-off-function-e
- [S-27] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/50583833--toolbar-support-customization (7.0.14 공지)
- [S-39] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/46571017-stock-the-pencils-preferences-as-buttons
- [S-40] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/50167776-pencil-tool-darkness-tilt-sensitivity
- [S-43] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/13329633-create-folders-within-notebooks
- [S-49] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/50694902-goodnotes-6-did-this-better-bring-those-text-feat
- [S-50] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/47039434-ability-to-crop-images
- [S-54] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/37901803-auto-shape-function
- [S-64] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/46494274-pdf-export-editable-bug-fix-missing-hyperlinks
- [S-65] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/39621949-flattened-pdf-export-should-retain-outlines-bookma
- [S-66] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/46042741-true-dark-mode
- [S-67] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/50784245-true-dark-mode-automatic-page-ink-color-inversi
- [S-68] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/39331441-automatic-switch-of-paper-and-ink-colour-between-d
- [S-69] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/10878894-add-2-and-3-finger-tap-shortcuts-for-undo-and-redo
- [S-70] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/35954707-enable-gestures-on-apple-pencil (사용자 댓글 · 2차)
- [S-71] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/48613325-reorder-apple-pencil-menu
- [S-72] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/32460554-pointer-tool (Trail 1초·v5.2)
- [S-74] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/49725242-keyboard-shortcut-for-laser-pointer
- [S-75] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/50694566-laser-pointer-whiteboard
- [S-76] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/45859309-highlighter-draw-in-straight-line-always-straight
- [S-77] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/46105504-customize-pen-settings-pressure-stability
- [S-82] https://feedback.goodnotes.com/forums/950440-customer-suggestions-for-goodnotes-android-windo/suggestions/47414834-bug-report-pdf-export-in-goodnotes-removes-existi
- [S-83] https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/42989358-cover-image-scaling-on-import-to-notebook-template

### 2차 글 (스니펫)
- [S-29] https://www.apkmirror.com/apk/goodnotes/goodnotes-for-android/ · 2차
- [S-30] https://www.apkmirror.com/?p=6572019 등 APKMirror 버전 페이지 · 2차
- [S-31] https://alternativeto.net/news/2025/10/goodnotes-update-brings-new-plans-ai-features-whiteboards-and-text-documents · 2차
- [S-33] https://blog.pwabuilder.com/posts/how-goodnotes-uses-web-apis-to-create-a-great-pwa-for-windows/ · 2차
- [S-34] https://productivitybrief.com/setup-guides/goodnotes-for-windows-guide-2026 · 2차
- [S-36] https://everytalkin.co.uk/goodnotes-for-windows-review/ · 2차
- [S-38] https://mynexttablet.com/goodnotes-android-samsung-tablets/ · 2차
- [S-41] https://www.lemon8-app.com/@createheartwork/7353643604424327685?region=ca · 2차
- [S-44] https://www.macstories.net/reviews/goodnotes-5-the-macstories-review/ · 2차
- [S-58] https://omnisizes.com/digital/goodnotes-paper/ · 2차
- [S-73] https://beingpaperless.com/goodnotes/ · 2차
- [S-78] https://job-boards.greenhouse.io/goodnotes/jobs/5690965004 (Senior Graphics Engineer – Ink Rendering) · 2차
- [S-80] https://payhip.com/b/XKtLn (플래너 판매자 테스트 매트릭스) · 2차
- [S-81] https://slashtest.huit.harvard.edu/how-to-create-goodnotes-templates-complete-guide-tips-and-answers-2026 · 2차(저품질, 참고만)
- [S-84] https://www.wendaful.com/2021/05/goodnotes-app-elements-feature/ · 2차
- [S-85] https://discovernotes.gumroad.com/l/dynamicsquared · 2차

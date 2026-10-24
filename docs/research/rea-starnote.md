# REA — 스타노트 (StarNote, Android) · 필기감 · 잉크 · 포맷

> 열람일: **2026-10-24** · 작성: Platen 리서처 · 상태: 공개 자료 기반 1차 조사 (실측은 LOCAL_TODO)
>
> **식별 확신도: 높음** — 사용자가 갤럭시 탭에서 쓰는 "스타노트"는 **Onyx International Inc.(广州文石, BOOX 제조사)의 `StarNote: Handwritten Notes`(패키지 `com.onyx.galaxy.global.note`)** 로 확정. 한국 커뮤니티(디시 갤럭시탭 갤러리·삼성멤버스·유튜브)에서 "스타노트"로 불리는 앱이 이것 하나뿐이고, Play 한국어 제목이 "StarNote: 노트 필기 & PDF"이며, 만년필·필압·안정화 슬라이더 등 사용자가 기준점으로 삼은 요소가 모두 이 앱의 공개 설정 항목과 일치한다. 경쟁 후보(동명의 "Star Note Pro" 메모장 등)는 S펜 필기 앱이 아니므로 제외.
>
> **열람 방식 고지**: 이 세션은 네트워크 정책상 다음 호스트의 **직접 페치가 전부 차단**(프록시 CONNECT 403)되었다 — play.google.com · apps.apple.com · galaxystore.samsung.com · github.com(페이지·API) · m.dcinside.com / gall.dcinside.com · youtube.com · reddit.com · producthunt.com · alternativeto.net · apkcombo / apkpure / uptodown / softonic / aptoide · namu.wiki · search.naver.com · clien.net · ruliweb.com · r1.community.samsung.com · community.linkareer.com · nozomunoto.com · yeyulingfeng.com · ababtools.com · sj.qq.com · tiktok.com · web.archive.org · r.jina.ai 등. **되는 것**: WebSearch(스니펫) · **raw.githubusercontent.com** · gist.github.com. 그래서 (a) 웹 검색 엔진이 돌려준 **페이지 스니펫**과 (b) **GitHub raw 직접 열람** 두 경로만 썼다. 표기 규칙: `[스니펫]` = 검색 결과에 노출된 인용문만 읽음(등급에도 "(스니펫)" 병기), `[직접]` = 원문 파일 전체 열람. 세션 검색 한도(200회)도 소진되어 추가 탐색은 후속 세션 필요.
>
> 등급 기준: **공식 문서** (스토어·개발사·앱 내 문자열) / **1차 관찰** (스크린샷·파일·바이트 직접 확인) / **2차 글** (리뷰·위키·커뮤니티) / **추정**
>
> **법적·윤리 주의**: 3자 GitHub 저장소 중 두 곳(`Dhyan2404/StarNotes`, `NOT4LONG01/rea-starnote`)은 APK를 디컴파일/정적 분석한 결과물이다. 본 문서는 거기서 **UI 문자열·상수·파일 구조 같은 사실과 수치만** 인용했고 코드·에셋은 가져오지 않았다. Platen 구현 시에도 동일 원칙(원리와 수치만) 유지.

---

## 1. 앱 식별 (0단계 결과)

| 항목 | 내용 | 출처·등급 |
|---|---|---|
| 정식 명칭 | **StarNote: Handwritten Notes** (영문 Play) / **StarNote: 노트 필기 & PDF** (한국어 Play) / 갤럭시 스토어 "StarNote: Note-taking, PDFs" / iOS "StarNote: Handwriting&PDF" | Play·App Store·Galaxy Store [스니펫] · 공식 문서 |
| 패키지명 | `com.onyx.galaxy.global.note` (글로벌판). **중국 내수판은 별도 패키지 `com.onyx.galaxy.note`**(iOS `id6740199654` "StarNote笔记") — 기능이 먼저 들어가는 경우가 있어 참고용 | Play URL · GitHub 코드 검색 [직접] · 공식 문서/1차 관찰 |
| 개발사 | **Onyx International Inc.** (广州文石信息科技, BOOX 전자잉크 태블릿 제조사). App Store 판매자 "Onyx International Inc.", Galaxy Store 판매자 "ONYX INTERNATIONAL INC. /刘路", Play 개발자명은 한 스니펫에서 "BOOX CO., LIMITED"로 표기됨(동일 그룹) | App Store·Galaxy Store·Play [스니펫] · 공식 문서 |
| 스토어 URL | Play <https://play.google.com/store/apps/details?id=com.onyx.galaxy.global.note> · Galaxy Store <https://galaxystore.samsung.com/detail/com.onyx.galaxy.global.note> · App Store <https://apps.apple.com/us/app/starnote-handwriting-pdf/id6751570915> · 공식 사이트 다운로드 <https://site.starnote.ai/download> (커뮤니티 위키가 안내) | [스니펫]/[직접 위키] |
| 출시 | Play 등록 2025-05-09(스니펫) · Product Hunt 런칭 2025-05-18 · iOS 최초 릴리스 2025-10-15 | Play·hunted.space·App Store [스니펫] · 공식/2차 |
| 현재 버전 | **Android 1.5.3 (versionCode 5703, minSdk 29 / targetSdk 36)** — 3자 APK 정적 분석과 ReVanced 패치 레지스트리(2026-10-20 생성, 버전 목록 1.4.3/1.5.2/1.5.3) 일치. 중국판도 1.5.3(2026-09-24, 텐센트 응용보). **Play 최종 업데이트 2026-10-15** | rea-starnote feature-inventory [직접]·Zy0x 레지스트리 JSON [직접]·Play [스니펫] · 1차 관찰/공식 |
| 가격 모델 | **무료 + 일회성 "Lifetime PRO"**(구독 없음). App Store 표기 US$5.99(다른 스냅샷 US$6.99). 무료판은 **노트북 개수 제한**(갤럭시 스토어 문구; 사용자 리뷰 "10개"). Pro는 StarNote 계정 로그인에 묶임(iPad·Android 공유 가능). 2026-10-15 업데이트로 "Pro 워터마크 제거 혜택" 추가. **원화 가격은 자료 부족** | App Store·Galaxy Store·Play [스니펫]·커뮤니티 위키 FAQ [직접] · 공식/2차 |
| 규모·평점 | Play 4.5★/약 8.02K 리뷰, 설치 1,000,000+ (스니펫 간 수치 불일치 있음: AppBrain 4.17/1,810) | Play·AppBrain [스니펫] · 2차 |
| 커뮤니티 | r/StarNoteApp · 공식 Discord · 비공식 위키(readthedocs) · 한국: 디시 갤럭시탭 마이너 갤러리, 삼성멤버스 | 위키 [직접] · 2차 |

- 출처: <https://play.google.com/store/apps/details?id=com.onyx.galaxy.global.note> [스니펫] · <https://apps.apple.com/us/app/starnote-handwriting-pdf/id6751570915> [스니펫] · <https://galaxystore.samsung.com/detail/com.onyx.galaxy.global.note> [스니펫] · <https://raw.githubusercontent.com/NOT4LONG01/rea-starnote/main/docs/feature-inventory.md> [직접] · <https://github.com/Zy0x/YouTube-Revanced> (`web/registry/v2/apps/com.onyx.galaxy.global.note.json`) [직접, 코드검색] · <https://sj.qq.com/appdetail/com.onyx.galaxy.note> [스니펫] · 열람 2026-10-24

---

## 2. 필기감·잉크 (최우선)

### 2-1. 펜 종류 (로스터)
- **관찰**: 앱 내 문자열(한국어 리소스)로 확인되는 필기 도구: **만년필**(`fountain_pen`), **서예 펜**(`calligraphy_pen`/`art_fountain_pen`, 중국어 원문 "艺术钢笔"; 변형 "Calligraphy 1~5"), **소프트 페인트 펜**(`soft_paint_pen`, 중국어 "柔绘笔", v1.2+ 추가), **볼펜**, **연필**, **붓펜**(`brush_pen`), 형광펜, **레이저 포인터**, 발췌 펜(Excerpt). 커뮤니티 위키는 "핵심 5펜 = 만년필·캘리그래피·소프트브러시·볼펜·연필"로 정리.
- **펜 박스(필통)**: 사용자 프리셋 **16개**까지 저장(위키). 앱 문자열에 "펜박스 개수 제한 %d" / "펜 개수 제한 %d" 경고가 있어 상한이 코드로 존재(수치는 자료 부족). 레딧에 "펜 박스 한도 풀어달라" 스레드 다수.
- 출처: 앱 문자열 `res/values-ko/strings.xml`·`res/values/strings.xml` (Dhyan2404/StarNotes 디컴파일, v1.3.0 기준) [직접] · 위키 pens.md·tools.md [직접] · r/StarNoteApp 제목 CSV [직접] · 등급: 공식 문서(앱 내 문자열) / 2차 글

### 2-2. 펜별 조절 파라미터 (사용자 조절 UI)
| 펜 | 조절 항목 (위키 tools.md) | 비고 |
|---|---|---|
| 만년필 | **압력 감도 · 두께 · 스트로크 안정화** | "0%로 두어도 약간의 필압 반응이 남는다"(위키 + 레딧 스레드 "0% pressure sensitivity on fountain pen still pressure sensitive?") |
| 서예 펜 | 두께 · 안정화 | 획 **방향**에 따라 굵기 변화(끌 펜촉 로직), 필압 아님 |
| 소프트 페인트 펜 | 두께 | 쓰는 동안 채워지고 **끝을 속도 기반으로 테이퍼** |
| 볼펜 | 두께 · 안정화 | 필압·속도 무관 균일 굵기 |
| 연필 | 압력 감도 · 두께 · **밀도**(`transparency_factor_desc`="밀도") | 반투명, 겹칠수록 진해짐(포화까지) |

- **한국어 UI 라벨(앱 문자열)**: `pressure_sensitivity`=**압력 감도**, `thickness`=**두께**, `smooth`=**스트로크 안정화**, `speed`=**속도**, `velocity_sensitivity_amplifier`=**속도 민감도**, `adapt_pen_stroke_width`=**자동 스트로크 너비**, `pressure_sensitive_eraser`=**압력 감지 지우개**, `stroke_convert_to_graph_with_pressure`=**필압에 따라 굵기가 조절되는 단일 획 도형화**. 두께 단위는 **mm**(`float_format_two_float_point_with_mm`="%.2fmm").
- **숨은/미번역 파라미터**: 기본 리소스에 중국어 그대로 남은 속도 관련 문자열 4종 — `velocity`="高速度灵敏度"(고속 민감도), `velocity_ignore_threshold`="低速度忽略阈值"(저속 무시 임계), `velocity_lower_bond`="最大低速度", `velocity_upper_bound`="最小高速度". 즉 **속도→굵기 모델이 4구간(무시 임계·저속 상한·고속 하한·민감도)** 으로 설계돼 있고, 일반 UI에는 "속도"·"속도 민감도"만 노출되는 것으로 추정.
- **슬라이더 범위·단계**: 중국 리뷰(6개 안드로이드 필기앱 파라미터 비교)가 StarNote 만년필을 **"압감 %·속도 %"** 두 축으로 추천값 제시 → 두 슬라이더 모두 **0~100%** 표시. 추천값: 귀여운 글씨 [압감 0%, 속도 20%] 또는 [20%, 20%] / 일반 필기 [압감 50%, 속도 20~60%] / 흘림 [압감 60%, 속도 40%] 또는 [80%, 60%]; 연필 [두께 0.9~1.0, 압감 40~80%]. 단계 수(스텝)는 **자료 부족**.
- **저장값과의 대응**: 실제 `.note` 파일의 스트로크 브러시 JSON에 `pressureSensitivity: 0.375`, `smoothLevel: 0.2`, `velocitySensitivity/velocityAmplifier/velocityIgnoreThreshold/velocityLowerBound/velocityUpperBound: 0.0`, `alphaFactor: 1.0`, `brushShape: 1`, `directionEnabled: true`, `maxPressure: 32767.0`, `dpi`, `displayScale` 필드가 기록됨 → UI %는 내부 0~1 실수로 저장.
- **설정 동선**: 위키 "툴바의 도구를 **두 번 탭**하면 해당 도구 설정 메뉴" / 중국 다운로드 사이트 설명 "좌측 브러시 아이콘 **길게 누르기**로 압력 감도·유창도 조절" / 위키 pens.md "압력 커브는 **설정 > 스타일러스**" (스타일러스 설정 메뉴 `stylus_settings` 존재). 세 경로가 공존하는 것으로 보이며 정확한 동선은 LOCAL_TODO.
- 출처: 위키 tools.md·pens.md·setup.md [직접] · 앱 문자열 [직접] · rea-starnote format-spec.md §4 [직접] · 夜雨聆风 "6款安卓端笔记软件手写参数测评" <https://www.yeyulingfeng.com/95906.html> [스니펫] · <https://www.7xz.com/softs/28625.html> 등 중국 다운로드 사이트 [스니펫] · 등급: 공식 문서(문자열)/1차 관찰(파일)/2차 글

### 2-3. 필압 → 굵기/농도 반응
- **관찰**: 만년필은 "굵기가 오직 누르는 압력으로 결정"(위키), 연필은 압력이 굵기+밀도 모두에 반영. "힘-굵기 커브는 완전 커스텀 불가"(위키). 사용자 보고: iOS 중국판 리뷰 "만년필에 압감을 조금만 켜도 **획 시작점에 잉크가 과다 분출(큰 점)**" — 첫 샘플 압력 스파이크 미처리 가능성(추정). 한국 사용자(삼성멤버스, 2026-03): 최신판에 안정화 기능 추가·보정 강도 조절 가능, **한글은 알파벳과 보정 거동이 다르다**는 관찰.
- **수치**: 원시 포인트 = `dt(ms, uint32) · x(float32) · y(float32) · pressure(uint32, 0~32767)` 16바이트/점, 빅엔디안. 정규화 p = raw/32767. **틸트·방위각은 저장되지 않음** → 커뮤니티 로드맵에도 "Tilt Detection: Backlog". 스트로크 기본 굵기(`note_info` 1.9) 기본값 5.0, 샘플 파일에서 형광/브러시 계열 5.91px vs 3.54px.
- **3자 재구성 공식(추정, StarNote 원본 아님)**: rea-starnote가 클린룸으로 쓴 `width = base × ((1−s) + s × p^0.75)`, p≤0이면 base×0.2, 하한 0.5px — 실제 엔진과의 일치 여부 미검증. Platen은 이 수식을 채택하지 말고 실측(LOCAL_TODO ①)으로 커브를 뽑을 것.
- 출처: 위키 tools.md·pens.md [직접] · rea-starnote format-spec.md §4·§5 [직접] · rea-starnote `src/model/stroke-interpolator.ts` [직접] · 위키 roadmap.csv [직접] · App Store CN 리뷰 <https://apps.apple.com/cn/app/id6740199654> [스니펫] · 삼성멤버스 <https://r1.community.samsung.com/t5/태블릿/삼성노트-기능-제안/m-p/40042354> 계열 스레드 [스니펫] · 등급: 1차 관찰(파일)/2차 글/추정

### 2-4. 스무딩(안정화)·테이퍼
- **스무딩**: 명칭 "스트로크 안정화"(Stroke Stabilization). App Store 업데이트 노트: "만년필·아트 만년필(서예 펜)에 **획 안정성 파라미터 추가**, 개인 필기 습관에 맞게 조절". 위키: "엔진이 떨림을 자동 보정해 고배율에서도 깨끗". 저장 기본값 `smoothLevel 0.2`(0~1 스케일 추정). rea-starnote는 이를 Catmull-Rom 텐션으로 해석(추정).
- **테이퍼**: **전용 테이퍼 슬라이더는 발견되지 않음.** 끝처리는 (a) 소프트 페인트 펜의 "속도 기반 끝 테이퍼"와 (b) 만년필의 속도 파라미터(속도/속도 민감도 + 숨은 4구간 임계)로 구현된 것으로 추정. 테이퍼 길이 수치 **자료 부족** → LOCAL_TODO ②.
- 출처: App Store 미국/중국 버전 기록 [스니펫] · 위키 tools.md [직접] · format-spec.md [직접] · 등급: 공식 문서(스니펫)/2차/추정

### 2-5. 지연(레이턴시) 평가 — 삼성노트·굿노트 비교
- **관찰**: 마케팅 문구 "Proprietary Ink Engine", "low-latency", "paper-like feel"(공식). 한국 커뮤니티 인용: 디시 "스타노트도 내 생각에는 **아이패드 굿노트급의 필기감**이 나오는거 같다" / "그냥 종이에 쓰듯이 확대도 안 하고 빠르게 휙휙 쓰긴 했는데 필기감 좋네" / "필기 앱 중 스타노트가 가장 무난(업뎃도 자주하고 가격도 적당)". 유튜브 제목 "와.. 삼성노트보다 좋은데요?… 스타노트 상세리뷰"(본문 미열람). 반대로 디시 필기앱 가이드는 "필기 반응 속도는 어느 화면에서나 **삼성노트가 우위**"(StarNote 특정 언급 아님). 레딧: "Scribble to erase가 버벅이고 느림". 위키 FAQ에 "필기가 끊기면 백그라운드 CPU·저전력 모드 확인" 트러블슈팅 항목 존재 = 지연 민원이 있다는 간접 증거.
- **수치**: ms 단위 측정 자료 **없음**. 등급: 2차 글. → LOCAL_TODO(지연은 BRIEF상 삼성노트가 기준이므로 StarNote 측정은 선택).
- 출처: <https://m.dcinside.com/board/galaxy_tab/338457> · <https://gall.dcinside.com/mgallery/board/view/?id=galaxy_tab&no=343579> · <https://m.dcinside.com/board/galaxy_tab/341110> · <https://gall.dcinside.com/mgallery/board/view/?id=galaxy_tab&no=253209> · <https://www.youtube.com/watch?v=8r59q-53fbA> · <https://www.youtube.com/watch?v=WV5NyZmEHek> (모두 [스니펫]) · 위키 faq.md [직접] · r/StarNoteApp `t3_1qs7m0z` [직접 CSV]

### 2-6. 엔진 계보 (참고)
- 디컴파일 패키지에 `com.onyx.android.sdk.universal.*`(BOOX 노트앱과 공유되는 Onyx 범용 SDK), 네이티브 `libengine_sdk.so / libgraphicv2.so / libgraphenginev2.so / libarchive.so`, 손글씨 인식은 `com.sunia.HTREnginev2`(Sunia). 앱 코드는 패커(Baidu Protect / "Sagittarius")로 보호. 텍스트 박스는 WebView+ProseMirror. 벡터 기반(확대해도 픽셀화 없음).
- 출처: GitHub 코드 검색 결과(Dhyan2404/StarNotes 경로명) [직접] · jnshuiji/starnote-shortcut README [직접] · format-spec.md [직접] · 등급: 1차 관찰(파일명)/2차

---

## 3. 기능·UX 동선

| 항목 | 관찰 | 출처·등급 |
|---|---|---|
| 툴바 배치 | 상단 툴바(스타일러스 전용 토글 포함) + "숏컷 바"(쓰기 모드: 손가락/스타일러스 vs 펜 전용, 내보내기 아이콘) + 우상단 ⋯ 메뉴(내보내기 등). 라이브러리는 좌상단 톱니(설정). 펜 박스는 떠다니는 팔레트(레딧 "Floating Pen Box" 요청·"Pen Box moving" 버그) | 위키 faq/pdf_export/tools [직접]·문자열 `shortcut_bar_writing_mode*` [직접]·CSV [직접] · 2차/공식 |
| 펜 설정 동선 | 도구 **재탭(두 번 탭)** → 설정 팝업(위키). 압력 커브는 설정>스타일러스. 지우개 "자동 복귀"(지우고 펜 떼면 이전 도구로) 옵션 | 위키 tools.md [직접] · 2차 |
| 페이지·노트북·폴더 | 라이브러리: 그리드+표지, All/Notes/PDF 탭, **폴더·하위폴더**, **다단계 태그**(`Notes/Class Notes`), 고정(상한 있음), 휴지통, 스마트 검색, 정렬. 노트는 **무한 캔버스("BOUNDLESS", 2100×2100 타일 격자)** 또는 고정 비율 페이지. 페이지 추가/복제/삭제/재배열, 자동 페이지 추가, 페이지 비율 잠금, 북마크, 페이지 링크. **멀티탭** + 좌우/상하 **분할 화면** 문자열. 삼성멤버스: "4페이지 보기" 있음 | feature-inventory [직접]·format-spec [직접]·문자열 `tab_note_options_*` [직접]·삼성멤버스 [스니펫] · 1차/2차 |
| 올가미 | 자유 올가미 / 사각 올가미. 포함·제외 필터: 도형·사진/스티커·테이프·텍스트박스·손글씨·형광펜·링크·태그·**표**. 선택 후 이동·복사·크기·회전·복제. 1.3.4부터 이미지도 올가미 선택 | 위키 tools.md [직접]·Play 1.3.4 노트 [스니펫] · 2차/공식 |
| 텍스트 | 리치 텍스트 박스(글꼴·크기·B/I/U·정렬·5색 프리셋). "스타일러스 길게 눌러 텍스트 선택". 키보드 직접 타이핑 버튼은 요청 사항 | 위키·문자열·CSV [직접] |
| 이미지 | 앨범·카메라·문서 스캔, 크롭, 스티커(리소스 센터+사용자 가져오기), 이미지 1회 탭 즉시 선택(1.3.0 중국판 노트) | 위키·feature-inventory [직접]·중국 다운로드 사이트 [스니펫] |
| 도형 보정 | "단일 획 도형화": 획 끝에서 **펜을 멈춰 누르면** 도형으로 스냅. 옵션 "필압에 따라 굵기 조절되는 도형화". 도형 도구 자체는 직선·파선·화살표·축·중괄호·삼각/사각/다각/원/별·입체(원뿔·각기둥·구) 등 ~50종, 채움색·투명도 | 위키 tools.md [직접]·문자열 [직접] |
| 템플릿·표지 | §5 참조 | |
| 내부 링크 | 페이지 링크·북마크·텍스트 링크·발췌(Excerpt) 펜(다른 노트로 점프). **내보낸 PDF에서는 내부 링크가 동작하지 않음(v1.4.2 기준)**, "무한 캔버스에서 링크 깨짐" 스레드 | 위키 pdf_export [직접]·CSV `t3_1qut3y2` [직접] · 2차 |
| 검색 | 라이브러리 스마트 검색(제목·텍스트) + **AI 손글씨 인식 검색**(11개 언어, Pro). "Handwriting Search: In Progress"(커뮤니티 로드맵). 손글씨 OCR은 내보낸 PDF에 임베드되지 않음 | Play [스니펫]·위키 roadmap.csv·pdf_export [직접] |
| 내보내기 | PDF(편집 가능/평면화, 배경 포함 여부, 여백), PNG(다중은 zip), **`.note`**, 오디오. 로컬 사본은 항상 `storage/documents/starnote/export`에 저장 | 위키 backups/pdf_export [직접]·Play 2026-10-15 노트 [스니펫] |
| 다크 모드 | "라이트/다크 테마", 테마명 Classic Dark·Dark Forest·야간. PDF **스마트 반전**(1.3.4). 요청: 순수 블랙 AMOLED 테마(백로그) | 문자열·roadmap.csv·CSV `t3_1rzop9f` [직접] |
| 제스처 설정 | 메뉴 "제스처 설정": 한 손가락 두 번 클릭, 두 손가락 클릭, 세 손가락 클릭, **스타일러스 버튼(길게 누르기)** — 지원 목록 "SAMSUNG S Pen / VIVO Pencil 1 / Lenovo", 스타일러스 더블클릭(HUAWEI/OPPO/VIVO), 주 버튼 단일 클릭(XIAOMI). "한 손가락으로 쓰고 두 손가락으로 슬라이드" 모드, 손바닥 오작동 방지, 스타일러스 전용 모드, 줌 잠금. S펜 버튼 기본 동작은 자료 부족(단축키 미설정 안내 문자열 있음) | 문자열 [직접]·위키 setup/faq [직접]·Play 리뷰 [스니펫] · 공식/2차 |
| 레이저 포인터 | **있음**: 모드 선/점, **굵기**, **지속 시간** 조절. 전용 색상 세트 "레이저 펜". 레딧 "타이밍·움직임 개선" 요청 | 위키 tools.md·문자열·CSV [직접] |
| 표 삽입 | **있음(1.3.4, 2026 초)**: 헤더 행/열 토글, 행·열 **1~50**, 테두리 굵기, 셀 채움색. 셀에 일반 텍스트 입력 가능 여부는 질문 스레드만 존재 | 위키 tools.md·CSV [직접] · 2차 |
| 기타 | 테이프(마스킹, 능동 회상용; 삼성멤버스에서 삼성노트에 요청됨), 자, 돋보기(줌박스, 자동 전진), 녹음(잉크 동기), 타이머/스톱워치, 에빙하우스 복습, 노트 비밀번호(4~12자리)+생체, 레이어(추가/잠금/가시성/불투명도), QR | 위키·feature-inventory·문자열 [직접] |

---

## 4. 저장 포맷 — 판정: **부분** (읽기: 3자 공개 분석으로 상당 부분 해독 / 쓰기 호환: 미검증)

- **자체 확장자**: **`.note`**. 라이브러리에서 노트북 길게 누르기 → 내보내기 → "StarNote Format (.note)". 앱은 `.note`를 Android VIEW/SEND 인텐트(`ThirdPartyImportActivity`, 경로 패턴 `*.note`)로 연다 → `adb shell am start -a android.intent.action.VIEW -d file:///sdcard/Download/x.note` 식 상호운용 테스트 가능.
- **컨테이너(공개 분석)**: 암호화 없는 **표준 ZIP**. 루트 = 문서 UUID 폴더. 내부: 썸네일 PNG · `extra/pb/extra`(Protobuf 봉투: 스키마 버전 1, 앱 versionCode 5703, 계정 UUID, 문서 UUID) · `note/pb/note_info`(Protobuf + 내장 JSON: 레이어/타일 맵, 기본 굵기 5.0, 펜 기본 JSON, 기기 정보) · `virtual/doc|page/pb/*`(뷰포트·무한 캔버스 타일 배치) · `document/<uuid>/template/json/*.template_json`(속지 규칙) · **`shape/Page[r-c]#<uuid>#<ts>.zip`**(도형 레코드 Protobuf: UUID, 타임스탬프, ARGB 색 varint, 기본 굵기 float, 바운딩 박스 JSON, 3×3 행렬 JSON, 텍스트 스타일 JSON, **브러시 물성 JSON(§2-2)**, 도형 타입 id, 레이어 id, 선 스타일 JSON, 그룹 UUID, ProseMirror HTML) · **`point/Page[r-c]/…#points`**(원시 포인트 바이너리: 76B 헤더 + N×16B 포인트 + 4B 총시간 + 44B×M 트레일러) · `stash/`(실행취소 스냅샷) · `ftsContent/ ftsPageState/`(검색 인덱스).
- **도형 타입 id(관찰)**: 2 = 자유 스타일러스 획, 16 = 리치 텍스트 박스, 46 = 브러시/형광 동역학 획, 2000 = 타일 경계 프록시, (19 = 이미지, Onyx 공개 SDK 기준 **추정**). 파일 하나에 포인트 스트림이 지워진 과거 획도 남아 있음(용량 절감 목적 추정).
- **타일 좌표계**: 타일 2100×2100 단위, `Page[Y-X]`, 음수 인덱스로 상·좌 확장. 속지 줄간격 예 86.81 단위, A4 = 1485×2100.
- **백업·동기화**: 로컬 백업 폴더 **`Documents/StarNote/backup`**(상수 `BACKUP_RELATIVE_PATH="StarNote/backup"`), 로컬 백업 보관 간격 상수 86,400,000 ms(1일), 자동 백업 재시도 30초. 클라우드: **Google Drive · OneDrive** 바인딩 후 "자동 백업"(**단방향** 백업이라는 앱 내 고지), 코드상 Dropbox·Evernote·Baidu·Alibaba·WebDAV(NextCloud 안내문) 커넥터도 존재. 위키(약 2026 상반기)는 "실시간 동기화 없음, 복원은 수동·중복 생성"이라 했고, Play 노트는 **2026-09-02 "클라우드 동기화 추가"**, 10-15 "노트 많을 때 동기화 실패 수정". 사용자 보고: 재로그인 후 2일치 작업 소실, 백업이 수동이라 불편, 중국산+동기화 약함.
- **타 포맷 가져오기**: PDF·EPUB·DOCX·PPTX·CAJ·TXT·이미지(중국판 설명·Play). **`.goodnotes`·Noteshelf·`.sdocx` 가져오기 증거 없음** → 레딧 "굿노트→스타노트 마이그레이션" 스레드는 PDF 경유로 추정.
- **판정 근거**: 컨테이너·포인트 레코드·브러시 JSON·템플릿 JSON은 3자 공개 스펙으로 **읽기 가능** 수준까지 문서화됨(샘플 파일 294 레코드 검증). 그러나 (i) 도형 타입 전체·이미지·오디오 레코드 미확인, (ii) 3자 앱이 쓴 파일을 StarNote가 여는지 **미검증**, (iii) 코드가 패킹돼 공식 스키마(.proto) 없음 → **"부분"**. Platen 1차 범위(BRIEF: `.sdocx` 후속, `.goodnotes` 판정 대기)에 StarNote `.note`는 없으므로, **"읽기 전용 가져오기(획·색·굵기·압력)" 후속 과제**로만 적어 둔다.
- 출처: <https://raw.githubusercontent.com/NOT4LONG01/rea-starnote/main/docs/format-spec.md> [직접] · <https://raw.githubusercontent.com/NOT4LONG01/rea-starnote/main/docs/feature-inventory.md> [직접] · Dhyan2404/StarNotes `BackupConstant.java`·`BackupFilePaths.java`(디컴파일 상수) [직접] · 위키 backups.md·sync.md [직접] · 문자열 `cloud_backup_tips`·`webdav_login_tip` [직접] · Play 2026-09-02/10-15 노트 [스니펫] · 등급: 1차 관찰(3자 바이트 검증)/공식(문자열)/2차

---

## 5. 템플릿·속지

- **내장 속지**: 앱 문자열에 `template_*` **83개**(중복 포함). 종류: 빈 종이·가는/굵은 가로줄(12/16/20줄)·좁은/넓은 줄·격자(Small/Big/20×20/25×26/30×16)·점 격자·세로줄·코넬(A/B/C)·4선 영어·톈쯔(田字)·오선/사선/육선 악보·일간/주간/월간 플래너·습관 추적(주/월/100일)·To-do·시간표·수업 계획·독서 노트·오답 노트(A/B)·SWOT(3종)·4분면·McKinsey·Accenture·Todai·가계부·연간 일정·법률 서식·스포츠 작전판(농구/축구/테니스/셔틀) 등. 중국 블로그는 "약 50종 용지 템플릿"으로 소개(버전 차이). "추천 용지", 용지 적응/자유 이동 옵션, 노트 전체 템플릿 변경 경고 문구 존재.
- **표지**: `cover_*` **73개**(과일·동물·그래픽·일러스트·단색 과목 라벨·2026-A~D 연간). App Store 노트 "2026 커버 템플릿 추가". 사용자 표지 교체는 PDF/PNG/JPG 또는 StarNote 전용 표지 파일(전용 "표지 변경" 버튼 없음 — NozomuNoto 가이드).
- **사용자 템플릿**: 가능(레딧 "How to create Custom Templates (Visual Guide)"), 빠른 노트 기본 템플릿 지정(설정>문서 설정). 템플릿 JSON 필드: `universalShapeType`(`layout_horizontal_line_fitted`/`layout_grid`/`layout_cornell`), `spacing`, `strokeAttr{color,width 0.7875,roundCorner}`.
- **하이퍼링크 PDF 플래너**: 지원하되 **읽기 모드(펼친 책 아이콘)에서만 링크 탭 가능**; 쓰기 모드에선 획이 그려짐. 판매자(NozomuNoto)가 StarNote Android 전용 "알려진 문제" 페이지를 운영할 정도로 주의점 많음(ZIP·표지·스티커 파일을 잘못 가져오는 문제, 링크 1개 먼저 테스트 권고). Play 리뷰: "펜은 쓰고 손가락은 탐색 — 모드 전환 없이 하이퍼링크 노트북 쓰기 편함(삼성노트 읽기모드보다 낫다)".
- **스티커**: 리소스 센터 제공 + PNG 가져오기 + "스티커에 추가"(자기 드로잉 저장은 백로그). 테이프(마스킹) 도구는 스티커와 별개.
- 출처: 문자열 `res/values/strings.xml` [직접] · ababtools <https://ababtools.com/?post=4038> [스니펫] · App Store 버전 기록 [스니펫] · NozomuNoto <https://nozomunoto.com/faqs/starnote-android-known-problems/> 외 FAQ 시리즈 [스니펫] · r/StarNoteApp `t3_1q8rtq9` [직접 CSV] · format-spec.md §6 [직접] · Play 리뷰 [스니펫] · 등급: 공식(문자열)/2차

---

## 6. 사용자 평가 (반복 패턴)

**장점 3**
1. **필기감** — 디시 "굿노트급", "빠르게 써도 좋다", Play 리뷰 "펜 설정 덕에 글씨가 더 단정해 보인다", App Store CN "만년필 손맛 좋다". (2차)
2. **가격·무료 범위** — 일회성 Pro(≈US$6), 무료로도 "충분", 유튜브 "삼성노트+플렉슬+노트인 장점을 모은 무료 앱". (2차)
3. **기능 밀도** — 무한 캔버스·레이어·태그·테이프·멀티탭·4페이지 보기·표·레이저·녹음; 삼성멤버스에서 삼성노트에 StarNote 기능(테이프·멀티탭·4페이지)을 요구하는 글 다수. (2차)

**단점 3**
1. **백업·동기화 신뢰성** — 수동 백업, 단방향 클라우드, 재로그인 후 데이터 소실 보고, 크래시/필기 소실 보고(应用宝·Play), "device sync가 치명적 약점" 스레드. (2차)
2. **플랫폼·연동 한계** — Windows 없음(레딧 최다 요청), 내보낸 PDF에 링크·OCR 미포함, 무료 10노트/내보내기 10개 제한. (2차)
3. **개발사 신뢰·완성도** — "중국 개발사·동기화 약함"(삼성멤버스), 미번역 중국어 UI 잔존(문자열 확인), 레이어·태그 "미성숙", 흘림 지우기 지연, 펜 박스 상한. (2차/공식 문자열)

- 출처: §2-5 디시 링크 · <https://community.linkareer.com/honeytips/1869249> [스니펫] · r/StarNoteApp CSV(`t3_1qnb53y`, `t3_1pc66r2`, `t3_1mqyzdy`, `t3_1qbntrs`, `t3_1n9tf1e`) [직접] · <https://sj.qq.com/appdetail/com.onyx.galaxy.note/review> [스니펫] · Play 리뷰 [스니펫] · 삼성멤버스 [스니펫] · 열람 2026-10-24

---

## 7. 가져올 원리 (Platen 적용)

1. **펜 종류별로 노출 파라미터를 다르게** — 만년필(압력·두께·안정화), 볼펜(두께·안정화만, 압력 무시), 연필(압력·두께·밀도), 캘리(방향 기반 굵기). 사용자는 "펜마다 다른 슬라이더"를 자연스럽게 받아들인다(레딧 "pen settings 공유" 문화). BRIEF의 "사용자 조절 가능(굿노트식)"과 양립.
2. **원시 입력과 렌더 파라미터의 분리 저장** — 포인트 스트림(dt·x·y·p)을 그대로 보존하고 브러시 물성(JSON: pressureSensitivity·smoothLevel·velocity 4구간·alpha·brushShape)을 획 단위로 저장 → 나중에 설정을 바꿔도 재렌더 가능, 외부 브러시(Procreate/ABR) 임포트 모델과도 호환되는 구조. Platen 포맷 설계에 그대로 채택(필드명은 독자 정의).
3. **테이퍼는 "속도" 축으로** — 전용 테이퍼 슬라이더 대신 속도 민감도 + (저속 무시 임계 / 저속 상한 / 고속 하한) 구간 모델. Platen은 이 구간 모델을 **테이퍼 길이·끝 굵기 비율**로 번역해 노출(사용자에겐 "끝처리" 슬라이더, 내부는 속도 함수).
4. **안정화는 저강도 기본값(≈0.2/1.0) + 한글 전용 튜닝** — StarNote가 한글에서 보정 거동이 다르다는 관찰을 반영해, 짧은 획·급격한 방향 전환(ㄱ·ㄴ·ㅈ)에서 코너를 보존하는 지그재그 테스트(LOCAL_TODO ③)를 합격 기준으로.
5. **도구 설정 동선 = 활성 도구 재탭** + 지우개 자동 복귀 + S펜 버튼 길게 누르기 매핑 테이블(제스처 설정 메뉴에 손가락 2/3탭과 함께 배치). 레이저(선/점·굵기·지속시간)와 표(1~50행·열·헤더·테두리·채움)의 파라미터 셋은 BRIEF 3R 항목의 기본 스펙으로 차용.

## 8. 버릴 것
- Pro를 **계정 로그인에 묶는 구조**, 단방향 클라우드 "백업"을 동기화로 부르는 UX(Platen은 Drive 양방향·오프라인 우선).
- **틸트 미저장** 포맷(Platen은 tilt·orientation 필드 예약 — 연필 질감에 필요).
- 80+ 템플릿·70+ 표지 라이브러리(도구 앱 원칙 "캔버스 > 장식"; 소수 기본 + 사용자 PDF/이미지 속지).
- 손글씨 미화·AI 포인트 과금·OCR 11개 언어(1차 범위 밖, 스트로크 보존 우선).
- 내보낸 PDF에서 깨지는 내부 링크, 미번역 UI 문자열, 코드 패킹.

## 9. LOCAL_TODO 후보 (갤럭시 탭 S9+에 설치된 StarNote 실측)
① **필압 3단계 획 → 픽셀 폭**: 만년필, 두께 고정(예 0.50mm), 압력 감도 0% / 50% / 100% 각각에서 약·중·강 3단계 필압 가로획 → 스크린샷(100% 줌) → 양식: `설정값 | 필압단계 | 최대폭px | 최소폭px | 시작점 blob 유무`. 볼펜·연필도 동일 양식 1줄씩.
② **테이퍼 길이**: 만년필 속도 0% / 50% / 100%에서 빠른 "휙" 획 → 끝 테이퍼 길이(px)와 끝 폭 비율 측정; 소프트 페인트 펜 1회 대조.
③ **지그재그 스무딩 비교**: 안정화 0 / 50 / 100%로 "ㅈㅈㅈ·WWW" 지그재그 → 코너 라운딩 정도·지연감 메모; 삼성노트 동일 획과 나란히 스크린샷.
④ **저장 파일 샘플 확보**: 3획짜리 노트를 "StarNote Format(.note)"로 내보내고 PC로 복사 → `file sample.note` / `unzip -l sample.note` 출력 기록(§4 구조와 대조, 포맷 버전·versionCode 확인). 추가로 `Documents/StarNote/backup` 폴더 목록 1회.
⑤ (보조) 펜 설정 팝업 스크린샷 1장: 슬라이더 **범위·스텝 수·단위**, 진입 경로(재탭/길게 누르기) 확정 + 설정>정보의 버전 문자열.

---

## 10. 출처 목록 (열람일 2026-10-24)

**직접 열람(GitHub raw / 코드 검색)**
- NakoTurtle/StarNote-Unofficial-Wiki — README·docs/{index,setup,pens,tools,features,backups,sync,faq,pdf_export.md,roadmap.csv}·data/cleaned_feature_data.csv(r/StarNoteApp 940개 스레드 제목·점수) — <https://github.com/NakoTurtle/StarNote-Unofficial-Wiki> · 2차 글(비공식 위키, "개발사 제작 아님" 고지)
- NOT4LONG01/rea-starnote — README·docs/feature-inventory.md·docs/format-spec.md·docs/android.md·src/model/stroke-interpolator.ts — <https://github.com/NOT4LONG01/rea-starnote> · 1차 관찰(APK 정적 분석 + 샘플 .note 바이트 검증) / 수식은 추정
- Dhyan2404/StarNotes — `decompiled/apktool_project/res/values{,-ko}/strings.xml`, `AndroidManifest.xml`, `BackupConstant.java`, `BackupFilePaths.java`(v1.3.0 글로벌판 디컴파일) — <https://github.com/Dhyan2404/StarNotes> · 공식 문서(앱 내 문자열·상수)
- jnshuiji/starnote-shortcut README(중국판 패키지·패커·설정 메뉴 구조 언급) — <https://github.com/jnshuiji/starnote-shortcut> · 2차
- Zy0x/YouTube-Revanced 레지스트리 JSON(패키지명·버전 1.4.3/1.5.2/1.5.3, 2026-10-20) · AmpleReVanced/revanced-patches `Constants.kt`(XAPK 형식) — 2차

**검색 스니펫 경유(직접 페치 차단)**
- Google Play 글로벌/한국어/인도/캐나다 리스팅 및 "새로운 기능"(2026-09-02, 2026-10-15) — <https://play.google.com/store/apps/details?id=com.onyx.galaxy.global.note> · 공식 문서(스니펫)
- App Store 미국 <https://apps.apple.com/us/app/starnote-handwriting-pdf/id6751570915> · 중국판 <https://apps.apple.com/cn/app/id6740199654> (버전 기록·리뷰) · 공식/2차
- Galaxy Store <https://galaxystore.samsung.com/detail/com.onyx.galaxy.global.note> · 공식
- hunted.space/Product Hunt <https://hunted.space/product/starnote> · moge.ai <https://moge.ai/ko/product/starnote> · AppBrain <https://www.appbrain.com/app/starnote-handwritten-notes/com.onyx.galaxy.global.note> · 2차
- 텐센트 응용보 <https://sj.qq.com/appdetail/com.onyx.galaxy.note> · 7xz <https://www.7xz.com/softs/28625.html> · ababtools <https://ababtools.com/?post=4038> · 夜雨聆风 <https://www.yeyulingfeng.com/95906.html>, <https://www.yeyulingfeng.com/175884.html> · 2차
- 디시 갤럭시탭 갤러리 <https://m.dcinside.com/board/galaxy_tab/338457> · <https://m.dcinside.com/board/galaxy_tab/341110> · <https://m.dcinside.com/board/galaxy_tab/347696> · <https://gall.dcinside.com/mgallery/board/view/?id=galaxy_tab&no=343579> · <https://gall.dcinside.com/mgallery/board/view/?id=galaxy_tab&no=253209> · 2차
- 유튜브 <https://www.youtube.com/watch?v=WV5NyZmEHek> · <https://www.youtube.com/watch?v=8r59q-53fbA> · <https://www.youtube.com/watch?v=ZLdCWNxX7Cg> (제목·설명만) · 2차
- 삼성멤버스 "삼성노트 기능 제안" 계열 <https://r1.community.samsung.com/t5/태블릿/삼성노트-기능-제안/m-p/40042354> 외(스타노트 언급 10건) · 링커리어 <https://community.linkareer.com/honeytips/1869249> · 2차
- NozomuNoto StarNote Android FAQ 시리즈 <https://nozomunoto.com/faqs/starnote-android-known-problems/> · <https://nozomunoto.com/faqs/starnote-android-use-hyperlinks/> · <https://nozomunoto.com/faqs/starnote-android-change-cover/> · 2차
- TikTok 공식 계정 검색 페이지 <https://www.tiktok.com/discover/starnote-pen-setting> ("두께·안정화 완전 커스텀" 문구) · 공식(스니펫)

**자료 부족으로 남긴 항목**: 원화 가격 · 슬라이더 스텝 수 · 테이퍼 길이 수치 · 지연 ms · S펜 버튼 기본 동작 · 펜 박스 상한 수치 · 공식 도움말 사이트(앱 내 "도움말 및 피드백"만 확인) · 유튜브 리뷰 본문의 삼성노트 대비 구체 표현.

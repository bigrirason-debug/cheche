# 타자기 디자인 레퍼런스 — Platen

> 작성일·열람일: **2026-10-24**. DESIGN.md의 재료. BRIEF v3의 확정 은유(마진 스톱=구조 / 캐리지=구조 / 페이지 피드=암시 / 탭 스톱=후속 / 플래튼 노브=안 함)와 모순되지 않게 작성.
>
> **등급 표기**: `공식` = 제조사·재단·플랫폼 1차 문서 / `1차` = 박물관 기록·실물 관찰·매뉴얼 / `2차` = 리뷰·블로그·판매 목록 / `추정` = 수치·색을 본인이 추정.
> **접근 표기**: `[직접]` = URL을 직접 열어 본문 확인 / `[스니펫]` = 이 환경의 프록시가 해당 호스트를 차단(403)해 **검색엔진 결과 스니펫만** 확인. 스니펫 경유 항목은 등급을 한 단계 낮춰 읽을 것.
> **색상 hex 원칙**: 아래 모든 타자기 바디·리본·종이 hex는 **문자 기술(“sea-foam green”, “red ABS”)로부터의 추정값**이다. 이미지에서 샘플링하지 않았다. 시안 단계에서 실제 에셋을 찍거나 구해서 다시 샘플링할 것.
> **환경 메모(네트워크 정책, 코디네이터 확인)**: 이 세션의 프록시는 wikipedia·moma·ia.net·learn.microsoft.com·m3.material.io·support.google.com·fonts.google.com·x.com·대부분의 디자인 사이트를 403으로 차단한다. 되는 것은 **WebSearch(스니펫)** 와 **raw.githubusercontent.com** 뿐(github.com 리포 페이지도 이번 세션에선 일부 열렸으나 보장되지 않음). 따라서 **모든 폰트 라이선스는 폰트 리포의 raw 라이선스 파일(OFL.txt / LICENSE)로 직접 확인**했고(§5, §9 S1~S13), 그 외 항목은 스니펫 경유다. 차단 호스트는 재시도하지 않았다.

---

## 0. 요약 (한 화면)

- **구조 은유 중 가장 쓸 만한 매핑 3개**: ① 마진 스톱 → 줄 가이드·속지 여백 핸들(눈금자 위 두 개의 빨간 마커) ② 캐리지 → 페이지 스트립·좌우 페이지 이동 ③ 2색 리본 → 색 시스템(검정 잉크 기본 + 빨강 단일 포인트) + 펜의 검정/빨강 즉시 토글(리본 셀렉터).
- **방향 A 핵심 레퍼런스**: Hanx Writer(이미지 에셋 전면 스큐어모프의 장단), iOS 6 Notes(종이 질감을 필기면에 깔면 생기는 문제) + 2025 네오-스큐어모피즘 원칙(깊이는 크롬·주요 액션에만, 밀도 높은 화면은 평면).
- **방향 B 핵심 레퍼런스**: iA Writer(Plex 기반 Mono/Duo/Quattro, 포커스 모드, 라이트/다크), Bear(빨강 단일 포인트 `#DD4C4F`, 2차) + Ulysses(크롬 다크 ↔ 시트 라이트 분리).
- **추천 서체 조합**: **IBM Plex Mono + IBM Plex Sans KR** (둘 다 OFL 1.1, 한 가족이라 x-height·획 대비가 맞음) / 대안 **Courier Prime(OFL) + Pretendard(OFL)**. 모노 한글 대체는 **D2Coding(OFL 1.1)**.
- **버릴 것**: 모노 서체 전면 사용, 캐리지 리턴 모션, CSS로 그린 재질, 타입바·기계 그림, 소리.

---

## 1. 실제 타자기 해부 → UI 은유 후보표

근거: Smith-Corona 사용 설명서(Zephyr Deluxe / Portable) 스캔 PDF, 빈티지 타자기 FAQ, 1945 AMES 정비 매뉴얼 색인 — 출처 S19 `[스니펫]` `1차(매뉴얼)/2차(FAQ)`. 2색 리본은 S20, 키 톱은 S21.

| 부품 | 실물 기능 (한 줄) | UI 매핑 후보 | 방향 A(실사) 표현 | 방향 B(미니멀) 표현 | 권고 | 이유 |
|---|---|---|---|---|---|---|
| **플래튼** (platen) | 종이를 감는 고무 원통. 타이프가 두드리는 받침 | 페이지 **뒤**의 받침면(페이지 밖으로 보이는 띠). 종이 자체는 건드리지 않음 | 페이지 상·하 가장자리 밖으로 보이는 검은 고무 원통 **사진 에셋**(그라데이션 코드 금지) | 없음(평면 배경색만) | **암시(A) / 제외(B)** | 종이는 깨끗해야 하고, 플래튼을 CSS로 그린 것이 “못생김” 판정의 원인이었음 |
| **플래튼 노브** | 플래튼을 손으로 돌려 종이 이송 | 스크롤 휠·페이지 이송 다이얼 | — | — | **제외(확정)** | BRIEF 2b |
| **페이퍼 베일** (paper bail) | 롤러 달린 막대. 종이를 플래튼에 밀착시키고, **여백 설정용 눈금자**를 겸함(S19 Smith-Corona 매뉴얼) | 페이지 상단 **고정 바 = 툴바 프레임**. 눈금(ruler) 수용 위치 | 금속 막대 + 고무 롤러 사진 에셋을 툴바 배경으로. 그 위에 아이콘 | 얇은 상단 스트립. 눈금 틱은 “자”의 구조이지 선화 장식이 아니므로 허용(틱은 잉크색 20% 농도, 추정) | **구조** | 툴바는 어차피 필요하고, 베일은 실물에서도 눈금자를 겸해 “눈금 + 마진 마커”가 자연스럽게 얹힘 |
| **페이퍼 가이드** (paper guide) | 종이 왼쪽 끝을 맞추는 이동식 턱 | 페이지 정렬·용지 크기 선택 | 베일 왼쪽 작은 금속 턱 | — | **제외(후속)** | 마진 스톱과 역할이 겹침. 용지 크기는 설정 화면에서 해결 |
| **캐리지** (carriage) | 플래튼·종이를 싣고 좌우로 이동하는 틀 | **페이지·노트 이동, 페이지 스트립(썸네일 띠)**, 좌우 스와이프 | 페이지 좌우에 캐리지 레일 사진 에셋(가장자리만) | 페이지 스트립만. 레일 그림 없음 | **구조(확정)** | BRIEF 2b. 모션은 정체성이 아님(Carriage 교훈) — 이동은 기본 플랫폼 트랜지션 |
| **캐리지 리턴 레버** | 캐리지를 오른쪽 끝으로 되돌리고 한 줄 이송 | “다음 페이지/새 페이지” 액션, “첫 페이지로” | 레버 **형태의 아이콘**(이미지 아님) | 아이콘 | **구조(아이콘 1개)** | 레버 모션은 폐기 교훈. 아이콘 의미만 차용 |
| **마진 스톱** (margin stop) | 좌·우 여백 위치를 잡는 슬라이더(S19 “key, lever, or button used to adjust the setting of margins”) | **줄 가이드·속지 여백 핸들**: 눈금자 위 좌·우 2개 마커. 드래그로 속지 여백·줄 가이드 폭 조정 | 베일 위 작은 금속 슬라이더 사진 에셋 | 눈금자 위 **빨간 삼각 마커 2개**(빨강 포인트의 정당한 쓰임) | **구조(확정)** | BRIEF 2b. 실물에서도 베일 눈금과 짝이므로 베일=툴바 결정과 맞물림 |
| **마진 벨** | 우측 여백 n자 전에 종 울림 | 페이지 끝 경고 | — | — | **제외** | 소리는 정체성 아님. 시각 대체도 필기엔 불필요 |
| **탭 스톱** (tab stop) | 캐리지가 멈출 열 위치 설정 | 스냅 가이드·열 그리드·**표 삽입의 열 위치** | 눈금 아래 작은 핀 | 눈금 아래 점 | **후속(확정)** | BRIEF 2b. 표 기능(3R)과 함께 설계 |
| **타입 가이드** (type guide) | 타입바가 종이를 치는 지점의 금속 조준부. 중앙 맞춤에도 쓰임(S19) | S펜 **호버 커서/현재 도구 표시** | — | 호버 커서를 작은 노치형으로(추정) | **암시(선택)** | 필기감에 영향 없는 범위에서만. 못 생기면 바로 제거 |
| **타입바** (typebar) | 활자가 달린 지렛대 다발 | 펜 로스터(도구 묶음) | 타입바 바스켓 사진 | — | **제외** | B는 “기계 그림 없음”이 규칙, A에서도 필기면 근처에 기계 묘사는 소음 |
| **리본(2색)** | 위 검정/아래 빨강 반반 리본. 셀렉터로 어느 반을 칠지 선택(S20) | ① **색 시스템**: 검정 잉크 기본 + 빨강 단일 포인트 ② 펜의 **검정/빨강 즉시 토글**(리본 셀렉터 3단: 검정·빨강·스텐실 → 검정·빨강·지우개로 번안 가능) | 셀렉터 레버 아이콘 | 두 색 점 토글 | **구조** | 2색 리본은 BRIEF의 “검정/빨강 2색” 결정의 실물 근거. 토글은 실제로 유용(강조 필기) |
| **리본 바이브레이터** | 타자 순간 리본을 들어올리는 부품(S20 FAQ) | — | — | — | **제외** | UI 대응물 없음 |
| **라인 스페이스 레버** | 플래튼 회전량 1·1.5·2줄 선택(S19 FAQ) | **줄 가이드 간격 프리셋 토글**(예: 6/7/8 mm — 값은 추정, DESIGN.md에서 확정) | 레버 아이콘 | 3단 세그먼트 | **구조** | 마진 스톱(여백)과 짝을 이뤄 “속지 = 마진 + 줄 간격” 2개 컨트롤로 정리됨 |
| **용지 받침** (paper table) | 플래튼 뒤에서 종이를 받치는 판(S19 Zephyr 도해에 명칭만 확인) | 페이지 **바깥 배경**(책상/판). 페이지 썸네일이 놓이는 면 | 도장 금속판 사진 에셋 | 단색 배경 | **암시(A) / 배경색(B)** | 배경은 조용히(BRIEF 4) |
| **키 톱** (keytop) | 둥근 키. 1930년대 유리+니켈/크롬 링, 이후 베이클라이트·플라스틱(S21, 2차) | 원형 **도구 버튼** | 실물 키 톱 **사진 에셋** 버튼(CSS 링·광택 금지) | 원형 아이콘 버튼, 링 없음 | **구조(A, 에셋 필수) / 제외(B)** | 키 톱은 스큐어모프 UI에서 제일 먼저 “싸구려”가 되는 부위. 이미지 아니면 하지 않음 |
| **페이지 피드**(종이 삽입) | 플래튼 뒤로 종이를 넣고 돌려 올림 | 페이지 상단이 베일 아래로 “물려 있는” 모습 | 페이지 상단 가장자리가 베일 에셋 아래로 들어감 | 페이지 상단만 미세 그림자 | **시각 암시(확정)** | BRIEF 2b |

**은유표 결론**: 구조로 채택할 것은 **베일(툴바 프레임+눈금) · 마진 스톱 · 라인 스페이스 레버 · 캐리지(페이지 스트립) · 2색 리본(색 시스템+토글) · 키 톱(A만)**. 나머지는 암시 또는 제외.

---

## 2. 실제 타자기 색·재질 팔레트

모든 hex는 **추정**(문자 기술 기반). 기종별 근거 등급은 표기.

| 기종 | 근거(출처·등급·접근) | 바디 색 기술 | hex 근사(추정) | 메모 |
|---|---|---|---|---|
| Olivetti **Valentine** (1968 설계/1969 생산, Sottsass + Perry King) | MoMA 소장 4576 “ABS plastic”, Nationalmuseum “bright red… pop art”, Domus 1969 “red and made of ABS” — S22 `1차` `[스니펫]`; 색상 변형은 S23 `2차` | 선명한 빨강 ABS. 그 외 흰·초록·파랑(박물관 기록), 달걀 노랑·연회색(2차). 초록/파랑은 쇼룸용 극소량(경매사) | 빨강 `#D5342B`, 흰 `#EDE7DA`, 초록 `#7FA86A`, 파랑 `#4F86B5` | “Valentine red”가 이 기종의 표식(S32 Finestre sull'Arte, 2차). **Olivetti 공식 브랜드 hex는 공개된 것이 없음**(S32) |
| Olivetti **Lettera 22** (1950, Nizzoli + Beccio) | Archivio Storico Olivetti, Centre Pompidou, HNF 기록 — S24 `1차` `[스니펫]` | 공식 색 목록 미확인. 판매 기술 “pastel colors”, 대표 개체는 청회색/청록 | 청회색 `#9FB3B4`, 베이지 `#CFC4A8` | 1954 Compasso d'Oro, MoMA 소장. 색 목록은 **자료 부족** |
| Olivetti **Lettera 32** (1963, Nizzoli) | Wikipedia Olivetti typewriters, HNF — S25 `1차` `[스니펫]`; 색은 경매 목록 `2차` | 청록/청회색이 대표. 파랑·황록·빨강 개체 보고 | 청록 `#7FA7A3`, 황록 `#B9BE7A` | 금속 바디 |
| **Hermes 3000** (1958, Paillard, Yverdon) | Wikipedia — S26 `1차` `[스니펫]` | “light green… mint or sea-foam green”. 1966 이후 각진 바디·색 미세 변화 | `#B4D1C0`(추정, 세이지 쪽 `#A9C4B0`) | 키 톱은 같은 계열 연녹 |
| **Olympia SM3** (1950년대) | typewriterdatabase 개체 기록 — S27 `1차(개체)` `[스니펫]` | 크링클(주름) 도장: 회색, 짙은 녹색, 버건디, 드물게 크림. 갈색 베이클라이트 키, 크롬 하이라이트 | 회색 `#8C8D86`, 녹 `#3F5A4A`, 버건디 `#6B2A33`, 크림 `#E8E0CC` | **재질 참고 1순위**: 크링클 도장 + 크롬 링 — A안 에셋 촬영 대상 |
| **Olympia SM9** (1964~) | boffosocko, museum-digital, typewriterdatabase — S28 `1차(개체)` `[스니펫]` | 투톤: 오프화이트/크림 상판 + 짙은 회색 하판(“Altweiss-Dunkelgrau”), 백·청록 키 | 크림 `#EDE6D6`, 짙은 회 `#4A4B4A` | 투톤 = “밝은 종이 영역 + 어두운 크롬” 구도의 실물 근거 |
| **Smith-Corona Silent Super** (1950년대) | typewriterdatabase 개체 — S29 `1차(개체)` `[스니펫]` | Seafoam Green, Alpine Blue(나란히 두면 구분 어려움) | 시폼 `#9FC5B8`, 알파인 블루 `#8FB6C6` | “desert sand” 등 다른 색은 미확인 |
| **IBM Selectric** (1961, Eliot Noyes) | IBM History “eight color combinations”, Henry Ford 562990 “red, black, silver” — S30 `공식/1차` `[스니펫]` | 8색 조합(전체 목록 미확인). 빨강·검정·은 예시 | 빨강 `#A8322A`, 은 `#B8B8B4` | Olivetti에서 조형 영감(IBM 기록) |
| **Underwood** (No.5 등) | 이번 검색에서 결과 없음 → **자료 부족** | 일반 지식: 검정 광택 에나멜 + 금색 데칼(미확인) | 검정 `#141414`, 금 `#C9A227` (추정) | 사용 시 재확인 |

**2색 리본 전형색** (S20 `2차/특허`): 위 검정·아래 빨강 반반, 셀렉터로 선택. 실제 타이프 인상은 순흑이 아니라 섬유에 눌린 **온화한 검정**, 빨강은 **주홍 기운의 진홍**.
- 검정 잉크 인상: `#1C1A17` (추정)
- 빨강 인상: `#B3261E` ~ `#C8322B` (추정)

**종이 색 후보** (추정): 크림 타자 용지 `#F6F1E7` / 따뜻한 크림 `#F3EDDF` / 흰 본드지 `#FAFAF7`. 다크 종이는 §7.

**키 톱 재질 메모** (S21 `2차` `[스니펫]`): 1930년대 Royal — 유리 + 니켈 링, 금/검정 글자. Smith-Corona 1935/38 — “유리처럼 보이지만” 금속 링을 두른 베이클라이트/플라스틱. 초기 IBM 전동 — 셀룰로스·베이클라이트에 래커. **교훈**: 실물도 ‘유리 느낌 플라스틱’이었다. 즉 **사진 에셋**이면 충분히 성립하고, 코드로 광택을 그리면 바로 티가 난다.

---

## 3. 방향 A 레퍼런스 (실사 스큐어모픽)

| # | 대상 | 출처 | 좋은 점 | 나쁜 점 | 수치 | 재질 표현 | 판정 |
|---|---|---|---|---|---|---|---|
| A1 | **Hanx Writer** (2014, Hitcents × Tom Hanks, iPad) | S33 BGR·Cayman Compass·City A.M. `2차` `[스니펫]` | 실제 소장 타자기를 **그대로 찍어** 키보드·바디를 만든 전면 이미지 에셋. 출시 1주 만에 iPad 무료 1위, 1,600+ 리뷰 4.5★ — 실사 스큐어모프가 대중 소구력이 있음을 증명 | 정체성의 절반이 **소리·모션**(“clickity clack”, “ding”)이라 우리 전제와 반대. 기계가 화면 하단 절반을 차지 → 쓰는 면이 좁음. 유료 기종(Hanx 707 등) IAP | 미확인(색·간격 수치 없음) | **이미지**(촬영 기반, 추정) | 참고: “에셋은 실물에서”. 반면교사: 기계가 필기면을 침범하면 안 됨 |
| A2 | **iOS 6 Notes** (2007~2013) | S34 Macworld·Wikipedia Notes(Apple)·iMore `2차` `[스니펫]` | 노란 리걸패드 + 줄 + 종이 질감 = 사물로서의 종이. 기본 서체 Noteworthy / Marker Felt / Helvetica 선택 | **질감이 필기면 위에** 깔려 있어 iOS 7에서 흰 종이로 교체됨(“white paper-texture screen”). 서체 선택도 삭제 | — | 이미지(시스템 텍스처 에셋, 추정) | 반면교사: 질감은 **크롬에만**, 종이는 깨끗하게(BRIEF 4와 일치) |
| A3 | **2025 네오-스큐어모피즘 / Apple Liquid Glass** | S35 TechRadar·Clay·UXmatters·uiverse `2차` `[스니펫]` | “깊이는 아이콘·히어로·주요 액션에, 밀도 높은 화면은 평면”(Clay). 과도한 리얼리즘은 소음·렌더 비용·주의 분산(Clay) | 유리 재질을 **셰이더/코드로** 만들었음 → 우리 교훈상 재질 코드 재현은 탈락 | — | **코드**(셰이더) → **교훈상 탈락**(원칙만 차용) | 원칙 채택: 깊이의 위치 선정 |
| A4 | **Freewrite Smart Typewriter**(하드웨어, Astrohaus) | S37 getfreewrite 제품 페이지·Good e-Reader `공식/2차` `[스니펫]` | “타자기”를 이름으로만 쓰고 기계 묘사는 없음. E Ink 패널 + 기계식 키(Kailh Box Brown). 특별판 Ghost(투명), Cosmic(검정 스페클) | 리뷰에 “busy, almost four pounds”(초기 모델). 알루미늄 바디 여부 이번 검색에서 미확인 | 미확인 | 실물 | A·B 공통 참고: **재질은 실제 사물**, 그래픽은 타이포만 |
| A5 | **실물 기종 촬영**: Olympia SM3 크링클+크롬, SM9 투톤, Hermes 3000 시폼 | §2 S27·S28·S26 | 에셋을 **직접 촬영/구매**하면 “진짜 이미지” 조건을 만족 | 저작권: 타사 로고·엠블럼은 프레임 밖. 기종 외관 자체는 의장권 만료 기종(1950~60년대)으로 제한 | — | 이미지(촬영) | A안 에셋 조달 계획의 기준 |

**방향 A 권고**: 에셋은 (1) 고무 플래튼 띠, (2) 금속 베일 바(+눈금), (3) 마진 스톱 슬라이더, (4) 키 톱 버튼, (5) 용지 받침 판 — **5종 이내**. 그 외는 평면. 다크 모드에서는 금속을 어둡게 찍은 **별도 에셋 세트**가 필요(코드 색보정으로 돌리면 못생김 재발 위험).

---

## 4. 방향 B 레퍼런스 (미니멀 + 타자기 암시)

| # | 대상 | 출처 | 색 hex | 서체 | 행간·간격·라운드 | 빨강 포인트 비율 | 다크 모드 | 메모 |
|---|---|---|---|---|---|---|---|---|
| B1 | **iA Writer** | S18·S36 ia.net(스니펫), App Store, iA-Fonts 리포 `[직접]` | **미확인**(공식 hex 없음). 서드파티 Obsidian 테마가 권하는 액센트 `#18BEEC`은 iA 공식 아님(2차) | iA Writer **Mono / Duo / Quattro** — IBM Plex 기반(리포 README `[직접]`). Duo: m·M·w·W에 1.5폭. Quattro: ½·¾·1·1.5의 4폭(2차) | 미확인 → **자료 부족** | 빨강 아님(파랑 커서·하이라이트 6색 선택) | Light/Dark/System 테마(지원 문서 스니펫) | 모노 활자를 **본문에만**, UI는 시스템 산세리프. “모노 전면”이 아니라는 점이 Carriage와의 차이 |
| B2 | **Bear** (Shiny Frog) | S38 webdesignhot design.md·refero·community.bear.app `2차` `[스니펫]` | 액센트 `#DD4C4F`(호버 `#C43A3D`), 테마 태그 빨강 `#D14C3E` — **역설계 값, 공식 아님** | Bear Sans / Bear Sans Headline(독점) | 헤드라인 51px(2차). 그 외 미확인 | **단일 포인트**: 링크·마스코트·섹션 제목에만 | 테마별 라이트/다크 | 빨강 1색 포인트 운용의 가장 가까운 상용 예 |
| B3 | **Ulysses** | S39 TidBITS·help.ulysses.app·Macworld `공식/2차` `[스니펫]` | 미확인 | 사용자 지정 1서체 | 행간·단락 간격·들여쓰기·줄 폭을 설정으로 노출 | 없음 | **“Dark mode”(크롬만 어둡게) vs “dark theme”(시트도 어둡게)를 분리** | 우리 다크 설계의 직접 근거: 크롬 다크 + 종이 라이트 조합을 1급 옵션으로 |
| B4 | **Olivetti 그래픽 유산 — Giovanni Pintori** | S31·S32 Wikipedia·Letterform Archive·peoplesgdarchive `1차/2차` `[스니펫]` | 공식 브랜드 hex 없음(S32). Lettera 22 포스터: 파랑·노랑·빨강·초록 아치(2차) | 기하 도형 + 미니멀 | — | 빨강은 여러 색 중 하나. “빨강 지배” 포스터는 Valentine 캠페인(1969) | — | B안의 정신: **기계 사진 대신 기하·타이포**. 1950 아트디렉터 취임 |
| B5 | **Freewrite 사이트/제품** | S37 | 미확인 | — | — | 특별판 “Valentine” 라벨 존재(모델 미확인) | — | “타자기”라는 말만 남기고 기계 그림을 지운 포지셔닝 |

**방향 B 권고**: 빨강은 (1) 마진 스톱 마커 2개, (2) 선택/활성 상태 1개, (3) 빨강 잉크 토글 — **화면당 빨강 면적 1% 미만**(추정 목표). 모노 활자는 **숫자·페이지 번호·눈금 라벨·툴팁 단축키**에만, 한국어 UI 텍스트는 산세리프.

---

## 5. 모노 서체 후보표

“타자기 느낌 강도”와 “UI 가독성”은 ★ 5점 **추정**. 라이선스는 리포에서 `[직접]` 확인한 것만 `공식`.

| 서체 | 라이선스(출처) | 한글 | 타자기 느낌 | UI 가독성 | 메모 |
|---|---|---|---|---|---|
| **Courier Prime** | **OFL 1.1** — google/fonts `ofl/courierprime/OFL.txt`(raw) + quoteunquoteapps/CourierPrime README `[직접]` `공식` | 없음 | ★★★★ | ★★★ | IBM Courier(1956, Howard Kettler)의 현대화. 세리프 선명, 카운터 넓힘, 볼드 진함, 커시브 이탤릭(google/fonts 설명 `[직접]`). Sans/Code 변형 존재. 스크린플레이용 메트릭 |
| **IBM Plex Mono** | **OFL 1.1**, RFN “Plex” — google/fonts `ofl/ibmplexmono/OFL.txt`(raw) + IBM/plex README `[직접]` `공식` | 없음(단, **Plex Sans KR** 같은 가족에 있음) | ★★★ | ★★★★ | 이탤릭이 Selectric Italic 12에서 유래, Mono 자체가 Selectric 활자들에 대한 헌정(Cooper Hewitt·typographica, S17 `2차` 스니펫). **UI 가족 통일**이 가능한 유일한 후보 |
| **Special Elite** | **Apache 2.0** — google/fonts `apache/specialelite/LICENSE.txt` + METADATA.pb(raw) `[직접]` `공식` (designer Astigmatic, subsets latin) | 없음 | ★★★★★ | ★ | Smith Corona Special Elite·Remington Noiseless 모사 + 잉크 번짐 그런지. **디스플레이 전용** — 로고/스플래시 외 금지 |
| **iA Writer Mono / Duo / Quattro** | **OFL 1.1**, RFN “iA Writer”·“Plex” — iaolo/iA-Fonts Mono/LICENSE.md `[직접]` `공식`. 단 README에 **다른 텍스트 에디터 번들·기본 서체 사용 금지, iA Writer 모방 제품 금지, 외부 사용 시 크레딧 요청** `[직접]` | 없음 | ★★★ | ★★★★ | 라이선스상 가능해도 README 가이드라인이 “에디터 번들”을 막음 → **Platen에 번들 부적합**. 같은 뿌리인 IBM Plex Mono를 쓰라고 README가 직접 권함 |
| **JetBrains Mono** | **OFL 1.1**(소스 Apache 2.0) — google/fonts `ofl/jetbrainsmono/OFL.txt`(raw) + JetBrains/JetBrainsMono README `[직접]` `공식` | 없음(README에 한국어 언급 없음) | ★ | ★★★★★ | 큰 x-height, 8웨이트+이탤릭. 코드 서체 인상이 강해 타자기 맛은 없음 |
| **Fira Code** | **OFL 1.1** — google/fonts `ofl/firacode/OFL.txt`(raw) + tonsky/FiraCode README `[직접]` `공식` | 없음 | ★ | ★★★★ | Fira Mono + 리거처. 합자가 오히려 방해 |
| **Space Mono** | **OFL 1.1** — google/fonts `ofl/spacemono/OFL.txt`(raw) + googlefonts/spacemono README `[직접]` `공식` (Colophon, Latin Extended) | 없음 | ★★ | ★★ | 기하 슬랩, 디스플레이 지향 |
| **ITC American Typewriter** | **상용**(Monotype ITC 상표; MyFonts·Fonts In Use, S15 `2차` 스니펫). 1974 Joel Kaden·Tony Stan | 없음 | ★★★★ | ★★★ | 비례폭 “타자기풍”. 상용이라 사이드로드 앱에도 데스크톱/앱 임베딩 라이선스 필요 → **제외 권고** |
| **D2Coding** | **OFL 1.1**, RFN “D2Coding” — naver/d2codingfont `OFL.txt`(raw) + README `[직접]` `공식` | **완전**(11,172자, 한글 = 라틴 2배폭) | ★ | ★★★★ | 나눔바른고딕 기반(FONTRIX, 2015). 모노 한글 대체 1순위. 라틴은 고딕체라 타자기 맛 없음 → 라틴은 Courier Prime/Plex Mono, 한글만 D2Coding으로 폴백 |
| **Nanum Gothic Coding** | **OFL 1.1**, RFN “Nanum…” 계열 — google/fonts `ofl/nanumgothiccoding/OFL.txt` + METADATA.pb(raw) `[직접]` `공식` (NHN 2010, Sandoll, subsets korean) | 완전 | ★ | ★★★ | D2Coding의 대안 |

### UI 본문용 산세리프 + 한글 후보

| 서체 | 라이선스 | 한글 | 메모 |
|---|---|---|---|
| **IBM Plex Sans KR** | **OFL 1.1**, RFN “Plex” — google/fonts `ofl/ibmplexsanskr/OFL.txt`(raw) + IBM/plex README(`@ibm/plex-sans-kr`) `[직접]` `공식` | 완전 | Plex Mono와 한 가족. **B안 1순위** |
| **Pretendard** | **OFL 1.1**, RFN “Pretendard”(+Adobe ‘Source’ RFN 포함) — orioncactus/pretendard `LICENSE`(raw) + README `[직접]` `공식`. 단독 판매만 금지 | 완전 | Inter(라틴) + 본고딕(한글) + M PLUS(가나). 9웨이트 + Variable. **범용 1순위**, Courier Prime과 조합 시 |
| **Noto Sans KR** | **OFL 1.1**, RFN ‘Source’(Adobe) — google/fonts `ofl/notosanskr/OFL.txt`(raw) `[직접]` `공식`. (notofonts/noto-cjk 리포의 LICENSE 경로는 404였음) | 완전, 7웨이트(noto-cjk Sans README raw `[직접]`) | 파일이 크다(서브셋 필요) |
| **Nanum Gothic** | **OFL 1.1**, RFN “Nanum, NanumGothic…” — google/fonts `ofl/nanumgothic/OFL.txt` + METADATA.pb(raw) `[직접]` `공식` | 완전 | 산돌클라우드 안내: 번들·재배포·수정 허용, 폰트 파일 단독 유료 판매 금지(S14 `2차` 스니펫) |

**추천 조합**
1. **B안**: IBM Plex Mono(숫자·눈금·단축키) + IBM Plex Sans KR(UI 전반) — 모두 OFL. 모노 한글이 꼭 필요한 자리(예: 페이지 번호 “3 / 12쪽”)는 D2Coding 폴백.
2. **A안**: Courier Prime(라벨·제목) + Pretendard(UI) + D2Coding(모노 한글 폴백) — 모두 OFL. 로고/스플래시 한정 Special Elite(Apache 2.0).
3. **금지**: 모노 서체로 한국어 UI 전체를 조판하는 것(Carriage 교훈 + 한글 모노는 고딕체라 타자기 맛도 없음).

---

## 6. 손글씨 앱 크롬 레퍼런스 수치

**핵심 한계**: 굿노트·삼성노트·스타노트의 툴바 높이·아이콘 px은 공개 문서에 없고, 이 환경에서 스크린샷을 열 수 없어 **실측 불가 → `docs/LOCAL_TODO.md` 숙제로 이관 권고**. 아래는 확인된 정성 정보와 플랫폼 공식 수치.

| 앱 | 확인된 사실 | 출처·등급 | 수치 |
|---|---|---|---|
| **GoodNotes** | 6.3에서 툴바 아이콘 숨김·순서 변경 가능. 7.0 재설계 후 “cluttered”, 플로팅 요소 토글·이전 레이아웃 복귀 요청 다수. 사이드바 페이지 뷰가 2×2로 축소됐다는 불만. 가로 모드에서 툴바를 옆으로 보내 세로 공간 확보 제안 | S40 feedback.goodnotes `2차(사용자 포럼)` `[스니펫]` | 미확인(추정: 툴바 44~52pt) |
| **GoodNotes 다크** | UI만 어두워지고 종이는 흰색 유지. “True dark mode”(종이+잉크 자동 반전) 수년째 요청. iOS 색 반전은 빨강→청록으로 뒤집혀 부적합하다는 사용자 보고 | S41 `2차` `[스니펫]` | — |
| **삼성노트** | 페이지 스타일·템플릿 설정에 “다크 모드에서 색 조정” 옵션. 흰 페이지 + 다크 모드에서 툴바만 검정으로 남는 불일치 보고. 기본 종이 색·템플릿은 지정 가능하나 기본 펜은 불가 | S42 community.samsung `2차` `[스니펫]` | 미확인 |
| **스타노트(StarNote)** | 만년필·볼펜·형광펜, 색·굵기 조절, PDF 주석. **UI 수치·툴바 리뷰 없음** | S43 App Store id6751570915·hunted.space `공식(스토어)/2차` `[스니펫]` | 자료 부족 |

**플랫폼 공식 터치 타깃**

| 플랫폼 | 수치 | 출처 |
|---|---|---|
| **Material(Android)** | 최소 **48×48 dp**(약 9 mm), 타깃 간 **≥ 8 dp**. 아이콘은 24 dp로 보여도 패딩 포함 48 dp. Compose `minimumInteractiveComponentSize` 기본 48 dp | S44 support.google.com 7101858·kotlinlang Compose API `공식` `[스니펫]` |
| **Windows / Fluent** | 표준 **7.5 mm = 40×40 epx**(135 PPI, 1.0x). Compact 32×32 epx. 터치 우선 UI 권장 **44×44 epx + 최소 4 epx 간격**. 폭 ≥120 epx면 높이 32 epx 허용 | S45 learn.microsoft.com guidelines-for-targeting·touch-interactions `공식` `[스니펫]` |

**Platen 크롬 초안(추정, 위 공식 수치에서 도출)**: 툴바 높이 **56 dp**(Android) / **48 epx**(Windows), 아이콘 24 dp, 터치 영역 48 dp, 아이콘 간격 8 dp, 페이지 주변 여백 16~24 dp(태블릿 가로에서 좌우 캐리지 레일 영역 포함 32 dp).

---

## 7. 다크 모드 메모

### 7.1 원칙: “반전”이 아니라 “의미색 재매핑”
- 픽셀 반전(iOS 색 반전식)은 빨강을 청록으로 뒤집는다(S41 사용자 보고). → 스트로크에 **의미색 토큰**(`ink.default`, `ink.red`, `hl.yellow`…)을 저장하고, 테마별 실제값으로 렌더.
- `ink.default`: 라이트 `#1A1A1A` → 다크 `#E8E4DC`(크림을 어둡게 한 종이 위 **온화한 밝은 회색**, 순백 금지).
- Ulysses처럼 **크롬 다크 + 종이 라이트**를 별도 옵션으로(눈부심 민감 사용자는 전체 다크).

### 7.2 형광펜 가시성 — 혼합 모드 원리(로컬 계산)
- 라이트 종이: 형광펜은 **Multiply**가 자연스럽다. `#FFE44D × #F6F1E7 = #F6D746` — 잉크 `#1A1A1A` 위에 덮어도 잉크가 검게 유지(`#191918`).
- 다크 종이에서 Multiply는 실패: `#FFE44D × #1C1B19 = #1C1808` → **보이지 않음**(PixPin 문서도 동일 현상 기술, S46).
- 다크에서는 **Screen(또는 Lighten)**: `#FFE44D ⊕ #1C1B19 = #FFE75E` → 선명. 단 밝은 잉크 `#E8E4DC` 위에 Screen을 덮으면 잉크가 씻겨나가므로 **형광펜 레이어를 잉크 아래에** 두거나(GoodNotes 사용자 요청과 동일, S46), 다크에서는 알파 35~45%의 Normal로 깔고 레이어 순서를 잉크 아래로.
- 구현 메모: 테마별 `hl.blend = multiply | screen`, `hl.alpha`, `hl.zOrder = belowInk` 토큰 3개면 된다.

### 7.3 2색 리본 빨강의 대비(WCAG 2.x 상대휘도 공식, 로컬 스크립트 계산 — `계산`)

| 전경 | 크림 `#F6F1E7` | 흰 `#FAFAF7` | 다크 종이 `#1C1B19` | 다크 크롬 `#121212` |
|---|---|---|---|---|
| 잉크 `#1A1A1A` | **15.46** | 16.64 | 1.01 | 1.08 |
| 잉크 반전 `#E8E4DC` | 1.13 | 1.21 | **13.57** | 14.77 |
| 빨강 A `#B3261E` | **5.81** | 6.25 | 2.63 | 2.87 |
| 빨강 B `#C8102E` | **5.23** | 5.63 | 2.93 | 3.18 |
| 빨강 C `#D7322C` | 4.26 | 4.58 | 3.59 | 3.91 |
| 빨강 D `#E5484D` | 3.48 | 3.74 | 4.40 | 4.79 |
| 빨강 E `#F26B5E` | 2.65 | 2.85 | **5.77** | 6.28 |
| 빨강 F `#FF6B6B` | 2.47 | 2.65 | 6.20 | 6.75 |

**결론**: 라이트는 `#B3261E`~`#C8102E`(AA 텍스트 4.5:1 통과), 다크는 `#F26B5E`(5.77:1) — **빨강도 테마별 2값 토큰**. 마커·아이콘(비텍스트 3:1 기준)은 다크에서 `#E5484D`까지 허용.

---

## 8. 결론 — DESIGN.md 재료

### 8.1 가져올 원리 5개
1. **장식은 크롬, 종이는 비운다.** iOS 6 Notes가 질감을 필기면에 깔아 iOS 7에서 제거당했다. 깊이는 아이콘·주요 액션·프레임에만(네오-스큐어모피즘 원칙).
2. **베일 = 툴바 = 눈금자.** 실물 페이퍼 베일이 여백 눈금을 겸하므로 툴바 하단 눈금 위에 마진 스톱 마커 2개 + 라인 스페이스 3단이 얹히는 “속지 컨트롤”이 자연스럽다.
3. **2색 리본 = 색 시스템.** 검정 잉크 기본 + 빨강 단일 포인트(화면당 1% 미만) + 펜의 검정/빨강 토글. 빨강은 라이트/다크 2값.
4. **크롬 다크와 종이 다크를 분리**(Ulysses). 잉크·형광펜은 픽셀 반전이 아니라 의미색 재매핑 + 혼합 모드 전환.
5. **재질은 실물에서만.** 키 톱·플래튼·금속은 촬영/구매한 이미지 에셋(다크용 별도 세트), 코드 재질 금지.

### 8.2 버릴 것
- 모노 서체로 UI 전체 조판(Carriage 교훈 1) — 모노는 숫자·눈금·단축키에만.
- 캐리지 리턴·레버 모션, 타자 소리, 마진 벨(Carriage 교훈 2 + BRIEF).
- CSS/셰이더로 흉내 낸 플래튼·금속·유리(“못생김” 판정; Liquid Glass 방식도 우리에겐 탈락).
- 타입바·기계 내부 그림, 플래튼 노브, 페이퍼 가이드, 리본 바이브레이터.
- Special Elite류 그런지 서체의 UI 사용, American Typewriter(상용), iA Writer 서체 번들(README 가이드라인 위배).

### 8.3 시안 토큰 초안

**방향 A — 실사 스큐어모픽** (재질은 전부 이미지 에셋 전제)

| 토큰 | 라이트 | 다크 | 비고 |
|---|---|---|---|
| `paper` | `#F6F1E7` | `#232220` | 종이는 깨끗(질감 이미지 없음) |
| `ink.default` | `#1A1A1A` | `#E8E4DC` | 의미색 |
| `ink.red` | `#B3261E` | `#F26B5E` | 5.81 / 5.77 |
| `chrome.metal` | `#8C8D86`(SM3 회색 크링클, 에셋 평균색) | `#4A4B4A`(SM9 하판) | 에셋 뒤 폴백색 |
| `chrome.platen` | `#2B2825` | `#1A1817` | 고무 띠 에셋 폴백 |
| `chrome.highlight` | `#C9C2B4`(크롬 링) | `#6E6A63` | 에셋 전용 |
| `hl.yellow` | `#FFE44D` multiply | `#FFE44D` screen, belowInk | §7.2 |
| 서체 | Courier Prime(라벨·제목) / Pretendard(UI) / D2Coding(모노 한글) | 동일 | 전부 OFL |
| 간격 | 4·8·12·16·24·32·48 dp | 동일 | 툴바 56 dp, 아이콘 24, 타깃 48 |
| 라운드 | 키 톱 버튼 = 원(full), 카드 6 dp, 페이지 0 dp | 동일 | 에셋 모서리와 일치시킬 것 |

**방향 B — 모던 미니멀 + 타자기 암시**

| 토큰 | 라이트 | 다크 | 비고 |
|---|---|---|---|
| `paper` | `#F6F1E7` | `#1C1B19` | |
| `chrome` | `#ECE7DC` | `#121212` | 크롬 다크 + 종이 라이트 조합 허용 |
| `ink.default` | `#1A1A1A` | `#E8E4DC` | 15.46 / 13.57 |
| `ink.red` | `#C8102E` | `#F26B5E` | 5.23 / 5.77. 마진 마커·활성 상태·빨강 잉크에만 |
| `muted` | `#8A857B` | `#8E8A82` | 눈금·비활성 아이콘 |
| `rule` | `#1A1A1A` @20% | `#E8E4DC` @20% | 줄 가이드·눈금 틱 |
| `hl.yellow` | `#FFE44D` multiply | `#FFE44D` screen, belowInk | |
| 서체 | IBM Plex Mono(숫자·눈금·단축키) + IBM Plex Sans KR(UI) + D2Coding(모노 한글 폴백) | 동일 | 전부 OFL, 한 가족 |
| 간격 | 4 기반: 4·8·16·24·32·48 dp | 동일 | 툴바 48 dp(Android 56 허용), 아이콘 24, 타깃 48 / Win 40 epx |
| 라운드 | 4 dp(버튼) · 8 dp(패널) · 페이지 0 dp | 동일 | 필 형태 금지(타자기 정서와 멀다, 추정) |

### 8.4 자료 부족 → LOCAL_TODO 후보
- 굿노트·삼성노트·스타노트 **툴바 높이·아이콘 px·페이지 여백·종이 hex** 실측(실기기 스크린샷 샘플링).
- iA Writer 라이트/다크 배경·커서 hex, 행간(앱 설치 후 샘플링).
- Olivetti Lettera 22 공식 색 목록, Selectric 8색 조합 전체, Underwood 마감(아카이브 직접 열람).
- Material 3 `m3.material.io` 원문과 iOS HIG 44 pt 원문 재확인(이번 환경 차단).

---

## 9. 출처 목록 (열람일 2026-10-24)

폰트 — `[직접]` 열람 (라이선스 원문은 raw 파일로 확인)
- S1 https://raw.githubusercontent.com/google/fonts/main/ofl/courierprime/OFL.txt , https://github.com/quoteunquoteapps/CourierPrime — 공식
- S2 https://raw.githubusercontent.com/google/fonts/main/ofl/courierprime/DESCRIPTION.en_us.html — 공식
- S3 https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexmono/OFL.txt , https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexsanskr/OFL.txt , https://raw.githubusercontent.com/IBM/plex/master/README.md — 공식
- S4 https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexmono/DESCRIPTION.en_us.html — 공식
- S5 https://raw.githubusercontent.com/google/fonts/main/apache/specialelite/LICENSE.txt , https://raw.githubusercontent.com/google/fonts/main/apache/specialelite/METADATA.pb , https://raw.githubusercontent.com/google/fonts/main/apache/specialelite/DESCRIPTION.en_us.html — 공식
- S6 https://raw.githubusercontent.com/iaolo/iA-Fonts/master/iA%20Writer%20Mono/LICENSE.md , https://raw.githubusercontent.com/iaolo/iA-Fonts/master/README.md — 공식
- S7 https://raw.githubusercontent.com/google/fonts/main/ofl/jetbrainsmono/OFL.txt , https://raw.githubusercontent.com/JetBrains/JetBrainsMono/master/README.md — 공식
- S8 https://raw.githubusercontent.com/google/fonts/main/ofl/firacode/OFL.txt , https://github.com/tonsky/FiraCode — 공식
- S9 https://raw.githubusercontent.com/google/fonts/main/ofl/spacemono/OFL.txt , https://github.com/googlefonts/spacemono — 공식
- S10 https://raw.githubusercontent.com/orioncactus/pretendard/main/LICENSE , https://github.com/orioncactus/pretendard — 공식
- S11 https://raw.githubusercontent.com/naver/d2codingfont/master/OFL.txt , https://github.com/naver/d2codingfont — 공식
- S12 https://raw.githubusercontent.com/google/fonts/main/ofl/notosanskr/OFL.txt , https://raw.githubusercontent.com/notofonts/noto-cjk/main/Sans/README.md — 공식 (noto-cjk `Sans/LICENSE.txt`·루트 `LICENSE`는 404)
- S13 https://raw.githubusercontent.com/google/fonts/main/ofl/nanumgothic/OFL.txt , https://raw.githubusercontent.com/google/fonts/main/ofl/nanumgothiccoding/OFL.txt , 같은 폴더의 METADATA.pb — 공식

폰트 — `[스니펫]`
- S14 나눔글꼴 라이선스: https://www.sandollcloud.com/font/17678/NanumSquareNeo , https://www.sandollcloud.com/free-font/204/NanumGothicOTF — 2차
- S15 American Typewriter: https://www.myfonts.com/fonts/itc/american-typewriter , https://www.fontsinuse.com/typefaces/10/itc-american-typewriter , https://en.wikipedia.org/wiki/American_Typewriter — 2차
- S16 Courier Prime 배경: https://johnaugust.com/?p=10306 , https://nofilmschool.com/2013/02/screenplay-standard-font-here-courier-prime , https://en.wikipedia.org/wiki/Courier_(typeface) — 2차
- S17 IBM Plex Mono와 Selectric: https://collection.cooperhewitt.org/objects/2318809961 , https://typographica.org/typeface-reviews/ibm-plex/ , https://en.wikipedia.org/wiki/IBM_Plex , https://eyeondesign.aiga.org/ibms-custom-corporate-typeface-is-now-available-for-anyone-to-use/ — 1차/2차
- S18 iA Writer 서체: https://ia.net/topics/a-typographic-christmas , https://ia.net/topics/in-search-of-the-perfect-writing-font , https://blakewatson.com/journal/almost-monospaced-the-perfect-fonts-for-writing/ , https://www.macstories.net/ios/ia-writer-5-2-better-typography-and-external-library-locations/ — 2차

타자기 — `[스니펫]`
- S19 부품: https://cdn.shopify.com/s/files/1/2384/6833/files/Smith-Corona_Zephyr_Deluxe_Typewriter_instructions_Manual_book.pdf?93 , https://cdn.shopify.com/s/files/1/2384/6833/files/Smith-Corona_Portable_Typewriter_Operating_instruction_Manual.pdf , https://mrmrsvintagetypewriters.com/pages/typewriter-faq-the-ultimate-100-questions-answered , https://wiki.pathfindersonline.org/w/AY_Honors/Typewriting/Answer_Key , https://typewriterdatabase.com/1945-AMES-OAMI-MechanicalTrainingMan.index.manual — 1차(매뉴얼)/2차
- S20 2색 리본: https://patents.justia.com/patent/4037708 , https://throughouthistory.com/?p=2271 — 1차(특허)/2차
- S21 키 톱: https://www.typewriterdatabase.com/1938-smith-corona-standard.4669.typewriter , https://typewriterdatabase.com/1935-smith-corona-silent.18805.typewriter , https://www.1stdibs.com/creators/royal-typewriter-company-1/furniture , https://deskthority.net/viewtopic.php?p=441517 — 2차
- S22 Valentine: https://www.moma.org/collection/works/4576 , https://collection.nationalmuseum.se/en/collection/item/6657 , https://domusweb.it/en/from-the-archive/2011/08/25/red-valentine.html , https://www.metmuseum.org/art/collection/search/739409 — 1차
- S23 Valentine 색상: https://ourstory.moretonbay.qld.gov.au/nodes/view/52924 , https://massmadesoul.com/features/valentine , https://www.quittenbaum.de/en/auctions/design/182B/perry-a-kingolivetti-ivrea-rare-valentine-typewriter-1969-design-145091/ , https://www.antikeychop.com/olivetti-valentine — 1차/2차
- S24 Lettera 22: https://www.archiviostoricolivetti.it/en/lettera-22/ , https://www.centrepompidou.fr/en/ressources/oeuvre/cdqnBop , https://owl.museum-digital.de/pdf/object/3808.pdf?lang=en — 1차
- S25 Lettera 32: https://en.wikipedia.org/wiki/Olivetti_typewriters , https://owl.museum-digital.de/pdf/object/5407.pdf?lang=en , https://www.catawiki.com/en/l/104856040-marcello-nizzoli-olivetti-lettera-32-typewriter-1960-1970 — 1차/2차
- S26 Hermes 3000: https://en.wikipedia.org/wiki/Hermes_3000 , https://davidhayes.ca/?p=465 — 2차
- S27 Olympia SM3: https://www.typewriterdatabase.com/1958-olympia-sm3.5927.typewriter , https://typewriterdatabase.com/1955-olympia-sm3.27054.typewriter , https://www.typewriters101.com/store/p582/1956_Olympia_SM3.html , https://boffosocko.com/2024/09/02/acquisition-1958-olympia-sm3-portable-typewriter/ — 1차(개체)
- S28 Olympia SM9: https://boffosocko.com/2025/03/03/acquisition-1964-olympia-sm9-portable-typewriter/ , https://westfalen.museum-digital.de/pdf/object/34303.pdf?lang=en , https://www.typewriterdatabase.com/1964-olympia-sm9.19127.typewriter , https://typewriterdatabase.com/1969-olympia-sm9.20396.typewriter — 1차(개체)
- S29 Smith-Corona Silent Super: https://www.typewriterdatabase.com/1957-smith-corona-silent-super.16228.typewriter , https://www.typewriterdatabase.com/1958-smith-corona-silent-super.24790.typewriter , https://www.typewriterdatabase.com/1959-smith-corona-silent-super.16492.typewriter — 1차(개체)
- S30 IBM Selectric: https://www.ibm.com/history/selectric , https://thehenryford.org/collections-and-research/digital-collections/artifact/562990 — 공식/1차
- S31 Pintori: https://en.wikipedia.org/wiki/Giovanni_Pintori , https://peoplesgdarchive.org/item/18727/olivetti-lettera-22-advertisement-posterandnbsp , https://antikbar.co.uk/products/olivetti-lettera-22-typewriter-giovanni-pintori-pa3364 — 1차/2차
- S32 Olivetti 빨강: https://www.finestresullarte.info/en/works-and-artists/a-splash-of-color-on-everyday-color-olivetti-s-valentine-typewriter , https://letterformarchive.org/news/giovanni-pintori-for-olivetti , https://madegooddesigns.com/?p=93498 — 2차

앱·UI — `[스니펫]`
- S33 Hanx Writer: https://bgr.com/2014/08/19/hanx-writer-ipad-app/ , https://www.caymancompass.com/2014/08/29/typewriter-nostalgia-thanks-to-the-hanx-writer-app/ , https://www.cityam.com/tom-hanks-created-typewriter-app-and-it-s-now-no1-apple-s-app-store/ — 2차
- S34 iOS 6 Notes: https://www.macworld.com/article/221952/get-to-know-ios-7-notes-and-reminders.html , https://en.wikipedia.org/wiki/Notes_(Apple) , https://www.imore.com/how-change-default-font-notes-app-iphone-and-ipad — 2차
- S35 네오-스큐어모피즘: https://www.techradar.com/phones/iphone/apples-new-liquid-glass-design-puts-the-spotlight-on-skeuomorphism-for-the-first-time-since-ios-6-and-im-all-for-it , https://clay.global/blog/skeuomorphism-ui , https://uxmatters.com/mt/archives/2024/11/the-renaissance-of-skeuomorphic-design-in-modern-user-experiences-bridging-the-digital-and-the-physi.php , https://blog.simone.computer/is-skeumorphism-back — 2차
- S36 iA Writer 테마: https://ia.net/writer/support/basics/settings , https://apps.apple.com/app/id775737590 , https://www.obsidianstats.com/themes/obsidian-ia-writer — 공식(지원 문서)/2차
- S37 Freewrite: https://getfreewrite.com/products/freewrite-smart-typewriter-3rd-gen , https://getfreewrite.com/products/freewrite-smart-typewriter , https://goodereader.com/blog/?p=330490 , https://freddiedeboer.substack.com/p/review-freewrite-alpha — 공식(제품)/2차
- S38 Bear: https://www.webdesignhot.com/design.md/bear-app/ , https://styles.refero.design/style/4afe77ff-e7fa-41d8-96d6-ce8cdc159f97 , https://community.bear.app/t/bear-theme-file-parameter-documentation/13590 — 2차
- S39 Ulysses: https://tidbits.com/2016/08/13/writing-app-ulysses-blends-power-and-simplicity/ , https://help.ulysses.app/dive-into-editing/editor-customization-guide , https://help.ulysses.app/mac-customization-guide , https://www.macworld.com/article/3278249/ulysses-13-review-for-mac-and-ios.html — 공식(도움말)/2차
- S40 GoodNotes 툴바: https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/41494096-toolbar-redesign-customization , https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/50550846--sep-23-launch-new-toolbar-suggestions?page=3 , https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/50583833--toolbar-support-customization — 2차
- S41 GoodNotes 다크: https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/50784245-true-dark-mode-automatic-page-ink-color-inversi , https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/46042741-true-dark-mode , https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes/suggestions/38754304-automatic-color-inversion-for-dark-paper — 2차
- S42 삼성노트: https://us.community.samsung.com/t5/Samsung-Apps-and-Services/Samsung-Notes-Dark-Font-fix-App-Feature-Request/td-p/2358196 , https://eu.community.samsung.com/t5/suggestion/change-toolbar-colour-in-notes-based-on-page-colour/td-p/11300891 , https://r1.community.samsung.com/t5/tablets/inconsistent-dark-mode-in-taskbar/m-p/19378648/highlight/true — 2차
- S43 스타노트: https://apps.apple.com/us/app/starnote-handwriting-pdf/id6751570915 , https://hunted.space/product/starnote — 공식(스토어)/2차
- S44 Material 터치 타깃: https://support.google.com/accessibility/android/answer/7101858?hl=en , https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/minimum-interactive-component-size.html — 공식
- S45 Fluent 터치 타깃: https://learn.microsoft.com/en-my/windows/apps/develop/input/guidelines-for-targeting , https://learn.microsoft.com/et-ee/windows/apps/develop/input/touch-interactions — 공식
- S46 형광펜 혼합 모드: https://pixpin.com/docs/mark/mark-pencil , https://learn.microsoft.com/en-us/previous-versions/visualstudio/design-tools/expression-studio-2/cc294750(v=expression.10) , https://feedback.goodnotes.com/forums/191274-customer-suggestions-for-goodnotes-apple/suggestions/49554980--highlighter-automatically-set-highlighter-to-bac — 공식/2차
- S47 WCAG 대비·혼합 계산: 로컬 Python 스크립트(WCAG 2.x 상대휘도 공식, multiply/screen 공식) — 계산. 원문 w3.org는 이번에 열지 않음.

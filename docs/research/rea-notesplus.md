# REA — Notes Plus / Notes+ (iOS) · 연필 질감 · 펜 로스터

> 열람일: **2026-10-24** · 작성: Platen 리서처 · 상태: 공개 자료 기반 1차 조사 (실측 불가 — iOS 전용)
>
> **열람 방식 고지**: 이 세션의 네트워크 정책상 apps.apple.com · writeon.cool · beingpaperless.com · namu.wiki · lemon8 · youtube · reddit 등 **직접 페치가 전부 차단**되었고, 웹 검색 엔진이 돌려준 **페이지 스니펫(인용문 포함)** 만 열람할 수 있었다. 따라서 아래 "출처"는 모두 검색 결과에 실제로 노출된 URL이며, 원문 전문을 읽은 것이 아니다. 등급은 이를 반영해 보수적으로 매겼다 (공식 스토어 문구라도 스니펫 경유 = "공식 문서(스니펫)"). 세션 검색 한도(200회)도 소진되어 추가 탐색은 후속 세션 필요.
>
> 등급 기준: **공식 문서** (스토어·개발사·특허) / **1차 관찰** (스크린샷·영상 직접 확인) / **2차 글** (리뷰·위키·커뮤니티) / **추정** / **사용자 관찰** (BRIEF 메모, 미검증)

---

## 0. 가장 중요한 발견 — "Notes Plus"와 "Notes+"는 **서로 다른 두 앱**이다

| | **A. Notes Plus / Notes Plus X** | **B. Notes+ : Notes for Pencil** |
|---|---|---|
| 개발사 | Viet Tran (WriteOn / 구 Prime Circa Inc., 베트남) | SINGFISH PTE. LTD. (싱가포르) |
| App Store ID | Notes Plus `id374211477` (2010~) / Notes Plus X `id1501351725` (2020~) | `id1609361989` (2023-01-20 출시) |
| 가격 | 유료 US$12.99 (각각) · 번들 US$14.99 | **무료** + 인앱(AI 구독) |
| 펜 계열 | 만년필·볼펜·젖은 붓·캘리그래피 (벡터 잉크 특허) | 만년필·펜·**연필**·모노라인·형광펜 + 특수선(직선·물결·점선·이중·삼중) + 수채·텍스트펜·AI스냅펜·배니시·레이저·캡처 |
| 연필 도구 | **공개 자료에 없음** | 있음 (이름만 확인, 렌더 설명 없음) |

**판정**: BRIEF 2b의 "연필 = Notes+(iOS)" 및 사용자 메모(계열×변형 로스터, 특수선 점선·물결·이중·삼중, 수채, "Choose Default Pens" 시트)는 **B. Singfish Notes+** 와 일치한다. Viet Tran의 Notes Plus X에는 연필·특수선·수채 도구의 공개 기록이 없다. 이하 본문은 **B를 주 대상**으로 하고, A는 비교·보조로만 다룬다. → DECISIONS.md에 "REA 대상 Notes+ = Singfish `id1609361989`" 로 명시 권고.

- 출처: App Store Notes Plus X <https://apps.apple.com/us/app/notes-plus-x/id1501351725> · App Store Notes+ <https://apps.apple.com/us/app/notes-notes-for-pencil/id1609361989> · 나무위키 Notes+ <https://namu.wiki/w/Notes+> · 열람 2026-10-24 · 등급: 공식 문서(스니펫) + 2차 글

---

## 1. 앱 정체

### 1-A. Notes+ (Singfish) — 주 대상
- **관찰**: iPad 전용 설계(iPhone도 설치 가능), 무료, 인앱 구독 "AI One"(월/년)·"Summary Generator"(월). 요구 OS iOS 17.0+. 용량 약 160~169 MB(스냅샷별 상이). 평점 4.7★ / 약 3.1~3.2K(미국). 나무위키 기준 다운로드 800만+. 연령 16+.
- **버전**: 6.5.8 (10월 15일) · 6.5.7 (9월 29일) · 6.5.6 (9월 22일) · 6.3.8에 "iOS 27 베타 크래시 수정" 포함. 연도는 자료 간 불일치(앱스토어 스니펫은 월·일만, 미러 사이트는 2026) — iOS 27 베타 대응이 6.3.8에 있으므로 **2026년**으로 판단.
- **플랫폼**: iPadOS/iOS만 확인. Android·Windows·Mac 버전은 **자료 부족**(검색 한도 소진으로 미확인).
- 출처: <https://apps.apple.com/us/app/notes-notes-for-pencil/id1609361989> (공식 문서·스니펫) · <https://www.appbrain.com/appstore/notes-notes-for-pencil/ios-1609361989> (2차) · <https://apppricinglab.com/app/apple/1609361989> (2차) · <https://namu.wiki/w/Notes+> (2차) · <https://apps.apple.com/us/developer/singfish-pte-ltd/id1745986454> (공식) · 열람 2026-10-24

### 1-B. Notes Plus / Notes Plus X (Viet Tran) — 보조
- **관찰**: Notes Plus(원조, 2010) US$12.99, v6.6.4(2023-11-09, 3rd-party 기준), 평점 약 3.55/1.6K. Notes Plus X는 "문서 기반 앱으로 완전 재작성(document-based rewrite)", US$12.99, iOS 13+, v1.17(2024-02-22, 3rd-party), 평점 3.1★/56(한국 스토어). 두 앱 번들 US$14.99. 2025년 리뷰어 평: "업데이트가 드물고 산발적, 개발자가 더 이상 활발하지 않은 느낌".
- 특허: US 9,189,695 B2 "Vector texturing for free-form drawing" (출원 2014-06-02, 등록 2015-11-17, 발명자 Viet Tran·Xuyen Tran, 양수인 Prime Circa Inc.). Google Patents 상태 "Expired – Fee Related". 공개 요약: "터치 기반 자유 드로잉/필기에 **벡터 텍스처**를 적용, 왜곡 없이 크게 확대 가능". 청구항 본문은 미열람.
- 출처: <https://apps.apple.com/us/app/notes-plus-x/id1501351725> · <https://apps.apple.com/app/notes-plus/id374211477> · <https://notes-plus-x.updatestar.com/> · <https://worldsapps.com/download-notes-plus> · <https://beingpaperless.com/these-handwriting-apps-are-worth-buying-for-2025/> · <https://patents.google.com/patent/US9189695B2/en> · <https://patents.justia.com/patent/20140355884> · 열람 2026-10-24 · 등급: 공식 문서(스니펫)/2차 글

---

## 2. 연필 질감 (최우선) — **결론: 공개 자료 부족**

### 2-1. 확인된 사실
| 관찰 | 수치/문구 | 출처 · 등급 |
|---|---|---|
| Notes+ 에 "연필" 계열이 존재 | 나무위키 펜 목록: 만년필, 펜, **연필**, 모노라인 펜, 형광펜 … | <https://namu.wiki/w/Notes+> · 2차 글 |
| 잉크 엔진은 벡터 기반 | "벡터 기반 잉크, 펜 두께 최대 30pt" (r4판: 0.5~30pt) | 나무위키 (위) · 2차 글 |
| 6.5.6에 **크레용** 추가 + 안정화·자동 정제 | "Added support for adjusting writing stabilization / automatic handwriting refinement / **crayons**" | App Store 버전 노트 <https://apps.apple.com/us/app/notes-notes-for-pencil/id1609361989> · 공식 문서(스니펫) |
| 손글씨 안정화 단계 선택 | 개발사 홍보 "Handwriting stabilisation levels" | X @notesplus_app <https://x.com/notesplus_app?lang=en> · 공식(스니펫) |
| 마케팅 문구 | "realistic writing experience" — 연필 질감 구체 설명 없음 | App Store (위) · 공식(스니펫) |

### 2-2. 찾지 못한 것 (자료 부족)
- 연필 4종의 **이름·차이** — 공개 자료 없음. "연필 4종"은 사용자 관찰 등급 유지.
- **텍스처/그레인 방식**(비트맵 스탬프 vs 절차적 노이즈 vs 벡터 텍스처), **필압→농도** 매핑, **틸트 셰이딩** 유무 — 개발사·스토어·리뷰 어디에도 언급 없음. 검색에서 "pencil tilt texture" 는 Notability·Apple Notes 결과만 반환.
- 겹침 시 농도 누적(알파 누적 vs 곱연산) — 관찰 영상 미확보.
- Notes Plus X(Viet Tran)는 **연필 도구 자체가 자료에 없음**(계열: 만년필·볼펜·젖은 붓·캘리그래피). 2015년 사용자 가이드 v4.7도 Ballpoint/Fountain/Calligraphy/Wet Brush 4종만 기술.
  - 출처: <http://static.gowriteon.com/files/UserGuide_V4-7.pdf> · 공식 문서(스니펫)

### 2-3. 추정 (근거 명시)
- 나무위키가 "벡터 기반 잉크"라 적고 6.5.6에서 크레용이 **별도 도구**로 추가된 점으로 보아, Notes+의 연필·크레용은 벡터 획 위에 **질감 레이어를 씌우는 방식**(Viet Tran 특허의 "vector texturing"과 유사한 접근이거나, 획 알파 마스크 + 노이즈 텍스처)일 가능성이 있다. **추정** — 스크린샷 확대 확인 전까지 설계 근거로 쓰지 말 것.
- 사용자 평에 "필기 딜레이·잔상" 지적이 있는데(§6), 질감 렌더가 GPU 셰이더가 아닌 CPU 래스터/오버드로일 경우 나타나는 전형 증상 — **추정**.

---

## 3. 펜 로스터 구조

### 3-1. 메모 검증 결과
| 메모 항목 | 검증 | 출처 · 등급 |
|---|---|---|
| 계열: 만년필·펜·연필·모노라인·형광 | **확인** — 나무위키 최신판 동일 5계열 | <https://namu.wiki/w/Notes+> · 2차 글 |
| 변형 수 4·4·4·4·3 | **미확인** — 계열별 개수는 어느 자료에도 없음. 구판 "32종 펜&형광펜"과 합산 가능성만 있음(4+4+4+4+3=19 + 특수선 5 + 수채 1 + 텍스트·AI스냅·배니시·레이저·캡처 5 = 30 ≈ 32, 정합성은 있으나 증거 아님) | 사용자 관찰 유지 |
| 특수선: 점선·물결·이중·삼중 | **확인 + 확장** — 나무위키 "직선·물결·점선·이중·삼중선"; 리뷰어 "single line, wavy, dashed, double line, double wavy, even triple lines" (이중 물결 포함) | 나무위키 · <https://beingpaperless.com/notes-review-another-cool-free-handwriting-app-for-the-ipad/> · 2차 글 |
| 수채 | **확인** — "수채화 펜" | 나무위키 · 2차 글 |
| 추가 발견 | 텍스트 펜, AI 스냅 펜(드래그 영역에 AI 기능), 배니시 펜, 레이저 펜, 캡처(스크린샷) 펜, 리드 펜(reed pen, 리뷰어 언급), 크레용(6.5.6), 형광펜 PDF 텍스트 스냅(대충 그어도 직선 보정) | 나무위키 · App Store · beingpaperless · 2차/공식(스니펫) |

### 3-2. 조절 파라미터
- **관찰**: 나무위키 r13판 — 펜별 **점도(viscosity)·색상·투명도** 조절. 두께 범위 0.5~30pt. 공식 Lemon8 게시물(@notesplus_app)에 "형광펜 size 38, 58% transparency / 만년필 / 펜 / 이중선 펜" 조합 예시 → 크기·투명도가 수치 슬라이더임을 시사.
- **필압·틸트 토글 유무**: 자료 부족.
- **수채 동작**(번짐·겹침 농도): 자료 부족.
- 출처: <https://namu.wiki/w/Notes+?uuid=f7ada687-594a-43e7-ad5e-b31463906c82> (r13) · <https://www.lemon8-app.com/@notesplus_app/7657175642991084040?region=sg> · 열람 2026-10-24 · 등급: 2차 글 / 공식 SNS(스니펫)

### 3-3. 비교 — Notes Plus X(Viet Tran)의 파라미터 (참고)
- 2015 사용자 가이드: 펜 모드 Ballpoint·Fountain·Calligraphy·Wet Brush. **Viscosity**(일부 펜만, 0.05 단위, "높을수록 획 굵기 변화가 큼/잉크 흐름 저항 큼"), **Thickness** 0.5 단위, **Nib angle**(캘리그래피). 리뷰어: "두께 제어는 인상적이나 **필압·불투명도 옵션 없음**". 2018 Macdrifter: "viscosity를 바꾸면 잉크가 두껍고 끈적해지지만 제대로 느껴진 적 없음, 지연 있음".
- 출처: <http://static.gowriteon.com/files/UserGuide_V4-7.pdf> · <https://beingpaperless.com/notes-plus-x-2024-full-review-pt-1-the-basics/> · <https://www.macdrifter.com/2018/02/the-difference-of-good-ink-on-the-ipad-pro.html> · 열람 2026-10-24 · 공식 문서(스니펫)/2차 글

---

## 4. 펜 설정 UI 2층 구조

| 항목 | 관찰 | 출처 · 등급 |
|---|---|---|
| 1층: 툴바 = 즐겨찾기 펜꽂이 | "You can customise pen tools on the toolbar … **The main toolbar doubles as your favourites toolbar**" — 상단 고정 또는 플로팅 전환 가능 | <https://beingpaperless.com/notes-review-another-cool-free-handwriting-app-for-the-ipad/> · 2차 글 |
| 2층: "Choose Default Pens" 시트 | **공개 자료에서 문구 확인 실패**. 2025 로스터 평 "partially customisable toolbar — personalise your pens only" 가 '펜만 교체 가능한 슬롯' 구조를 간접 지지 | <https://beingpaperless.com/top-handwriting-apps-with-the-best-user-experience-2025/> · 2차 글 / 시트 명칭은 **사용자 관찰** |
| 툴바 슬롯 수 | 자료 부족 | — |
| 설정 동선 단계 수 | 자료 부족. 단, 단점으로 "필기 중 색·두께를 바꾸려면 **펜 옵션을 열어야 함**"이 지적됨 → 슬롯 탭 1 + 옵션 열기 1 + 조절 1 = 최소 3단계로 **추정** | 나무위키 <https://namu.wiki/w/Notes+> · 2차 글 / 추정 |
| 비교: Notes Plus X | 펜용 즐겨찾기 툴바 + **형광펜용 별도 즐겨찾기 툴바** 2개 | beingpaperless pt1 · 2차 글 |

---

## 5. 기타 기능 표 (Notes+ Singfish 기준, 괄호는 Notes Plus X)

| 기능 | Notes+ 관찰 | 출처 · 등급 |
|---|---|---|
| 올가미 | **자료 부족**(스토어 스니펫에 미노출) | — |
| 도형 | "퀵 셰이프 — 자동 도구로 정밀 도형" (NPX: shape detection / smart shape) | App Store KR <https://apps.apple.com/kr/app/%EB%85%B8%ED%8A%B8/id1609361989> · 공식(스니펫) |
| 텍스트 | "텍스트 펜" 존재 (NPX: 텍스트 도구 "불안정, 글자가 사라지고 페이지가 튐", 커스텀 폰트 미지원) | 나무위키 · <https://beingpaperless.com/what-can-add-in-notes-plus-x-full-review-pt-2/> · 2차 글 |
| 손글씨 인식 | 수식 필기 인식·노테이션 입력 호평(커뮤니티); AI Solver(한국 미지원); 일반 손글씨→텍스트 변환은 미확인 (NPX: 40+ 언어 변환, 아랍어 지원, "정확도 최상급") | 디시 <https://gall.dcinside.com/mgallery/board/view/?id=ipad1&no=749190> · 나무위키 · <https://beingpaperless.com/is-notes-plus-x-worth-it-2024-review-pt-3/> · 2차 글 |
| 템플릿 | 무료 템플릿 다수, 6.5.6/6.5.7에 신규 템플릿 추가, 속지 변경·추가 가능 | App Store · 공식(스니펫) |
| 가져오기 | PDF·PPT·Word·이미지·웹페이지→PDF, Windows/Mac 파일, Google Classroom/Scholar(6.5.7) | App Store · 나무위키 · 공식/2차 |
| 내보내기 | **자료 부족**(스니펫 미노출). 구 리뷰: "노트북 저장/복원 백업 옵션 없음" | beingpaperless Notes+ 리뷰 · 2차 글(구버전) |
| 다크 모드 | 6.5.7 "Dark Mode용 어두운 배경 옵션(설정에서 켬)" 추가 | App Store 버전 노트 · 공식(스니펫) |
| 레이어 | 최대 3 레이어(같은 문제 반복 풀이용) | App Store · 공식(스니펫) |
| 듀얼 페이지 | 같은 문서 분할 보기 | App Store · 공식(스니펫) |
| 녹음 | 필기 동기 녹음 | 나무위키 · 2차 |
| 기타 | 가리막 도구, 계산기·시계·브라우저 내장, 비밀번호 잠금, iCloud 동기화, 필기 안정화 단계, 자동 손글씨 정제 | App Store · 나무위키 · X 공식 계정 |
| 포맷·GitHub | GitHub 검색(저장소·코드): Notes+/Notes Plus 포맷 파서 **없음**. 유일 결과 `quodlibetor/NotesPlusPortability`(2012, iPad 브라우저 의존 우회 셸 스크립트, 포맷 무관) | <https://github.com/quodlibetor/NotesPlusPortability> · 1차 관찰(검색 결과) |

---

## 6. 사용자 평가

### Notes+ (Singfish)
**장점**
1. 무료인데 도구·템플릿이 풍부, 굿노트에서 갈아탄 사용자 다수 — App Store KR/US 리뷰 · 2차
2. 페이지 확장(캔버스 늘리기)·수식 인식·녹음 편의성 — 미코 <https://meeco.kr/mini/39325820> · 디시(위) · 2차
3. 필기감 자체는 좋다는 전언 ("써보라고 했는데 필기감은 좋대") · 2025 UX 로스터 2위 — 디시 · beingpaperless 2025 · 2차

**단점**
1. **잦은 튕김/크래시**, 형광펜 사용 시 크래시 — App Store KR("너무 튕겨요!")·US 리뷰 · 2차
2. **필기 딜레이·잔상**("필기 딜레이가 매우 치명적") — 디시 · 2차
3. 펜 커스터마이징 자유도 낮음, 문서 탭 부재, 색·두께 변경 시 펜 옵션 열어야 함, 형광펜이 글자 위를 덮어 흐리게 함, 형광펜 굵기 일정하게 긋기 요청 — 미코 · 나무위키 · App Store · 2차

### Notes Plus X (Viet Tran)
- 장점: 만년필 획 ("fine flowing fountain pen"), 손글씨 변환 정확도·언어 수, 안정성(최근 리뷰) — App Store 리뷰 · beingpaperless pt3 · 2차
- 단점: 텍스트 도구 불안정, 검색이 가져온 문서를 색인 안 함, 필기 중 글자가 지워지는 현상, 잉크가 "끈적(sticky)", 개발 비활성 — beingpaperless pt1~3 · Macworld <https://www.macworld.com/article/231219/the-best-note-taking-apps-for-the-ipad-and-apple-pencil.html> · 2차

---

## 7. Platen에 가져올 원리 (공개 자료 범위 내)

1. **로스터 = 계열 × 변형 프리셋 + 기능성 펜 분리** — 필기 계열(만년필·펜·연필·모노라인·형광)과 주석용 특수선(직선·물결·점선·이중·삼중)과 유틸 펜(레이저·배니시·캡처)을 **같은 슬롯 체계**에 넣되 카테고리로 묶는다. Platen의 레이저 포인터(BRIEF 3R)는 Notes+처럼 "펜의 한 종류"로 로스터에 두면 UI가 단순해진다. (근거: 나무위키 펜 목록 · 2차)
2. **툴바 = 즐겨찾기 펜꽂이 2층 구조** — 메인 툴바가 곧 즐겨찾기 슬롯, 슬롯 교체는 별도 시트. 고정/플로팅 전환 제공. Platen의 "마진 스톱" 은유와 결합 가능. (근거: beingpaperless · 2차; 시트명은 사용자 관찰)
3. **특수선은 획 지오메트리 변형**으로 구현 — 이중·삼중선은 중심선 오프셋 복제, 물결·점선은 아크길이 파라미터 함수. 벡터 획 엔진이면 저비용. Notes+의 "이중 물결"까지 조합 가능해야 함. (근거: 리뷰 열거 · 2차; 구현 방식은 추정)
4. **형광펜 텍스트 스냅** — PDF 텍스트 위에서 대충 그어도 직선 보정·텍스트 밀착. PDF 주석 1차 범위에 유용. 단 "글자 위를 덮어 흐림" 불만 → Platen은 형광펜을 **multiply 블렌드/텍스트 아래 레이어**로. (근거: 나무위키 · App Store 리뷰 · 2차)
5. **필기 안정화 단계 + 자동 정제를 사용자 노출 설정**으로 — BRIEF의 "필압 커브·스무딩·테이퍼 사용자 조절"과 정합. 6.5.6의 "stabilization 조절" 추가가 사용자 요구를 반영한 변화로 읽힘. (근거: App Store 버전 노트 · 공식 스니펫)

## 8. 버릴 것
- **연필 렌더 방식을 Notes+에서 베끼려는 계획** — 공개 설명이 전무하고 실측도 불가. 연필 질감은 스크린샷 측정(§9) 후 **자체 설계**(노이즈 마스크 + 필압→알파·폭, 틸트→폭·입자 밀도)로 가고, Notes+는 "목표 룩" 참조 이미지로만 쓴다.
- **Notes Plus X의 viscosity 파라미터** — 사용자에게 직관적이지 않다는 평이 반복(Macdrifter). Platen은 "필압 커브·테이퍼" 같은 결과 직결 명칭 사용.
- **AI 구독·내장 브라우저·계산기** 류 부가 기능 — Platen 범위 밖.
- **"32종" 같은 가짓수 경쟁** — 계열 5 × 변형 소수 + 외부 브러시 임포트가 Platen 차별점.

## 9. 공개 영상·스크린샷에서 확보할 목록 (후속 세션 — 검색 한도 복구 후)

| # | 장면 | 측정 목적 | 후보 소스 |
|---|---|---|---|
| S1 | 연필 4종을 같은 압력·속도로 나란히 그은 획 **확대(200%+)** | 입자 크기·밀도, 가장자리 거칠기, 4종 차이(경도 HB/2B식인지, 폭 차이인지) | Lemon8/X @notesplus_app 게시물, 유튜브 "Notes+ iPad" 리뷰(미발견 — 재검색 필요) |
| S2 | 연필 획 **겹침 부분** | 농도 누적 방식(알파 누적 vs 포화) | 동일 |
| S3 | 연필 획의 **가늘게→굵게** 필압 변화 구간 | 필압→폭/농도 커브 형태(선형/감마) | 동일 |
| S4 | 펜 기울인 셰이딩 장면(있다면) | 틸트 지원 여부·폭 확장 비율 | 동일 |
| S5 | 크레용 vs 연필 비교 | 텍스처 공유 여부(같은 엔진의 파라미터 차이인지) | 6.5.6 이후 영상 |
| S6 | 펜 옵션 시트 전체 화면 | 파라미터 목록(크기·투명도·점도·필압·틸트 토글), 슬라이더 범위(0.5~30pt 확인) | 동일 |
| S7 | "Choose Default Pens"(가칭) 시트 | 슬롯 수, 카테고리 탭 구성, 변형 개수 4·4·4·4·3 검증 | 동일 |
| S8 | 메인 툴바 고정/플로팅 두 상태 | 슬롯 수·배치, 형광펜 분리 여부 | beingpaperless 리뷰 이미지 |
| S9 | 수채 펜 겹침·물 번짐 | 블렌드 모드·엣지 처리 | 동일 |
| S10 | 특수선(이중·삼중·물결·점선) 곡선 구간 확대 | 오프셋 복제 시 코너 처리, 점선 간격 고정 여부 | 동일 |

영상 URL·타임스탬프: **이번 세션 미확보**(유튜브·레딧 접근 차단 + 검색에서 Notes+ 전용 영상 미검출). 사용자가 iPad 접근 가능한 지인·공개 영상으로 S1~S3만이라도 확보하면 연필 설계 착수 가능.

---

## 10. 출처 목록 (실제 검색 결과에 노출·인용된 URL만, 열람일 모두 2026-10-24, 모두 스니펫 경유)

**공식(스토어·개발사·특허)**
- App Store Notes+ (US) — <https://apps.apple.com/us/app/notes-notes-for-pencil/id1609361989>
- App Store 노트+ (KR) — <https://apps.apple.com/kr/app/%EB%85%B8%ED%8A%B8/id1609361989>
- App Store 개발사 SINGFISH — <https://apps.apple.com/us/developer/singfish-pte-ltd/id1745986454>
- App Store Notes Plus X — <https://apps.apple.com/us/app/notes-plus-x/id1501351725>
- App Store Notes Plus — <https://apps.apple.com/app/notes-plus/id374211477>
- App Store Notes Plus Bundle — <https://apps.apple.com/us/app-bundle/notes-plus-bundle/id1581263659?l=ko>
- Notes Plus User Guide v4.7 (2015, Prime Circa) — <http://static.gowriteon.com/files/UserGuide_V4-7.pdf>
- Notes Plus 튜토리얼(Medium 공식) — <https://medium.com/notesplusapp/notes-plus-tutorials-9e674dfdee19>
- Notes+ 공식 X — <https://x.com/notesplus_app?lang=en>
- Notes+ 공식 Lemon8 게시물 — <https://www.lemon8-app.com/@notesplus_app/7657175642991084040?region=sg>
- 특허 US9189695B2 — <https://patents.google.com/patent/US9189695B2/en> · <https://patents.justia.com/patent/20140355884> · <https://patents.justia.com/assignee/prime-circa-inc>

**2차 글(리뷰·위키·커뮤니티·집계)**
- 나무위키 Notes+ — <https://namu.wiki/w/Notes+> (r4: `?uuid=dad068d1-d045-44b6-bb0d-f0909ed77424`, r13: `?uuid=f7ada687-594a-43e7-ad5e-b31463906c82`)
- Being Paperless — Notes+ 리뷰 <https://beingpaperless.com/notes-review-another-cool-free-handwriting-app-for-the-ipad/> · 2025 UX 로스터 <https://beingpaperless.com/top-handwriting-apps-with-the-best-user-experience-2025/> · 2025 구매 가치 <https://beingpaperless.com/these-handwriting-apps-are-worth-buying-for-2025/> · Notes Plus X pt1 <https://beingpaperless.com/notes-plus-x-2024-full-review-pt-1-the-basics/> · pt2 <https://beingpaperless.com/what-can-add-in-notes-plus-x-full-review-pt-2/> · pt3 <https://beingpaperless.com/is-notes-plus-x-worth-it-2024-review-pt-3/>
- Macdrifter 2018 — <https://www.macdrifter.com/2018/02/the-difference-of-good-ink-on-the-ipad-pro.html>
- Macworld — <https://www.macworld.com/article/231219/the-best-note-taking-apps-for-the-ipad-and-apple-pencil.html>
- 디시인사이드 아이패드 갤러리 장문 후기 — <https://gall.dcinside.com/mgallery/board/view/?id=ipad1&no=749190>
- 미코 — <https://meeco.kr/mini/39325820>
- 클리앙 — <https://www.clien.net/service/board/cm_iphonien/19009468>
- AppBrain — <https://www.appbrain.com/appstore/notes-notes-for-pencil/ios-1609361989>
- AppPricingLab — <https://apppricinglab.com/app/apple/1609361989>
- UpdateStar(Notes Plus X 1.17) — <https://notes-plus-x.updatestar.com/>
- worldsapps(Notes Plus 6.6.4) — <https://worldsapps.com/download-notes-plus>
- GitHub 검색 유일 결과 — <https://github.com/quodlibetor/NotesPlusPortability>

**접근 실패(참고용, 내용 미열람)**: writeon.cool · apps.appfollow.io · reddit · youtube · web.archive.org — 전부 프록시 403/DNS 차단.

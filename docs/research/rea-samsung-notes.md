# REA — 삼성노트(Samsung Notes) + Android 공식 저지연 잉크 기술

> 작성 2026-10-24 · Platen 리서치. 열람일은 모두 **2026-10-24**.
> 형식: 관찰 → 수치 → 출처·등급. 실기기 실측은 문서 끝 "LOCAL_TODO 후보"로 분리.

> **정리 완료(D-020 ③, 2026-10-24)** — 디컴파일·네이티브 정적 분석 유래 항목 삭제, 항목별 판단은 문서 끝 "11. 정리 기록".

## 0. 범위·등급·네트워크 고지

**등급 범례**
- **공식 문서** — Android Developers, androidx 릴리스 노트, 라이선스 파일, AOSP 미러 소스.
- **1차 관찰** — 공개 리포지토리의 README·소스·코드 검색 결과.
- **2차 글** — 제3자의 분석 문서·커뮤니티 글. 실기기 검증 아님.
- **2차 글(3자 포맷 분석)** — 사용자 소유 `.sdocx` 파일의 바이트 분석(twangodev `file-format.md`·`integrity-findings.md`·`rendering-support.md`, bxff `RESEARCH.md`). twangodev의 APK/네이티브 정적 분석 문서 유래 내용은 D-020 ③으로 삭제.
- **(스니펫)** — WebSearch 결과 요약만 봤고 **원문 페이지는 열지 못함**. 등급을 한 단계 낮춰 취급.
- **이전 프로젝트 실측** — BRIEF가 인정한 iwhg 실측 사실.
- **추정** — 위 자료로 직접 확인 못 했지만 정황상 유력. 반드시 실측/재확인.

**이 세션에서 열람 실패(DNS 차단)한 호스트** — 재시도하지 않음. 해당 정보는 (스니펫) 또는 "자료 부족"으로 표기:
`samsung.com`, `ushl.samsung.com`, `*.community.samsung.com`, `developer.samsung.com`, `news.samsung.com`, `samsungmobilepress.com`, `apps.microsoft.com`(microsoft.com → 리다이렉트), `sammobile.com`, `sammyfans.com`, `gsmarena.com`, `wikipedia.org`, `wacom.com`, `androidcentral.com`, `androidheadlines.com`, `9to5google.com`, `xda-developers.com`, `apkmirror.com`, `android.googlesource.com`. `api.github.com`(gh CLI)은 세션 정책으로 403. `github.com` 페이지와 `raw.githubusercontent.com`, `developer.android.com`은 정상. WebSearch는 세션 예산 소진으로 후반부 질의 불가(에어 액션·호버 거리·터치 리포트 레이트·하이퍼링크 플래너 등은 "자료 부족").

**타사 코드 사용 금지 준수** — 아래 GPL 파서(twangodev/sdocx, Ethran/notable 등)는 *원리와 수치*만 적었다. 디컴파일 APK 리포·네이티브 정적 분석 유래 항목은 D-020 ③에 따라 삭제했고 참조하지 않는다. Platen에 코드·에셋을 가져오면 안 된다.

---

## 1. 앱 정체·버전

| 항목 | 관찰 | 출처·등급 |
|---|---|---|
| 플랫폼 | Android(One UI 기본 앱, 갤럭시 전용) + Windows(Microsoft Store) _(삭제: 디컴파일 유래 — D-020 ③: 내부 패키지 경로)_ | (스니펫) Microsoft Store·Samsung 지원 |
| Windows판 제한(연혁) | 2023-10 업데이트로 "Samsung PC, Galaxy Book 전용" 제한 → 2025-05 업데이트로 다시 모든 Windows PC 설치 가능, 단 **Samsung Account 앱 설치 필수**, 비삼성 PC에선 "Galaxy Book에 최적화" 경고 표시 | (스니펫) 9to5google 2023-10-03, SamMobile 2025-05-22 — 원문 미열람 |
| Windows판 펜 종류 설명 | 스토어 설명: "pens, fountain pens, pencils, highlighters, etc., 다양한 색·굵기" | (스니펫) Microsoft Store 9NBLGGH43VHV |
| 2025~2026 기능 추가 | 가로 페이지 레이아웃·스티키 노트(One UI 8 베타, 2025-08) / **표 삽입** + 새 연필·형광펜 펜 타입(One UI 8.5, 2026-01) / 직선 형광펜·직선 마커 4종을 통합하고 **"Straighten lines" 토글**로 대체(2026-08) / 레이저 포인터(커뮤니티 보고 2025-03, 앱 4.4.26.71) | (스니펫) GSMArena news-69244, SamMobile 2026-01-22·2026-08-10, eu.community.samsung.com 11850692 |

(삭제 행 2 — D-020 ③: 포트 리포 존재 근거, RE 문서 참조 APK 버전)

**자료 부족**: 현재 스토어 버전 번호(Android/Windows), Windows판에서 빠진 기능의 공식 목록, 비삼성 기기 지원 여부의 공식 근거.

---

## 2. 지연(latency) — 최우선

### 2a. 삼성노트가 체감 지연이 적은 이유 — 공개 정보로 재구성

_(삭제: 디컴파일 유래 — D-020 ③: S Pen SDK 엔진 내부 구조 — 프론트버퍼 드로우패드·예측 토글·VSync 전달 클래스, 신경망 예측기 수치·타이밍·램프, 펜 입력 경로, MotionEvent 어댑터, 기기 정책)_ 공개 자료로 남는 것은 마케팅 수치뿐이며, 2.8 ms도 "삼성노트로 측정"이다.

| 관찰 | 수치/세부 | 출처·등급 |
|---|---|---|
| Wacom 보도자료: **2.8 ms는 Tab S8 Ultra/S8+에서 "Samsung Notes 사용 시" 측정** | 삼성 발표 지연치는 삼성노트 앱 포함 수치 | (스니펫) wacom.com 2022/1469 — 원문 미열람 |

(삭제 행 9 — D-020 ③)

**추정(실측 필요)**: 삼성노트 체감 지연 = (하드웨어 EMR + 120 Hz 패널) + 앱 측 저지연 기법. 어떤 기법인지는 공개 자료로 확인되지 않으나, 공식 API(§2c)만으로도 프론트버퍼 렌더와 모션 예측은 구현 가능하다. 커뮤니티에도 "같은 기기라도 앱마다 지연이 크게 다르다"는 관찰이 있다 — (스니펫) Shapr3D 포럼.

### 2b. S펜·Tab S9+ 공식 스펙

| 항목 | 값 | 출처·등급 |
|---|---|---|
| 필압 레벨 | 4096 | (스니펫) 리셀러 스펙(EJ-PX710), Samsung Note8 지원 페이지 "2048→4096" — 공식 Tab S9 스펙 페이지는 차단 |
| S펜 지연 | Tab S8+/S8 Ultra **2.8 ms**(기본 Tab S8은 6.2 ms); Tab S9 시리즈도 2.8 ms로 보도 | (스니펫) AndroidHeadlines, AndroidCentral — 삼성 1차 자료 미확인 |
| 디스플레이 | 12.4" 120 Hz(추정; gsmarena·삼성 스펙 페이지 차단) | 추정 |
| S펜 리포트(샘플링) 레이트, 호버 거리 | **자료 부족** → LOCAL_TODO ② | — |
| 터치 리포트 레이트 | **자료 부족** → LOCAL_TODO ② | — |
| Samsung Developers S Pen SDK/Remote SDK 문서 | 열람 실패(차단) | — |

### 2c. Android 공식 저지연 기술 표

#### (1) `androidx.graphics:graphics-core` (`androidx.graphics.lowlatency`)

| 항목 | 내용 | 출처·등급 |
|---|---|---|
| 최신 버전 | **안정 1.0.4 (2025-12-03)** "특정 기기 호환성·성능 개선" / **1.1.0-beta01 (2026-10-07)**: `CanvasFrontBufferedRenderer#execute` 추가, **CanvasFrontBufferedRenderer·LowLatencyCanvasView의 뒤집힌 버퍼 변환 수정**, API<32 회전 폴백, "버퍼 내용을 보존하지 않는 기기"의 시각적 아티팩트 수정, surface 재생성 시 중복 draw/스톨 수정, SurfaceControl 메모리 누수 수정, draw 루프 할당 감소 | 공식 문서 — https://developer.android.com/jetpack/androidx/releases/graphics |
| 연혁 | 1.0.0-alpha01(2022-10-24) `GLFrontBufferedRenderer` / alpha04(2023-06-07) `CanvasFrontBufferedRenderer` / 1.0.0-beta01(2023-12-13) `LowLatencyCanvasView`, `setDataSpace` 요구 T→Q 완화 / 1.0.0 안정 2024-05-29 / 1.0.2 Android 14+ "front buffer usage flag 미지원 기기" 깜빡임 수정 / 1.0.3 API<33 전체화면 깜빡임 수정 | 공식 문서(동일) |
| 무엇을 주나 | 진행 중 획을 **프론트버퍼(화면이 스캔아웃 중인 버퍼)에 직접** 그려 버퍼 스왑 지연을 없앰. 획이 끝나면 `commit()`으로 멀티버퍼 레이어에 합성 | 공식 문서 — advanced-stylus-features 가이드 |
| API | `GLFrontBufferedRenderer<T>(surfaceView, callback)`: `renderFrontBufferedLayer(data)` → `onDrawFrontBufferedLayer`, `commit()` → `onDrawMultiBufferedLayer`, `cancel()`(팜 리젝션 시). `CanvasFrontBufferedRenderer<T>`는 같은 모델을 `Canvas`로. 콜백은 `@WorkerThread`("CanvasRenderThread"), `execute(runnable)`은 렌더 스레드에 큐잉(블로킹 금지). 버퍼 포맷 RGBA_8888/RGBX_8888 보장, `colorSpace`는 Android U+에서만 적용 | 공식 문서(가이드) + 1차 관찰(androidx 소스 KDoc) |
| `LowLatencyCanvasView` | `ViewGroup`. 내부 `SurfaceView`(z-order top) 위 프론트버퍼 `SurfaceControl` 1장 + 끝나면 버퍼를 `Bitmap`으로 감싸 View 트리의 `onDraw`로 이동. "단순한 사례를 위해 설정 자유도를 희생". 와이드 컬러는 API 34+·Activity 호스트에서만. 콜백 `onRedrawRequested / onDrawFrontBufferedLayer / onFrontBufferedLayerRenderComplete` | 1차 관찰 — androidx 소스 KDoc |
| 최소 버전 | `@RequiresApi(Q)` = **API 29**. 가이드: "Android 10+, ChromeOS Android 10+" | 공식 문서 + 소스 |
| 제약 | **작은 영역(필기)만**. 전체화면·팬·줌에 쓰면 **티어링**. 획당 수백 점이므로 데이터 객체는 가볍게. `requestUnbufferedDispatch`를 `ACTION_DOWN`에서 호출 권장. 1.1.0-beta01까지 변환/깜빡임 버그가 계속 고쳐지고 있음 → 기기별 QA 필수 | 공식 문서 |

#### (2) `androidx.input:input-motionprediction`

| 항목 | 내용 | 출처·등급 |
|---|---|---|
| 최신 버전 | **1.0.0 안정 (2025-11-19)**. rc01(2025-11-05)에서 **minSdk 21→23**. beta06(2025-08-13) "시스템 예측 API가 있으면 사용". beta04(2024-05) 내장 예측 기본화, 예측이 급정지하지 않게 수정. beta03 정확한 도구에서 신뢰도↑. beta01 예측 이벤트에 orientation·tilt 포함 | 공식 문서 — https://developer.android.com/jetpack/androidx/releases/input |
| 무엇을 주나 | `MotionEventPredictor.newInstance(view)` / `record(event)` 매 이벤트 / `predict()` → **다음 프레임 렌더 시점**의 예측 `MotionEvent`(x, y, pressure, orientation, tilt) 또는 null | 공식 문서(가이드) + 1차 관찰(Javadoc) |
| 동작 | API 34+에서는 프레임워크 `android.view.MotionPredictor`(API 34 추가 — API diff 확인)를 사용하되 **"stylus 이벤트로 제한될 수 있음"** → `MotionPredictor.isPredictionAvailable` 확인 권고. 그 외는 라이브러리 내장 **칼만 필터**(`KalmanMotionEventPredictor`: `PredictionEstimator`가 예측 시간차를 내고 `MultiPointerPredictor.predict(dt)`) | 1차 관찰 — androidx 소스 Javadoc; 공식 API diff 34 |
| 제약 | 예측점은 **최종 렌더에 쓰지 말 것**, 새 실점이 오면 제거. 가이드 본문은 "API 19+"라고 쓰여 있으나 릴리스 노트상 minSdk는 23 — 문서 불일치 | 공식 문서 |
| 이전 프로젝트 실측 | **기본 OFF가 체감상 나았다(밀림)**. Platen에서 예측을 켠다면 예측 길이 램프·페이드 같은 완화가 필요하다는 뜻 | 이전 프로젝트 실측 |

#### (3) `androidx.ink`

| 항목 | 내용 | 출처·등급 |
|---|---|---|
| 최신 버전 | **1.0.0 안정 (2025-12-17)** (graphics-core 1.0.4 의존) / **1.1.0-alpha10 (2026-10-21)** | 공식 문서 — https://developer.android.com/jetpack/androidx/releases/ink |
| 1.1.x 알파 변화 | a01 `InProgressShape.isCanceled` / a02 `BrushFamily` 직렬화 안정화, `BrushFamily.decode` max-version / **a03 프로그램적 브러시 커스터마이즈 API 공개**, `BrushPaint.TextureLayer` 서브클래스 분리 / a04 KMP `ByteArray` 저장 API / **a05 부분 획 지우개(메시 편집) 실험적** / a06 브러시 버저닝·텍스처 애니메이션, `InProgressStrokesView` 메모리 누수 수정 / a07 밀집 파티클 브러시 성능 회귀 수정 / a08 KMP iOS `MetalRenderer`(실험) / a09 색 시프트 **Oklab**, `SATURATION/LUMINOSITY`→`CHROMA/LIGHTNESS` / a10 부분 지우개 절단면 안티에일리어싱 | 공식 문서(동일) |
| 1.0.x 변화 중 Platen 관련 | a03 `StrokeInputBatch` 직렬화·올가미 / **a04 커스텀 `BrushFamily`(proto 기반) — 연필·레이저 포인터 예시** / a05 Compose 모듈, `TextureBitmapStore`를 brush 모듈로 이동 / a06 이모지 형광펜 / **a07 형광펜용 `SelfOverlap`, 입력 스무딩 개선, 각도 단위 도(degree)** / beta02 **실험적 커스텀 저지연 셰이프 API**(`InProgressShapesView`) | 공식 문서(동일) |
| 모듈 | `ink-strokes`(`StrokeInputBatch`·`InProgressStroke`·`Stroke`), `ink-brush`(`Brush`·`BrushFamily`·`StockBrushes`, `TextureBitmapStore`), `ink-geometry`(`Box`·`Vec`·`PartitionedMesh`, 교차·변환), `ink-authoring`(`InProgressStrokesView`, Views) / `ink-authoring-compose`(`InProgressStrokes`), `ink-rendering`(`CanvasStrokeRenderer`·`ViewStrokeRenderer`), `ink-storage`(protobuf + 델타 압축), `ink-nativeloader`(C++ 로드), `-compose` 변형 3종 | 공식 문서 — ink-api-modules, ink-api-setup |
| 의존 | `ink-authoring`의 build.gradle: **graphics-core 1.0.4 + input-motionprediction 1.0.0**, compileSdk 35. 내부 `CanvasInProgressStrokesRenderHelperV29`가 `CanvasFrontBufferedRenderer`를 쓰고 `FrontBufferToHwuiHandoff`(@RequiresApi Q)로 View 트리로 넘김 → **API 29 미만에서는 저지연 경로 없음(추정: 일반 View 경로 폴백)** | 1차 관찰 — androidx 소스 |
| 브러시 모델 | `Brush(family, color, size, epsilon)`. **스톡 패밀리(androidx-main 기준)**: `marker`(원형 고정폭), `pressurePen`(필압·속도 반응, 스타일러스 필기 최적), `highlighter`(치즐 팁, 반투명 색, `SelfOverlap ANY/ACCUMULATE/DISCARD`), `dashedLine`(올가미용), `emojiHighlighter`(텍스처 사용 — `clientTextureId`를 `TextureBitmapStore`로 매핑해야 보임; 미니 이모지 트레일은 Android U+). `predictionFadeOutBehavior`(BrushBehavior). **현재 소스에 스톡 연필 없음** — 연필은 커스텀 BrushFamily 예시로만 언급 | 1차 관찰 — `StockBrushes.kt`; 공식 릴리스 노트 |
| 이전 프로젝트 실측 대조 | 실측 "스톡 연필 = 텍스처 타일링 브러시, `TextureBitmapStore` 없으면 맨 회색 획", "`TextureLayer.animationDurationMillis` 0이면 크래시(1 이상)". 현재 main의 `StockBrushes.kt`에는 pencil이 없으므로 실측 당시 버전(또는 커스텀 패밀리)의 이야기로 보인다. 텍스처 브러시엔 `TextureBitmapStore` 필수라는 원칙은 `emojiHighlighter` KDoc과 일치 | 이전 프로젝트 실측 + 1차 관찰 |
| 커스텀 브러시 | `BrushFamily.decode(inputStream)`(gzip된 BrushFamily proto), `@ExperimentalInkCustomBrushApi`. 1.1.0-alpha03부터 프로그램적 생성 API 공개. **Procreate/ABR → BrushFamily 변환의 착지점** | 공식 문서 — ink-api-brush-apis |
| `InProgressStrokesView` | `FrameLayout`, `@UiThread`. `startStroke(MotionEvent, pointerId, brush, …)` → `InProgressStrokeId`, `addToStroke(event, id, prediction?)`(**예측 MotionEvent를 인자로 넘기는 구조**), `finishStroke`, `cancelStroke`, `cancelUnfinishedStrokes`. `textureBitmapStore`는 **첫 획 또는 `eagerInit()` 이전에 설정**. `maskPath`(떠 있는 UI 아래 wet ink 가림), `motionEventToViewTransform`, `handoffDebounceTimeMs`, `requestHandoff()`. 완료 획은 `InProgressStrokesFinishedListener`로 전달되고 클라이언트가 **같은 UI 프레임에 자기 그림을 그린 뒤 `removeFinishedStrokes`** 해야 깜빡임/이중 표시 없음. `flush()`는 best-effort(onPause). 지연 콜백은 할당 금지 | 1차 관찰 — androidx 소스 KDoc |
| 좌표·epsilon | world unit = **1 dp** 권장, epsilon **≤ 0.25**(고밀도에서 ~1 px), 10배 줌 허용 시 **0.025**. epsilon이 작을수록 CPU/메모리/GPU 비용↑ | 공식 문서 — ink-api-coordinate-system |
| 저장 | `StrokeInputBatch.encode/decode`(ink-storage, protobuf+델타). 브러시는 패밀리 enum + colorLong + size + epsilon만 저장 권고 | 공식 문서 — ink-api-state-preservation |
| 지오메트리 | 전체 획 지우개: 이전점→현재점 `MutableSegment` → `populateFromSegmentAndPadding` → `shape.intersects(parallelogram, AffineTransform.IDENTITY)`. 부분 지우개는 1.1.0-alpha05 실험 API 전까지 `StrokeInputBatch` 분할로 직접 | 공식 문서 — ink-api-geometry-apis |
| 라이선스 | androidx 전체 **Apache License 2.0**(LICENSE.txt). 코어 **google/ink Apache-2.0**, Bazel 7 + NDK, 모듈 color/types/geometry/brush/strokes/rendering/storage, "인터페이스 안정성 보장 없음" | 공식 문서(라이선스) + 1차 관찰(google/ink README) |
| 공식 샘플 | **androidx.ink 전용 공식 샘플 리포는 찾지 못함**(android/platform-samples user-interface 목록, android/snippets touchinput 목록에 없음). 저지연·예측 공식 샘플은 **코드랩 `android/large-screen-codelabs/advanced-stylus`**(start / palm-rejection / low-latency / end, Apache 2.0): Compose + `SurfaceView` + `GLFrontBufferedRenderer` + `MotionEventPredictor` + `FLAG_CANCELED`, 사용 버전 graphics-core 1.0.0-alpha03 · motionprediction 1.0.0-beta01(구버전) | 1차 관찰 + 공식 코드랩 |

#### (4) 프레임워크 `MotionEvent` / `View` API

| API | 무엇을 주나 | 제약·버전 | 출처·등급 |
|---|---|---|---|
| 배칭·`getHistorySize()/getHistoricalX/Y/AxisValue/EventTimeNanos` | `ACTION_MOVE` 한 개에 여러 샘플이 묶임. 과거 샘플은 historical, 현재 좌표가 최신 | 클래스 Javadoc "Batching" 절. `getHistoricalEventTimeNanos`·`getEventTimeNanos`는 **API 34** 추가 | 1차 관찰(AOSP 미러 MotionEvent.java) + 공식 API diff 34 |
| `View.requestUnbufferedDispatch(MotionEvent)` | 입력 배칭을 끄고 즉시 전달. 가이드는 `ACTION_DOWN`에서 호출 | API 레벨: MotionEvent 인자 오버로드 **API 21(추정)**; `(int source)` 오버로드는 API 29 diff에 없음 → 버전 미확정 | 공식 문서(가이드) / 추정 |
| `AXIS_PRESSURE` | 0~1, **캘리브레이션에 따라 1 초과 가능 → 정규화** | — | 공식 문서 |
| `AXIS_TILT` | 라디안, 0 = 수직, π/2 = 눕힘(실제로 π/2는 거의 안 나옴). 첫 포인터 단축 getter 없음 | API 14(추정) | 공식 문서 / 추정 |
| `AXIS_ORIENTATION` | 라디안, 0~π 시계방향 / 0~−π 반시계. 납작 브러시에 유용 | API 9(추정) | 공식 문서 / 추정 |
| `AXIS_DISTANCE` + `ACTION_HOVER_ENTER/MOVE/EXIT` | 호버 거리(0.0 = 접촉, 클수록 멂). **정밀도는 하드웨어 의존 — 임계 로직에 쓰지 말 것**. Compose: `hoverable`·`indication` | HOVER 액션 API 12/14(추정) | 공식 문서 / 추정 |
| `BUTTON_STYLUS_PRIMARY/SECONDARY`, `ACTION_BUTTON_PRESS/RELEASE`, `getButtonState()` | 옆버튼 | API 23(추정; API diff 23 페이지가 바이너리로 와서 미확인) | AOSP Javadoc 존재 확인 / 추정 |
| 팜 리젝션 `ACTION_CANCEL` | 내비 제스처·팜 리젝션 시 발생. `getPointerId(getActionIndex())`로 획 찾아 제거 후 재렌더 | API 1 | 공식 문서 |
| 팜 리젝션 `FLAG_CANCELED` | `ACTION_POINTER_UP`/`ACTION_CANCEL`에만 설정. 세트돼 있으면 그 포인터의 마지막 DOWN부터 취소 | **API 33**(API diff 확인) | 공식 문서 + API diff |
| `TOOL_TYPE_PALM` | "The tool is a palm and should be rejected" — **`@hide`**. SDK 앱은 못 씀. 공식 스타일러스 가이드에도 없음 | 공개 API 아님 | 1차 관찰(AOSP 미러 소스) |
| 가장자리 제스처 오입력 | `WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` | — | 공식 문서 |
| `CLASSIFICATION_*`, `getClassification()` | 딥프레스/모호 제스처 분류 | API 29(API diff 확인), PINCH/TWO_FINGER_SWIPE API 34 | 공식 API diff |
| `MotionPredictor`(프레임워크) | 시스템 예측기, `isPredictionAvailable(deviceId, source)` | **API 34**(API diff 확인). 스타일러스 한정 가능 | 공식 API diff + 1차 관찰(Javadoc) |
| 텍스트 필드 손글씨 | API 34부터 모든 `EditText`에 스타일러스 손글씨 자동. **캔버스 위 텍스트 필드는 `setAutoHandwritingEnabled(false)`** 필요(필기 가로챔 방지) | API 34 | 공식 문서 — stylus-input-in-text-fields |
| 스타일러스 에뮬레이션(테스트) | Android 14 `persist.debug.input.simulate_stylus_with_touch`, 15+ `debug.input.simulate_stylus_with_touch`(루팅/에뮬레이터) | — | 공식 문서(동일) |

#### (5) 프론트버퍼를 쓰는 오픈소스 필기 앱 — 조합 사례

| 리포 | 조합 | 관찰 | 라이선스 | 등급 |
|---|---|---|---|---|
| android/large-screen-codelabs `advanced-stylus` | Compose UI + `SurfaceView` + `GLFrontBufferedRenderer<Segment>` + `MotionEventPredictor` + `FLAG_CANCELED` | 공식 패턴: DOWN에 `requestUnbufferedDispatch`+front, MOVE front(+예측 세그먼트), UP `commit()`, `glScissor`로 예측 잔상 제거 | Apache 2.0 | 공식 코드랩 |
| Ethran/notable | `GLFrontBufferedRenderer<StrokePoint>`, `MotionEventPredictor.record`만 호출(예측 미사용), DOWN에 `requestUnbufferedDispatch`, 스타일러스만 그림, ~16 ms 스로틀 | Onyx BOOX e-ink 대상(Onyx SDK 의존, API 29+) | 미확인(README에 명시 없음) | 1차 관찰 |
| Giovix64/Mente | **Compose + `CanvasFrontBufferedRenderer` 단일 SurfaceView + 자체 예측**, 6종 펜(필압·틸트·방위), 스타일러스 전용 모드 팜 리젝션 | README 경고: 렌더러에 요청을 쏟으면 프레임을 못 내놓음 → in-flight 1개 제한·자동 재생성 | GPL-3.0 | 1차 관찰 |
| ccptr/snot | Compose + Rust 코어(UniFFI) + Android 10+에서 "front-buffered layer" | 삼성노트식 텍스트+필기 혼합을 표방. 데스크톱(GPUI)은 필압 없음 | GPL-3.0+ | 1차 관찰 |
| lloyd-g-w/len | **Flutter 앱 + Android 네이티브 `WetInkLayer`(`CanvasFrontBufferedRenderer`)를 Flutter 뷰와 같은 바운드에 겹침** — wet 획만 프론트버퍼, dry는 Flutter | Flutter 후보 스택이 S펜 지연을 잡으려면 이 구조가 필요하다는 실증. S Pen 우선, 안정화·팜 리젝션 | GPL-3.0+ | 1차 관찰(코드 검색 + README) |
| LanRhyme/ReveriePaint | Compose + Krita C++ 코어 + `SurfaceView(setZOrderOnTop)` + `CanvasFrontBufferedRenderer` 미리보기 오버레이 | 주석: 부모 컨테이너가 스케일 애니메이션 중일 때 **첫 획의 프론트버퍼 `HardwareBuffer`가 축소 크기로 생성돼 획이 갈라짐**(펜업 `commit()` 후 복구) → 캔버스 진입 애니메이션은 fade만 | GPL-3.0 | 1차 관찰(소스 주석) |
| Satwik-Miyyapuram/weaveforge | `CanvasFrontBufferedRenderer` 오버레이, 주석 "닙→픽셀 약 6 ms vs 웹뷰 30~50 ms" | 수치는 저자 주장, 미검증 | 미확인 | 1차 관찰(주석) |
| jr-mccoy/AI-Chat-Sandbox | 주석: graphics-core와 `AndroidView` 통합이 "fiddly"해서 일반 View로 후퇴 | 통합 난이도 증언 | 미확인 | 1차 관찰(주석) |
| nahkd123/FrontBufferDraw | "여러 기기에서 프론트버퍼 성능 테스트" 앱 | 결과 미게시 | CC0 | 1차 관찰 |

---

## 3. 필기감·잉크

| 항목 | 관찰 → 수치 | 출처·등급 |
|---|---|---|
| 기본 펜(볼펜형) | 필압→굵기 약 **1.2배** 범위, 끝 테이퍼 속도 의존 **0.58~0.85**(0까지 안 감) | 이전 프로젝트 실측 |
| 커뮤니티 교차 | "표준 펜 굵기는 속도 의존, 필압 반응은 만년필·경사펜·잉크 브러시에만" / 어떤 업데이트 후 필압 반응이 세졌다는 보고(날짜 불명) | (스니펫) r1.community.samsung.com 2870358, 31975559 |
| 캘리그라피 펜 | 닙 **141°**(↗↙), 납작비 **0.39**, 필압 무관 | 이전 프로젝트 실측 |
| 사용자에게 보이는 펜 수 | 즐겨찾는 펜 추가 시 **6종 펜 타입** 중 선택 + 색 + 굵기. 내부 펜 종류 수·펜별 렌더 모델·굵기 보간·불투명도 규칙은 **자료 부족** | (스니펫) Samsung 지원 "How to use Samsung Notes" |
| 스무딩 설정 UI | 공식 토글 없음. 커뮤니티 "스무딩을 끌 수 있으면" 요청, 빠른 획이 덜 그려진다는 보고(Tab S6 Lite, One UI 6.1) | (스니펫) r1.community 30274740 |
| 필압 커브 설정 UI | 없음(굵기·색만). 내부 커브 처리는 **자료 부족** _(삭제: 디컴파일 유래 — D-020 ③: 어댑터 무변환 → 엔진 고정값 추론)_ | (스니펫) |
| **형광펜 다크 모드 비가시** | 2023 커뮤니티: "어두운 종이에서 형광펜이 안 보임"(해결책 미기재). 별개 스레드: 설정 → Page style and template → **"Adjust color in dark mode"** 옵션으로 글자색 반전. 공식 문서 없음 | (스니펫) r1.community 18658456, us.community 2011431 |
| 삼성 PDF 내보내기의 형광펜 | 서드파티 변환기가 "자기 출력의 형광펜이 삼성 PDF 내보내기보다 약 2배 진하다"고 기록 → 삼성 내보내기는 형광펜 알파를 낮게 굽는다는 간접 증거 | 1차 관찰 — FizzyHedgehog/sdocx-converter-android README |
| 예측 잔상 처리 | androidx.ink: `predictionFadeOutBehavior` _(삭제: 디컴파일 유래 — D-020 ③: 삼성 램프·리셋 방식)_ | 1차 관찰 |

(삭제 행 4 — D-020 ③: 내부 펜 레지스트리, 펜 굵기 레벨 보간, Fountain V14 렌더 모델, 펜별 불투명도)

---

## 4. 기능·UX 동선

대부분 공식 도움말이 차단돼 **(스니펫) 또는 자료 부족**. 실측 항목은 LOCAL_TODO에 최소화해 넣었다.

| 항목 | 관찰 | 출처·등급 |
|---|---|---|
| 툴바·펜 설정 동선(아이콘 재탭?) | **자료 부족** | — |
| 즐겨찾는 펜 | "Favourite pen 아이콘 → 추가 아이콘"으로 추가, 빨간 마이너스로 제거. **슬롯 수 자료 부족**. 커뮤니티에 "즐겨찾는 펜 폴더" 요청(긴 목록을 순회해야 함) → 폴더 없음 | (스니펫) Samsung 지원 페이지, eu.community 7028521 |
| 세로 스크롤·페이지 카드·폭맞춤 기본 | **자료 부족**. 단, "pageless" 문서(연속 1장)가 포맷에 존재(변환기가 ISO 페이지로 분할 옵션 제공) + 2025-08 **가로 페이지 레이아웃** 추가 | 1차 관찰(sdocx2pdf README) + (스니펫) |
| 폴더 | 지원(일반 지식) — 공식 페이지 미열람 | 추정 |
| 올가미 | 자유형 올가미 크롭은 `.spi`로 저장. 도구 UI는 **자료 부족** _(삭제: 디컴파일 유래 — D-020 ③: 내부 펜 이름)_ | 1차 관찰(FizzyHedgehog) |
| 텍스트·이미지 | 객체 타입: Stroke(1), TextBox(2), Image(3), Container(4), Shape(7), Line(8), Voice(10), Formula(11), Web(13), Painting(14), Video(16), **Link(17)**, Plot(20), Math(21), **Table(22, 리치텍스트 내)**, CodeBlock(23), AttachedFile(24), StrokeGroup(100); Deprecated Table(12) | 2차 글(3자 포맷 분석) — `rendering-support.md`(페이지 레코드의 객체 타입 ID) |
| 도형 보정 | Shape/Line 객체 + 2026-08 "Straighten lines" 토글(직선 형광펜 4종을 대체) _(삭제: 디컴파일 유래 — D-020 ③: 내부 Beautify 펜)_ | 2차 글(3자 포맷 분석) + (스니펫) |
| 템플릿·표지 | 설정 "Page style and template"(다크 모드 색 보정 포함) | (스니펫) |
| 검색(손글씨 인식) | **자료 부족**(검색 예산 소진). 일반 지식상 지원 | 추정 |
| 내보내기 | **.sdocx(Samsung Notes file) · PDF · Word · PowerPoint · 이미지 · 텍스트**. PPT는 가로 레이아웃으로 변환, 손글씨는 이미지·타이핑은 텍스트. Word 내보내기는 Note10 세대부터 | (스니펫) Samsung 지원 ANS10002404 등 |
| 옆버튼 관례 | **자료 부족**(공식 페이지 차단). 삼성 기본 "버튼 누른 채 긋기 = 지우개"는 추정 → LOCAL_TODO | 추정 |
| 손가락 필기 토글 | 설정에 **"Finger drawing off"**(S펜 전용 그리기) | (스니펫) Samsung 지원 ANS10003217 |
| 에어 액션/제스처 설정 | **자료 부족** | — |
| 레이저 포인터 | 커뮤니티: 2025-03 업데이트(4.4.26.71)에 **빛줄기 모드·점 모드·색 선택** _(삭제: 디컴파일 유래 — D-020 ③: 내부 펜 이름)_ | (스니펫) |
| 표 삽입 | One UI 8.5(2026-01)에서 추가; 포맷엔 Table(22)·Deprecated Table(12) | (스니펫) + 2차 글(3자 포맷 분석) |

---

## 5. .sdocx (+ .sdoc / .snb)

### 5a. 검증 사실 ↔ 공개 분석 대조

| 이전 프로젝트 실측 | 공개 분석 | 판정 |
|---|---|---|
| zip 변형 | ZIP 컨테이너. 엔트리 `note.note`(헤더·텍스트), `pageIdInfo.dat`(페이지 순서·해시), `<UUID>.page`, `media/mediaInfo.dat`, 미디어, `end_tag.bin`. 엔트리 순서·압축 방식은 의미 없음 | **일치** — 2차 글(3자 포맷 분석) `file-format.md` |
| EOCD 뒤 **148바이트** 푸터 | "컴팩트 end-tag 레코드가 ZIP EOCD 뒤에 덧붙음(압축 해제 없이 읽기 위함)". 크기: 픽스처 A **144**, B·C **148**(길이 0 선택 문자열 포함 시). 고정 서명 문자열로 끝남. 내용: format_version, note id, 타임스탬프, 치수, 앱 이름·버전, minimum_format_version, 마지막 본 페이지 | **일치(148은 현행 변형)** |
| zip general-purpose flag 0x0806 / version-made-by 0 / external-attr 0 | 공개 문서에 **언급 없음** | 실측 고유 — 유지 |
| 무결성 해시 검증 없음 | 해시는 **존재**한다: `note.note` 끝 SHA-256(앞 전부), 같은 값이 `pageIdInfo.dat` 선두; 페이지 끝 32바이트 해시 + 26바이트 서명; 객체/레이어/페이지 해시는 "UUID+수정시각" 기반 논리식. **삼성노트가 가져오기 때 검증하는지는 문서가 말하지 않음** | 실측(검증 안 함)과 모순 없음 — "해시는 있으나 미검증"으로 정리 |
| 획 100% 벡터 파싱(필압·틸트·색·굵기) | 점 수 + 압축/원시. 압축: 첫 점 절대 시드 + 델타. 좌표 델타 **1/32** 정밀(16비트 워드: 부호 1 + 정수 10 + 소수 5비트), 압력·틸트·방위 델타 **1/4096**, 타임스탬프 uint16 델타(ms 모드 옵션). 속성 비트: 압축·틸트/방위 유무·지우개·고정폭·ms·탑레이어. 스타일 필드: **펜 이름 string-table ID(필드 7)**, 고급설정 ID(필드 1, 예 `"2;"` → Marker2 V2), ARGB(저장 순서 B,G,R,A), 크기, 고정폭. 86획 검증 바운딩박스 충전 중앙값 100%, 최악 96.7% | **일치** — 2차 글(3자 포맷 분석) `file-format.md`, bxff `RESEARCH.md` |
| Android MIME `application/spd` | 공개 문서에 **언급 없음** _(삭제: 디컴파일 유래 — D-020 ③: APK 문자열 대조)_ | 실측 고유 — 유지 |
| — | 버전 마커 `format_version`/`minimum_format_version`이 노트·페이지·엔드태그에 각각, 미디어 매니페스트는 3001 경계로 레이아웃 변경(`EOFX`/`EOF`) | 추가 정보 |
| — | 쓰기 실증: **Dietrich가 .sdocx 내보내기(페이지·획·도형·텍스트 필드·이미지·프레임) 구현**. 큰 캔버스는 삼성노트가 느려지거나 거부할 수 있음 | 1차 관찰 — Dietrich README |

### 5b. 오픈소스 파서·변환기 목록

생성일·갱신일은 GitHub 검색 메타(`created_at`/`updated_at`)이며 `updated_at`은 마지막 커밋이 아닐 수 있다(스타 등도 갱신 트리거). 커밋 날짜는 페이지에서 확인 불가.

| 리포 | 언어 | 라이선스 | ★ | 생성 / 갱신 | 범위·구조 | 등급 |
|---|---|---|---|---|---|---|
| squ1dd13/sdocx2pdf | Rust | **MIT** | 50 | 2026-02-18 / 2026-10-20 | `sdocx/`(파서 크레이트: lib, doc, note_doc, page, page_list, media_info, bits, byte_stream, context, end_tag) + `sdocx2pdf/`(바이너리, krilla PDF 1.7). 곡률 기반 키포인트 → 베지에, 일부 펜은 압력→폭. 지원: 손글씨·도형·임베디드 PDF(검색 가능 텍스트 보존)·JPEG·pageless 분할·펜 폭 배율. 미지원: 타이핑 텍스트·링크·페인팅·`.spi`, 캘리그라피≈만년필, 연필/브러시/잉크펜 미구분, 점선 실선화, Tab S11만 테스트. 191 커밋 | 1차 관찰 |
| twangodev/sdocx | Rust + WASM + TS | **GPL-3.0** | 31 | 2026-03-07 / 2026-10-22 | `crates/{sdocx, sdocx-cli, sdocx-wasm}`, `web/`(로컬 뷰어), `conformance/`, `docs/`(파일 포맷·spi 코덱·PDF 분석 문서; **APK·네이티브 정적 분석 문서는 참조 안 함 — D-020 ③**). 출력 SVG(기본)/PNG/PDF. crates.io `sdocx`,`sdocx-cli`, npm `@twango/sdocx`. 쓰기 미지원. 1,395 커밋. 렌더링 매트릭스: Fountain V14/V16, DefaultPen 0~3, Marker2/4 V8, Calligraphy V1(메시), InkPen V1/V4, MagicPen, Oblique 부분 지원; AirBrush/BrushPen/Crayon2 근사; **Rainbow·지우개·직선화·라이브 예측은 재현 안 함** | 1차 관찰 + 2차 글 |
| Downhillghost/Dietrich | Python | **MIT** | 6 | 2026-04-19 / 2026-09-28 | Kaitai Struct 스펙에서 생성한 파서. 읽기: 페이지·획(점별 압력·시간)·리치텍스트·텍스트필드·이미지·PDF 배경·`.spi` 메타. **쓰기: .sdocx / Excalidraw / PNG**. 키보드 텍스트 스케일은 경험식(720폭 1.6, 961폭 3.2, 1080폭 2.19) | 1차 관찰 |
| bxff/samsung-notes-format | Python | **MIT** | 4 | 2025-10-08 / 2026-09-25 | 획 기하 전용(`render.py` ~190줄 SVG). RESEARCH.md에 델타 디코드 상세. 압력·틸트·색은 twangodev로 안내. "삼성 바이너리·디컴파일 미사용" 명시 | 1차 관찰 |
| DomHeadroom/sdocx-hexpatterns | ImHex | **GPL-2.0** | 3 | 2025-02-22 / 2026-09-03 | `mediaInfo.hexpat`, `pageIdInfo.hexpat` | 1차 관찰 |
| FizzyHedgehog/sdocx-converter-android | Kotlin + Rust | **MIT**(단 "All rights reserved" 문구 혼재) | 6 | 2026-10-14 / 2026-10-21 | sdocx2pdf를 서브모듈로 쓴 Android/Galaxy Tab 변환 앱(minSdk 26, Compose, cargo-ndk arm64). 삼성노트 공유 시트 수신. 형광펜 2배 진함·회전 텍스트 줄바꿈 차이 | 1차 관찰 |
| lu-p/sdocx-to-txt-python-converter | Python | 미표시 | 17 | 2021-07-07 / 2026-10-23 | 텍스트만 추출(5개 스크립트) | 1차 관찰 |
| 0xA01F3/sdocx_parser | Python | 미확인 | 1 | 2026-07-26 | HTML/PDF/텍스트, 불릿·체크박스 구조 | 검색 메타만 |
| Mhemd139/Samsung_Notes | TS | 미확인 | 9 | 2026-09-30 / 2026-10-22 | PWA 뷰어 + 읽기 전용 MCP 서버 | 검색 메타만 |
| ashwalk33r/samsung-notes-sdoc-parser | 셸 | MIT | 0 | 2020-01-31 | **.sdoc 파서가 아님** — 내보낸 PDF를 OCR(ocrmypdf) | 1차 관찰 |

**.sdoc / .snb 관계**: `.sdoc`(구 Samsung Notes)와 `.sdocx`의 관계는 공개 자료 **부족** _(삭제: 디컴파일 유래 — D-020 ③: APK 확장자 상수)_. `.snb`(S Note) 파서/변환기 GitHub 검색 **0건** → 자료 부족. `.sdoc` 내부 구조 공개 분석도 찾지 못함.

### 5c. 판정

- **읽기: 가능.** 독립 구현 2개(MIT·GPL)가 획·압력·틸트·색·펜ID·텍스트·이미지·PDF 배경까지 읽는다. 단 "펜 재질 100% 재현"은 가장 앞선 GPL 구현도 "부분"이다. Platen은 스트로크(기하+압력+틸트+색+굵기+펜ID→내부 브러시 매핑)만 가져오면 되므로 **가능**.
- **쓰기: 부분.** 컨테이너·해시·엔드태그·ZIP 플래그 요건은 실측+Dietrich로 재현 가능. 삼성 펜 고유 파라미터(고급설정 문자열·`.spi`)는 재현 불가 → "삼성노트에서 열리는 .sdocx(기본 펜 매핑)" 수준.
- **라이선스 주의**: twangodev(GPL-3.0)·hexpatterns(GPL-2.0)는 **원리만**. 코드 참조는 MIT 계열(sdocx2pdf, Dietrich, bxff)도 복사 금지(BRIEF).
- BRIEF대로 **.sdocx 읽기·쓰기는 후속**. 자체 포맷은 "ZIP + 페이지 파일 + EOCD 뒤 요약 푸터 + 델타 압축 획" 구조를 참고할 가치가 있다(5번 원리).

---

## 6. 템플릿·속지·PDF

| 항목 | 관찰 | 출처·등급 |
|---|---|---|
| PDF 가져오기·주석·내보내기 | 공식 지원: PDF 가져와 주석 후 PDF로 다시 내보내기. 포맷 안에 PDF 페이지 배경이 저장되고(`pdf-paper-storage`), 원본 PDF 벡터·텍스트가 보존돼 변환기가 "검색 가능 텍스트 보존"으로 뽑아냄 | (스니펫) Samsung ANS10002404 + 1차 관찰(sdocx2pdf) + 2차 글(pdf-* 문서) |
| 하이퍼링크 플래너(PDF 내부 링크 탐색) | **자료 부족**. 포맷에 Link(17) 객체는 있으나 PDF 링크 네비게이션 지원 여부는 미확인 | 2차 글 / — |
| 표지·템플릿 | "Page style and template" 설정; 다크 모드 색 보정 옵션 | (스니펫) |
| 스티커·페인팅 | 삼성 고유 이미지 코덱 **`.spi`**(스티커·페인팅·올가미 크롭·캐시). 공개 분석 22개 문서가 있으나 변환기들은 대부분 미지원 | 2차 글 + 1차 관찰 |
| 속지 생태계 | Samsung 공식 템플릿/커뮤니티 속지 조사 불가(차단) | 자료 부족 |

---

## 7. 가져올 원리 (3~5)

1. **Wet/Dry 2층 + 프론트버퍼는 wet 층만.** androidx.ink(`InProgressStrokesView`→HWUI 핸드오프)·공식 코드랩·사례 앱(len, Mente, ReveriePaint) 모두 같은 구조. _(삭제: 디컴파일 유래 — D-020 ③: 삼성 내부 클래스 근거)_ 진행 획만 프론트버퍼, 펜업에 `commit()`, 완료 획은 **같은 UI 프레임**에 dry 층으로 옮긴 뒤 제거(깜빡임 방지). 전체화면·팬·줌은 절대 프론트버퍼로 하지 않는다.
2. **예측은 "다음 프레임 렌더 시점"을 목표로(motionprediction 공식 동작), 길이는 점진 램프.** _(삭제: 디컴파일 유래 — D-020 ③: 삼성 예측 지평·램프 수치)_ Platen: 기본 OFF(실측)로 시작하되, 켤 때는 ① `MotionPredictor.isPredictionAvailable`/라이브러리 ② 예측 길이 램프 ③ `predictionFadeOutBehavior`류 페이드 ④ 설정 화면에 노출(BRIEF 3R) — 이렇게 설계해 "밀림"을 없앤다.
3. **입력 파이프라인 규칙**: `ACTION_DOWN`에 `requestUnbufferedDispatch` → historical 샘플 전부 소비 → 압력은 1 초과 정규화·엔진은 커브 미적용(커브는 브러시 단계) → `FLAG_CANCELED`(API 33)/`ACTION_CANCEL`로 마지막 획 취소(`TOOL_TYPE_PALM`은 못 씀) → 핸들러에서 할당 금지.
4. **브러시 = 패밀리 + (색·크기·epsilon) 분리 저장, 크기는 문서 상대 단위(ink: world unit = dp).** ink의 "패밀리 enum만 저장" 권고와 같은 방향. _(삭제: 디컴파일 유래 — D-020 ③: 삼성 레벨 보간·클램프 수치)_ Procreate/ABR 임포터의 착지점은 ink `BrushFamily`(1.1.0-alpha03 공개 API) 또는 자체 모델이지만, 직렬화는 패밀리 ID 참조로.
5. **파일 컨테이너**: ZIP + 페이지별 바이너리 + 델타 압축 획(좌표 1/32, 압력 1/4096, 시간 uint16 델타) + **EOCD 뒤 요약 푸터**(압축 해제 없이 메타 읽기) + 페이지 해시. Platen 자체 포맷·Drive 동기화의 "빠른 목록/썸네일"에 그대로 유효.

## 8. 버릴 것

- _(삭제: 디컴파일 유래 — D-020 ③: 기기 모델 하드코딩 분기)_ Platen은 기기 분기 대신 설정으로 노출(원칙만 유지).
- **펜 종류 수 경쟁과 `.spi` 전용 코덱** — 1차 펜 4종(펜·만년필·연필·형광펜)+임포트로 대체.
- **압력 커브·스무딩 설정 없음**(삼성) — BRIEF는 사용자 조절이 확정.
- **형광펜 다크 모드 비가시** — BRIEF 제약(형광펜이 다크에서도 보일 것)과 정면 충돌. 혼합 모드(multiply)만 쓰지 말고 다크 테마에서 screen/alpha 전환 설계.
- `GLFrontBufferedRenderer` 직접 OpenGL 경로 — 스톡 조합은 `CanvasFrontBufferedRenderer`/ink 쪽이 유지보수 중(1.1.0-beta01 수정 다수). Flutter 선택 시에도 네이티브 wet 층은 Canvas 계열로.
- 삼성 Windows판처럼 "계정 앱 필수" 관문 — Platen Windows는 열람·편집 전용, 로그인은 Drive 동기화에만.

## 9. LOCAL_TODO 후보 (최소)

① **펜다운→첫 픽셀 지연(삼성노트 vs Platen 스파이크)** — Tab S9+ 120 Hz, 240 fps 슬로모 촬영, 펜촉 접촉 프레임~첫 잉크 프레임 수 × 4.17 ms. 각 5회, 중앙값. 보조: `adb shell dumpsys SurfaceFlinger --latency <layer>`(최근 Android에선 `--latency`가 비어 올 수 있음 — 안 되면 `dumpsys gfxinfo <pkg> framestats`). 기록 양식: `기기 / 앱·버전 / 펜 / 예측 ON·OFF / 프레임수 / ms / 비고`. 삼성노트 설정의 예측 토글 유무도 함께 기록.
② **S펜·터치 리포트 레이트** — `adb shell getevent -lt /dev/input/eventN`(S펜 EMR 노드와 터치 노드 각각), 1초 직선 획에서 `EV_SYN` 간격 중앙값 → Hz. 호버 시 `ABS_DISTANCE` 최대값과 실제 거리(자) 2~3점.
③ **형광펜 다크 모드 재현** — 삼성노트 다크 테마 + 검정/어두운 템플릿, 노랑 형광펜으로 검정 글자 위 → 스크린샷; "Adjust color in dark mode" ON/OFF 비교; PDF 내보내기 결과의 형광펜 알파 비교.
(추가 1개) 옆버튼 기본 동작·즐겨찾는 펜 슬롯 상한·펜 설정 재탭 동선 — 1분 확인 수준.

## 10. 출처 목록 (열람일 2026-10-24)

**공식 문서 (developer.android.com, 라이선스, AOSP 미러)**
- androidx ink 릴리스 — https://developer.android.com/jetpack/androidx/releases/ink
- androidx graphics 릴리스 — https://developer.android.com/jetpack/androidx/releases/graphics
- androidx input 릴리스 — https://developer.android.com/jetpack/androidx/releases/input
- 고급 스타일러스 기능(Views) — https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features
- 고급 스타일러스 기능(Compose) — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/advanced-stylus-features
- 텍스트 필드 스타일러스 — https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/stylus-input-in-text-fields
- Ink API 개요 — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/about-ink-api
- Ink 설정 — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-setup
- Ink 모듈 — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-modules
- Ink 브러시 — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-brush-apis
- Ink 획 그리기 — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-draw-stroke
- Ink 지오메트리 — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-geometry-apis
- Ink 상태·저장 — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-state-preservation
- Ink 좌표·epsilon — https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-coordinate-system
- 코드랩 Enhance stylus support — https://developer.android.com/codelabs/large-screens/advanced-stylus-support
- API diff 29 MotionEvent — https://developer.android.com/sdk/api_diff/29/changes/android.view.MotionEvent
- API diff 33 MotionEvent — https://developer.android.com/sdk/api_diff/33/changes/android.view.MotionEvent
- API diff 34 MotionEvent — https://developer.android.com/sdk/api_diff/34/changes/android.view.MotionEvent
- API diff 34 android.view 패키지 — https://developer.android.com/sdk/api_diff/34/changes/pkg_android.view.html
- API diff 29 View — https://developer.android.com/sdk/api_diff/29/changes/android.view.View
- androidx LICENSE(Apache 2.0) — https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt
- AOSP 미러 MotionEvent.java — https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/MotionEvent.java
- (열람 실패) MotionEvent·MotionPredictor·View·InProgressStrokesView·StockBrushes 레퍼런스 페이지 — 본문이 사이드바에 밀려 추출 불가, API 레벨은 "추정" 표기

**1차 관찰 (GitHub)**
- androidx ink 디렉터리 — https://github.com/androidx/androidx/tree/androidx-main/ink ; ink-authoring — https://github.com/androidx/androidx/tree/androidx-main/ink/ink-authoring ; build.gradle — https://github.com/androidx/androidx/blob/androidx-main/ink/ink-authoring/build.gradle
- InProgressStrokesView.kt — https://github.com/androidx/androidx/blob/androidx-main/ink/ink-authoring/src/androidMain/kotlin/androidx/ink/authoring/InProgressStrokesView.kt
- StockBrushes.kt — https://github.com/androidx/androidx/blob/androidx-main/ink/ink-brush/src/commonMain/kotlin/androidx/ink/brush/StockBrushes.kt
- CanvasFrontBufferedRenderer.kt — https://github.com/androidx/androidx/blob/androidx-main/graphics/graphics-core/src/main/java/androidx/graphics/lowlatency/CanvasFrontBufferedRenderer.kt
- LowLatencyCanvasView.kt — https://github.com/androidx/androidx/blob/androidx-main/graphics/graphics-core/src/main/java/androidx/graphics/lowlatency/LowLatencyCanvasView.kt
- MotionEventPredictor.java — https://github.com/androidx/androidx/blob/androidx-main/input/input-motionprediction/src/main/java/androidx/input/motionprediction/MotionEventPredictor.java ; KalmanMotionEventPredictor.java — https://github.com/androidx/androidx/blob/androidx-main/input/input-motionprediction/src/main/java/androidx/input/motionprediction/kalman/KalmanMotionEventPredictor.java
- google/ink — https://github.com/google/ink
- 코드랩 리포 — https://github.com/android/large-screen-codelabs/tree/main/advanced-stylus ; palm-rejection — https://github.com/android/large-screen-codelabs/tree/main/advanced-stylus/palm-rejection
- platform-samples UI 목록 — https://github.com/android/platform-samples/tree/main/samples/user-interface ; snippets touchinput — https://github.com/android/snippets/tree/main/compose/snippets/src/main/java/com/example/compose/snippets/touchinput
- Yanndroid/Samsung-Notes-Port — **삭제됨(참조 안 함)** — 디컴파일 smali 리포, D-020 ③
- squ1dd13/sdocx2pdf — https://github.com/squ1dd13/sdocx2pdf ; src — https://github.com/squ1dd13/sdocx2pdf/tree/master/sdocx/src
- twangodev/sdocx — https://github.com/twangodev/sdocx ; README raw — https://raw.githubusercontent.com/twangodev/sdocx/main/README.md ; crates — https://github.com/twangodev/sdocx/tree/main/crates ; docs — https://github.com/twangodev/sdocx/tree/main/docs
- bxff/samsung-notes-format — https://github.com/bxff/samsung-notes-format ; RESEARCH.md — https://github.com/bxff/samsung-notes-format/blob/main/RESEARCH.md
- DomHeadroom/sdocx-hexpatterns — https://github.com/DomHeadroom/sdocx-hexpatterns
- Downhillghost/Dietrich — https://github.com/Downhillghost/Dietrich
- lu-p/sdocx-to-txt-python-converter — https://github.com/lu-p/sdocx-to-txt-python-converter
- ashwalk33r/samsung-notes-sdoc-parser — https://github.com/ashwalk33r/samsung-notes-sdoc-parser
- FizzyHedgehog/sdocx-converter-android — https://github.com/FizzyHedgehog/sdocx-converter-android
- Ethran/notable — https://github.com/Ethran/notable ; OpenGLRenderer.kt — https://github.com/Ethran/notable/blob/main/app/src/main/java/com/ethran/notable/editor/drawing/OpenGLRenderer.kt
- lloyd-g-w/len — https://github.com/lloyd-g-w/len
- LanRhyme/ReveriePaint — https://github.com/LanRhyme/ReveriePaint
- Giovix64/Mente — https://github.com/Giovix64/Mente
- ccptr/snot — https://github.com/ccptr/snot
- nahkd123/FrontBufferDraw — https://github.com/nahkd123/FrontBufferDraw
- (코드 검색 결과만) Satwik-Miyyapuram/weaveforge, jr-mccoy/AI-Chat-Sandbox, flandyw/folio, h3c-hexin/Android_WriteBoard, OmarIhsan/Malhoutha — GitHub code search `GLFrontBufferedRenderer` / `CanvasFrontBufferedRenderer` / `"application/spd"`

**2차 글(3자 포맷 분석) — twangodev/sdocx 파일 포맷 문서(raw, 사용자 소유 .sdocx 바이트 분석)**
- https://github.com/twangodev/sdocx/blob/main/docs/reverse-engineering/README.md
- https://github.com/twangodev/sdocx/blob/main/docs/reverse-engineering/file-format.md
- https://github.com/twangodev/sdocx/blob/main/docs/reverse-engineering/integrity-findings.md
- https://github.com/twangodev/sdocx/blob/main/docs/rendering-support.md

**삭제됨(참조 안 함) — twangodev/sdocx APK·네이티브 정적 분석 문서 (D-020 ③)**
- motion-event-adapter-findings.md · pen-selection-findings.md · pen-size-findings.md · pen-opacity-findings.md · neural-model-findings.md · predictor-timing-findings.md · prediction-length-findings.md · predictor-device-policy-findings.md · fountain-v14.md · inkpen2-input-findings.md

**(스니펫) — 검색 결과 요약만, 원문 미열람**
- Samsung 지원: How to use Samsung Notes — https://www.samsung.com/in/support/mobile-devices/how-to-use-samsung-notes/ ; PDF 가져오기/내보내기 — https://www.samsung.com/us/support/answer/ANS10002404/ ; S Pen 설정 — https://www.samsung.com/us/support/answer/ANS10003217/ ; Note8 S Pen(4096) — https://www.samsung.com/sg/support/mobile-devices/what-are-the-new-improvements-for-s-pen-in-samsung-galaxy-note8
- Microsoft Store Samsung Notes — https://apps.microsoft.com/store/detail/samsung-notes/9NBLGGH43VHV
- SamMobile: Windows 전 기기 허용(2025-05) — https://www.sammobile.com/news/samsung-notes-apps-working-all-windows-devices/ ; 직선 형광펜 통합(2026-08) — https://www.sammobile.com/news/has-samsung-notes-removed-straight-highlighter-pen-tool/ ; One UI 8.5 표(2026-01) — https://www.sammobile.com/news/here-is-how-one-ui-8-5-is-reshaping-samsung-notes/
- 9to5Google Galaxy Book 제한(2023-10) — https://9to5google.com/2023/10/03/samsung-notes-windows-galaxy-book/
- GSMArena 가로 페이지·스티키(2025-08) — https://www.gsmarena.com/samsung_notes_updated_with_landscape_support_other_improvements-news-69244.php
- Wacom 2.8 ms 보도자료(2022) — https://wacom.com/en-cl/about-wacom/news-and-events/2022/1469
- AndroidCentral Tab S9 vs S8 — https://www.androidcentral.com/tablets/samsung-galaxy-tab-s9-vs-galaxy-tab-s8 ; AndroidHeadlines — https://www.androidheadlines.com/?p=1056129
- Samsung 커뮤니티: 다크 모드 — https://us.community.samsung.com/t5/Galaxy-S-Phones/Samsung-Notes-in-Dark-Mode/m-p/2011431 ; 어두운 종이 형광펜 — https://r1.community.samsung.com/t5/tablets/samsung-notes/td-p/18658456/page/2 ; 필압 — https://r1.community.samsung.com/t5/tablets/s-pen-sensitivity/td-p/2870358 , https://r1.community.samsung.com/t5/tablets/anyone-else-notice-a-change-to-the-pen-stroke-in-notes-since-a/m-p/31975559 ; 스무딩 — https://r1.community.samsung.com/t5/support/samsung-notes-app-not-responding-well-to-long-quick-strokes/m-p/30274740 ; 즐겨찾는 펜 폴더 — https://eu.community.samsung.com/t5/suggestions/favourite-pen-folders-in-notes/m-p/7028521 ; 레이저 포인터 — https://eu.community.samsung.com/t5/mobile-apps-services/neuer-laserpointer-in-der-notes-app/td-p/11850692
- Shapr3D 포럼(앱별 지연 차이) — https://discourse.shapr3d.com/t/surface-pen/1120

## 11. 정리 기록 (D-020 ③, 2026-10-24)

색인은 `feature-matrix.md` §6-2(M1~M36, 줄 번호는 정리 전 기준). 판단 규칙: "메모 명시"·"동류" = 삭제 / "경계" = 출처가 사용자 소유 `.sdocx` 파일의 바이트 분석(twangodev `file-format`·`integrity`·`rendering-support`, bxff)이면 유지하고 등급 "2차 글(3자 포맷 분석)", APK 디컴파일(Yanndroid smali)·네이티브 정적 분석(twangodev `*-findings`, `fountain-v14`) 유래면 삭제.

| # | 처리 | 근거 |
|---|---|---|
| M1 | 메모 블록 → "정리 완료" 한 줄로 교체 | 규칙 4 |
| M2 | 등급 범례에서 "디컴파일 APK 클래스/문자열 존재 여부" 삭제, "2차 글(3자 포맷 분석)" 추가 | 메모(범례 정합) |
| M3 | 준수 고지 문구 재작성(디컴파일 리포 참조 안 함) | 메모 |
| M4 | 삭제(§1 패키지 경로) | 메모 명시(클래스명) |
| M5 | 행 삭제(포트 리포 존재 근거). "갤럭시 전용"은 스토어 사실로 플랫폼 행에 유지 | 동류 |
| M6 | 행 삭제(RE 참조 APK 버전 4.4.45.37) | 동류 |
| M7~M12 | §2a 표 행 삭제(프론트버퍼 드로우패드·예측 토글·VSync 전달·신경망 예측기 수치·예측 타이밍·점진 램프) + "핵심 발견" 문장 삭제 | 메모 명시 |
| M13~M15 | §2a 표 행 삭제(InkPen2 입력 경로·MotionEvent 어댑터·기기 정책) — 삭제 행 합계 9 | 동류(네이티브 정적 분석) |
| M16 | "추정 종합" 재작성 — 하드웨어·공식 API 가능성·Shapr3D 커뮤니티 관찰만 남김 | 경계 — M7~M12에 의존하는 추론이라 디컴파일 유래 부분 삭제, 비디컴파일 부분 유지 |
| M17 | §3 행 삭제(내부 펜 레지스트리) | 동류(pen-selection) |
| M18 | §3 행 삭제(굵기 레벨 보간·클램프) | 동류(pen-size) |
| M19 | §3 행 삭제(Fountain V14 모델) | 동류(fountain-v14) |
| M20 | §3 행 삭제(펜별 불투명도) — §3 삭제 행 합계 4 | 동류(pen-opacity) |
| M21 | 부분 삭제 — "UI 없음"(스니펫) 유지, "어댑터 무변환 → 엔진 고정값" 추론 삭제 | 경계 |
| M22 | 삭제(삼성 램프·리셋 재인용). androidx.ink `predictionFadeOutBehavior` 유지 | 메모 명시 |
| M23 | 삭제(`SelectPen` 내부 펜). `.spi` 크롭(FizzyHedgehog) 유지 | 동류 |
| M24 | **유지**, 등급 "2차 글(3자 포맷 분석)" | 경계 — 객체 타입 ID는 페이지 레코드에 저장되는 값으로 `rendering-support.md`가 파일 렌더 지원 매트릭스로 정리한 것. 파일 포맷 사실이지 APK 코드 사실이 아님 |
| M25 | 삭제(`Beautify` 펜) | 동류 |
| M26 | 삭제(`LaserPen`) | 동류 |
| M27 | **유지**(M24와 동일 근거), 등급 재표기 | 경계 |
| M28 | 삭제(APK smali 문자열 대조). 실측 MIME 사실은 "실측 고유 — 유지" | 메모 명시 |
| M29 | 삭제(APK 확장자 상수) | 동류 |
| M30 | 삭제(§7-2 삼성 지평·램프 문장). Platen 설계(기본 OFF·램프·페이드·설정 노출)는 자체 설계로 유지, 목표 시점 근거를 motionprediction 공식 동작으로 교체 | 메모 명시 |
| M31 | 삭제(§7-4 삼성 레벨 보간 문장). 크기 단위 근거를 ink 공식 문서(world unit = dp)로 교체 | 동류(M18 재인용) |
| M32 | 삭제(§8 SM-P61·SM-T39). "설정으로 노출" 원칙만 유지 | 동류(M15 재인용) |
| M33 | 삭제(§8 "펜 30여 종 레지스트리" → "펜 종류 수 경쟁") | 동류(M17 재인용) |
| M34 | **유지** — LOCAL_TODO ①의 "삼성노트 설정의 예측 토글 유무 기록" | 경계 — 사용자가 자기 기기 설정 화면을 보고 기록하는 실측 항목. 디컴파일 결과를 인용하지 않으며 UI 관찰로 독립 성립 |
| M35 | 출처 목록 분리 — `file-format`·`integrity-findings`·`rendering-support`·README는 "2차 글(3자 포맷 분석)"으로 유지, `*-findings` 9건·`fountain-v14`는 "삭제됨(참조 안 함)" | 메모 명시/동류 — 문서별로 파일 분석과 APK·네이티브 분석을 분리 |
| M36 | 출처 목록 Yanndroid → "삭제됨(참조 안 함)" | 메모 명시 |

**유지 판정 재확인**: 공식 Android API(§2c 전체) · MIT/GPL 파서 기반 `.sdocx` 판정(§5, 파일 포맷 사실) · 이전 프로젝트 실측(§3 기본 펜 1.2배·테이퍼 0.58~0.85·캘리 141°/0.39, §5a) · 공개 스니펫(2.8 ms·4096 레벨·커뮤니티) — 전부 손대지 않음.

**등급 분포 변화**: "1차 관찰(디컴파일 클래스·문자열)" 소멸(0건) · twangodev 유래 "2차 글" 중 파일 포맷 문서는 "2차 글(3자 포맷 분석)"으로 재표기, 정적 분석 문서 유래는 삭제 · 새로 "자료 부족"이 된 항목: 삼성노트 내부 저지연 구조, 내부 펜 종류 수·렌더 모델·굵기 보간·불투명도, 압력 커브 내부 처리, 올가미 UI, `.sdoc` 관계, 비삼성 기기 지원 공식 근거.

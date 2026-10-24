# 스택 비교 + M0 스파이크 설계 — Platen

> 작성 2026-10-24 · 상태: **리서치 초안(승인 대기)** · 작성 환경: 클라우드 Linux 컨테이너(실기기 없음)
> 판정 축 가중치(BRIEF·발주 기준): **스타일러스 입력·저지연 렌더 > Android/Windows 렌더 일치 > 빌드 환경 > Google/PDF 라이브러리 > 패키징**

## 0. 읽기 전에 — 자료 접근 제한 고지

이 컨테이너의 아웃바운드 프록시는 아래 호스트를 **차단**(CONNECT 403)했다. 해당 호스트의 문서는 **직접 열람하지 못했고**, 가능한 경우 동일 내용의 **공식 GitHub 리포 raw 마크다운/소스**로 대체했다(열람일은 모두 2026-10-24).

- 차단: `docs.flutter.dev`, `api.flutter.dev`, `developers.google.com`, `cloud.google.com`, `support.google.com`, `source.android.com`, `learn.microsoft.com`, `jetbrains.com`/`blog.jetbrains.com`, `skia.org`, `pdfbox.apache.org`, `news.samsung.com`/`samsung.com`, `apple.com`/`support.apple.com`, `dl.acm.org`, `arxiv.org`, `researchgate.net`, 대학 호스트(dgp.toronto.edu 등), `github.com` 페이지 일부(blob/raw 리다이렉트), `api.github.com`(세션 정책: 이 리포 외 접근 불가).
- 정상: `developer.android.com`, `pub.dev`, `nuget.org`, `repo1.maven.org`, `kotlinlang.org`, `raw.githubusercontent.com`, `github.com` 이슈/릴리스 페이지(WebFetch), GitHub 코드 검색(MCP).
- **WebSearch는 이 턴의 공유 예산이 소진되어 한 번도 수행하지 못했다**(스니펫 기반 자료 없음). 따라서 지각 지연 연구(Ng 2012·Jota 2013·Annett 2014), 삼성 S펜 2.8 ms·Apple Pencil 9 ms 같은 **수치는 원문 미열람 → 등급 "추정"**으로만 표기했고, 합격선은 가능한 한 **삼성노트 실측 대비 상대값**으로 정의했다.

등급 표기: **[공식]** 공식 문서/공식 리포 · **[1차]** 공식 소스코드·이슈·메타데이터를 직접 관찰 · **[2차]** 커뮤니티 글/리포 · **[추정]** 미열람·기억 기반(로컬 확인 필요).

## 1. 현재 컨테이너 도구 인벤토리 [1차, 읽기 전용 명령]

| 항목 | 값 |
|---|---|
| OS / CPU / RAM / 디스크 | Ubuntu 24.04.5 LTS · 4 vCPU · 15 GiB(스왑 0) · 루트 여유 30 GB · **KVM 없음**(에뮬레이터 불가) |
| Java | OpenJDK **21.0.12.1** (`/usr/lib/jvm/java-21-openjdk-amd64`), `JAVA_HOME` 설정됨 |
| Gradle / Maven | Gradle **8.14.3** (`/opt/gradle`, 내장 Kotlin 2.0.21) · Maven 3.9.11 |
| Android SDK | **없음** (`sdkmanager`·`adb` 없음, `ANDROID_HOME` 미설정) → APK 빌드는 cmdline-tools 설치 승인 필요 |
| Flutter / Dart | **없음** |
| .NET / MSBuild / Mono / Wine | **없음** |
| Kotlin CLI | 없음(Gradle 플러그인으로 충분) |
| 기타 | Python 3.13.17, Node 22.22.0, Rust/cargo 있음 |

→ 어떤 후보를 고르든 **Windows 산출물(exe/MSIX)은 이 컨테이너에서 만들 수 없고**, APK도 SDK 설치 승인 후에만 가능하다(§7, §11 참조).

## 2. 후보 정의

| ID | 스택 | 비고 |
|---|---|---|
| **A** | Flutter(Impeller) — Android + Windows 단일 코드 | Dart |
| **B** | Kotlin Multiplatform + Compose Multiplatform(Android + Desktop/JVM Windows) | Kotlin |
| **C** | 네이티브 2개 — Android: Kotlin/Jetpack Compose + androidx.ink / Windows: WinUI 3(C#, Windows App SDK) | 2개 언어 |
| **D** | Avalonia UI(.NET, SkiaSharp) — Android + Windows 단일 코드 | 추가안. 근거: 양 플랫폼이 **같은 Skia 바이너리(SkiaSharp)** 로 그려 렌더 일치가 구조적으로 가장 쉽고, Android 백엔드가 펜 필압·히스토리 샘플을 이미 전달함(§3-1). 그 외 .NET MAUI(펜 축 데이터 API 없음, 자료 부족), Qt(GPL/LGPL/상용 — **라이선스 플래그**), Tauri/웹 캔버스(프론트버퍼·FLAG_CANCELED 접근 불가), React Native(플랫폼 채널 필요)는 근거 수집을 못 해 표에서 제외[추정]. |

## 3. 축별 비교표 (각 셀: 사실 + 출처)

### 3-1. 스타일러스 입력

Android 플랫폼 기준선(모든 후보의 상한) [공식/1차]:
- `MotionEvent`: `getPressure`/`AXIS_PRESSURE`, `AXIS_TILT`(=25), `AXIS_ORIENTATION`(=8), `AXIS_DISTANCE`(=24, 호버 거리), `ACTION_HOVER_*`, `BUTTON_STYLUS_PRIMARY`(=1<<5), `TOOL_TYPE_STYLUS/ERASER`, **`TOOL_TYPE_PALM`(=5)**, `FLAG_CANCELED`, `getHistorySize()`/`getHistoricalAxisValue(axis, pointerIndex, pos)`. 배칭 문서: "For efficiency, motion events with ACTION_MOVE may batch together multiple movement samples…" — AOSP 미러 소스 `core/java/android/view/MotionEvent.java` [1차] (https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/MotionEvent.java)
- 공식 가이드 "Advanced stylus features": 필압 0–1(기기에 따라 >1 → 정규화), 틸트 0~π/2 rad, 방위 ±π rad, 호버 `AXIS_DISTANCE`, 팜 리젝션 = `ACTION_CANCEL`(API 1)·`FLAG_CANCELED`(API 33) 수신 시 해당 포인터 획 제거 후 재렌더, 모션 예측 `androidx.input:input-motionprediction`, 저지연 `androidx.graphics:graphics-core`(API 29+) [공식] (https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features, Compose판 https://developer.android.com/develop/ui/compose/touch-input/stylus-input/advanced-stylus-features — Compose는 `pointerInteropFilter`로 `MotionEvent` 수신)

| | A Flutter | B KMP/Compose | C 네이티브 2개 | D Avalonia |
|---|---|---|---|---|
| 필압·틸트·방위·호버 거리 | 프레임워크 API에 있음: `PointerEvent.pressure/pressureMin/pressureMax/distance/orientation/tilt/buttons/kind` [1차 `packages/flutter/lib/src/gestures/events.dart`]. Android 임베딩 `AndroidTouchProcessor.java`가 `getPressure`, `AXIS_DISTANCE`, `AXIS_ORIENTATION`, `AXIS_TILT`(스타일러스만), `getButtonState`를 패킷에 넣음 [1차]. | Compose 공통 API: `PointerInputChange.pressure`, `type`(`PointerType.Stylus/Eraser`) [1차 androidx `PointerEvent.kt`]. 틸트·방위·거리는 Android 소스셋의 **`PointerEvent.motionEvent`** 확장으로 원본 `MotionEvent` 접근(androidx 샘플이 `pointerEvent.motionEvent?.getAxisValue(MotionEvent.AXIS_TILT)` 사용) [1차 `compose/ui/ui/samples/.../PointerInputModifierSamples.kt`]. 또는 `pointerInteropFilter` [공식]. → **플랫폼 채널 없이 전부 가능** | Android: 플랫폼 전부(위 기준선) [공식]. Windows: WinUI/UWP `PointerPointProperties.Pressure/XTilt/YTilt/Twist`, `IsBarrelButtonPressed`, `IsInverted` [공식, MicrosoftDocs/windows-dev-docs `uwp/ui-input/pen-and-stylus-interactions.md` raw] | `PointerPoint.Properties`에 `Pressure/XTilt/YTilt/Twist` 정의 [1차 `src/Avalonia.Base/Input/PointerPoint.cs`]. **Android 백엔드는 `Pressure`만 전달(`Math.Min(e.GetPressure(index),1)`), 틸트/방위/거리 매핑 없음** [1차 `src/Android/Avalonia.Android/Platform/Specific/Helpers/AndroidMotionEventsHelper.cs` grep: Tilt 0건] |
| 옆버튼 | `kPrimaryStylusButton = kSecondaryButton`, `kSecondaryStylusButton` [1차]. **이슈 #42846 "Stylus doesn't trigger PointerDownEvent"**: 2019년 개방, P2, 갤럭시 탭 S3/S4/S6 Lite/S7/S8+/S9 보고, "S펜 버튼을 먼저 누르고 화면에 대면 down/up 이벤트가 오지 않음", 마지막 댓글 2025-11-04, 수정 PR 없음 [1차] (https://github.com/flutter/flutter/issues/42846) | `motionEvent.buttonState and BUTTON_STYLUS_PRIMARY` [공식/1차] | Android 동일. Windows `IsBarrelButtonPressed` [공식] | Android: API 23+ `StylusPrimary` → 수정자 매핑 있음 [1차 같은 파일] |
| 팜 리젝션 신호 | `ACTION_CANCEL`→`PointerChange.CANCEL`만 전달 [1차 `AndroidTouchProcessor.java`]. **`FLAG_CANCELED`·`TOOL_TYPE_PALM` 처리 없음**(grep 0건) [1차]. 이슈 #57476 "Accessing Pressure from a MotionEvent"(2020, P3, 미해결)가 `MotionEvent` 노출 요구 [1차] | 원본 `MotionEvent`로 `FLAG_CANCELED`, `TOOL_TYPE_PALM`, `ACTION_CANCEL` 전부 수신 [공식/1차] | 동일(전부) | `ACTION_CANCEL`은 매핑, `FLAG_CANCELED`/`TOOL_TYPE_PALM` 처리 없음(grep) [1차] |
| 히스토리 샘플(`getHistorical*`) | **없음**: 프레임워크 `PointerEvent`에 coalesced/historical 필드 없음, Android 임베딩이 `getHistorySize()`를 호출하지 않음(코드 검색 0건) → 배치된 중간 샘플 **유실** [1차] | `PointerInputChange.historical: List<HistoricalChange>`(uptimeMillis·position; **필압 없음**) [1차 `PointerEvent.kt`] + `motionEvent.getHistoricalPressure/AxisValue`로 필압·틸트 히스토리까지 [1차] | 전부 [공식] | `IntermediatePoints`에 `GetHistoricalPressure` 포함해 전달 [1차 같은 파일] |
| 호버 이벤트 | `ACTION_HOVER_MOVE`→`PointerChange.HOVER` [1차]. 이슈 #148810 "Hovering stylus" 닫힘(2024) [1차 제목만] | `PointerEventType.Move`(눌리지 않은 상태) + `hoverable`/`motionEvent` [공식] | 전부 | Android `Move`/`Hover` 매핑 있음(일반 포인터 경로) [1차] |
| Windows 포인터(마우스/터치, 요구사항) | 데스크톱 포인터 지원(과거 이슈 #101337 "Windows 스타일러스가 터치로 인식" 닫힘) [1차 제목]. | `Modifier.pointerInput`(안정) / `onPointerEvent`(실험) / `awtEventOrNull` [공식 kotlinlang "Mouse events"]. 터치는 `combinedClickable` 설명에만 언급. **이슈 #5278(2025-04): Windows 터치스크린에서 `detectTransformGestures` 핀치 불능, 터치패드는 정상 → YouTrack으로 이관** [1차] → **Windows 터치는 스파이크 검증 항목** | WinUI 3 포인터 전부 [공식] | Win32 백엔드 포인터(상세 미확인) [추정] |
| Windows 펜(비요구, 한 줄) | 가능(위 이슈로 보아 동작) [추정] | 데스크톱 문서에 스타일러스·필압 언급 없음 → **자료 부족** | `PointerPointProperties` 전부 [공식] | `PointerPoint` 정의상 가능 [1차 정의만] |

### 3-2. 캔버스 렌더 경로 · 저지연 · 셰이더 · 일치성

Android 기준선: `androidx.graphics:graphics-core` **1.0.4 안정(2025-12-03) / 1.1.0-beta01(2026-10-07)** — `GLFrontBufferedRenderer`(1.0.0-alpha01), `CanvasFrontBufferedRenderer`(alpha04), `LowLatencyCanvasView`(1.1.0-beta01 수정 노트에 등장), `SurfaceView` 기반, API 29+, "small portions of the screen, handwriting" 용도, `requestUnbufferedDispatch()` 병용 [공식] (https://developer.android.com/jetpack/androidx/releases/graphics, https://developer.android.com/develop/ui/views/graphics/low-latency). 모션 예측 `androidx.input:input-motionprediction` **1.0.0(2025-11-19)** [공식] (https://developer.android.com/jetpack/androidx/releases/input).

| | A Flutter | B KMP/Compose | C 네이티브 2개 | D Avalonia |
|---|---|---|---|---|
| 2D 백엔드 | **Impeller**: Android API 29+ 기본(3.27~, Vulkan 미지원 기기는 OpenGL 폴백), **Windows/macOS/Linux도 3.47부터 기본** [공식 flutter/website `sites/docs/src/content/perf/impeller.md` raw]. stable 채널 CHANGELOG 최상단 = **Flutter 3.47(3.47.7)** [1차]. 단 이슈 #183495(2026-03 개방, 설계 문서)에 따르면 Windows Impeller는 **OpenGL ES 경로**이고 Vulkan 백엔드는 제안 단계; #191353 "Windows app significantly slower when impeller enabled"(2026-09, 개방) [1차] | Android: Jetpack Compose(플랫폼 `android.graphics`/HWUI). Desktop: **Skiko**(Skia for Kotlin) — `GraphicsApi { OPENGL, DIRECT3D, ANGLE, … }` [1차 `skiko/src/commonMain/.../GraphicsApi.kt`], Skiko **0.156.1(2026-10-23, Skia m156)**, "Kotlin/JVM on Windows(x86_64)" [1차 릴리스·README]. Compose Multiplatform **1.12.2 안정(2026-10-15) / 1.13.0-beta01(10-23)**, Desktop(JVM) 안정 등급 [공식 kotlinlang supported-platforms; 1차 릴리스 페이지] | Android: HWUI/Skia(Canvas) 또는 `androidx.ink` 렌더러. Windows: **Win2D 1.4.0(2026-03-16, Direct2D 래퍼, nuget는 MS EULA 링크·GitHub README는 MIT)** [1차 nuget·README] 또는 **SkiaSharp 4.153.1(2026-09-30, MIT)** [공식 nuget] | SkiaSharp(Android·Windows 동일 Skia) — Avalonia 12.1.4(2026-10-09), MIT [1차 릴리스·licence.md]. (Skia 기본 백엔드라는 서술은 README에 없음 → [추정]) |
| 저지연(프론트버퍼) | 프레임워크 내 불가. 경로는 **PlatformView(AndroidView)로 `androidx.ink`/SurfaceView 임베드**: 공식 문서상 Texture layer(기본)·Hybrid composition(스레드 병합으로 FPS 저하)·HCPP(실험, API 34+·Vulkan·Impeller 필요) [공식 `platform-views.md` raw]. **이슈 #178108(2026-06, P2, 개방)**: `InProgressStrokesView`를 AndroidView로 넣으면 진행 중 획이 펜 끝에서 벗어났다가 펜을 떼면 제자리로 "스냅" [1차]. 이슈 #175790 "3-4 frame latency on 120fps iPad"(개방) [1차 제목] | **가능**: Compose `AndroidView`로 `LowLatencyCanvasView`/`GLFrontBufferedRenderer`를 직접 호스팅하거나, `androidx.ink:ink-authoring-compose`의 `InProgressStrokes` 컴포저블 사용(ink-authoring은 `graphics-core 1.0.4`·`input-motionprediction 1.0.0`에 의존) [1차 `ink/ink-authoring/build.gradle`]. `requestUnbufferedDispatch(pointerEvent.motionEvent)` 패턴은 androidx 테스트에 존재 [1차] | 동일(가장 직접적) | 자체 SurfaceView/GL 스왑체인. 프론트버퍼 API 없음 → **자료 부족**(네이티브 인터롭 필요 추정) |
| 커스텀 셰이더·텍스처 스탬핑 | GLSL `.frag` → `FragmentProgram`, `sampler2D` 유니폼 지원, Skia·Impeller 모두 지원, OpenGLES에서 텍스처 y-플립 주의 [공식 `fragment-shaders.md` raw] | Android: `android.graphics.RuntimeShader`(AGSL, API 33+ — Tab S9+는 Android 13 이상이므로 충족)[추정: 레퍼런스 페이지 본문 미수신]. Desktop: Skiko `RuntimeEffect`(SkSL). **공통 코드에서는 expect/actual 필요**(AGSL≈SkSL이라 셰이더 소스는 공유 가능) [추정]. 스탬프 브러시는 `drawImage` 반복 또는 `ImageShader`로 공통 구현 가능 [공식 Compose Canvas API, 추정 결합] | Android: RuntimeShader/Canvas. Windows: Direct2D 커스텀 이펙트(HLSL) 또는 SkiaSharp `SKRuntimeEffect` — **엔진 2벌** | SkiaSharp `SKRuntimeEffect` 1벌 |
| 대형 캔버스(수천 획) 벤치마크 | 공개 벤치 없음 → 자료 부족. Impeller Windows 성능 이슈(#191353) 리스크 | 공개 벤치 없음 → 자료 부족(스파이크 측정) | 자료 부족 | 자료 부족 |
| **Android↔Windows 렌더 일치** | 양쪽 Impeller(3.47+)라 **같은 래스터라이저 계열**. 단 백엔드 상이(Android Vulkan/GLES vs Windows GLES)·GPU 상이로 픽셀 동일성은 미보장 [공식+추정] | 양쪽 Skia 계열이지만 **Android는 플랫폼 HWUI의 Skia, Desktop은 Skiko m156** → 버전 상이. `androidx.ink` 렌더러는 **Desktop JVM 타깃이 없음**(`ink-rendering/build.gradle`: `androidLibrary` + `ios()`만; `ink-strokes`는 `jvm()`이 "Linux x86_64·Mac arm64만" 주석) [1차] → Windows에서 ink 렌더 재현 불가. **자체 스트로크 지오메트리+Compose Canvas 렌더러를 commonMain에 두면 동일 코드 경로** 확보 가능 | 두 엔진(Android Skia vs Win2D/Direct2D 혹은 SkiaSharp) → **스펙·테스트 벡터로만 일치 보장**, 중복 구현 | **동일 Skia 바이너리(SkiaSharp) + 동일 코드** → 구조적으로 가장 유리 |

### 3-3. androidx.ink(Google Ink API) 사용 가능성 [공식/1차]

- 버전: **1.0.0 안정(2025-12-17)**, 최신 알파 **1.1.0-alpha10(2026-10-21)**. 아티팩트 `ink-authoring(-compose)`, `ink-brush(-compose)`, `ink-geometry(-compose)`, `ink-rendering`, `ink-strokes`, `ink-storage`, `ink-nativeloader`. 브러시 텍스처 레이어(`BrushPaint.TextureLayer`), 텍스처 애니메이션, protobuf 직렬화(`ink-storage`) 포함. "Inking on Android and server-side JVM (currently supported on Linux for x86_64)". (https://developer.android.com/jetpack/androidx/releases/ink)
- 타깃: `ink-strokes` = Android + jvm(Linux x64/Mac arm64) + iOS; `ink-rendering` = Android + iOS(iOS는 `MetalRenderer`, 실험) → **Windows JVM 렌더 없음** [1차 build.gradle].
- androidx.pdf 1.0.0-beta01의 `EditablePdfViewerFragment` 주석 도구가 androidx.ink 위에 구축됨 [공식].

| 후보 | 사용 가능성 | 쓰지 않을 때 대체 |
|---|---|---|
| A | PlatformView 경유만(이슈 #178108 오프셋 버그 개방) → 사실상 자체 엔진 + Dart 쪽 재렌더 필요 | Dart 자체 스트로크 엔진(Impeller Canvas), 프론트버퍼 없음 |
| B | **Android 소스셋에서 그대로 사용**(`ink-authoring-compose`). Windows는 ink 불가 → **공통 스트로크 모델/렌더러를 commonMain에 자체 구현**하고 Android는 "젖은 잉크(진행 중)"만 ink로, 확정 획은 공통 렌더러로 그리는 하이브리드 권장 | 자체 엔진(graphics-core 프론트버퍼 직접 사용) |
| C | Android 전부 사용 가능. Windows는 별도 엔진 필수 | 동일 |
| D | 불가(Java 바인딩 생성 필요 — 자료 부족) | SkiaSharp 자체 엔진 |

### 3-4. PDF (가져오기 렌더 · 주석 오버레이 · 내보내기)

| 플랫폼/후보 | 라이브러리 · 버전 · 라이선스 · 커버리지 · 출처 |
|---|---|
| Android 공통(B·C) | **플랫폼 `android.graphics.pdf.PdfRenderer`**(API 21+, 렌더 전용) [공식 레퍼런스 — 본문 미수신, 추정 보강]. **`androidx.pdf` 1.0.0-beta01(2026-08-26)**: `pdf-viewer-fragment`, 읽기/렌더 minSdk 28 백포트(alpha18), `EditablePdfViewerFragment`(펜·형광펜·지우개·되돌리기, androidx.ink 기반)·`PdfWriteHandle`로 저장, 다수 API `@ExperimentalPdfApi` [공식] (https://developer.android.com/jetpack/androidx/releases/pdf). **pdfbox-android 2.0.27.0(Maven 2023-01-02, Apache-2.0, PDFBox 2.0.27 포팅 — 2년 이상 정체)** [1차 README·maven-metadata] |
| Desktop JVM(B) | **Apache PDFBox 3.0.8(2026-07-15, Apache-2.0)** — 렌더(`PDFRenderer`)+생성/주석 쓰기 [1차 maven-metadata]. PDFium JNI 바인딩은 유지되는 Kotlin/JVM 패키지를 확인 못 함 → 자료 부족 |
| Windows C#(C·D) | **Windows.Data.Pdf** `PdfDocument`/`PdfPage.RenderToStreamAsync`(렌더 전용, OS 내장) [공식 MicrosoftDocs/winrt-api raw]. **PdfPig 0.1.16(2026-08-22, Apache-2.0)** 텍스트/구조 읽기·생성(래스터 없음) [1차 nuget·README]. **PDFtoImage 5.4.0(2026-08-16, MIT)** PDFium+SkiaSharp 래스터, WinUI 3 테스트됨, PDFium 스레드 비안전 [1차 nuget]. **QuestPDF 2026.9.1 — "Community and commercial"(연매출 $1M 미만 무료) → 라이선스 플래그** [1차 nuget] |
| Flutter(A) | **pdfrx 2.10.1(MIT, PDFium, Android/iOS/macOS/Windows/Linux/Web)**, **pdfx 2.11.0(MIT; Android=PdfRenderer, Windows=PDFium, Linux 없음)**, **printing 5.15.1(Apache-2.0, `pdf` 패키지로 생성·Windows/Linux는 PDFium 래스터)**, **syncfusion_flutter_pdfviewer 35.1.41(상용/커뮤니티 라이선스, 텍스트 마크업 주석만 언급)** [공식 pub.dev]. 주석 "쓰기"는 `pdf` 패키지로 재생성하는 방식 |
| PDFium 라이선스 | BSD 3-Clause("Copyright 2014 The PDFium Authors") [1차 chromium/pdfium LICENSE raw] |

공통 설계 메모: 1차 범위(주석 오버레이 + 내보내기)는 **"원본 PDF 페이지를 래스터 배경으로 두고 Platen 획을 위에 그린 뒤, 내보내기 시 원본 PDF에 획을 벡터/이미지로 합성"** 이 가장 라이브러리 의존이 낮다. B에서는 Android=pdfbox-android(2.0 API), Desktop=PDFBox 3.0 → 패키지명·API가 달라 expect/actual 래퍼 필요 [추정].

### 3-5. Google 로그인 + Drive

- Android(B·C 공통): **Credential Manager**(`androidx.credentials` **1.6.0 안정 2026-04-08**, `credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid`), `setServerClientId(WEB_CLIENT_ID)`(**웹 클라이언트 ID** 필요), API 19+ [공식] (https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation). **Drive 스코프 토큰 발급(AuthorizationClient 등)은 developers.google.com 차단으로 미확인 → 자료 부족.** 대안: AppAuth-Android(RFC 8252 네이티브 앱 OAuth, Custom Tabs) — 최신 **0.11.1(2021-12-22, 정체)** [1차 maven-metadata·README].
- 데스크톱 루프백 OAuth 공식 문서(developers.google.com/identity/protocols/oauth2/native-app)·**테스트 모드 리프레시 토큰 7일 만료/테스트 사용자 100명 제한**: **미열람 → [추정]**, LOCAL_TODO로 확인.

| 후보 | Android | Windows 데스크톱(루프백+PKCE) | 출처 |
|---|---|---|---|
| A | `google_sign_in` **7.2.0(BSD-3)** 플랫폼표 = Android/iOS/macOS/Web — **Windows·Linux 없음** [공식 pub.dev] | **`googleapis_auth` 2.3.5(BSD-3)** "Installed application / Desktop" `clientViaUserConsent`가 localhost로 리다이렉트 수신; 단 Android/iOS 클라이언트 ID와 함께 Flutter 앱에서 쓰지 말라는 경고 [공식 pub.dev]. `googleapis` 17.0.0에 `drive/v3` 포함 [공식] | https://pub.dev/packages/google_sign_in · https://pub.dev/packages/googleapis_auth |
| B | Credential Manager(위). Drive 스코프: 자료 부족(AuthorizationClient) / AppAuth | **google-oauth-client-jetty 1.39.0(2025-03, "maintenance mode only")** `LocalServerReceiver`(JDK 내장 `com.sun.net.httpserver`로 루프백) [1차 소스·README] + **google-api-services-drive v3-rev20261011-2.0.0(2026-10-18)** [1차 maven-metadata]. 라이선스: Apache-2.0 표준이나 README에서 미확인 → [추정] | https://raw.githubusercontent.com/googleapis/google-oauth-java-client/main/... |
| C | 위와 동일 | **Google.Apis.Auth 1.77.0(2026-09-28, Apache-2.0)** `LocalServerCodeReceiver`(127.0.0.1 권장, 실패 시 localhost) [1차 nuget·소스] + **Google.Apis.Drive.v3 1.77.0.4301(2026-10-20, Apache-2.0)** [공식 nuget] | nuget.org |
| D | .NET Android에서 Credential Manager 바인딩 → 자료 부족 | C와 동일(.NET) | — |

### 3-6. 패키징

| | A | B | C | D |
|---|---|---|---|---|
| APK 사이드로드 | Gradle 서명(`keytool` 업로드 키 생성 안내) [공식 `deployment/android.md` raw]. 공통: 동일 인증서로 계속 서명, 디버그 인증서는 배포 부적합, 키 분실 시 업데이트 불가 [공식 app-signing]. **자체 서명키로 사이드로드 가능**(Play 요건은 비적용) | 동일(AGP `signingConfigs`) | 동일 | .NET Android 서명(자료 부족) |
| Windows exe/MSIX | `flutter build windows` → `build\windows\runner\Release`; **`msix` pub 3.18.0(MIT)**: 기본 **자체 서명 테스트 인증서로 서명하고 설치 시 인증서를 머신에 설치**(`install_certificate: false` 가능) [공식 pub.dev·website raw] | jpackage(JDK 17+) → **`.exe`/`.msi`**, ProGuard 내장, Windows 서명은 문서에 없음 [공식 kotlinlang compose-native-distribution] | **MSIX는 서명 필수 + 기기에서 신뢰되는 인증서 필요**; 개발용 자체 서명 무료, Azure Artifact Signing ~$10/월, OV $300–500/년 [공식 msix-docs raw]. Windows 11 **미서명 MSIX**는 `Add-AppxPackage -AllowUnsigned`+특수 OID, "테스트용, 널리 배포 금지" [공식]. **비패키지 WinUI 3**: `WindowsPackageType=None`, 자체 포함 시 `PublishSingleFile`(WinAppSDK 1.5+) [공식] | .NET `dotnet publish` exe(자료 부족) |
| SmartScreen | 미서명/자체 서명 → "Windows protected your PC", 사용자가 "Run anyway" 선택; 서명해도 평판 쌓일 때까지 경고 [공식 windows-dev-docs `smartscreen-reputation.md` raw] — **모든 후보 공통**(사용자 1인 사이드로드라 수용 가능) | 동일 | 동일 | 동일 |
| 앱 크기 | 자료 부족(측정 항목) | JDK 번들(jlink 모듈 최소화) → 수십 MB대 [추정] | 자료 부족 | 자료 부족 |

### 3-7. 빌드 환경

GitHub Actions 러너 사실 [1차 actions/runner-images README raw]:
- `ubuntu-24.04`: Android SDK(Build-tools 34–37, NDK 27), Java 8/11/**17(default)**/21/25, Gradle 9.8.1, Kotlin 2.4.21, .NET 8/9/10 SDK. Flutter 미설치(`subosito/flutter-action`이 설치, Linux/Windows/macOS 지원).
- `windows-2022`: Visual Studio Enterprise 2022 17.14, **WiX 3.14.1**, .NET 8/9/10, Android SDK, Windows SDK.

| | A | B | C | D |
|---|---|---|---|---|
| APK (Linux 러너/이 컨테이너) | 가능(러너). 컨테이너: Flutter SDK + Android SDK 설치 승인 필요 | 가능(러너: ubuntu assembleDebug — JetBrains `Kotlin/kmp-production-sample` 워크플로 [1차]). 컨테이너: Android cmdline-tools 설치 승인 필요(JDK 21·Gradle 8.14.3은 있음) | Android 부분 B와 동일 | 러너 가능(.NET workload). 컨테이너: .NET 설치 필요 |
| Windows 산출물 | **Windows 러너 필요**(VS "Desktop development with C++" 워크로드 요구 [공식 windows/setup.md raw]). 예: `leoafarias/sidekick` `windows-latest`+`flutter build windows` [2차] | **Windows 러너 필요**: "Cross-compilation is currently not supported … build the specific format using the corresponding compatible OS only" [공식 kotlinlang]. 예: `kmp-production-sample` `windows-latest` → `packageMsi` [1차 공식 샘플] | **Windows 러너 필요**(MSBuild/WinAppSDK). 커뮤니티 예: `LanceMcCarthy/DevOpsExamples` `main_build-winui.yml` [2차 경로만] | .NET은 `-r win-x64` 크로스 퍼블리시가 원칙적으로 가능(네이티브 빌드 단계 없음) → **Linux 러너/이 컨테이너에서 Windows exe 가능성** [추정, 문서 차단] |
| 이 컨테이너 요약 | APK만(설치 후) | APK만(설치 후) | APK만(설치 후) | APK+exe 가능성(설치 후, 추정) |

### 3-8. 브러시 파서 구현 언어 적합성

포맷 사실:
- Procreate `.brush` = ZIP 컨테이너(`Brush.archive`(bplist/NSKeyedArchiver), `Shape.png`, `Grain.png`, `QuickLook/Thumbnail.png`) — **커뮤니티 역공학 서술**, Procreate 5.4에서 스크립트가 깨졌다는 경고 포함 [2차 `mrlfarano/myBrush/docs/procreate-format-feasibility.md`; GitHub 코드 검색 92건 중 다수 동일 구조]. `.brushset` = `.brush` 묶음 [2차]. 공식 스펙 없음 → **리스크 1순위 유지**.
- Photoshop `.abr`: v1/v2(고정 헤더) · v6+(`8BIM` 섹션 `samp`/`desc`/`patt`/`phry`) — 공개 구현: **ag-psd `src/abr.ts`(MIT, TypeScript)** `readAbr`이 sampled brush·dynamics(jitter/minimum/control)·패턴 파싱 [1차]; **GIMP `app/core/gimpbrush-load.c`(GPL-3.0+)**, **Krita `libs/brush/kis_abr_brush_collection.cpp`(GPL-2.0+)** → **GPL 플래그: 포맷 이해용 열람만, 코드 이식 금지** [1차 헤더].
- NSKeyedArchiver 해제($objects/$top/UID 참조 복원)는 어느 언어에서도 **plist 파서 위에 직접 구현**해야 한다 [추정, 아래 라이브러리 설명 기준].

| 요소 | A Dart | B Kotlin(Android+JVM) | C Kotlin + C# | D C# |
|---|---|---|---|---|
| zip | `archive` 4.5.2(MIT) [공식 pub.dev] | `java.util.zip`(JDK/Android 내장) | + .NET `System.IO.Compression` | .NET 내장 |
| bplist | `plist_parser` 0.3.0(MIT, XML+Binary, NSKeyedArchiver 언급 없음) [공식 pub.dev] | **dd-plist 1.30(Maven 2026-07-24; GitHub v1.31.0 2026-10-07, MIT)** Cocoa Binary v0 지원 [1차] | dd-plist + **plist-cil 2.3.1(2025-08-07, MIT)** [1차 nuget] | plist-cil |
| .abr | 자체 구현(ag-psd 참고) | 자체 구현(ag-psd 참고) — 1벌 | **2벌**(Kotlin + C#) 또는 공통 포맷(변환 결과 JSON+PNG)을 Android에서 만들어 Drive로 동기화 | 1벌 |
| PNG 디코드 | `image` 4.12.0(MIT, PNG r/w·PSD 읽기) [공식] | Android `BitmapFactory` / JVM `ImageIO`(색 관리 차이 가능) [추정] | + SkiaSharp/`Windows.Graphics.Imaging` | SkiaSharp |
| 언어 적합성 소결 | 가능. 바이너리 파싱 라이브러리 생태계 얇음 | **가장 유리**: 성숙한 bplist 라이브러리, 공통 모듈 1벌, JVM 테스트 용이 | 2벌 유지 비용 | 가능 |

### 3-9. 리스크 · 성숙도 · 커뮤니티

| | A | B | C | D |
|---|---|---|---|---|
| 핵심 리스크 | (1) 히스토리 샘플 유실·`FLAG_CANCELED`/`TOOL_TYPE_PALM` 미노출(엔진 수정 전까지 구조적) (2) S펜 버튼 이슈 #42846 6년째 개방 (3) 저지연은 PlatformView 의존 + #178108 (4) Windows Impeller(GLES) 성능 이슈 #191353 [1차] | (1) Compose Desktop **Windows 터치** 제스처 결함 이력(#5278→YouTrack) (2) ink 렌더러 Windows 불가 → 자체 렌더러 필수 (3) Desktop 입력 API 일부 실험(`onPointerEvent`) (4) Android/Desktop Skia 버전 상이 | (1) 코드 중복(스트로크 엔진·브러시 파서·동기화 2벌) (2) 렌더 일치는 스펙/테스트 벡터로만 보장 (3) MSIX 서명·WinAppSDK 1.8 vs 2.5 분기 [1차 릴리스] | (1) Android 틸트 미전달·팜 플래그 없음 (2) 프론트버퍼 경로 자료 부족 (3) Android용 Google 바인딩 자료 부족 (4) 커뮤니티가 Android 쪽에서 작음 |
| 성숙도 | Flutter 3.47 stable, Impeller 전 플랫폼 기본 [공식] | CMP 1.12.2 stable, Desktop 안정 등급, Kotlin 2.4.21 [공식] | Android: 최성숙. Windows App SDK 1.8.12(09-24)·2.5.1(09-16) 병행 [1차] | Avalonia 12.1.4(2026-10-09) [1차] |
| 커뮤니티 규모 | 매우 큼 | 큼(Android) / 중간(Desktop) | 큼 / 큼 | 중간 |

## 4. 점수표 · 추천 · 대안

가중치: 입력+저지연 **40** · 렌더 일치 **20** · 빌드 환경 **15** · Google/PDF **15** · 패키징 **10** (5점 만점 → 가중 환산). 점수는 위 표의 사실에 근거한 **필자 판정[추정]**.

| 축(가중) | A | B | C | D |
|---|---|---|---|---|
| 스타일러스 입력·저지연(40) | 2.0 → 16 | **5.0 → 40** | 5.0 → 40 | 2.5 → 20 |
| 렌더 일치(20) | 4.0 → 16 | 3.5 → 14 | 2.0 → 8 | 4.5 → 18 |
| 빌드 환경(15) | 3.0 → 9 | 3.0 → 9 | 3.0 → 9 | 3.5 → 10.5 |
| Google/PDF(15) | 4.0 → 12 | 3.0 → 9 | 4.0 → 12 | 3.0 → 9 |
| 패키징(10) | 4.0 → 8 | 4.0 → 8 | 3.5 → 7 | 3.0 → 6 |
| **합계(100)** | **61** | **80** | **76** | **63.5** |

### 추천 1 — **B. Kotlin Multiplatform + Compose Multiplatform**
근거(가중 순):
1. **입력·저지연(최대 가중)에서 플랫폼 상한을 플랫폼 채널 없이 그대로 쓴다**: `PointerEvent.motionEvent`로 필압·틸트·방위·호버·옆버튼·`FLAG_CANCELED`·`TOOL_TYPE_PALM`·히스토리 샘플 전부, `AndroidView`로 `graphics-core` 프론트버퍼 또는 `ink-authoring-compose` 직접 사용. A는 히스토리 샘플·팜 플래그가 구조적으로 막혀 있고 S펜 버튼 이슈가 열려 있다.
2. **렌더 일치**: ink 렌더러는 Windows에 없으므로 어차피 자체 렌더러가 필요한데, B는 그 렌더러를 **commonMain 1벌**(Compose `Canvas` + `ImageShader`/`RuntimeShader`·`RuntimeEffect` expect/actual)로 양 플랫폼에서 돌릴 수 있다. C는 2벌이다.
3. **브러시 파서·동기화·파일 포맷·UI**를 Kotlin 1벌로 공유 → 추정 **코드 공유율 60–75%**(플랫폼별: 잉크 입력 레이어·프론트버퍼·Google 인증·PDF 백엔드 래퍼·패키징). 참고: A ≈ 85–90%(단 핵심 축 미달), C ≈ 5–10%(스펙·테스트 벡터만), D ≈ 85%.
4. 빌드: APK는 Linux(러너/컨테이너+SDK 설치), Windows는 `windows-latest` + `packageMsi/packageExe` — 공식 샘플 워크플로 존재.

설계 전제(스파이크에서 확인): Android는 **"젖은 잉크 = androidx.ink(또는 graphics-core 자체 프론트버퍼) / 마른 잉크 = 공통 렌더러"** 하이브리드. ink의 `BrushFamily`를 쓰면 마른 잉크 재렌더 시 모양이 달라질 수 있으므로, **진행 중 획도 공통 스트로크 지오메트리 + 공통 셰이더로 그리는 자체 프론트버퍼 경로**를 1순위로 측정하고, ink는 대조군으로 둔다.

### 대안 1 — **C. 네이티브 2개(Kotlin/Compose + androidx.ink ↔ WinUI 3 C# + SkiaSharp)**
- 채택 조건: M0에서 **Compose Desktop Windows 터치/마우스 편집(올가미·페이지 관리) 또는 1만 획 렌더가 합격선 미달**일 때. 입력·저지연 점수는 B와 동일하고, Windows 쪽은 가장 성숙한 툴킷이다.
- 완화책: Windows 렌더는 Win2D 대신 **SkiaSharp(MIT)** 로 두고, 스트로크 지오메트리를 **언어 중립 스펙 + 골든 테스트 벡터(JSON→PNG)** 로 고정해 두 엔진의 일치를 자동 검증.
- 비용: 브러시 파서·동기화·포맷 2벌(또는 Android에서 변환 후 결과만 동기화).

(A Flutter는 Impeller가 3.47부터 양 플랫폼 기본이라 렌더 일치 면에서 매력적이지만, 최대 가중 축의 결함이 엔진 수준이라 1차 후보에서 제외. D Avalonia는 Android 틸트·팜 플래그·프론트버퍼 근거 부족으로 제외하되, "Windows exe를 Linux에서 크로스 빌드"가 필요해지면 재검토.)

## 5. M0 스파이크 설계

### 5-1. 목적
B(권장) 하나로 **Android 입력·지연 상한**과 **Windows(Compose Desktop) 편집·렌더 하한**을 수치로 확인하고, 불합격 시 C로 전환한다. 실기기 측정은 **사용자 로컬 숙제**(LOCAL_TODO).

### 5-2. 스파이크 앱 최소 기능
Android(APK) — 모듈 `spike-android`:
1. 전체화면 캔버스. **펜 입력 로그**(CSV): `eventTime`, `action`, `toolType`, x, y, pressure, tilt, orientation, distance, buttonState, `flags&FLAG_CANCELED`, `historySize`, 히스토리 각 샘플(시각·좌표·필압).
2. **폴리라인 렌더**(필압→굵기 선형 매핑, 스무딩 없음).
3. 토글 **프론트버퍼 on/off**: on = `AndroidView{LowLatencyCanvasView 또는 GLFrontBufferedRenderer}` + `requestUnbufferedDispatch`; off = 일반 Compose `Canvas` 재구성 경로.
4. 토글 **예측 on/off**: `MotionEventPredictor.record/predict` (예측점은 다음 실제 샘플 도착 시 폐기).
5. 토글 **ink 대조군**: `InProgressStrokes`(ink-authoring-compose) 경로.
6. 버튼 **합성 1만 획 로드**(고정 시드 난수, 평균 30 점/획, 필압 변동 포함) → 로드 후 팬/줌 중 프레임 통계 수집.
7. 화면 모서리 **입력 마커**: 펜 다운 시 좌상단 50×50 px 사각형을 즉시 검게 토글(슬로모 촬영 기준점).
8. 통계 오버레이: 현재 입력 Hz(최근 1 s 샘플 수, 히스토리 포함/미포함 두 값), 필압 고유값 개수(세션 누적), 틸트/방위 수신 여부, 호버 수신 여부, `FLAG_CANCELED`/`TOOL_TYPE_PALM` 발생 횟수, Choreographer 기반 프레임 시간 p50/p95, 드롭 프레임 비율.
9. 내부 타임스탬프 로그: `MotionEvent.eventTime` → 핸들러 진입 → 렌더 제출(프론트버퍼 `renderFrontBufferedLayer` 호출 시각) → `Window.OnFrameMetricsAvailableListener`(일반 경로) 지표.

Windows(Compose Desktop, exe) — 모듈 `spike-desktop`:
1. Android 로그로 저장한 획 파일을 읽어 **공통 렌더러**로 표시(동일 코드 경로 검증: 같은 입력 → 양 플랫폼 PNG 덤프 → 픽셀 diff).
2. 마우스/터치 **올가미 선택·이동·삭제**, 페이지 넘김, 텍스트 박스 1개, 이미지 1개 삽입.
3. 합성 1만 획 로드 후 팬/줌 프레임 시간 측정(Direct3D 백엔드 기본, OpenGL/Software 비교 토글).
4. 셰이더 경로: 스탬프 브러시 1종(원형 그레인 PNG)을 `ImageShader`/`RuntimeEffect`로 그려 Android `RuntimeShader` 결과와 비교.

### 5-3. 측정 항목 · 방법 · 합격선

| # | 항목 | 측정 방법(로컬) | 합격선 | 근거 |
|---|---|---|---|---|
| M1 | **펜 다운→첫 픽셀 지연(ms)** | 240 fps 슬로모(프레임당 4.17 ms)로 펜촉과 화면을 함께 촬영, 펜촉 접촉 프레임→잉크/마커 첫 변화 프레임 계수(≥10회 중앙값). 보조: `adb shell dumpsys gfxinfo <pkg> framestats`(일반 경로만), `adb shell dumpsys SurfaceFlinger --latency <layer>`[추정: source.android.com 미열람], 앱 내 타임스탬프 | **상대**: 동일 방법으로 측정한 **삼성노트 중앙값 + 8 ms(2 슬로모 프레임) 이내**(프론트버퍼 on). **절대 가드레일**: ≤ 30 ms(on), 예측 on 시 ≤ 20 ms 체감치 | BRIEF "지연=삼성노트". 절대값은 미열람 문헌(Ng 2012: 1 ms 프로토타입 대비 10 ms 차이도 지각; Jota 2013: 드래그 ~2 ms 지각 가능; Annett 2014: 잉킹 ~40–50 ms 수준에서 지각 보고; 삼성 S펜 "2.8 ms", Apple Pencil "9 ms" 마케팅치)에 기댄 **[추정]** — 원문 확인은 LOCAL_TODO |
| M2 | 입력 샘플 레이트(Hz)·히스토리 샘플 수 | 앱 로그: 1 s당 `ACTION_MOVE` 수와 `historySize` 합계. 교차: `adb shell getevent -lt /dev/input/eventN`의 `SYN_REPORT` 간격[추정] | 히스토리 포함 유효 샘플 ≥ **240 Hz**(S펜 디지타이저 보고율[추정]); 히스토리 미포함(프레임 동기) ≈ 120 Hz | 히스토리 접근이 가능해야 획 품질(스타노트 수준 매끈함)이 나옴 |
| M3 | 필압 해상도 | 세션 누적 고유 pressure 값 개수, 최소/최대 | ≥ **1,024 단계** 관찰(4,096 단계 디지타이저[추정]), 최대값 >1 발생 여부 기록 | 필압 커브 사용자 조절(D-012) 전제 |
| M4 | 틸트/방위 수신 | 로그에 `AXIS_TILT`·`AXIS_ORIENTATION` 비상수 값 | 둘 다 수신(틸트 0~~1.2 rad 범위 변화) | 만년필·연필 질감 |
| M5 | 호버 수신 | `ACTION_HOVER_*` + `AXIS_DISTANCE` 변화, 호버 중 손가락 터치 무시 가능 여부 | 수신 + 호버 중 손가락 이벤트 구분 가능 | 입력 정책(D-012) |
| M6 | 팜 리젝션 | 펜 필기 중 손바닥 접촉 20회: `TOOL_TYPE_PALM`/`FLAG_CANCELED`/`ACTION_CANCEL` 수신 횟수, 오획 잔존 수 | 오획 잔존 **0**(취소 신호 수신 후 롤백 성공) | 플랫폼 신호 접근 검증 |
| M7 | 120 Hz 프레임 유지율(1만 획) | 합성 1만 획 로드 후 10 s 팬/줌: Choreographer 프레임 시간 분포; `dumpsys gfxinfo framestats` 교차 | p95 ≤ 8.3 ms, 드롭 ≤ 5 %(타일 캐시 허용) | Tab S9+ 120 Hz[추정] |
| M8 | 예측 on/off 비교 | M1 반복, 오버슈트(예측점이 실제 궤적에서 벗어난 최대 px) | 체감 지연 감소 ≥ 1 슬로모 프레임, 오버슈트 ≤ 2 px | androidx.input 1.0.0 |
| M9 | 프론트버퍼 on/off 비교 | M1 반복 | on이 off보다 ≥ 1 프레임(8.3 ms) 빠름 | graphics-core 문서 |
| M10 | Windows 편집·렌더 | 터치/마우스 올가미 성공률, 1만 획 팬/줌 p95 | 터치 제스처 100 % 동작(핀치 포함), p95 ≤ 16.7 ms | 이슈 #5278 검증 |
| M11 | 렌더 일치 | 동일 획 파일 → Android/Windows PNG 덤프, 픽셀 diff | 알파 가중 차이 ≤ 2 % 픽셀, 구조적 차이 0 | D-009 |

### 5-4. 판정 양식

| 항목 | 측정값(Android, FB on/off, 예측 on/off) | 삼성노트 기준값 | 합격선 | 판정 | 증거(영상/CSV 경로) |
|---|---|---|---|---|---|
| M1 지연 | | | 삼성+8 ms, ≤30 ms | | |
| M2 Hz/히스토리 | | — | ≥240 Hz | | |
| M3 필압 단계 | | — | ≥1,024 | | |
| M4 틸트/방위 | | — | 수신 | | |
| M5 호버 | | — | 수신+구분 | | |
| M6 팜 | | — | 오획 0 | | |
| M7 120 Hz | | — | p95≤8.3 ms | | |
| M8 예측 | | — | ≥1 프레임 | | |
| M9 FB | | — | ≥1 프레임 | | |
| M10 Windows | | — | 100 %, p95≤16.7 ms | | |
| M11 일치 | | — | ≤2 % | | |

전환 규칙: M1·M2·M6 중 하나라도 불합격 → 스택 재검토(측정 오류 배제 후). M10 불합격 → **C 대안**으로 전환(Android 코드는 유지). M11 불합격 → 공통 렌더러 설계 수정(스택 유지).

## 6. 의존성 후보 목록(승인용, 권장안 B 기준)

| 이름 | 버전 | 라이선스 | 용도 | 출처 등급 |
|---|---|---|---|---|
| Kotlin | 2.4.21 | Apache-2.0 | 언어 | 공식 |
| Compose Multiplatform(Gradle 플러그인) | 1.12.2 | Apache-2.0 | UI(Android+Desktop) | 1차 릴리스 |
| androidx.compose.ui | 1.12.1 | Apache-2.0 | Android UI | 공식 |
| androidx.graphics:graphics-core | 1.0.4(안정) / 1.1.0-beta01 | Apache-2.0 | 프론트버퍼 저지연 | 공식 |
| androidx.input:input-motionprediction | 1.0.0 | Apache-2.0 | 모션 예측 | 공식 |
| androidx.ink:* (authoring-compose, brush, geometry, rendering, strokes, storage, nativeloader) | 1.0.0 | Apache-2.0 | 스파이크 대조군·Android 젖은 잉크 후보 | 공식 |
| androidx.credentials + credentials-play-services-auth | 1.6.0 | Apache-2.0 | Google 로그인(Android) | 공식 |
| com.google.android.libraries.identity.googleid:googleid | 최신(미확인) | Apache-2.0[추정] | Google ID 토큰 | 공식(버전 자리표시) |
| google-oauth-client-jetty | 1.39.0 | Apache-2.0[추정] | Desktop 루프백 OAuth(유지보수 모드 플래그) | 1차 |
| google-api-services-drive | v3-rev20261011-2.0.0 | Apache-2.0[추정] | Drive API(JVM; Android에서도 사용 검토) | 1차 |
| Apache PDFBox | 3.0.8 | Apache-2.0 | Desktop PDF 렌더·내보내기 | 1차 |
| pdfbox-android | 2.0.27.0 | Apache-2.0 | Android PDF 내보내기(정체 플래그) | 1차 |
| androidx.pdf:pdf-viewer-fragment | 1.0.0-beta01 | Apache-2.0 | Android PDF 렌더(대안: 플랫폼 PdfRenderer) | 공식 |
| dd-plist | 1.30(Maven) | MIT | Procreate bplist 파싱 | 1차 |
| Skiko(CMP에 포함) | 0.156.1 | Apache-2.0 | Desktop Skia | 1차 |
| (대안 C용) SkiaSharp | 4.153.1 | MIT | Windows 렌더 | 공식 |
| (대안 C용) Google.Apis.Auth / Google.Apis.Drive.v3 | 1.77.0 / 1.77.0.4301 | Apache-2.0 | Windows OAuth/Drive | 공식 |
| (대안 C용) PDFtoImage / PdfPig | 5.4.0 / 0.1.16 | MIT / Apache-2.0 | Windows PDF | 1차 |
| (참고 전용, 의존 아님) ag-psd `abr.ts` | — | MIT | .abr 포맷 참고 | 1차 |
| (열람만, 코드 사용 금지) GIMP `gimpbrush-load.c`, Krita `kis_abr_brush_collection.cpp` | — | **GPL-3.0+ / GPL-2.0+** | .abr 포맷 이해 | 1차 |
| (플래그) QuestPDF | 2026.9.1 | Community/상용 | 사용 비권장 | 1차 |

컨테이너 설치 승인 필요 항목: Android cmdline-tools + platform 35/36 + build-tools(APK 빌드), 선택적으로 Flutter SDK(A 비교 스파이크 시).

## 7. LOCAL_TODO 후보

1. 갤럭시 탭 S9+에서 스파이크 APK 설치 후 §5-3 M1–M9 측정(240 fps 슬로모 영상, CSV, `dumpsys gfxinfo framestats`, `getevent -lt`), 삼성노트 동일 조건 M1 측정.
2. Windows 11 노트북(터치 가능 여부 확인)에서 스파이크 exe로 M10·M11 측정, SmartScreen 경고 화면 캡처.
3. Google Cloud 프로젝트·OAuth 동의 화면(테스트 모드) 생성, **웹 클라이언트 ID**(Credential Manager용)·**데스크톱 클라이언트 ID**(루프백) 발급, Android 클라이언트에 사이드로드 서명키 SHA-1 등록; 테스트 모드 리프레시 토큰 만료(7일 추정)·테스트 사용자 제한을 공식 문서에서 확인.
4. 차단된 1차 자료 로컬 확인: Ng et al. UIST 2012 / Jota et al. CHI 2013 / Annett et al. GI 2014 원문 수치, 삼성 S펜 지연 공식 수치(Tab S9 뉴스룸), Apple Pencil 지연 수치, `dumpsys SurfaceFlinger --latency`·`getevent` 공식 문서, Android `RuntimeShader`·`PdfRenderer` 레퍼런스 본문.
5. 컨테이너에 Android SDK 설치 승인 → `./gradlew :spike-android:assembleDebug` 가능 여부 확인; Windows 산출물은 GitHub Actions `windows-latest` 워크플로로.
6. `platen` 패키지명(Android applicationId `…platen`)·Windows 패키지 식별자 충돌 확인(D-002).
7. 사용자 소유 Procreate `.brush/.brushset`·`.abr` 샘플 파일을 로컬에서 확보해 파서 테스트 벡터로(타사 에셋 동봉 금지 준수).

## 8. 출처 목록 (열람일 2026-10-24)

Android/AndroidX [공식]
- https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features
- https://developer.android.com/develop/ui/compose/touch-input/stylus-input/advanced-stylus-features
- https://developer.android.com/develop/ui/views/touch-and-input/stylus-input (허브)
- https://developer.android.com/develop/ui/views/graphics/low-latency
- https://developer.android.com/jetpack/androidx/releases/graphics
- https://developer.android.com/jetpack/androidx/releases/input
- https://developer.android.com/jetpack/androidx/releases/ink
- https://developer.android.com/jetpack/androidx/releases/pdf
- https://developer.android.com/jetpack/androidx/releases/compose
- https://developer.android.com/jetpack/androidx/releases/credentials
- https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
- https://developer.android.com/studio/publish/app-signing
- (본문 미수신·내비게이션만) https://developer.android.com/reference/android/view/MotionEvent, …/android/graphics/pdf/PdfRenderer, …/android/graphics/RuntimeShader, …/kotlin/androidx/compose/ui/input/pointer/PointerInputChange

소스코드·메타데이터 [1차]
- https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/main/core/java/android/view/MotionEvent.java
- https://raw.githubusercontent.com/flutter/flutter/master/packages/flutter/lib/src/gestures/events.dart
- https://raw.githubusercontent.com/flutter/flutter/master/engine/src/flutter/shell/platform/android/io/flutter/embedding/android/AndroidTouchProcessor.java
- https://raw.githubusercontent.com/flutter/flutter/stable/CHANGELOG.md
- https://raw.githubusercontent.com/flutter/website/main/sites/docs/src/content/perf/impeller.md
- https://raw.githubusercontent.com/flutter/website/main/sites/docs/src/content/ui/design/graphics/fragment-shaders.md
- https://raw.githubusercontent.com/flutter/website/main/sites/docs/src/content/platform-integration/windows/setup.md · …/windows/building.md · …/android/platform-views.md · …/deployment/windows.md · …/deployment/android.md
- https://raw.githubusercontent.com/androidx/androidx/androidx-main/ink/ink-strokes/build.gradle · …/ink/ink-rendering/build.gradle · …/ink/ink-authoring/build.gradle
- androidx 코드 검색: `compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/input/pointer/PointerEvent.kt`(HistoricalChange, PointerType.Stylus), `compose/ui/ui/samples/.../PointerInputModifierSamples.kt`(motionEvent·AXIS_TILT), `compose/ui/ui/src/androidDeviceTest/.../RequestUnbufferedDispatchTest.kt`
- JetBrains/skiko 코드 검색 `skiko/src/commonMain/kotlin/org/jetbrains/skiko/GraphicsApi.kt`; https://raw.githubusercontent.com/JetBrains/skiko/master/README.md; https://github.com/JetBrains/skiko/releases
- https://github.com/JetBrains/compose-multiplatform/releases; https://raw.githubusercontent.com/JetBrains/compose-multiplatform/master/README.md; https://repo1.maven.org/maven2/org/jetbrains/compose/compose-gradle-plugin/maven-metadata.xml
- https://raw.githubusercontent.com/AvaloniaUI/Avalonia/master/src/Avalonia.Base/Input/PointerPoint.cs · …/src/Android/Avalonia.Android/Platform/Specific/Helpers/AndroidMotionEventsHelper.cs · …/licence.md · …/readme.md; https://github.com/AvaloniaUI/Avalonia/releases
- https://raw.githubusercontent.com/googleapis/google-oauth-java-client/main/google-oauth-client-jetty/src/main/java/com/google/api/client/extensions/jetty/auth/oauth2/LocalServerReceiver.java · …/README.md; https://repo1.maven.org/maven2/com/google/oauth-client/google-oauth-client-jetty/maven-metadata.xml; https://repo1.maven.org/maven2/com/google/apis/google-api-services-drive/maven-metadata.xml
- https://raw.githubusercontent.com/googleapis/google-api-dotnet-client/main/Src/Support/Google.Apis.Auth/OAuth2/LocalServerCodeReceiver.cs
- https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/maven-metadata.xml; https://repo1.maven.org/maven2/com/tom-roush/pdfbox-android/maven-metadata.xml; https://raw.githubusercontent.com/TomRoush/PdfBox-Android/master/README.md
- https://repo1.maven.org/maven2/com/googlecode/plist/dd-plist/maven-metadata.xml; https://raw.githubusercontent.com/3breadt/dd-plist/master/README.md; https://github.com/3breadt/dd-plist/releases
- https://raw.githubusercontent.com/claunia/plist-cil/master/README.md; https://repo1.maven.org/maven2/net/openid/appauth/maven-metadata.xml; https://raw.githubusercontent.com/openid/AppAuth-Android/master/README.md
- https://raw.githubusercontent.com/chromium/pdfium/main/LICENSE
- https://raw.githubusercontent.com/Agamnentzar/ag-psd/master/src/abr.ts · …/LICENSE; https://raw.githubusercontent.com/GNOME/gimp/master/app/core/gimpbrush-load.c; https://raw.githubusercontent.com/KDE/krita/master/libs/brush/kis_abr_brush_collection.cpp
- https://raw.githubusercontent.com/microsoft/Win2D/master/README.md · LICENSE.txt
- https://raw.githubusercontent.com/actions/runner-images/main/images/windows/Windows2022-Readme.md · …/images/ubuntu/Ubuntu2404-Readme.md; https://raw.githubusercontent.com/subosito/flutter-action/main/README.md
- https://raw.githubusercontent.com/Kotlin/kmp-production-sample/master/.github/workflows/gradle.yml (공식 JetBrains 샘플)
- https://raw.githubusercontent.com/MicrosoftDocs/msix-docs/main/msix-src/package/signing-package-overview.md · …/unsigned-package.md
- https://raw.githubusercontent.com/MicrosoftDocs/windows-dev-docs/docs/hub/apps/package-and-deploy/smartscreen-reputation.md · …/choose-distribution-path.md · …/deploy-overview.md · …/unpackage-winui-app.md · …/uwp/ui-input/pen-and-stylus-interactions.md
- https://raw.githubusercontent.com/MicrosoftDocs/winrt-api/docs/windows.data.pdf/pdfdocument.md (+ 코드 검색 `pdfpage_rendertostreamasync_*.md`)
- GitHub 이슈: flutter/flutter #42846, #57476, #178108, #183495, #191353, #175790, #148810, #101337, #53532; JetBrains/compose-multiplatform #5278, #1555; dotnet/maui(검색 결과 관련 이슈 없음)

패키지 레지스트리 [공식]
- pub.dev: google_sign_in, googleapis_auth, googleapis, pdfrx, pdfx, printing, syncfusion_flutter_pdfviewer, image, archive, plist_parser, msix
- nuget.org: SkiaSharp, Google.Apis.Auth, Google.Apis.Drive.v3, PdfPig, PDFtoImage, QuestPDF, Microsoft.Graphics.Win2D, plist-cil
- kotlinlang.org: compose-native-distribution.html, compose-desktop-mouse-events.html, supported-platforms.html, releases.html

커뮤니티 [2차]
- https://raw.githubusercontent.com/mrlfarano/myBrush/main/docs/procreate-format-feasibility.md (Procreate .brush 컨테이너 서술); GitHub 코드 검색 "Brush.archive" 92건, ".abr 8BIM samp" 170건
- https://raw.githubusercontent.com/leoafarias/sidekick/main/.github/workflows/pr.yaml (Flutter Windows 빌드 예)
- LanceMcCarthy/DevOpsExamples `.github/workflows/main_build-winui.yml` (WinUI 빌드 예, 경로만 확인)

미열람·추정 [추정]
- Ng et al. "Designing for Low-Latency Direct-Touch Input"(UIST 2012); Jota et al. "How Fast is Fast Enough?"(CHI 2013); Annett et al. "How Low Should We Go?"(GI 2014); 삼성 S펜 지연 공식치; Apple Pencil 지연 공식치; Google OAuth 네이티브 앱 가이드·테스트 모드 토큰 만료 정책; `dumpsys SurfaceFlinger --latency`·`getevent` 문서; Avalonia Skia 기본 백엔드·.NET 크로스 퍼블리시.

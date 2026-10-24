# 외부 브러시 포맷 조사 — Procreate .brush/.brushset · Photoshop .abr · Platen 내부 브러시 모델 초안

> 작성: 2026-10-24 · 상태: **리서치 초안(v1)** · 관련: `docs/BRIEF.md` §2 "외부 브러시", §2b "브러시 임포트(리스크 1순위)"
>
> 등급 표기 — **공식**: 포맷 소유사의 문서 / **1차**: 실제 파일 덤프 또는 파서 소스를 직접 열람 / **2차**: README·포럼·블로그 요약 / **(스니펫)**: WebSearch 결과 요약만 보고 원문은 못 연 것 / **추정**: 근거는 있으나 직접 확인 못 함.
> 모든 URL은 2026-10-24에 실제로 열람(이 세션에서는 github.com 리포·커밋 페이지와 raw.githubusercontent.com, pub.dev가 열렸고, 소스 본문은 raw로 읽었다). 열람에 실패한 호스트(help.procreate.com, procreate.com, adobe.com, community.adobe.com, gitlab.gnome.org, invent.kde.org, gitlab.com 프로젝트 페이지, fileformats.archiveteam.org, krita-artists.org, eur-lex, wikipedia, 법령 사이트 등)는 이 환경의 DNS/프록시에서 차단되어 **본문을 인용하지 않았고** 재시도하지 않았다(§9 LOCAL_TODO L5). GIMP·Krita 소스는 GitHub 미러(GNOME/gimp, KDE/krita)의 raw에서 읽었다.
>
> 코드 복사 없음. 아래는 원리·구조·필드명·수치만 정리한 것이다. **GPL/AGPL/CC BY-NC 리포는 "구현 참고만, 코드 이식 불가"**로 플래그했다.

---

## 0. 요약 판정

| 포맷 | 해독 가능성 | 근거 한 줄 |
|---|---|---|
| Procreate `.brush` / `.brushset` | **가능(동역학 포함)** — 단, 비공식 | zip + `Brush.archive`(NSKeyedArchiver 바이너리 plist) + `Shape.png`/`Grain.png`. 키 이름은 실파일 덤프 3건과 MIT 스키마(aumlette-lab v1.7)로 거의 전부 확인. 의미·단위는 일부 **추정**(크기 단위, 테이퍼 길이 단위). 공식 포맷 문서 없음. |
| Photoshop `.abr` | **가능(팁)** + **부분(동역학)** | v1/v2는 Adobe 6.0 스펙에 공식 기술. v6+(6.1/6.2, 7, 9, 10)는 `8BIM` 섹션(`samp`/`desc`/`patt`/`phry`)으로 비공식이지만 MIT 파서 3종(ag-psd, abrkit, brushkit)이 디스크립터 동역학까지 디코드. GIMP/Krita는 팁만 읽음. |

축소안(B안): 둘 다 **팁 이미지 + 간격 + 이름**만 가져오고 동역학은 Platen 기본값 — 이 수준은 GPL 참조 없이도 MIT 파서 원리만으로 확실히 된다.

---

## 1. Procreate `.brush` / `.brushset`

### 1.1 컨테이너 구조 (1차)

| 항목 | 확인 내용 | 출처 |
|---|---|---|
| `.brush` | **ZIP**(stored/deflate). 루트에 `Brush.archive`, 선택적 `Shape.png`, 선택적 `Grain.png`, `QuickLook/Thumbnail.png`. | [S-P3] Lightbox 설계문서(실파일 1건 기반) · [S-P8] NekoPhoto procreate.cpp 주석 · [S-P13] jank-examples 노트 · [S-P5] barakbl README |
| `.brushset` | **ZIP**. 브러시마다 폴더 1개(폴더명 UUID 관찰) + 루트 `brushset.plist`. 각 폴더 안이 `.brush`와 동일 구조. | [S-P8] NekoPhoto 주석("UUID로 이름 붙은 폴더") · [S-P6] brush-converters read.js · [S-P3] |
| `brushset.plist` | 키 `name`(세트 이름), `brushes`(폴더명 배열 = 표시 순서). **형식은 관찰이 갈림**: brush-converters는 바이너리 plist로 읽고, mkbrushset은 binary1로 쓰며, NekoPhoto 주석은 XML이라 함 → 파서는 **둘 다 수용**해야 함. | [S-P6] · [S-P14] mkbrushset · [S-P8] |
| `Reset/` 하위 폴더 | 브러시 폴더 안에 `Reset/Brush.archive`(원본 설정 백업)가 들어 있을 수 있음. 파서는 제외해야 함. | [S-P6] · [S-P7] Brush-Converter brushset-parser.py |
| `__MACOSX/` | macOS에서 재압축된 세트에 섞여 들어옴. 제외. | [S-P6] · [S-P8] |
| 번들 셰이프/그레인 | Procreate 내장 소스를 쓴 브러시는 `Shape.png`/`Grain.png`가 **파일에 없고** `bundledShapePath`/`bundledGrainPath`에 내장 파일명만 기록(예: `Brush-Preset-Medium.png`, `Brush-Preset-Blank.png`). 내장 이미지는 앱 자산이므로 **동봉 불가** → 대체 로직 필요(§3.5). | [S-P10] geologic-patterns 템플릿 덤프 · [S-P11] Ish-main 덤프 · [S-P12] PROArt 덤프 · [S-P3] |
| 그레인 확장자 | `grain.jpg`(JPEG) 관찰 1건. 파서는 png/jpg 모두 수용. | [S-P15] VarunVF notes · [S-P6] |
| `.brushlibrary` | Procreate 공식 포맷 목록(.brush/.brushset/.brushlibrary)에 있다고 하나 구조는 **자료 부족**. | (스니펫) help.procreate.com/articles/daaqbd-importing-your-brushes — 미열람 |

### 1.2 `Brush.archive` 직렬화 (1차)

- **바이너리 plist(`bplist00`) 안의 NSKeyedArchiver 아카이브.** 최상위 딕셔너리: `$archiver = "NSKeyedArchiver"`, `$version = 100000`, `$top = {root: UID(1)}`, `$objects = [ "$null", <루트 객체>, ... ]`. 루트 객체의 `$class`가 가리키는 클래스 사전: `$classname = "SilicaBrush"`, `$classes = ["SilicaBrush", "NSObject"]`(구형) 또는 `["SilicaBrush", "ValkyrieBrush", "NSObject"]`(신형). [S-P10][S-P11][S-P12][S-P15]
- **디코드 원리**: plist 파서(Python `plistlib`, Android는 자체 bplist 리더 필요)로 읽은 뒤, 값 중 UID 타입을 `$objects[index]`로 치환하는 재귀 해석을 하면 평탄한 `{키: 값}` 사전이 나온다. 중첩 객체: `dynamicsPressure*Curve`는 `ValkyrieMagnitudinalCurve`(`points` = NSArray of 문자열 `"{x, y}"`, `resolution = 1024`), `*RollParameters`는 `ValkyrieBrushRollParameters`(`activationFalloff/Origin/Range`, `mode`), `creationDate`는 NSDate(`NS.time`). 순환 참조 방어(깊이 제한)를 둘 것. [S-P11][S-P12][S-P6][S-P17] chahui
- 주의: 일부 파서는 `$objects[1]`을 루트로 **하드코딩**하는데([S-P7][S-P13]), 올바른 방법은 `$top.root`를 따르는 것([S-P6][S-P17]).
- `color`는 16바이트 data(추정: RGBA float×4), 브러시 본질과 무관. `signedID`/`resetSignedID`는 brush-converters가 출력 시 기록한다고 하지만 실파일 덤프 3건에는 **없음**. [S-P4][S-P12]

### 1.3 공개된 키 목록 (실파일 덤프 3건 + MIT 스키마)

범위는 **원시값(raw)** 기준. GUI 퍼센트는 Procreate가 곡선 변환해 표시한다(aumlette 스키마의 환산식 참고, 예: Spacing GUI% ≈ round(72.943·raw^0.4558)). 출처: [S-P1] aumlette 스키마 v1.7(1차: 스키마 파일 열람) · [S-P10][S-P11][S-P12] 실파일 덤프 · [S-P4] brush-converters settings.js · [S-P8] NekoPhoto.

**스트로크 경로 / 안정화**

| 키 | 타입 | 원시 범위 | 의미 | 등급 |
|---|---|---|---|---|
| `plotSpacing` | float | 0–2 (관찰 0.0026, 2.5e-5, 0.0) | 스탬프 간격, **팁 지름 대비 비율** | 1차(범위: 스키마) |
| `plotSpacingJitter` | float | 0–10 | 간격 랜덤 확장 | 1차 |
| `plotSpacingSpeed` | float | 0–1(스키마 불일치 메모 있음) | 속도→간격 | 1차 |
| `plotJitter` | float | 0–13.78 | 측방향 산란(Jitter Lateral) | 1차 |
| `plotJitterLongitudinal` | float | 동일 | 진행방향 산란 | 1차 |
| `dynamicsFalloff` | float | 0–1 | 스트로크 진행에 따라 사라짐(Fall Off) | 1차 |
| `plotSmoothing` | float | 0–1 | **StreamLine** 양 | 1차 |
| `dynamicsPressureSmoothing` | float | 0–1 | StreamLine 필압 | 1차(스키마) |
| `plotMovingAverageStabilization` | float | 0–1 | 안정화(Stabilization) | 1차(스키마) |
| `plotFFTSmoothingAmount` / `plotFFTSmoothingBias` | float | 0–1 | 모션 필터링 양/표현 | 1차(스키마) |
| `plotSpacingVersion` | int | 관찰 1 | 간격 계산 버전 마커 | 1차 |

**테이퍼**

| 키 | 타입 | 범위 | 의미 | 등급 |
|---|---|---|---|---|
| `pencilTaperStartLength` / `pencilTaperEndLength` | float | 0–1 | 스타일러스 테이퍼 시작/끝 길이 | 1차 |
| `pencilTaperSize` / `pencilTaperOpacity` | float | 0–1 | 테이퍼 끝단 크기/불투명 | 1차 |
| `pencilTaperShape` | float | 0–1 (0 sharp, 1 blunt) | 팁 모양 | 1차 |
| `pencilTaperSizeLinked` | bool | | 양끝 연동 | 1차 |
| `pencilTipAnimation` | bool | | 팁 애니메이션 | 1차 |
| `taperStartLength` … `taperShape`, `taperSizeLinked`, `taperPressure` | 동일 | | **터치(손가락/마우스)용** 별도 세트 | 1차 |
| `taperVersion` | int | 0/1 | 0 = Classic Taper 켜짐(스키마 해석) | 1차(스키마) |
| 길이 단위 | — | — | 길이 1.0이 몇 px인지 **미확인**. NekoPhoto는 300 px로 가정(저자 스스로 "guess"). | **추정** |

**셰이프**

| 키 | 타입 | 범위 | 의미 | 등급 |
|---|---|---|---|---|
| `bundledShapePath` | string/null | | 내장 셰이프 파일명 | 1차 |
| `shapeInverted` | bool | | 셰이프 이미지 반전 | 1차 |
| `shapeRotation` | float | −1–1 | 스트로크 방향 추종(1 = 추종, −1 = 역방향; 구형 파일에서는 고정 각도로 쓰였다는 메모) | 1차 + 2차 |
| `shapeAngle` | float | 라디안(스키마: raw×180/π) | 고정 각도 | 1차(단위는 스키마 vs settings.js가 "회전수 비율"로 상이 → **추정**) |
| `shapeScatter` | float | 0–2 (×180° 해석) | 랜덤 회전 | 1차 |
| `shapeCount` | float | 0–1 (×16) | 스탬프 개수 | 1차 |
| `shapeCountJitter` | float | 0–1 | 개수 지터 | 1차 |
| `shapeRandomise` | bool | | 스트로크마다 랜덤 시작각 | 1차 |
| `shapeFlipXJitter` / `shapeFlipYJitter` | bool | | 랜덤 뒤집기 | 1차 |
| `shapeRoundness` | float | 0.01–1 | 원형도 | 1차 |
| `shapeAzimuth`, `shapeRoll`, `shapeRollMode` | bool/bool/int | | 입력 스타일(방위각/배럴롤) | 1차 |
| `shapeFilter` / `shapeFilterMode` | bool/int | [F,null]=없음, [T,1]=Classic, [T,0]=Improved | 셰이프 필터링(안티에일리어싱) | 1차(스키마) |
| `shapeOrientation` | int | 관찰 1 | 의미 미확인 | **추정** |
| `oriented` | bool | | Orient to Screen | 1차 |
| `stamp` | bool | | 스탬프 미리보기 모드 | 1차 |
| `dynamicsPressureShapeRoundness(+Minimum)`, `dynamicsTiltShapeRoundness(+Minimum)`, `jitterShapeRoundness(X)` | float | | 필압/틸트/지터→원형도 | 1차 |

**그레인**

| 키 | 타입 | 범위 | 의미 | 등급 |
|---|---|---|---|---|
| `bundledGrainPath` | string/null | | 내장 그레인 파일명 | 1차 |
| `textureApplication` | int | 0 = Moving, 1 = Texturised | 그레인 모드 | 1차(스키마) |
| `textureMovement` | float | 0–1 | 0 stamp → 1 rolling | 1차 |
| `textureScale` | float | 0–16 (GUI ≈ 25·√raw) | 스케일 | 1차 |
| `textureZoom` | float | 0–1 | 0 = 크기 따라감, 1 = 크롭 | 1차 |
| `textureRotation` | float | −1–1 | 1 = 스트로크 추종 | 1차 |
| `grainDepth` / `grainDepthMinimum` / `grainDepthJitter` | float | 0–1 | 깊이(강도) | 1차 |
| `textureOffsetJitter` | bool | | 오프셋 지터 | 1차 |
| `grainBlendMode` / `grainBlendModeExtended` | int 쌍 | 표 §1.3 블렌드 | 그레인 블렌드 | 1차(스키마) |
| `textureBrightness` / `textureContrast` | float | −0.75–0.75 / −1–1 | 밝기/대비 | 1차 |
| `textureInverted`, `textureFilter`, `textureFilterMode`, `textureOrientation`, `texturizedGrainFollowsCamera` | | | | 1차 |

**렌더링 / 블렌드 / 웻믹스**

| 키 | 의미 | 등급 |
|---|---|---|
| `renderingRecursiveMixing`, `renderingModulatedTransfer`, `renderingMaxTransfer` | 3-bool 조합 → Light/Uniform/Intense/Heavy Glaze, Uniform/Intense Blending | 1차(스키마) |
| `dynamicsGlazedFlow`(구형 `dynamicsGlazeFlow`) | Flow 0–1 | 1차 |
| `wetEdgesAmount`, `burntEdgesAmount(+BlendMode)` | 웻/번트 엣지 | 1차 |
| `blendMode` / `extendedBlend`(스키마: `extendedBlend2`) | 정수 쌍. Normal 0,0 / Multiply 1,1 / Screen 2,2 / Add 3,3 / Lighten 4,4 / Exclusion 5,5 / Difference 6,6 / Subtract 7,7 / Linear Burn 8,8 / Color Dodge 9,9 / Color Burn 10,10 / Overlay 11,11 / Hard Light 12,12 / Colour 13,13 / Luminosity 14,14 / Hue 15,15 / Saturation 16,16 / Soft Light 17,17 / Behind 18,18 / Darken 19,34 / Hard Mix 20,20 / Vivid 0,21 / Linear Light 0,22 / Pin Light 0,23 / Lighter Colour 0,24 / Dark Colour 25,0 / Divide 26,26 / (그레인 전용) Height 27,27 · Linear Height 28,28 | 1차(스키마; 두 번째 값이 언제 쓰이는지는 스키마도 미기술 → **추정**) |
| `blendGammaCorrect`, `alphaThreshold(+Amount)` | 루미넌스 블렌딩 / 알파 임계 | 1차(스키마) |
| `dynamicsMix`(Dilution), `dynamicsLoad`(Charge), `dynamicsPressureMix`(Attack), `dynamicsWetAccumulation`(Pull), `dynamicsMixSoftening`(Grade), `dynamicsBlur(+Jitter)`, `dynamicsWetnessJitter` | 웻믹스 | 1차 |
| `dualBlendMode(+Extended)` | 듀얼 브러시 결합 모드 | 1차(스키마) |

**다이내믹스 / Apple Pencil / 속성**

| 키 | 범위 | 의미 | 등급 |
|---|---|---|---|
| `dynamicsPressureSize` | 0–1(스키마는 −1–1) | 필압→크기 양. 해석: 최소 크기 = 1 − p (NekoPhoto, brush-converters 모두) | 1차 + 추정(해석) |
| `dynamicsPressureOpacity` | 0–1 | 필압→**플로우**(스탬프당 불투명) | 1차 |
| `dynamicsPressureOpacityTransfer` | 0–1 | 필압→불투명(스키마 "Opacity") | 1차(스키마) |
| `dynamicsPressureBleed(+Speed)` | 0–1 | 필압→블리드 | 1차 |
| `dynamicsPressureSizeCurve` / `…OpacityCurve` / `…BleedCurve` / `…ShapeRoundnessCurve` / `…Hue/Saturation/Brightness/SecondaryColorCurve` / `…TransferModulationCurve` | 곡선 객체 | 제어점 리스트 `"{x, y}"`, `resolution 1024`. 기본 (0,0),(1,1) | 1차 |
| `dynamicsPressureOverall` | 곡선 | 전체 필압 커브(스키마) — 덤프 3건엔 없음 | 1차(스키마) |
| `dynamicsPressureSizeSpeed` / `…OpacitySpeed` / `…BleedSpeed` | 0–1 (관찰 0.3997) | 필압 반응 **속도(지연)**, 속도 다이내믹스 아님 | 1차 |
| `dynamicsSpeedSize` / `dynamicsSpeedOpacity` | −1–1 | 속도→크기/불투명 | 1차 |
| `dynamicsJitterSize` / `dynamicsJitterOpacity` | 0–1 | 랜덤 크기/불투명 | 1차 |
| `dynamicsTiltSize/Opacity/Bleed/Gradation/Compression`, `dynamicsTiltAngle`(0–1 = 0–90°), `*TiltAngle` 다수(관찰 0.1) | | 틸트 | 1차 |
| `dynamicsRoll*`, `attackRoll*`, `*RollParameters` | | 배럴 롤(Apple Pencil Pro) | 1차 |
| `hoverOutline`(0/1/2), `hoverPressure`, `hoverFill` | | 호버 | 1차 |
| `maxSize` / `minSize` | 0–16 (관찰 1.0, 1.019, 2.937) | 크기 상한/하한. **단위 미확인**: Lightbox는 ×100 px, NekoPhoto는 200 px @1.0, ST-iPhonie는 ×120 — 모두 "추정"이라 명시 | **추정** |
| `paintSize` / `paintOpacity` | 0–1 | 마지막 슬라이더 위치(min~max 사이 비율). 구형 파일은 px 저장 | 1차 |
| `maxOpacity` / `minOpacity` | 0–1 | 불투명 범위 | 1차 |
| `eraseSize/Opacity`, `smudgeSize/Opacity`, `saved*` 배열 | | 지우개/스머지 모드 슬라이더 | 1차 |
| `dynamicsJitter{Hue,Saturation,Lightness,Darkness}`, `…Stroke*`, `jitterSecondary`, `dynamicsPressure/Tilt/Roll {Hue,Saturation,Brightness,SecondaryColor}` | | 컬러 다이내믹스 | 1차 |
| `metallicAmount/Scale`, `roughnessAmount/Scale`, `heightAmount/Scale`, `bundledMetallic/Roughness/HeightPath` | | 3D 머티리얼 | 1차 |
| `name`, `authorName`, `creationDate`, `previewSize`, `preview*`, `importedFromABR`, `maxPressureSizeClamped`, `version`(관찰 2) | | 메타 | 1차 |

### 1.4 버전별 차이 (관찰된 키 집합의 층위)

실파일 덤프 3건을 키 집합으로 층위화하면 아래와 같다. Procreate 앱 버전 번호는 **파일 안에 없고**(`version = 2`는 아카이브 포맷 버전) 어떤 앱 버전이 어떤 키를 추가했는지는 공식 자료를 못 열어 **추정**이다.

| 층 | 특징 키 | 덤프 | 앱 버전 추정 |
|---|---|---|---|
| A(구형) | `version` 없음, `$classes=[SilicaBrush,NSObject]`, 커브 객체 없음, `pencilTaper*` 없음, `dynamicsGlazeFlow`(d 없음), `dynamicsGlazeType`, `dynamicsPressureSpeed`, `textureZoom/Rotation`, `shapeOrientation` | [S-P10] geologic-patterns 템플릿 | Procreate 4.x 또는 5.0 초기(추정) |
| B | `version=2`, `ValkyrieBrush` 상속, `ValkyrieMagnitudinalCurve` 커브, `pencilTaper*`+`taperVersion`, `shapeFilter(Mode)`, `dynamicsRenderType`, `blendGammaCorrect` | [S-P11] Ish-main | Procreate 5.x(추정) |
| C(신형) | B + `plotSpacingVersion`, `shapeRoll/RollMode`, `attackRoll*`·`dynamicsRoll*`·`*RollParameters`, `hover*`, `metallic/roughness/height*`, `texturizedGrainFollowsCamera`, `importedFromABR`, `creationDate`, `renderingMaxTransfer`, `plotFFTSmoothing*`, `plotMovingAverageStabilization` | [S-P12] PROArt | Procreate 5.2–5.4(추정: 3D 머티리얼·안정화·호버·배럴 롤 기능 시기와 일치) |

- brush-converters 주석: "Procreate 4와 5는 같은 SilicaBrush 키 이름을 쓴다", 구형은 `paintSize`를 px로, `shapeRotation`을 고정 각도로 썼다(검출: `shapeAngle` 부재). [S-P4] (2차)
- myBrush 리포는 최소 키만 가진 XML plist `Brush.archive`가 **Procreate 5.4에서 임포트 성공**했다고 보고(5.4 신규 브러시 시스템 문서는 미열람). [S-P16] (2차)
- Procreate Handbook 버전별 페이지(help.procreate.com/procreate/handbook/{5.0,5.1,5.2}/brushes/brush-studio-settings) — (스니펫): 5.0/5.1 문서에 "이전 버전 테이퍼 동작으로 되돌리는 토글"(→ `taperVersion`의 Classic Taper와 부합)이 있고, Brush Studio 속성 수가 5.0/5.1의 11개에서 5.2에 12개로 늘었다고 함. 원문 미열람 → LOCAL_TODO L5.
- 파서 정책: **누락 키 = 기본값**, 알 수 없는 키 = 보존(라운드트립 필요 시) 또는 무시.

### 1.5 셰이프·그레인 이미지 규격

| 항목 | 확인 내용 | 등급 / 출처 |
|---|---|---|
| 채널 해석 | **흰색 = 칠해짐(불투명), 검정 = 투명**, 알파가 있으면 luminance×alpha. `shapeInverted`/`textureInverted`로 반전. | 1차 [S-P3][S-P6][S-P8][S-P18] ST-iPhonie |
| 그레인 해석 | 동일(255 = 잉크가 비침). 밝기/대비는 이미지에 한 번 구워서 적용. | 1차 [S-P6][S-P8] |
| 셰이프 해상도 | 실파일 1건 2048×2048(Lightbox). 합성 생성기는 512 또는 256. 튜토리얼들은 2048×2048 또는 1800 px 이상 권장(yal.cc/procreate-binary-brush, paperlike.com 블로그 — (스니펫), 미열람). 공식 최대치 **자료 부족**. | 1차(1건) + (스니펫) + 추정 |
| 배경 테두리 | NekoPhoto는 셰이프의 어두운 테두리를 Procreate 배경으로 보고 제거. | 1차(구현 선택) |
| 포맷 | PNG 기본, JPG 그레인 관찰. | 1차 |

### 1.6 오픈소스 구현 목록

| 리포 | 언어 | 라이선스 | 최종 커밋 | 파싱 범위 | 플래그 |
|---|---|---|---|---|---|
| [S-P1] aumlette-lab/procreate-brush-decoder | TypeScript(Next.js) | **MIT** | 2025-10-17 | 스키마 v1.7로 **전 패널** 키→GUI 값 디코드, diff 뷰어. 렌더 없음 | 원리·키 참조 가능 |
| [S-P4][S-P6] sandyseacastle/brush-converters | JS/WASM | **MIT** | 2026-10-19 | 읽기+쓰기(bplist 직접 구현), CSP 변환. 타협 목록 명시 | 참조 가능 |
| [S-P8] vomitselfie/NekoPhoto `procreate.cpp` | C++ | **GPL-3.0-or-later** | 2026-10-21 | 가장 넓은 동역학 매핑(필압 커브·테이퍼·틸트·롤·그레인) + 신뢰도 표기 | **구현 참고만, 코드 이식 불가** |
| [S-P7] tohsakarat/Brush-Converter `brushset-parser.py` | Python | **CC BY-NC 4.0** | 2025-11-22 | 원시 덤프→JSON, 이미지 추출 | **비상업 조건, 코드 이식 불가** |
| [S-P2] Leon-Schoenbrunn/CSP2PC | Python | MIT | 2025-09-22 | `.brush` **쓰기**(Seed.brush 템플릿 치환), 테이퍼/웻믹스 키 설정 | 참조 가능 |
| [S-P5] barakbl/procreate_brush_claude_tools | Python | MIT | 2026-02-22 | `.brush` 쓰기(생성기) | 참조 가능 |
| [S-P19] MorrowShore/ProcreateBrushExtract | 미표기 | **AGPL-3.0** | 2025-10-14 | 파라미터·팁 추출(웹) | **코드 이식 불가** |
| [S-P20] asterryplace/brushporter (FreyaLupen 변환기의 반-포크) | Python(Krita 플러그인) | **GPL-3.0** | 2024-11-27 | Krita 프리셋 변환 | **코드 이식 불가** |
| [S-P21] pmwoz/brushkit | Rust | MIT | 2026-10-21 | 이름+팁만, fuzzing·wasm. "실파일 5803개 중 최대 Brush.archive 10164 B" 통계 | 참조 가능 |
| [S-P22] chsh2/import_paint_brush | Python(Blender) | GPL-3.0 | 2026-09-19 | `.brushset` 팁 이미지만 | 코드 이식 불가 |
| [S-P23] katrinaau97-eng/brushset-to-abr | C# | **CC BY-NC-SA 4.0** | 2026-08-11 | brushset → ABR v6/v7 **쓰기**(동역학 디스크립터 포함) | 코드 이식 불가 |
| [S-P3] orlandomenke/Lightbox 설계문서 | 문서 | 미확인 | — | 실파일 1건에서 읽은 레이아웃·키·법적 메모 | 문서 참조 |
| [S-P14] aredridel/mkbrushset | Shell(아카이브됨) | 미확인 | — | `.brushset` 포장(binary plist) | — |
| Freya Lupen "Procreate to Krita Brush Converter"(GitLab) | Python | 미확인 | — | 포럼 스니펫만. **미열람** | 자료 부족 |

---

## 2. Photoshop `.abr`

### 2.1 공식 자료 유무

- **Adobe Photoshop 6.0 File Formats Specification(2000-11, v6.0 R2)** §3 "Brushes" — **공식**. `.ABR`(Mac 타입 `8BBR`). 커버 범위는 **version = 1**뿐이며 "Descriptor structure"(Table 2-62)도 같은 문서에 있다. Brush-Converter 리포에 동봉된 PDF 미러로 열람. [S-A1]
- Adobe 현행 "Photoshop File Formats Specification"(adobe.com) — 이 환경에서 **열람 불가(DNS)**. 검색 스니펫은 PSD/PSB 중심이고 ABR 절이 없다고 하나 미확인 → **자료 부족**. abrkit README는 "Modern ABR is not publicly specified in full"이라고 명시. [S-A6]

### 2.2 버전별 구조

**헤더(공통)**: `int16 version`, `int16 (count | subversion)`. v1/v2에선 두 번째 값이 브러시 개수, v6+에선 서브버전(1 또는 2). [S-A1][S-A2][S-A3][S-A4]

**v1 / v2 (공식 + 1차)** — 레코드 반복: `int16 type`(1 = computed, 2 = sampled), `int32 size`(남은 바이트, 모르는 타입 건너뛰기용).
- computed(14 B): `misc int32`, `spacing int16 0–999`, `diameter 1–999`, `roundness 0–100`, `angle −180–180`, `hardness 0–100`. GIMP·Krita는 computed를 **건너뜀**. [S-A1][S-A2][S-A3]
- sampled: `misc int32`, `spacing int16`, (v2만) UCS-2 이름, `antialias u8`, `bounds int16×4`, `bounds-long int32×4`, `depth int16 (=8)`, 이미지: `compression int16`(0 raw / 1 RLE) + 데이터. RLE = 행별 바이트 수 테이블(int16×rows) 후 PackBits. 16384행 초과 시 청크 분할. [S-A1]

**v6+ (비공식, 1차)** — `8BIM` 리소스 블록 연쇄: `'8BIM'` + `key(4)` + `u32 length` + data, 4바이트 정렬. 관찰된 키: `samp`, `desc`, `patt`, `phry`, `lPdc`, `titl`. 지원 버전 매트릭스: GIMP = 6.1/6.2/10.1/10.2(7, 9 거부), Krita = 6.1/6.2만, ag-psd/abrkit/brushkit = 6·7·9·10(서브 1·2), NekoPhoto = 6–10. [S-A2][S-A3][S-A4][S-A5][S-A6][S-A7]
- **`samp`**: 레코드마다 `u32 length`(4바이트 패딩), Pascal 문자열 ID(UUID 형식 36자 → 37 B), 그 뒤 **서브버전 1이면 10 B, 2면 264 B** 미지 필드(길이 필드 직후부터 세면 47/301 B — GIMP·Krita·NekoPhoto는 이 고정 스킵을 사용), `int32 top/left/bottom/right`, `u16 depth(8|16)`, `u8 compression(0 raw|1 RLE)`, 데이터. RLE는 v1과 같은 행별 길이표+PackBits. 16-bit는 상위 바이트만 취하는 구현이 다수. [S-A2][S-A3][S-A4][S-A5][S-A7][S-P15]
- **`desc`**: `u32 descriptor version`(= 16) + Action Descriptor 1개. 루트 항목 `Brsh`(VlLs)의 각 `Objc`가 프리셋 하나. 프리셋은 `sampledData`(TEXT, UUID)로 `samp` 레코드와 연결. [S-A4][S-A5][S-A7]
- **`patt`**: 텍스처 패턴. 레코드 `u32 length`, `u32 version`, `u32 mode`, `u16 height, width`, UTF-16 이름, Pascal ID, (mode 2면 768 B 색인표), 그 뒤 Virtual Memory Array 리스트(채널별 raw/RLE). ag-psd는 PSD 스펙의 Pattern 구조 리더를 재사용. [S-A5][S-A4] (1차: Brush-Converter 구현, ag-psd 구현)
- **`phry`**: 계층(폴더) 정보 디스크립터. ag-psd는 읽고 버림("용도 불명"). [S-A4]

**디스크립터 포맷 원리(공식 Table 2-62 + 1차)**: `u32 len` + UTF-16 class name, `classID`(u32 len, 0이면 4자 코드), `u32 item count`, 항목마다 `key`(u32 len, 0이면 4자) + `OSType`: `obj `(reference), `Objc`/`GlbO`(descriptor), `VlLs`(list: u32 count + 타입 태그 붙은 항목들), `doub`, `UntF`(unit 4자 + double; 단위 `#Prc` 퍼센트, `#Pxl` 픽셀, `#Ang` 도), `TEXT`(u32 len 포함 null, UTF-16BE), `enum`(type ID + value ID), `long`(i32), `bool`(u8), `type`/`GlbC`(class), `alis`, `tdta`(u32 len + raw). 6.0 스펙 표기는 `Enmr`/`Clss`이나 실파일 ABR은 `enum`/`type`으로 관찰. [S-A1][S-A8] brushkit descriptor.rs [S-A5]

### 2.3 디스크립터에서 복원 가능한 파라미터 (1차: ag-psd abr.ts · photocraft abr_map.rs · NekoPhoto abr.cpp)

| 그룹 | 키 | 의미 / 단위 |
|---|---|---|
| 팁 | `Nm  ` 이름, `Dmtr` 지름(#Pxl), `Angl`(#Ang), `Rndn`(#Prc), `Hrdn`(computed만), `Spcn` 간격(#Prc, 지름 대비 %), `Intr` 간격 사용 여부(off면 속도 기반), `flipX`/`flipY`, `sampledData` UUID; 클래스가 `computedBrush`/`sampledBrush`(/`dTips`/`dBrush` 특수 팁) |
| 셰이프 다이내믹스 `useTipDynamics` | `szVr`, `angleDynamics`, `roundnessDynamics` — 각각 하위 `jitter`(#Prc), `bVTy`(제어), `fStp`(페이드 스텝), `Mnm`(최소); `minimumDiameter`, `minimumRoundness`, `tiltScale`, `brushProjection` |
| 산란 `useScatter` | `scatterDynamics`(jitter+제어), `bothAxes`, `Cnt `(1–16), `countDynamics` |
| 텍스처 `useTexture` | `Txtr`(패턴 ID/이름), `textureScale`, `textureDepth`, `minimumDepth`, `textureDepthDynamics`, `textureBlendMode`(기본 Multiply), `InvT` 반전, `TxtC` 팁마다, `textureBrightness`, `textureContrast`, `protectTexture` |
| 듀얼 브러시 `useDualBrush` | `dualBrush`: 중첩 `Brsh`(Dmtr/Spcn/Rndn/Angl/Hrdn), `BlnM`, `Flip`, 자체 scatter/`Cnt ` |
| 컬러 다이내믹스 `useColorDynamics` | `clVr`, `H   `, `Strt`, `Brgh`, `purity`, `colorDynamicsPerTip` |
| 전송 `usePaintDynamics` | `opVr`(불투명 지터+제어+최소), `prVr`(플로우), `wtVr`, `mxVr` |
| 기타 | `Wtdg` 웻 엣지, `Nose` 노이즈, `Rpt ` 에어브러시, `useBrushSize`, `useBrushPose`+pose 키, `toolOptions`(`Opct`, `flow`, `smoothing*`, `Md  `) |
| **제어 enum `bVTy`** | 0 Off, 1 Fade, 2 Pen Pressure, 3 Pen Tilt, 4 Stylus Wheel, 5 Initial Direction, 6 Direction, 7 Initial Rotation, 8 Rotation (ag-psd·photocraft). **NekoPhoto는 5 = Rotation, 6 = Initial Direction, 7 = Direction으로 상이** → 샘플 파일로 검증 필요(**미해결**) |
| 필압 커브 | **없음**. 필압 매핑은 `bVTy = 2` + `Mnm`(최소값)의 선형 하나뿐 |

### 2.4 파서별 복원 범위

| 리포 | 언어 | 라이선스 | 최종 커밋 | 버전 | 팁 | 디스크립터 동역학 | 패턴 | 쓰기 | 플래그 |
|---|---|---|---|---|---|---|---|---|---|
| [S-A2] GIMP `app/core/gimpbrush-load.c` | C | **GPL-3.0-or-later**(abr2gbr, Marco Lamberto 2001 GPL 포함) | — | 1, 2, 6.1, 6.2, 10.1, 10.2 | 8-bit(16→8) | **없음**(v6+ 간격 25 하드코딩, 주석에 "desc 파싱 필요") | 없음 | 없음 | 코드 이식 불가 |
| [S-A3] Krita `libs/brush/kis_abr_brush_collection.cpp` | C++ | **GPL-2.0-or-later** | — | 1, 2, 6.1, 6.2 (7·10 거부) | 8-bit만(16-bit 오독 가능) | 없음(spacing은 "XXX" TODO) | 없음 | 없음 | 코드 이식 불가 |
| [S-A9] scurest/abrupng | Rust | **GPL-3.0**(GIMP 2.8 파생) | 2019-07-25 | 미표기 | PNG(검정 = 투명) | 없음 | 없음 | 없음 | 코드 이식 불가 |
| [S-A4] Agamnentzar/ag-psd `src/abr.ts` | TypeScript | **MIT** | 2026-10-23 | 6, 7, 9, 10 (v1/2 거부) | 8/16 raw, 8 RLE | **전부**(§2.3 표) | 있음 | 없음(ABR) | **1순위 참조** |
| [S-A6] focale-editor/abrkit | Dart | **MIT** | 2026-10-21 | 1, 2, 6, 7, 9, 10 | 1/8/16-bit raw·RLE | 전부(v16 디스크립터, 미지 필드 보존) | 전 색상모드 | **있음**(인코더) | Flutter 채택 시 직접 후보 |
| [S-A7] pmwoz/brushkit | Rust | MIT | 2026-10-21 | 1, 2, 6, 7, 9, 10 | 있음 | 디스크립터 파싱(키 의미 매핑은 최소) | 있음 | 없음 | 참조 가능 |
| [S-A10] vomitselfie/NekoPhoto `abr.cpp` | C++ | GPL-3.0-or-later | 2026-10-21 | 1, 2, 6–10 | 8/16 | 크기·각도·원형도·산란·전송 매핑(텍스처·듀얼은 메모만) | 없음 | 없음 | 코드 이식 불가 |
| [S-A5] tohsakarat/Brush-Converter `abr_solver.py` | Python | CC BY-NC 4.0 | 2025-11-22 | 서브버전 1/2 | 있음 | 디스크립터 → JSON 덤프(의미 매핑 없음) | 있음(VMA 디코드) | 없음 | 코드 이식 불가 |
| [S-A11] chsh2/import_paint_brush `abr_parser.py` | Python | GPL-3.0 | 2026-09-19 | 1, 2, 6, 7 | 있음 | 부분(이름·간격·크기 제어 등 태그표) | 없음 | 없음 | 코드 이식 불가 |
| [S-A12] storytold/photocraft `abr_map.rs` | Rust | 미확인 | — | — | — | 광범위 매핑 표(§2.3의 키 다수) | — | — | 라이선스 미확인 → 참조만 |
| psd-tools(Python) | — | MIT | — | ABR 리더 **없음**(README 기준) | | | | | |

---

## 3. Platen 내부 브러시 모델 초안

### 3.1 전제와 엔진 선택

- 스트로크 = 폴리라인 + 샘플별 `(x, y, pressure, tiltX, tiltY, t)`. 렌더 엔진 2종을 **브러시가 선택**:
  - **(가) `profile`**: 폭·불투명 프로필을 따라 폴리라인을 스트로크(펜·만년필·형광펜). 스탬프 없음 → 가장 싸고 매끈함.
  - **(나) `stamp`**: 팁 이미지를 간격 배치 + 그레인 마스크(연필·질감). Procreate·ABR 임포트 결과는 기본적으로 여기로 들어간다. 단, 팁이 "원형에 가깝고 그레인 없음"이면 임포터가 `profile`로 **승격**해도 됨(판정: 원형도·경도 추정, chahui가 하듯 90%/50% 반경 비교).
- 패키지: 이전 선례처럼 **zip(`brush.json` + `tip.png` + `grain.png`)**, 브러시 id = zip 바이트 SHA-1 앞 12자리, 노트 파일에 사용 브러시 동봉. 재사용 여부 미정이지만 바꿀 이유는 아직 없음.

### 3.2 지원 파라미터 (brush.json)

| 필드 | 타입 | 범위 | 기본 | 비고 |
|---|---|---|---|---|
| `schema` | int | | 1 | |
| `id`, `name`, `author`, `source{format, originalName, importedAt, lossNotes[]}` | | | | `format ∈ {native, procreate, abr}` |
| `engine` | enum | `profile` \| `stamp` | profile | |
| `size.base` | float px | 0.5–200 | 3 | 100% 줌 기준 지름(문서 px) |
| `size.min` / `size.max` | float px | | 0.5 / 60 | 사용자 슬라이더 범위 |
| `opacity` | float | 0–1 | 1 | 스트로크 상한 |
| `flow` | float | 0–1 | 1 | 스탬프당 알파(stamp 전용) |
| `pressure.size.amount` | float | 0–1 | 0.7 | 최소폭 = base×(1−amount) |
| `pressure.size.curve` | points[] | 2–8점, (0,0)…(1,1) 단조 | [(0,0),(1,1)] | §3.4 |
| `pressure.opacity.amount` / `.curve` | | | 0 / 선형 | 만년필은 0.3 등 |
| `velocity.size` | float | −1–1 | 0 | 속도→폭(음수 = 빠를수록 얇게) |
| `velocity.refSpeed` | float px/s | | 1500 | 1500 px/s에서 효과 100% |
| `tilt.size` / `tilt.opacity` | float | −1–1 / 0–1 | 0 | S펜 틸트 사용 |
| `taper.start.length` / `taper.end.length` | float px | 0–300 | 0 / 12 | 끝단 길이(픽셀). 속도 기반 자동 테이퍼는 `taper.auto` |
| `taper.start.size` / `taper.end.size` | float | 0–1 | 0 | 끝단 최소 폭 비율 |
| `taper.auto` | bool | | true | 펜 뗄 때 속도로 끝 테이퍼 |
| `smoothing.streamline` | float | 0–1 | 0.3 | 지연 보간 강도(사용자 조절) |
| `smoothing.stabilize` | float | 0–1 | 0 | 이동평균 윈도 |
| `cap` | enum | round/flat/square | round | 형광펜 flat |
| `blend` | enum | normal/multiply/behind/erase | normal | 형광펜 multiply(다크 모드 보정은 렌더러) |
| `stamp.spacing` | float | 0.01–2 | 0.15 | 지름 대비 비율 |
| `stamp.jitter` | float | 0–2 | 0 | 측방향 산란(지름 비율) |
| `stamp.sizeJitter` / `stamp.opacityJitter` | float | 0–1 | 0 | |
| `stamp.tip.image` | path | | tip.png | 8-bit alpha(§3.5) |
| `stamp.tip.roundness` | float | 0.01–1 | 1 | 이미지에 추가로 적용되는 세로 스케일 |
| `stamp.tip.angle` | float deg | 0–360 | 0 | |
| `stamp.tip.followStroke` | float | −1–1 | 0 | 1 = 진행 방향 추종 |
| `stamp.tip.scatterAngle` | float deg | 0–180 | 0 | 랜덤 회전 |
| `stamp.tip.randomStart` | bool | | false | 스트로크마다 랜덤 시작각 |
| `stamp.tip.flipX` / `flipY` | bool | | false | 랜덤 뒤집기 |
| `stamp.tip.count` | int | 1–4 | 1 | 4 초과는 필기에 의미 없음(Procreate 16, PS 16 → 클램프) |
| `stamp.tip.azimuth` | bool | | false | S펜 orientation으로 회전(후속) |
| `grain.image` | path/null | | null | 타일 8-bit |
| `grain.depth` | float | 0–1 | 0 | 0 = 그레인 없음 |
| `grain.scale` | float | 0.25–4 | 1 | 1 = 타일 원본 px |
| `grain.mode` | enum | `moving` \| `textured` | textured | moving = 팁과 함께 이동(프리컴포즈 8장 순환), textured = 캔버스 고정 |
| `grain.inverted` | bool | | false | 임포트 시 이미지에 구워 넣고 false로 정규화 |
| `grain.rotateWithStroke` | float | 0–1 | 0 | moving 모드 전용 |

**버릴 파라미터와 이유**

| 버림 | 이유 |
|---|---|
| 컬러 다이내믹스(색상/채도/밝기 지터·틸트/롤 색) 전체 | 필기 앱은 단색 잉크. 다크 모드 반전과 충돌 |
| 웻믹스(Dilution/Charge/Attack/Pull/Grade/Blur), 렌더링 모드(Glaze/Blending), 번트 엣지, 알파 임계 | 페인팅용 혼색 엔진 필요. 벡터 스트로크 모델과 비호환 |
| 듀얼 브러시, 3D 머티리얼(metallic/roughness/height), 호버 설정, 배럴 롤 전부 | S펜은 롤 없음·3D 없음. 호버는 OS 레벨 |
| Photoshop 노이즈·에어브러시 빌드업·브러시 포즈·툴 옵션·스무딩 모드 | 자체 스무딩이 대체. 포즈는 입력 오버라이드 |
| 속도→간격, 간격 지터, Fall Off, 틸트 압축/그라데이션, 필압 반응 속도(lag) | 효과 미미, 실기 검증 비용 대비 가치 낮음. 후속 후보 |
| 필압→원형도, 틸트→원형도, 원형도 지터 | 연필 납작 효과는 `tilt.size`로 1차 근사. 후속 후보 |
| 지우개/스머지 모드 슬라이더, 저장된 슬라이더 배열, preview*, 색상 | 앱 상태값 |

### 3.3 변환 손실표

손실 등급: **0** 무손실 · **1** 근사(단위·스케일 추정) · **2** 부분(일부 의미만) · **3** 버림.

**Procreate → 내부**

| Procreate 키 | 내부 필드 | 변환 | 손실 |
|---|---|---|---|
| `Shape.png` + `shapeInverted` | `stamp.tip.image` | lum×alpha → alpha, 반전 적용, 트림·리사이즈 | 0–1(리사이즈) |
| `bundledShapePath`(이미지 없음) | `stamp.tip.image` | 이름 휴리스틱(round/soft/oval…) → 합성 원/타원, 아니면 기본 원 + `lossNotes` | 2 |
| `Grain.png` + `textureInverted` + `textureBrightness/Contrast` | `grain.image` | 구워 넣기 | 1(대비 공식 추정) |
| `bundledGrainPath` | `grain.image=null`, `grain.depth` 보존 | 내장 그레인 없음 → 노이즈 타일 대체 또는 무그레인 + 메모 | 2 |
| `plotSpacing` | `stamp.spacing` | 그대로(하한 0.02) | 0 |
| `plotJitter` | `stamp.jitter` | 그대로 | 0 |
| `plotJitterLongitudinal` | — | 버림 | 3 |
| `plotSmoothing` | `smoothing.streamline` | 그대로 | 0(알고리즘은 다름 → 체감 1) |
| `plotMovingAverageStabilization` | `smoothing.stabilize` | 그대로 | 1 |
| `maxSize`, `minSize`, `paintSize` | `size.max/min/base` | ×K px, **K 미확정**(100/120/200 후보). base = min + paintSize×(max−min) | 1 |
| `maxOpacity`, `paintOpacity` | `opacity` | | 0 |
| `dynamicsGlazedFlow` | `flow` | | 1(글레이즈 모델 차이) |
| `dynamicsPressureSize` + `…SizeCurve` | `pressure.size.amount/curve` | 점 리스트 그대로(단조 보정) | 0 |
| `dynamicsPressureOpacity` + `…OpacityCurve` | `pressure.opacity` | Procreate는 플로우 기준 → 우리는 opacity/flow 중 엔진별 선택 | 1 |
| `dynamicsSpeedSize` | `velocity.size` | 부호 그대로, refSpeed 추정 | 1 |
| `dynamicsSpeedOpacity` | — | | 3 |
| `dynamicsTiltSize/Opacity`, `dynamicsTiltAngle` | `tilt.*` | 시작각 → 데드존 | 1 |
| `dynamicsJitterSize/Opacity` | `stamp.sizeJitter/opacityJitter` | | 0 |
| `pencilTaperStart/EndLength`, `pencilTaperSize/Opacity` | `taper.*` | 길이×L px(**L 미확정**, 300 가설) | 1 |
| `taper*`(터치용) | — | S펜 전용 앱이므로 버림(Windows 편집은 필기 없음) | 3 |
| `shapeRotation` | `stamp.tip.followStroke` | | 0 |
| `shapeAngle` | `stamp.tip.angle` | 단위(rad vs 회전수) 확인 필요 | 1 |
| `shapeScatter`, `shapeRandomise`, `shapeFlipX/YJitter`, `shapeRoundness` | `stamp.tip.*` | | 0 |
| `shapeCount(+Jitter)` | `stamp.tip.count` | ×16 후 1–4 클램프 | 2 |
| `shapeAzimuth` | `stamp.tip.azimuth` | | 0(후속 구현 전까지 3) |
| `shapeRoll*`, `*Roll*` | — | | 3 |
| `textureApplication`, `textureMovement` | `grain.mode` | 0→moving, 1→textured; movement<0.5면 moving에서도 stamp 고정 | 2 |
| `textureScale` | `grain.scale` | 역수/√ 관계 추정 | 1 |
| `textureZoom`, `textureRotation`, `textureOffsetJitter`, `grainDepthMinimum/Jitter`, `grainBlendMode` | — | depth만 보존 | 3 |
| `grainDepth` | `grain.depth` | | 0 |
| `blendMode`(0/1/18) | `blend` | Normal/Multiply/Behind만, 그 외 → normal + 메모 | 2 |
| 렌더링 모드, 웻믹스, 번트 엣지, 컬러 다이내믹스, 머티리얼, 호버 | — | | 3 |
| `name`, `authorName` | `name`, `author` | | 0 |

**.abr → 내부**

| ABR 필드 | 내부 필드 | 변환 | 손실 |
|---|---|---|---|
| `samp` 픽셀(8/16-bit, raw/RLE) | `stamp.tip.image` | 값 = 커버리지(255 = 칠함). 16→8 상위 바이트 | 0 |
| v1/v2 computed (`diameter/roundness/angle/hardness`) | `engine=profile` 또는 합성 원 팁 | hardness → 가장자리 페더 | 1 |
| `Nm  ` | `name` | | 0 |
| `Dmtr` | `size.base`(= max) | px 그대로(Photoshop 문서 px ≈ 우리 문서 px) | 0–1 |
| `Spcn`(+`Intr`) | `stamp.spacing` | %/100; `Intr=false`면 기본 0.15 + 메모 | 0 |
| `Angl`, `Rndn`, `flipX/Y` | `stamp.tip.angle/roundness/flipX/Y` | | 0 |
| `Hrdn` | 팁 페더 | computed만 | 1 |
| `szVr.bVTy=2(Pressure)` + `minimumDiameter` | `pressure.size.amount = 1−min`, 커브 선형 | | 0 |
| `szVr.bVTy=3(Tilt)` | `tilt.size` | 부호 추정 | 1 |
| `szVr.bVTy=1(Fade)`, 4(Wheel), 5–8(Direction/Rotation) | — | 테이퍼 유사 효과만(`fStp×spacing` → `taper.end.length`) | 2–3 |
| `szVr.jitter` | `stamp.sizeJitter` | | 0 |
| `angleDynamics.jitter` / `bVTy=Direction` / `Initial Direction` | `scatterAngle` / `followStroke=1` / `randomStart` | enum 번호 충돌(§2.3) 검증 필요 | 0–1 |
| `roundnessDynamics`, `minimumRoundness` | — | | 3 |
| `useScatter`: `scatterDynamics.jitter`, `bothAxes`, `Cnt ` | `stamp.jitter`, `count` | % → 지름 비율 | 1 |
| `useTexture`: `Txtr`→`patt` 픽셀, `textureScale`, `textureDepth`, `InvT`, `textureBlendMode` | `grain.image/scale/depth/inverted`, 모드 textured | 블렌드는 multiply만 | 1–2 |
| `useDualBrush` | — | | 3 |
| `useColorDynamics` | — | | 3 |
| `usePaintDynamics`: `opVr`(pressure+Mnm), `prVr` | `pressure.opacity.amount = 1−Mnm`, `flow` | | 0 |
| `Wtdg`, `Nose`, `Rpt `, pose, toolOptions, `smoothing*` | — | | 3 |
| `phry` 폴더 구조 | 세트 이름 | | 2 |

### 3.4 필압 커브 표현

- **제어점 N개(2–8, 단조 증가, 끝점 (0,0)/(1,1) 고정)** + 런타임 256-엔트리 LUT로 샘플링(모노톤 큐빅 보간). 이유: Procreate 커브가 제어점 리스트라 **무손실 수입**(`"{x, y}"` 문자열 파싱만), 사용자 설정 화면에서 드래그 편집이 자연스럽다(BRIEF 3R "필압 커브 사용자 조절").
- 지수(감마) 형식은 **프리셋 UI**로만 제공하고 저장은 제어점으로 변환(예: γ → 5점 샘플). ABR은 커브가 없으므로 `(0, min)–(1, 1)` 2점 선형.
- 전체 필압 커브(`dynamicsPressureOverall`에 해당)는 브러시가 아니라 **앱 설정**(입력 보정)으로 두고 브러시 커브와 합성.

### 3.5 셰이프·그레인 이미지 정규화 규칙

1. 디코드 후 `alpha = luminance × srcAlpha`(RGBA) 또는 `alpha = gray`(그레이). 색 채널이 거의 평탄하고 알파가 있으면 알파를 채택(chahui 휴리스틱).
2. 반전 플래그(`shapeInverted`/`textureInverted`/`InvT`)는 **임포트 시 구워 넣고** 모델에는 false로 저장.
3. **셰이프**: 바운딩 박스로 트림 + 1 px 패딩, 정사각 캔버스에 중앙 배치, 긴 변 ≤ **256 px**(펜류) / ≤ **512 px**(질감류, `size.max > 64`일 때)로 다운샘플(고품질 필터). 8-bit 단일 채널 PNG. 4096 px 초과 헤더는 디코드 전에 거부(메모리 방어).
4. **그레인**: 타일로 취급. 정사각으로 크롭(중앙), ≤ **512 px**, 밝기/대비 적용 후 8-bit PNG. 비정사각 ABR 패턴은 크롭 + 메모.
5. ABR `samp` 바이트는 이미 커버리지 → 그대로 alpha. 16-bit는 상위 바이트.
6. 번들 소스(이미지 없음)는 §3.3 대체 규칙 + `source.lossNotes`에 원래 파일명 기록.
7. moving 그레인은 로드 시 **팁×그레인 프리컴포즈 8장**(그레인 오프셋 8종)을 순환 — 선례 유지. 메모리 = 8 × D² B (D=256 → 512 KB, D=512 → 2 MB).

### 3.6 필기용 추가 파라미터

`taper.*.length`(px) · `taper.auto`(속도 기반 끝 테이퍼) · `smoothing.streamline`/`stabilize` · `velocity.size`+`refSpeed` · `cap`(형광펜 flat) · `blend=multiply`+다크 모드 가시성은 렌더러 정책 · 모두 **설정 화면에서 브러시별 오버라이드 가능**(BRIEF 3R).

### 3.7 렌더 비용 추정

스탬프 수는 입력 주파수가 아니라 **펜 속도 ÷ 간격 픽셀**로 결정된다(120 Hz는 샘플 수이며, 샘플 사이는 보간해 스탬프를 채움).

```
spacing_px = spacing × D = 0.05 × 64 px = 3.2 px
stamps/s   = v / spacing_px
```

| 펜 속도 v (갤럭시 탭 S9+ 266 ppi, 1 cm ≈ 105 px) | stamps/s | 120 Hz 프레임당 | 필레이트 (64² = 4096 px/스탬프) |
|---|---|---|---|
| 5 cm/s ≈ 525 px/s (느린 필기) | 164 | 1.4 | 0.7 Mpx/s |
| 15 cm/s ≈ 1575 px/s (보통) | 492 | 4.1 | 2.0 Mpx/s |
| 40 cm/s ≈ 4200 px/s (빠른 획) | 1313 | 10.9 | 5.4 Mpx/s |

- 결론: 64 px·5%는 CPU로도 여유. 악화 조건은 **큰 팁 + 작은 간격**: D=512, spacing 2% → 10 px → 40 cm/s에서 420 stamps/s × 262 144 px = **110 Mpx/s** → GPU 필수, 또는 임포트 시 간격 하한(`max(spacing, 0.03)`)과 팁 상한(512)으로 묶는다.
- 필압으로 D가 줄면 간격 px도 줄어 스탬프 수가 늘어남(얇은 끝에서 최대). 간격을 **현재 D가 아닌 base D 기준**으로 계산하면 상한이 고정된다(품질 손실 미미).
- `count>1`, `grain.mode=moving`(8장 순환은 추가 비용 없음), `scatterAngle`(회전 샘플링)은 상수배.

---

## 4. 법적·정책 메모

- 전제: 사용자 소유 파일만 읽음, 타사 에셋(Procreate 내장 셰이프/그레인, Photoshop 패턴) 동봉 금지, 앱 바이너리 역컴파일 없음, DRM 없음(둘 다 평문 zip/바이너리). 이 범위에서 **포맷 파싱 자체의 알려진 소송·분쟁 사례: 자료 부족**(1차 법령·판례 사이트가 이 환경에서 열리지 않아 인용 불가).
- 간접 근거(2차·추정): GIMP(2001년 abr2gbr 이래)와 Krita가 ABR 리더를 20년 이상 배포했고, Procreate 자체가 ABR을 임포트하며(`importedFromABR` 키 1차 관찰 [S-P12]), Procreate 브러시 파서가 GitHub에 다수 공개돼 있다. 분쟁 흔적은 찾지 못했다(부재 증명은 아님).
- myBrush 리포 메모(2차 [S-P16]): Procreate 이용약관(2025-07-09)이 "웹사이트·서비스·도구"의 리버스 엔지니어링을 금지한다고 하며, 사용자 내보내기 파일에 적용되는지는 불명확하다고 적음. 원문 미열람 → LOCAL_TODO.
- 라이선스 위생: GPL/AGPL(GIMP·Krita·abrupng·NekoPhoto·brushporter·chsh2·MorrowShore)과 CC BY-NC(Brush-Converter·brushset-to-abr)는 **원리만**. MIT(ag-psd·abrkit·brushkit·aumlette·brush-converters·CSP2PC·barakbl)는 참조 가능하되 BRIEF 원칙상 **코드 복사 없이** 구조만 가져온다.
- Adobe 6.0 스펙 PDF는 "informational use only" 조건의 공개 문서. 미러(제3자 리포 동봉)로 읽었으므로 원본 URL 확인을 LOCAL_TODO에 둔다.

---

## 5. 가져올 원리 (3~5개)

1. **컨테이너 = zip + 설정 1파일 + 팁/그레인 PNG** (Procreate 구조 그대로가 우리 선례와 일치) — 임포터는 "폴더 = 브러시 1개" 규칙으로 `.brush`/`.brushset`/`.zip`을 한 코드로 처리하고, `Reset/`·`__MACOSX/`는 건너뛴다.
2. **NSKeyedArchiver 해석 = `$top.root`부터 UID 치환**. `$objects[1]` 하드코딩 금지, 깊이 제한, 미지 키 보존.
3. **ABR = 헤더 2+2 B → `8BIM` 블록 워킹 → `samp`(47/301 스킵, bounds, depth, RLE) + `desc`(디스크립터 v16, `Brsh` 리스트, `sampledData`로 연결)**. v1/v2는 공식 스펙대로 별도 경로.
4. **필압 = 제어점 커브**(Procreate 호환) — ABR의 "제어 + 최소값"은 2점 커브로 흡수.
5. **이미지 정규화를 임포트 시점에 끝낸다**(반전·밝기/대비·트림·리사이즈를 구워 넣음) → 런타임 모델은 단순 alpha 팁 + alpha 타일만 안다.

## 6. 버릴 것

컬러 다이내믹스·웻믹스·렌더링 모드·듀얼 브러시·3D 머티리얼·호버·배럴 롤·브러시 포즈·노이즈/에어브러시·툴 옵션 스무딩·터치용 테이퍼 세트·`count>4`·Normal/Multiply/Behind 외 블렌드 모드. 동역학 중 **필압 반응 속도(lag)·속도→간격·Fall Off·원형도 다이내믹스**는 후속 후보로 보류.

## 7. 리스크와 축소안

| 리스크 | 징후 | 축소안 |
|---|---|---|
| Procreate 크기·테이퍼 **단위 미확정**(K px, L px) | 수입 브러시가 너무 굵거나 테이퍼가 안 보임 | 실기 샘플로 K·L 보정 상수 1회 측정(LOCAL_TODO). 그 전엔 K=100, L=300 가정 + 임포트 직후 미리보기에서 사용자가 크기 보정 |
| Procreate 신버전이 키 추가/의미 변경 | `version` 또는 `plotSpacingVersion`/`taperVersion` 값 변화 | 누락 키 = 기본값, 미지 키 보존. 버전 마커를 `lossNotes`에 기록 |
| ABR `bVTy` enum 번호 충돌(5–8) | 각도 제어 브러시가 엉뚱하게 회전 | 샘플 파일로 확정. 그 전엔 5–8을 모두 `followStroke=1`로 뭉갬 |
| ABR 서브버전 미지 필드(10/264 B) 구조 변경 | v10.2 이후 파일에서 bounds 오독 | 바운드 검증(양수·≤8192) 실패 시 레코드 건너뛰고 "팁만 복구" 모드 |
| 번들 셰이프/그레인 부재 | Procreate 기본 브러시 대부분 | 이름 휴리스틱 → 합성 팁, 그레인은 노이즈 타일 또는 없음. UI에서 "일부 질감 미포함" 표시 |
| 해독 전면 실패 | — | **B안: 팁 이미지 + 간격 + 이름만** 수입, 나머지 Platen 기본값. 이 경로는 GIMP·Krita 수준이라 확실히 동작 |
| 라이선스 오염 | GPL 코드 참조 흔적 | 구현은 MIT 파서(ag-psd/abrkit) 구조와 공식 6.0 스펙만 보고 작성, 커밋 메시지에 참고 출처 명시 |
| 성능 | 큰 팁+작은 간격 | §3.7 상한(팁 ≤512, spacing ≥0.03, base-D 기준 간격) |

---

## 8. 스택 선택에 주는 힌트

- Flutter 채택 시 **abrkit(Dart, MIT)**이 ABR 디코더+인코더로 직접 후보(의존성 추가는 승인 사항). Procreate 쪽은 Dart bplist/NSKeyedArchiver 리더를 직접 써야 함(소규모).
- KMP/Kotlin 채택 시 둘 다 직접 구현(ag-psd 구조를 참조). Android의 `java.util.zip` + 자체 bplist 리더.
- 어느 쪽이든 **Windows 편집 앱도 같은 임포터**를 써야 노트 안 브러시 동봉이 양쪽에서 렌더된다 → 공용 코드(KMP 공유 모듈 또는 Dart 패키지)에 둘 것.

---

## 9. LOCAL_TODO 후보 (사용자 제공 요청 양식)

`docs/research/samples/` 아래에 올려 주시면 실제 바이트로 §1.3·§2.3의 추정 항목(K, L, shapeAngle 단위, bVTy 번호, brushset.plist 형식)을 확정하겠습니다. **구매한 상용 브러시는 리포에 커밋하지 말고** 로컬 경로만 알려 주세요(기본 브러시 또는 직접 만든 브러시가 가장 좋음).

| # | 요청 | 양식 / 메모 |
|---|---|---|
| L1 | Procreate에서 **직접 만든** 브러시 2개를 `.brush`로 내보내기: (a) 사용자 Shape.png+Grain.png 사용, 필압 커브 3점 이상 수정, 테이퍼 50%; (b) 내장 셰이프/그레인만 사용 | 파일명 `procreate-<앱버전>-custom.brush`, `-bundled.brush`. 앱 버전·iPadOS 버전 메모 |
| L2 | 같은 브러시를 포함한 **`.brushset`** 1개(2개 이상 브러시, 순서 바꿔서) | `brushset.plist`가 XML인지 바이너리인지 확정용 |
| L3 | Brush Studio 화면 캡처: Stroke Path·Taper·Shape·Grain·Apple Pencil·Properties 패널(L1(a) 브러시) | 원시값 ↔ GUI % 환산식과 단위 K·L 측정용. Maximum Size 슬라이더 값과 실제 획 굵기(px) 비교 1회 |
| L4 | `.abr` 샘플: (a) Photoshop CC에서 "브러시 내보내기"로 만든 v10 파일(텍스처·산란·전송 설정 포함, 각도 제어 = Direction 하나, Rotation 하나) (b) 구형 v1/v2 파일(무료 구형 팩) | 상용 팩 불가. 가능하면 직접 생성 |
| L5 | 아래 공식 페이지를 PDF로 저장해 전달(이 환경에서 DNS 차단): Procreate Handbook "Brush Studio Shape/Grain/Settings", "Importing your brushes", "Procreate 5.4 new brush system"; Procreate Terms & Conditions; Adobe "Photoshop File Formats Specification"(ABR 절 유무 확인) | URL은 §10 "미열람" 목록 참조 |
| L6 | 실기(갤럭시 탭 S9+)에서 펜 속도 분포 측정(px/s 히스토그램) | §3.7 refSpeed·성능 상한 보정 |

---

## 10. 출처 목록 (열람일 2026-10-24)

**Procreate**

- [S-P1] aumlette-lab/procreate-brush-decoder — 리포 https://github.com/aumlette-lab/procreate-brush-decoder · 스키마 https://raw.githubusercontent.com/aumlette-lab/procreate-brush-decoder/procreate-brush-decoder/src/data/procreate-brush-decoder-v1.7.json · 커밋 https://github.com/aumlette-lab/procreate-brush-decoder/commits/procreate-brush-decoder — MIT — **1차(스키마 파일 열람; 제3자 리버스 산출물)**
- [S-P2] Leon-Schoenbrunn/CSP2PC — https://github.com/Leon-Schoenbrunn/CSP2PC · https://github.com/Leon-Schoenbrunn/CSP2PC/commits/main — MIT — 2차(README)
- [S-P3] orlandomenke/Lightbox `docs/DESIGN-procreate-import.md` — https://raw.githubusercontent.com/orlandomenke/Lightbox/main/docs/DESIGN-procreate-import.md — 라이선스 미확인 — 1차(실파일 1건 관찰 기록)
- [S-P4] sandyseacastle/brush-converters `lib/procreate/settings.js` — https://raw.githubusercontent.com/sandyseacastle/brush-converters/main/lib/procreate/settings.js — MIT — 1차(소스)
- [S-P5] barakbl/procreate_brush_claude_tools — https://github.com/barakbl/procreate_brush_claude_tools · https://github.com/barakbl/procreate_brush_claude_tools/commits/main — MIT — 2차
- [S-P6] sandyseacastle/brush-converters `lib/procreate/read.js` — https://raw.githubusercontent.com/sandyseacastle/brush-converters/main/lib/procreate/read.js · 리포 https://github.com/sandyseacastle/brush-converters · 커밋 https://github.com/sandyseacastle/brush-converters/commits/main — MIT — 1차(소스)
- [S-P7] tohsakarat/Brush-Converter — https://github.com/tohsakarat/Brush-Converter · https://raw.githubusercontent.com/tohsakarat/Brush-Converter/main/README.md · https://raw.githubusercontent.com/tohsakarat/Brush-Converter/main/brushset-parser.py · https://github.com/tohsakarat/Brush-Converter/commits/main — **CC BY-NC 4.0** — 1차(소스)
- [S-P8] vomitselfie/NekoPhoto `src/core/src/procreate.cpp` — https://raw.githubusercontent.com/vomitselfie/NekoPhoto/main/src/core/src/procreate.cpp · `docs/brush-engine.md` https://raw.githubusercontent.com/vomitselfie/NekoPhoto/main/docs/brush-engine.md · 리포 https://github.com/vomitselfie/NekoPhoto · 커밋 https://github.com/vomitselfie/NekoPhoto/commits/main — **GPL-3.0-or-later** — 1차(소스)
- [S-P10] davenquinn/geologic-patterns `procreate-brushes/template/Brush.archive`(XML plist) — https://raw.githubusercontent.com/davenquinn/geologic-patterns/master/procreate-brushes/template/Brush.archive — 라이선스 미확인 — **1차(실파일 덤프, 구형)**
- [S-P11] Thoseidiots/Ish-main `brush_archive.xml` — https://raw.githubusercontent.com/Thoseidiots/Ish-main/main/brush_archive.xml — 라이선스 미확인 — **1차(실파일 덤프, version 2)**
- [S-P12] naveenkumara2024-gif/PROArt `research/format-probe/brush_archive_keys.txt` — https://raw.githubusercontent.com/naveenkumara2024-gif/PROArt/main/research/format-probe/brush_archive_keys.txt — 라이선스 미확인 — **1차(실파일 3건 키 덤프, 신형)**
- [S-P13] pfeodrippe/jank-examples 노트 — https://raw.githubusercontent.com/pfeodrippe/jank-examples/main/ai/20260106-procreate-brush-import-plan.md · https://raw.githubusercontent.com/pfeodrippe/jank-examples/main/ai/20260106-procreate-brush-import.md — 2차
- [S-P14] aredridel/mkbrushset — https://raw.githubusercontent.com/aredridel/mkbrushset/main/mkbrushset — 1차(스크립트)
- [S-P15] VarunVF/brush_converter `notes.md` — https://raw.githubusercontent.com/VarunVF/brush_converter/main/notes.md — 1차(덤프 발췌)
- [S-P16] mrlfarano/myBrush `docs/procreate-format-feasibility.md` — https://raw.githubusercontent.com/mrlfarano/myBrush/main/docs/procreate-format-feasibility.md — 2차(5.4 임포트 성공 보고·법적 메모)
- [S-P17] PainterAnkry/chahui `client/renderer/brush-import.js` — https://raw.githubusercontent.com/PainterAnkry/chahui/main/client/renderer/brush-import.js — 라이선스 미표기 — 1차(소스; Procreate+ABR)
- [S-P18] hey8v0/ST-iPhonie `ui/paint/brush-import.js` — https://raw.githubusercontent.com/hey8v0/ST-iPhonie/main/ui/paint/brush-import.js — 라이선스 미표기 — 1차(소스)
- [S-P19] MorrowShore/ProcreateBrushExtract — https://github.com/MorrowShore/ProcreateBrushExtract · https://github.com/MorrowShore/ProcreateBrushExtract/commits/main — **AGPL-3.0** — 2차
- [S-P20] asterryplace/brushporter — https://github.com/asterryplace/brushporter · https://github.com/asterryplace/brushporter/commits/main — **GPL-3.0** — 2차
- [S-P21] pmwoz/brushkit — https://github.com/pmwoz/brushkit · https://github.com/pmwoz/brushkit/commits/main · https://github.com/pmwoz/brushkit/tree/main/crates — MIT — 2차(README)
- [S-P22] chsh2/import_paint_brush — https://github.com/chsh2/import_paint_brush · https://github.com/chsh2/import_paint_brush/commits/main — GPL-3.0 — 2차
- [S-P23] katrinaau97-eng/brushset-to-abr — https://github.com/katrinaau97-eng/brushset-to-abr · https://github.com/katrinaau97-eng/brushset-to-abr/tree/main · https://github.com/katrinaau97-eng/brushset-to-abr/commits/main — **CC BY-NC-SA 4.0** — 2차
- GitHub 코드 검색(`"Brush.archive"`, `"SilicaBrush" "$classname"`, `"pencilTaperStartLength"`, `"shapeRoll"`, `"plotSpacingVersion"`) — 발견 경로. 결과 스니펫은 위 1차 소스로 대체 열람.

**Photoshop ABR**

- [S-A1] Adobe Photoshop 6.0 File Formats Specification v6.0 R2 (2000-11), §"Brushes" Tables 3-2~3-7, §"Descriptor structure" Tables 2-62~2-67 — 미러 PDF https://raw.githubusercontent.com/tohsakarat/Brush-Converter/main/ps6ffspecsv2.pdf (로컬 pdftotext로 열람) — **공식(미러)**
- [S-A2] GIMP `app/core/gimpbrush-load.c` (GitHub 미러) — https://raw.githubusercontent.com/GNOME/gimp/master/app/core/gimpbrush-load.c — **GPL-3.0-or-later** — 1차(소스)
- [S-A3] Krita `libs/brush/kis_abr_brush_collection.cpp` (GitHub 미러) — https://raw.githubusercontent.com/KDE/krita/master/libs/brush/kis_abr_brush_collection.cpp — **GPL-2.0-or-later** — 1차(소스)
- [S-A4] Agamnentzar/ag-psd `src/abr.ts` — https://raw.githubusercontent.com/Agamnentzar/ag-psd/master/src/abr.ts · 리포 https://github.com/Agamnentzar/ag-psd · 라이선스 https://github.com/Agamnentzar/ag-psd/blob/master/LICENSE · 커밋 https://github.com/Agamnentzar/ag-psd/commits/master — MIT — 1차(소스)
- [S-A5] tohsakarat/Brush-Converter `abr_solver.py` — https://raw.githubusercontent.com/tohsakarat/Brush-Converter/main/abr_solver.py — CC BY-NC 4.0 — 1차(소스)
- [S-A6] abrkit — https://pub.dev/packages/abrkit · https://pub.dev/documentation/abrkit/latest/ · https://github.com/focale-editor/abrkit · https://github.com/focale-editor/abrkit/commits/main — MIT — 2차(README/문서)
- [S-A7] pmwoz/brushkit `crates/abr/src` — https://github.com/pmwoz/brushkit/tree/main/crates/abr/src — MIT — 2차(파일 목록)
- [S-A8] pmwoz/brushkit `crates/abr/src/descriptor.rs` — https://raw.githubusercontent.com/pmwoz/brushkit/main/crates/abr/src/descriptor.rs — MIT — 1차(소스)
- [S-A9] scurest/abrupng — https://github.com/scurest/abrupng · https://github.com/scurest/abrupng/commits/master — GPL-3.0(GIMP 2.8 파생) — 2차
- [S-A10] vomitselfie/NekoPhoto `src/core/src/abr.cpp` — https://raw.githubusercontent.com/vomitselfie/NekoPhoto/main/src/core/src/abr.cpp — GPL-3.0-or-later — 1차(소스)
- [S-A11] chsh2/import_paint_brush `brush_file_parsers/abr_parser.py` — https://raw.githubusercontent.com/chsh2/import_paint_brush/main/brush_file_parsers/abr_parser.py — GPL-3.0 — 1차(소스)
- [S-A12] storytold/photocraft `crates/io/src/abr_map.rs` — https://raw.githubusercontent.com/storytold/photocraft/main/crates/io/src/abr_map.rs — 라이선스 미확인 — 1차(소스)
- psd-tools — https://github.com/psd-tools/psd-tools — MIT — 2차(ABR 리더 없음 확인)

**(스니펫) 등급으로만 언급한 URL(원문 미열람)**: help.procreate.com/articles/daaqbd-importing-your-brushes · help.procreate.com/procreate/handbook/5.0/brushes/brush-studio-settings · …/5.1/… · …/5.2/brushes/brush-studio · yal.cc/procreate-binary-brush · paperlike.com/blogs/paperlikers-insights/procreate-brush-studio-explained · krita-artists.org/t/procreate-brush-import-plugin-prototype/104894 (Freya Lupen 변환기, "미완성·프리셋 깨짐" 자평).

**열람 시도했으나 실패(인용 안 함, LOCAL_TODO L5)**: help.procreate.com(Brush Studio Shape/Grain/Settings, Importing your brushes, 5.4 brush system) · procreate.com/terms-conditions · adobe.com/devnet-apps/photoshop/fileformatashtml · community.adobe.com ABR 스레드 · fileformats.archiveteam.org/wiki/Photoshop_brush · gitlab.gnome.org · invent.kde.org · krita-artists.org(Freya Lupen 변환기 스레드) · developer.apple.com NSKeyedArchiver(응답은 왔으나 본문 비어 있음) · opensource.apple.com CFBinaryPList.c · eur-lex / wikipedia / copyright.gov(법적 메모용).

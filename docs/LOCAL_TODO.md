# LOCAL_TODO — 사용자 로컬 숙제

실기기(갤럭시 탭 S9+ · Windows 노트북)·로컬 파일·계정이 필요한 항목. **필수**는 마일스톤이 막히는 것, **권장**은 등급을 "실측"으로 올리는 것. 결과는 `docs/research/samples/` 또는 `docs/measurements/`에 두고, 상용 브러시·개인 노트 원본은 커밋하지 않는다.

## 0. 환경 (지금)
| # | 항목 | 등급 | 방법 |
|---|---|---|---|
| E1 | **네트워크 정책 개방** | 권장(리서치 품질) | 세션 제목줄 클라우드 환경 메뉴 → Edit → Network access를 넓히거나 Allowed domains에 추가: `support.goodnotes.com goodnotes.com developer.samsung.com apps.apple.com play.google.com developers.google.com support.google.com docs.flutter.dev learn.microsoft.com jetbrains.com fonts.google.com github.com api.github.com youtube.com reddit.com namu.wiki blog.naver.com`. 안내: https://code.claude.com/docs/en/cloud-environments#network-access |
| E2 | Android SDK 설치 승인 | 필수(M0 APK) | 계획서 §9.1 승인으로 대체 |

## 1. M0 스파이크 측정 (M0 APK/exe 배포 후)
| # | 항목 | 등급 | 실행 | 기록 양식 |
|---|---|---|---|---|
| S1 | 펜 다운→첫 픽셀 지연 | 필수 | `adb install spike-android.apk`. 다른 폰으로 **240fps 슬로모** 촬영: 펜촉과 화면 좌상단 마커가 한 프레임에 들어오게. 프론트버퍼 on/off × 예측 on/off 4조합 × 10회. **삼성노트도 같은 조건 10회**. | `docs/measurements/m0-latency.csv`: `조합, 회차, 접촉프레임, 첫변화프레임, 프레임차, ms(=차×4.17)` + 영상 파일명 |
| S2 | 입력 Hz·히스토리·필압 단계·틸트·호버 | 필수 | 스파이크 화면 오버레이 수치를 10초 필기 후 스크린샷. 보조: `adb shell getevent -lt /dev/input/eventN`(S펜 장치)을 5초 캡처해 `SYN_REPORT` 간격 | 스크린샷 + `m0-input.txt`(Hz 두 값, 고유 필압 수, 틸트/방위/호버 yes/no) |
| S3 | 팜 리젝션 | 필수 | 손바닥을 대고 20회 필기, 오획 남는 횟수 세기 | `m0-palm.txt`: `회차, FLAG_CANCELED 수신, 오획 잔존` |
| S4 | 1만 획 프레임 | 필수 | "1만 획 로드" → 10초 팬/줌 → 오버레이 p50/p95/드롭 스크린샷. 보조 `adb shell dumpsys gfxinfo <pkg> framestats > m0-frames.txt` | 스크린샷 + 파일 |
| S5 | Windows 편집·렌더 | 필수 | 노트북에 msi 설치(SmartScreen "추가 정보 → 실행" 캡처). 마우스(및 터치 가능 시 터치)로 올가미·핀치·페이지 넘김·1만 획 팬/줌. Android 스파이크에서 저장한 획 파일을 열어 PNG 덤프 버튼 → 두 PNG 업로드 | `m0-windows.txt`(성공/실패 항목, p95) + PNG 2장 |

## 2. Google OAuth (M7 전까지, 약 40분)
| # | 항목 | 등급 | 실행 |
|---|---|---|---|
| G1 | Cloud 프로젝트 `platen-sync` 생성 → Google Drive API 사용 설정 | 필수 | console.cloud.google.com |
| G2 | OAuth 동의 화면: External, 앱 이름 Platen, 범위 `…/auth/drive.file`(+`openid email profile`), 테스트 사용자에 본인 Gmail | 필수 | 스파이크 후 "앱 게시 → In production"(비민감 범위라 검증 제출 불필요) |
| G3 | 클라이언트 생성: Android ×2(디버그·릴리스 SHA-1, applicationId = 계획서 A2 확정값) · 웹 애플리케이션 ×1(Credential Manager용) · 데스크톱 앱 ×1 | 필수 | SHA-1: 디버그 `keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android`, 릴리스 `keytool -list -v -keystore <release.jks> -alias <alias>`. 릴리스 keystore는 오프라인 백업 |
| G4 | 값 전달 | 필수 | 리포에 넣지 말고: 웹/Android/데스크톱 client_id는 `local.properties`(gitignore) 형식으로 제게 알려주거나 GitHub Secrets(`PLATEN_WEB_CLIENT_ID`, `PLATEN_DESKTOP_CLIENT_ID`, `PLATEN_DESKTOP_CLIENT_SECRET`, `PLATEN_RELEASE_KEYSTORE_BASE64`, `PLATEN_KEYSTORE_PASSWORD`)에 등록 |
| G5 | 실측(R2·R1) | 권장 | 두 클라이언트로 각각 로그인 → 한쪽이 만든 파일이 다른 쪽에 보이는지; 7일 뒤 Testing 토큰 만료 여부 |

## 3. 샘플 파일 (M3 전까지)
| # | 항목 | 등급 | 양식 |
|---|---|---|---|
| B1 | Procreate **직접 만든** 브러시 2개 `.brush`(a: 사용자 Shape+Grain, 필압 커브 3점 이상, 테이퍼 50% / b: 내장 셰이프·그레인만) + 둘을 담은 `.brushset` 1개 | 권장(없으면 합성 샘플로 구조만 검증) | `docs/research/samples/procreate-<앱버전>-{custom,bundled}.brush`, `-set.brushset`. Brush Studio 패널 캡처(Stroke Path·Taper·Shape·Grain·Apple Pencil) 동봉 → 크기 상수 K·테이퍼 L 보정 |
| B2 | `.abr` 샘플: Photoshop "브러시 내보내기"로 만든 v10(텍스처·산란·전송 포함) + 구형 v1/v2 무료 파일 | 권장 | `samples/abr-v10-custom.abr`, `abr-v2-*.abr`(상용 팩 금지) |
| B3 | 스타노트 노트 1개 **내보내기/백업 파일** | 권장(포맷 등급 상향) | `samples/starnote-sample.note`(개인 내용 없는 테스트 노트) |

## 4. 필기감 기준 스크린샷 (M2 판정용)
| # | 항목 | 방법 | 양식 |
|---|---|---|---|
| F1 | 스타노트 만년필·삼성노트 기본 펜·(M2부터) Platen: 같은 굵기 설정으로 **약·중·강 필압 가로선 3줄 + 빠른 지그재그 1줄 + "platen" 필기 1줄** | 100% 줌, 화면 스크린샷(PNG 원본) | `docs/measurements/feel-<앱>-<날짜>.png`. 제가 픽셀 폭·테이퍼 길이·떨림 진폭을 측정해 수치화 |
| F2 | 스타노트 설정 화면(만년필·연필 슬라이더) 캡처 | — | `feel-starnote-settings.png` |

## 5. 이름·법무 (M7 전까지, 10분)
| # | 항목 | 방법 |
|---|---|---|
| N1 | App Store에서 "Platen" 검색 → "Platen: Paper & Ink" 개발사·카테고리 캡처 | iPhone/iPad 또는 웹 |
| N2 | KIPRIS("플래튼"/"PLATEN", 9·42류)·USPTO 1회 검색 결과 요약 | 결과를 DECISIONS에 기록 요청 |
| N3 | `applicationId` 최종값 확인(계획서 A2) | 한 줄 답변 |

## 6. (선택) 차단된 원문 확인
리서치 문서의 "(스니펫)" 항목 원문: Google OAuth 만료 정책(`developers.google.com/identity/protocols/oauth2#expiration`), Drive 범위 민감도(`…/workspace/drive/api/guides/api-specific-auth`), Procreate Handbook Brush Studio, Adobe 파일 포맷 스펙 ABR 절. E1이 해결되면 제가 직접 합니다.

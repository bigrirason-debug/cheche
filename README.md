# Platen

타자기 구조 은유의 손글씨 노트앱 — Android(갤럭시 탭 S9+, S펜) 필기 + Windows 편집, Google Drive 동기화, Procreate/Photoshop 브러시 가져오기.

- 기획·결정: [`docs/BRIEF.md`](docs/BRIEF.md) · [`docs/PLAN.md`](docs/PLAN.md) · [`docs/DECISIONS.md`](docs/DECISIONS.md) · 현재 상태 [`docs/HANDOFF.md`](docs/HANDOFF.md)
- 사용자 숙제(실기기 측정·OAuth 설정): [`docs/LOCAL_TODO.md`](docs/LOCAL_TODO.md)
- 디자인 토큰: [`docs/design/DESIGN.md`](docs/design/DESIGN.md)

## 설치 파일 받기
GitHub → Actions → `build` 워크플로의 최신 성공 실행 → **Artifacts**
- `platen-android-debug-apk` → 압축 해제 후 `app-debug.apk` 를 태블릿에 설치(`adb install -r app-debug.apk` 또는 파일로 옮겨 "출처를 알 수 없는 앱" 허용)
- `platen-windows` → `Platen-<버전>.msi`(또는 `.exe`) 실행. 서명되지 않은 개인 빌드라 SmartScreen 경고가 뜨면 "추가 정보 → 실행"

## Google Drive 동기화 켜기 (선택)
앱은 로그인 없이도 로컬로 동작한다. 동기화하려면 Google Cloud 콘솔에서(자세히: LOCAL_TODO G1~G4):
1. 프로젝트 생성 → Google Drive API 사용 → OAuth 동의 화면(External, 범위 `drive.file`)
2. **데스크톱 앱** 클라이언트 → JSON 다운로드 → Windows 의 `%USERPROFILE%\.platen\oauth-client.json` 으로 저장
3. **웹 애플리케이션** 클라이언트 ID → 리포 시크릿 `PLATEN_WEB_CLIENT_ID` 로 등록 후 APK 재빌드, **Android** 클라이언트(패키지 `io.github.bigrirason_debug.platen` + 서명 SHA-1)도 생성
4. 각 앱의 설정 → 계정 → 로그인. 노트는 Drive 의 `Platen` 폴더에 `.platen` 파일로 올라간다.

## 빌드
```
./gradlew desktopTest                                   # 공통·데스크톱 테스트
./gradlew :app-desktop:run                              # 데스크톱 실행
./gradlew -Pplaten.android=true :app-android:assembleDebug   # APK (Android SDK 필요)
./gradlew :app-desktop:packageMsi                       # Windows 에서
```
구조: `core-model`(문서·`.platen` 포맷) · `core-ink`(스트로크 지오메트리) · `core-render`(Compose 렌더러) · `brush`(내장 브러시·임포터) · `pdf` · `sync`(Drive·OAuth) · `ui` · `app-desktop` · `app-android`.

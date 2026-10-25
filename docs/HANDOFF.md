# HANDOFF (항상 덮어쓰기)

- 시각: 2026-10-25 11:25 KST · 사용자 지시로 **여기서 중단** · head = Opus(D-025)
- 단계: ⑤ 구현 — 전 모듈 구현·통합 완료, CI 녹색화 진행 중에 멈춤

## 확인된 것(증거)
- 로컬 desktopTest: core-model 7 · core-ink 18 · brush 30 · sync 27 · pdf 8 · core-render 7 · ui 27 = **124건 통과**
- CI 런 25(`d6`… 커밋 538bedf 이전 상태): **JVM 테스트 잡 성공**, **Windows 잡 성공 → 산출물 `platen-windows`(MSI/EXE, 약 155MB) 업로드됨**, Android 잡 실패(compileSdk 37 요구)
- Android 실패 원인 3건은 모두 수정해 푸시함(플러그인 중복 적용 / 설정 접근자 / compileSdk 37). **수정 반영 런 28(dc74ffe)은 진행 중이었고 결과 미확인** — APK 가 나왔는지 Actions 에서 확인 필요

## 이번 세션 통합 내용
- UI ← brush(내장 9종·Procreate/ABR 임포트·노트 동봉), pdf(홈 "열기"로 PDF 가져오기·배경 렌더·내보내기), sync(데스크톱 루프백 / Android Credential Manager+AuthorizationClient, Drive `drive.file`, 내용 해시 dirty·충돌 사본)
- Android 젖은 잉크 프론트버퍼 오버레이(D-026, 설정 토글로 끄기 가능), undo z-순서 버그 수정

## 다음 행동(재개 시)
1. Actions 런 28 결과 확인 → Android 실패면 로그(`What went wrong`)대로 수정. 남은 위험: androidMain 코드(ui·sync·pdf·brush·app-android)는 한 번도 컴파일된 적 없음
2. 성공하면 `platen-android-debug-apk` 를 태블릿에 설치 → LOCAL_TODO ★·S1~S5
3. 후속: 릴리스 서명 키 고정, PDF 한글 폰트 임베드, 프론트버퍼 실측 후 조정

## 사용자 숙제
LOCAL_TODO ★(산출물 설치), G1~G4(동기화 켜기: `~/.platen/oauth-client.json`, 시크릿 `PLATEN_WEB_CLIENT_ID`)

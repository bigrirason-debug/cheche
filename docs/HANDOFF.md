# HANDOFF (항상 덮어쓰기)

- 시각: 2026-10-25 10:45 KST · 마감 12:50 KST · head = Opus(D-025)
- 단계: ⑤ 구현 — 모듈 전부 구현·통합 완료, **CI 녹색화 진행 중**
- 완료(로컬 테스트 증거, 컨테이너 desktopTest): core-model 7 · core-ink 18 · brush 30 · sync 27 · pdf 8 · core-render 7(로컬 CMP 1.8.2 고정 경로) · ui 27(통합 4 포함) = **124건 통과**
- 통합: UI ← brush(내장 9종·Procreate/ABR 임포트·노트 동봉), pdf(가져오기·내보내기), sync(데스크톱 루프백 / Android Credential Manager+AuthorizationClient, Drive `drive.file`, 내용 해시 dirty·충돌 사본)
- CI: `.github/workflows/build.yml` 3잡(JVM 테스트+목업 PNG / APK / Windows 테스트+MSI·EXE). Android·Compose 1.12.2 는 CI 에서만 컴파일됨
- 남은 일(우선순위): ① CI 녹색 + 산출물 링크 ② Android 프론트버퍼(graphics-core) 연결 — 현재 일반 Compose Canvas 경로 ③ 릴리스 서명 키 고정 ④ PDF 한글 폰트 임베드 ⑤ 실기기 판정(LOCAL_TODO S1~S5, F1)
- 사용자 숙제: LOCAL_TODO ★ 절(산출물 설치), G1~G4(동기화 켜기)

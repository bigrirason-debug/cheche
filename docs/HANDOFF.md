# HANDOFF (항상 덮어쓰기)

- 단계: ⑤ 구현 — **M2~M7 병렬 착수**(전면 auto, D-020). 마감: 2026-10-25 12:50 KST
- 완료: core-model(문서 모델·brush.json·설정·`.platen` 컨테이너 PSTK v1, 테스트 7건) · KMP 골격 · CI 3잡(JVM 테스트 / APK ubuntu / Windows msi+exe+render·ui 테스트) · PLAN v1.1(§12 카운실 반영) · COUNCIL-REVIEW.md
- 진행 중(서브에이전트, 모듈 소유 경계): core-ink / brush(로스터 9종·Procreate·ABR 임포터) / sync(Drive·루프백 OAuth·충돌 엔진) / pdf(PDFBox 가져오기·내보내기) / core-render(Compose 렌더러·다크 재매핑·타일 캐시) / ui+app-desktop+app-android(셸·설정·진단·DESIGN.md)
- 환경 제약: 컨테이너는 Google Maven·Android SDK 차단 → APK 는 GitHub Actions 에서만. 로컬은 desktopTest 만. 첫 CI 는 모듈 미완성으로 실패 예상(정상).
- head 다음 행동: 에이전트 결과 수신 → 통합 빌드(`./gradlew desktopTest`) → 실 구현을 ui 인터페이스(BrushRepository/SyncFacade/PdfFacade)에 연결 → CI 녹색 → 산출물(APK·msi) 링크를 HANDOFF 에 기록 → 디컴파일 유래 항목 삭제(feature-matrix §6) → LOCAL_TODO 갱신
- 사용자 숙제(LOCAL_TODO): G1~G4 OAuth 클라이언트(없으면 동기화는 로그인 단계에서 안내 메시지), S1~S5 실기기 측정, B1~B3 샘플

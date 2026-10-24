# 구현 에이전트 공통 규칙 (head 발행)

- 패키지: `io.github.bigrirason_debug.platen.<module>` (core-model → `.model`, core-ink → `.ink`, core-render → `.render`, brush → `.brush`, sync → `.sync`, pdf → `.pdf`, ui → `.ui`).
- 소스셋: `commonMain` = 순수 Kotlin(java.* 금지) · `jvmCommon` = 데스크톱+Android 공유(java.* 허용) · `desktopMain`/`androidMain` = actual. 테스트는 `commonTest`(순수) 또는 `desktopTest`(JVM). **androidMain 은 이 컨테이너에서 컴파일되지 않는다**(CI 전용) → 최소화하고 API 레퍼런스를 확신할 때만 쓴다.
- 빌드/테스트: `./gradlew :<module>:desktopTest --console=plain` (Gradle 락 때문에 다른 에이전트와 순차 실행됨 — 대기는 정상). 네트워크는 Maven 미러만 열려 있다. `-Pplaten.android=true` 금지(Google Maven 차단).
- 의존성: `gradle/libs.versions.toml` 에 있는 것만. 추가가 필요하면 코드에 넣지 말고 완료 보고에 "의존성 요청: 이름·버전·라이선스·이유"로 적는다.
- 금지: 타사 코드 복사(GPL/AGPL/CC-NC 는 열람도 참고 수준만), 디컴파일 유래 수치 사용, 다른 모듈 파일 수정(core-model API 변경이 필요하면 보고), git push.
- 커밋: 자기 모듈 디렉터리만 `git add <module> && git commit -m "<module>: ..."`. `index.lock` 충돌 시 2초 후 재시도(최대 5회). push 는 head 가 한다.
- 품질: 공개 API 에 KDoc 한 줄, 수치 상수에는 근거 주석(리서치 문서 절 번호). 테스트는 결정적(시드 고정). 성능 테스트는 soft(assert 대신 println 수치 + 상한 2배에서만 fail).
- 보고(10줄 이내): 구현 범위, 테스트 수/결과, 미구현·가정, 의존성 요청, 다른 모듈에 필요한 API 변경.

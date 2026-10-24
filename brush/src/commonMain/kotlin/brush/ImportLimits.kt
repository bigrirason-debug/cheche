package io.github.bigrirason_debug.platen.brush

/**
 * 임포터 방어 상한(docs/COUNCIL-REVIEW.md 좌석 4 Security "§4.4 임포트 방어 상한 표").
 * 사용자 파일은 전부 신뢰하지 않는 입력이다: zip 폭탄·경로 탐색·UID 순환·거대 헤더를 디코드 전에 끊는다.
 * 초과 시 [ImportLimitExceeded] 를 던지며, 호출자는 파일 단위로 중단한다(부분 복구는 ABR "팁만 복구" 모드만).
 */
object ImportLimits {
    /** zip 엔트리 수 상한(브러시셋 수백 개 × 파일 4개 이내). */
    const val ZIP_MAX_ENTRIES = 500
    /** 엔트리당 압축 해제 상한 바이트(2048² RGBA PNG ≈ 16MB 의 2배). */
    const val ZIP_MAX_ENTRY_BYTES = 32L * 1024 * 1024
    /** 파일 전체 압축 해제 상한 바이트. */
    const val ZIP_MAX_TOTAL_BYTES = 64L * 1024 * 1024
    /** 중첩 zip 허용 깊이(.brushset 안의 .brush 1단계만). */
    const val ZIP_MAX_NESTING = 1

    /** NSKeyedArchiver UID 치환 재귀 깊이(brush-formats §1.2 "순환 참조 방어"). */
    const val PLIST_MAX_DEPTH = 64
    /** `$objects` 배열 길이 상한(실파일 최대 Brush.archive 10 KB 관찰, §1.6 brushkit 통계). */
    const val PLIST_MAX_OBJECTS = 100_000

    /** ABR 팁/패턴 bounds 한 변 상한(brush-formats §7 "바운드 검증(양수·≤8192)"). */
    const val ABR_MAX_SIDE = 8192
    /** ABR 팁/패턴 면적 상한(px). */
    const val ABR_MAX_AREA = 64L * 1024 * 1024
    /** ABR samp/desc/patt 레코드 수 상한. */
    const val ABR_MAX_RECORDS = 4096

    /** PNG/JPEG 헤더 폭·높이 상한 — 디코드 전에 검사(brush-formats §3.5 3항). */
    const val IMAGE_MAX_SIDE = 4096

    /** 이미지 바이트 상한(엔트리 상한과 동일). */
    const val IMAGE_MAX_BYTES = ZIP_MAX_ENTRY_BYTES
}

/** 방어 상한 초과. 메시지에 어떤 상한인지와 관측값을 적는다. */
class ImportLimitExceeded(message: String) : RuntimeException(message)

/** 포맷 해석 실패(구조 오류). 호출자가 "팁만 복구" 또는 파일 단위 노트로 전환한다. */
class BrushFormatException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

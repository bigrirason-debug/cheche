package io.github.bigrirason_debug.platen.model

/** 문서 포맷 스키마 버전. 쓰기 버전과 읽기 최소 버전을 분리한다(PLAN §4.5). */
object PlatenSchema {
    const val WRITE_VERSION: Int = 1
    const val MIN_READER: Int = 1
    const val MIME: String = "application/vnd.platen.note+zip"
    const val EXTENSION: String = "platen"
}

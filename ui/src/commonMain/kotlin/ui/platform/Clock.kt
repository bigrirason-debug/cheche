package io.github.bigrirason_debug.platen.ui.platform

/** 날짜 표시(로컬 타임존). actual 은 jvmCommon(java.time). */
expect object PlatformClock {
    /** "2026-10-24 14:05" */
    fun formatDateTime(epochMillis: Long): String
}

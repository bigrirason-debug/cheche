package io.github.bigrirason_debug.platen.ui.platform

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

actual object PlatformClock {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    actual fun formatDateTime(epochMillis: Long): String =
        runCatching { fmt.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())) }.getOrDefault("—")
}

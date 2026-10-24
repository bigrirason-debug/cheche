package io.github.bigrirason_debug.platen.sync

/** 순수 Kotlin UTC 포맷터(commonMain 은 java.* 금지). 충돌 사본 이름 등 파일명 안전 문자열용. */
object UtcTime {
    /** epoch millis → `yyyyMMdd-HHmmss`(UTC). 파일명에 쓸 수 있도록 콜론 없음. */
    fun fileStamp(millis: Long): String {
        val days = floorDiv(millis, 86_400_000L)
        val msOfDay = millis - days * 86_400_000L
        val (y, m, d) = civilFromDays(days)
        val secOfDay = (msOfDay / 1000L).toInt()
        val h = secOfDay / 3600
        val mi = (secOfDay % 3600) / 60
        val s = secOfDay % 60
        return "${pad(y, 4)}${pad(m, 2)}${pad(d, 2)}-${pad(h, 2)}${pad(mi, 2)}${pad(s, 2)}"
    }

    private fun pad(v: Int, w: Int): String = v.toString().padStart(w, '0')

    private fun floorDiv(a: Long, b: Long): Long {
        val q = a / b
        return if ((a % b != 0L) && ((a < 0) != (b < 0))) q - 1 else q
    }

    /** Howard Hinnant 의 civil_from_days(공개 알고리즘). */
    private fun civilFromDays(z0: Long): Triple<Int, Int, Int> {
        val z = z0 + 719_468L
        val era = (if (z >= 0) z else z - 146_096L) / 146_097L
        val doe = z - era * 146_097L
        val yoe = (doe - doe / 1460L + doe / 36_524L - doe / 146_096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = if (mp < 10L) mp + 3L else mp - 9L
        val year = if (m <= 2L) y + 1L else y
        return Triple(year.toInt(), m.toInt(), d.toInt())
    }
}

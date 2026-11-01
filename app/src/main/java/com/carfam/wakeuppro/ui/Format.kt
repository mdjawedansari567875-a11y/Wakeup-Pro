package com.carfam.wakeuppro.ui

import com.carfam.wakeuppro.data.K
import java.util.Locale

fun fmtTime(h: Int, m: Int, is24: Boolean): String =
    if (is24) String.format(Locale.US, "%02d:%02d", h, m)
    else {
        val hh = if (h % 12 == 0) 12 else h % 12
        String.format(Locale.US, "%d:%02d %s", hh, m, if (h < 12) "AM" else "PM")
    }

private val DAY3 = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

fun daysText(mask: Int): String = when (mask) {
    0 -> "Once"
    K.EVERY_DAY -> "Every day"
    K.WEEKDAYS -> "Weekdays"
    K.WEEKENDS -> "Weekends"
    else -> listOf(1, 2, 3, 4, 5, 6, 0).filter { ((mask shr it) and 1) == 1 }.joinToString(" ") { DAY3[it] }
}

fun untilText(ms: Long): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0)
    val d = totalMin / 1440
    val h = (totalMin % 1440) / 60
    val m = totalMin % 60
    return buildString {
        if (d > 0) append("${d}d ")
        if (h > 0 || d > 0) append("${h}h ")
        append("${m}m")
    }
}

fun fmtStopwatch(ms: Long): String {
    val t = ms.coerceAtLeast(0)
    val h = t / 3_600_000
    val m = (t / 60_000) % 60
    val s = (t / 1000) % 60
    val milli = t % 1000
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d.%03d", h, m, s, milli)
    else String.format(Locale.US, "%02d:%02d.%03d", m, s, milli)
}

fun fmtHms(ms: Long): String {
    val total = (ms.coerceAtLeast(0) + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

package com.carfam.wakeuppro.data

object K {
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_KIND = "kind"
    const val EXTRA_PRACTICE = "practice"
    const val KIND_ALARM = "alarm"
    const val KIND_TIMER = "timer"
    const val ACTION_ALARM = "com.carfam.wakeuppro.ACTION_ALARM"
    const val ACTION_TIMER = "com.carfam.wakeuppro.ACTION_TIMER"
    const val ACTION_STOP = "com.carfam.wakeuppro.ACTION_STOP"
    const val TEST_ID = -99

    // Day bitmask: bit 0 = Sunday ... bit 6 = Saturday (Calendar.DAY_OF_WEEK - 1)
    const val EVERY_DAY = 127
    const val WEEKDAYS = 62   // Mon..Fri
    const val WEEKENDS = 65   // Sun + Sat
}

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Int,
    val enabled: Boolean,
    val label: String = ""
) {
    val repeats: Boolean get() = days != 0
}

data class AppSettings(
    val volume: Int = 100,
    val soundUri: String = "",
    val vibration: Boolean = true,
    val darkTheme: Boolean = true,
    val difficulty: Int = 2,        // 0 easy, 1 medium, 2 hard (default HARD)
    val is24h: Boolean = false,
    val challengeCount: Int = 3
)

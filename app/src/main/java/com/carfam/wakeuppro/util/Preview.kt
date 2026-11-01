package com.carfam.wakeuppro.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlin.math.roundToInt

/** Short alarm-sound preview used by the Settings screen. */
object Preview {
    private var current: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())

    fun play(ctx: Context, uri: String, volumePct: Int) {
        stop()
        try {
            val am = ctx.getSystemService(AudioManager::class.java)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, (max * volumePct / 100f).roundToInt().coerceIn(1, max), 0)
            val u: Uri = if (uri.isBlank()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else Uri.parse(uri)
            val r = RingtoneManager.getRingtone(ctx, u) ?: return
            r.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            r.play()
            current = r
            handler.postDelayed({ stop() }, 3000)
        } catch (_: Exception) {
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        try { current?.stop() } catch (_: Exception) {}
        current = null
    }

    /** Title to URI pairs of the alarm sounds installed on the phone. */
    fun alarmSounds(ctx: Context): List<Pair<String, String>> {
        val list = mutableListOf("Default alarm" to "")
        try {
            val rm = RingtoneManager(ctx)
            rm.setType(RingtoneManager.TYPE_ALARM)
            val c = rm.cursor
            while (c.moveToNext()) {
                list.add(c.getString(RingtoneManager.TITLE_COLUMN_INDEX) to rm.getRingtoneUri(c.position).toString())
            }
        } catch (_: Exception) {
        }
        return list
    }
}

package com.carfam.wakeuppro.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.carfam.wakeuppro.MainActivity
import com.carfam.wakeuppro.data.K
import com.carfam.wakeuppro.data.Store
import kotlin.math.roundToInt

/**
 * Foreground service that plays the alarm sound + vibration until it is stopped.
 * For alarms it is only stopped from AlarmRingActivity after all brain games are solved.
 * For timers it stops from the notification button / Timer screen / after 60 s.
 */
class AlarmService : Service() {

    companion object {
        @Volatile var ringing = false
        @Volatile var kind = K.KIND_ALARM
        const val CHANNEL = "wakeup_alarm"
        const val NOTIF_ID = 4711

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AlarmService::class.java))
        }
    }

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == K.ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val k = intent?.getStringExtra(K.EXTRA_KIND) ?: K.KIND_ALARM
        kind = k
        ringing = true

        val notif = buildNotification(k)
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notif,
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            )
        } catch (_: Exception) {
            // Restarted by the system in a state where foreground start is not allowed.
            ringing = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (wakeLock == null) {
            val pm = getSystemService(PowerManager::class.java)
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeuppro:ring").apply {
                acquire(60 * 60 * 1000L)
            }
        }
        startSound()
        startVibration()

        handler.removeCallbacksAndMessages(null)
        if (k == K.KIND_TIMER) {
            handler.postDelayed({ stopSelf() }, 60_000L)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        vibrator?.cancel()
        vibrator = null
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) {}
        wakeLock = null
        ringing = false
        super.onDestroy()
    }

    // ---------------- Sound ----------------

    private fun candidateUris(custom: String): List<Uri> {
        val list = mutableListOf<Uri>()
        if (custom.isNotBlank()) list.add(Uri.parse(custom))
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { list.add(it) }
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let { list.add(it) }
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { list.add(it) }
        return list
    }

    private fun startSound() {
        if (player != null) return
        val s = Store.get(this).settings()
        val am = getSystemService(AudioManager::class.java)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val vol = (max * (s.volume / 100f)).roundToInt().coerceIn(1, max)
        try { am.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0) } catch (_: Exception) {}

        for (uri in candidateUris(s.soundUri)) {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(this, uri)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                player = mp
                return
            } catch (_: Exception) {
                // try next candidate
            }
        }
    }

    private fun startVibration() {
        if (!Store.get(this).settings().vibration) return
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator = v
        v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 900, 500, 900, 500), 0))
    }

    // ---------------- Notification ----------------

    private fun buildNotification(k: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "Alarms & timers", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = "Alarm and timer alerts"
            }
            nm.createNotificationChannel(ch)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        if (k == K.KIND_ALARM) {
            val ring = Intent(this, AlarmRingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pi = PendingIntent.getActivity(this, 1, ring, flags)
            b.setContentTitle("WAKEUP PRO - Alarm")
                .setContentText("Solve the brain challenge to stop the alarm")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
        } else {
            val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), flags)
            val stop = PendingIntent.getService(
                this, 3,
                Intent(this, AlarmService::class.java).setAction(K.ACTION_STOP), flags
            )
            b.setContentTitle("Timer finished")
                .setContentText("Time is up!")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(open)
                .addAction(0, "Stop", stop)
        }
        return b.build()
    }
}

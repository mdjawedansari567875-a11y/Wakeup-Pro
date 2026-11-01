package com.carfam.wakeuppro.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.carfam.wakeuppro.MainActivity
import com.carfam.wakeuppro.data.Alarm
import com.carfam.wakeuppro.data.K
import com.carfam.wakeuppro.data.Store
import java.util.Calendar

object AlarmScheduler {

    private const val TIMER_REQ = 424242
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** Next time (epoch millis) this alarm should ring, strictly after [now]. */
    fun nextTrigger(a: Alarm, now: Long = System.currentTimeMillis()): Long {
        val base = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, a.hour)
            set(Calendar.MINUTE, a.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (a.days == 0) {
            if (base.timeInMillis <= now) base.add(Calendar.DAY_OF_YEAR, 1)
            return base.timeInMillis
        }
        for (i in 0..7) {
            val c = base.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, i)
            val bit = c.get(Calendar.DAY_OF_WEEK) - 1
            if (((a.days shr bit) and 1) == 1 && c.timeInMillis > now) return c.timeInMillis
        }
        return base.timeInMillis + 7 * 86_400_000L
    }

    private fun alarmPi(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .setAction(K.ACTION_ALARM)
            .putExtra(K.EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, id + 1000, i, FLAGS)
    }

    private fun showPi(ctx: Context): PendingIntent =
        PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), FLAGS)

    /** setAlarmClock = exact, Doze-exempt, and needs no special exact-alarm permission. */
    fun schedule(ctx: Context, a: Alarm) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(nextTrigger(a), showPi(ctx)), alarmPi(ctx, a.id))
    }

    fun cancel(ctx: Context, a: Alarm) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmPi(ctx, a.id))
    }

    fun rescheduleAll(ctx: Context) {
        Store.get(ctx).alarms().forEach { if (it.enabled) schedule(ctx, it) else cancel(ctx, it) }
    }

    /** Next enabled alarm and its trigger time, or null. */
    fun nextAlarm(ctx: Context): Pair<Alarm, Long>? =
        Store.get(ctx).alarms().filter { it.enabled }
            .map { it to nextTrigger(it) }
            .minByOrNull { it.second }

    /** Rings the full alarm (sound + challenges) in ~10 seconds, for testing. */
    fun scheduleTest(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmPi(ctx, K.TEST_ID)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 10_000L, showPi(ctx)), pi)
    }

    // ---------------- Timer ----------------

    private fun timerPi(ctx: Context): PendingIntent {
        val i = Intent(ctx, TimerReceiver::class.java).setAction(K.ACTION_TIMER)
        return PendingIntent.getBroadcast(ctx, TIMER_REQ, i, FLAGS)
    }

    fun scheduleTimer(ctx: Context, millisFromNow: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(System.currentTimeMillis() + millisFromNow, showPi(ctx)),
            timerPi(ctx)
        )
    }

    fun cancelTimer(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(timerPi(ctx))
    }
}

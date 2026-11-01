package com.carfam.wakeuppro.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.carfam.wakeuppro.data.K
import com.carfam.wakeuppro.data.Store

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(K.EXTRA_ID, -1)
        if (id != K.TEST_ID) {
            val store = Store.get(ctx)
            val a = store.alarm(id) ?: return
            if (!a.enabled) return
            // Repeating alarms re-arm themselves; one-shot alarms switch off after ringing.
            if (a.repeats) AlarmScheduler.schedule(ctx, a) else store.upsert(a.copy(enabled = false))
        }
        val svc = Intent(ctx, AlarmService::class.java)
            .putExtra(K.EXTRA_ID, id)
            .putExtra(K.EXTRA_KIND, K.KIND_ALARM)
        ContextCompat.startForegroundService(ctx, svc)
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Store.get(ctx).putStr("tm_state", "done")
        val svc = Intent(ctx, AlarmService::class.java)
            .putExtra(K.EXTRA_KIND, K.KIND_TIMER)
        ContextCompat.startForegroundService(ctx, svc)
    }
}

/** Restores every alarm after reboot / app update / clock or timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(ctx)
    }
}

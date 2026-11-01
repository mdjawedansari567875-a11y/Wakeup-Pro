package com.carfam.wakeuppro

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.carfam.wakeuppro.alarm.AlarmRingActivity
import com.carfam.wakeuppro.alarm.AlarmScheduler
import com.carfam.wakeuppro.alarm.AlarmService
import com.carfam.wakeuppro.data.K
import com.carfam.wakeuppro.data.Store
import com.carfam.wakeuppro.ui.AppRoot
import com.carfam.wakeuppro.ui.WakeTheme
import com.carfam.wakeuppro.util.Perms

class MainActivity : ComponentActivity() {

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store.get(this)

        if (Build.VERSION.SDK_INT >= 33 && !Perms.notifications(this)) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Make sure every enabled alarm is armed (cheap, and fixes anything lost by the system).
        AlarmScheduler.rescheduleAll(this)

        setContent {
            var settings by remember { mutableStateOf(store.settings()) }
            WakeTheme(settings.darkTheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(store, settings) {
                        settings = it
                        store.saveSettings(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // An alarm is ringing: the only way forward is through the brain challenge.
        if (AlarmService.ringing && AlarmService.kind == K.KIND_ALARM) {
            startActivity(Intent(this, AlarmRingActivity::class.java))
        }
    }
}

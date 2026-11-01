package com.carfam.wakeuppro.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.carfam.wakeuppro.data.K
import com.carfam.wakeuppro.data.Store
import com.carfam.wakeuppro.games.ChallengeHost
import com.carfam.wakeuppro.ui.WakeTheme

/**
 * Full-screen alarm UI shown over the lock screen. The back button is disabled and there is
 * no stop button until every brain game has been solved.
 */
class AlarmRingActivity : ComponentActivity() {

    private var practice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        practice = intent.getBooleanExtra(K.EXTRA_PRACTICE, false)

        // Nothing is ringing and this is not practice -> nothing to solve.
        if (!practice && !(AlarmService.ringing && AlarmService.kind == K.KIND_ALARM)) {
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (practice) finish() // alarm: back is ignored
            }
        })

        val store = Store.get(this)
        val settings = store.settings()
        setContent {
            WakeTheme(settings.darkTheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ChallengeHost(
                        settings = settings,
                        store = store,
                        practice = practice,
                        onAlarmOff = {
                            if (!practice) AlarmService.stop(this@AlarmRingActivity)
                        },
                        onStop = {
                            if (!practice) AlarmService.stop(this@AlarmRingActivity)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

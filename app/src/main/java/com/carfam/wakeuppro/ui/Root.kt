package com.carfam.wakeuppro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carfam.wakeuppro.data.AppSettings
import com.carfam.wakeuppro.data.Store

@Composable
fun AppRoot(store: Store, settings: AppSettings, onSettings: (AppSettings) -> Unit) {
    var screen by remember { mutableStateOf("main") }
    BackHandler(enabled = screen != "main") {
        screen = if (screen == "support") "settings" else "main"
    }
    when (screen) {
        "settings" -> SettingsScreen(
            settings = settings,
            onChange = onSettings,
            onBack = { screen = "main" },
            onSupport = { screen = "support" }
        )
        "support" -> SupportScreen(onBack = { screen = "settings" })
        else -> MainScreen(store, settings, onOpenSettings = { screen = "settings" })
    }
}

@Composable
private fun MainScreen(store: Store, settings: AppSettings, onOpenSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        containerColor = cs.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WAKEUP PRO", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = cs.onBackground)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = cs.surface) {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Alarm, contentDescription = null) }, label = { Text("Alarm") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Timer, contentDescription = null) }, label = { Text("Stopwatch") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.HourglassBottom, contentDescription = null) }, label = { Text("Timer") }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> AlarmTab(store, settings)
                1 -> StopwatchTab(store)
                else -> TimerTab(store)
            }
        }
    }
}

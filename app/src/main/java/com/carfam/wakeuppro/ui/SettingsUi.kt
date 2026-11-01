package com.carfam.wakeuppro.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carfam.wakeuppro.alarm.AlarmRingActivity
import com.carfam.wakeuppro.alarm.AlarmScheduler
import com.carfam.wakeuppro.data.AppSettings
import com.carfam.wakeuppro.data.K
import com.carfam.wakeuppro.util.Preview
import kotlin.math.roundToInt

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = cs.primary)
            content()
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = cs.onBackground)
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onSupport: () -> Unit
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var showSounds by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val sounds = remember { Preview.alarmSounds(ctx) }
    val soundName = sounds.firstOrNull { it.second == settings.soundUri }?.first ?: "Default alarm"

    DisposableEffect(Unit) { onDispose { Preview.stop() } }

    Column(Modifier.fillMaxSize()) {
        TopBar("Settings", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Section("ALARM VOLUME") {
                Slider(
                    value = settings.volume.toFloat(),
                    onValueChange = { onChange(settings.copy(volume = it.roundToInt().coerceIn(10, 100))) },
                    valueRange = 10f..100f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${settings.volume}%", fontWeight = FontWeight.Bold, color = cs.onSurface)
                    OutlinedButton(onClick = { Preview.play(ctx, settings.soundUri, settings.volume) }) { Text("Preview") }
                }
            }
            Section("ALARM SOUND") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(soundName, color = cs.onSurface, modifier = Modifier.weight(1f))
                    Button(onClick = { showSounds = true }) { Text("Change") }
                }
            }
            Section("VIBRATION") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Vibrate when the alarm rings", color = cs.onSurface)
                    Switch(checked = settings.vibration, onCheckedChange = { onChange(settings.copy(vibration = it)) })
                }
            }
            Section("THEME") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = settings.darkTheme, onClick = { onChange(settings.copy(darkTheme = true)) }, label = { Text("Dark") })
                    FilterChip(selected = !settings.darkTheme, onClick = { onChange(settings.copy(darkTheme = false)) }, label = { Text("Light") })
                }
            }
            Section("DEFAULT GAME DIFFICULTY") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Easy", "Medium", "Hard").forEachIndexed { i, l ->
                        FilterChip(selected = settings.difficulty == i, onClick = { onChange(settings.copy(difficulty = i)) }, label = { Text(l) })
                    }
                }
                Text(
                    "Difficulty also adapts a little to how well you do.",
                    fontSize = 13.sp, color = cs.onSurfaceVariant
                )
            }
            Section("CLOCK FORMAT") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !settings.is24h, onClick = { onChange(settings.copy(is24h = false)) }, label = { Text("12-hour") })
                    FilterChip(selected = settings.is24h, onClick = { onChange(settings.copy(is24h = true)) }, label = { Text("24-hour") })
                }
            }
            Section("BRAIN CHALLENGES PER ALARM") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 5).forEach { n ->
                        FilterChip(
                            selected = settings.challengeCount == n,
                            onClick = { onChange(settings.copy(challengeCount = n)) },
                            label = { Text(if (n == 3) "3 (default)" else "$n") }
                        )
                    }
                }
            }
            Section("TEST") {
                Button(
                    onClick = {
                        ctx.startActivity(
                            Intent(ctx, AlarmRingActivity::class.java)
                                .putExtra(K.EXTRA_PRACTICE, true)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Practice a brain challenge") }
                OutlinedButton(
                    onClick = {
                        AlarmScheduler.scheduleTest(ctx)
                        Toast.makeText(ctx, "Test alarm rings in 10 seconds - lock the phone to test", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ring a real test alarm in 10 s") }
            }
            Section("ABOUT") {
                OutlinedButton(onClick = onSupport, modifier = Modifier.fillMaxWidth()) { Text("Contact Support") }
                OutlinedButton(onClick = { showInfo = true }, modifier = Modifier.fillMaxWidth()) { Text("App Information") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSounds) {
        AlertDialog(
            onDismissRequest = { Preview.stop(); showSounds = false },
            title = { Text("Alarm sound") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    sounds.forEach { (title, uri) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                onChange(settings.copy(soundUri = uri))
                                Preview.play(ctx, uri, settings.volume)
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (uri == settings.soundUri) "●" else "○", color = cs.primary)
                            Spacer(Modifier.size(12.dp))
                            Text(title)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { Preview.stop(); showSounds = false }) { Text("Done") } }
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("WAKEUP PRO 1.0") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Smart alarm clock with 28 brain-training games. Every alarm picks different games and brand-new puzzles; you must solve them all to stop the alarm.")
                    Text("Reliability notes", fontWeight = FontWeight.Bold)
                    Text(
                        "• Alarms use Android's exact alarm-clock API and are restored after a reboot.\n" +
                            "• If you force-stop the app from Settings, Android cancels its alarms until you open it again. No app can prevent this.\n" +
                            "• Some brands (Xiaomi, Oppo, Vivo, Realme, Samsung, OnePlus) kill background apps. Set battery to Unrestricted and enable Autostart for WAKEUP PRO.\n" +
                            "• Full-screen alarm over the lock screen needs the notification and full-screen permissions."
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } }
        )
    }
}

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val email = "klariumai@gmail.com"
    fun sendMail() {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                    .putExtra(Intent.EXTRA_SUBJECT, "WAKEUP PRO Support")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Toast.makeText(ctx, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("Contact Support", onBack)
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.size(96.dp).clip(CircleShape).background(AccentBrush),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { Text("⏰", fontSize = 48.sp) }
            Text("WAKEUP PRO", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
            Text("Questions, bugs or ideas? Get in touch.", color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("DEVELOPER NAME", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary)
                        Text("Carfam", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                    }
                    Column {
                        Text("CONTACT EMAIL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary)
                        Text(
                            email, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ElectricBlue,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { sendMail() }.padding(vertical = 4.dp)
                        )
                    }
                }
            }
            Button(onClick = { sendMail() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text("Send an email", fontWeight = FontWeight.Bold)
            }
        }
    }
}

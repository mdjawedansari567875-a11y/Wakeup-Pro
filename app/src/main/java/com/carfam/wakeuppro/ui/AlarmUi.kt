package com.carfam.wakeuppro.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carfam.wakeuppro.alarm.AlarmScheduler
import com.carfam.wakeuppro.data.Alarm
import com.carfam.wakeuppro.data.AppSettings
import com.carfam.wakeuppro.data.K
import com.carfam.wakeuppro.data.Store
import com.carfam.wakeuppro.util.Perms
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AlarmTab(store: Store, settings: AppSettings) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var alarms by remember { mutableStateOf(store.alarms()) }
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1000)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Perms.openNotificationSettings(ctx)
    }

    val locale = Locale.getDefault()
    val timeFmt = remember(settings.is24h) { SimpleDateFormat(if (settings.is24h) "HH:mm" else "hh:mm", locale) }
    val secFmt = remember { SimpleDateFormat("ss", locale) }
    val ampmFmt = remember { SimpleDateFormat("a", locale) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMMM yyyy", locale) }
    val nowDate = Date(tick)
    val next = remember(alarms, tick / 30_000) { AlarmScheduler.nextAlarm(ctx) }

    fun toast(a: Alarm) {
        val ms = AlarmScheduler.nextTrigger(a) - System.currentTimeMillis()
        Toast.makeText(ctx, "Alarm set for ${untilText(ms)} from now", Toast.LENGTH_SHORT).show()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(timeFmt.format(nowDate), fontSize = 76.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.padding(bottom = 14.dp)) {
                            Text(secFmt.format(nowDate), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                            if (!settings.is24h) Text(ampmFmt.format(nowDate), fontSize = 18.sp, color = cs.onSurfaceVariant)
                        }
                    }
                    Text(dateFmt.format(nowDate), fontSize = 16.sp, color = cs.onSurfaceVariant)
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("NEXT ALARM", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary)
                        Spacer(Modifier.height(4.dp))
                        if (next == null) {
                            Text("No alarm scheduled", fontSize = 18.sp, color = cs.onSurface)
                        } else {
                            val (a, at) = next
                            val dayName = SimpleDateFormat("EEE", locale).format(Date(at))
                            Text(
                                "$dayName  ${fmtTime(a.hour, a.minute, settings.is24h)}",
                                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = cs.onSurface
                            )
                            Text("rings in ${untilText(at - tick)}", color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                val needNotif = !Perms.notifications(ctx)
                val needFull = !Perms.fullScreen(ctx)
                val needBattery = !Perms.batteryUnrestricted(ctx)
                if (needNotif || needFull || needBattery) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠ Allow these so the alarm can wake you", fontWeight = FontWeight.Bold, color = cs.onSurface)
                            if (needNotif) {
                                Button(onClick = {
                                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }, modifier = Modifier.fillMaxWidth()) { Text("Allow notifications") }
                            }
                            if (needFull) {
                                Button(onClick = { Perms.openFullScreenSettings(ctx) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Allow full-screen alarm")
                                }
                            }
                            if (needBattery) {
                                Button(onClick = { Perms.requestBatteryUnrestricted(ctx) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Disable battery optimisation")
                                }
                            }
                        }
                    }
                }
            }
            if (alarms.isEmpty()) {
                item {
                    Text(
                        "No alarms yet.\nTap + to add your first alarm.",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center, color = cs.onSurfaceVariant, fontSize = 16.sp
                    )
                }
            }
            items(alarms, key = { it.id }) { a ->
                Card(
                    Modifier.fillMaxWidth().clickable { editing = a; showEditor = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                fmtTime(a.hour, a.minute, settings.is24h),
                                fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (a.enabled) cs.onSurface else cs.onSurfaceVariant
                            )
                            Text(
                                daysText(a.days) + if (a.label.isNotBlank()) "  ·  ${a.label}" else "",
                                color = cs.onSurfaceVariant, fontSize = 14.sp
                            )
                        }
                        Switch(checked = a.enabled, onCheckedChange = { on ->
                            val n = a.copy(enabled = on)
                            store.upsert(n)
                            if (on) { AlarmScheduler.schedule(ctx, n); toast(n) } else AlarmScheduler.cancel(ctx, n)
                            alarms = store.alarms()
                        })
                        IconButton(onClick = {
                            AlarmScheduler.cancel(ctx, a)
                            store.delete(a.id)
                            alarms = store.alarms()
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = cs.onSurfaceVariant) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = null; showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = ElectricBlue,
            contentColor = Color.White
        ) { Icon(Icons.Filled.Add, contentDescription = "Add alarm") }
    }

    if (showEditor) {
        AlarmEditor(
            initial = editing,
            is24h = settings.is24h,
            onDismiss = { showEditor = false },
            onSave = { saved ->
                val a = if (saved.id == 0) saved.copy(id = store.nextId()) else saved
                store.upsert(a)
                AlarmScheduler.schedule(ctx, a)
                alarms = store.alarms()
                showEditor = false
                toast(a)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditor(initial: Alarm?, is24h: Boolean, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val now = Calendar.getInstance()
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: now.get(Calendar.HOUR_OF_DAY),
        initialMinute = initial?.minute ?: now.get(Calendar.MINUTE),
        is24Hour = is24h
    )
    var days by remember { mutableIntStateOf(initial?.days ?: 0) }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S") // index = bit (0 = Sunday)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = cs.surface, modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(if (initial == null) "New alarm" else "Edit alarm", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurface)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) }

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Once" to 0, "Every day" to K.EVERY_DAY, "Weekdays" to K.WEEKDAYS, "Weekends" to K.WEEKENDS).forEach { (l, m) ->
                        FilterChip(selected = days == m, onClick = { days = m }, label = { Text(l) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (bit in listOf(1, 2, 3, 4, 5, 6, 0)) {
                        val on = ((days shr bit) and 1) == 1
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (on) cs.primary else cs.surfaceVariant)
                                .clickable { days = days xor (1 shl bit) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dayLetters[bit], fontWeight = FontWeight.Bold, color = if (on) Color.White else cs.onSurface)
                        }
                    }
                }
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(30) },
                    label = { Text("Label (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.size(8.dp))
                    Button(onClick = {
                        onSave(Alarm(initial?.id ?: 0, state.hour, state.minute, days, true, label.trim()))
                    }) { Text("Save") }
                }
            }
        }
    }
}

package com.carfam.wakeuppro.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carfam.wakeuppro.alarm.AlarmScheduler
import com.carfam.wakeuppro.alarm.AlarmService
import com.carfam.wakeuppro.data.Store
import kotlinx.coroutines.delay

// ============================== STOPWATCH ==============================

@Composable
fun StopwatchTab(store: Store) {
    val cs = MaterialTheme.colorScheme
    val boot = SystemClock.elapsedRealtime()
    // After a reboot elapsedRealtime restarts from 0, so a saved start in the "future" is invalid.
    val valid = !(store.getBool("sw_run") && store.getLong("sw_start") > boot)
    var running by remember { mutableStateOf(valid && store.getBool("sw_run")) }
    var acc by remember { mutableLongStateOf(if (valid) store.getLong("sw_acc") else 0L) }
    var startAt by remember { mutableLongStateOf(if (valid) store.getLong("sw_start") else 0L) }
    var laps by remember {
        mutableStateOf(
            if (valid) store.getStr("sw_laps").split(",").filter { it.isNotEmpty() }.map { it.toLong() }
            else emptyList()
        )
    }
    var now by remember { mutableLongStateOf(boot) }

    fun persist() {
        store.putBool("sw_run", running)
        store.putLong("sw_acc", acc)
        store.putLong("sw_start", startAt)
        store.putStr("sw_laps", laps.joinToString(","))
    }

    LaunchedEffect(running) {
        while (running) {
            now = SystemClock.elapsedRealtime()
            delay(33)
        }
        now = SystemClock.elapsedRealtime()
    }
    val elapsed = acc + if (running) now - startAt else 0L

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        Text(
            fmtStopwatch(elapsed), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace, color = cs.onBackground
        )
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (running) {
                OutlinedButton(
                    onClick = { laps = laps + elapsed; persist() },
                    modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(18.dp)
                ) { Text("LAP", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            } else {
                OutlinedButton(
                    onClick = { acc = 0L; laps = emptyList(); persist() },
                    enabled = elapsed > 0L,
                    modifier = Modifier.weight(1f).height(60.dp), shape = RoundedCornerShape(18.dp)
                ) { Text("RESET", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
            PrimaryButton(
                if (running) "PAUSE" else if (elapsed > 0L) "RESUME" else "START",
                modifier = Modifier.weight(1f),
                color = if (running) Bad else Good
            ) {
                if (running) {
                    acc += SystemClock.elapsedRealtime() - startAt
                    running = false
                } else {
                    startAt = SystemClock.elapsedRealtime()
                    running = true
                }
                persist()
            }
        }
        Spacer(Modifier.height(18.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(laps.reversed()) { revIdx, total ->
                val i = laps.size - 1 - revIdx
                val lapTime = total - (if (i > 0) laps[i - 1] else 0L)
                Row(
                    Modifier.fillMaxWidth().background(cs.surfaceVariant, RoundedCornerShape(14.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap ${i + 1}", fontWeight = FontWeight.Bold, color = cs.onSurface)
                    Text(fmtStopwatch(lapTime), fontFamily = FontFamily.Monospace, color = cs.onSurface)
                    Text(fmtStopwatch(total), fontFamily = FontFamily.Monospace, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

// ============================== TIMER ==============================

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TimerTab(store: Store) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val boot = SystemClock.elapsedRealtime()

    var state by remember { mutableStateOf(store.getStr("tm_state", "idle")) }
    var total by remember { mutableLongStateOf(store.getLong("tm_total")) }
    var end by remember { mutableLongStateOf(store.getLong("tm_end")) }
    var left by remember { mutableLongStateOf(store.getLong("tm_left")) }
    var h by rememberSaveable { mutableStateOf(0) }
    var m by rememberSaveable { mutableStateOf(5) }
    var s by rememberSaveable { mutableStateOf(0) }
    var now by remember { mutableLongStateOf(boot) }

    // Saved running timer that is impossible (e.g. after a reboot) -> reset it.
    LaunchedEffect(Unit) {
        if (state == "running" && end - boot > total + 5000) {
            state = "idle"
            store.putStr("tm_state", "idle")
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            if (state == "running" && store.getStr("tm_state") == "done") state = "done"
            delay(100)
        }
    }

    fun persist() {
        store.putStr("tm_state", state)
        store.putLong("tm_total", total)
        store.putLong("tm_end", end)
        store.putLong("tm_left", left)
    }

    val finished = state == "done" || (state == "running" && end - now <= 0)
    val remaining = when {
        finished -> 0L
        state == "running" -> end - now
        state == "paused" -> left
        else -> h * 3_600_000L + m * 60_000L + s * 1000L
    }
    val idle = state == "idle"
    val progress = if (idle || total <= 0L) 1f else (remaining / total.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        if (idle) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stepper(h, 99, "HOURS") { h = it }
                Stepper(m, 59, "MIN") { m = it }
                Stepper(s, 59, "SEC") { s = it }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1 min" to 60, "5 min" to 300, "10 min" to 600, "15 min" to 900).forEach { (label, secs) ->
                    AssistChip(onClick = { h = secs / 3600; m = (secs % 3600) / 60; s = secs % 60 }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(30.dp))
            Text(fmtHms(remaining), fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, color = cs.onBackground)
        } else {
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                val track = cs.surfaceVariant
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(
                        brush = Brush.linearGradient(listOf(ElectricBlue, Violet)),
                        startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        fmtHms(remaining), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace, color = if (finished) Bad else cs.onBackground
                    )
                    if (finished) Text("TIME'S UP!", fontWeight = FontWeight.ExtraBold, color = Bad, fontSize = 18.sp)
                    if (state == "paused") Text("PAUSED", color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                finished -> PrimaryButton("STOP", color = Bad) {
                    AlarmService.stop(ctx)
                    AlarmScheduler.cancelTimer(ctx)
                    state = "idle"
                    persist()
                }
                idle -> PrimaryButton("START", color = Good) {
                    val ms = h * 3_600_000L + m * 60_000L + s * 1000L
                    if (ms <= 0L) {
                        Toast.makeText(ctx, "Set a time first", Toast.LENGTH_SHORT).show()
                    } else {
                        total = ms
                        end = SystemClock.elapsedRealtime() + ms
                        state = "running"
                        AlarmScheduler.scheduleTimer(ctx, ms)
                        persist()
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = {
                            AlarmScheduler.cancelTimer(ctx)
                            state = "idle"
                            persist()
                        },
                        modifier = Modifier.weight(1f).height(64.dp), shape = RoundedCornerShape(20.dp)
                    ) { Text("RESET", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    if (state == "running") {
                        PrimaryButton("PAUSE", modifier = Modifier.weight(1f), color = Bad) {
                            left = (end - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                            AlarmScheduler.cancelTimer(ctx)
                            state = "paused"
                            persist()
                        }
                    } else {
                        PrimaryButton("RESUME", modifier = Modifier.weight(1f), color = Good) {
                            end = SystemClock.elapsedRealtime() + left
                            AlarmScheduler.scheduleTimer(ctx, left)
                            state = "running"
                            persist()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, max: Int, label: String, onChange: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onChange(if (value >= max) 0 else value + 1) }) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "More", tint = cs.primary, modifier = Modifier.size(36.dp))
        }
        Text(
            String.format(java.util.Locale.US, "%02d", value),
            fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground
        )
        IconButton(onClick = { onChange(if (value <= 0) max else value - 1) }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Less", tint = cs.primary, modifier = Modifier.size(36.dp))
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
    }
}

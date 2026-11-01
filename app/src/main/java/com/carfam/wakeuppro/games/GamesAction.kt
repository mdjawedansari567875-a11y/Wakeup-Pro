package com.carfam.wakeuppro.games

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carfam.wakeuppro.ui.AccentBrush
import com.carfam.wakeuppro.ui.Bad
import com.carfam.wakeuppro.ui.ElectricBlue
import com.carfam.wakeuppro.ui.Good
import com.carfam.wakeuppro.ui.PrimaryButton
import com.carfam.wakeuppro.ui.Tile
import com.carfam.wakeuppro.ui.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Reaction & attention test: wait for green, tap fast, never tap on red. */
@Composable
fun ReactionGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val delays = p.items.map { it.toLong() }
    val limit = p.ints[0].toLong()
    var round by remember { mutableIntStateOf(0) }
    var state by remember { mutableIntStateOf(0) } // 0 = wait (red), 1 = go (green)
    var goAt by remember { mutableLongStateOf(0L) }
    var last by remember { mutableLongStateOf(0L) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(round) {
        state = 0
        delay(delays[round])
        goAt = SystemClock.elapsedRealtime()
        state = 1
        delay(limit)
        if (!done) { done = true; onResult(false) }
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Round ${round + 1} / ${delays.size}", color = cs.onSurfaceVariant)
        Box(
            Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(28.dp))
                .background(if (state == 0) Color(0xFFE53935) else Color(0xFF22C55E))
                .clickable {
                    if (!done) {
                        if (state == 0) {
                            done = true
                            onResult(false)
                        } else {
                            last = SystemClock.elapsedRealtime() - goAt
                            if (round == delays.size - 1) { done = true; onResult(true) } else round++
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(if (state == 0) "WAIT…" else "TAP NOW!", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        if (last > 0) Text("Last reaction: $last ms", color = cs.onSurfaceVariant)
    }
}

private val TAP_BRUSHES = listOf(
    listOf(Color(0xFF3D7BFF), Color(0xFF8B5CF6)),
    listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
)

/** Tap challenge: N taps before the countdown ends. */
@Composable
fun TapGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val need = p.ints[0]
    val secs = p.ints[1]
    val colors = TAP_BRUSHES[p.ints[2] % TAP_BRUSHES.size]
    var left by remember { mutableIntStateOf(need) }
    var progress by remember { mutableFloatStateOf(1f) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val total = secs * 1000
        var t = total
        while (t > 0 && !done) {
            delay(50)
            t -= 50
            progress = t / total.toFloat()
        }
        if (!done) { done = true; onResult(false) }
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("$left", fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
        Text("TAPS LEFT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.primary)
        LinearProgressIndicator(
            progress = { progress },
            color = if (progress < 0.3f) Bad else cs.primary,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
        )
        Box(
            Modifier.padding(top = 10.dp).size(230.dp).clip(CircleShape)
                .background(Brush.linearGradient(colors))
                .clickable {
                    if (!done) {
                        left--
                        if (left <= 0) { done = true; onResult(true) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("TAP!", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

/** Shake to dismiss: uses the accelerometer. Falls back to tapping on devices without one. */
@Composable
fun ShakeGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val need = p.ints[0]
    var count by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    fun hit() {
        if (done) return
        count++
        if (count >= need) { done = true; onResult(true) }
    }

    DisposableEffect(Unit) {
        var lastHit = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
                if (g > 20f) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastHit > 120) {
                        lastHit = now
                        hit()
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    val track = cs.surfaceVariant
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    Color(0xFFFFE14D), -90f, 360f * count / need.toFloat(), false,
                    Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📳", fontSize = 40.sp)
                Text("$count / $need", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
            }
        }
        Text("SHAKE TO DISMISS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
        Text("Keep shaking your phone vigorously", color = cs.onSurfaceVariant)
        if (sensor == null) {
            PrimaryButton("No motion sensor - TAP to shake") { hit() }
        }
    }
}

private class PanelTask(val kind: String, val name: String, val target: String, val opts: List<String>)

/** Control panel: configure every system to match the target, one task at a time. */
@Composable
fun ControlPanelGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val tasks = remember(p.id) {
        p.items.map {
            val x = it.split("|")
            PanelTask(x[0], x[1], x[2], x.getOrNull(3)?.split(",") ?: emptyList())
        }
    }
    var cur by remember { mutableIntStateOf(0) }
    var toggle by remember(cur) { mutableStateOf<Boolean?>(null) }
    var number by remember(cur) { mutableIntStateOf(50) }
    var choice by remember(cur) { mutableStateOf<String?>(null) }
    var message by remember(cur) { mutableStateOf("") }
    val t = tasks[cur]

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("TARGET CONFIGURATION", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = cs.primary)
            tasks.forEachIndexed { i, task ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        (if (i < cur) "✔ " else if (i == cur) "▶ " else "• ") + task.name,
                        fontWeight = if (i == cur) FontWeight.Bold else FontWeight.Normal,
                        color = if (i < cur) Good else cs.onSurface
                    )
                    Text(task.target, fontWeight = FontWeight.Bold, color = cs.onSurface)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CURRENT CONSOLE", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = cs.primary)
                Text("Task ${cur + 1} of ${tasks.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = cs.onSurface)
            }
            Text("Set ${t.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
            when (t.kind) {
                "T" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(true to "ON", false to "OFF").forEach { (v, label) ->
                        Tile(
                            Modifier.weight(1f).height(60.dp),
                            bg = if (toggle == v) cs.primary else cs.background,
                            onClick = { toggle = v }
                        ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (toggle == v) Color.White else cs.onSurface) }
                    }
                }
                "V" -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tile(Modifier.weight(1f).height(60.dp), bg = cs.background, onClick = { number = (number - 10).coerceAtLeast(0) }) {
                        Text("− 10", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = cs.onSurface)
                    }
                    Text("$number", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurface, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f))
                    Tile(Modifier.weight(1f).height(60.dp), bg = cs.background, onClick = { number = (number + 10).coerceAtMost(100) }) {
                        Text("+ 10", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = cs.onSurface)
                    }
                }
                else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    t.opts.forEach { o ->
                        Tile(
                            Modifier.weight(1f).height(60.dp),
                            bg = if (choice == o) cs.primary else cs.background,
                            onClick = { choice = o }
                        ) { Text(o, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (choice == o) Color.White else cs.onSurface) }
                    }
                }
            }
            if (message.isNotEmpty()) Text(message, color = Bad, fontWeight = FontWeight.Bold)
            PrimaryButton("CONFIRM", color = ElectricBlue) {
                val ok = when (t.kind) {
                    "T" -> toggle?.toString() == t.target
                    "V" -> number.toString() == t.target
                    else -> choice == t.target
                }
                if (ok) {
                    if (cur == tasks.size - 1) onResult(true) else cur++
                } else {
                    message = "Wrong value - check the target for ${t.name}"
                }
            }
        }
    }
}

/** Balance test: steer the ball with a D-pad and hold it inside the ring. */
@Composable
fun BalanceGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hold = p.ints[0]
    val innerPct = p.ints[1] / 100f
    val drift = p.ints[2] / 100f
    val angle = Math.toRadians(p.ints[3].toDouble())
    var bx by remember { mutableFloatStateOf((cos(angle) * 0.75).toFloat()) }
    var byy by remember { mutableFloatStateOf((sin(angle) * 0.75).toFloat()) }
    var dirX by remember { mutableIntStateOf(0) }
    var dirY by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var vx = 0f
        var vy = 0f
        var t = 0f
        while (!done) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            t += dt
            if (t > 0.6f) {
                t = 0f
                vx = (Random.nextFloat() - 0.5f) * 2f * drift
                vy = (Random.nextFloat() - 0.5f) * 2f * drift
            }
            bx += (dirX * 0.9f + vx) * dt
            byy += (dirY * 0.9f + vy) * dt
            val r = sqrt(bx * bx + byy * byy)
            if (r > 1f) { bx /= r; byy /= r }
            progress = if (r < innerPct) progress + dt else (progress - dt).coerceAtLeast(0f)
            if (progress >= hold) { done = true; onResult(true) }
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(270.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val radius = size.minDimension / 2
                val ballR = 14.dp.toPx()
                drawCircle(Color(0xFFFFD27A), radius - 2.dp.toPx(), c, style = Stroke(3.dp.toPx()))
                drawCircle(Color(0xFFFFD27A), radius * innerPct, c, style = Stroke(3.dp.toPx()))
                val travel = radius - ballR
                drawCircle(Color.White, ballR, Offset(c.x + bx * travel, c.y + byy * travel))
            }
        }
        LinearProgressIndicator(
            progress = { (progress / hold).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
        )
        Text("${"%.1f".format(progress)}s / ${hold}s  -  hold the ball inside the ring", color = cs.onSurfaceVariant, fontSize = 14.sp)

        @Composable
        fun Pad(label: String, dx: Int, dy: Int) {
            Box(
                Modifier.size(62.dp).clip(CircleShape).background(cs.surfaceVariant)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            if (dx != 0) dirX = dx
                            if (dy != 0) dirY = dy
                            tryAwaitRelease()
                            if (dx != 0 && dirX == dx) dirX = 0
                            if (dy != 0 && dirY == dy) dirY = 0
                        })
                    },
                contentAlignment = Alignment.Center
            ) { Text(label, fontSize = 26.sp, color = cs.onSurface) }
        }
        Pad("▲", 0, -1)
        Row(horizontalArrangement = Arrangement.spacedBy(62.dp)) {
            Pad("◀", -1, 0)
            Pad("▶", 1, 0)
        }
        Pad("▼", 0, 1)
    }
}

/** Simon: watch the coloured sequence, then repeat it. */
@Composable
fun SimonGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val seq = remember(p.id) { p.items.map { it.toInt() } }
    val colors = listOf(Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835))
    var phase by remember { mutableIntStateOf(0) }
    var lit by remember { mutableIntStateOf(-1) }
    var pos by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(900)
        for (s in seq) {
            lit = s
            delay(550)
            lit = -1
            delay(250)
        }
        phase = 1
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (phase == 0) "WATCH CAREFULLY…" else "YOUR TURN  (${pos}/${seq.size})",
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.primary
        )
        Box(Modifier.fillMaxWidth(0.85f)) {
            com.carfam.wakeuppro.ui.SquareGrid(2, 4, 14.dp) { i ->
                Tile(
                    Modifier.fillMaxSize(),
                    bg = colors[i].copy(alpha = if (lit == i) 1f else 0.3f),
                    onClick = if (phase == 1) {
                        {
                            scope.launch { lit = i; delay(160); lit = -1 }
                            if (i == seq[pos]) {
                                pos++
                                if (pos == seq.size) onResult(true)
                            } else onResult(false)
                        }
                    } else null
                ) {}
            }
        }
    }
}

/** Catch the dots: tap each dot before it disappears. */
@Composable
fun CatchDotGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val need = p.ints[0]
    val maxMiss = p.ints[1]
    val life = p.ints[2]
    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    var fx by remember { mutableFloatStateOf(0.5f) }
    var fy by remember { mutableFloatStateOf(0.5f) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(600)
        while (!done) {
            fx = Random.nextFloat()
            fy = Random.nextFloat()
            visible = true
            var t = 0
            while (t < life && visible && !done) { delay(30); t += 30 }
            if (visible && !done) {
                misses++
                visible = false
                if (misses > maxMiss) { done = true; onResult(false) }
            }
            delay(250)
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Hits $hits / $need     Misses $misses / ${maxMiss + 1}", fontWeight = FontWeight.Bold, color = cs.onBackground)
        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(cs.surfaceVariant)
        ) {
            val dot = 70.dp
            if (visible) {
                Box(
                    Modifier.offset((maxWidth - dot) * fx, (maxHeight - dot) * fy).size(dot).clip(CircleShape)
                        .background(AccentBrush)
                        .clickable {
                            if (!done && visible) {
                                visible = false
                                hits++
                                if (hits >= need) { done = true; onResult(true) }
                            }
                        }
                )
            }
        }
    }
}

/** Swipe rules: swipe the same way as the arrow, or the opposite way when told to. */
@Composable
fun SwipeArrowsGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val rounds = remember(p.id) { p.items.map { val x = it.split("|"); x[0].toInt() to x[1].toInt() } }
    var idx by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val dir = rounds[idx].first
    val opp = rounds[idx].second

    fun handle(total: Offset) {
        if (done) return
        val ax = abs(total.x)
        val ay = abs(total.y)
        if (max(ax, ay) < 70f) return
        val got = if (ax > ay) (if (total.x > 0) 1 else 3) else (if (total.y > 0) 2 else 0)
        val want = if (opp == 1) (dir + 2) % 4 else dir
        if (got == want) {
            if (idx == rounds.size - 1) { done = true; onResult(true) } else idx++
        } else {
            done = true
            onResult(false)
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Swipe ${idx + 1} / ${rounds.size}", color = cs.onSurfaceVariant)
        Text(ARROWS[dir], fontSize = 110.sp, color = cs.onBackground)
        Text(
            if (opp == 1) "Swipe the OPPOSITE way" else "Swipe the SAME way",
            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = if (opp == 1) Bad else Good
        )
        Box(
            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(cs.surfaceVariant)
                .pointerInput(idx) {
                    var total = Offset.Zero
                    detectDragGestures(
                        onDragStart = { total = Offset.Zero },
                        onDragEnd = { handle(total) },
                        onDrag = { change, amount ->
                            change.consume()
                            total += amount
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) { Text("Swipe anywhere here", color = cs.onSurfaceVariant) }
    }
}

/** Slider lock: set the slider to an exact value and press LOCK. */
@Composable
fun SliderLockGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val target = p.ints[0]
    val tol = p.ints[1]
    var v by remember { mutableFloatStateOf(if (target < 50) 85f else 15f) }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("TARGET", fontWeight = FontWeight.Bold, color = cs.primary)
        Text("$target", fontSize = 76.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
        Text("Current: ${v.roundToInt()}   (±$tol allowed)", color = cs.onSurfaceVariant)
        Slider(value = v, onValueChange = { v = it }, valueRange = 0f..100f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(Modifier.weight(1f).height(56.dp), bg = cs.surfaceVariant, onClick = { v = (v - 1f).coerceAtLeast(0f) }) {
                Text("− 1", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
            Tile(Modifier.weight(1f).height(56.dp), bg = cs.surfaceVariant, onClick = { v = (v + 1f).coerceAtMost(100f) }) {
                Text("+ 1", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton("🔒  LOCK", color = Violet) {
            onResult(abs(v.roundToInt() - target) <= tol)
        }
    }
}

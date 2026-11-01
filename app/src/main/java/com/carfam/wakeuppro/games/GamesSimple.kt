package com.carfam.wakeuppro.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carfam.wakeuppro.ui.Bad
import com.carfam.wakeuppro.ui.Good
import com.carfam.wakeuppro.ui.Keypad
import com.carfam.wakeuppro.ui.PrimaryButton
import com.carfam.wakeuppro.ui.SquareGrid
import com.carfam.wakeuppro.ui.Tile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Multiple-choice games: missing number, sequence, pattern, shapes, colours, odd-one-out, logic, math, direction, teasers. */
@Composable
fun McqGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val colorMode = p.optionColors.isNotEmpty()
    val cols = if (!colorMode && (p.options.any { it.length > 12 } || p.options.size == 3)) 1 else 2
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (p.promptColor != 0L) {
            Spacer(Modifier.height(4.dp))
            Spacer(
                Modifier.size(130.dp).clip(RoundedCornerShape(30.dp)).background(Color(p.promptColor))
                    .border(3.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
            )
        }
        if (p.prompt.isNotBlank()) {
            val big = p.prompt.length <= 30 && !p.prompt.contains('\n')
            Text(
                p.prompt,
                fontSize = if (big) 34.sp else 20.sp,
                lineHeight = if (big) 44.sp else 29.sp,
                fontWeight = if (big) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = cs.onBackground
            )
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in p.options.indices.chunked(cols)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in row) {
                        Tile(
                            Modifier.weight(1f).height(if (colorMode) 92.dp else 84.dp),
                            bg = if (colorMode) Color(p.optionColors[i]) else cs.surfaceVariant,
                            border = if (colorMode) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                            onClick = { onResult(i == p.answer) }
                        ) {
                            if (!colorMode) {
                                val t = p.options[i]
                                Text(
                                    t,
                                    fontSize = if (t.length > 10) 18.sp else 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = cs.onSurface
                                )
                            }
                        }
                    }
                    if (row.size < cols) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Number memory (forward) and reverse number memory. */
@Composable
fun DigitMemoryGame(p: Puzzle, reverse: Boolean, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val digits = p.items[0]
    val showMs = p.ints[0]
    var phase by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(showMs) }
    var input by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        var t = showMs
        while (t > 0) { left = t; delay(100); t -= 100 }
        phase = 1
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (phase == 0) {
            Spacer(Modifier.height(20.dp))
            Text("MEMORIZE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.primary)
            Text(digits, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace, color = cs.onBackground)
            LinearProgressIndicator(
                progress = { left / showMs.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
        } else {
            Text(
                if (reverse) "Type it BACKWARDS" else "Type the number",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.primary
            )
            Text(
                input.ifEmpty { "·" },
                fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace, color = cs.onBackground
            )
            Keypad(
                onDigit = { if (input.length < digits.length) input += it },
                onBack = { input = input.dropLast(1) },
                onOk = { onResult(input == (if (reverse) digits.reversed() else digits)) },
                okEnabled = input.length == digits.length
            )
        }
    }
}

/** Stroop colour test: tap the INK colour of the word. */
@Composable
fun StroopGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var idx by remember { mutableIntStateOf(0) }
    val parts = p.items[idx].split("|")
    val word = STROOP_NAMES[parts[0].toInt()]
    val ink = parts[1].toInt()
    val opts = parts[2].split(",").map { it.toInt() }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Word ${idx + 1} of ${p.items.size}", color = cs.onSurfaceVariant, fontSize = 15.sp)
        Text(word, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold, color = Color(STROOP_COLORS[ink]))
        Text("What COLOUR is the ink?", fontSize = 17.sp, color = cs.onBackground)
        SquareGrid(2, 4, 12.dp, 1.7f) { i ->
            Tile(Modifier.fillMaxSize(), bg = cs.surfaceVariant, onClick = {
                if (opts[i] == ink) {
                    if (idx == p.items.size - 1) onResult(true) else idx++
                } else onResult(false)
            }) {
                Text(STROOP_NAMES[opts[i]], fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
        }
    }
}

/** Word memory: study words, then pick them out of a bigger list. */
@Composable
fun WordMemoryGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val secs = p.ints[0]
    var phase by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(secs) }
    val selected = remember { mutableStateListOf<Int>() }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left-- }
        phase = 1
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (phase == 0) {
            Text("MEMORIZE  ($left)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.primary)
            p.items.forEach {
                Text(it.uppercase(), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = cs.onBackground)
            }
        } else {
            Text(
                "Tap the ${p.items.size} words you saw  (${selected.size}/${p.items.size})",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cs.primary, textAlign = TextAlign.Center
            )
            SquareGrid(3, p.options.size, 8.dp, 1.7f) { i ->
                Tile(
                    Modifier.fillMaxSize(),
                    bg = if (i in selected) cs.primary else cs.surfaceVariant,
                    onClick = { if (i in selected) selected.remove(i) else selected.add(i) }
                ) {
                    Text(p.options[i], fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (i in selected) Color.White else cs.onSurface)
                }
            }
            PrimaryButton("CONFIRM", enabled = selected.size == p.items.size) {
                onResult(selected.toSet() == p.answers)
            }
        }
    }
}

/** Quick decision: 5 rapid-fire questions with a countdown each. */
@Composable
fun QuickDecisionGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val secs = p.ints[0]
    var idx by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(1f) }
    var finished by remember { mutableStateOf(false) }
    val parts = p.items[idx].split("|")
    LaunchedEffect(idx) {
        val total = secs * 1000
        var t = total
        progress = 1f
        while (t > 0) {
            delay(50)
            t -= 50
            progress = t / total.toFloat()
        }
        if (!finished) { finished = true; onResult(false) }
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text("Question ${idx + 1} / ${p.items.size}", color = cs.onSurfaceVariant, fontSize = 15.sp)
        LinearProgressIndicator(
            progress = { progress },
            color = if (progress < 0.3f) Bad else cs.primary,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
        )
        Text(parts[0], fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = cs.onBackground)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 0..1) {
                Tile(Modifier.weight(1f).height(100.dp), bg = cs.surfaceVariant, onClick = {
                    if (!finished) {
                        if (i == parts[3].toInt()) {
                            if (idx == p.items.size - 1) { finished = true; onResult(true) } else idx++
                        } else {
                            finished = true
                            onResult(false)
                        }
                    }
                }) {
                    Text(parts[1 + i], fontSize = 28.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                }
            }
        }
    }
}

/** Symbol matching: tap every symbol equal to the target. */
@Composable
fun SymbolMatchGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val found = remember { mutableStateListOf<Int>() }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("FIND ALL OF THIS SYMBOL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cs.primary)
        Text(p.prompt, fontSize = 60.sp, color = cs.onBackground)
        SquareGrid(3, 12, 10.dp, 1.35f) { i ->
            Tile(
                Modifier.fillMaxSize(),
                bg = if (i in found) Good else cs.surfaceVariant,
                onClick = {
                    if (i in p.answers) {
                        if (i !in found) {
                            found.add(i)
                            if (found.size == p.answers.size) onResult(true)
                        }
                    } else onResult(false)
                }
            ) { Text(p.items[i], fontSize = 38.sp, color = cs.onSurface) }
        }
        Text("${found.size} / ${p.answers.size} found", color = cs.onSurfaceVariant)
    }
}

/** Short-term memory: remember which squares lit up. */
@Composable
fun MemoryGridGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val n = p.ints[0]
    val flash = p.ints[1]
    var phase by remember { mutableIntStateOf(0) }
    val tapped = remember { mutableStateListOf<Int>() }
    LaunchedEffect(Unit) {
        delay(700)
        delay(flash.toLong())
        phase = 1
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (phase == 0) "MEMORIZE THE LIT SQUARES" else "TAP THE SQUARES THAT LIT UP  (${tapped.size}/${p.answers.size})",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.primary, textAlign = TextAlign.Center
        )
        SquareGrid(n, n * n, 10.dp) { i ->
            val lit = i in p.answers
            val click: (() -> Unit)? = if (phase == 1) {
                {
                    if (lit) {
                        if (i !in tapped) {
                            tapped.add(i)
                            if (tapped.size == p.answers.size) onResult(true)
                        }
                    } else onResult(false)
                }
            } else null
            val bg = when {
                phase == 0 && lit -> cs.primary
                phase == 1 && i in tapped -> Good
                else -> cs.surfaceVariant
            }
            Tile(Modifier.fillMaxSize(), bg = bg, onClick = click) {}
        }
    }
}

/** Image matching: classic flip-the-cards pairs game. */
@Composable
fun CardMatchGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val flipped = remember { mutableStateListOf<Int>() }
    val matched = remember { mutableStateListOf<Int>() }
    var busy by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }
    val cols = if (p.items.size <= 6) 3 else 4
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Moves: $moves   ·   Pairs: ${matched.size / 2}/${p.items.size / 2}", color = cs.onSurfaceVariant)
        SquareGrid(cols, p.items.size, 10.dp, 0.9f) { i ->
            val open = i in flipped || i in matched
            Tile(
                Modifier.fillMaxSize(),
                bg = if (i in matched) Good.copy(alpha = 0.35f) else if (open) cs.surfaceVariant else cs.secondary,
                onClick = {
                    if (!busy && !open) {
                        flipped.add(i)
                        if (flipped.size == 2) {
                            busy = true
                            moves++
                            val a = flipped[0]
                            val b = flipped[1]
                            scope.launch {
                                delay(750)
                                if (p.items[a] == p.items[b]) { matched.add(a); matched.add(b) }
                                flipped.clear()
                                busy = false
                                if (matched.size == p.items.size) onResult(true)
                            }
                        }
                    }
                }
            ) {
                Text(if (open) p.items[i] else "?", fontSize = if (open) 38.sp else 30.sp,
                    fontWeight = FontWeight.Bold, color = if (open) cs.onSurface else Color.White)
            }
        }
    }
}

/** Find the different shape/letter in a grid. */
@Composable
fun DifferentShapeGame(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val n = p.ints[0]
    SquareGrid(n, n * n, 8.dp) { i ->
        Tile(Modifier.fillMaxSize(), bg = cs.surfaceVariant, onClick = { onResult(i == p.answer) }) {
            Text(p.items[i], fontSize = if (n >= 5) 24.sp else 36.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
        }
    }
}

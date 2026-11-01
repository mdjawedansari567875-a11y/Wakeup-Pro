package com.carfam.wakeuppro.games

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carfam.wakeuppro.data.AppSettings
import com.carfam.wakeuppro.data.Store
import com.carfam.wakeuppro.ui.Bad
import com.carfam.wakeuppro.ui.Good
import com.carfam.wakeuppro.ui.PrimaryButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NO_SCROLL = setOf(
    GameType.SWIPE_ARROWS, GameType.BALANCE, GameType.CATCH_DOT, GameType.TAP_COUNTER,
    GameType.SHAKE, GameType.REACTION, GameType.SLIDER_LOCK, GameType.SIMON
)

/**
 * Runs the whole alarm challenge: N different games, wrong answers regenerate a fresh puzzle,
 * no skipping. Only after the last game does the STOP ALARM button appear.
 */
@Composable
fun ChallengeHost(
    settings: AppSettings,
    store: Store,
    practice: Boolean,
    onStop: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val count = settings.challengeCount.coerceIn(1, 5)
    val diff = remember { (settings.difficulty + store.adaptive()).coerceIn(0, 2) }
    val types = remember { Engine.pickGames(store, count) }

    var step by remember { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    var fails by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableIntStateOf(0) } // 0 none, 1 correct, 2 wrong
    val scope = rememberCoroutineScope()

    fun finishAll() {
        if (!practice) {
            val delta = if (fails == 0) 1 else if (fails >= 4) -1 else 0
            val next = (store.adaptive() + delta).coerceIn(-settings.difficulty, 2 - settings.difficulty)
            store.setAdaptive(next)
        }
    }

    val onResult: (Boolean) -> Unit = { ok ->
        if (feedback == 0) {
            if (ok) {
                feedback = 1
                scope.launch {
                    delay(800)
                    feedback = 0
                    attempt = 0
                    step++
                    if (step >= count) finishAll()
                }
            } else {
                feedback = 2
                fails++
                scope.launch {
                    delay(1200)
                    feedback = 0
                    attempt++
                }
            }
        }
    }

    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val fmt = remember(settings.is24h) {
        SimpleDateFormat(if (settings.is24h) "HH:mm" else "hh:mm a", Locale.getDefault())
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(fmt.format(Date(now)), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
            Column(horizontalAlignment = Alignment.End) {
                Text(if (practice) "PRACTICE" else "⏰ ALARM", fontWeight = FontWeight.ExtraBold, color = cs.primary)
                Text(
                    if (step < count) "Game ${step + 1}/$count" else "All done!",
                    color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(count) { i ->
                Box(
                    Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (i < step) Good else if (i == step) cs.primary else cs.surfaceVariant)
                )
            }
        }
        if (practice && step < count) {
            TextButton(onClick = onStop) { Text("Exit practice") }
        } else {
            Spacer(Modifier.height(10.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Crossfade(targetState = step * 100 + attempt, animationSpec = tween(350), label = "game") { key ->
                val s = key / 100
                if (s >= count) {
                    DoneContent(practice, onStop)
                } else {
                    val pz = remember(key) { Engine.puzzle(types[s], diff, store) }
                    GameScreen(pz, onResult)
                }
            }
            if (feedback != 0) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                        .background((if (feedback == 1) Good else Bad).copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (feedback == 1) "✔  Correct!" else "✘  Wrong!\nNew puzzle coming…",
                        fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun GameScreen(p: Puzzle, onResult: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scrolls = p.type !in NO_SCROLL
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(p.type.emoji, fontSize = 30.sp)
            Text(p.type.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
        }
        Spacer(Modifier.height(4.dp))
        Text(p.instruction, fontSize = 15.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .then(if (scrolls) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            contentAlignment = Alignment.TopCenter
        ) {
            GameBody(p, onResult)
        }
    }
}

@Composable
private fun GameBody(p: Puzzle, onResult: (Boolean) -> Unit) {
    when (p.type) {
        GameType.MISSING_NUMBER, GameType.SEQUENCE, GameType.PATTERN_COMPLETION,
        GameType.SHAPE_RECOGNITION, GameType.COLOR_RECOGNITION, GameType.ODD_ONE_OUT,
        GameType.LOGIC, GameType.MENTAL_MATH, GameType.DIRECTION, GameType.BRAIN_TEASER -> McqGame(p, onResult)
        GameType.NUMBER_MEMORY -> DigitMemoryGame(p, false, onResult)
        GameType.REVERSE_MEMORY -> DigitMemoryGame(p, true, onResult)
        GameType.STROOP -> StroopGame(p, onResult)
        GameType.WORD_MEMORY -> WordMemoryGame(p, onResult)
        GameType.QUICK_DECISION -> QuickDecisionGame(p, onResult)
        GameType.SYMBOL_MATCH -> SymbolMatchGame(p, onResult)
        GameType.SHORT_TERM_MEMORY -> MemoryGridGame(p, onResult)
        GameType.IMAGE_MATCH -> CardMatchGame(p, onResult)
        GameType.DIFFERENT_SHAPE -> DifferentShapeGame(p, onResult)
        GameType.REACTION -> ReactionGame(p, onResult)
        GameType.TAP_COUNTER -> TapGame(p, onResult)
        GameType.SHAKE -> ShakeGame(p, onResult)
        GameType.CONTROL_PANEL -> ControlPanelGame(p, onResult)
        GameType.BALANCE -> BalanceGame(p, onResult)
        GameType.SIMON -> SimonGame(p, onResult)
        GameType.CATCH_DOT -> CatchDotGame(p, onResult)
        GameType.SWIPE_ARROWS -> SwipeArrowsGame(p, onResult)
        GameType.SLIDER_LOCK -> SliderLockGame(p, onResult)
    }
}

@Composable
private fun DoneContent(practice: Boolean, onStop: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "done"
    )
    Column(
        Modifier.fillMaxSize().scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 90.sp)
        Spacer(Modifier.height(10.dp))
        Text("Brain awake!", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = cs.onBackground)
        Text("All challenges completed", color = cs.onSurfaceVariant, fontSize = 16.sp)
        Spacer(Modifier.height(36.dp))
        PrimaryButton(
            if (practice) "FINISH" else "STOP ALARM",
            modifier = Modifier.height(96.dp),
            color = Good,
            onClick = onStop
        )
    }
}

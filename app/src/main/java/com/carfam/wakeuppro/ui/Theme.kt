package com.carfam.wakeuppro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF070B1A)
val NavySurface = Color(0xFF0F1630)
val CardDark = Color(0xFF18234A)
val ElectricBlue = Color(0xFF3D7BFF)
val Violet = Color(0xFF8B5CF6)
val Good = Color(0xFF22C55E)
val Bad = Color(0xFFEF4444)

val AccentBrush = Brush.linearGradient(listOf(ElectricBlue, Violet))

@Composable
fun WakeTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(
            primary = ElectricBlue, secondary = Violet,
            background = Navy, surface = NavySurface, surfaceVariant = CardDark,
            onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB4BEDC)
        )
    } else {
        lightColorScheme(
            primary = ElectricBlue, secondary = Violet,
            background = Color(0xFFF2F4FB), surface = Color.White, surfaceVariant = Color(0xFFE3E8F7),
            onPrimary = Color.White, onBackground = Color(0xFF0B1020), onSurface = Color(0xFF0B1020),
            onSurfaceVariant = Color(0xFF50597A)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Rounded tappable tile used by almost every game. */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    bg: Color,
    border: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    var m = modifier.clip(shape).background(bg).border(2.dp, border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, contentAlignment = Alignment.Center, content = content)
}

/** Grid of equally sized cells; cell(i) is drawn for i in 0 until count. */
@Composable
fun SquareGrid(
    cols: Int,
    count: Int,
    spacing: Dp = 8.dp,
    ratio: Float = 1f,
    cell: @Composable (Int) -> Unit
) {
    val rows = (count + cols - 1) / cols
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (c in 0 until cols) {
                    val i = r * cols + c
                    Box(Modifier.weight(1f).aspectRatio(ratio)) {
                        if (i < count) cell(i)
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = ElectricBlue,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Keypad(onDigit: (Char) -> Unit, onBack: () -> Unit, onOk: () -> Unit, okEnabled: Boolean) {
    val cs = MaterialTheme.colorScheme
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✔")
    SquareGrid(3, 12, 10.dp, 1.8f) { i ->
        val k = keys[i]
        val bg = when (k) {
            "✔" -> if (okEnabled) Good else cs.surfaceVariant
            else -> cs.surfaceVariant
        }
        Tile(Modifier.fillMaxSize(), bg = bg, onClick = {
            when (k) {
                "⌫" -> onBack()
                "✔" -> { if (okEnabled) onOk() }
                else -> onDigit(k[0])
            }
        }) {
            Text(k, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = if (k == "✔" && okEnabled) Color.White else cs.onSurface)
        }
    }
}

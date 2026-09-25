package com.naampath.colorpath3d.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Magenta

private val steps = listOf(
    "CONNECT MATCHING COLORS" to "Touch a glowing node and drag to its twin.",
    "FOLLOW THE GRID" to "Move up, down, left, or right. The path stays on the tiles.",
    "DON'T CROSS OTHER PATHS" to "A finished path holds its tiles. Go around it.",
    "CONNECT THEM ALL" to "The level clears when every pair is linked.",
    "READY?" to "START PUZZLE"
)

@Composable
fun TutorialScreen(step: Int, onNext: () -> Unit) {
    val copy = steps[step.coerceIn(0, steps.lastIndex)]
    val transition = rememberInfiniteTransition(label = "finger")
    val shift by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "shift"
    )
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("TUTORIAL", color = Magenta)
        Text(copy.first, color = Cyan)
        Text(copy.second, color = com.naampath.colorpath3d.ui.theme.Mist)
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val y = size.height / 2f
            drawCircle(Cyan, 18f, Offset(size.width * 0.15f, y))
            drawCircle(Cyan, 18f, Offset(size.width * 0.85f, y))
            drawCircle(Magenta, 14f, Offset(size.width * shift, y - 36f))
        }
        NeonButton(if (step == steps.lastIndex) "START PUZZLE" else "NEXT", onClick = onNext)
    }
}

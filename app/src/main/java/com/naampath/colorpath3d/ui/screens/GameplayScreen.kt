package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.game.GameCommand
import com.naampath.colorpath3d.gameplay.GameState
import com.naampath.colorpath3d.model.RunStatus
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist
import kotlin.math.hypot

@Composable
fun GameplayScreen(
    title: String,
    state: GameState?,
    cameraRotation: Boolean,
    onCommand: (GameCommand) -> Unit,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onHint: () -> Unit
) {
    val width = remember { mutableFloatStateOf(1f) }
    val height = remember { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged {
                    width.floatValue = it.width.toFloat().coerceAtLeast(1f)
                    height.floatValue = it.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(cameraRotation) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        fun send(phase: Int, x: Float, y: Float) {
                            onCommand(GameCommand.Pointer(phase, x, y, width.floatValue, height.floatValue))
                        }
                        send(0, down.position.x, down.position.y)
                        var previousSpan = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2 && cameraRotation) {
                                val span = distance(pressed[0].position, pressed[1].position)
                                if (previousSpan > 0f && span > 0f) {
                                    onCommand(GameCommand.Pointer(4, previousSpan / span, 0f, width.floatValue, height.floatValue))
                                }
                                previousSpan = span
                                event.changes.forEach { it.consume() }
                            } else {
                                val change = pressed.firstOrNull() ?: event.changes.firstOrNull() ?: break
                                if (change.pressed) send(1, change.position.x, change.position.y)
                                else {
                                    send(2, change.position.x, change.position.y)
                                    break
                                }
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
        )
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BACK", color = Cyan, modifier = Modifier.padding(8.dp).pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onBack()
                    }
                })
                Text(title, color = Cyan)
                Text("PAUSE", color = Cyan, modifier = Modifier.padding(8.dp).pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onPause()
                    }
                })
            }
            state?.let {
                Text(
                    "Moves ${it.moves}${it.moveLimit?.let { limit -> "/$limit" } ?: ""}   " +
                        "${it.elapsedMs / 1000}s   ${it.pairsConnected}/${it.pairsTotal}",
                    color = Mist
                )
                if (it.status == RunStatus.TIME_UP) Text("TIME UP", color = Amber)
                if (it.status == RunStatus.MOVES_EXHAUSTED) Text("OUT OF MOVES", color = Amber)
                if (it.status == RunStatus.FAILED) Text("PERFECT RUN ENDED", color = Amber)
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonButton("UNDO", modifier = Modifier.weight(1f)) { onCommand(GameCommand.Undo) }
            NeonButton("RESTART", modifier = Modifier.weight(1f)) { onCommand(GameCommand.Restart) }
            NeonButton("HINT", modifier = Modifier.weight(1f), onClick = onHint)
        }
    }
}

private fun distance(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)

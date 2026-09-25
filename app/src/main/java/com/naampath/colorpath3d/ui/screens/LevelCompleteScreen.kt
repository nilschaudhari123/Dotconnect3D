package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.naampath.colorpath3d.gameplay.LevelResult
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.components.NeonCard
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun LevelCompleteScreen(
    result: LevelResult,
    bestScore: Int,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onSelect: () -> Unit
) {
    Dialog(onDismissRequest = onSelect) {
        NeonCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (result.completed) "LEVEL COMPLETE!" else "TRY AGAIN", color = Cyan)
                Text("★".repeat(result.stars.coerceIn(0, 3)), color = Amber)
                Text("Moves ${result.moves}", color = Mist)
                Text("Time ${result.elapsedMs / 1000}s", color = Mist)
                Text("Score ${result.score}    Best $bestScore", color = Mist)
                if (result.completed) NeonButton("NEXT LEVEL", onClick = onNext)
                NeonButton("RETRY", onClick = onRetry)
                NeonButton("LEVEL SELECT", onClick = onSelect)
            }
        }
    }
}

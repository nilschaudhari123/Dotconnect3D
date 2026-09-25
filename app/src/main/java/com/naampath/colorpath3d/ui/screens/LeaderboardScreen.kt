package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.data.db.LocalScoreEntity
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun LeaderboardScreen(
    rows: List<LocalScoreEntity>,
    note: String?,
    onBoard: (String) -> Unit,
    onSync: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("LEADERBOARD", color = Cyan)
        Text("Scores stay on this device unless Google Play Games is configured.", color = Mist)
        NeonButton("HIGHEST SCORE") { onBoard("CLASSIC") }
        NeonButton("FASTEST") { onBoard("TIMED") }
        NeonButton("DAILY") { onBoard("DAILY") }
        NeonButton("WEEKLY") { onBoard("ENDLESS") }
        NeonButton("MONTHLY") { onBoard("HARD") }
        rows.take(8).forEachIndexed { index, row ->
            Text("${index + 1}. ${row.score}  ${row.timeMs / 1000}s", color = Amber)
        }
        if (note != null) Text(note, color = Mist)
        NeonButton("SYNC WITH GOOGLE PLAY", onClick = onSync)
        NeonButton("BACK", onClick = onBack)
    }
}

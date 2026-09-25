package com.naampath.colorpath3d.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.progress.CampaignState
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun DailyScreen(state: CampaignState, reward: Int, onPlay: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val dayNumber = ChronoUnit.DAYS.between(LocalDate.of(2026, 1, 1), today).toInt() + 1
    val done = state.lastDailyDay == today.toEpochDay()
    val stars = if (done) "★★★" else "☆☆☆"
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DAILY CHALLENGE", color = Cyan)
        Text("DAY $dayNumber", color = Amber)
        Text(stars, color = Amber)
        Text("STREAK: ${state.streak} DAYS", color = Mist)
        Text("Reward $reward coins", color = Mist)
        NeonButton(if (done) "PLAY AGAIN" else "START", onClick = onPlay)
        NeonButton("SHARE RESULT") {
            val text = "COLOR PATH 3D — Daily day $dayNumber\n$stars\nStreak: ${state.streak} days"
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(share, "Share"))
        }
        NeonButton("BACK", onClick = onBack)
    }
}

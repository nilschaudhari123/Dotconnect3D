package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.progress.CampaignState
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun StatisticsScreen(state: CampaignState, onBack: () -> Unit) {
    val classic = state.levels.values.filter { it.mode == GameMode.CLASSIC.name && it.completed }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("STATISTICS", color = Cyan)
        Text("Campaign clears ${classic.size} / 1000", color = Mist)
        Text("Stars ${classic.sumOf { it.stars }}", color = Amber)
        Text("Best score ${classic.maxOfOrNull { it.bestScore } ?: 0}", color = Mist)
        Text("Coins ${state.wallet.coins}", color = Amber)
        Text("Daily streak ${state.streak}", color = Mist)
        Text("Hintless clears ${state.hintlessClears}", color = Mist)
        Text("Endless reached ${state.endlessIndex}", color = Mist)
        NeonButton("BACK", onClick = onBack)
    }
}

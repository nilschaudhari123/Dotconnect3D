package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.level.World
import com.naampath.colorpath3d.progress.LevelRecord
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.components.NeonCard
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist
import com.naampath.colorpath3d.ui.theme.Panel

@Composable
fun WorldSelectScreen(worlds: List<World>, cleared: (World) -> Int, onWorld: (World) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("WORLDS", color = Cyan)
        worlds.forEach { world ->
            NeonCard(Modifier.fillMaxWidth().clickable { onWorld(world) }) {
                Column {
                    Text(world.name, color = Cyan)
                    Text(world.subtitle, color = Mist)
                    Text("Levels ${world.firstLevel}–${world.lastLevel}   ${cleared(world)} cleared", color = Amber)
                }
            }
        }
        NeonButton("BACK", onClick = onBack)
    }
}

@Composable
fun LevelGridScreen(
    world: World,
    modeLabel: String,
    onCycleMode: () -> Unit,
    unlocked: (Int) -> Boolean,
    record: (Int) -> LevelRecord?,
    onLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(world.name, color = Cyan)
        NeonButton(modeLabel, onClick = onCycleMode)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(92.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((world.firstLevel..world.lastLevel).toList()) { id ->
                val open = unlocked(id)
                val saved = record(id)
                val stars = when (saved?.stars ?: 0) {
                    3 -> "★★★"
                    2 -> "★★"
                    1 -> "★"
                    else -> ""
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (open) Panel else Panel.copy(alpha = 0.4f))
                        .clickable(enabled = open) { onLevel(id) }
                        .padding(10.dp)
                ) {
                    Column {
                        Text(if (open) "$id" else "Lock", color = if (open) Cyan else Mist)
                        Text(stars, color = Amber)
                        Text(saved?.bestMoves?.takeIf { saved.completed }?.let { "$it moves" } ?: "—", color = Mist)
                    }
                }
            }
        }
        NeonButton("BACK", onClick = onBack)
    }
}

@Composable
fun RowSpacer() { Row {} }

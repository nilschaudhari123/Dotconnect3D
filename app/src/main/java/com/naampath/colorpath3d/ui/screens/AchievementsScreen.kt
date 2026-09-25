package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.achievements.AchievementId
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.components.NeonCard
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun AchievementsScreen(unlocked: Set<AchievementId>, catalog: List<AchievementId>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("ACHIEVEMENTS", color = Cyan)
        catalog.forEach { item ->
            val owned = item in unlocked
            NeonCard {
                Column {
                    Text(item.title, color = if (owned) Amber else Mist)
                    Text(item.description, color = Mist)
                    Text(if (owned) "Unlocked" else "Locked", color = if (owned) Cyan else Mist)
                }
            }
        }
        NeonButton("BACK", onClick = onBack)
    }
}

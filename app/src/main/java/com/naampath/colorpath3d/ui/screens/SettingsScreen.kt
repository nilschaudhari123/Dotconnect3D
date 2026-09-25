package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.data.SettingsState
import com.naampath.colorpath3d.model.Quality
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun SettingsScreen(state: SettingsState, onChange: ((SettingsState) -> SettingsState) -> Unit, onPrivacy: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SETTINGS", color = Cyan)
        toggle("Music", state.music) { onChange { it.copy(music = !it.music) } }
        toggle("Sound", state.sound) { onChange { it.copy(sound = !it.sound) } }
        toggle("Haptic", state.haptic) { onChange { it.copy(haptic = !it.haptic) } }
        toggle("Color-blind symbols", state.colorBlind) { onChange { it.copy(colorBlind = !it.colorBlind) } }
        toggle("High contrast", state.highContrast) { onChange { it.copy(highContrast = !it.highContrast) } }
        toggle("Reduced effects", state.reducedEffects) { onChange { it.copy(reducedEffects = !it.reducedEffects) } }
        toggle("Camera rotation", state.cameraRotation) { onChange { it.copy(cameraRotation = !it.cameraRotation) } }
        toggle("Large text", state.largeText) { onChange { it.copy(largeText = !it.largeText) } }
        Text("Quality ${state.quality.name}", color = Mist, modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Quality.entries.forEach { quality ->
                NeonButton(quality.name, modifier = Modifier.weight(1f)) { onChange { it.copy(quality = quality) } }
            }
        }
        NeonButton("PRIVACY", onClick = onPrivacy)
        NeonButton("BACK", onClick = onBack)
    }
}

@Composable
private fun toggle(label: String, value: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Mist)
        Switch(checked = value, onCheckedChange = { onClick() })
    }
}

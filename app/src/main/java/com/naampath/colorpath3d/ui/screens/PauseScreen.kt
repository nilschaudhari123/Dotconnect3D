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
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.components.NeonCard
import com.naampath.colorpath3d.ui.theme.Cyan

@Composable
fun PauseScreen(onResume: () -> Unit, onRestart: () -> Unit, onSettings: () -> Unit, onQuit: () -> Unit) {
    Dialog(onDismissRequest = onResume) {
        NeonCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PAUSED", color = Cyan)
                NeonButton("RESUME", onClick = onResume)
                NeonButton("RESTART", onClick = onRestart)
                NeonButton("SETTINGS", onClick = onSettings)
                NeonButton("LEVEL SELECT", onClick = onQuit)
            }
        }
    }
}

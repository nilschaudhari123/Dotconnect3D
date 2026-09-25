package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("PRIVACY", color = Cyan)
        Text(
            "Color Path 3D stores puzzle progress, settings, and purchase entitlements on this device. " +
                "Analytics events describe gameplay actions such as level completion and do not include your name, contacts, or precise location. " +
                "Advertisements and Play purchases use Google services when a network is available. " +
                "Core puzzles, scores, and settings work offline. " +
                "Replace this placeholder with your published privacy policy URL before release.",
            color = Mist,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        NeonButton("BACK", onClick = onBack)
    }
}

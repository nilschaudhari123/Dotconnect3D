package com.naampath.colorpath3d.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun SplashScreen(visible: Boolean) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "splash")
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("COLOR PATH 3D", color = Cyan.copy(alpha = alpha), textAlign = TextAlign.Center)
        Text("CONNECT  •  THINK  •  MASTER", color = Mist.copy(alpha = alpha), modifier = Modifier.padding(top = 8.dp))
        Text("BY NAAM", color = Color.White.copy(alpha = alpha * 0.8f), modifier = Modifier.padding(top = 18.dp, bottom = 36.dp))
    }
}

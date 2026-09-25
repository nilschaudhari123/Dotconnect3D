package com.naampath.colorpath3d.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Magenta
import com.naampath.colorpath3d.ui.theme.Panel

@Composable
fun NeonButton(label: String, modifier: Modifier = Modifier, accent: Color = Cyan, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Panel, Color(0xFF0C1020))))
            .border(1.5.dp, Brush.horizontalGradient(listOf(accent, Magenta.copy(alpha = 0.7f))), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = accent)
    }
}

@Composable
fun NeonCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Panel.copy(alpha = 0.92f))
            .border(1.dp, Cyan.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) { content() }
}

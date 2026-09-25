package com.naampath.colorpath3d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF070814)
val Panel = Color(0xFF14182C)
val Cyan = Color(0xFF3DFFF2)
val Magenta = Color(0xFFFF3D8A)
val Amber = Color(0xFFFFC857)
val Mist = Color(0xFFB7C0E0)

private val scheme = darkColorScheme(
    primary = Cyan,
    secondary = Magenta,
    tertiary = Amber,
    background = Ink,
    surface = Panel,
    onPrimary = Color(0xFF04120F),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ColorPathTheme(largeText: Boolean = false, content: @Composable () -> Unit) {
    val scale = if (largeText) 1.15f else 1f
    val type = Typography(
        headlineLarge = TextStyle(fontSize = (34 * scale).sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
        headlineMedium = TextStyle(fontSize = (24 * scale).sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontSize = (20 * scale).sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = (16 * scale).sp),
        labelLarge = TextStyle(fontSize = (15 * scale).sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    )
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}

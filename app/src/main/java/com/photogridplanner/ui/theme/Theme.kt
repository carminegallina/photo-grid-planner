package com.photogridplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE8D7B9),
    onPrimary = Color(0xFF251A10),
    secondary = Color(0xFF94B8A0),
    onSecondary = Color(0xFF0D2217),
    tertiary = Color(0xFFD46A6A),
    onTertiary = Color(0xFF300D0D),
    background = Color(0xFF0D0F14),
    onBackground = Color(0xFFF2F0EA),
    surface = Color(0xFF151922),
    onSurface = Color(0xFFE9E4DA),
    surfaceVariant = Color(0xFF242B35),
    onSurfaceVariant = Color(0xFFC6CBD2),
    outline = Color(0xFF59616E),
)

@Composable
fun PhotoGridPlannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}

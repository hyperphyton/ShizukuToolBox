package com.narezany.shizukutoolbox.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ToolboxColors = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF06210F),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF062A38),
    background = Color(0xFF0B0F12),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF11171C),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B242B),
    onSurfaceVariant = Color(0xFFB6C2CC),
    outline = Color(0xFF3A474F),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0907)
)

/** Приложение всегда тёмное: так удобнее читать вывод терминала. */
@Composable
fun ToolboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ToolboxColors,
        content = content
    )
}

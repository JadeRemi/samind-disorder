package com.samind.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Sage = Color(0xFF7BA97C)
val SageDark = Color(0xFF3E5C41)
val Mist = Color(0xFFEFF3EE)
val Ink = Color(0xFF2B332C)

private val colors = lightColorScheme(
    primary = SageDark,
    secondary = Sage,
    background = Mist,
    surface = Mist,
    onPrimary = Mist,
    onBackground = Ink,
    onSurface = Ink,
)

@Composable
fun SamindTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

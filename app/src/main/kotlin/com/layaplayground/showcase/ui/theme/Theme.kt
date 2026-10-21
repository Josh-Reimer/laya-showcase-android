package com.layaplayground.showcase.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LayaPurple = androidx.compose.ui.graphics.Color(0xFF7C5CFF)
private val LayaTeal = androidx.compose.ui.graphics.Color(0xFF38E1C6)
private val LayaDarkBg = androidx.compose.ui.graphics.Color(0xFF121318)
private val LayaDarkSurface = androidx.compose.ui.graphics.Color(0xFF1C1E26)

private val DarkColors = darkColorScheme(
    primary = LayaPurple,
    secondary = LayaTeal,
    background = LayaDarkBg,
    surface = LayaDarkSurface
)

private val LightColors = lightColorScheme(
    primary = LayaPurple,
    secondary = LayaTeal
)

@Composable
fun LayaShowcaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

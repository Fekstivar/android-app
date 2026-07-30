package xyz.metiq.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalMetiqColors = compositionLocalOf { MetiqColors.Dark }

private val darkScheme = darkColorScheme(
    background = MetiqColors.Dark.background,
    surface = MetiqColors.Dark.foreground,
    onBackground = MetiqColors.Dark.textPrimary,
    onSurface = MetiqColors.Dark.textPrimary,
)

private val lightScheme = lightColorScheme(
    background = MetiqColors.Light.background,
    surface = MetiqColors.Light.foreground,
    onBackground = MetiqColors.Light.textPrimary,
    onSurface = MetiqColors.Light.textPrimary,
)

@Composable
fun MetiqTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val tokens = if (darkTheme) MetiqColors.Dark else MetiqColors.Light
    CompositionLocalProvider(LocalMetiqColors provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme else lightScheme,
            typography = MetiqTypography,
            content = content,
        )
    }
}

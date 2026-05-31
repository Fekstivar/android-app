package xyz.metiq.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun MetiqTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMetiqColors provides MetiqColors.Dark) {
        MaterialTheme(
            colorScheme = darkScheme,
            typography = MetiqTypography,
            content = content,
        )
    }
}

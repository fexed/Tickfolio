package com.fexed.checklist.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.fexed.checklist.data.preferences.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = ParchmentLightPrimary,
    onPrimary = ParchmentLightOnPrimary,
    primaryContainer = ParchmentLightPrimaryContainer,
    onPrimaryContainer = ParchmentLightOnPrimaryContainer,
    secondary = ParchmentLightSecondary,
    onSecondary = ParchmentLightOnSecondary,
    secondaryContainer = ParchmentLightSecondaryContainer,
    onSecondaryContainer = ParchmentLightOnSecondaryContainer,
    tertiary = ParchmentLightTertiary,
    onTertiary = ParchmentLightOnTertiary,
    tertiaryContainer = ParchmentLightTertiaryContainer,
    onTertiaryContainer = ParchmentLightOnTertiaryContainer,
    background = ParchmentLightBackground,
    onBackground = ParchmentLightInk,
    surface = ParchmentLightSurface,
    onSurface = ParchmentLightInk,
    surfaceVariant = ParchmentLightSurfaceVariant,
    onSurfaceVariant = ParchmentLightInkMuted,
    surfaceContainer = ParchmentLightSurfaceContainer,
    surfaceContainerHigh = ParchmentLightSurfaceContainerHigh,
    outline = ParchmentLightOutline,
    outlineVariant = ParchmentLightOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = ParchmentDarkPrimary,
    onPrimary = ParchmentDarkOnPrimary,
    primaryContainer = ParchmentDarkPrimaryContainer,
    onPrimaryContainer = ParchmentDarkOnPrimaryContainer,
    secondary = ParchmentDarkSecondary,
    onSecondary = ParchmentDarkOnSecondary,
    secondaryContainer = ParchmentDarkSecondaryContainer,
    onSecondaryContainer = ParchmentDarkOnSecondaryContainer,
    tertiary = ParchmentDarkTertiary,
    onTertiary = ParchmentDarkOnTertiary,
    tertiaryContainer = ParchmentDarkTertiaryContainer,
    onTertiaryContainer = ParchmentDarkOnTertiaryContainer,
    background = ParchmentDarkBackground,
    onBackground = ParchmentDarkInk,
    surface = ParchmentDarkSurface,
    onSurface = ParchmentDarkInk,
    surfaceVariant = ParchmentDarkSurfaceVariant,
    onSurfaceVariant = ParchmentDarkInkMuted,
    surfaceContainer = ParchmentDarkSurfaceContainer,
    surfaceContainerHigh = ParchmentDarkSurfaceContainerHigh,
    outline = ParchmentDarkOutline,
    outlineVariant = ParchmentDarkOutlineVariant
)

@Composable
fun ChecklistTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    handwritingFontEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (handwritingFontEnabled) Typography.withHandwritingFont() else Typography,
        content = content
    )
}

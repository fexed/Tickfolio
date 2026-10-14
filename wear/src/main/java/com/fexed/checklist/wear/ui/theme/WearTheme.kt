package com.fexed.checklist.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

private val DarkColorScheme = ColorScheme(
    primary = Color(0xFFDFBC93),
    primaryDim = Color(0xFFCDC2B3),
    primaryContainer = Color(0xFF583B1F),
    onPrimary = Color(0xFF3E2711),
    onPrimaryContainer = Color(0xFFFCE6D0),
    secondary = Color(0xFFCDC2B3),
    secondaryDim = Color(0xFFB7ABA0),
    secondaryContainer = Color(0xFF4A4237),
    onSecondary = Color(0xFF342D24),
    onSecondaryContainer = Color(0xFFEBE1D4),
    tertiary = Color(0xFFE5A88B),
    tertiaryDim = Color(0xFFCDC2B3),
    tertiaryContainer = Color(0xFF643621),
    onTertiary = Color(0xFF472211),
    onTertiaryContainer = Color(0xFFFFDBCF),
    surfaceContainerLow = Color(0xFF221E1B),
    surfaceContainer = Color(0xFF27231E),
    surfaceContainerHigh = Color(0xFF322D27),
    onSurface = Color(0xFFF1ECE3),
    onSurfaceVariant = Color(0xFFB7ABA0),
    outline = Color(0xFF52493F),
    outlineVariant = Color(0xFF3A332B),
    background = Color(0xFF171513),
    onBackground = Color(0xFFF1ECE3)
)

@Composable
fun TickfolioWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

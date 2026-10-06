package com.hajun.layar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = PaleTeal,
    onPrimaryContainer = InkNavy,
    secondary = InkNavy,
    onSecondary = WarmPaper,
    secondaryContainer = Color(0xFFE4EAE7),
    onSecondaryContainer = InkNavy,
    tertiary = Color(0xFF775826),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2E3C4),
    onTertiaryContainer = Color(0xFF382806),
    background = WarmPaper,
    onBackground = InkNavy,
    surface = WarmPaper,
    onSurface = InkNavy,
    surfaceVariant = Color(0xFFE7ECE5),
    onSurfaceVariant = MutedInk,
    surfaceTint = Teal,
    surfaceBright = PaperWhite,
    surfaceDim = Color(0xFFE6E4DC),
    surfaceContainerLowest = PaperWhite,
    surfaceContainerLow = Color(0xFFF1EFE6),
    surfaceContainer = Color(0xFFECECE3),
    surfaceContainerHigh = Color(0xFFE5E8DF),
    surfaceContainerHighest = Color(0xFFDFE4DB),
    outline = Color(0xFF75847F),
    outlineVariant = PaperLine,
    error = Color(0xFFA63D35),
    onError = Color.White,
    errorContainer = Color(0xFFFBE1D9),
    onErrorContainer = Color(0xFF502019),
    inverseSurface = InkNavy,
    inverseOnSurface = WarmPaper,
    inversePrimary = NightTeal,
    scrim = InkNavy,
)

private val DarkColors = darkColorScheme(
    primary = NightTeal,
    onPrimary = Color(0xFF123F3B),
    primaryContainer = Color(0xFF25524C),
    onPrimaryContainer = Color(0xFFD7F2E9),
    secondary = NightText,
    onSecondary = InkNavy,
    secondaryContainer = NightRaised,
    onSecondaryContainer = NightText,
    tertiary = Color(0xFFE2BD7D),
    onTertiary = Color(0xFF3E2C0C),
    tertiaryContainer = Color(0xFF57431F),
    onTertiaryContainer = Color(0xFFF6E4BD),
    background = NightInk,
    onBackground = NightText,
    surface = NightInk,
    onSurface = NightText,
    surfaceVariant = NightRaised,
    onSurfaceVariant = NightMuted,
    surfaceTint = NightTeal,
    surfaceBright = Color(0xFF30464F),
    surfaceDim = NightInk,
    surfaceContainerLowest = Color(0xFF0B171E),
    surfaceContainerLow = NightSurface,
    surfaceContainer = Color(0xFF1C303A),
    surfaceContainerHigh = NightRaised,
    surfaceContainerHighest = Color(0xFF2A404A),
    outline = Color(0xFF8EA3A5),
    outlineVariant = NightLine,
    error = Color(0xFFFFB3A6),
    onError = Color(0xFF601B16),
    errorContainer = Color(0xFF7A2C24),
    onErrorContainer = Color(0xFFFFDAD2),
    inverseSurface = NightText,
    inverseOnSurface = InkNavy,
    inversePrimary = Teal,
    scrim = Color.Black,
)

@Composable
fun LayarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LayarTypography,
        content = content,
    )
}

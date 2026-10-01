package com.sonnik.topower.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Спокойная палитра без цветов обоев: светлый фон, белые карточки, один акцентный синий,
 * зелёный — для достижений, красный — только для предупреждений.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5C8F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7F5),
    onPrimaryContainer = Color(0xFF0F2A47),
    secondary = Color(0xFF56606D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EBF1),
    onSecondaryContainer = Color(0xFF1B2430),
    tertiary = Color(0xFF2E7D5B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6EFE2),
    onTertiaryContainer = Color(0xFF0C3A26),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFBE4E2),
    onErrorContainer = Color(0xFF5C1A16),
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF1A1C1F),
    surface = Color(0xFFF3F5F8),
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFE6E9EE),
    onSurfaceVariant = Color(0xFF5F6672),
    outline = Color(0xFF8A919C),
    outlineVariant = Color(0xFFDDE1E6),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7F0),
    onPrimary = Color(0xFF0F2A47),
    primaryContainer = Color(0xFF2A4566),
    onPrimaryContainer = Color(0xFFDCE7F5),
    secondary = Color(0xFFBCC5D1),
    onSecondary = Color(0xFF26303B),
    secondaryContainer = Color(0xFF2C333C),
    onSecondaryContainer = Color(0xFFDCE3EC),
    tertiary = Color(0xFF8FD6AE),
    onTertiary = Color(0xFF0C3A26),
    tertiaryContainer = Color(0xFF1E4D37),
    onTertiaryContainer = Color(0xFFD6EFE2),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF4A1C19),
    onErrorContainer = Color(0xFFFBE4E2),
    background = Color(0xFF111317),
    onBackground = Color(0xFFE3E5E9),
    surface = Color(0xFF111317),
    onSurface = Color(0xFFE3E5E9),
    surfaceVariant = Color(0xFF2A2F36),
    onSurfaceVariant = Color(0xFFA9B0BA),
    outline = Color(0xFF6E7681),
    outlineVariant = Color(0xFF30353D),
    surfaceContainerLowest = Color(0xFF0C0E11),
    surfaceContainerLow = Color(0xFF16191D),
    surfaceContainer = Color(0xFF181B20),
    surfaceContainerHigh = Color(0xFF23272E),
    surfaceContainerHighest = Color(0xFF1C2026),
)

@Composable
fun SonnikTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

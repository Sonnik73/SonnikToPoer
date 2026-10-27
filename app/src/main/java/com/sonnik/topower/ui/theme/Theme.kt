package com.sonnik.topower.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

/** Светлая, тёмная или как в системе. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Цветовая палитра: основной акцент, второй акцент (столбцы графиков, достижения) и лёгкий оттенок
 * фона; [tintStrength] — насколько сильно оттенок окрашивает фон и карточки (у тематических сильнее).
 * [STEEL] — исходная палитра приложения, [DYNAMIC] — цвета обоев (Android 12+).
 */
enum class ThemePalette(
    val lightPrimary: Color,
    val darkPrimary: Color,
    val lightTertiary: Color,
    val darkTertiary: Color,
    val tint: Color,
    val tintStrength: Float = 1f,
) {
    STEEL(Color(0xFF2F5C8F), Color(0xFFA9C7F0), Color(0xFF2E7D5B), Color(0xFF8FD6AE), Color(0xFF2F5C8F)),
    FOREST(Color(0xFF2E6B3F), Color(0xFF94D3A2), Color(0xFF7A5A14), Color(0xFFE0C27A), Color(0xFF3F6B4A)),
    SUNSET(Color(0xFFB4501C), Color(0xFFFFB68F), Color(0xFF00796B), Color(0xFF7FD3C6), Color(0xFFB4501C)),
    AMETHYST(Color(0xFF6A4BA8), Color(0xFFCDB8FF), Color(0xFF2E7D7A), Color(0xFF86D5CF), Color(0xFF6A4BA8)),
    GRAPHITE(Color(0xFF3C4043), Color(0xFFD0D3D6), Color(0xFF5B6B78), Color(0xFFB4C2CC), Color(0xFF6B6F73)),

    /** «Кузница»: тёмный металл и раскалённое железо. */
    FORGE(Color(0xFFB8441A), Color(0xFFFF8A50), Color(0xFF8D6E2A), Color(0xFFE3B95C), Color(0xFF7A4A30), tintStrength = 2.5f),

    /** «Гарнизон»: олива, бронза и пергамент — в тон «Крепости». */
    GARRISON(Color(0xFF56682F), Color(0xFFBFD08A), Color(0xFF8C5A2B), Color(0xFFE2A66B), Color(0xFFA68A4E), tintStrength = 3f),

    DYNAMIC(Color(0xFF2F5C8F), Color(0xFFA9C7F0), Color(0xFF2E7D5B), Color(0xFF8FD6AE), Color(0xFF2F5C8F));

    companion object {
        /** Цвета обоев есть только с Android 12. */
        val available: List<ThemePalette>
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) entries else entries - DYNAMIC
    }
}

/*
 * Спокойная палитра без цветов обоев: светлый фон, белые карточки, один акцентный цвет,
 * второй — для достижений и графиков, красный — только для предупреждений.
 * Остальные палитры строятся от неё заменой акцентов и лёгким оттенком фона.
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

private val DarkBase = Color(0xFF111317)

private fun lightScheme(p: ThemePalette): ColorScheme = LightColors.copy(
    primary = p.lightPrimary,
    primaryContainer = lerp(p.lightPrimary, Color.White, 0.86f),
    onPrimaryContainer = lerp(p.lightPrimary, Color.Black, 0.55f),
    secondary = lerp(LightColors.secondary, p.tint, 0.2f * p.tintStrength),
    secondaryContainer = lerp(LightColors.secondaryContainer, p.tint, 0.1f * p.tintStrength),
    tertiary = p.lightTertiary,
    tertiaryContainer = lerp(p.lightTertiary, Color.White, 0.85f),
    onTertiaryContainer = lerp(p.lightTertiary, Color.Black, 0.6f),
    background = lerp(Color(0xFFF4F5F7), p.tint, 0.05f * p.tintStrength),
    surface = lerp(Color(0xFFF4F5F7), p.tint, 0.05f * p.tintStrength),
    surfaceVariant = lerp(Color(0xFFE6E8EC), p.tint, 0.08f * p.tintStrength),
    outlineVariant = lerp(Color(0xFFDDE0E4), p.tint, 0.06f * p.tintStrength),
)

private fun darkScheme(p: ThemePalette): ColorScheme = DarkColors.copy(
    primary = p.darkPrimary,
    onPrimary = lerp(p.darkPrimary, Color.Black, 0.75f),
    primaryContainer = lerp(p.darkPrimary, DarkBase, 0.68f),
    onPrimaryContainer = lerp(p.darkPrimary, Color.White, 0.7f),
    secondary = lerp(DarkColors.secondary, p.tint, 0.2f * p.tintStrength),
    secondaryContainer = lerp(DarkColors.secondaryContainer, p.tint, 0.1f * p.tintStrength),
    tertiary = p.darkTertiary,
    onTertiary = lerp(p.darkTertiary, Color.Black, 0.75f),
    tertiaryContainer = lerp(p.darkTertiary, DarkBase, 0.7f),
    onTertiaryContainer = lerp(p.darkTertiary, Color.White, 0.7f),
    background = lerp(DarkBase, p.tint, 0.04f * p.tintStrength),
    surface = lerp(DarkBase, p.tint, 0.04f * p.tintStrength),
    surfaceVariant = lerp(DarkColors.surfaceVariant, p.tint, 0.08f * p.tintStrength),
    outlineVariant = lerp(DarkColors.outlineVariant, p.tint, 0.06f * p.tintStrength),
    surfaceContainerLowest = lerp(DarkColors.surfaceContainerLowest, p.tint, 0.03f * p.tintStrength),
    surfaceContainerLow = lerp(DarkColors.surfaceContainerLow, p.tint, 0.05f * p.tintStrength),
    surfaceContainer = lerp(DarkColors.surfaceContainer, p.tint, 0.05f * p.tintStrength),
    surfaceContainerHigh = lerp(DarkColors.surfaceContainerHigh, p.tint, 0.05f * p.tintStrength),
    surfaceContainerHighest = lerp(DarkColors.surfaceContainerHighest, p.tint, 0.05f * p.tintStrength),
)

/** Чёрный фон для AMOLED: экран не светится, карточки чуть светлее. */
private fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0C),
    surfaceContainer = Color(0xFF111214),
    surfaceContainerHigh = Color(0xFF1B1C1F),
    surfaceContainerHighest = Color(0xFF151618),
)

/** Схема цветов палитры [palette] в светлом или тёмном варианте. */
@Composable
@ReadOnlyComposable
fun colorSchemeFor(palette: ThemePalette, dark: Boolean, pureBlack: Boolean = false): ColorScheme {
    val scheme = when {
        palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        palette == ThemePalette.STEEL || palette == ThemePalette.DYNAMIC -> if (dark) DarkColors else LightColors
        else -> if (dark) darkScheme(palette) else lightScheme(palette)
    }
    return if (dark && pureBlack) scheme.pureBlack() else scheme
}

/** Тёмная ли тема для выбранного режима. */
@Composable
@ReadOnlyComposable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Тёмная ли текущая тема (по фону) — вместо системной настройки, которую тема может переопределять. */
val isAppInDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun SonnikTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    palette: ThemePalette = ThemePalette.STEEL,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = colorSchemeFor(palette, isDarkTheme(mode), pureBlack), content = content)
}

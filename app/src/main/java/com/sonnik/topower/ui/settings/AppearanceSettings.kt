package com.sonnik.topower.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.ui.LocalAppSettings
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.ui.theme.ThemeMode
import com.sonnik.topower.ui.theme.ThemePalette
import com.sonnik.topower.ui.theme.colorSchemeFor
import com.sonnik.topower.ui.theme.isDarkTheme

private const val COLUMNS = 3

/** Оформление: светлая/тёмная тема, палитра и чёрный фон. */
@Composable
fun AppearanceSettings() {
    val settings = LocalAppSettings.current
    val mode by settings.themeMode.collectAsStateWithLifecycle()
    val palette by settings.themePalette.collectAsStateWithLifecycle()
    val pureBlack by settings.pureBlack.collectAsStateWithLifecycle()
    val femaleBody by settings.femaleBody.collectAsStateWithLifecycle()
    val dark = isDarkTheme(mode)

    SectionCard {
        Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(
                    selected = m == mode,
                    onClick = { settings.setThemeMode(m) },
                    label = { Text(stringResource(modeLabel(m))) },
                )
            }
        }
        Text(stringResource(R.string.body_type_title), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !femaleBody, onClick = { settings.setFemaleBody(false) }, label = { Text(stringResource(R.string.body_male)) })
            FilterChip(selected = femaleBody, onClick = { settings.setFemaleBody(true) }, label = { Text(stringResource(R.string.body_female)) })
        }
        Text(stringResource(R.string.theme_palette), style = MaterialTheme.typography.bodyMedium)
        ThemePalette.available.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    PaletteSwatch(
                        palette = p,
                        dark = dark,
                        pureBlack = pureBlack,
                        selected = p == palette,
                        onClick = { settings.setThemePalette(p) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (mode != ThemeMode.LIGHT) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.theme_pure_black))
                    Text(
                        stringResource(R.string.theme_pure_black_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = pureBlack, onCheckedChange = { settings.setPureBlack(it) })
            }
        }
    }
}

/** Образец палитры в текущем режиме: фон, карточка с основным акцентом и вторым акцентом. */
@Composable
private fun PaletteSwatch(
    palette: ThemePalette,
    dark: Boolean,
    pureBlack: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = colorSchemeFor(palette, dark, pureBlack)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.background)
                .padding(6.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.surfaceContainer)
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.width(36.dp).height(6.dp).clip(CircleShape).background(scheme.primary))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(scheme.tertiary))
                    Box(Modifier.width(22.dp).height(8.dp).clip(CircleShape).background(scheme.primaryContainer))
                }
            }
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                        .padding(2.dp),
                )
            }
        }
        Text(
            stringResource(paletteLabel(palette)),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun modeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}

private fun paletteLabel(palette: ThemePalette): Int = when (palette) {
    ThemePalette.STEEL -> R.string.palette_steel
    ThemePalette.FOREST -> R.string.palette_forest
    ThemePalette.SUNSET -> R.string.palette_sunset
    ThemePalette.AMETHYST -> R.string.palette_amethyst
    ThemePalette.GRAPHITE -> R.string.palette_graphite
    ThemePalette.FORGE -> R.string.palette_forge
    ThemePalette.GARRISON -> R.string.palette_garrison
    ThemePalette.CYBERPUNK -> R.string.palette_cyberpunk
    ThemePalette.DYNAMIC -> R.string.palette_dynamic
}

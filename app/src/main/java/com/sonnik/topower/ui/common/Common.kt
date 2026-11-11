@file:OptIn(ExperimentalMaterial3Api::class)

package com.sonnik.topower.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.sonnik.topower.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.domain.isValidNumber
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetEntity
import com.sonnik.topower.domain.formatDuration
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.ui.inventory.equipmentName
import com.sonnik.topower.ui.inventory.rangeKg
import kotlin.math.abs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.Locale

/** Открывает настройки; задаётся в корне навигации. */
val LocalOpenSettings = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Экран со своей верхней панелью. Отступы системных панелей берёт на себя внешний Scaffold.
 * [showSettings] добавляет кнопку настроек — на экранах вкладок.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    showSettings: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    actions()
                    if (showSettings) {
                        val openSettings = LocalOpenSettings.current
                        IconButton(onClick = openSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_settings))
                        }
                    }
                },
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/**
 * Карточка приложения: как в Material 3, но в светлой теме с мягкой тенью, чтобы карточки
 * отделялись от фона. В тёмной теме тени не видно — там их различает цвет.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAppInDarkTheme) 0.dp else 1.5.dp),
        content = content,
    )
}

/** Плитка статистики: подпись, значение и значок. */
data class Stat(val label: String, val value: String, val icon: ImageVector? = null)

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Card(modifier = modifier) {
        // Значок — в правом верхнем углу, чтобы плитка не становилась выше.
        Box(Modifier.padding(12.dp).fillMaxWidth()) {
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (icon != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

/** Ряд плиток статистики одинаковой ширины. */
@Composable
fun StatRow(vararg tiles: Stat) {
    // Плитки одной высоты, даже если подпись у одной из них в две строки.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { StatTile(it.label, it.value, Modifier.weight(1f).fillMaxHeight(), it.icon) }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.delete),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value.trim()); onDismiss() },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
    supportingText: String? = null,
    allowNegative: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val allowed = when {
                decimal && allowNegative -> "0123456789.,-"
                decimal -> "0123456789.,"
                else -> "0123456789"
            }
            onValueChange(text.filter { it in allowed })
        },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        // Неразборчивое число («1.2.3», слишком большое) подсвечивается, а не пропадает молча.
        isError = !isValidNumber(value, decimal, allowNegative),
        modifier = modifier,
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
    )
}

// ---- Форматирование ----

private fun zoned(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())

/** Шаблон даты без года, если дата в текущем году: «1 окт.» / «1 окт. 2025». */
private fun datePattern(millis: Long): String =
    if (zoned(millis).year == LocalDate.now().year) "d MMM" else "d MMM yyyy"

private fun format(millis: Long, pattern: String): String =
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(zoned(millis))

/** «1 окт.» */
fun formatDate(millis: Long): String = format(millis, datePattern(millis))

/** «1 окт., 19:57» */
fun formatDateTime(millis: Long): String = format(millis, datePattern(millis) + ", HH:mm")

/** «ср, 1 окт. · 19:57» — для списков тренировок. */
fun formatDayDateTime(millis: Long): String = format(millis, "EEE, " + datePattern(millis) + " · HH:mm")

/** «01.10» — подписи на графиках. */
fun formatShortDate(millis: Long): String = format(millis, "dd.MM")

@Composable
fun kg(value: Double): String = stringResource(R.string.fmt_kg, formatNumber(value))

/**
 * Текст подхода, например «60 кг × 10», «2 × 10 кг · 12 повт.», «СВ+10 кг × 8 (90 кг)»,
 * «Красная+Синяя 18–39 кг × 12», «1:30», «5 км».
 * [bodyWeight] — вес тела на тренировке, нужен для упражнений с учётом собственного веса;
 * [equipment] — инвентарь по id, чтобы показать названия резин.
 */
@Composable
fun formatSet(
    set: SetEntity,
    exercise: ExerciseEntity,
    bodyWeight: Double?,
    equipment: Map<String, EquipmentEntity> = emptyMap(),
): String {
    val parts = mutableListOf<String>()
    val weight = set.weight
    val bandMin = set.bandMin
    val bandText: String? = if (bandMin != null && weight != null) {
        val names = listOfNotNull(set.equipmentId, set.equipment2Id)
            .mapNotNull { equipment[it] }
            .map { equipmentName(it) }
            .joinToString("+")
        val range = rangeKg(bandMin, abs(weight))
        if (names.isEmpty()) range else "$names $range"
    } else {
        null
    }
    val load: String? = when {
        exercise.useBodyWeight -> {
            val extra = weight ?: 0.0
            val bw = stringResource(R.string.fmt_bodyweight)
            val base = when {
                bandText != null -> if (extra < 0) "$bw − $bandText" else "$bw + $bandText"
                extra > 0 -> "$bw+${kg(extra)}"
                extra < 0 -> "$bw−${kg(-extra)}"
                else -> bw
            }
            if (bodyWeight != null) "$base (${kg(bodyWeight + extra)})" else base
        }
        bandText != null -> bandText
        weight != null && (exercise.trackWeight || exercise.equipmentType != null) ->
            if (set.pair) "2 × ${kg(weight)}" else kg(weight)
        else -> null
    }
    val reps = set.reps
    when {
        load != null && reps != null && set.pair -> {
            parts += load
            parts += stringResource(R.string.fmt_reps, reps.toString())
        }
        load != null && reps != null -> parts += "$load × $reps"
        load != null -> parts += load
        reps != null -> parts += stringResource(R.string.fmt_reps, reps.toString())
    }
    set.durationSec?.let { parts += formatDuration(it) }
    set.distanceKm?.let { parts += stringResource(R.string.fmt_km, formatNumber(it)) }
    return parts.joinToString(" · ")
}

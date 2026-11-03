@file:OptIn(ExperimentalLayoutApi::class)

package com.sonnik.topower.ui.muscles

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.ui.theme.isAppInDarkTheme
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleState
import com.sonnik.topower.domain.MuscleStatus
import com.sonnik.topower.domain.MuscleSets
import com.sonnik.topower.domain.VolumeLevel
import com.sonnik.topower.domain.volumeLevel
import com.sonnik.topower.domain.weeklySets
import com.sonnik.topower.reminders.muscleStatuses
import com.sonnik.topower.domain.recoveredFraction
import com.sonnik.topower.domain.state
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Строка с названием мышцы. */
fun muscleNameRes(muscle: Muscle): Int =
    when (muscle) {
        Muscle.CHEST -> R.string.m_chest
        Muscle.FRONT_DELTS -> R.string.m_front_delts
        Muscle.SIDE_DELTS -> R.string.m_side_delts
        Muscle.REAR_DELTS -> R.string.m_rear_delts
        Muscle.TRAPS -> R.string.m_traps
        Muscle.LATS -> R.string.m_lats
        Muscle.LOWER_BACK -> R.string.m_lower_back
        Muscle.BICEPS -> R.string.m_biceps
        Muscle.TRICEPS -> R.string.m_triceps
        Muscle.FOREARMS -> R.string.m_forearms
        Muscle.ABS -> R.string.m_abs
        Muscle.OBLIQUES -> R.string.m_obliques
        Muscle.GLUTES -> R.string.m_glutes
        Muscle.QUADS -> R.string.m_quads
        Muscle.HAMSTRINGS -> R.string.m_hamstrings
        Muscle.ADDUCTORS -> R.string.m_adductors
        Muscle.CALVES -> R.string.m_calves
        Muscle.BRACHIALIS -> R.string.m_brachialis
        Muscle.BRACHIORADIALIS -> R.string.m_brachioradialis
        Muscle.SERRATUS -> R.string.m_serratus
        Muscle.TERES_MAJOR -> R.string.m_teres_major
        Muscle.INFRASPINATUS -> R.string.m_infraspinatus
        Muscle.TIBIALIS -> R.string.m_tibialis
    }

@Composable
fun muscleName(muscle: Muscle): String = stringResource(muscleNameRes(muscle))

@Composable
fun muscleNames(muscles: Collection<Muscle>): String =
    muscles.sortedBy { Muscle.rank(it) }.map { muscleName(it) }.joinToString(", ")

private val Red = Color(0xFFE53935)
private val Orange = Color(0xFFFB8C00)
private val Yellow = Color(0xFFFDD835)
private val Green = Color(0xFF43A047)

/** Опорные цвета шкалы восстановления: доля восстановления → цвет. */
private val RecoveryStops = listOf(0f to Red, 0.35f to Orange, 0.7f to Yellow, 1f to Green)

/** Плавный цвет для доли восстановления 0…1: красный → оранжевый → жёлтый → зелёный. */
fun recoveryColor(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    val upper = RecoveryStops.indexOfFirst { it.first >= f }.coerceAtLeast(1)
    val (f0, c0) = RecoveryStops[upper - 1]
    val (f1, c1) = RecoveryStops[upper]
    return lerp(c0, c1, (f - f0) / (f1 - f0))
}

/** Цвет мышцы: по шкале восстановления, зелёный — восстановилась, серый — давно или никогда. */
fun muscleColor(status: MuscleStatus, now: Long, idle: Color): Color = when (status.state(now)) {
    MuscleState.RECOVERING -> recoveryColor(status.recoveredFraction(now))
    MuscleState.READY -> Green
    MuscleState.STALE, MuscleState.NEVER -> idle
}

/** Оставшееся время «ч:мм:сс» (часы не ограничены сутками: 71:59:12). */
fun formatCountdown(ms: Long): String {
    val total = (ms.coerceAtLeast(0) + 999) / 1000
    return "%d:%02d:%02d".format(total / 3600, total % 3600 / 60, total % 60)
}

/**
 * Текущее время, обновляемое каждую секунду, пока экран на виду. Тикает на фоновом диспетчере
 * в реальном времени, чтобы не мешать тестам Compose с виртуальными часами дождаться простоя.
 */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        // В тестах на Robolectric отсчёт не нужен: ежесекундные перерисовки мешают дождаться простоя.
        if (Build.FINGERPRINT == "robolectric") return@LaunchedEffect
        withContext(Dispatchers.Default) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    now = System.currentTimeMillis()
                    delay(1000 - now % 1000)
                }
            }
        }
    }
    return now
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Легенда: градиент восстановления от «только что» до «готова» и серый «давно». */
@Composable
private fun RecoveryLegend(idle: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(*RecoveryStops.toTypedArray()))
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.muscles_legend_fresh),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.muscles_ready),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LegendDot(idle, stringResource(R.string.muscles_stale))
    }
}

/** Карточка «Состояние мышц» для первого экрана: тело спереди и сзади, легенда и подсказка. */
@Composable
fun MuscleStatusCard(modifier: Modifier = Modifier) {
    val repo = LocalRepository.current
    val rows by remember { repo.dao.observeMuscleWork() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val now = rememberNow()
    val statuses = remember(rows) { muscleStatuses(rows) }
    var volumeMode by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<Muscle?>(null) }
    val volume = remember(rows, now / VOLUME_REFRESH_MS) {
        weeklySets(rows.map { MuscleSets(it.time, Muscle.parse(it.muscles), it.sets) }, now)
    }
    val idle = if (isAppInDarkTheme) Color(0xFF4A515B) else Color(0xFFC3C9D1)
    val bodyColor = MaterialTheme.colorScheme.surfaceVariant
    val gapColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val colorOf: (Muscle) -> Color = if (volumeMode) {
        { m -> volumeColor(volumeLevel(volume[m] ?: 0), idle) }
    } else {
        { m -> statuses[m]?.let { muscleColor(it, now, idle) } ?: idle }
    }

    var guide by rememberSaveable { mutableStateOf(false) }
    if (guide) MuscleGuideDialog(onDismiss = { guide = false })

    SectionCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.muscles_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            MuscleGuideButton { guide = true }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !volumeMode,
                onClick = { volumeMode = false },
                label = { Text(stringResource(R.string.muscles_mode_recovery)) },
            )
            FilterChip(
                selected = volumeMode,
                onClick = { volumeMode = true },
                label = { Text(stringResource(R.string.muscles_mode_volume)) },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to R.string.muscles_front, true to R.string.muscles_back).forEach { (back, label) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BodyFigure(
                        back = back,
                        colorOf = colorOf,
                        bodyColor = bodyColor,
                        gapColor = gapColor,
                        modifier = Modifier.fillMaxWidth(),
                        selected = selected,
                        selectedColor = MaterialTheme.colorScheme.onSurface,
                        onTap = { m -> selected = if (selected == m) null else m },
                    )
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        selected?.let { m ->
            SelectedMuscleInfo(m, statuses[m], volume[m] ?: 0, volumeMode, now)
        } ?: Text(
            stringResource(R.string.muscles_tap_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (volumeMode) {
            VolumeLegend(idle)
            VolumeDetails(volume)
            return@SectionCard
        }
        RecoveryLegend(idle)

        val all = statuses.values
        if (all.all { it.lastTrained == null }) {
            Text(
                stringResource(R.string.muscles_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Сначала — дольше всех отдыхавшие: им нагрузка нужнее.
            val ready = all.filter { it.lastTrained != null && it.state(now) != MuscleState.RECOVERING }
                .sortedBy { it.lastTrained }
            val recovering = all.filter { it.state(now) == MuscleState.RECOVERING }.sortedBy { it.readyAt }
            if (ready.isNotEmpty()) {
                Text(
                    stringResource(R.string.muscles_train_now, ready.map { muscleName(it.muscle) }.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (recovering.isNotEmpty()) {
                val fullRecovery = recovering.maxOf { it.readyAt ?: now }
                Text(
                    stringResource(R.string.muscles_full_recovery, formatCountdown(fullRecovery - now)),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                recovering.forEach { status ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(muscleColor(status, now, idle), CircleShape)
                        )
                        Text(
                            muscleName(status.muscle),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                        Text(
                            formatCountdown((status.readyAt ?: now) - now),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Нажатая на схеме мышца: название и состояние восстановления или объём за неделю. */
@Composable
private fun SelectedMuscleInfo(muscle: Muscle, status: MuscleStatus?, sets: Int, volumeMode: Boolean, now: Long) {
    val detail = when {
        volumeMode -> stringResource(R.string.volume_sets_week, sets)
        status == null -> stringResource(R.string.muscle_never)
        else -> when (status.state(now)) {
            MuscleState.RECOVERING -> stringResource(R.string.muscle_ready_in, formatCountdown((status.readyAt ?: now) - now))
            MuscleState.READY -> stringResource(R.string.muscle_ready_now)
            MuscleState.STALE -> stringResource(R.string.muscle_stale_long)
            MuscleState.NEVER -> stringResource(R.string.muscle_never)
        }
    }
    Text(
        muscleName(muscle) + " — " + detail,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Объём пересчитывается раз в минуту — точнее не нужно. */
private const val VOLUME_REFRESH_MS = 60_000L

private val VolumeLow = Color(0xFFFFD54F)
private val VolumeModerate = Color(0xFFAED581)
private val VolumeHigh = Color(0xFF8E24AA)

/** Цвет мышцы по недельному объёму: мало — жёлтый, норма — зелёный, много — фиолетовый. */
fun volumeColor(level: VolumeLevel, idle: Color): Color = when (level) {
    VolumeLevel.NONE -> idle
    VolumeLevel.LOW -> VolumeLow
    VolumeLevel.MODERATE -> VolumeModerate
    VolumeLevel.OPTIMAL -> Green
    VolumeLevel.HIGH -> VolumeHigh
}

@Composable
private fun VolumeLegend(idle: Color) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegendDot(idle, "0")
        LegendDot(VolumeLow, "1–5")
        LegendDot(VolumeModerate, "6–9")
        LegendDot(Green, stringResource(R.string.volume_optimal))
        LegendDot(VolumeHigh, "21+")
    }
}

/** Подходы за неделю по мышцам: кому не хватает нагрузки и сколько у остальных. */
@Composable
private fun VolumeDetails(volume: Map<Muscle, Int>) {
    Text(
        stringResource(R.string.volume_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val none = volume.filterValues { it == 0 }.keys.sortedBy { Muscle.rank(it) }
    val low = volume.filterValues { it in 1..9 }.keys.sortedBy { volume[it] }
    if (low.isNotEmpty()) {
        Text(stringResource(R.string.volume_low, muscleNames(low)), style = MaterialTheme.typography.bodyMedium)
    }
    if (none.isNotEmpty() && none.size < Muscle.entries.size) {
        Text(
            stringResource(R.string.volume_none, muscleNames(none)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    volume.filterValues { it > 0 }.entries.sortedByDescending { it.value }.forEach { (muscle, sets) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(volumeColor(volumeLevel(sets), Color.Gray), CircleShape)
            )
            Text(
                muscleName(muscle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Text(
                stringResource(R.string.volume_sets, sets),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Кнопка «?» рядом со схемой: открывает подсказку с названиями мышц. */
@Composable
fun MuscleGuideButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.muscles_guide),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Подсказка «Названия мышц»: нажатие на мышцу на схеме или на её название подсвечивает её
 * и показывает название. [selected] — мышцы упражнения, они слегка подкрашены.
 */
@Composable
fun MuscleGuideDialog(onDismiss: () -> Unit, selected: Set<Muscle> = emptySet()) {
    var current by rememberSaveable { mutableStateOf<Muscle?>(null) }
    val accent = MaterialTheme.colorScheme.primary
    val tint = accent.copy(alpha = 0.35f)
    val idle = if (isAppInDarkTheme) Color(0xFF4A515B) else Color(0xFFC3C9D1)
    val bodyColor = MaterialTheme.colorScheme.surfaceVariant
    val gapColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val colorOf: (Muscle) -> Color = { m ->
        when (m) {
            current -> accent
            in selected -> tint
            else -> idle
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.muscles_guide)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(false to R.string.muscles_front, true to R.string.muscles_back).forEach { (back, label) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            BodyFigure(
                                back = back,
                                colorOf = colorOf,
                                bodyColor = bodyColor,
                                gapColor = gapColor,
                                modifier = Modifier.fillMaxWidth(),
                                onTap = { current = it },
                            )
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                val name = current?.let { muscleName(it) }
                Text(
                    name ?: stringResource(R.string.muscles_guide_hint),
                    style = if (name != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                    color = if (name != null) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Muscle.displayOrder.forEach { m ->
                        FilterChip(
                            selected = m == current,
                            onClick = { current = if (m == current) null else m },
                            label = { Text(muscleName(m)) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}

/** Выбор основных мышц упражнения. */
@Composable
fun MusclePicker(selected: Set<Muscle>, onChange: (Set<Muscle>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Muscle.displayOrder.forEach { m ->
            FilterChip(
                selected = m in selected,
                onClick = { onChange(if (m in selected) selected - m else selected + m) },
                label = { Text(muscleName(m)) },
            )
        }
    }
}

/** Варианты ответа «насколько тяжело было» и соответствующий срок восстановления, часов. */
val RECOVERY_OPTIONS = listOf(
    24 to R.string.recovery_easy,
    48 to R.string.recovery_normal,
    72 to R.string.recovery_hard,
    96 to R.string.recovery_max,
)

@Composable
fun RecoveryDialog(exerciseName: String, current: Int?, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.recovery_title))
                Text(exerciseName, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.recovery_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RECOVERY_OPTIONS.forEach { (hours, label) ->
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hours == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(hours); onDismiss() }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

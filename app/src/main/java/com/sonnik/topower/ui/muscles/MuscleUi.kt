@file:OptIn(ExperimentalLayoutApi::class)

package com.sonnik.topower.ui.muscles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleState
import com.sonnik.topower.domain.MuscleStatus
import com.sonnik.topower.domain.MuscleWork
import com.sonnik.topower.domain.computeMuscleStatus
import com.sonnik.topower.domain.hoursLeft
import com.sonnik.topower.domain.recoveredFraction
import com.sonnik.topower.domain.state
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.SectionCard
import kotlinx.coroutines.delay

@Composable
fun muscleName(muscle: Muscle): String = stringResource(
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
    }
)

@Composable
fun muscleNames(muscles: Collection<Muscle>): String =
    muscles.sortedBy { it.ordinal }.map { muscleName(it) }.joinToString(", ")

private val Red = Color(0xFFE53935)
private val Orange = Color(0xFFFB8C00)
private val Yellow = Color(0xFFFDD835)
private val Green = Color(0xFF43A047)

/** Красный → оранжевый → жёлтый по мере восстановления, зелёный — восстановилась, серый — давно или никогда. */
fun muscleColor(status: MuscleStatus, now: Long, idle: Color): Color = when (status.state(now)) {
    MuscleState.RECOVERING -> {
        val f = status.recoveredFraction(now)
        if (f < 0.5f) lerp(Red, Orange, f * 2) else lerp(Orange, Yellow, (f - 0.5f) * 2)
    }
    MuscleState.READY -> Green
    MuscleState.STALE, MuscleState.NEVER -> idle
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

/** Карточка «Состояние мышц» для первого экрана: тело спереди и сзади, легенда и подсказка. */
@Composable
fun MuscleStatusCard(modifier: Modifier = Modifier) {
    val repo = LocalRepository.current
    val rows by remember { repo.dao.observeMuscleWork() }.collectAsStateWithLifecycle(initialValue = emptyList())
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val statuses = remember(rows) {
        computeMuscleStatus(rows.map { MuscleWork(it.workoutId, it.time, Muscle.parse(it.muscles), it.recoveryHours) })
    }
    val idle = if (isSystemInDarkTheme()) Color(0xFF4A515B) else Color(0xFFC3C9D1)
    val bodyColor = MaterialTheme.colorScheme.surfaceVariant
    val gapColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val colorOf: (Muscle) -> Color = { m -> statuses[m]?.let { muscleColor(it, now, idle) } ?: idle }

    SectionCard(modifier) {
        Text(stringResource(R.string.muscles_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(false to R.string.muscles_front, true to R.string.muscles_back).forEach { (back, label) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BodyFigure(
                        back = back,
                        colorOf = colorOf,
                        bodyColor = bodyColor,
                        gapColor = gapColor,
                        modifier = Modifier.fillMaxWidth(0.85f),
                    )
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(Red, stringResource(R.string.muscles_recovering))
            LegendDot(Green, stringResource(R.string.muscles_ready))
            LegendDot(idle, stringResource(R.string.muscles_stale))
        }

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
                val text = recovering.map { stringResource(R.string.muscles_hours_left, muscleName(it.muscle), it.hoursLeft(now)) }
                Text(
                    stringResource(R.string.muscles_recovering_list, text.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Выбор основных мышц упражнения. */
@Composable
fun MusclePicker(selected: Set<Muscle>, onChange: (Set<Muscle>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Muscle.entries.forEach { m ->
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

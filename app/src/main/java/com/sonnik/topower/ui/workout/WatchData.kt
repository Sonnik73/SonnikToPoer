package com.sonnik.topower.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.data.WorkoutEntity
import com.sonnik.topower.domain.parseInt
import com.sonnik.topower.ui.common.NumberField

/** Значения со смарт-часов, введённые пользователем. */
data class WatchValues(
    val maxHeartRate: Int?,
    val avgHeartRate: Int?,
    val durationSec: Int?,
    val calories: Int?,
)

/**
 * Ввод данных с часов. При [finishing] диалог открывается кнопкой «Завершить»:
 * есть «Сохранить и завершить» и «Завершить без данных».
 */
@Composable
fun WatchDataDialog(
    workout: WorkoutEntity,
    finishing: Boolean,
    onSave: (WatchValues) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    var maxHr by rememberSaveable { mutableStateOf(workout.maxHeartRate?.toString().orEmpty()) }
    var avgHr by rememberSaveable { mutableStateOf(workout.avgHeartRate?.toString().orEmpty()) }
    var hours by rememberSaveable { mutableStateOf(workout.watchDurationSec?.let { (it / 3600).toString() }.orEmpty()) }
    var minutes by rememberSaveable { mutableStateOf(workout.watchDurationSec?.let { (it % 3600 / 60).toString() }.orEmpty()) }
    var calories by rememberSaveable { mutableStateOf(workout.calories?.toString().orEmpty()) }

    fun collect(): WatchValues {
        val h = parseInt(hours)
        val m = parseInt(minutes)
        val duration = if (h == null && m == null) null else ((h ?: 0) * 3600 + (m ?: 0) * 60)
        return WatchValues(
            maxHeartRate = parseInt(maxHr)?.takeIf { it > 0 },
            avgHeartRate = parseInt(avgHr)?.takeIf { it > 0 },
            durationSec = duration?.takeIf { it > 0 },
            calories = parseInt(calories)?.takeIf { it > 0 },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.watch_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.watch_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberField(maxHr, { maxHr = it }, stringResource(R.string.watch_max_hr), Modifier.fillMaxWidth(), decimal = false)
                NumberField(avgHr, { avgHr = it }, stringResource(R.string.watch_avg_hr), Modifier.fillMaxWidth(), decimal = false)
                Text(stringResource(R.string.watch_duration), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(hours, { hours = it }, stringResource(R.string.watch_hours), Modifier.weight(1f), decimal = false)
                    NumberField(minutes, { minutes = it }, stringResource(R.string.watch_minutes), Modifier.weight(1f), decimal = false)
                }
                NumberField(calories, { calories = it }, stringResource(R.string.watch_calories), Modifier.fillMaxWidth(), decimal = false)
                if (finishing) {
                    TextButton(onClick = { onSkip(); onDismiss() }) { Text(stringResource(R.string.watch_skip_finish)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(collect()); onDismiss() }) {
                Text(stringResource(if (finishing) R.string.watch_save_finish else R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Короткая сводка «❤ макс 165 · ср 128 · ⏱ 1:12 · 🔥 540 ккал» или null, если данных нет. */
@Composable
fun watchSummary(workout: WorkoutEntity): String? {
    if (!workout.hasWatchData) return null
    val parts = mutableListOf<String>()
    workout.maxHeartRate?.let { parts += stringResource(R.string.watch_max_hr_short, it) }
    workout.avgHeartRate?.let { parts += stringResource(R.string.watch_avg_hr_short, it) }
    workout.watchDurationSec?.let { parts += stringResource(R.string.watch_duration_short, formatHoursMinutes(it)) }
    workout.calories?.let { parts += stringResource(R.string.watch_calories_short, it) }
    return parts.joinToString(" · ")
}

/** Секунды в «1:05» (часы:минуты). */
fun formatHoursMinutes(totalSec: Int): String = "%d:%02d".format(totalSec / 3600, totalSec % 3600 / 60)

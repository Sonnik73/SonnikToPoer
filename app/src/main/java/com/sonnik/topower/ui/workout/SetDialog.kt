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
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetEntity
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.domain.parseDecimal
import com.sonnik.topower.domain.parseInt
import com.sonnik.topower.ui.common.NumberField
import com.sonnik.topower.ui.exercises.CheckRow

data class SetValues(
    val reps: Int?,
    val weight: Double?,
    val durationSec: Int?,
    val distanceKm: Double?,
    val isWarmup: Boolean,
)

/**
 * Ввод подхода. Показываются только поля, которые измеряются в упражнении.
 * [prefill] — подход, значениями которого заполняется форма.
 */
@Composable
fun SetDialog(
    exercise: ExerciseEntity,
    setNumber: Int,
    isNew: Boolean,
    prefill: SetEntity?,
    bodyWeight: Double?,
    onSave: (SetValues) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val showWeight = exercise.trackWeight || exercise.useBodyWeight
    var reps by rememberSaveable { mutableStateOf(prefill?.reps?.toString().orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(prefill?.weight?.let(::formatNumber).orEmpty()) }
    var minutes by rememberSaveable { mutableStateOf(prefill?.durationSec?.let { (it / 60).toString() }.orEmpty()) }
    var seconds by rememberSaveable { mutableStateOf(prefill?.durationSec?.let { (it % 60).toString() }.orEmpty()) }
    var distance by rememberSaveable { mutableStateOf(prefill?.distanceKm?.let(::formatNumber).orEmpty()) }
    var warmup by rememberSaveable { mutableStateOf(if (isNew) false else prefill?.isWarmup ?: false) }
    var showError by rememberSaveable { mutableStateOf(false) }

    fun collect(): SetValues {
        val min = parseInt(minutes)
        val sec = parseInt(seconds)
        val duration = if (min == null && sec == null) null else (min ?: 0) * 60 + (sec ?: 0)
        return SetValues(
            reps = if (exercise.trackReps) parseInt(reps) else null,
            weight = if (showWeight) parseDecimal(weight) else null,
            durationSec = if (exercise.trackTime) duration else null,
            distanceKm = if (exercise.trackDistance) parseDecimal(distance) else null,
            isWarmup = warmup,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (isNew) stringResource(R.string.new_set) else stringResource(R.string.set_title, setNumber))
                Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (exercise.trackReps) {
                    NumberField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = stringResource(R.string.reps_label),
                        decimal = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showWeight) {
                    val extra = parseDecimal(weight) ?: 0.0
                    NumberField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = stringResource(
                            if (exercise.useBodyWeight) R.string.extra_weight_label else R.string.weight_label
                        ),
                        supportingText = when {
                            exercise.useBodyWeight && bodyWeight != null ->
                                stringResource(R.string.working_weight, formatNumber(bodyWeight + extra))
                            exercise.useBodyWeight -> stringResource(R.string.extra_weight_hint)
                            else -> null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (exercise.trackTime) {
                    Text(stringResource(R.string.time_label), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = minutes,
                            onValueChange = { minutes = it },
                            label = stringResource(R.string.time_min),
                            decimal = false,
                            modifier = Modifier.weight(1f),
                        )
                        NumberField(
                            value = seconds,
                            onValueChange = { seconds = it },
                            label = stringResource(R.string.time_sec),
                            decimal = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (exercise.trackDistance) {
                    NumberField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = stringResource(R.string.distance_label),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CheckRow(warmup, stringResource(R.string.warmup), { warmup = it })
                if (showError) {
                    Text(
                        stringResource(R.string.set_empty_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (onDelete != null) {
                    TextButton(onClick = { onDelete(); onDismiss() }) {
                        Text(stringResource(R.string.delete_set), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val values = collect()
                val empty = values.reps == null && values.weight == null &&
                    values.durationSec == null && values.distanceKm == null
                if (empty) {
                    showError = true
                } else {
                    onSave(values)
                    onDismiss()
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

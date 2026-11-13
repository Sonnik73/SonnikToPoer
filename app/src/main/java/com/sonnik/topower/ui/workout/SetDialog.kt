@file:OptIn(ExperimentalLayoutApi::class)

package com.sonnik.topower.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetEntity
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.domain.parseDecimal
import com.sonnik.topower.domain.isValidNumber
import com.sonnik.topower.domain.parseInt
import com.sonnik.topower.ui.common.AppIcons
import com.sonnik.topower.ui.common.NumberField
import com.sonnik.topower.ui.exercises.CheckRow
import com.sonnik.topower.ui.inventory.ColorDot
import com.sonnik.topower.ui.inventory.equipmentChipLabel
import com.sonnik.topower.ui.inventory.rangeKg

data class SetValues(
    val reps: Int?,
    val weight: Double?,
    val durationSec: Int?,
    val distanceKm: Double?,
    val isWarmup: Boolean,
    val equipmentId: String? = null,
    val equipment2Id: String? = null,
    val pair: Boolean = false,
    val bandMin: Double? = null,
)

private const val MAX_BANDS = 2

/** Шаг кнопок ± у веса: как двойная прогрессия — блины по 2,5 кг. */
private const val WEIGHT_STEP = 2.5

/**
 * Числовое поле с кнопками ±: повтор последнего подхода правится на одну-две единицы,
 * вес — на шаг [WEIGHT_STEP], без набора на клавиатуре.
 */
@Composable
private fun StepperField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    step: Double,
    decimal: Boolean,
    allowNegative: Boolean = false,
    supportingText: String? = null,
) {
    fun bump(delta: Double) {
        val current = (if (decimal) parseDecimal(value) else parseInt(value)?.toDouble())
            ?: if (delta > 0) 0.0 else step // пустое поле: «+» начинает с шага, «−» не уходит в минус
        val next = if (allowNegative) current + delta else (current + delta).coerceAtLeast(0.0)
        onValueChange(formatNumber(next, if (decimal) 2 else 0))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { bump(-step) }) { Icon(AppIcons.Minus, contentDescription = stringResource(R.string.decrease)) }
        NumberField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            decimal = decimal,
            supportingText = supportingText,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { bump(step) }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase)) }
    }
}

/**
 * Ввод подхода. Показываются только поля, которые измеряются в упражнении.
 * [prefill] — подход, значениями которого заполняется форма.
 * [equipment] — инвентарь того типа, что указан в упражнении: выбирается одним нажатием.
 */
@Composable
fun SetDialog(
    exercise: ExerciseEntity,
    setNumber: Int,
    isNew: Boolean,
    prefill: SetEntity?,
    bodyWeight: Double?,
    equipment: List<EquipmentEntity>,
    onSave: (SetValues) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val equipmentType = EquipmentType.of(exercise.equipmentType)
    val isBand = equipmentType?.isBand == true
    val byId = equipment.associateBy { it.id }
    val showWeight = exercise.trackWeight || exercise.useBodyWeight || equipmentType != null
    // Минус — помощь в упражнениях с весом тела; и в старых подходах, где он уже записан.
    val allowNegativeWeight = exercise.useBodyWeight || (prefill?.weight ?: 0.0) < 0

    var reps by rememberSaveable { mutableStateOf(prefill?.reps?.toString().orEmpty()) }
    var weight by rememberSaveable {
        mutableStateOf(prefill?.takeIf { it.bandMin == null }?.weight?.let(::formatNumber).orEmpty())
    }
    var minutes by rememberSaveable { mutableStateOf(prefill?.durationSec?.let { (it / 60).toString() }.orEmpty()) }
    var seconds by rememberSaveable { mutableStateOf(prefill?.durationSec?.let { (it % 60).toString() }.orEmpty()) }
    var distance by rememberSaveable { mutableStateOf(prefill?.distanceKm?.let(::formatNumber).orEmpty()) }
    var warmup by rememberSaveable { mutableStateOf(if (isNew) false else prefill?.isWarmup ?: false) }
    var selected by rememberSaveable {
        mutableStateOf(listOfNotNull(prefill?.equipmentId, prefill?.equipment2Id).filter { it in byId })
    }
    var pair by rememberSaveable { mutableStateOf(prefill?.pair ?: false) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var showInvalid by rememberSaveable { mutableStateOf(false) }

    // Клавиатура открыта из окна диалога: без явного скрытия она остаётся на экране тренировки.
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    fun close(action: () -> Unit = {}) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        action()
        onDismiss()
    }

    val selectedBands = if (isBand) selected.mapNotNull { byId[it] } else emptyList()
    val bandMin = selectedBands.sumOf { it.minResistance ?: 0.0 }
    val bandMax = selectedBands.sumOf { it.maxResistance ?: 0.0 }
    val usesBands = selectedBands.isNotEmpty()

    fun select(item: EquipmentEntity) {
        if (isBand) {
            selected = when {
                item.id in selected -> selected - item.id
                selected.size >= MAX_BANDS -> selected.drop(1) + item.id
                else -> selected + item.id
            }
        } else if (item.id in selected) {
            selected = emptyList()
        } else {
            selected = listOf(item.id)
            item.weight?.let { weight = formatNumber(it) }
        }
    }

    fun collect(): SetValues {
        val min = parseInt(minutes)
        val sec = parseInt(seconds)
        val duration = if (min == null && sec == null) null else (min ?: 0) * 60 + (sec ?: 0)
        val manualWeight = parseDecimal(weight)
        // Обычный снаряд остаётся привязанным, только если вес не меняли вручную.
        val single = selected.firstOrNull()?.let { byId[it] }?.takeIf { !isBand && it.weight == manualWeight }
        return SetValues(
            reps = if (exercise.trackReps) parseInt(reps) else null,
            weight = when {
                !showWeight -> null
                usesBands -> if (exercise.bandAssist) -bandMax else bandMax
                else -> manualWeight
            },
            durationSec = if (exercise.trackTime) duration else null,
            distanceKm = if (exercise.trackDistance) parseDecimal(distance) else null,
            isWarmup = warmup,
            equipmentId = if (usesBands) selected[0] else single?.id,
            equipment2Id = if (usesBands) selected.getOrNull(1) else null,
            pair = equipmentType?.canPair == true && pair,
            bandMin = if (usesBands) bandMin else null,
        )
    }

    AlertDialog(
        onDismissRequest = { close() },
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
                if (equipmentType != null) {
                    if (equipment.isEmpty()) {
                        Text(
                            stringResource(R.string.no_equipment_of_type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            equipment.forEach { item ->
                                FilterChip(
                                    selected = item.id in selected,
                                    onClick = { select(item) },
                                    label = { Text(equipmentChipLabel(item)) },
                                    leadingIcon = if (item.color != null) {
                                        { ColorDot(item.color) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                        if (isBand) {
                            Text(
                                if (usesBands) {
                                    stringResource(
                                        if (exercise.bandAssist) R.string.band_assistance else R.string.band_resistance,
                                        rangeKg(bandMin, bandMax),
                                    )
                                } else {
                                    stringResource(R.string.band_pick_hint)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (equipmentType.canPair) {
                        CheckRow(pair, stringResource(R.string.pair_label), { pair = it })
                    }
                }
                if (exercise.trackReps) {
                    StepperField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = stringResource(R.string.reps_label),
                        step = 1.0,
                        decimal = false,
                    )
                }
                if (showWeight && !usesBands) {
                    val extra = parseDecimal(weight) ?: 0.0
                    StepperField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = stringResource(
                            if (exercise.useBodyWeight) R.string.extra_weight_label else R.string.weight_label
                        ),
                        // Отрицательный доп. вес — помощь (например, гравитрон), только для упражнений с весом тела.
                        step = WEIGHT_STEP,
                        decimal = true,
                        allowNegative = allowNegativeWeight,
                        supportingText = when {
                            exercise.useBodyWeight && bodyWeight != null ->
                                stringResource(R.string.working_weight, formatNumber(bodyWeight + extra))
                            exercise.useBodyWeight -> stringResource(R.string.extra_weight_hint)
                            else -> null
                        },
                    )
                }
                if (usesBands && exercise.useBodyWeight && bodyWeight != null) {
                    val extra = if (exercise.bandAssist) -bandMax else bandMax
                    Text(
                        stringResource(R.string.working_weight, formatNumber(bodyWeight + extra)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                if (showInvalid) {
                    Text(
                        stringResource(R.string.set_invalid_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (showError) {
                    Text(
                        stringResource(R.string.set_empty_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (onDelete != null) {
                    TextButton(onClick = { close(onDelete) }) {
                        Text(stringResource(R.string.delete_set), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Видимые поля с неразборчивым числом: такой подход не сохраняем, а просим исправить.
                val invalid = (exercise.trackReps && !isValidNumber(reps, decimal = false)) ||
                    (showWeight && !usesBands && !isValidNumber(weight, allowNegative = allowNegativeWeight)) ||
                    (exercise.trackTime && !(isValidNumber(minutes, decimal = false) && isValidNumber(seconds, decimal = false))) ||
                    (exercise.trackDistance && !isValidNumber(distance))
                showInvalid = invalid
                val values = collect()
                val empty = values.reps == null && values.weight == null &&
                    values.durationSec == null && values.distanceKm == null
                if (invalid) {
                    showError = false
                } else if (empty) {
                    showError = true
                } else {
                    close { onSave(values) }
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = { close() }) { Text(stringResource(R.string.cancel)) } },
    )
}

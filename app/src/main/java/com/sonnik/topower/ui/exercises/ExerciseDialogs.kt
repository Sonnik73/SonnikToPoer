@file:OptIn(ExperimentalLayoutApi::class)

package com.sonnik.topower.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.ui.inventory.equipmentTypeLabel
import com.sonnik.topower.ui.LocalRepository

@Composable
fun CheckRow(checked: Boolean, text: String, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Text(text)
    }
}

/**
 * Создание или редактирование упражнения. [onSaved] вызывается после записи в базу,
 * поэтому сразу после него можно ссылаться на упражнение (добавлять в тренировку).
 */
@Composable
fun ExerciseEditDialog(
    initial: ExerciseEntity?,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSaved: (ExerciseEntity) -> Unit = {},
) {
    val repo = LocalRepository.current
    var name by rememberSaveable { mutableStateOf(initial?.name ?: initialName) }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var reps by rememberSaveable { mutableStateOf(initial?.trackReps ?: true) }
    var weight by rememberSaveable { mutableStateOf(initial?.trackWeight ?: true) }
    var time by rememberSaveable { mutableStateOf(initial?.trackTime ?: false) }
    var distance by rememberSaveable { mutableStateOf(initial?.trackDistance ?: false) }
    var bodyWeight by rememberSaveable { mutableStateOf(initial?.useBodyWeight ?: false) }
    var equipmentType by rememberSaveable { mutableStateOf(EquipmentType.of(initial?.equipmentType)) }
    var bandAssist by rememberSaveable { mutableStateOf(initial?.bandAssist ?: false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val nameError = name.isBlank()
    val trackError = !(reps || weight || time || distance || bodyWeight)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_exercise else R.string.edit_exercise)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    singleLine = true,
                    isError = showErrors && nameError,
                    supportingText = if (showErrors && nameError) {
                        { Text(stringResource(R.string.name_required)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.exercise_category)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.what_to_track),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                CheckRow(reps, stringResource(R.string.track_reps), { reps = it })
                CheckRow(weight, stringResource(R.string.track_weight), { weight = it })
                CheckRow(time, stringResource(R.string.track_time), { time = it })
                CheckRow(distance, stringResource(R.string.track_distance), { distance = it })
                if (showErrors && trackError) {
                    Text(
                        stringResource(R.string.track_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                CheckRow(bodyWeight, stringResource(R.string.use_body_weight), { bodyWeight = it })
                Text(
                    stringResource(R.string.use_body_weight_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.exercise_equipment), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = equipmentType == null,
                        onClick = { equipmentType = null },
                        label = { Text(stringResource(R.string.equipment_none)) },
                    )
                    EquipmentType.entries.forEach { t ->
                        FilterChip(
                            selected = equipmentType == t,
                            onClick = {
                                equipmentType = t
                                // Снаряд задаёт нагрузку, поэтому вес в подходе нужен.
                                weight = true
                            },
                            label = { Text(equipmentTypeLabel(t)) },
                        )
                    }
                }
                if (equipmentType == EquipmentType.BAND && bodyWeight) {
                    CheckRow(bandAssist, stringResource(R.string.band_assist), { bandAssist = it })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.exercise_note)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nameError || trackError) {
                    showErrors = true
                    return@TextButton
                }
                val base = initial ?: ExerciseEntity(name = name)
                val exercise = base.copy(
                    name = name.trim(),
                    category = category.trim(),
                    note = note.trim(),
                    trackReps = reps,
                    trackWeight = weight,
                    trackTime = time,
                    trackDistance = distance,
                    useBodyWeight = bodyWeight,
                    equipmentType = equipmentType?.name,
                    bandAssist = equipmentType == EquipmentType.BAND && bodyWeight && bandAssist,
                )
                repo.launch {
                    saveExercise(exercise)
                    onSaved(exercise)
                }
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Выбор нескольких упражнений из справочника с поиском и созданием нового. */
@Composable
fun ExercisePickerDialog(
    onDismiss: () -> Unit,
    onPicked: (List<String>) -> Unit,
) {
    val repo = LocalRepository.current
    val exercises by remember { repo.dao.observeExercises(includeArchived = false) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }
    var creating by rememberSaveable { mutableStateOf(false) }

    val filtered = exercises.filter {
        query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) ||
            it.category.contains(query.trim(), ignoreCase = true)
    }
    val exactMatch = exercises.any { it.name.equals(query.trim(), ignoreCase = true) }

    if (creating) {
        ExerciseEditDialog(
            initial = null,
            initialName = query.trim(),
            onDismiss = { creating = false },
            onSaved = { created ->
                onPicked(selected.toList() + created.id)
                onDismiss()
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_exercises)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    if (!exactMatch) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { creating = true }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, Modifier.padding(horizontal = 12.dp))
                                Text(
                                    if (query.isBlank()) stringResource(R.string.new_exercise)
                                    else stringResource(R.string.create_exercise_named, query.trim()),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    items(filtered, key = { it.id }) { exercise ->
                        CheckRow(
                            checked = exercise.id in selected,
                            text = if (exercise.category.isBlank()) exercise.name
                            else "${exercise.name} · ${exercise.category}",
                            onChange = { checked ->
                                if (checked) selected.add(exercise.id) else selected.remove(exercise.id)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = { onPicked(selected.toList()); onDismiss() },
            ) { Text(stringResource(R.string.add_selected, selected.size)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

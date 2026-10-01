package com.sonnik.topower.ui.workout

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.SetEntity
import com.sonnik.topower.data.WorkoutEntity
import com.sonnik.topower.data.WorkoutExerciseFull
import com.sonnik.topower.domain.SetRecord
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.domain.setScore
import com.sonnik.topower.timer.RestTimer
import com.sonnik.topower.ui.LocalAppSettings
import com.sonnik.topower.ui.LocalRestTimer
import com.sonnik.topower.ui.stats.config
import com.sonnik.topower.domain.parseDecimal
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.NumberField
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.TextInputDialog
import com.sonnik.topower.ui.common.formatDateTime
import com.sonnik.topower.ui.common.formatSet
import com.sonnik.topower.ui.exercises.ExercisePickerDialog
import com.sonnik.topower.ui.muscles.RecoveryDialog

/** Состояние открытого диалога подхода: [set] == null — новый подход. */
private data class SetDialogState(val item: WorkoutExerciseFull, val set: SetEntity?)

@Composable
fun WorkoutScreen(
    workoutId: String,
    onBack: () -> Unit,
    openStats: (String) -> Unit,
) {
    val repo = LocalRepository.current
    val workout by remember(workoutId) { repo.dao.observeWorkout(workoutId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val items by remember(workoutId) { repo.dao.observeWorkoutExercises(workoutId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Уведомления нужны таймеру отдыха при заблокированном экране (Android 13+).
    val context = LocalContext.current
    val notificationsMsg = stringResource(R.string.notifications_needed)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, notificationsMsg, Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) {
        if (!RestTimer.canNotify(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var finishing by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.workout_title),
        onBack = onBack,
        actions = { workout?.let { WorkoutActions(it, onBack, finishing, { finishing = it }) } },
        bottomBar = { RestTimerBar() },
    ) { padding ->
        workout?.let { w ->
            WorkoutContent(
                workout = w,
                items = items,
                openStats = openStats,
                onFinish = { finishing = true },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun WorkoutActions(
    workout: WorkoutEntity,
    onBack: () -> Unit,
    finishing: Boolean,
    setFinishing: (Boolean) -> Unit,
) {
    val repo = LocalRepository.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saveTemplate by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.template_saved)

    val restTimer = LocalRestTimer.current
    Box {
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.save_as_template)) },
                onClick = { menu = false; saveTemplate = true },
            )
            if (workout.finishedAt != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.resume_workout)) },
                    onClick = { menu = false; repo.launch { resumeWorkout(workout) } },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_workout)) },
                onClick = { menu = false; confirmDelete = true },
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_workout) + "?",
            text = stringResource(R.string.delete_workout_confirm),
            onConfirm = {
                repo.launch { deleteWorkout(workout.id) }
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
    if (finishing) {
        WatchDataDialog(
            workout = workout,
            finishing = true,
            onSave = { v ->
                restTimer.stop()
                repo.launch { finishWorkout(workout.withWatch(v)) }
                onBack()
            },
            onSkip = {
                restTimer.stop()
                repo.launch { finishWorkout(workout) }
                onBack()
            },
            onDismiss = { setFinishing(false) },
        )
    }
    if (saveTemplate) {
        TextInputDialog(
            title = stringResource(R.string.save_as_template),
            label = stringResource(R.string.template_name),
            onConfirm = { name ->
                repo.launch {
                    saveWorkoutAsTemplate(workout.id, name)
                    Toast.makeText(context, savedMsg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { saveTemplate = false },
        )
    }
}

@Composable
private fun WorkoutContent(
    workout: WorkoutEntity,
    items: List<WorkoutExerciseFull>,
    openStats: (String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repo = LocalRepository.current
    val latestWorkout by rememberUpdatedState(workout)
    var bodyWeightText by remember(workout.id) { mutableStateOf(workout.bodyWeight?.let(::formatNumber) ?: "") }
    var noteText by remember(workout.id) { mutableStateOf(workout.note) }
    var picking by remember { mutableStateOf(false) }
    var setDialog by remember { mutableStateOf<SetDialogState?>(null) }
    var removing by remember { mutableStateOf<WorkoutExerciseFull?>(null) }
    var editingWatch by remember { mutableStateOf(false) }
    var recoveryFor by remember { mutableStateOf<WorkoutExerciseFull?>(null) }

    val allEquipment by remember { repo.dao.observeEquipment(includeArchived = true) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val equipmentById = allEquipment.associateBy { it.id }

    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val restTimer = LocalRestTimer.current
    val defaultRest by settings.defaultRestSec.collectAsStateWithLifecycle()
    val plateauEnabled by settings.plateauEnabled.collectAsStateWithLifecycle()
    val plateauSessions by settings.plateauSessions.collectAsStateWithLifecycle()
    val newRecordMsg = stringResource(R.string.new_record)

    // Рекорд и застой по каждому упражнению; пересчитываются при любом изменении подходов.
    var progress by remember { mutableStateOf<Map<String, ProgressUi>>(emptyMap()) }
    LaunchedEffect(items, workout.startedAt) {
        progress = items.map { it.exercise }.distinctBy { it.id }.associate { exercise ->
            exercise.id to progressFor(repo.dao.setsForExercise(exercise.id), exercise, workout.id, workout.startedAt)
        }
    }

    // Подходы прошлой тренировки по каждому упражнению — для подсказки и автозаполнения.
    var previous by remember { mutableStateOf<Map<String, List<SetEntity>>>(emptyMap()) }
    val exerciseIds = items.map { it.exercise.id }.distinct()
    LaunchedEffect(exerciseIds, workout.startedAt) {
        previous = exerciseIds.associateWith { repo.previousSets(it, latestWorkout) }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatDateTime(workout.startedAt), style = MaterialTheme.typography.titleMedium)
                workout.finishedAt?.let {
                    Text(
                        stringResource(R.string.workout_finished_at, formatDateTime(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NumberField(
                    value = bodyWeightText,
                    onValueChange = { text ->
                        bodyWeightText = text
                        repo.launch { updateWorkout(latestWorkout.copy(bodyWeight = parseDecimal(text))) }
                    },
                    label = stringResource(R.string.body_weight_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editingWatch = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        watchSummary(workout)?.let { stringResource(R.string.watch_line, it) }
                            ?: stringResource(R.string.watch_line_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.watch_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.workout_empty_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(items, key = { it.item.id }) { entry ->
            ExerciseCard(
                entry = entry,
                bodyWeight = workout.bodyWeight,
                equipment = equipmentById,
                previous = previous[entry.exercise.id].orEmpty(),
                progress = progress[entry.exercise.id],
                plateauSessions = if (plateauEnabled) plateauSessions else null,
                isFirst = entry == items.first(),
                isLast = entry == items.last(),
                onAddSet = { setDialog = SetDialogState(entry, null) },
                onEditSet = { setDialog = SetDialogState(entry, it) },
                onMove = { delta -> repo.launch { moveWorkoutExercise(workout.id, entry.item.id, delta) } },
                onRemove = { removing = entry },
                onStats = { openStats(entry.exercise.id) },
                onRecovery = if (entry.exercise.muscles.isNotBlank()) {
                    { recoveryFor = entry }
                } else {
                    null
                },
            )
        }

        item {
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_exercise))
            }
        }

        item {
            OutlinedTextField(
                value = noteText,
                onValueChange = { text ->
                    noteText = text
                    repo.launch { updateWorkout(latestWorkout.copy(note = text)) }
                },
                label = { Text(stringResource(R.string.workout_note)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (workout.finishedAt == null) {
            item {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.finish_workout_full))
                }
            }
        }
    }

    if (picking) {
        ExercisePickerDialog(
            onDismiss = { picking = false },
            onPicked = { ids -> repo.launch { addExercisesToWorkout(workout.id, ids) } },
        )
    }

    setDialog?.let { state ->
        val sorted = state.item.sets.sortedBy { it.position }
        val index = if (state.set == null) sorted.size else sorted.indexOf(state.set)
        val prev = previous[state.item.exercise.id].orEmpty()
        // Новый подход заполняем по прошлому подходу этой тренировки или по прошлой тренировке.
        val prefill = state.set ?: sorted.lastOrNull() ?: prev.getOrNull(index) ?: prev.lastOrNull()
        SetDialog(
            exercise = state.item.exercise,
            setNumber = index + 1,
            isNew = state.set == null,
            prefill = prefill,
            bodyWeight = workout.bodyWeight,
            equipment = allEquipment.filter {
                !it.archived && it.type == state.item.exercise.equipmentType
            },
            onSave = { values ->
                val base = state.set ?: SetEntity(workoutExerciseId = state.item.item.id, position = -1)
                val exercise = state.item.exercise
                if (state.set == null && workout.finishedAt == null) {
                    restTimer.start(exercise.id, exercise.name, exercise.restSec ?: defaultRest)
                }
                val score = setScore(
                    SetRecord(
                        workoutId = workout.id,
                        date = workout.startedAt,
                        bodyWeight = workout.bodyWeight,
                        useBodyWeight = exercise.useBodyWeight,
                        reps = values.reps,
                        weight = values.weight,
                        durationSec = values.durationSec,
                        distanceKm = values.distanceKm,
                        isWarmup = values.isWarmup,
                        pair = values.pair,
                    ),
                    exercise.config(),
                )
                val p = progress[exercise.id]
                val bestSoFar = listOfNotNull(p?.recordScore, p?.currentBest).maxOrNull()
                if (score != null && p?.record != null && bestSoFar != null && score > bestSoFar + 1e-9) {
                    Toast.makeText(context, newRecordMsg, Toast.LENGTH_LONG).show()
                }
                // После последнего запланированного подхода спрашиваем, насколько было тяжело.
                val planned = state.item.item.plannedSets
                val workingBefore = state.item.sets.count { !it.isWarmup }
                if (state.set == null && !values.isWarmup && planned != null && workingBefore + 1 == planned &&
                    exercise.muscles.isNotBlank() && state.item.item.recoveryHours == null
                ) {
                    recoveryFor = state.item
                }
                repo.launch {
                    saveSet(
                        base.copy(
                            reps = values.reps,
                            weight = values.weight,
                            durationSec = values.durationSec,
                            distanceKm = values.distanceKm,
                            isWarmup = values.isWarmup,
                            equipmentId = values.equipmentId,
                            equipment2Id = values.equipment2Id,
                            pair = values.pair,
                            bandMin = values.bandMin,
                        )
                    )
                }
            },
            onDelete = state.set?.let { s -> { repo.launch { deleteSet(s) }; Unit } },
            onDismiss = { setDialog = null },
        )
    }

    recoveryFor?.let { entry ->
        RecoveryDialog(
            exerciseName = entry.exercise.name,
            current = entry.item.recoveryHours,
            onPick = { hours -> repo.launch { setRecoveryHours(entry.item.id, hours) } },
            onDismiss = { recoveryFor = null },
        )
    }

    if (editingWatch) {
        WatchDataDialog(
            workout = workout,
            finishing = false,
            onSave = { v -> repo.launch { updateWorkout(latestWorkout.withWatch(v)) } },
            onSkip = {},
            onDismiss = { editingWatch = false },
        )
    }

    removing?.let { entry ->
        ConfirmDialog(
            title = stringResource(R.string.remove_from_workout) + "?",
            text = stringResource(R.string.remove_exercise_confirm),
            onConfirm = { repo.launch { removeWorkoutExercise(workout.id, entry.item.id) } },
            onDismiss = { removing = null },
        )
    }
}

@Composable
private fun ExerciseCard(
    entry: WorkoutExerciseFull,
    bodyWeight: Double?,
    equipment: Map<String, EquipmentEntity>,
    previous: List<SetEntity>,
    progress: ProgressUi?,
    plateauSessions: Int?,
    isFirst: Boolean,
    isLast: Boolean,
    onAddSet: () -> Unit,
    onEditSet: (SetEntity) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onStats: () -> Unit,
    onRecovery: (() -> Unit)?,
) {
    val exercise = entry.exercise
    val sets = entry.sets.sortedBy { it.position }
    var menu by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val planned = entry.item.plannedSets
                    val workingSets = sets.count { !it.isWarmup }
                    Text(
                        if (planned != null) stringResource(R.string.sets_progress, workingSets, planned)
                        else stringResource(R.string.sets_count, workingSets),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (planned != null && workingSets >= planned) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (!isFirst) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_up)) },
                                onClick = { menu = false; onMove(-1) },
                            )
                        }
                        if (!isLast) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_down)) },
                                onClick = { menu = false; onMove(1) },
                            )
                        }
                        if (onRecovery != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.recovery_menu)) },
                                onClick = { menu = false; onRecovery() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.statistics)) },
                            onClick = { menu = false; onStats() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_from_workout)) },
                            onClick = { menu = false; onRemove() },
                        )
                    }
                }
            }

            ProgressLines(progress, exercise, equipment, plateauSessions)
            entry.item.recoveryHours?.let { hours ->
                Text(
                    stringResource(R.string.recovery_line, hours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp, bottom = 4.dp),
                )
            }

            if (previous.isNotEmpty()) {
                val text = compactSets(previous.map { formatSet(it, exercise, null, equipment) })
                Text(
                    stringResource(R.string.previous_time, text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp, bottom = 4.dp),
                )
            }

            sets.forEachIndexed { i, set ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEditSet(set) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (set.isWarmup) stringResource(R.string.warmup_short) else "${i + 1}.",
                        modifier = Modifier.width(32.dp),
                        color = if (set.isWarmup) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(formatSet(set, exercise, bodyWeight, equipment), style = MaterialTheme.typography.bodyLarge)
                }
            }

            FilledTonalButton(onClick = onAddSet, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_set))
            }
        }
    }
}

private fun WorkoutEntity.withWatch(v: WatchValues) = copy(
    maxHeartRate = v.maxHeartRate,
    avgHeartRate = v.avgHeartRate,
    watchDurationSec = v.durationSec,
    calories = v.calories,
)

/** Одинаковые подряд подходы сворачиваются: «2 × 12 кг · 11 повт. ×3; 2 × 12 кг · 9 повт.». */
private fun compactSets(sets: List<String>): String {
    val groups = mutableListOf<Pair<String, Int>>()
    sets.forEach { text ->
        val last = groups.lastOrNull()
        if (last != null && last.first == text) groups[groups.lastIndex] = text to last.second + 1 else groups += text to 1
    }
    return groups.joinToString("; ") { (text, count) -> if (count > 1) "$text ×$count" else text }
}

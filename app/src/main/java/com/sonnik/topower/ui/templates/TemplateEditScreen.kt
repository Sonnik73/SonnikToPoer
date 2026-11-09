package com.sonnik.topower.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.TemplateExerciseFull
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.TextInputDialog
import com.sonnik.topower.ui.exercises.ExercisePickerDialog
import kotlinx.coroutines.launch

@Composable
fun TemplateEditScreen(
    templateId: String,
    onBack: () -> Unit,
    openWorkout: (String) -> Unit,
) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val template by remember(templateId) { repo.dao.observeTemplate(templateId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val items by remember(templateId) { repo.dao.observeTemplateExercises(templateId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = template?.name ?: stringResource(R.string.template_title),
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = { menu = false; renaming = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_template)) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Button(
                    onClick = {
                        // Повторное нажатие не создаёт вторую тренировку.
                        if (!starting) {
                            starting = true
                            scope.launch {
                                try {
                                    openWorkout(repo.startWorkout(templateId))
                                } finally {
                                    starting = false
                                }
                            }
                        }
                    },
                    enabled = items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.start_workout_title))
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(stringResource(R.string.template_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(items, key = { it.item.id }) { entry ->
                TemplateExerciseRow(
                    entry = entry,
                    isFirst = entry == items.first(),
                    isLast = entry == items.last(),
                    onSetsChange = { sets ->
                        repo.launch { updateTemplateExercise(entry.item.copy(plannedSets = sets)) }
                    },
                    onMove = { delta -> repo.launch { moveTemplateExercise(templateId, entry.item.id, delta) } },
                    onRemove = { repo.launch { removeTemplateExercise(templateId, entry.item.id) } },
                )
            }
            item {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_exercise))
                }
            }
        }
    }

    if (picking) {
        ExercisePickerDialog(
            onDismiss = { picking = false },
            onPicked = { ids -> repo.launch { addExercisesToTemplate(templateId, ids) } },
        )
    }
    if (renaming) {
        template?.let { t ->
            TextInputDialog(
                title = stringResource(R.string.rename),
                label = stringResource(R.string.template_name),
                initial = t.name,
                onConfirm = { name -> repo.launch { saveTemplate(t.copy(name = name)) } },
                onDismiss = { renaming = false },
            )
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_template) + "?",
            text = stringResource(R.string.delete_template_confirm),
            onConfirm = {
                repo.launch { deleteTemplate(templateId) }
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TemplateExerciseRow(
    entry: TemplateExerciseFull,
    isFirst: Boolean,
    isLast: Boolean,
    onSetsChange: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onSetsChange(entry.item.plannedSets - 1) },
                        enabled = entry.item.plannedSets > 1,
                    ) { Text("−") }
                    Text(stringResource(R.string.planned_sets, entry.item.plannedSets))
                    TextButton(onClick = { onSetsChange(entry.item.plannedSets + 1) }) { Text("+") }
                }
            }
            Column {
                IconButton(onClick = { onMove(-1) }, enabled = !isFirst) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                }
                IconButton(onClick = { onMove(1) }, enabled = !isLast) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

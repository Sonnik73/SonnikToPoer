package com.sonnik.topower.ui.exercises

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.ui.inventory.equipmentTypeLabel
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.EmptyState
import com.sonnik.topower.ui.common.ScreenScaffold

@Composable
fun trackedSummary(e: ExerciseEntity): String = buildList {
    if (e.useBodyWeight) add(stringResource(R.string.use_body_weight))
    if (e.trackReps) add(stringResource(R.string.track_reps))
    if (e.trackWeight) add(stringResource(R.string.track_weight))
    if (e.trackTime) add(stringResource(R.string.track_time))
    if (e.trackDistance) add(stringResource(R.string.track_distance))
    EquipmentType.of(e.equipmentType)?.let { add(equipmentTypeLabel(it)) }
}.joinToString(", ")

@Composable
fun ExercisesScreen(openStats: (String) -> Unit) {
    val repo = LocalRepository.current
    val context = LocalContext.current
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val exercises by remember(showArchived) { repo.dao.observeExercises(showArchived) }
        .collectAsStateWithLifecycle(initialValue = null)

    var editing by remember { mutableStateOf<ExerciseEntity?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ExerciseEntity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val archivedMsg = stringResource(R.string.exercise_archived_msg)

    ScreenScaffold(
        title = stringResource(R.string.nav_exercises),
        showSettings = true,
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.show_archived)) },
                        leadingIcon = {
                            Checkbox(checked = showArchived, onCheckedChange = null)
                        },
                        onClick = { showArchived = !showArchived; menuOpen = false },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_exercise))
            }
        },
    ) { padding ->
        val list = exercises
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(stringResource(R.string.no_exercises), Modifier.padding(padding))
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(list, key = { it.id }) { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        onClick = { editing = exercise },
                        onStats = { openStats(exercise.id) },
                        onDelete = { deleting = exercise },
                        onToggleArchive = {
                            repo.launch { saveExercise(exercise.copy(archived = !exercise.archived)) }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (creating) ExerciseEditDialog(initial = null, onDismiss = { creating = false })
    editing?.let { ExerciseEditDialog(initial = it, onDismiss = { editing = null }) }
    deleting?.let { exercise ->
        ConfirmDialog(
            title = stringResource(R.string.delete_exercise_title, exercise.name),
            text = stringResource(R.string.delete_exercise_confirm),
            onConfirm = {
                repo.launch {
                    val deleted = deleteOrArchiveExercise(exercise)
                    if (!deleted) Toast.makeText(context, archivedMsg, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onStats: () -> Unit,
    onDelete: () -> Unit,
    onToggleArchive: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            val details = listOfNotNull(
                exercise.category.takeIf { it.isNotBlank() },
                trackedSummary(exercise),
                if (exercise.archived) stringResource(R.string.archived) else null,
            ).joinToString(" · ")
            Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.statistics)) },
                        onClick = { menu = false; onStats() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = { menu = false; onClick() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (exercise.archived) R.string.unarchive else R.string.archive)) },
                        onClick = { menu = false; onToggleArchive() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        },
    )
}

package com.sonnik.topower.ui.exercises

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import com.sonnik.topower.ui.muscles.muscleName
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
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.ui.muscles.muscleNames
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.ui.inventory.equipmentTypeLabel
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ConfirmDialog
import com.sonnik.topower.ui.common.EmptyState
import com.sonnik.topower.ui.common.ScreenScaffold

/** Краткое описание упражнения: мышцы и тип инвентаря (или «собственный вес»), иначе — что измеряется. */
@Composable
fun trackedSummary(e: ExerciseEntity): String {
    val muscles = Muscle.parse(e.muscles)
    val tool = EquipmentType.of(e.equipmentType)?.let { equipmentTypeLabel(it) }
        ?: if (e.useBodyWeight) stringResource(R.string.body_weight_tag) else null
    if (muscles.isNotEmpty()) return listOfNotNull(muscleNames(muscles), tool).joinToString(" · ")
    if (tool != null) return tool
    return buildList {
        if (e.trackReps) add(stringResource(R.string.track_reps))
        if (e.trackWeight) add(stringResource(R.string.track_weight))
        if (e.trackTime) add(stringResource(R.string.track_time))
        if (e.trackDistance) add(stringResource(R.string.track_distance))
    }.joinToString(", ")
}

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

    // Фильтры: поиск, часть тела, инвентарь (ключ: имя типа или BODYWEIGHT — так сохраняется при повороте).
    var query by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf<BodyArea?>(null) }
    var toolKey by rememberSaveable { mutableStateOf<String?>(null) }
    val muscleNamesMap = Muscle.entries.associateWith { muscleName(it) }

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
        val all = exercises
        when {
            all == null -> Unit
            all.isEmpty() -> EmptyState(stringResource(R.string.no_exercises), Modifier.padding(padding))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                val tools = ToolFilter.available(all)
                val tool = tools.firstOrNull { it.key == toolKey }
                val list = filterExercises(all, query, area, tool) { muscleNamesMap.getValue(it) }
                ExerciseFilters(
                    query = query,
                    onQuery = { query = it },
                    area = area,
                    onArea = { area = if (area == it) null else it },
                    tools = tools,
                    tool = tool,
                    onTool = { toolKey = if (tool == it) null else it.key },
                    found = list.size,
                    total = all.size,
                    onReset = { query = ""; area = null; toolKey = null },
                )
                if (list.isEmpty()) {
                    Text(
                        stringResource(R.string.exercise_filter_nothing),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyColumn(
                    Modifier.weight(1f),
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

private val ToolFilter.key: String get() = type?.name ?: "BODYWEIGHT"

@Composable
private fun areaLabel(area: BodyArea): String = stringResource(
    when (area) {
        BodyArea.CHEST -> R.string.area_chest
        BodyArea.BACK -> R.string.area_back
        BodyArea.SHOULDERS -> R.string.area_shoulders
        BodyArea.ARMS -> R.string.area_arms
        BodyArea.CORE -> R.string.area_core
        BodyArea.LEGS -> R.string.area_legs
    }
)

/** Поиск, части тела и инвентарь над списком; повторное нажатие на чип снимает фильтр. */
@Composable
private fun ExerciseFilters(
    query: String,
    onQuery: (String) -> Unit,
    area: BodyArea?,
    onArea: (BodyArea) -> Unit,
    tools: List<ToolFilter>,
    tool: ToolFilter?,
    onTool: (ToolFilter) -> Unit,
    found: Int,
    total: Int,
    onReset: () -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text(stringResource(R.string.exercise_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.exercise_filter_reset)) } }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            items(BodyArea.entries) { a ->
                FilterChip(selected = area == a, onClick = { onArea(a) }, label = { Text(areaLabel(a)) })
            }
        }
        if (tools.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tools) { t ->
                    FilterChip(
                        selected = tool == t,
                        onClick = { onTool(t) },
                        label = {
                            Text(t.type?.let { equipmentTypeLabel(it, plural = true) } ?: stringResource(R.string.body_weight_tag))
                        },
                    )
                }
            }
        }
        if (found != total) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.exercise_filter_found, found, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReset) { Text(stringResource(R.string.exercise_filter_reset)) }
            }
        }
        HorizontalDivider()
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
                exercise.category.takeIf { it.isNotBlank() && exercise.muscles.isBlank() },
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

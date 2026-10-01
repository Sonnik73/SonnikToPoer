package com.sonnik.topower.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.TemplateSummary
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.SectionTitle
import com.sonnik.topower.ui.common.TextInputDialog
import com.sonnik.topower.ui.common.formatDateTime
import kotlinx.coroutines.launch

@Composable
fun WorkoutHomeScreen(
    openWorkout: (String) -> Unit,
    openTemplate: (String) -> Unit,
) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    val active by remember { repo.dao.observeActiveWorkouts() }.collectAsStateWithLifecycle(emptyList())
    val templates by remember { repo.dao.observeTemplateSummaries() }.collectAsStateWithLifecycle(emptyList())
    var newTemplateDialog by rememberSaveable { mutableStateOf(false) }

    fun start(templateId: String?) {
        scope.launch { openWorkout(repo.startWorkout(templateId)) }
    }

    ScreenScaffold(title = stringResource(R.string.nav_workout), showSettings = true) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (active.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.active_workouts)) }
                items(active, key = { "active-" + it.id }) { workout ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openWorkout(workout.id) },
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.started_at, formatDateTime(workout.startedAt)),
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = { openWorkout(workout.id) }) {
                                Text(stringResource(R.string.continue_workout))
                            }
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.start_workout_title)) }
            item {
                Button(onClick = { start(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.start_empty_workout))
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(stringResource(R.string.templates_title), Modifier.weight(1f))
                    TextButton(onClick = { newTemplateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.new_template))
                    }
                }
            }
            if (templates.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_templates),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(templates, key = { it.template.id }) { summary ->
                TemplateCard(
                    summary = summary,
                    onStart = { start(summary.template.id) },
                    onEdit = { openTemplate(summary.template.id) },
                )
            }
        }
    }

    if (newTemplateDialog) {
        TextInputDialog(
            title = stringResource(R.string.new_template),
            label = stringResource(R.string.template_name),
            onConfirm = { name -> scope.launch { openTemplate(repo.createTemplate(name)) } },
            onDismiss = { newTemplateDialog = false },
        )
    }
}

@Composable
private fun TemplateCard(summary: TemplateSummary, onStart: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(summary.template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    summary.exerciseNames ?: stringResource(R.string.exercise_count, 0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(onClick = onStart) { Text(stringResource(R.string.start)) }
        }
    }
}

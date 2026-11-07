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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
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
import com.sonnik.topower.ui.measurements.MeasurementsCard
import com.sonnik.topower.ui.muscles.MuscleStatusCard
import com.sonnik.topower.ui.muscles.muscleName
import com.sonnik.topower.ui.muscles.rememberNow
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleStatus
import com.sonnik.topower.domain.blockingMuscles
import com.sonnik.topower.domain.templateReadiness
import com.sonnik.topower.ui.muscles.recoveryColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import com.sonnik.topower.domain.hoursLeft
import com.sonnik.topower.reminders.muscleStatuses
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
    val context = LocalContext.current
    val names = Muscle.entries.associateWith { muscleName(it) }
    val generatedName = stringResource(R.string.generated_template_name)
    val nothingMsg = stringResource(R.string.generate_template_nothing)

    // Готовность мышц шаблонов: готовые к тренировке шаблоны — первыми.
    val muscleRows by remember { repo.dao.observeMuscleWork() }.collectAsStateWithLifecycle(emptyList())
    val templateMuscleRows by remember { repo.dao.observeTemplateMuscles() }.collectAsStateWithLifecycle(emptyList())
    val statuses = remember(muscleRows) { muscleStatuses(muscleRows) }
    val templateMuscles = remember(templateMuscleRows) {
        templateMuscleRows.groupBy { it.templateId }.mapValues { (_, rows) -> rows.flatMap { Muscle.parse(it.muscles) }.toSet() }
    }
    val now = rememberNow()
    val blocking = templates.associate { t ->
        t.template.id to templateMuscles[t.template.id]?.let { blockingMuscles(it, statuses, now) }
    }
    val readiness = templates.associate { t ->
        t.template.id to templateMuscles[t.template.id]?.let { templateReadiness(it, statuses, now) }
    }
    // Самые готовые к тренировке — сверху; шаблоны без мышц — в конце.
    val sortedTemplates = templates.sortedByDescending { readiness[it.template.id] ?: -1f }

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
            item { MuscleStatusCard() }
            item { MeasurementsCard() }
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
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val id = repo.generateTemplate(System.currentTimeMillis()) { muscles ->
                                generatedName.format(muscles.take(3).joinToString(", ") { names.getValue(it) })
                            }
                            if (id != null) openTemplate(id) else Toast.makeText(context, nothingMsg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.generate_template))
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
            items(sortedTemplates, key = { it.template.id }) { summary ->
                TemplateCard(
                    summary = summary,
                    blocking = blocking[summary.template.id],
                    readiness = readiness[summary.template.id],
                    now = now,
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
private fun TemplateCard(
    summary: TemplateSummary,
    blocking: List<MuscleStatus>?,
    readiness: Float?,
    now: Long,
    onStart: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Полоса готовности: от красной (мышцы не восстановились) до зелёной.
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(readiness?.let { recoveryColor(it) } ?: Color.Transparent)
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(summary.template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        summary.exerciseNames ?: stringResource(R.string.exercise_count, 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    readiness?.let {
                        Text(
                            stringResource(R.string.template_readiness, (it * 100).roundToInt()),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    when {
                        blocking == null -> Unit
                        blocking.isEmpty() -> Text(
                            stringResource(R.string.template_ready),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        else -> Text(
                            stringResource(
                                R.string.template_recovering,
                                blocking.map { muscleName(it.muscle) + " " + stringResource(R.string.hours_short, it.hoursLeft(now)) }
                                    .joinToString(", "),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FilledTonalButton(onClick = onStart) { Text(stringResource(R.string.start)) }
            }
        }
    }
}

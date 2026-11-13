package com.sonnik.topower.ui.workout

import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import com.sonnik.topower.domain.weekStart
import com.sonnik.topower.domain.weekStreak
import com.sonnik.topower.ui.theme.isAppInDarkTheme
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.Button
import com.sonnik.topower.ui.common.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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

    // Пока тренировка создаётся, повторное нажатие не создаёт вторую.
    var starting by remember { mutableStateOf(false) }
    fun start(templateId: String?) {
        if (starting) return
        starting = true
        scope.launch {
            try {
                openWorkout(repo.startWorkout(templateId))
            } finally {
                starting = false
            }
        }
    }

    ScreenScaffold(title = stringResource(R.string.nav_workout), showSettings = true) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { WeekHero(onStart = { start(null) }) }
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
                        else -> {
                            // Не больше трёх мышц в строке: длинный список ломает высоту карточки,
                            // остальные — «ещё N» (полный список открывается на схеме мышц).
                            val shown = blocking.take(3)
                            val hidden = blocking.size - shown.size
                            Text(
                                stringResource(
                                    R.string.template_recovering,
                                    shown.map { muscleName(it.muscle) + " " + stringResource(R.string.hours_short, it.hoursLeft(now)) }
                                        .joinToString(", ") + if (hidden > 0) " " + stringResource(R.string.recovering_more, hidden) else "",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                FilledTonalButton(onClick = onStart) { Text(stringResource(R.string.start)) }
            }
        }
    }
}

/**
 * Шапка главного экрана: тренировки на этой неделе, серия недель подряд, давность последней
 * тренировки и кнопка «Начать тренировку» (пустая тренировка; по шаблону — из списка ниже).
 */
@Composable
private fun WeekHero(onStart: () -> Unit) {
    val repo = LocalRepository.current
    val dates by remember { repo.dao.observeWorkoutDates() }.collectAsStateWithLifecycle(emptyList())
    val now = rememberNow()
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = dates.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    val thisWeek = days.count { weekStart(it) == weekStart(today) }
    val streak = weekStreak(dates, today, zone)
    val lastDaysAgo = days.maxOrNull()?.let { ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) }

    // В светлой теме — основной цвет палитры, в тёмной — его приглушённый вариант.
    val dark = isAppInDarkTheme
    val base = if (dark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    val content = if (dark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.3f))))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.home_week_title), style = MaterialTheme.typography.labelLarge, color = content.copy(alpha = 0.8f))
        Text(
            pluralStringResource(R.plurals.home_week_workouts, thisWeek, thisWeek),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = content,
        )
        val details = listOfNotNull(
            if (streak >= 2) stringResource(R.string.home_streak, streak) else null,
            when (lastDaysAgo) {
                null -> stringResource(R.string.home_no_workouts)
                0 -> stringResource(R.string.home_last_today)
                1 -> stringResource(R.string.home_last_yesterday)
                else -> stringResource(R.string.home_last_days, lastDaysAgo)
            },
        ).joinToString(" · ")
        Text(details, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.85f))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = content, contentColor = base),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_workout_title), fontWeight = FontWeight.SemiBold)
        }
    }
}

package com.sonnik.topower.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.domain.computeOverallStats
import com.sonnik.topower.domain.formatDuration
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.BarChart
import com.sonnik.topower.ui.common.ChartPoint
import com.sonnik.topower.ui.common.EmptyState
import com.sonnik.topower.ui.common.LineChart
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.ui.common.SectionTitle
import com.sonnik.topower.ui.common.StatRow
import com.sonnik.topower.ui.common.kg
import com.sonnik.topower.ui.workout.formatHoursMinutes
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(openExercise: (String) -> Unit) {
    val repo = LocalRepository.current
    val dates by remember { repo.dao.observeWorkoutDates() }.collectAsStateWithLifecycle(initialValue = null)
    val sets by remember { repo.dao.observeAllSets() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val bodyWeights by remember { repo.dao.observeBodyWeights() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val workouts by remember { repo.dao.observeWorkouts() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val exercises by remember { repo.dao.observeExercises(includeArchived = true) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    ScreenScaffold(title = stringResource(R.string.nav_stats), showSettings = true) { padding ->
        val workoutDates = dates ?: return@ScreenScaffold
        if (workoutDates.isEmpty()) {
            EmptyState(stringResource(R.string.no_stats), Modifier.padding(padding))
            return@ScreenScaffold
        }
        val zone = ZoneId.systemDefault()
        val overall = remember(workoutDates, sets) {
            computeOverallStats(workoutDates, sets.map { it.toRecord() }, LocalDate.now(zone), zone)
        }
        val workoutsPerExercise = remember(sets) {
            sets.filter { !it.set.isWarmup }.groupBy { it.exerciseId }.mapValues { (_, v) -> v.map { it.workoutId }.distinct().size }
        }
        val exercisesWithData = exercises.filter { it.id in workoutsPerExercise }
        val weekFormat = remember { DateTimeFormatter.ofPattern("dd.MM") }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionTitle(stringResource(R.string.stats_overview)) }
            item {
                StatRow(
                    stringResource(R.string.stat_total_workouts) to overall.totalWorkouts.toString(),
                    stringResource(R.string.stat_this_week) to overall.workoutsThisWeek.toString(),
                    stringResource(R.string.stat_this_month) to overall.workoutsThisMonth.toString(),
                )
            }
            item {
                StatRow(
                    stringResource(R.string.stat_total_sets) to overall.totalSets.toString(),
                    stringResource(R.string.stat_tonnage) to formatNumber(overall.totalTonnage, 0),
                )
            }
            // Данные со смарт-часов: сумма ккал, средняя длительность и средний максимальный пульс.
            val calories = workouts.mapNotNull { it.calories }
            val durations = workouts.mapNotNull { it.watchDurationSec }
            val maxHrs = workouts.mapNotNull { it.maxHeartRate }
            if (calories.isNotEmpty() || durations.isNotEmpty() || maxHrs.isNotEmpty()) {
                item {
                    StatRow(
                        stringResource(R.string.stat_calories) to (if (calories.isEmpty()) "—" else calories.sum().toString()),
                        stringResource(R.string.stat_avg_duration) to
                            (if (durations.isEmpty()) "—" else formatHoursMinutes(durations.average().toInt())),
                        stringResource(R.string.stat_avg_max_hr) to
                            (if (maxHrs.isEmpty()) "—" else maxHrs.average().toInt().toString()),
                    )
                }
            }
            if (overall.totalDistanceKm > 0 || overall.totalDurationSec > 0) {
                item {
                    StatRow(
                        stringResource(R.string.stat_distance) to formatNumber(overall.totalDistanceKm),
                        stringResource(R.string.stat_duration) to formatDuration(overall.totalDurationSec.toInt()),
                    )
                }
            }
            item {
                SectionCard {
                    Text(stringResource(R.string.workouts_per_week), style = MaterialTheme.typography.titleSmall)
                    BarChart(
                        values = overall.weekly.map { it.count },
                        firstLabel = overall.weekly.first().weekStart.format(weekFormat),
                        lastLabel = overall.weekly.last().weekStart.format(weekFormat),
                    )
                }
            }
            if (bodyWeights.isNotEmpty()) {
                item {
                    SectionCard {
                        Text(stringResource(R.string.body_weight_chart), style = MaterialTheme.typography.titleSmall)
                        LineChart(
                            points = bodyWeights.map { ChartPoint(it.startedAt, it.bodyWeight) },
                            formatY = { kg(it) },
                        )
                    }
                }
            }
            if (exercisesWithData.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.by_exercise)) }
                items(exercisesWithData, key = { it.id }) { exercise ->
                    ListItem(
                        modifier = Modifier.clickable { openExercise(exercise.id) },
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.stat_workouts) + ": " + workoutsPerExercise[exercise.id],
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
    }
}

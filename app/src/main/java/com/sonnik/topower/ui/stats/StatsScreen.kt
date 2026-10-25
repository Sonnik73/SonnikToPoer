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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.domain.BucketUnit
import com.sonnik.topower.domain.StatsPeriod
import com.sonnik.topower.domain.activityBuckets
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
import com.sonnik.topower.ui.share.ShareResultsButton
import com.sonnik.topower.ui.workout.formatHoursMinutes
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(openExercise: (String) -> Unit) {
    val repo = LocalRepository.current
    val dates by remember { repo.dao.observeWorkoutDates() }.collectAsStateWithLifecycle(initialValue = null)
    val sets by remember { repo.dao.observeAllSets() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val bodyWeights by remember { repo.dao.observeBodyWeights() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val workouts by remember { repo.dao.observeWorkouts() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val exercises by remember { repo.dao.observeExercises(includeArchived = true) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var period by rememberSaveable { mutableStateOf(StatsPeriod.ALL) }

    ScreenScaffold(
        title = stringResource(R.string.nav_stats),
        showSettings = true,
        actions = { ShareResultsButton(dates.orEmpty(), sets, workouts, exercises) },
    ) { padding ->
        val workoutDates = dates ?: return@ScreenScaffold
        if (workoutDates.isEmpty()) {
            EmptyState(stringResource(R.string.no_stats), Modifier.padding(padding))
            return@ScreenScaffold
        }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        // Всё, кроме плиток «На этой неделе» и «В этом месяце», считаем только за выбранный период.
        val start = period.start(today, zone) ?: Long.MIN_VALUE
        val periodDates = remember(workoutDates, start) { workoutDates.filter { it >= start } }
        val periodSets = remember(sets, start) { sets.filter { it.workoutDate >= start } }
        val periodWorkouts = workouts.filter { it.startedAt >= start }
        val periodWeights = bodyWeights.filter { it.startedAt >= start }
        val overall = remember(workoutDates, sets) {
            computeOverallStats(workoutDates, sets.map { it.toRecord() }, today, zone)
        }
        val inPeriod = remember(periodDates, periodSets) {
            computeOverallStats(periodDates, periodSets.map { it.toRecord() }, today, zone)
        }
        val (bucketUnit, buckets) = remember(workoutDates, period) { activityBuckets(workoutDates, period, today, zone) }
        val workoutsPerExercise = remember(periodSets) {
            periodSets.filter { !it.set.isWarmup }.groupBy { it.exerciseId }.mapValues { (_, v) -> v.map { it.workoutId }.distinct().size }
        }
        val exercisesWithData = exercises.filter { it.id in workoutsPerExercise }
        val dayFormat = remember { DateTimeFormatter.ofPattern("dd.MM") }
        val monthFormat = remember { DateTimeFormatter.ofPattern("LLL yyyy", Locale.getDefault()) }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SectionTitle(stringResource(R.string.stats_overview)) }
            item { PeriodChips(period, onSelect = { period = it }) }
            item {
                StatRow(
                    stringResource(R.string.stat_total_workouts) to inPeriod.totalWorkouts.toString(),
                    stringResource(R.string.stat_this_week) to overall.workoutsThisWeek.toString(),
                    stringResource(R.string.stat_this_month) to overall.workoutsThisMonth.toString(),
                )
            }
            item {
                StatRow(
                    stringResource(R.string.stat_total_sets) to inPeriod.totalSets.toString(),
                    stringResource(R.string.stat_tonnage) to formatNumber(inPeriod.totalTonnage, 0),
                )
            }
            // Данные со смарт-часов: сумма ккал, средняя длительность и средний максимальный пульс.
            val calories = periodWorkouts.mapNotNull { it.calories }
            val durations = periodWorkouts.mapNotNull { it.watchDurationSec }
            val maxHrs = periodWorkouts.mapNotNull { it.maxHeartRate }
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
            val extraTiles = buildList {
                if (inPeriod.totalDistanceKm > 0) add(R.string.stat_distance to formatNumber(inPeriod.totalDistanceKm))
                if (inPeriod.totalDurationSec > 0) add(R.string.stat_duration to formatDuration(inPeriod.totalDurationSec.toInt()))
            }
            if (extraTiles.isNotEmpty()) {
                item {
                    StatRow(*extraTiles.map { (label, value) -> stringResource(label) to value }.toTypedArray())
                }
            }
            item {
                SectionCard {
                    val title = when (bucketUnit) {
                        BucketUnit.DAY -> R.string.workouts_per_day
                        BucketUnit.WEEK -> R.string.workouts_per_week
                        BucketUnit.MONTH -> R.string.workouts_per_month
                    }
                    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                    BarChart(
                        values = buckets.map { it.count },
                        barLabels = buckets.map { it.start.format(if (bucketUnit == BucketUnit.MONTH) monthFormat else dayFormat) },
                    )
                }
            }
            if (periodWeights.isNotEmpty()) {
                item {
                    SectionCard {
                        Text(stringResource(R.string.body_weight_chart), style = MaterialTheme.typography.titleSmall)
                        LineChart(
                            points = periodWeights.map { ChartPoint(it.startedAt, it.bodyWeight) },
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

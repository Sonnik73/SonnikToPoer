package com.sonnik.topower.ui.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import com.sonnik.topower.ui.common.AppIcons
import com.sonnik.topower.ui.common.Stat
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
import com.sonnik.topower.data.ExerciseEntity
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
        // Чаще выполняемые — выше; при равенстве — по алфавиту.
        val exercisesWithData = exercises.filter { it.id in workoutsPerExercise }
            .sortedWith(compareByDescending<ExerciseEntity> { workoutsPerExercise[it.id] }.thenBy { it.name.lowercase() })
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
                    Stat(stringResource(R.string.stat_total_workouts), inPeriod.totalWorkouts.toString(), AppIcons.Dumbbell),
                    Stat(stringResource(R.string.stat_this_week), overall.workoutsThisWeek.toString(), Icons.Filled.DateRange),
                    Stat(stringResource(R.string.stat_this_month), overall.workoutsThisMonth.toString(), Icons.Filled.DateRange),
                )
            }
            item {
                StatRow(
                    Stat(stringResource(R.string.stat_total_sets), inPeriod.totalSets.toString(), Icons.Filled.Check),
                    Stat(stringResource(R.string.stat_tonnage), formatNumber(inPeriod.totalTonnage, 0), AppIcons.Weight),
                )
            }
            // Данные со смарт-часов: сумма ккал, средняя длительность и средний максимальный пульс.
            val calories = periodWorkouts.mapNotNull { it.calories }
            val durations = periodWorkouts.mapNotNull { it.watchDurationSec }
            val maxHrs = periodWorkouts.mapNotNull { it.maxHeartRate }
            if (calories.isNotEmpty() || durations.isNotEmpty() || maxHrs.isNotEmpty()) {
                item {
                    StatRow(
                        Stat(stringResource(R.string.stat_calories), if (calories.isEmpty()) "—" else calories.sum().toString(), AppIcons.Flame),
                        Stat(
                            stringResource(R.string.stat_avg_duration),
                            if (durations.isEmpty()) "—" else formatHoursMinutes(durations.average().toInt()),
                            AppIcons.Clock,
                        ),
                        Stat(
                            stringResource(R.string.stat_avg_max_hr),
                            if (maxHrs.isEmpty()) "—" else maxHrs.average().toInt().toString(),
                            Icons.Filled.Favorite,
                        ),
                    )
                }
            }
            val extraTiles = buildList {
                if (inPeriod.totalDistanceKm > 0) add(Triple(R.string.stat_distance, formatNumber(inPeriod.totalDistanceKm), Icons.Filled.Place))
                if (inPeriod.totalDurationSec > 0) add(Triple(R.string.stat_duration, formatDuration(inPeriod.totalDurationSec.toInt()), AppIcons.Clock))
            }
            if (extraTiles.isNotEmpty()) {
                item {
                    StatRow(*extraTiles.map { (label, value, icon) -> Stat(stringResource(label), value, icon) }.toTypedArray())
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

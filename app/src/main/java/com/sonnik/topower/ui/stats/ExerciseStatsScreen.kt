package com.sonnik.topower.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.domain.Metric
import com.sonnik.topower.domain.StatsPeriod
import com.sonnik.topower.domain.computeExerciseStats
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.ChartPoint
import com.sonnik.topower.ui.common.EmptyState
import com.sonnik.topower.ui.common.LineChart
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.SectionCard
import com.sonnik.topower.ui.common.StatRow
import com.sonnik.topower.ui.common.formatDate
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ExerciseStatsScreen(exerciseId: String, onBack: () -> Unit) {
    val repo = LocalRepository.current
    val exercise by remember(exerciseId) { repo.dao.observeExercise(exerciseId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val sets by remember(exerciseId) { repo.dao.observeSetsForExercise(exerciseId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedMetric by rememberSaveable { mutableStateOf<Metric?>(null) }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.ALL) }

    ScreenScaffold(title = exercise?.name ?: stringResource(R.string.statistics), onBack = onBack) { padding ->
        val ex = exercise ?: return@ScreenScaffold
        if (sets.isEmpty()) {
            EmptyState(stringResource(R.string.no_stats), Modifier.padding(padding))
            return@ScreenScaffold
        }
        val zone = ZoneId.systemDefault()
        val start = period.start(LocalDate.now(zone), zone) ?: Long.MIN_VALUE
        val stats = remember(sets, ex, start) {
            computeExerciseStats(sets.filter { it.workoutDate >= start }.map { it.toRecord() }, ex.config())
        }
        if (stats == null) {
            Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PeriodChips(period, onSelect = { period = it })
                Text(stringResource(R.string.no_data_in_period), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@ScreenScaffold
        }
        val summary = stats.metrics.firstOrNull { it.metric == selectedMetric } ?: stats.metrics.firstOrNull()

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PeriodChips(period, onSelect = { period = it }) }
            item {
                Text(
                    stringResource(R.string.period, formatDate(stats.firstDate), formatDate(stats.lastDate)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                StatRow(
                    stringResource(R.string.stat_workouts) to stats.workoutCount.toString(),
                    stringResource(R.string.stat_sets) to stats.setCount.toString(),
                )
            }

            if (summary != null) {
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        stats.metrics.forEach { m ->
                            FilterChip(
                                selected = m.metric == summary.metric,
                                onClick = { selectedMetric = m.metric },
                                label = { Text(metricLabel(m.metric)) },
                            )
                        }
                    }
                }
                item {
                    StatRow(
                        stringResource(R.string.stat_start) to formatMetric(summary.metric, summary.first.value),
                        stringResource(R.string.stat_max) to formatMetric(summary.metric, summary.best.value),
                        stringResource(R.string.stat_last) to formatMetric(summary.metric, summary.last.value),
                    )
                }
                item {
                    val gain = formatMetric(summary.metric, summary.gain)
                    val percent = summary.gainPercent?.let { " (+${formatNumber(it, 1)}%)" }.orEmpty()
                    SectionCard {
                        Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.stat_gain) + ": +" + gain + percent,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.stat_max) + ": " + formatDate(summary.best.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LineChart(
                            points = summary.series.map { ChartPoint(it.date, it.value) },
                            formatY = { formatMetric(summary.metric, it) },
                        )
                    }
                }
            }

            stats.bestSet?.let { best ->
                item {
                    SectionCard {
                        Text(stringResource(R.string.best_set), style = MaterialTheme.typography.titleSmall)
                        Text(formatRecord(best, ex), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            formatDate(best.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionCard {
                    Text(stringResource(R.string.first_workout), style = MaterialTheme.typography.titleSmall)
                    Text(
                        formatDate(stats.firstDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    stats.firstSessionSets.forEachIndexed { i, record ->
                        Text("${i + 1}. " + formatRecord(record, ex))
                    }
                }
            }

            if (stats.metrics.any { it.metric == Metric.E1RM }) {
                item {
                    Text(
                        stringResource(R.string.e1rm_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

package com.sonnik.topower.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.WorkoutSummary
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.EmptyState
import com.sonnik.topower.ui.common.ScreenScaffold
import com.sonnik.topower.ui.common.formatDateTime
import com.sonnik.topower.ui.workout.watchSummary

@Composable
fun HistoryScreen(openWorkout: (String) -> Unit) {
    val repo = LocalRepository.current
    val workouts by remember { repo.dao.observeWorkoutSummaries() }.collectAsStateWithLifecycle(initialValue = null)

    ScreenScaffold(title = stringResource(R.string.nav_history), showSettings = true) { padding ->
        val list = workouts
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(stringResource(R.string.history_empty), Modifier.padding(padding))
            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.workout.id }) { summary ->
                    WorkoutCard(summary, onClick = { openWorkout(summary.workout.id) })
                }
            }
        }
    }
}

@Composable
private fun WorkoutCard(summary: WorkoutSummary, onClick: () -> Unit) {
    val w = summary.workout
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(
                    formatDateTime(w.startedAt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (w.finishedAt == null) {
                    Text(stringResource(R.string.in_progress), color = MaterialTheme.colorScheme.primary)
                }
            }
            val details = listOfNotNull(
                w.bodyWeight?.let { stringResource(R.string.body_weight_short, formatNumber(it)) },
                stringResource(R.string.exercises_and_sets, summary.exerciseCount, summary.setCount),
            ).joinToString(" · ")
            Text(details, style = MaterialTheme.typography.bodyMedium)
            watchSummary(w)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            summary.exerciseNames?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (w.note.isNotBlank()) {
                Text(w.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

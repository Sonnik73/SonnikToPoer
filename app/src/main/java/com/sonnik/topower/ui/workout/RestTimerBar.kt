package com.sonnik.topower.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.data.AppSettings
import com.sonnik.topower.domain.formatDuration
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.LocalRestTimer
import kotlinx.coroutines.delay

/** Время, после которого панель «Отдых окончен» скрывается сама. */
private const val DONE_VISIBLE_MS = 60_000L

/**
 * Панель отсчёта отдыха внизу экрана тренировки. Изменение длительности кнопками ±15 с
 * запоминается как время отдыха этого упражнения.
 */
@Composable
fun RestTimerBar() {
    val timer = LocalRestTimer.current
    val repo = LocalRepository.current
    val state by timer.state.collectAsStateWithLifecycle()
    val rest = state ?: return

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(rest) {
        while (true) {
            now = System.currentTimeMillis()
            if (now > rest.endAt + DONE_VISIBLE_MS) {
                timer.stop()
                break
            }
            delay(250)
        }
    }
    val remainingMs = (rest.endAt - now).coerceAtLeast(0)
    val done = remainingMs == 0L

    fun adjust(delta: Int) {
        val total = timer.adjust(delta) ?: return
        repo.launch { setExerciseRest(rest.exerciseId, total) }
    }

    Surface(
        color = if (done) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (done) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.rest_over),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { timer.stop() }) { Text(stringResource(R.string.rest_close)) }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.rest_title) + " · " + rest.exerciseName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                        Text(
                            formatDuration(((remainingMs + 999) / 1000).toInt()),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(onClick = { timer.stop() }) { Text(stringResource(R.string.rest_skip)) }
                }
                LinearProgressIndicator(
                    progress = { 1f - remainingMs / (rest.totalSec * 1000f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { adjust(-AppSettings.REST_STEP_SEC) },
                        enabled = rest.totalSec > AppSettings.MIN_REST_SEC,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.rest_minus)) }
                    FilledTonalButton(
                        onClick = { adjust(AppSettings.REST_STEP_SEC) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.rest_plus)) }
                }
            }
        }
    }
}

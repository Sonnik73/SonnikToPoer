package com.sonnik.topower.ui.workout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/** Высота панели: одна строка, чтобы не закрывать список подходов. */
private val BAR_HEIGHT = 56.dp

/** Время, после которого панель «Отдых окончен» скрывается сама. */
private const val DONE_VISIBLE_MS = 60_000L

/**
 * Компактная панель отсчёта отдыха внизу экрана тренировки: кнопки ±15 с, время и «Пропустить» в одну строку. Изменение длительности кнопками ±15 с
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

    val shape = RoundedCornerShape(16.dp)
    Surface(
        color = if (done) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = shape,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (done) {
            Row(
                Modifier.padding(start = 16.dp, end = 4.dp).height(BAR_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.rest_over),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { timer.stop() }) { Text(stringResource(R.string.rest_close)) }
            }
            return@Surface
        }
        Column {
            Row(
                Modifier.padding(horizontal = 6.dp).height(BAR_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton(stringResource(R.string.rest_minus), enabled = rest.totalSec > AppSettings.MIN_REST_SEC) {
                    adjust(-AppSettings.REST_STEP_SEC)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDuration(((remainingMs + 999) / 1000).toInt()),
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        stringResource(R.string.rest_title) + " · " + rest.exerciseName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StepButton(stringResource(R.string.rest_plus)) { adjust(AppSettings.REST_STEP_SEC) }
                IconButton(onClick = { timer.stop() }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.rest_skip))
                }
            }
            LinearProgressIndicator(
                progress = { 1f - remainingMs / (rest.totalSec * 1000f) },
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier.fillMaxWidth().height(3.dp),
            )
        }
    }
}

/** Небольшая кнопка «−15 с» / «+15 с». */
@Composable
private fun StepButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp),
        modifier = Modifier.height(36.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium) }
}

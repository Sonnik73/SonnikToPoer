package com.sonnik.topower.ui.share

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonnik.topower.R
import com.sonnik.topower.ui.muscles.LocalFemaleBody
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetWithContext
import com.sonnik.topower.data.WorkoutEntity
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleSets
import com.sonnik.topower.domain.VolumeLevel
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.domain.periodSummary
import com.sonnik.topower.domain.recentRecords
import com.sonnik.topower.domain.weekStreak
import com.sonnik.topower.domain.weeklySets
import com.sonnik.topower.ui.LocalRepository
import com.sonnik.topower.ui.common.formatSet
import com.sonnik.topower.ui.muscles.muscleName
import com.sonnik.topower.ui.stats.config
import com.sonnik.topower.ui.stats.toRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAY_MS = 86_400_000L

/**
 * Кнопка «Поделиться результатами»: собирает карточку с итогами недели и открывает системное
 * меню отправки (мессенджеры, сохранение). Данные никуда не уходят без выбора пользователя.
 */
@Composable
fun ShareResultsButton(
    workoutDates: List<Long>,
    sets: List<SetWithContext>,
    workouts: List<WorkoutEntity>,
    exercises: List<ExerciseEntity>,
) {
    val repo = LocalRepository.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val equipment by remember { repo.dao.observeEquipment(includeArchived = true) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val muscleRows by remember { repo.dao.observeMuscleWork() }.collectAsStateWithLifecycle(initialValue = emptyList())

    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val since = now - 7 * DAY_MS
    val exById = exercises.associateBy { it.id }
    val eqById = equipment.associateBy { it.id }

    val week = periodSummary(workoutDates, sets.map { it.toRecord() }, now, 7)
    val month = periodSummary(workoutDates, emptyList(), now, 30)
    val streak = weekStreak(workoutDates, LocalDate.now(zone), zone)
    val calories = workouts.filter { it.startedAt >= since }.mapNotNull { it.calories }.sum()
    val volume = weeklySets(muscleRows.map { MuscleSets(it.time, Muscle.parse(it.muscles), it.sets) }, now)
    // Пересчёт по всем подходам — только когда меняются данные, а не при каждой перерисовке.
    val recent = remember(sets, exercises) {
        recentRecords(sets, { it.exerciseId }, { it.toRecord() }, { exById[it]?.config() }, since).take(3)
    }
    val records = recent
        .mapNotNull { r ->
            val ex = exById[r.exerciseId] ?: return@mapNotNull null
            ShareRecordLine(
                exercise = ex.name,
                set = formatSet(r.record.set, ex, r.record.bodyWeight, eqById),
                gain = "+" + formatNumber(r.gainPercent, 0) + "%",
            )
        }

    val dateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().format(dateFormat)
    val data = ShareCardData(
        title = stringResource(R.string.share_title),
        period = stringResource(R.string.share_period, day(since + DAY_MS), day(now)),
        tiles = listOf(
            ShareTile(week.workouts.toString(), stringResource(R.string.share_workouts_week)),
            ShareTile(month.workouts.toString(), stringResource(R.string.share_workouts_month)),
            ShareTile(workoutDates.size.toString(), stringResource(R.string.share_workouts_total)),
            ShareTile(streak.toString(), stringResource(R.string.share_streak)),
            ShareTile(formatNumber(week.tonnage, 0), stringResource(R.string.share_tonnage)),
            if (calories > 0) ShareTile(calories.toString(), stringResource(R.string.share_calories))
            else ShareTile(week.sets.toString(), stringResource(R.string.share_sets)),
        ),
        musclesTitle = stringResource(R.string.share_muscles),
        femaleBody = LocalFemaleBody.current,
        volume = volume,
        muscleNames = Muscle.entries.associateWith { muscleName(it) },
        setsFormat = stringResource(R.string.volume_sets),
        volumeLegend = listOf(
            VolumeLevel.LOW to "1–5",
            VolumeLevel.MODERATE to "6–9",
            VolumeLevel.OPTIMAL to "10–20",
            VolumeLevel.HIGH to "21+",
        ),
        recordsTitle = stringResource(R.string.share_records),
        records = records,
        noRecords = stringResource(R.string.share_no_records),
        footer = stringResource(R.string.share_footer),
    )
    val text = stringResource(R.string.share_text, week.workouts, streak)
    val chooser = stringResource(R.string.share_results)
    val error = stringResource(R.string.share_error)

    IconButton(
        enabled = workoutDates.isNotEmpty(),
        onClick = {
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.Default) { renderShareCard(data) }
                    withContext(Dispatchers.IO) { shareImage(context, bitmap, text, chooser) }
                } catch (e: Exception) {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            }
        },
    ) {
        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share_results))
    }
}

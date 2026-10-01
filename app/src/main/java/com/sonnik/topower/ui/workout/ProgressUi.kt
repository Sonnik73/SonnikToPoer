package com.sonnik.topower.ui.workout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetWithContext
import com.sonnik.topower.domain.computeProgress
import com.sonnik.topower.domain.formatNumber
import com.sonnik.topower.domain.setScore
import com.sonnik.topower.ui.common.formatDate
import com.sonnik.topower.ui.common.formatSet
import com.sonnik.topower.ui.inventory.equipmentChipLabel
import com.sonnik.topower.ui.stats.config
import com.sonnik.topower.ui.stats.toRecord
import kotlin.math.abs

/**
 * Рекорд и застой по упражнению относительно текущей тренировки:
 * [record] и [stalledSessions] считаются по тренировкам до неё, [currentBest] — лучший подход в ней.
 */
data class ProgressUi(
    val record: SetWithContext?,
    val recordScore: Double?,
    val stalledSessions: Int,
    val currentBest: Double?,
) {
    val isNewRecord: Boolean
        get() = currentBest != null && (recordScore == null || currentBest > recordScore + 1e-9) && record != null
}

/** Рассчитывает [ProgressUi] по всем подходам упражнения. */
fun progressFor(
    rows: List<SetWithContext>,
    exercise: ExerciseEntity,
    workoutId: String,
    workoutStartedAt: Long,
): ProgressUi {
    val cfg = exercise.config()
    val prior = rows.filter { it.workoutId != workoutId && it.workoutDate < workoutStartedAt }
    val progress = computeProgress(prior.map { it.toRecord() }, cfg)
    val currentBest = rows.filter { it.workoutId == workoutId }.mapNotNull { setScore(it.toRecord(), cfg) }.maxOrNull()
    return ProgressUi(
        record = progress.recordIndex?.let { prior[it] },
        recordScore = progress.recordScore,
        stalledSessions = progress.stalledSessions,
        currentBest = currentBest,
    )
}

/** Строки «Рекорд», «Новый рекорд!» и подсказка о застое под названием упражнения. */
@Composable
fun ProgressLines(
    progress: ProgressUi?,
    exercise: ExerciseEntity,
    equipment: Map<String, EquipmentEntity>,
    plateauSessions: Int?,
) {
    progress ?: return
    val modifier = Modifier.padding(end = 12.dp, bottom = 4.dp)
    progress.record?.let { record ->
        Text(
            stringResource(
                R.string.record_line,
                formatSet(record.set, exercise, record.bodyWeight, equipment),
                formatDate(record.workoutDate),
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier,
        )
    }
    when {
        progress.isNewRecord -> Text(
            stringResource(R.string.new_record_badge),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
            modifier = modifier,
        )
        plateauSessions != null && progress.stalledSessions >= plateauSessions -> Text(
            stringResource(
                R.string.plateau_line,
                progress.stalledSessions,
                plateauSuggestions(exercise, progress.record, equipment.values.toList()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
    }
}

/** Варианты увеличения нагрузки: следующий снаряд из инвентаря, +вес, +повтор, +время, +дистанция. */
@Composable
fun plateauSuggestions(exercise: ExerciseEntity, record: SetWithContext?, equipment: List<EquipmentEntity>): String {
    val options = mutableListOf<String>()
    val type = EquipmentType.of(exercise.equipmentType)
    val set = record?.set
    val weight = abs(set?.weight ?: 0.0)
    val available = equipment.filter { !it.archived && it.type == exercise.equipmentType }

    if (type?.isBand == true) {
        // Резина сопротивляется — нужна туже; помогает — нужна слабее.
        val next = if (exercise.bandAssist) {
            available.filter { (it.maxResistance ?: 0.0) < weight }.maxByOrNull { it.maxResistance ?: 0.0 }
        } else {
            available.filter { (it.maxResistance ?: 0.0) > weight }.minByOrNull { it.maxResistance ?: 0.0 }
        }
        next?.let { options += stringResource(R.string.suggest_item, equipmentChipLabel(it)) }
    } else if (exercise.trackWeight || type != null) {
        val next = available.filter { (it.weight ?: 0.0) > weight }.minByOrNull { it.weight ?: 0.0 }
        options += if (next != null) {
            stringResource(R.string.suggest_item, equipmentChipLabel(next))
        } else {
            stringResource(R.string.suggest_plus_kg, formatNumber(2.5))
        }
    }
    if (exercise.trackReps) options += stringResource(R.string.suggest_plus_rep)
    if (exercise.trackTime) options += stringResource(R.string.suggest_plus_time, 10)
    if (exercise.trackDistance) {
        val step = ((set?.distanceKm ?: 1.0) * 0.1).coerceAtLeast(0.1)
        options += stringResource(R.string.suggest_plus_distance, formatNumber(step, 1))
    }
    return options.joinToString(stringResource(R.string.plateau_or))
}

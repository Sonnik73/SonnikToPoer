package com.sonnik.topower.ui.workout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetWithContext
import com.sonnik.topower.domain.Metric
import com.sonnik.topower.domain.computeProgress
import com.sonnik.topower.domain.setValue
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

/** Больше повторов — формула Эпли сильно завышает максимум. */
private const val MAX_E1RM_REPS = 12

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

/** Строки «Рекорд», «Новый рекорд» и подсказка о застое под названием упражнения. */
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
        // Расчётный максимум честен только для подходов до 12 повторов.
        val e1rm = record.set.reps?.takeIf { it in 2..MAX_E1RM_REPS }
            ?.let { setValue(record.toRecord(), Metric.E1RM) }
            ?.let { " · " + stringResource(R.string.e1rm_short, formatNumber(it, 1)) }
        Text(
            stringResource(
                R.string.record_line,
                formatSet(record.set, exercise, record.bodyWeight, equipment),
                formatDate(record.workoutDate),
            ) + e1rm.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
    when {
        progress.isNewRecord -> Text(
            stringResource(R.string.new_record_badge),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier,
        )
        plateauSessions != null && progress.stalledSessions >= plateauSessions -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(end = 12.dp, top = 2.dp, bottom = 6.dp),
        ) {
            Text(
                stringResource(
                    R.string.plateau_line,
                    progress.stalledSessions,
                    plateauSuggestions(exercise, progress.record, equipment.values.toList()),
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
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

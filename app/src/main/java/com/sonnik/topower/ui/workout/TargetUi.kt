package com.sonnik.topower.ui.workout

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sonnik.topower.R
import com.sonnik.topower.data.EquipmentEntity
import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetEntity
import com.sonnik.topower.domain.PlainSet
import com.sonnik.topower.domain.SetTarget
import com.sonnik.topower.domain.TargetKind
import com.sonnik.topower.domain.nextTarget
import com.sonnik.topower.domain.repRange
import com.sonnik.topower.ui.common.formatSet
import com.sonnik.topower.ui.stats.config

/** Цель на сегодня: [set] — подход, которым заполняется форма, [sets] — сколько рабочих подходов было. */
data class TargetUi(val target: SetTarget, val set: SetEntity, val sets: Int)

/** Шаг веса для упражнений без инвентаря. */
private const val WEIGHT_STEP = 2.5

/**
 * Следующая нагрузка после [weight]: ближайший снаряд тяжелее из инвентаря (для резины-помощника —
 * слабее), без инвентаря — +[WEIGHT_STEP] кг. null — тяжелее нечем.
 */
private fun nextLoad(exercise: ExerciseEntity, weight: Double, equipment: List<EquipmentEntity>): Pair<Double, EquipmentEntity?>? {
    val type = EquipmentType.of(exercise.equipmentType)
    val available = equipment.filter { !it.archived && it.type == exercise.equipmentType }
    val candidates = when {
        type?.isBand == true -> available.mapNotNull { eq ->
            eq.maxResistance?.let { (if (exercise.bandAssist) -it else it) to eq }
        }
        type != null -> available.mapNotNull { eq -> eq.weight?.let { it to eq } }
        else -> return (weight + WEIGHT_STEP) to null
    }
    return candidates.filter { it.first > weight + 1e-9 }.minByOrNull { it.first }
}

/** Рассчитывает цель по рабочим подходам прошлой тренировки [previous]. */
fun targetFor(exercise: ExerciseEntity, previous: List<SetEntity>, equipment: List<EquipmentEntity>): TargetUi? {
    val working = previous.filter { !it.isWarmup }
    if (working.isEmpty()) return null
    val loads = mutableMapOf<Double, EquipmentEntity?>()
    val target = nextTarget(
        previous = working.map { PlainSet(it.reps, it.weight, it.durationSec, it.distanceKm) },
        cfg = exercise.config(),
        repRange = repRange(exercise.repMin, exercise.repMax),
    ) { w -> nextLoad(exercise, w, equipment)?.let { (weight, eq) -> loads[weight] = eq; weight } } ?: return null

    // Основа — последний подход в рабочем весе: снаряд, пара, резина остаются те же.
    val topWeight = working.mapNotNull { it.weight }.maxOrNull()
    val base = (working.lastOrNull { it.weight == topWeight } ?: working.last()).copy(
        reps = target.reps,
        durationSec = target.durationSec,
        distanceKm = target.distanceKm,
        isWarmup = false,
    )
    val set = if (target.kind == TargetKind.MORE_LOAD) {
        val eq = loads[target.weight]
        when {
            eq == null -> base.copy(weight = target.weight, equipmentId = null, equipment2Id = null, bandMin = null)
            eq.equipmentType.isBand -> base.copy(
                weight = target.weight,
                equipmentId = eq.id,
                equipment2Id = null,
                bandMin = eq.minResistance ?: 0.0,
            )
            else -> base.copy(weight = target.weight, equipmentId = eq.id, equipment2Id = null, bandMin = null)
        }
    } else {
        base
    }
    return TargetUi(target, set, working.size)
}

/** Строка «Цель: …» под прошлой тренировкой. В компактном виде стоит в одной строке с «Прошлый раз». */
@Composable
fun TargetLine(
    target: TargetUi,
    exercise: ExerciseEntity,
    equipment: Map<String, EquipmentEntity>,
    compact: Boolean = false,
) {
    val sets = formatSet(target.set, exercise, null, equipment) + if (target.sets > 1) " ×${target.sets}" else ""
    val note = when (target.target.kind) {
        TargetKind.MORE_LOAD -> stringResource(R.string.target_more_load)
        TargetKind.MAKE_HARDER -> stringResource(R.string.target_harder)
        else -> null
    }
    Text(
        stringResource(R.string.target_line, sets) + (note?.let { " — $it" } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = if (compact) Modifier.padding(start = 8.dp) else Modifier.padding(end = 12.dp, bottom = 4.dp),
    )
}

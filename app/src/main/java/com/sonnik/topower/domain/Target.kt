package com.sonnik.topower.domain

/** Подход прошлой тренировки для расчёта цели. Вес — со знаком (помощь резины отрицательна). */
data class PlainSet(
    val reps: Int? = null,
    val weight: Double? = null,
    val durationSec: Int? = null,
    val distanceKm: Double? = null,
)

enum class TargetKind {
    /** Тот же вес, на повтор больше. */
    MORE_REPS,

    /** Верх диапазона повторов достигнут: следующий вес, повторы — от нижней границы. */
    MORE_LOAD,

    /** Верх диапазона достигнут, а тяжелее нечем: добавить вес или взять вариант сложнее. */
    MAKE_HARDER,
    MORE_TIME,
    MORE_DISTANCE,
}

data class SetTarget(
    val reps: Int?,
    val weight: Double?,
    val durationSec: Int?,
    val distanceKm: Double?,
    val kind: TargetKind,
)

/**
 * Цель на сегодня по двойной прогрессии: в рабочем весе прошлой тренировки добавляем по повтору,
 * пока самый слабый подход не дойдёт до верха [repRange], затем — следующий вес с нижней границы.
 * Без диапазона — просто +1 повтор. Упражнения на время и дистанцию растят их понемногу.
 *
 * [previous] — рабочие подходы прошлой тренировки; [nextWeight] — следующая нагрузка после
 * данной (из инвентаря или с шагом) или null, если тяжелее нечем.
 */
fun nextTarget(
    previous: List<PlainSet>,
    cfg: ExerciseConfig,
    repRange: IntRange?,
    nextWeight: (Double) -> Double?,
): SetTarget? {
    if (previous.isEmpty()) return null
    val topWeight = if (cfg.trackWeight) previous.mapNotNull { it.weight }.maxOrNull() else null
    val top = if (topWeight != null) previous.filter { it.weight == topWeight } else previous
    val duration = top.mapNotNull { it.durationSec }.maxOrNull()
    val distance = top.mapNotNull { it.distanceKm }.maxOrNull()

    if (cfg.trackReps) {
        val reps = top.mapNotNull { it.reps }.minOrNull() ?: return null
        if (repRange != null && reps >= repRange.last) {
            val heavier = if (cfg.trackWeight) nextWeight(topWeight ?: 0.0) else null
            return if (heavier != null) {
                SetTarget(repRange.first, heavier, duration, distance, TargetKind.MORE_LOAD)
            } else {
                SetTarget(reps, topWeight, duration, distance, TargetKind.MAKE_HARDER)
            }
        }
        val next = (reps + 1).let { if (repRange != null) it.coerceAtLeast(repRange.first) else it }
        return SetTarget(next, topWeight, duration, distance, TargetKind.MORE_REPS)
    }
    if (cfg.trackTime && duration != null) {
        val step = if (duration < 60) 5 else 10
        return SetTarget(null, topWeight, duration + step, distance, TargetKind.MORE_TIME)
    }
    if (cfg.trackDistance && distance != null) {
        // +5 %, но не меньше 100 м, с округлением до 100 м.
        val more = Math.round((distance + maxOf(0.1, distance * 0.05)) * 10) / 10.0
        return SetTarget(null, topWeight, duration, more, TargetKind.MORE_DISTANCE)
    }
    return null
}

/** Нормализованный диапазон повторов упражнения: обе границы ≥ 1, нижняя не больше верхней. */
fun repRange(min: Int?, max: Int?): IntRange? {
    val hi = max ?: return null
    val lo = (min ?: hi).coerceAtMost(hi)
    return if (lo < 1) null else lo..hi
}

package com.sonnik.topower.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val DAY_MS = 86_400_000L

/**
 * Сколько недель подряд (с понедельника) были тренировки, считая от текущей. Если на этой неделе
 * тренировок ещё нет, серия не прерывается — считаем от прошлой.
 */
fun weekStreak(workoutDates: List<Long>, today: LocalDate, zone: ZoneId): Int {
    val weeks = workoutDates.map { weekStart(Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) }.toSet()
    var week = weekStart(today)
    if (week !in weeks) week = week.minusWeeks(1)
    var streak = 0
    while (week in weeks) {
        streak++
        week = week.minusWeeks(1)
    }
    return streak
}

/** Рекорд за период: лучший подход [record] против прежнего лучшего результата [previousScore]. */
data class RecentRecord<T>(val exerciseId: String, val record: T, val score: Double, val previousScore: Double) {
    val gainPercent: Double get() = if (previousScore > 0) (score - previousScore) / previousScore * 100 else 0.0
}

/**
 * Рекорды, поставленные начиная с [since]: по каждому упражнению лучший подход периода, если он
 * превзошёл всё, что было до периода. Упражнения без истории до периода не считаются — первый
 * результат ещё не рекорд. Сортировка — по приросту в процентах.
 */
fun <T> recentRecords(
    rows: List<T>,
    exerciseId: (T) -> String,
    record: (T) -> SetRecord,
    config: (String) -> ExerciseConfig?,
    since: Long,
): List<RecentRecord<T>> =
    rows.groupBy(exerciseId).mapNotNull { (id, sets) ->
        val cfg = config(id) ?: return@mapNotNull null
        val scored = sets.mapNotNull { row -> setScore(record(row), cfg)?.let { row to it } }
        val before = scored.filter { record(it.first).date < since }.maxOfOrNull { it.second } ?: return@mapNotNull null
        val best = scored.filter { record(it.first).date >= since }.maxByOrNull { it.second } ?: return@mapNotNull null
        if (best.second > before + 1e-9) RecentRecord(id, best.first, best.second, before) else null
    }.sortedByDescending { it.gainPercent }

/** Итоги за последние [days] дней: тренировки, рабочие подходы и тоннаж. */
data class PeriodSummary(val workouts: Int, val sets: Int, val tonnage: Double)

fun periodSummary(workoutDates: List<Long>, sets: List<SetRecord>, now: Long, days: Int): PeriodSummary {
    val since = now - days * DAY_MS
    val working = sets.filter { !it.isWarmup && it.date >= since }
    return PeriodSummary(
        workouts = workoutDates.count { it >= since },
        sets = working.size,
        tonnage = working.sumOf { setValue(it, Metric.VOLUME) ?: 0.0 },
    )
}

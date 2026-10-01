package com.sonnik.topower.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Какие параметры измеряются в упражнении. */
data class ExerciseConfig(
    val trackReps: Boolean,
    val trackWeight: Boolean,
    val trackTime: Boolean,
    val trackDistance: Boolean,
    val useBodyWeight: Boolean,
) {
    /** Есть ли у упражнения «рабочий вес» — отягощение и/или собственный вес. */
    val hasLoad: Boolean get() = trackWeight || useBodyWeight
}

/** Один подход в контексте тренировки. */
data class SetRecord(
    val workoutId: String,
    val date: Long,
    val bodyWeight: Double?,
    val useBodyWeight: Boolean,
    val reps: Int?,
    val weight: Double?,
    val durationSec: Int?,
    val distanceKm: Double?,
    val isWarmup: Boolean,
)

enum class Metric { WEIGHT, REPS, E1RM, VOLUME, TIME, DISTANCE }

/** Рабочий вес: при учёте собственного веса = вес тела + дополнительный вес. */
fun effectiveWeight(r: SetRecord): Double? =
    if (r.useBodyWeight) {
        if (r.bodyWeight == null && r.weight == null) null else (r.bodyWeight ?: 0.0) + (r.weight ?: 0.0)
    } else {
        r.weight
    }

/** Оценка разового максимума по формуле Эпли. */
fun estimatedOneRepMax(weight: Double, reps: Int): Double? = when {
    reps <= 0 || weight <= 0.0 -> null
    reps == 1 -> weight
    else -> weight * (1 + reps / 30.0)
}

fun availableMetrics(cfg: ExerciseConfig): List<Metric> = buildList {
    if (cfg.hasLoad) add(Metric.WEIGHT)
    if (cfg.trackReps) add(Metric.REPS)
    if (cfg.hasLoad && cfg.trackReps) {
        add(Metric.E1RM)
        add(Metric.VOLUME)
    }
    if (cfg.trackTime) add(Metric.TIME)
    if (cfg.trackDistance) add(Metric.DISTANCE)
}

fun setValue(r: SetRecord, metric: Metric): Double? = when (metric) {
    Metric.WEIGHT -> effectiveWeight(r)
    Metric.REPS -> r.reps?.toDouble()
    Metric.E1RM -> {
        val w = effectiveWeight(r)
        val reps = r.reps
        if (w != null && reps != null) estimatedOneRepMax(w, reps) else null
    }
    Metric.VOLUME -> {
        val w = effectiveWeight(r)
        val reps = r.reps
        if (w != null && reps != null) w * reps else null
    }
    Metric.TIME -> r.durationSec?.toDouble()
    Metric.DISTANCE -> r.distanceKm
}

/** Значение метрики за тренировку: объём суммируется, остальное — максимум. */
fun sessionValue(sets: List<SetRecord>, metric: Metric): Double? {
    val values = sets.mapNotNull { setValue(it, metric) }
    if (values.isEmpty()) return null
    return if (metric == Metric.VOLUME) values.sum() else values.max()
}

data class MetricPoint(val date: Long, val value: Double)

data class MetricSummary(
    val metric: Metric,
    val first: MetricPoint,
    val last: MetricPoint,
    val best: MetricPoint,
    val series: List<MetricPoint>,
) {
    /** Прирост от первой тренировки до лучшего результата. */
    val gain: Double get() = best.value - first.value
    val gainPercent: Double? get() = if (first.value > 0.0) gain / first.value * 100.0 else null
}

data class ExerciseStats(
    val workoutCount: Int,
    val setCount: Int,
    val firstDate: Long,
    val lastDate: Long,
    val firstSessionSets: List<SetRecord>,
    val bestSessionSets: List<SetRecord>,
    val bestSet: SetRecord?,
    val metrics: List<MetricSummary>,
)

/**
 * Статистика по упражнению. Разминочные подходы не учитываются.
 * [records] — подходы, отсортированные по дате тренировки.
 */
fun computeExerciseStats(records: List<SetRecord>, cfg: ExerciseConfig): ExerciseStats? {
    val working = records.filter { !it.isWarmup }
    if (working.isEmpty()) return null
    val sessions: List<List<SetRecord>> = working
        .groupBy { it.workoutId }
        .values
        .sortedBy { it.first().date }

    val metrics = availableMetrics(cfg).mapNotNull { metric ->
        val series = sessions.mapNotNull { s -> sessionValue(s, metric)?.let { MetricPoint(s.first().date, it) } }
        if (series.isEmpty()) {
            null
        } else {
            MetricSummary(
                metric = metric,
                first = series.first(),
                last = series.last(),
                best = series.maxBy { it.value },
                series = series,
            )
        }
    }

    val bestMetric = when {
        cfg.hasLoad && cfg.trackReps -> Metric.E1RM
        else -> availableMetrics(cfg).firstOrNull()
    }
    val bestSet = bestMetric?.let { m -> working.filter { setValue(it, m) != null }.maxByOrNull { setValue(it, m)!! } }
    val bestSession = bestSet?.let { b -> sessions.first { it.first().workoutId == b.workoutId } } ?: emptyList()

    return ExerciseStats(
        workoutCount = sessions.size,
        setCount = working.size,
        firstDate = sessions.first().first().date,
        lastDate = sessions.last().first().date,
        firstSessionSets = sessions.first(),
        bestSessionSets = bestSession,
        bestSet = bestSet,
        metrics = metrics,
    )
}

data class WeekCount(val weekStart: LocalDate, val count: Int)

data class OverallStats(
    val totalWorkouts: Int,
    val workoutsThisWeek: Int,
    val workoutsThisMonth: Int,
    val totalSets: Int,
    val totalTonnage: Double,
    val totalDistanceKm: Double,
    val totalDurationSec: Long,
    val weekly: List<WeekCount>,
)

fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun computeOverallStats(
    workoutDates: List<Long>,
    sets: List<SetRecord>,
    today: LocalDate,
    zone: ZoneId,
    weeks: Int = 12,
): OverallStats {
    val dates = workoutDates.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    val thisWeek = weekStart(today)
    val working = sets.filter { !it.isWarmup }
    val tonnage = working.sumOf { r ->
        val w = effectiveWeight(r)
        val reps = r.reps
        if (w != null && reps != null) w * reps else 0.0
    }
    val weekly = (weeks - 1 downTo 0).map { back ->
        val start = thisWeek.minusWeeks(back.toLong())
        WeekCount(start, dates.count { weekStart(it) == start })
    }
    return OverallStats(
        totalWorkouts = dates.size,
        workoutsThisWeek = dates.count { weekStart(it) == thisWeek },
        workoutsThisMonth = dates.count { it.year == today.year && it.month == today.month },
        totalSets = working.size,
        totalTonnage = tonnage,
        totalDistanceKm = working.sumOf { it.distanceKm ?: 0.0 },
        totalDurationSec = working.sumOf { (it.durationSec ?: 0).toLong() },
        weekly = weekly,
    )
}

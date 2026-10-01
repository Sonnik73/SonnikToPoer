package com.sonnik.topower.domain

/**
 * Метрика, по которой выбирается рекордный подход: для упражнений с весом и повторениями —
 * расчётный максимум (честно сравнивает 60×10 и 65×6), иначе — первая измеряемая метрика.
 */
fun recordMetric(cfg: ExerciseConfig): Metric? =
    if (cfg.hasLoad && cfg.trackReps) Metric.E1RM else availableMetrics(cfg).firstOrNull()

/** Оценка подхода по [recordMetric]; разминка не оценивается. */
fun setScore(r: SetRecord, cfg: ExerciseConfig): Double? {
    if (r.isWarmup) return null
    val metric = recordMetric(cfg) ?: return null
    return setValue(r, metric)
}

/** Метрики, рост любой из которых считается прогрессом. Объём не учитывается: его растят числом подходов. */
private fun growthMetrics(cfg: ExerciseConfig): List<Metric> =
    availableMetrics(cfg).filter { it != Metric.VOLUME }

data class ExerciseProgress(
    /** Индекс рекордного подхода в исходном списке или null, если оценивать нечего. */
    val recordIndex: Int?,
    val recordScore: Double?,
    /** Сколько последних тренировок подряд ни один показатель не превзошёл прежний максимум. */
    val stalledSessions: Int,
    val sessionCount: Int,
)

/**
 * Рекорд и застой по истории упражнения. [records] — подходы, отсортированные по дате тренировки.
 * Тренировка считается ростом, если хотя бы одна метрика превысила максимум всех предыдущих тренировок.
 */
fun computeProgress(records: List<SetRecord>, cfg: ExerciseConfig): ExerciseProgress {
    var recordIndex: Int? = null
    var recordScore: Double? = null
    records.forEachIndexed { i, r ->
        val score = setScore(r, cfg) ?: return@forEachIndexed
        if (recordScore == null || score > recordScore!!) {
            recordScore = score
            recordIndex = i
        }
    }

    val metrics = growthMetrics(cfg)
    val sessions = records.filter { !it.isWarmup }.groupBy { it.workoutId }.values.sortedBy { it.first().date }
    val best = mutableMapOf<Metric, Double>()
    var stalled = 0
    sessions.forEach { session ->
        var grew = false
        metrics.forEach { m ->
            val value = sessionValue(session, m) ?: return@forEach
            val prev = best[m]
            if (prev == null || value > prev + 1e-9) {
                grew = true
                best[m] = value
            }
        }
        stalled = if (grew) 0 else stalled + 1
    }
    return ExerciseProgress(recordIndex, recordScore, stalled, sessions.size)
}

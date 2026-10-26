package com.sonnik.topower.ecosystem

import com.sonnik.ecosystem.contract.EcosystemContract
import com.sonnik.topower.data.AppDao
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.SetWithContext
import com.sonnik.topower.data.WorkoutEntity
import com.sonnik.topower.domain.Metric
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.recentRecords
import com.sonnik.topower.domain.recordMetric
import com.sonnik.topower.domain.recoveredFraction
import com.sonnik.topower.domain.setValue
import com.sonnik.topower.domain.state
import com.sonnik.topower.reminders.muscleStatuses
import com.sonnik.topower.ui.stats.config
import com.sonnik.topower.ui.stats.toRecord

private const val DAY_MS = 86_400_000L

/** Период выборки, включительно с обеих сторон. */
data class TimeRange(val from: Long, val to: Long) {
    operator fun contains(time: Long): Boolean = time in from..to

    companion object {
        /** Последние [EcosystemContract.DEFAULT_PERIOD_DAYS] дней до [now]. */
        fun lastDays(now: Long): TimeRange = TimeRange(now - EcosystemContract.DEFAULT_PERIOD_DAYS * DAY_MS, now)
    }
}

/**
 * Сводки для приложений экосистемы. Строка — значения в порядке колонок из [EcosystemContract].
 * Тренировка попадает в период по времени начала; разминочные подходы не учитываются нигде.
 * Тоннаж считается так же, как в общей статистике: рабочий вес × повторы × число снарядов.
 */
class EcosystemData(private val dao: AppDao) {

    suspend fun info(appVersion: String, lastChangedMark: Long): List<Array<Any?>> {
        val lastChanged = maxOf(lastChangedMark, dao.lastTrainingUpdate() ?: 0L)
        return listOf(arrayOf(EcosystemContract.SCHEMA_VERSION, appVersion, lastChanged))
    }

    suspend fun workouts(range: TimeRange): List<Array<Any?>> {
        val sets = dao.allSetsWithContext().filter { !it.set.isWarmup }.groupBy { it.workoutId }
        return dao.allWorkouts().filter { it.startedAt in range }.sortedBy { it.startedAt }.map { w ->
            val working = sets[w.id].orEmpty()
            arrayOf(
                w.id,
                w.startedAt,
                w.finishedAt,
                durationSec(w),
                working.size,
                working.sumOf(::tonnage),
                w.bodyWeight,
                w.avgHeartRate,
                w.maxHeartRate,
                w.calories,
            )
        }
    }

    suspend fun muscleVolume(range: TimeRange): List<Array<Any?>> {
        val exercises = dao.allExercises().associateBy { it.id }
        val working = dao.allSetsWithContext().filter { !it.set.isWarmup && it.workoutDate in range }
        val byMuscle = working.flatMap { s -> musclesOf(exercises[s.exerciseId]).map { it to s } }
            .groupBy({ it.first }, { it.second })
        return Muscle.entries.mapNotNull { muscle ->
            val list = byMuscle[muscle] ?: return@mapNotNull null
            arrayOf(
                muscle.name,
                list.size,
                list.sumOf { it.set.reps ?: 0 },
                list.sumOf(::tonnage),
                list.map { it.workoutId }.distinct().size,
            )
        }
    }

    /** Состояние каждой мышцы — тем же расчётом, что схема восстановления в приложении. */
    suspend fun recovery(now: Long): List<Array<Any?>> {
        val statuses = muscleStatuses(dao.muscleWork())
        return Muscle.entries.map { muscle ->
            val status = statuses.getValue(muscle)
            arrayOf(
                muscle.name,
                status.state(now).name,
                status.lastTrained,
                status.readyAt,
                status.recoveredFraction(now).toDouble(),
            )
        }
    }

    suspend fun bodyWeight(range: TimeRange): List<Array<Any?>> =
        dao.bodyWeights().filter { it.startedAt in range }.map { arrayOf(it.startedAt, it.bodyWeight) }

    /** Новые рекорды начиная с [since] — та же логика, что «Рекорды» в карточке «Поделиться». */
    suspend fun records(since: Long): List<Array<Any?>> {
        val exercises = dao.allExercises().associateBy { it.id }
        return recentRecords(
            rows = dao.allSetsWithContext(),
            exerciseId = { it.exerciseId },
            record = { it.toRecord() },
            config = { id -> exercises[id]?.config() },
            since = since,
        ).sortedBy { it.record.workoutDate }.mapNotNull { r ->
            val exercise = exercises[r.exerciseId] ?: return@mapNotNull null
            val metric = recordMetric(exercise.config()) ?: return@mapNotNull null
            arrayOf(r.exerciseId, exercise.name, metric.name, r.score, r.record.workoutDate)
        }
    }

    private fun musclesOf(exercise: ExerciseEntity?): Set<Muscle> = exercise?.let { Muscle.parse(it.muscles) }.orEmpty()

    private fun tonnage(s: SetWithContext): Double = setValue(s.toRecord(), Metric.VOLUME) ?: 0.0

    /** Время с часов, если внесено, иначе от начала до завершения; у незавершённой без часов — null. */
    private fun durationSec(w: WorkoutEntity): Long? =
        w.watchDurationSec?.toLong() ?: w.finishedAt?.let { (it - w.startedAt) / 1000 }
}

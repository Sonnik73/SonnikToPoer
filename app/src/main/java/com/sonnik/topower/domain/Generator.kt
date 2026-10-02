package com.sonnik.topower.domain

/** Упражнение-кандидат для подбора тренировки: [timesDone] — сколько тренировок оно уже было. */
data class ExerciseOption(val id: String, val muscles: Set<Muscle>, val timesDone: Int)

/** Сколько самых отдохнувших мышц стараемся нагрузить. */
const val GENERATED_TARGET_MUSCLES = 6
const val GENERATED_MIN_EXERCISES = 4
const val GENERATED_MAX_EXERCISES = 6

/**
 * Мышцы, которые можно тренировать, от самых отдохнувших: сначала не тренировавшиеся,
 * затем по давности последней нагрузки. Восстанавливающиеся не входят.
 */
fun restedMuscles(statuses: Map<Muscle, MuscleStatus>, now: Long): List<Muscle> =
    Muscle.entries
        .filter { statuses[it]?.state(now) != MuscleState.RECOVERING }
        .sortedBy { statuses[it]?.lastTrained ?: Long.MIN_VALUE }

/**
 * Подбор тренировки на самые отдохнувшие мышцы. Берутся только упражнения, все основные мышцы
 * которых уже восстановились. Для каждой следующей непокрытой мышцы выбирается упражнение,
 * которое покрывает больше всего ещё не нагруженных целевых мышц, при равенстве — привычное.
 * Результат: базовые (многосуставные) упражнения первыми.
 */
fun generateWorkout(options: List<ExerciseOption>, statuses: Map<Muscle, MuscleStatus>, now: Long): List<ExerciseOption> {
    val rested = restedMuscles(statuses, now)
    val allowed = rested.toSet()
    val targets = rested.take(GENERATED_TARGET_MUSCLES).toSet()
    val usable = options.filter { it.muscles.isNotEmpty() && allowed.containsAll(it.muscles) }
    val picked = mutableListOf<ExerciseOption>()
    val covered = mutableSetOf<Muscle>()
    rested.forEachIndexed { i, muscle ->
        if (picked.size >= GENERATED_MAX_EXERCISES) return@forEachIndexed
        if (i >= GENERATED_TARGET_MUSCLES && picked.size >= GENERATED_MIN_EXERCISES) return@forEachIndexed
        if (muscle in covered) return@forEachIndexed
        val best = usable
            .filter { muscle in it.muscles && it !in picked }
            .maxWithOrNull(
                compareBy<ExerciseOption> { (it.muscles intersect targets - covered).size }
                    .thenBy { it.timesDone }
                    .thenByDescending { it.id }
            ) ?: return@forEachIndexed
        picked += best
        covered += best.muscles
    }
    return picked.sortedByDescending { it.muscles.size }
}

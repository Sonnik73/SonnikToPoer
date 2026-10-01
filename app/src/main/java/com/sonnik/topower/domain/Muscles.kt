package com.sonnik.topower.domain

/** Мышечные группы на схеме тела. */
enum class Muscle {
    CHEST, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, TRAPS, LATS, LOWER_BACK,
    BICEPS, TRICEPS, FOREARMS, ABS, OBLIQUES, GLUTES, QUADS, HAMSTRINGS, ADDUCTORS, CALVES;

    companion object {
        /** «CHEST,TRICEPS» → набор мышц; неизвестные значения пропускаются. */
        fun parse(text: String): Set<Muscle> =
            text.split(',').mapNotNull { part -> entries.firstOrNull { it.name == part.trim() } }.toSet()

        fun format(muscles: Collection<Muscle>): String = muscles.sortedBy { it.ordinal }.joinToString(",") { it.name }
    }
}

/** Срок восстановления, если упражнение не оценили. */
const val DEFAULT_RECOVERY_HOURS = 48

/** Добавка к сроку за каждое следующее упражнение на ту же мышцу в одной тренировке. */
const val EXTRA_EXERCISE_HOURS = 12
const val MAX_EXTRA_HOURS = 24

/** Через сколько дней без нагрузки мышца считается давно не тренированной. */
const val STALE_DAYS = 10

private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 24 * HOUR_MS

/** Упражнение в тренировке: какие мышцы нагрузило и какой срок восстановления выбрали. */
data class MuscleWork(
    val workoutId: String,
    val time: Long,
    val muscles: Set<Muscle>,
    val recoveryHours: Int?,
)

data class MuscleStatus(val muscle: Muscle, val lastTrained: Long?, val readyAt: Long?)

enum class MuscleState { RECOVERING, READY, STALE, NEVER }

fun MuscleStatus.state(now: Long): MuscleState = when {
    lastTrained == null || readyAt == null -> MuscleState.NEVER
    now < readyAt -> MuscleState.RECOVERING
    now - lastTrained > STALE_DAYS * DAY_MS -> MuscleState.STALE
    else -> MuscleState.READY
}

/** Доля пройденного восстановления от 0 (только что) до 1 (восстановилась). */
fun MuscleStatus.recoveredFraction(now: Long): Float {
    val last = lastTrained ?: return 1f
    val ready = readyAt ?: return 1f
    if (ready <= last) return 1f
    return ((now - last).toFloat() / (ready - last)).coerceIn(0f, 1f)
}

/** Сколько часов осталось до восстановления (округление вверх), 0 — уже восстановилась. */
fun MuscleStatus.hoursLeft(now: Long): Int {
    val ready = readyAt ?: return 0
    return if (now >= ready) 0 else ((ready - now + HOUR_MS - 1) / HOUR_MS).toInt()
}

/**
 * Состояние каждой мышцы. Срок восстановления мышцы в тренировке — наибольший из сроков упражнений на неё
 * плюс [EXTRA_EXERCISE_HOURS] за каждое дополнительное упражнение (не больше [MAX_EXTRA_HOURS]).
 */
fun computeMuscleStatus(work: List<MuscleWork>): Map<Muscle, MuscleStatus> {
    val lastTrained = mutableMapOf<Muscle, Long>()
    val readyAt = mutableMapOf<Muscle, Long>()
    work.groupBy { it.workoutId }.values.forEach { exercises ->
        val time = exercises.maxOf { it.time }
        Muscle.entries.forEach { muscle ->
            val hitting = exercises.filter { muscle in it.muscles }
            if (hitting.isEmpty()) return@forEach
            val base = hitting.maxOf { it.recoveryHours ?: DEFAULT_RECOVERY_HOURS }
            val extra = ((hitting.size - 1) * EXTRA_EXERCISE_HOURS).coerceAtMost(MAX_EXTRA_HOURS)
            val ready = time + (base + extra) * HOUR_MS
            lastTrained[muscle] = maxOf(lastTrained[muscle] ?: Long.MIN_VALUE, time)
            readyAt[muscle] = maxOf(readyAt[muscle] ?: Long.MIN_VALUE, ready)
        }
    }
    return Muscle.entries.associateWith { MuscleStatus(it, lastTrained[it], readyAt[it]) }
}

/** Ключевые слова названий упражнений → основные мышцы (для подсказки при создании упражнения). */
private val keywordMuscles: List<Pair<List<String>, Set<Muscle>>> = listOf(
    listOf("разводк", "fly", "кроссовер") to setOf(Muscle.CHEST),
    listOf("отжиман", "push-up", "pushup") to setOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.FRONT_DELTS),
    listOf("жим леж", "жим лёж", "bench") to setOf(Muscle.CHEST, Muscle.TRICEPS, Muscle.FRONT_DELTS),
    listOf("брусь", "dip") to setOf(Muscle.CHEST, Muscle.TRICEPS),
    listOf("трицепс", "tricep", "французск") to setOf(Muscle.TRICEPS),
    listOf("молот", "hammer") to setOf(Muscle.BICEPS, Muscle.FOREARMS),
    listOf("бицепс", "bicep", "сгибани", "curl") to setOf(Muscle.BICEPS),
    listOf("обратным хват", "chin") to setOf(Muscle.LATS, Muscle.BICEPS),
    listOf("подтягиван", "pull-up", "pullup", "тяга верхнего", "pulldown") to setOf(Muscle.LATS),
    listOf("ли хейни", "шраг", "shrug") to setOf(Muscle.TRAPS, Muscle.LATS, Muscle.REAR_DELTS),
    listOf("тяга", "row") to setOf(Muscle.LATS),
    listOf("средняя дельт", "махи в сторон", "lateral raise") to setOf(Muscle.SIDE_DELTS),
    listOf("задняя дельт", "rear delt") to setOf(Muscle.REAR_DELTS),
    listOf("передняя дельт", "жим стоя", "армейск", "overhead press") to setOf(Muscle.FRONT_DELTS),
    listOf("пресс", "скручиван", "crunch", "ролик", "планк", "plank") to setOf(Muscle.ABS),
    listOf("косые", "oblique") to setOf(Muscle.OBLIQUES),
    listOf("гиперэкстенз", "становая", "deadlift") to setOf(Muscle.LOWER_BACK, Muscle.GLUTES, Muscle.HAMSTRINGS),
    listOf("присед", "squat", "выпад", "lunge") to setOf(Muscle.QUADS, Muscle.GLUTES),
    listOf("ягодиц", "мостик", "glute") to setOf(Muscle.GLUTES),
    listOf("икр", "носк", "скакалк", "calf") to setOf(Muscle.CALVES),
)

/** Подсказка мышц по названию упражнения; пустой набор, если ничего не подошло. */
fun suggestMuscles(name: String): Set<Muscle> {
    val text = name.lowercase()
    return keywordMuscles.firstOrNull { (keys, _) -> keys.any { it in text } }?.second.orEmpty()
}

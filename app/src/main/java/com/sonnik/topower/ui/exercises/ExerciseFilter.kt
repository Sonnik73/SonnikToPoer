package com.sonnik.topower.ui.exercises

import com.sonnik.topower.data.EquipmentType
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.domain.Muscle

/** Крупные части тела для фильтра списка упражнений. */
enum class BodyArea(val muscles: Set<Muscle>) {
    CHEST(setOf(Muscle.CHEST, Muscle.SERRATUS)),
    BACK(setOf(Muscle.LATS, Muscle.TRAPS, Muscle.LOWER_BACK, Muscle.TERES_MAJOR, Muscle.INFRASPINATUS)),
    SHOULDERS(setOf(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.REAR_DELTS, Muscle.INFRASPINATUS)),
    ARMS(setOf(Muscle.BICEPS, Muscle.TRICEPS, Muscle.FOREARMS, Muscle.BRACHIALIS, Muscle.BRACHIORADIALIS)),
    CORE(setOf(Muscle.ABS, Muscle.OBLIQUES)),
    LEGS(setOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.ADDUCTORS, Muscle.CALVES, Muscle.TIBIALIS)),
}

/** Фильтр по инвентарю: тип снаряда или «собственный вес» ([type] == null). */
data class ToolFilter(val type: EquipmentType?) {
    fun matches(e: ExerciseEntity): Boolean =
        if (type == null) e.useBodyWeight else EquipmentType.of(e.equipmentType) == type

    companion object {
        val BODYWEIGHT = ToolFilter(null)

        /** Варианты для списка: собственный вес и типы инвентаря, которые встречаются в упражнениях. */
        fun available(exercises: List<ExerciseEntity>): List<ToolFilter> {
            val types = exercises.mapNotNull { EquipmentType.of(it.equipmentType) }.toSet()
            return listOfNotNull(BODYWEIGHT.takeIf { exercises.any { it.useBodyWeight } }) +
                EquipmentType.entries.filter { it in types }.map { ToolFilter(it) }
        }
    }
}

/**
 * Отбор упражнений: [query] ищется в названии и в названиях основных мышц ([muscleName]),
 * [area] — хотя бы одна основная мышца из части тела, [tool] — инвентарь. Пустой фильтр пропускает всё.
 */
fun filterExercises(
    exercises: List<ExerciseEntity>,
    query: String,
    area: BodyArea?,
    tool: ToolFilter?,
    muscleName: (Muscle) -> String,
): List<ExerciseEntity> {
    val q = query.trim().lowercase()
    return exercises.filter { e ->
        val muscles = Muscle.parse(e.muscles)
        (q.isEmpty() || e.name.lowercase().contains(q) || muscles.any { muscleName(it).lowercase().contains(q) }) &&
            (area == null || muscles.any { it in area.muscles }) &&
            (tool == null || tool.matches(e))
    }
}

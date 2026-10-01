package com.sonnik.topower.data

import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.Muscle.ABS
import com.sonnik.topower.domain.Muscle.BICEPS
import com.sonnik.topower.domain.Muscle.CALVES
import com.sonnik.topower.domain.Muscle.CHEST
import com.sonnik.topower.domain.Muscle.FOREARMS
import com.sonnik.topower.domain.Muscle.FRONT_DELTS
import com.sonnik.topower.domain.Muscle.LATS
import com.sonnik.topower.domain.Muscle.REAR_DELTS
import com.sonnik.topower.domain.Muscle.SIDE_DELTS
import com.sonnik.topower.domain.Muscle.TRAPS
import com.sonnik.topower.domain.Muscle.TRICEPS

/**
 * Упражнения, которые появляются при первой установке. Основные мышцы сверены с описаниями техники:
 * у отжиманий — грудь, трицепс и передняя дельта; у тяги Ли Хейни — широчайшие и трапеции
 * (задняя дельта — вспомогательная, но упражнение делают ради неё).
 */
data class DefaultExercise(
    val nameRu: String,
    val nameEn: String,
    val muscles: Set<Muscle>,
    val trackReps: Boolean = true,
    val trackWeight: Boolean = true,
    val trackTime: Boolean = false,
    val useBodyWeight: Boolean = false,
    val equipmentType: EquipmentType? = null,
) {
    fun toEntity(russian: Boolean) = ExerciseEntity(
        name = if (russian) nameRu else nameEn,
        trackReps = trackReps,
        trackWeight = trackWeight,
        trackTime = trackTime,
        useBodyWeight = useBodyWeight,
        equipmentType = equipmentType?.name,
        muscles = Muscle.format(muscles),
    )
}

val DEFAULT_EXERCISES = listOf(
    DefaultExercise("Отжимания на упорах", "Push-ups on handles", setOf(CHEST, TRICEPS, FRONT_DELTS), useBodyWeight = true),
    DefaultExercise("Разводка гантелей на скамье", "Dumbbell bench fly", setOf(CHEST), equipmentType = EquipmentType.DUMBBELL),
    DefaultExercise("Резинка на перекладине, на трицепс", "Band triceps pushdown from bar", setOf(TRICEPS), equipmentType = EquipmentType.BAND),
    DefaultExercise("Резинка на трицепс из-за спины", "Overhead band triceps extension", setOf(TRICEPS), equipmentType = EquipmentType.BAND),
    DefaultExercise("Тяга Ли Хейни", "Lee Haney row", setOf(REAR_DELTS, TRAPS, LATS)),
    DefaultExercise("Молотки с гантелями", "Dumbbell hammer curls", setOf(BICEPS, FOREARMS), equipmentType = EquipmentType.DUMBBELL),
    DefaultExercise("Молоточек с упором в бедро", "Concentration hammer curl", setOf(BICEPS, FOREARMS), equipmentType = EquipmentType.DUMBBELL),
    DefaultExercise("Подтягивания узким обратным хватом", "Close-grip chin-ups", setOf(LATS, BICEPS), useBodyWeight = true),
    DefaultExercise("Подтягивания прямым широким хватом", "Wide-grip pull-ups", setOf(LATS), useBodyWeight = true),
    DefaultExercise("Средняя дельта с эспандером", "Band lateral raise", setOf(SIDE_DELTS), equipmentType = EquipmentType.BAND),
    DefaultExercise("Пресс с роликом", "Ab wheel rollout", setOf(ABS), useBodyWeight = true),
    DefaultExercise("Планка", "Plank", setOf(ABS), trackReps = false, trackWeight = false, trackTime = true, useBodyWeight = true),
    DefaultExercise("Прыжки на скакалке", "Jump rope", setOf(CALVES), trackReps = false, trackWeight = false, trackTime = true),
)

/**
 * Мышцы для упражнений, созданных до появления схемы мышц: по точному названию.
 * Включает и старые варианты названий («из за спины» без дефиса).
 */
val MUSCLES_BY_NAME: Map<String, Set<Muscle>> =
    DEFAULT_EXERCISES.flatMap { listOf(it.nameRu to it.muscles, it.nameEn to it.muscles) }.toMap() +
        mapOf("Резинка на трицепс из за спины" to setOf(TRICEPS))

package com.sonnik.topower.data

import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.Muscle.ABS
import com.sonnik.topower.domain.Muscle.ADDUCTORS
import com.sonnik.topower.domain.Muscle.GLUTES
import com.sonnik.topower.domain.Muscle.HAMSTRINGS
import com.sonnik.topower.domain.Muscle.LOWER_BACK
import com.sonnik.topower.domain.Muscle.OBLIQUES
import com.sonnik.topower.domain.Muscle.QUADS
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
    /** Рабочий диапазон повторов для подсказки цели. */
    val reps: IntRange? = null,
) {
    fun toEntity(russian: Boolean) = ExerciseEntity(
        name = if (russian) nameRu else nameEn,
        trackReps = trackReps,
        trackWeight = trackWeight,
        trackTime = trackTime,
        useBodyWeight = useBodyWeight,
        equipmentType = equipmentType?.name,
        muscles = Muscle.format(muscles),
        repMin = reps?.first,
        repMax = reps?.last,
        technique = TECHNIQUE[nameRu]?.let { if (russian) it.first else it.second }.orEmpty(),
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

private val DB = EquipmentType.DUMBBELL
private val BAND = EquipmentType.BAND
private val VEST = EquipmentType.WEIGHT_VEST

/**
 * Набор 2: по три упражнения на каждую группу мышц (она в них основная) — с гантелями, резинами,
 * пружинным эспандером, утяжелительным жилетом, перекладиной и скамьёй. Добавляется к уже
 * существующим упражнениям, совпадающие по названию пропускаются.
 */
val EXERCISE_PACK_2 = listOf(
    // Грудь
    DefaultExercise("Жим гантелей лёжа", "Dumbbell bench press", setOf(CHEST, TRICEPS, FRONT_DELTS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Отжимания в жилете", "Weighted vest push-ups", setOf(CHEST, TRICEPS, FRONT_DELTS), useBodyWeight = true, equipmentType = VEST, reps = 10..25),
    DefaultExercise("Сведение рук с резиной перед грудью", "Band chest fly", setOf(CHEST), equipmentType = BAND, reps = 12..20),
    // Плечи
    DefaultExercise("Подъём гантелей перед собой", "Dumbbell front raise", setOf(FRONT_DELTS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Жим гантелей сидя", "Seated dumbbell shoulder press", setOf(FRONT_DELTS, SIDE_DELTS, TRICEPS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Жим резины вверх стоя", "Band overhead press", setOf(FRONT_DELTS, TRICEPS), equipmentType = BAND, reps = 10..20),
    DefaultExercise("Махи гантелями в стороны", "Dumbbell lateral raise", setOf(SIDE_DELTS), equipmentType = DB, reps = 12..20),
    DefaultExercise("Тяга гантелей к подбородку", "Dumbbell upright row", setOf(SIDE_DELTS, TRAPS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Жим Арнольда", "Arnold press", setOf(SIDE_DELTS, FRONT_DELTS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Разводка гантелей в наклоне", "Bent-over dumbbell reverse fly", setOf(REAR_DELTS), equipmentType = DB, reps = 12..20),
    DefaultExercise("Растяжка резины перед грудью", "Band pull-apart", setOf(REAR_DELTS, TRAPS), equipmentType = BAND, reps = 15..25),
    DefaultExercise("Растяжка пружинного эспандера", "Spring chest expander pull", setOf(REAR_DELTS, TRAPS), equipmentType = EquipmentType.EXPANDER, reps = 10..20),
    // Трапеции
    DefaultExercise("Шраги с гантелями", "Dumbbell shrugs", setOf(TRAPS), equipmentType = DB, reps = 12..20),
    DefaultExercise("Шраги с резиной", "Band shrugs", setOf(TRAPS), equipmentType = BAND, reps = 15..25),
    DefaultExercise("Тяга резины к лицу", "Band face pull", setOf(TRAPS, REAR_DELTS), equipmentType = BAND, reps = 15..20),
    // Спина
    DefaultExercise("Тяга гантели одной рукой в наклоне", "One-arm dumbbell row", setOf(LATS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Подтягивания в жилете", "Weighted vest pull-ups", setOf(LATS, BICEPS), useBodyWeight = true, equipmentType = VEST, reps = 5..10),
    DefaultExercise("Тяга резины к поясу сидя", "Seated band row", setOf(LATS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Супермен (лёжа на животе)", "Superman hold", setOf(LOWER_BACK, GLUTES), useBodyWeight = true, trackWeight = false, reps = 12..20),
    DefaultExercise("Наклоны с резиной (гуд-морнинг)", "Band good morning", setOf(LOWER_BACK, HAMSTRINGS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Румынская тяга с гантелями", "Dumbbell Romanian deadlift", setOf(HAMSTRINGS, GLUTES, LOWER_BACK), equipmentType = DB, reps = 8..12),
    // Руки
    DefaultExercise("Сгибания рук с гантелями", "Dumbbell biceps curl", setOf(BICEPS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Концентрированные сгибания", "Concentration curl", setOf(BICEPS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Сгибания рук с резиной", "Band biceps curl", setOf(BICEPS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Французский жим с гантелью лёжа", "Lying dumbbell triceps extension", setOf(TRICEPS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Разгибание руки с гантелью в наклоне", "Dumbbell triceps kickback", setOf(TRICEPS), equipmentType = DB, reps = 12..15),
    DefaultExercise("Обратные отжимания от скамьи", "Bench dips", setOf(TRICEPS), useBodyWeight = true, equipmentType = VEST, reps = 10..20),
    DefaultExercise("Сгибания запястий с гантелями", "Dumbbell wrist curl", setOf(FOREARMS), equipmentType = DB, reps = 15..25),
    DefaultExercise("Сгибания рук обратным хватом", "Reverse-grip dumbbell curl", setOf(FOREARMS, BICEPS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Вис на перекладине", "Dead hang", setOf(FOREARMS), trackReps = false, trackWeight = false, trackTime = true, useBodyWeight = true),
    // Пресс
    DefaultExercise("Подъёмы ног в висе", "Hanging leg raise", setOf(ABS), useBodyWeight = true, trackWeight = false, reps = 8..15),
    DefaultExercise("Скручивания с гантелью", "Weighted crunch", setOf(ABS), equipmentType = DB, reps = 15..25),
    DefaultExercise("Скручивания с резиной на коленях", "Kneeling band crunch", setOf(ABS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Боковая планка", "Side plank", setOf(OBLIQUES), trackReps = false, trackWeight = false, trackTime = true, useBodyWeight = true),
    DefaultExercise("Наклоны в сторону с гантелью", "Dumbbell side bend", setOf(OBLIQUES), equipmentType = DB, reps = 12..20),
    DefaultExercise("Дровосек с резиной", "Band woodchop", setOf(OBLIQUES), equipmentType = BAND, reps = 12..20),
    // Ноги
    DefaultExercise("Гоблет-присед с гантелью", "Dumbbell goblet squat", setOf(QUADS, GLUTES), equipmentType = DB, reps = 10..15),
    DefaultExercise("Выпады с гантелями", "Dumbbell lunges", setOf(QUADS, GLUTES), equipmentType = DB, reps = 8..12),
    DefaultExercise("Приседания в жилете", "Weighted vest squats", setOf(QUADS, GLUTES), useBodyWeight = true, equipmentType = VEST, reps = 15..30),
    DefaultExercise("Болгарские выпады с гантелями", "Dumbbell Bulgarian split squat", setOf(GLUTES, QUADS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Ягодичный мост с упором в скамью", "Dumbbell hip thrust", setOf(GLUTES, HAMSTRINGS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Отведение ноги назад с резиной", "Band glute kickback", setOf(GLUTES), equipmentType = BAND, reps = 15..25),
    DefaultExercise("Становая тяга на одной ноге с гантелью", "Single-leg dumbbell deadlift", setOf(HAMSTRINGS, GLUTES), equipmentType = DB, reps = 8..12),
    DefaultExercise("Сгибание ног с резиной лёжа", "Lying band leg curl", setOf(HAMSTRINGS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Приседания сумо с гантелью", "Dumbbell sumo squat", setOf(ADDUCTORS, QUADS, GLUTES), equipmentType = DB, reps = 10..15),
    DefaultExercise("Боковые выпады с гантелью", "Dumbbell lateral lunge", setOf(ADDUCTORS, QUADS, GLUTES), equipmentType = DB, reps = 8..12),
    DefaultExercise("Сведение ноги с резиной стоя", "Standing band hip adduction", setOf(ADDUCTORS), equipmentType = BAND, reps = 15..25),
    DefaultExercise("Подъёмы на носки с гантелями", "Dumbbell calf raise", setOf(CALVES), equipmentType = DB, reps = 15..25),
    DefaultExercise("Подъёмы на носок на одной ноге в жилете", "Single-leg vest calf raise", setOf(CALVES), useBodyWeight = true, equipmentType = VEST, reps = 12..20),
    DefaultExercise("Разгибание стопы с резиной сидя", "Seated band calf press", setOf(CALVES), equipmentType = BAND, reps = 15..25),
)

/**
 * Мышцы для упражнений, созданных до появления схемы мышц: по точному названию.
 * Включает и старые варианты названий («из за спины» без дефиса).
 */
val MUSCLES_BY_NAME: Map<String, Set<Muscle>> =
    DEFAULT_EXERCISES.flatMap { listOf(it.nameRu to it.muscles, it.nameEn to it.muscles) }.toMap() +
        mapOf("Резинка на трицепс из за спины" to setOf(TRICEPS))

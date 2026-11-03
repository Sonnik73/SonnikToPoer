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
import com.sonnik.topower.domain.Muscle.BRACHIALIS
import com.sonnik.topower.domain.Muscle.BRACHIORADIALIS
import com.sonnik.topower.domain.Muscle.INFRASPINATUS
import com.sonnik.topower.domain.Muscle.SERRATUS
import com.sonnik.topower.domain.Muscle.TERES_MAJOR
import com.sonnik.topower.domain.Muscle.TIBIALIS

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
    DefaultExercise("Отжимания на упорах", "Push-ups on handles", setOf(CHEST, TRICEPS, FRONT_DELTS, SERRATUS), useBodyWeight = true),
    DefaultExercise("Разводка гантелей на скамье", "Dumbbell bench fly", setOf(CHEST), equipmentType = EquipmentType.DUMBBELL),
    DefaultExercise("Резинка на перекладине, на трицепс", "Band triceps pushdown from bar", setOf(TRICEPS), equipmentType = EquipmentType.BAND),
    DefaultExercise("Резинка на трицепс из-за спины", "Overhead band triceps extension", setOf(TRICEPS), equipmentType = EquipmentType.BAND),
    DefaultExercise("Тяга Ли Хейни", "Lee Haney row", setOf(REAR_DELTS, TRAPS, LATS)),
    DefaultExercise("Молотки с гантелями", "Dumbbell hammer curls", setOf(BICEPS, BRACHIALIS, BRACHIORADIALIS), equipmentType = EquipmentType.DUMBBELL),
    DefaultExercise("Молоточек с упором в бедро", "Concentration hammer curl", setOf(BICEPS, BRACHIALIS, BRACHIORADIALIS), equipmentType = EquipmentType.DUMBBELL),
    DefaultExercise("Подтягивания узким обратным хватом", "Close-grip chin-ups", setOf(LATS, BICEPS, BRACHIALIS, TERES_MAJOR), useBodyWeight = true),
    DefaultExercise("Подтягивания прямым широким хватом", "Wide-grip pull-ups", setOf(LATS, TERES_MAJOR, BRACHIALIS), useBodyWeight = true),
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
    DefaultExercise("Отжимания в жилете", "Weighted vest push-ups", setOf(CHEST, TRICEPS, FRONT_DELTS, SERRATUS), useBodyWeight = true, equipmentType = VEST, reps = 10..25),
    DefaultExercise("Сведение рук с резиной перед грудью", "Band chest fly", setOf(CHEST), equipmentType = BAND, reps = 12..20),
    // Плечи
    DefaultExercise("Подъём гантелей перед собой", "Dumbbell front raise", setOf(FRONT_DELTS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Жим гантелей сидя", "Seated dumbbell shoulder press", setOf(FRONT_DELTS, SIDE_DELTS, TRICEPS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Жим резины вверх стоя", "Band overhead press", setOf(FRONT_DELTS, TRICEPS), equipmentType = BAND, reps = 10..20),
    DefaultExercise("Махи гантелями в стороны", "Dumbbell lateral raise", setOf(SIDE_DELTS), equipmentType = DB, reps = 12..20),
    DefaultExercise("Тяга гантелей к подбородку", "Dumbbell upright row", setOf(SIDE_DELTS, TRAPS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Жим Арнольда", "Arnold press", setOf(SIDE_DELTS, FRONT_DELTS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Разводка гантелей в наклоне", "Bent-over dumbbell reverse fly", setOf(REAR_DELTS, INFRASPINATUS), equipmentType = DB, reps = 12..20),
    DefaultExercise("Растяжка резины перед грудью", "Band pull-apart", setOf(REAR_DELTS, TRAPS, INFRASPINATUS), equipmentType = BAND, reps = 15..25),
    DefaultExercise("Растяжка пружинного эспандера", "Spring chest expander pull", setOf(REAR_DELTS, TRAPS, INFRASPINATUS), equipmentType = EquipmentType.EXPANDER, reps = 10..20),
    // Трапеции
    DefaultExercise("Шраги с гантелями", "Dumbbell shrugs", setOf(TRAPS), equipmentType = DB, reps = 12..20),
    DefaultExercise("Шраги с резиной", "Band shrugs", setOf(TRAPS), equipmentType = BAND, reps = 15..25),
    DefaultExercise("Тяга резины к лицу", "Band face pull", setOf(TRAPS, REAR_DELTS, INFRASPINATUS), equipmentType = BAND, reps = 15..20),
    // Спина
    DefaultExercise("Тяга гантели одной рукой в наклоне", "One-arm dumbbell row", setOf(LATS, TERES_MAJOR), equipmentType = DB, reps = 8..12),
    DefaultExercise("Подтягивания в жилете", "Weighted vest pull-ups", setOf(LATS, BICEPS, TERES_MAJOR, BRACHIALIS), useBodyWeight = true, equipmentType = VEST, reps = 5..10),
    DefaultExercise("Тяга резины к поясу сидя", "Seated band row", setOf(LATS, TERES_MAJOR), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Супермен (лёжа на животе)", "Superman hold", setOf(LOWER_BACK, GLUTES), useBodyWeight = true, trackWeight = false, reps = 12..20),
    DefaultExercise("Наклоны с резиной (гуд-морнинг)", "Band good morning", setOf(LOWER_BACK, HAMSTRINGS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Румынская тяга с гантелями", "Dumbbell Romanian deadlift", setOf(HAMSTRINGS, GLUTES, LOWER_BACK), equipmentType = DB, reps = 8..12),
    // Руки
    DefaultExercise("Сгибания рук с гантелями", "Dumbbell biceps curl", setOf(BICEPS, BRACHIALIS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Концентрированные сгибания", "Concentration curl", setOf(BICEPS, BRACHIALIS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Сгибания рук с резиной", "Band biceps curl", setOf(BICEPS, BRACHIALIS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Французский жим с гантелью лёжа", "Lying dumbbell triceps extension", setOf(TRICEPS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Разгибание руки с гантелью в наклоне", "Dumbbell triceps kickback", setOf(TRICEPS), equipmentType = DB, reps = 12..15),
    DefaultExercise("Обратные отжимания от скамьи", "Bench dips", setOf(TRICEPS), useBodyWeight = true, equipmentType = VEST, reps = 10..20),
    DefaultExercise("Сгибания запястий с гантелями", "Dumbbell wrist curl", setOf(FOREARMS), equipmentType = DB, reps = 15..25),
    DefaultExercise("Сгибания рук обратным хватом", "Reverse-grip dumbbell curl", setOf(BRACHIORADIALIS, BRACHIALIS, FOREARMS), equipmentType = DB, reps = 10..15),
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
 * Набор 4 (1.31): упражнения на турнике и на новые мышцы помельче — брахиалис, плечелучевую,
 * переднюю зубчатую, большую круглую, подостную и переднюю большеберцовую. Как и набор 2,
 * добавляется к существующим, совпадающие по названию пропускаются.
 */
val EXERCISE_PACK_4 = listOf(
    // Турник
    DefaultExercise("Подтягивания нейтральным хватом", "Neutral-grip pull-ups", setOf(LATS, BRACHIALIS, BICEPS), useBodyWeight = true, reps = 5..10),
    DefaultExercise("Австралийские подтягивания", "Inverted row", setOf(LATS, REAR_DELTS, BICEPS), useBodyWeight = true, reps = 8..15),
    DefaultExercise("Негативные подтягивания", "Negative pull-ups", setOf(LATS, BICEPS, TERES_MAJOR), useBodyWeight = true, reps = 3..6),
    DefaultExercise("Лопаточные подтягивания", "Scapular pull-ups", setOf(LATS, TRAPS, TERES_MAJOR), useBodyWeight = true, trackWeight = false, reps = 8..15),
    DefaultExercise("Подъём коленей в висе", "Hanging knee raise", setOf(ABS), useBodyWeight = true, trackWeight = false, reps = 10..20),
    DefaultExercise("Подъём ног к перекладине", "Toes to bar", setOf(ABS, SERRATUS), useBodyWeight = true, trackWeight = false, reps = 5..12),
    DefaultExercise("Косые подъёмы коленей в висе", "Hanging oblique knee raise", setOf(OBLIQUES, ABS), useBodyWeight = true, trackWeight = false, reps = 8..15),
    DefaultExercise("Пресс на перекладине с подъемом ног", "Hanging L-sit", setOf(ABS, FOREARMS, QUADS), trackReps = false, trackWeight = false, trackTime = true, useBodyWeight = true),
    // Из упражнений автора
    DefaultExercise("Кубинское вращение", "Cuban rotation", setOf(INFRASPINATUS, REAR_DELTS), equipmentType = DB, reps = 10..15),
    DefaultExercise("Бицепс на скамье Скотта", "Dumbbell preacher curl", setOf(BICEPS, BRACHIALIS), equipmentType = DB, reps = 8..12),
    // Брахиалис и плечелучевая
    DefaultExercise("Молотки поперёк корпуса", "Cross-body hammer curl", setOf(BRACHIALIS, BRACHIORADIALIS, BICEPS), equipmentType = DB, reps = 8..12),
    DefaultExercise("Молотки с резиной", "Band hammer curl", setOf(BRACHIALIS, BRACHIORADIALIS, BICEPS), equipmentType = BAND, reps = 12..20),
    // Передняя зубчатая
    DefaultExercise("Пуловер с гантелью лёжа", "Dumbbell pullover", setOf(LATS, SERRATUS, CHEST), equipmentType = DB, reps = 10..15),
    DefaultExercise("Отжимания с протракцией лопаток", "Scapular push-ups", setOf(SERRATUS), useBodyWeight = true, trackWeight = false, reps = 10..20),
    DefaultExercise("Удары с резиной вперёд", "Band serratus punch", setOf(SERRATUS, FRONT_DELTS), equipmentType = BAND, reps = 12..20),
    // Подостная (ротаторы)
    DefaultExercise("Наружная ротация плеча с резиной", "Band external rotation", setOf(INFRASPINATUS, REAR_DELTS), equipmentType = BAND, reps = 12..20),
    DefaultExercise("Наружная ротация с гантелью лёжа на боку", "Side-lying dumbbell external rotation", setOf(INFRASPINATUS), equipmentType = DB, reps = 12..20),
    // Передняя большеберцовая
    DefaultExercise("Подъёмы носков стоя у стены", "Wall tibialis raise", setOf(TIBIALIS), useBodyWeight = true, trackWeight = false, reps = 15..30),
    DefaultExercise("Тяга носка на себя с резиной", "Band dorsiflexion", setOf(TIBIALIS), equipmentType = BAND, reps = 15..25),
    DefaultExercise("Ходьба на пятках", "Heel walk", setOf(TIBIALIS, CALVES), trackReps = false, trackWeight = false, trackTime = true),
)

/**
 * Мышцы упражнений по умолчанию до 1.31 (русское название → мышцы). Нужны, чтобы при обновлении
 * поменять мышцы только тем упражнениям, которые пользователь не редактировал.
 */
private val MUSCLES_BEFORE_1_31: Map<String, Set<Muscle>> = mapOf(
    "Отжимания на упорах" to setOf(CHEST, TRICEPS, FRONT_DELTS),
    "Разводка гантелей на скамье" to setOf(CHEST),
    "Резинка на перекладине, на трицепс" to setOf(TRICEPS),
    "Резинка на трицепс из-за спины" to setOf(TRICEPS),
    "Тяга Ли Хейни" to setOf(REAR_DELTS, TRAPS, LATS),
    "Молотки с гантелями" to setOf(BICEPS, FOREARMS),
    "Молоточек с упором в бедро" to setOf(BICEPS, FOREARMS),
    "Подтягивания узким обратным хватом" to setOf(LATS, BICEPS),
    "Подтягивания прямым широким хватом" to setOf(LATS),
    "Средняя дельта с эспандером" to setOf(SIDE_DELTS),
    "Пресс с роликом" to setOf(ABS),
    "Планка" to setOf(ABS),
    "Прыжки на скакалке" to setOf(CALVES),
    "Жим гантелей лёжа" to setOf(CHEST, TRICEPS, FRONT_DELTS),
    "Отжимания в жилете" to setOf(CHEST, TRICEPS, FRONT_DELTS),
    "Сведение рук с резиной перед грудью" to setOf(CHEST),
    "Подъём гантелей перед собой" to setOf(FRONT_DELTS),
    "Жим гантелей сидя" to setOf(FRONT_DELTS, SIDE_DELTS, TRICEPS),
    "Жим резины вверх стоя" to setOf(FRONT_DELTS, TRICEPS),
    "Махи гантелями в стороны" to setOf(SIDE_DELTS),
    "Тяга гантелей к подбородку" to setOf(SIDE_DELTS, TRAPS),
    "Жим Арнольда" to setOf(SIDE_DELTS, FRONT_DELTS),
    "Разводка гантелей в наклоне" to setOf(REAR_DELTS),
    "Растяжка резины перед грудью" to setOf(REAR_DELTS, TRAPS),
    "Растяжка пружинного эспандера" to setOf(REAR_DELTS, TRAPS),
    "Шраги с гантелями" to setOf(TRAPS),
    "Шраги с резиной" to setOf(TRAPS),
    "Тяга резины к лицу" to setOf(TRAPS, REAR_DELTS),
    "Тяга гантели одной рукой в наклоне" to setOf(LATS),
    "Подтягивания в жилете" to setOf(LATS, BICEPS),
    "Тяга резины к поясу сидя" to setOf(LATS),
    "Супермен (лёжа на животе)" to setOf(LOWER_BACK, GLUTES),
    "Наклоны с резиной (гуд-морнинг)" to setOf(LOWER_BACK, HAMSTRINGS),
    "Румынская тяга с гантелями" to setOf(HAMSTRINGS, GLUTES, LOWER_BACK),
    "Сгибания рук с гантелями" to setOf(BICEPS),
    "Концентрированные сгибания" to setOf(BICEPS),
    "Сгибания рук с резиной" to setOf(BICEPS),
    "Французский жим с гантелью лёжа" to setOf(TRICEPS),
    "Разгибание руки с гантелью в наклоне" to setOf(TRICEPS),
    "Обратные отжимания от скамьи" to setOf(TRICEPS),
    "Сгибания запястий с гантелями" to setOf(FOREARMS),
    "Сгибания рук обратным хватом" to setOf(FOREARMS, BICEPS),
    "Вис на перекладине" to setOf(FOREARMS),
    "Подъёмы ног в висе" to setOf(ABS),
    "Скручивания с гантелью" to setOf(ABS),
    "Скручивания с резиной на коленях" to setOf(ABS),
    "Боковая планка" to setOf(OBLIQUES),
    "Наклоны в сторону с гантелью" to setOf(OBLIQUES),
    "Дровосек с резиной" to setOf(OBLIQUES),
    "Гоблет-присед с гантелью" to setOf(QUADS, GLUTES),
    "Выпады с гантелями" to setOf(QUADS, GLUTES),
    "Приседания в жилете" to setOf(QUADS, GLUTES),
    "Болгарские выпады с гантелями" to setOf(GLUTES, QUADS),
    "Ягодичный мост с упором в скамью" to setOf(GLUTES, HAMSTRINGS),
    "Отведение ноги назад с резиной" to setOf(GLUTES),
    "Становая тяга на одной ноге с гантелью" to setOf(HAMSTRINGS, GLUTES),
    "Сгибание ног с резиной лёжа" to setOf(HAMSTRINGS),
    "Приседания сумо с гантелью" to setOf(ADDUCTORS, QUADS, GLUTES),
    "Боковые выпады с гантелью" to setOf(ADDUCTORS, QUADS, GLUTES),
    "Сведение ноги с резиной стоя" to setOf(ADDUCTORS),
    "Подъёмы на носки с гантелями" to setOf(CALVES),
    "Подъёмы на носок на одной ноге в жилете" to setOf(CALVES),
    "Разгибание стопы с резиной сидя" to setOf(CALVES),
)

/** Упражнения по умолчанию, у которых в 1.31 поменялись мышцы: (названия ru/en, было, стало). */
val MUSCLE_UPGRADES_1_31: List<Triple<List<String>, Set<Muscle>, Set<Muscle>>> by lazy {
    (DEFAULT_EXERCISES + EXERCISE_PACK_2).mapNotNull { e ->
        val before = MUSCLES_BEFORE_1_31[e.nameRu] ?: return@mapNotNull null
        if (before == e.muscles) null else Triple(listOf(e.nameRu, e.nameEn), before, e.muscles)
    }
}

/**
 * Мышцы для упражнений, созданных до появления схемы мышц: по точному названию.
 * Включает и старые варианты названий («из за спины» без дефиса).
 */
val MUSCLES_BY_NAME: Map<String, Set<Muscle>> =
    DEFAULT_EXERCISES.flatMap { listOf(it.nameRu to it.muscles, it.nameEn to it.muscles) }.toMap() +
        mapOf("Резинка на трицепс из за спины" to setOf(TRICEPS))

package com.sonnik.topower.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()

/**
 * Упражнение из справочника. Набор измеряемых параметров задаётся флагами track*,
 * useBodyWeight означает, что рабочий вес = вес тела + дополнительный вес.
 * equipmentType — тип инвентаря ([EquipmentType]), из которого выбирается снаряд в подходе;
 * bandAssist — резина помогает (её сопротивление вычитается из веса тела).
 */
@Serializable
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val category: String = "",
    val note: String = "",
    val trackReps: Boolean = true,
    val trackWeight: Boolean = false,
    val trackTime: Boolean = false,
    val trackDistance: Boolean = false,
    val useBodyWeight: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long = now(),
    val updatedAt: Long = now(),
    val equipmentType: String? = null,
    @ColumnInfo(defaultValue = "0")
    val bandAssist: Boolean = false,
    /** Своё время отдыха между подходами, сек; null — общее из настроек. */
    val restSec: Int? = null,
)

@Serializable
@Entity(tableName = "workouts", indices = [Index("startedAt")])
data class WorkoutEntity(
    @PrimaryKey val id: String = newId(),
    val startedAt: Long = now(),
    val finishedAt: Long? = null,
    val bodyWeight: Double? = null,
    val note: String = "",
    val updatedAt: Long = now(),
    /** Данные со смарт-часов, вносятся вручную после тренировки. */
    val maxHeartRate: Int? = null,
    val avgHeartRate: Int? = null,
    val watchDurationSec: Int? = null,
    val calories: Int? = null,
) {
    val hasWatchData: Boolean
        get() = maxHeartRate != null || avgHeartRate != null || watchDurationSec != null || calories != null
}

@Serializable
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String = newId(),
    val workoutId: String,
    val exerciseId: String,
    val position: Int,
    val plannedSets: Int? = null,
    val updatedAt: Long = now(),
)

@Serializable
@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class SetEntity(
    @PrimaryKey val id: String = newId(),
    val workoutExerciseId: String,
    val position: Int,
    val reps: Int? = null,
    val weight: Double? = null,
    val durationSec: Int? = null,
    val distanceKm: Double? = null,
    val isWarmup: Boolean = false,
    val createdAt: Long = now(),
    val updatedAt: Long = now(),
    /** Выбранный снаряд (гантель, резина…) и вторая резина, если их две. */
    val equipmentId: String? = null,
    val equipment2Id: String? = null,
    /** Пара одинаковых снарядов: weight — вес одного, записывается как «2 × 10 кг». */
    @ColumnInfo(defaultValue = "0")
    val pair: Boolean = false,
    /** Для резины: суммарное минимальное сопротивление; в weight — пиковое (максимальное). */
    val bandMin: Double? = null,
)

@Serializable
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val note: String = "",
    val updatedAt: Long = now(),
)

@Serializable
@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExerciseEntity(
    @PrimaryKey val id: String = newId(),
    val templateId: String,
    val exerciseId: String,
    val position: Int,
    val plannedSets: Int = 3,
    val updatedAt: Long = now(),
)

enum class EquipmentType {
    DUMBBELL, KETTLEBELL, BARBELL, BAND, EXPANDER, WEIGHT_VEST, OTHER;

    val isBand: Boolean get() = this == BAND
    /** Снаряды, которые обычно берут парой. */
    val canPair: Boolean get() = this == DUMBBELL || this == KETTLEBELL

    companion object {
        fun of(name: String?): EquipmentType? = entries.firstOrNull { it.name == name }
    }
}

/** Предмет инвентаря: у обычного снаряда — вес, у резины — сопротивление «от–до» и цвет. */
@Serializable
@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val id: String = newId(),
    val name: String = "",
    val type: String,
    val weight: Double? = null,
    val minResistance: Double? = null,
    val maxResistance: Double? = null,
    val color: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long = now(),
    val updatedAt: Long = now(),
) {
    val equipmentType: EquipmentType get() = EquipmentType.of(type) ?: EquipmentType.OTHER

    /** Значение для сортировки: вес или пиковое сопротивление. */
    val load: Double get() = weight ?: maxResistance ?: 0.0
}

// ---- Результаты запросов ----

data class WorkoutExerciseFull(
    @Embedded val item: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<SetEntity>,
)

data class TemplateExerciseFull(
    @Embedded val item: TemplateExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class WorkoutSummary(
    @Embedded val workout: WorkoutEntity,
    val exerciseCount: Int,
    val setCount: Int,
    val exerciseNames: String?,
)

data class TemplateSummary(
    @Embedded val template: TemplateEntity,
    val exerciseCount: Int,
    val exerciseNames: String?,
)

/** Подход вместе с данными тренировки и упражнения — основа для статистики. */
data class SetWithContext(
    @Embedded val set: SetEntity,
    val workoutId: String,
    val workoutDate: Long,
    val bodyWeight: Double?,
    val exerciseId: String,
    val useBodyWeight: Boolean,
)

data class BodyWeightPoint(
    val startedAt: Long,
    val bodyWeight: Double,
)

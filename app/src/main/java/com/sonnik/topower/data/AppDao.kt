package com.sonnik.topower.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ---- Упражнения ----

    @Query("SELECT * FROM exercises WHERE archived = 0 OR :includeArchived ORDER BY name COLLATE NOCASE")
    fun observeExercises(includeArchived: Boolean): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeExercise(id: String): Flow<ExerciseEntity?>

    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    @Query(
        """SELECT (SELECT COUNT(*) FROM workout_exercises WHERE exerciseId = :id) +
                  (SELECT COUNT(*) FROM template_exercises WHERE exerciseId = :id)"""
    )
    suspend fun exerciseUsageCount(id: String): Int

    @Query("SELECT DISTINCT category FROM exercises WHERE category != '' ORDER BY category COLLATE NOCASE")
    fun observeCategories(): Flow<List<String>>

    // ---- Инвентарь ----

    @Query("SELECT * FROM equipment WHERE archived = 0 OR :includeArchived ORDER BY type, COALESCE(weight, maxResistance, 0), name COLLATE NOCASE")
    fun observeEquipment(includeArchived: Boolean): Flow<List<EquipmentEntity>>

    @Upsert
    suspend fun upsertEquipment(item: EquipmentEntity)

    @Query("DELETE FROM equipment WHERE id = :id")
    suspend fun deleteEquipment(id: String)

    @Query("SELECT COUNT(*) FROM sets WHERE equipmentId = :id OR equipment2Id = :id")
    suspend fun equipmentUsageCount(id: String): Int

    // ---- Тренировки ----

    @Query(
        """SELECT w.*,
              (SELECT COUNT(*) FROM workout_exercises we WHERE we.workoutId = w.id) AS exerciseCount,
              (SELECT COUNT(*) FROM sets s JOIN workout_exercises we ON s.workoutExerciseId = we.id
                 WHERE we.workoutId = w.id) AS setCount,
              (SELECT GROUP_CONCAT(e.name, ', ') FROM workout_exercises we
                 JOIN exercises e ON e.id = we.exerciseId WHERE we.workoutId = w.id) AS exerciseNames
           FROM workouts w ORDER BY w.startedAt DESC"""
    )
    fun observeWorkoutSummaries(): Flow<List<WorkoutSummary>>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC")
    fun observeActiveWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: String): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: String): WorkoutEntity?

    @Upsert
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)

    @Query("SELECT bodyWeight FROM workouts WHERE bodyWeight IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun lastBodyWeight(): Double?

    @Query("SELECT startedAt, bodyWeight FROM workouts WHERE bodyWeight IS NOT NULL ORDER BY startedAt")
    fun observeBodyWeights(): Flow<List<BodyWeightPoint>>

    @Transaction
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExerciseFull>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getWorkoutExercises(workoutId: String): List<WorkoutExerciseEntity>

    @Upsert
    suspend fun upsertWorkoutExercise(item: WorkoutExerciseEntity)

    @Upsert
    suspend fun upsertWorkoutExercises(items: List<WorkoutExerciseEntity>)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: String)

    // ---- Подходы ----

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun nextSetPosition(workoutExerciseId: String): Int

    @Upsert
    suspend fun upsertSet(set: SetEntity)

    @Delete
    suspend fun deleteSet(set: SetEntity)

    @Query(
        """SELECT s.*, w.id AS workoutId, w.startedAt AS workoutDate, w.bodyWeight AS bodyWeight,
                  e.id AS exerciseId, e.useBodyWeight AS useBodyWeight
           FROM sets s
           JOIN workout_exercises we ON s.workoutExerciseId = we.id
           JOIN workouts w ON w.id = we.workoutId
           JOIN exercises e ON e.id = we.exerciseId
           WHERE e.id = :exerciseId
           ORDER BY w.startedAt, we.position, s.position"""
    )
    fun observeSetsForExercise(exerciseId: String): Flow<List<SetWithContext>>

    @Query(
        """SELECT s.*, w.id AS workoutId, w.startedAt AS workoutDate, w.bodyWeight AS bodyWeight,
                  e.id AS exerciseId, e.useBodyWeight AS useBodyWeight
           FROM sets s
           JOIN workout_exercises we ON s.workoutExerciseId = we.id
           JOIN workouts w ON w.id = we.workoutId
           JOIN exercises e ON e.id = we.exerciseId
           WHERE e.id = :exerciseId
           ORDER BY w.startedAt, we.position, s.position"""
    )
    suspend fun setsForExercise(exerciseId: String): List<SetWithContext>

    @Query("UPDATE exercises SET restSec = :restSec, updatedAt = :now WHERE id = :id")
    suspend fun setExerciseRest(id: String, restSec: Int?, now: Long)

    @Query(
        """SELECT s.*, w.id AS workoutId, w.startedAt AS workoutDate, w.bodyWeight AS bodyWeight,
                  e.id AS exerciseId, e.useBodyWeight AS useBodyWeight
           FROM sets s
           JOIN workout_exercises we ON s.workoutExerciseId = we.id
           JOIN workouts w ON w.id = we.workoutId
           JOIN exercises e ON e.id = we.exerciseId
           WHERE e.id = :exerciseId AND w.id != :excludeWorkoutId AND w.startedAt < :before
           ORDER BY w.startedAt DESC, we.position, s.position"""
    )
    suspend fun previousSetsForExercise(exerciseId: String, excludeWorkoutId: String, before: Long): List<SetWithContext>

    @Query(
        """SELECT s.*, w.id AS workoutId, w.startedAt AS workoutDate, w.bodyWeight AS bodyWeight,
                  e.id AS exerciseId, e.useBodyWeight AS useBodyWeight
           FROM sets s
           JOIN workout_exercises we ON s.workoutExerciseId = we.id
           JOIN workouts w ON w.id = we.workoutId
           JOIN exercises e ON e.id = we.exerciseId
           ORDER BY w.startedAt"""
    )
    fun observeAllSets(): Flow<List<SetWithContext>>

    @Query("SELECT startedAt FROM workouts ORDER BY startedAt")
    fun observeWorkoutDates(): Flow<List<Long>>

    @Query("SELECT * FROM workouts ORDER BY startedAt")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT DISTINCT we.exerciseId FROM workout_exercises we JOIN sets s ON s.workoutExerciseId = we.id")
    fun observeExerciseIdsWithSets(): Flow<List<String>>

    // ---- Шаблоны ----

    @Query(
        """SELECT t.*,
              (SELECT COUNT(*) FROM template_exercises te WHERE te.templateId = t.id) AS exerciseCount,
              (SELECT GROUP_CONCAT(e.name, ', ') FROM template_exercises te
                 JOIN exercises e ON e.id = te.exerciseId WHERE te.templateId = t.id) AS exerciseNames
           FROM templates t ORDER BY t.name COLLATE NOCASE"""
    )
    fun observeTemplateSummaries(): Flow<List<TemplateSummary>>

    @Query("SELECT * FROM templates WHERE id = :id")
    fun observeTemplate(id: String): Flow<TemplateEntity?>

    @Upsert
    suspend fun upsertTemplate(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Transaction
    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY position")
    fun observeTemplateExercises(templateId: String): Flow<List<TemplateExerciseFull>>

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY position")
    suspend fun getTemplateExercises(templateId: String): List<TemplateExerciseEntity>

    @Upsert
    suspend fun upsertTemplateExercise(item: TemplateExerciseEntity)

    @Upsert
    suspend fun upsertTemplateExercises(items: List<TemplateExerciseEntity>)

    @Query("DELETE FROM template_exercises WHERE id = :id")
    suspend fun deleteTemplateExercise(id: String)

    // ---- Резервная копия ----

    @Query("SELECT * FROM exercises") suspend fun allExercises(): List<ExerciseEntity>
    @Query("SELECT * FROM workouts") suspend fun allWorkouts(): List<WorkoutEntity>
    @Query("SELECT * FROM workout_exercises") suspend fun allWorkoutExercises(): List<WorkoutExerciseEntity>
    @Query("SELECT * FROM sets") suspend fun allSets(): List<SetEntity>
    @Query("SELECT * FROM templates") suspend fun allTemplates(): List<TemplateEntity>
    @Query("SELECT * FROM template_exercises") suspend fun allTemplateExercises(): List<TemplateExerciseEntity>
    @Query("SELECT * FROM equipment") suspend fun allEquipment(): List<EquipmentEntity>

    @Insert suspend fun insertExercises(items: List<ExerciseEntity>)
    @Insert suspend fun insertWorkouts(items: List<WorkoutEntity>)
    @Insert suspend fun insertWorkoutExercises(items: List<WorkoutExerciseEntity>)
    @Insert suspend fun insertSets(items: List<SetEntity>)
    @Insert suspend fun insertTemplates(items: List<TemplateEntity>)
    @Insert suspend fun insertTemplateExercises(items: List<TemplateExerciseEntity>)
    @Insert suspend fun insertEquipment(items: List<EquipmentEntity>)

    @Query("DELETE FROM sets") suspend fun clearSets()
    @Query("DELETE FROM workout_exercises") suspend fun clearWorkoutExercises()
    @Query("DELETE FROM workouts") suspend fun clearWorkouts()
    @Query("DELETE FROM template_exercises") suspend fun clearTemplateExercises()
    @Query("DELETE FROM templates") suspend fun clearTemplates()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM equipment") suspend fun clearEquipment()
}

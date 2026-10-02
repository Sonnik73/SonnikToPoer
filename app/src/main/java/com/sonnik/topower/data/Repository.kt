package com.sonnik.topower.data

import androidx.room.withTransaction
import com.sonnik.topower.domain.ExerciseOption
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleWork
import com.sonnik.topower.domain.computeMuscleStatus
import com.sonnik.topower.domain.generateWorkout
import com.sonnik.topower.domain.restedMuscles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Единая точка доступа к данным. Короткие операции записи запускаются в [scope] приложения,
 * чтобы не прерываться при уходе с экрана.
 */
class Repository(
    private val db: AppDatabase,
    private val scope: CoroutineScope,
) {
    val dao: AppDao = db.dao()
    private val seedLock = Mutex()

    fun launch(block: suspend Repository.() -> Unit): Job = scope.launch { block() }

    // ---- Упражнения ----

    suspend fun saveExercise(exercise: ExerciseEntity) {
        dao.upsertExercise(exercise.copy(name = exercise.name.trim(), updatedAt = now()))
    }

    suspend fun isExerciseUsed(id: String): Boolean = dao.exerciseUsageCount(id) > 0

    /** Удаляет упражнение, если оно нигде не использовалось, иначе отправляет в архив. */
    suspend fun deleteOrArchiveExercise(exercise: ExerciseEntity): Boolean {
        return if (isExerciseUsed(exercise.id)) {
            dao.upsertExercise(exercise.copy(archived = true, updatedAt = now()))
            false
        } else {
            dao.deleteExercise(exercise.id)
            true
        }
    }

    /** Запоминает время отдыха, выставленное для упражнения во время тренировки. */
    suspend fun setExerciseRest(exerciseId: String, restSec: Int?) = dao.setExerciseRest(exerciseId, restSec, now())

    suspend fun setRecoveryHours(workoutExerciseId: String, hours: Int?) =
        dao.setRecoveryHours(workoutExerciseId, hours, now())

    /** При первом запуске добавляет упражнения по умолчанию (если упражнений ещё нет). */
    suspend fun seedDefaultsIfNeeded(settings: AppSettings, russian: Boolean) = seedLock.withLock {
        if (!settings.defaultsSeeded) {
            if (dao.exerciseCount() == 0) dao.insertExercises(DEFAULT_EXERCISES.map { it.toEntity(russian) })
            settings.defaultsSeeded = true
        }
        // Набор 2 добавляется и новым, и уже установленным приложениям — кроме совпадающих по названию.
        if (settings.exercisePack < 2) {
            val existing = dao.allExercises().map { it.name.trim().lowercase() }.toSet()
            val added = EXERCISE_PACK_2
                .filter { it.nameRu.lowercase() !in existing && it.nameEn.lowercase() !in existing }
                .map { it.toEntity(russian) }
            if (added.isNotEmpty()) dao.insertExercises(added)
            settings.exercisePack = 2
        }
        // Техника выполнения: проставляем упражнениям, совпадающим по названию, если она ещё пустая.
        if (settings.exercisePack < 3) {
            val updated = dao.allExercises().filter { it.technique.isBlank() }.mapNotNull { e ->
                techniqueFor(e.name, russian)?.let { e.copy(technique = it) }
            }
            updated.forEach { dao.upsertExercise(it) }
            settings.exercisePack = 3
        }
    }

    // ---- Инвентарь ----

    suspend fun saveEquipment(item: EquipmentEntity) {
        dao.upsertEquipment(item.copy(name = item.name.trim(), updatedAt = now()))
    }

    /** Удаляет предмет, если он не встречается в подходах, иначе отправляет в архив. */
    suspend fun deleteOrArchiveEquipment(item: EquipmentEntity): Boolean {
        return if (dao.equipmentUsageCount(item.id) > 0) {
            dao.upsertEquipment(item.copy(archived = true, updatedAt = now()))
            false
        } else {
            dao.deleteEquipment(item.id)
            true
        }
    }

    // ---- Тренировки ----

    suspend fun startWorkout(templateId: String?): String = db.withTransaction {
        val workout = WorkoutEntity(bodyWeight = dao.lastBodyWeight())
        dao.upsertWorkout(workout)
        if (templateId != null) {
            val items = dao.getTemplateExercises(templateId).mapIndexed { index, te ->
                WorkoutExerciseEntity(
                    workoutId = workout.id,
                    exerciseId = te.exerciseId,
                    position = index,
                    plannedSets = te.plannedSets,
                )
            }
            dao.upsertWorkoutExercises(items)
        }
        workout.id
    }

    suspend fun updateWorkout(workout: WorkoutEntity) {
        dao.upsertWorkout(workout.copy(updatedAt = now()))
    }

    suspend fun finishWorkout(workout: WorkoutEntity) {
        dao.upsertWorkout(workout.copy(finishedAt = now(), updatedAt = now()))
    }

    suspend fun resumeWorkout(workout: WorkoutEntity) {
        dao.upsertWorkout(workout.copy(finishedAt = null, updatedAt = now()))
    }

    suspend fun deleteWorkout(id: String) = dao.deleteWorkout(id)

    suspend fun addExercisesToWorkout(workoutId: String, exerciseIds: List<String>) = db.withTransaction {
        val start = dao.getWorkoutExercises(workoutId).size
        dao.upsertWorkoutExercises(
            exerciseIds.mapIndexed { i, exId ->
                WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exId, position = start + i)
            }
        )
    }

    suspend fun removeWorkoutExercise(workoutId: String, itemId: String) = db.withTransaction {
        dao.deleteWorkoutExercise(itemId)
        val rest = dao.getWorkoutExercises(workoutId)
        dao.upsertWorkoutExercises(rest.mapIndexed { i, it -> it.copy(position = i, updatedAt = now()) })
    }

    suspend fun moveWorkoutExercise(workoutId: String, itemId: String, delta: Int) = db.withTransaction {
        val list = dao.getWorkoutExercises(workoutId).toMutableList()
        val from = list.indexOfFirst { it.id == itemId }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@withTransaction
        list.add(to, list.removeAt(from))
        dao.upsertWorkoutExercises(list.mapIndexed { i, it -> it.copy(position = i, updatedAt = now()) })
    }

    suspend fun saveSet(set: SetEntity) {
        val position = if (set.position < 0) dao.nextSetPosition(set.workoutExerciseId) else set.position
        dao.upsertSet(set.copy(position = position, updatedAt = now()))
    }

    suspend fun deleteSet(set: SetEntity) = dao.deleteSet(set)

    /** Подходы последней прошлой тренировки с этим упражнением (для подсказки и автозаполнения). */
    suspend fun previousSets(exerciseId: String, workout: WorkoutEntity): List<SetEntity> {
        val rows = dao.previousSetsForExercise(exerciseId, workout.id, workout.startedAt)
        val lastWorkoutId = rows.firstOrNull()?.workoutId ?: return emptyList()
        return rows.filter { it.workoutId == lastWorkoutId }.map { it.set }
    }

    // ---- Шаблоны ----

    suspend fun createTemplate(name: String): String {
        val template = TemplateEntity(name = name.trim())
        dao.upsertTemplate(template)
        return template.id
    }

    suspend fun saveTemplate(template: TemplateEntity) {
        dao.upsertTemplate(template.copy(name = template.name.trim(), updatedAt = now()))
    }

    suspend fun deleteTemplate(id: String) = dao.deleteTemplate(id)

    /**
     * Шаблон на самые отдохнувшие мышцы (см. [generateWorkout]); [name] получает нагруженные мышцы
     * от самых отдохнувших. null — подходящих упражнений нет (всё восстанавливается или мышцы не заданы).
     */
    suspend fun generateTemplate(now: Long, name: (List<Muscle>) -> String): String? {
        val statuses = computeMuscleStatus(
            dao.muscleWork().map { MuscleWork(it.workoutId, it.time, Muscle.parse(it.muscles), it.recoveryHours) }
        )
        val usage = dao.exerciseUsage().associate { it.exerciseId to it.count }
        val options = dao.allExercises().filter { !it.archived }
            .map { ExerciseOption(it.id, Muscle.parse(it.muscles), usage[it.id] ?: 0) }
        val picked = generateWorkout(options, statuses, now)
        if (picked.isEmpty()) return null
        val muscles = restedMuscles(statuses, now).filter { m -> picked.any { m in it.muscles } }
        val id = createTemplate(name(muscles))
        addExercisesToTemplate(id, picked.map { it.id })
        return id
    }

    suspend fun addExercisesToTemplate(templateId: String, exerciseIds: List<String>) = db.withTransaction {
        val start = dao.getTemplateExercises(templateId).size
        dao.upsertTemplateExercises(
            exerciseIds.mapIndexed { i, exId ->
                TemplateExerciseEntity(templateId = templateId, exerciseId = exId, position = start + i)
            }
        )
    }

    suspend fun updateTemplateExercise(item: TemplateExerciseEntity) {
        dao.upsertTemplateExercise(item.copy(updatedAt = now()))
    }

    suspend fun removeTemplateExercise(templateId: String, itemId: String) = db.withTransaction {
        dao.deleteTemplateExercise(itemId)
        val rest = dao.getTemplateExercises(templateId)
        dao.upsertTemplateExercises(rest.mapIndexed { i, it -> it.copy(position = i, updatedAt = now()) })
    }

    suspend fun moveTemplateExercise(templateId: String, itemId: String, delta: Int) = db.withTransaction {
        val list = dao.getTemplateExercises(templateId).toMutableList()
        val from = list.indexOfFirst { it.id == itemId }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@withTransaction
        list.add(to, list.removeAt(from))
        dao.upsertTemplateExercises(list.mapIndexed { i, it -> it.copy(position = i, updatedAt = now()) })
    }

    /** Создаёт шаблон из состава тренировки; плановое число подходов = фактическому. */
    suspend fun saveWorkoutAsTemplate(workoutId: String, name: String): String = db.withTransaction {
        val template = TemplateEntity(name = name.trim())
        dao.upsertTemplate(template)
        val items = dao.getWorkoutExercises(workoutId).mapIndexed { i, we ->
            TemplateExerciseEntity(
                templateId = template.id,
                exerciseId = we.exerciseId,
                position = i,
                plannedSets = maxOf(dao.nextSetPosition(we.id), we.plannedSets ?: 0, 1),
            )
        }
        dao.upsertTemplateExercises(items)
        template.id
    }

    // ---- Резервная копия ----

    /** Все подходы в CSV, см. [CsvExport]. */
    suspend fun exportCsv(
        header: List<String>,
        decimalComma: Boolean,
        warmupMark: String,
        yesMark: String,
        muscleNames: (String) -> String,
    ): String = db.withTransaction {
        CsvExport.build(
            header = header,
            exercises = dao.allExercises(),
            workouts = dao.allWorkouts(),
            workoutExercises = dao.allWorkoutExercises(),
            sets = dao.allSets(),
            equipment = dao.allEquipment(),
            zone = java.time.ZoneId.systemDefault(),
            decimalComma = decimalComma,
            warmupMark = warmupMark,
            yesMark = yesMark,
            muscleNames = muscleNames,
        )
    }

    suspend fun exportBackup(): String = db.withTransaction {
        BackupCodec.encode(
            BackupFile(
                exercises = dao.allExercises(),
                workouts = dao.allWorkouts(),
                workoutExercises = dao.allWorkoutExercises(),
                sets = dao.allSets(),
                templates = dao.allTemplates(),
                templateExercises = dao.allTemplateExercises(),
                equipment = dao.allEquipment(),
            )
        )
    }

    /** Полностью заменяет данные содержимым резервной копии. */
    suspend fun importBackup(text: String) {
        val backup = BackupCodec.decode(text)
        db.withTransaction {
            dao.clearSets()
            dao.clearWorkoutExercises()
            dao.clearWorkouts()
            dao.clearTemplateExercises()
            dao.clearTemplates()
            dao.clearExercises()
            dao.clearEquipment()
            dao.insertEquipment(backup.equipment)
            dao.insertExercises(backup.exercises)
            dao.insertWorkouts(backup.workouts)
            dao.insertWorkoutExercises(backup.workoutExercises)
            dao.insertSets(backup.sets)
            dao.insertTemplates(backup.templates)
            dao.insertTemplateExercises(backup.templateExercises)
        }
    }
}

package com.sonnik.topower.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WorkoutDataTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After fun tearDown() = db.close()

    /** Каждое поле пишется отдельно: заметка, вес, часы и завершение не затирают друг друга. */
    @Test fun workoutFieldsDoNotOverwriteEachOther() = runBlocking {
        val id = repo.startWorkout(null)
        repo.setWorkoutBodyWeight(id, 80.5)
        repo.setWorkoutNote(id, "Хорошо пошло")
        repo.setWorkoutWatch(id, maxHr = 160, avgHr = 120, durationSec = 3600, calories = 500)
        repo.finishWorkout(id)

        val w = db.dao().getWorkout(id)!!
        assertEquals(80.5, w.bodyWeight!!, 1e-9)
        assertEquals("Хорошо пошло", w.note)
        assertEquals(160, w.maxHeartRate)
        assertEquals(120, w.avgHeartRate)
        assertEquals(3600, w.watchDurationSec)
        assertEquals(500, w.calories)
        assertNotNull(w.finishedAt)

        repo.resumeWorkout(id)
        val resumed = db.dao().getWorkout(id)!!
        assertNull(resumed.finishedAt)
        assertEquals("Хорошо пошло", resumed.note)
    }

    /** Завершение — временем последнего подхода: забытая или открытая заново тренировка не «закончена только что». */
    @Test fun finishUsesLastSetTime() = runBlocking {
        val exercise = ExerciseEntity(name = "Жим")
        db.dao().insertExercises(listOf(exercise))
        val week = 7 * 86_400_000L
        val start = System.currentTimeMillis() - week
        val old = WorkoutEntity(startedAt = start)
        db.dao().upsertWorkout(old)
        repo.addExercisesToWorkout(old.id, listOf(exercise.id))
        val item = db.dao().getWorkoutExercises(old.id).single()
        db.dao().upsertSet(SetEntity(workoutExerciseId = item.id, position = 0, reps = 8, createdAt = start + 600_000))
        db.dao().upsertSet(SetEntity(workoutExerciseId = item.id, position = 1, reps = 8, createdAt = start + 1_800_000))

        repo.finishWorkout(old.id)
        assertEquals(start + 1_800_000, db.dao().getWorkout(old.id)!!.finishedAt)
        // «Продолжить» и снова «Завершить» без новых подходов — время то же.
        repo.resumeWorkout(old.id)
        repo.finishWorkout(old.id)
        assertEquals(start + 1_800_000, db.dao().getWorkout(old.id)!!.finishedAt)

        // Без подходов — текущее время.
        val empty = repo.startWorkout(null)
        val before = System.currentTimeMillis()
        repo.finishWorkout(empty)
        assertTrue(db.dao().getWorkout(empty)!!.finishedAt!! >= before)
    }

    /** В истории упражнения перечислены в порядке тренировки, а не добавления. */
    @Test fun historyNamesFollowWorkoutOrder() = runBlocking {
        val a = ExerciseEntity(name = "Жим")
        val b = ExerciseEntity(name = "Тяга")
        db.dao().insertExercises(listOf(a, b))
        val id = repo.startWorkout(null)
        repo.addExercisesToWorkout(id, listOf(a.id, b.id))
        val first = db.dao().getWorkoutExercises(id).first()
        repo.moveWorkoutExercise(id, first.id, 1)
        assertEquals("Тяга, Жим", db.dao().observeWorkoutSummaries().first().single().exerciseNames)
    }

    private suspend fun workoutWithSets(warmups: Int, working: Int): String {
        val exercise = ExerciseEntity(name = "Жим")
        db.dao().insertExercises(listOf(exercise))
        val id = repo.startWorkout(null)
        repo.addExercisesToWorkout(id, listOf(exercise.id))
        val item = db.dao().getWorkoutExercises(id).single()
        repeat(warmups) { repo.saveSet(SetEntity(workoutExerciseId = item.id, position = -1, reps = 12, isWarmup = true)) }
        repeat(working) { repo.saveSet(SetEntity(workoutExerciseId = item.id, position = -1, reps = 8)) }
        return id
    }

    /** Разминка не попадает в план шаблона: 1 разминочный + 3 рабочих = план 3. */
    @Test fun templateFromWorkoutCountsOnlyWorkingSets() = runBlocking {
        val workoutId = workoutWithSets(warmups = 1, working = 3)
        val templateId = repo.saveWorkoutAsTemplate(workoutId, "Верх")
        assertEquals(3, db.dao().getTemplateExercises(templateId).single().plannedSets)
    }

    /** В истории число подходов — рабочие, как и везде. */
    @Test fun historyCountsOnlyWorkingSets() = runBlocking {
        workoutWithSets(warmups = 2, working = 3)
        assertEquals(3, db.dao().observeWorkoutSummaries().first().single().setCount)
    }
}

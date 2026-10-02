package com.sonnik.topower.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.domain.Muscle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ExercisePackTest {
    @Test fun everyMuscleHasAtLeastThreeExercises() {
        Muscle.entries.forEach { m ->
            val count = EXERCISE_PACK_2.count { m in it.muscles }
            assertTrue("$m: $count", count >= 3)
        }
    }

    @Test fun namesAreUniqueAndRangesValid() {
        val all = DEFAULT_EXERCISES + EXERCISE_PACK_2
        assertEquals(all.size, all.map { it.nameRu.lowercase() }.toSet().size)
        assertEquals(all.size, all.map { it.nameEn.lowercase() }.toSet().size)
        EXERCISE_PACK_2.forEach { e ->
            e.reps?.let { assertTrue(e.nameRu, it.first in 1..it.last) }
            assertTrue(e.nameRu, e.trackReps == (e.reps != null))
        }
    }

    @Test fun packIsAddedOnceWithoutDuplicates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
            val settings = AppSettings(context)
            // Уже установленное приложение: свои упражнения, одно совпадает с набором.
            settings.defaultsSeeded = true
            db.dao().insertExercises(listOf(ExerciseEntity(name = "Моё"), ExerciseEntity(name = "Жим гантелей лёжа")))
            repo.seedDefaultsIfNeeded(settings, russian = true)
            repo.seedDefaultsIfNeeded(settings, russian = true)
            val names = db.dao().allExercises().map { it.name }
            assertEquals(2 + EXERCISE_PACK_2.size - 1, names.size)
            assertEquals(1, names.count { it == "Жим гантелей лёжа" })
            assertEquals(12, db.dao().allExercises().first { it.name == "Махи гантелями в стороны" }.repMin)
        } finally {
            db.close()
        }
    }

    @Test fun generatedTemplateTargetsRestedMuscles() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
            val dao = db.dao()
            dao.insertExercises(EXERCISE_PACK_2.map { it.toEntity(russian = true) })
            // Вчера — грудь и трицепс: их упражнения в подборку попасть не должны.
            val bench = dao.allExercises().first { it.name == "Жим гантелей лёжа" }
            val now = System.currentTimeMillis()
            val w = WorkoutEntity(startedAt = now - 20 * 3_600_000L, finishedAt = now - 19 * 3_600_000L)
            dao.upsertWorkout(w)
            val item = WorkoutExerciseEntity(workoutId = w.id, exerciseId = bench.id, position = 0, recoveryHours = 72)
            dao.upsertWorkoutExercise(item)
            dao.upsertSet(SetEntity(workoutExerciseId = item.id, position = 0, reps = 10, weight = 12.0))

            val id = repo.generateTemplate(now) { "Подбор: " + it.joinToString() }!!
            val picked = dao.getTemplateExercises(id).map { te -> dao.allExercises().first { it.id == te.exerciseId } }
            assertTrue(picked.size in 4..6)
            picked.forEach { e ->
                val muscles = Muscle.parse(e.muscles)
                assertTrue(e.name, Muscle.CHEST !in muscles && Muscle.TRICEPS !in muscles && Muscle.FRONT_DELTS !in muscles)
            }
        } finally {
            db.close()
        }
    }

    @Test fun everyDefaultExerciseHasTechnique() {
        (DEFAULT_EXERCISES + EXERCISE_PACK_2).forEach { e ->
            val (ru, en) = TECHNIQUE[e.nameRu] ?: error("нет техники: ${e.nameRu}")
            assertTrue(e.nameRu, ru.lines().size == 3 && en.lines().size == 3)
            assertEquals(ru, e.toEntity(russian = true).technique)
            assertEquals(en, e.toEntity(russian = false).technique)
        }
        assertEquals(TECHNIQUE.keys, (DEFAULT_EXERCISES + EXERCISE_PACK_2).map { it.nameRu }.toSet())
    }

    @Test fun techniqueFoundByAnyName() {
        val ru = TECHNIQUE.getValue("Резинка на трицепс из-за спины").first
        assertEquals(ru, techniqueFor("Резинка на трицепс из за спины", russian = true))
        assertEquals(TECHNIQUE.getValue("Планка").second, techniqueFor("Plank", russian = false))
        assertEquals(TECHNIQUE.getValue("Планка").first, techniqueFor(" планка ", russian = true))
        assertEquals(null, techniqueFor("Моё упражнение", russian = true))
    }

    @Test fun existingExercisesGetTechniqueOnce() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
            val settings = AppSettings(context)
            settings.defaultsSeeded = true
            settings.exercisePack = 2
            db.dao().insertExercises(
                listOf(
                    ExerciseEntity(name = "Планка"),
                    ExerciseEntity(name = "Пресс с роликом", technique = "Моя техника"),
                    ExerciseEntity(name = "Моё"),
                )
            )
            repo.seedDefaultsIfNeeded(settings, russian = true)
            val byName = db.dao().allExercises().associateBy { it.name }
            assertEquals(TECHNIQUE.getValue("Планка").first, byName.getValue("Планка").technique)
            assertEquals("Моя техника", byName.getValue("Пресс с роликом").technique)
            assertEquals("", byName.getValue("Моё").technique)
            assertEquals(3, settings.exercisePack)
        } finally {
            db.close()
        }
    }
}

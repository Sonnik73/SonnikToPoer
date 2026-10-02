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
}

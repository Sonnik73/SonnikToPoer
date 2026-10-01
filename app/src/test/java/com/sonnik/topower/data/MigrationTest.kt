package com.sonnik.topower.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Проверяет, что обновление приложения не теряет записанные тренировки. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2KeepsData() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO exercises (id, name, category, note, trackReps, trackWeight, trackTime, trackDistance, " +
                    "useBodyWeight, archived, createdAt, updatedAt) VALUES ('e1', 'Жим', '', '', 1, 1, 0, 0, 0, 0, 1, 1)"
            )
            execSQL("INSERT INTO workouts (id, startedAt, finishedAt, bodyWeight, note, updatedAt) VALUES ('w1', 1, NULL, 80.0, '', 1)")
            execSQL(
                "INSERT INTO workout_exercises (id, workoutId, exerciseId, position, plannedSets, updatedAt) " +
                    "VALUES ('we1', 'w1', 'e1', 0, 3, 1)"
            )
            execSQL(
                "INSERT INTO sets (id, workoutExerciseId, position, reps, weight, durationSec, distanceKm, isWarmup, " +
                    "createdAt, updatedAt) VALUES ('s1', 'we1', 0, 10, 60.0, NULL, NULL, 0, 1, 1)"
            )
            close()
        }

        // Открываем базу так же, как приложение: Room проверит, что схема после миграции совпадает с кодом.
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val dao = db.dao()
                val exercise = dao.allExercises().single()
                assertEquals("Жим", exercise.name)
                assertNull(exercise.equipmentType)
                assertFalse(exercise.bandAssist)

                val set = dao.allSets().single()
                assertEquals(60.0, set.weight!!, 1e-9)
                assertEquals(10, set.reps)
                assertFalse(set.pair)
                assertNull(set.equipmentId)

                dao.insertEquipment(listOf(EquipmentEntity(type = EquipmentType.BAND.name, minResistance = 7.0, maxResistance = 16.0)))
                assertEquals(1, dao.allEquipment().size)
            }
        } finally {
            db.close()
        }
    }
}

package com.sonnik.topower.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Проверяет, что обновление приложения не теряет записанные тренировки. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val dbName = "migration-test.db"
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Создаёт базу старой версии по экспортированной схеме Room (app/schemas). */
    private fun createDatabase(version: Int): SQLiteDatabase {
        val schema = File("schemas/com.sonnik.topower.data.AppDatabase/$version.json").readText()
        val database = Json.parseToJsonElement(schema).jsonObject["database"]!!.jsonObject
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        database["entities"]!!.jsonArray.forEach { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            fun sql(raw: String) = db.execSQL(raw.replace("\${TABLE_NAME}", table))
            sql(entity.jsonObject["createSql"]!!.jsonPrimitive.content)
            entity.jsonObject["indices"]?.jsonArray?.forEach { sql(it.jsonObject["createSql"]!!.jsonPrimitive.content) }
        }
        db.version = version
        return db
    }

    @Test
    fun migrate6To7AddsTechnique() {
        createDatabase(6).apply {
            val cols = "id, name, category, note, trackReps, trackWeight, trackTime, trackDistance, useBodyWeight, archived, createdAt, updatedAt, bandAssist, muscles, repMin, repMax"
            execSQL("INSERT INTO exercises ($cols) VALUES ('e1', 'Планка', '', 'моя заметка', 0, 0, 1, 0, 1, 0, 1, 1, 0, 'ABS', NULL, NULL)")
            close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val e = db.dao().allExercises().single()
                assertEquals("моя заметка", e.note)
                assertEquals("", e.technique)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate5To6AddsRepRange() {
        createDatabase(5).apply {
            val cols = "id, name, category, note, trackReps, trackWeight, trackTime, trackDistance, useBodyWeight, archived, createdAt, updatedAt, bandAssist, muscles"
            execSQL("INSERT INTO exercises ($cols) VALUES ('e1', 'Жим', '', '', 1, 1, 0, 0, 0, 0, 1, 1, 0, 'CHEST')")
            close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val e = db.dao().allExercises().single()
                assertEquals("CHEST", e.muscles)
                assertNull(e.repMin)
                assertNull(e.repMax)
                db.dao().upsertExercise(e.copy(repMin = 8, repMax = 12))
                assertEquals(12, db.dao().allExercises().single().repMax)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate4To5AssignsMusclesByName() {
        createDatabase(4).apply {
            val cols = "id, name, category, note, trackReps, trackWeight, trackTime, trackDistance, useBodyWeight, archived, createdAt, updatedAt, bandAssist"
            execSQL("INSERT INTO exercises ($cols) VALUES ('e1', 'Подтягивания узким обратным хватом', '', '', 1, 1, 0, 0, 1, 0, 1, 1, 0)")
            execSQL("INSERT INTO exercises ($cols) VALUES ('e2', 'Резинка на трицепс из за спины', '', '', 1, 1, 0, 0, 0, 0, 1, 1, 0)")
            execSQL("INSERT INTO exercises ($cols) VALUES ('e3', 'Моё упражнение', '', '', 1, 1, 0, 0, 0, 0, 1, 1, 0)")
            close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val byId = db.dao().allExercises().associateBy { it.id }
                assertEquals("LATS,BICEPS,BRACHIALIS", byId.getValue("e1").muscles)
                assertEquals("TRICEPS", byId.getValue("e2").muscles)
                assertEquals("", byId.getValue("e3").muscles)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate3To4AddsWatchData() {
        createDatabase(3).apply {
            execSQL("INSERT INTO workouts (id, startedAt, finishedAt, bodyWeight, note, updatedAt) VALUES ('w1', 1, 2, 80.0, '', 1)")
            close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val workout = db.dao().allWorkouts().single()
                assertEquals(80.0, workout.bodyWeight!!, 1e-9)
                assertNull(workout.maxHeartRate)
                assertFalse(workout.hasWatchData)
                db.dao().upsertWorkout(workout.copy(maxHeartRate = 165, watchDurationSec = 4320, calories = 540))
                val updated = db.dao().allWorkouts().single()
                assertEquals(165, updated.maxHeartRate)
                assertEquals(540, updated.calories)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate2To3AddsRestTime() {
        createDatabase(2).apply {
            execSQL(
                "INSERT INTO exercises (id, name, category, note, trackReps, trackWeight, trackTime, trackDistance, " +
                    "useBodyWeight, archived, createdAt, updatedAt, equipmentType, bandAssist) " +
                    "VALUES ('e1', 'Подтягивания', '', '', 1, 0, 0, 0, 1, 0, 1, 1, 'BAND', 1)"
            )
            close()
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val exercise = db.dao().allExercises().single()
                assertEquals("BAND", exercise.equipmentType)
                assertNull(exercise.restSec)
                db.dao().setExerciseRest("e1", 120, 2)
                assertEquals(120, db.dao().allExercises().single().restSec)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate1To2KeepsData() {
        createDatabase(1).apply {
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
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
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

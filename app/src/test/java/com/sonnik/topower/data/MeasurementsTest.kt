package com.sonnik.topower.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.ui.measurements.bodyShape
import com.sonnik.topower.ui.measurements.firstMeasurements
import com.sonnik.topower.ui.measurements.latestMeasurements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MeasurementsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After fun tearDown() = db.close()

    @Test fun savesFilledFieldsWithOneDateAndShowsChange() = runBlocking {
        repo.saveMeasurements(mapOf(MeasurementPart.BICEPS to 36.0, MeasurementPart.WAIST to 84.0), takenAt = 1_000)
        repo.saveMeasurements(mapOf(MeasurementPart.BICEPS to 37.5, MeasurementPart.CHEST to 0.0), takenAt = 2_000)
        val all = db.dao().allMeasurements()
        assertEquals(3, all.size) // нулевая грудь не сохраняется
        val latest = latestMeasurements(all.sortedBy { it.takenAt })
        assertEquals(37.5, latest.getValue(MeasurementPart.BICEPS).valueCm, 1e-9)
        assertEquals(1.5, latest.getValue(MeasurementPart.BICEPS).deltaCm!!, 1e-9)
        assertNull(latest.getValue(MeasurementPart.WAIST).deltaCm)
        assertNull(latest[MeasurementPart.CHEST])

        repo.deleteMeasurementsAt(2_000)
        assertEquals(listOf(1_000L, 1_000L), db.dao().allMeasurements().map { it.takenAt })
    }

    /** «Стало» на схеме: изменение к первому замеру, усиленное и ограниченное. */
    @Test fun shapeFromFirstToLatest() = runBlocking {
        repo.saveMeasurements(mapOf(MeasurementPart.BICEPS to 36.0, MeasurementPart.WAIST to 84.0, MeasurementPart.CALF to 38.0), takenAt = 1_000)
        repo.saveMeasurements(mapOf(MeasurementPart.BICEPS to 37.0), takenAt = 2_000)
        repo.saveMeasurements(mapOf(MeasurementPart.BICEPS to 37.8, MeasurementPart.WAIST to 81.9, MeasurementPart.CALF to 380.0), takenAt = 3_000)
        val all = db.dao().allMeasurements()
        val first = firstMeasurements(all)
        assertEquals(1_000L, first.getValue(MeasurementPart.BICEPS).takenAt)
        val shape = bodyShape(
            first.mapValues { it.value.valueCm },
            latestMeasurements(all.sortedBy { it.takenAt }).mapValues { it.value.valueCm },
        )
        assertEquals(1.2f, shape.biceps, 1e-3f) // +5 % × 4
        assertEquals(0.9f, shape.waist, 1e-3f) // −2.5 % × 4
        assertEquals(1.4f, shape.calf, 1e-3f) // опечатка 380 вместо 38 — не больше предела
        assertEquals(1f, shape.chest, 1e-3f) // груди не мерили
    }

    @Test fun measurementsSurviveBackup() = runBlocking {
        repo.saveMeasurements(mapOf(MeasurementPart.FOREARM to 30.0), takenAt = 1_000)
        val json = repo.exportBackup()
        db.dao().clearMeasurements()
        repo.importBackup(json)
        assertEquals(30.0, db.dao().allMeasurements().single().valueCm, 1e-9)
        // Копия старой версии без поля замеров восстанавливается.
        repo.importBackup("""{"app":"SonnikToPower","formatVersion":2}""")
        assertEquals(0, db.dao().allMeasurements().size)
    }
}

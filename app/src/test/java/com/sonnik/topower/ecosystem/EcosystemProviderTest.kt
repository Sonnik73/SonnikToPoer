package com.sonnik.topower.ecosystem

import android.content.ContentValues
import android.content.Context
import android.content.pm.PermissionInfo
import android.database.Cursor
import android.net.Uri
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.ecosystem.contract.EcosystemContract
import com.sonnik.topower.data.AppDatabase
import com.sonnik.topower.data.AppSettings
import com.sonnik.topower.data.Repository
import com.sonnik.topower.data.WorkoutEntity
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.MuscleSets
import com.sonnik.topower.domain.computeOverallStats
import com.sonnik.topower.domain.state
import com.sonnik.topower.domain.weeklySets
import com.sonnik.topower.reminders.muscleStatuses
import com.sonnik.topower.ui.seedDemoData
import com.sonnik.topower.ui.stats.toRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EcosystemProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
        EcosystemProvider.dao = db.dao()
        Robolectric.setupContentProvider(EcosystemProvider::class.java, EcosystemContract.AUTHORITY)
    }

    @After
    fun tearDown() {
        EcosystemProvider.dao = null
        db.close()
    }

    private fun seed() = runBlocking { seedDemoData(repo, AppSettings(context)) }

    private fun query(uri: String, projection: Array<String>? = null): Cursor =
        context.contentResolver.query(Uri.parse(uri), projection, null, null, null)!!

    /** Строки курсора как список «колонка → значение». */
    private fun rows(uri: String): List<Map<String, Any?>> = query(uri).use { c ->
        buildList {
            while (c.moveToNext()) {
                add(
                    c.columnNames.indices.associate { i ->
                        c.getColumnName(i) to when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> null
                            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                            else -> c.getString(i)
                        }
                    }
                )
            }
        }
    }

    private val allTime get() = "?from=0&to=${System.currentTimeMillis() + 60_000}"

    @Test
    fun everyUriReturnsContractColumns() {
        seed()
        val expected = mapOf(
            EcosystemContract.Info.URI to EcosystemContract.Info.COLUMNS,
            EcosystemContract.Workouts.URI to EcosystemContract.Workouts.COLUMNS,
            EcosystemContract.MuscleVolume.URI to EcosystemContract.MuscleVolume.COLUMNS,
            EcosystemContract.Recovery.URI to EcosystemContract.Recovery.COLUMNS,
            EcosystemContract.BodyWeight.URI to EcosystemContract.BodyWeight.COLUMNS,
            EcosystemContract.Records.URI to EcosystemContract.Records.COLUMNS,
        )
        expected.forEach { (uri, columns) ->
            query(uri).use { c ->
                assertArrayEquals(uri, columns, c.columnNames)
                assertTrue(uri, c.count > 0)
            }
        }

        val info = rows(EcosystemContract.Info.URI).single()
        assertEquals(1L, info["schema_version"])
        assertTrue((info["app_version"] as String).isNotEmpty())
        assertTrue(info["last_changed_at"] as Long > 0)

        // Последние 7 дней: тренировки 6 дней и 36 часов назад и текущая, ещё не завершённая.
        val workouts = rows(EcosystemContract.Workouts.URI)
        assertEquals(3, workouts.size)
        val active = workouts.last()
        assertNull(active["finished_at"])
        assertNull(active["duration_sec"])
        assertNull(active["calories"])
        assertEquals(2L, active["working_sets"]) // разминка не считается
        assertEquals(78.8, active["body_weight_kg"])
        assertEquals(2 * 14.0 * 12 + 78.8 * 8, active["tonnage_kg"] as Double, 1e-6) // жим парой + подтягивания с весом тела
        workouts.forEach { assertEquals(36, (it["id"] as String).length) }

        val recovery = rows(EcosystemContract.Recovery.URI)
        assertEquals(Muscle.entries.map { it.name }, recovery.map { it["muscle"] })
        val never = recovery.first { it["muscle"] == "HAMSTRINGS" }
        assertEquals("NEVER", never["state"])
        assertNull(never["last_trained_at"])
        assertEquals(1.0, never["recovered_fraction"])

        assertEquals(listOf(79.0, 78.8), rows(EcosystemContract.BodyWeight.URI).map { it["weight_kg"] }.takeLast(2))

        // Жим 2 × 14 кг на 12 повторов — новый рекорд по расчётному максимуму.
        val press = rows(EcosystemContract.Records.URI).single { it["exercise_name"] == "Жим гантелей лёжа" }
        assertEquals("E1RM", press["metric"])
        assertEquals(14.0 * (1 + 12 / 30.0), press["value"] as Double, 1e-9)
    }

    @Test
    fun tonnageAndSetsMatchStatisticsScreen() {
        seed()
        val zone = ZoneId.systemDefault()
        val overall = runBlocking {
            val dao = db.dao()
            computeOverallStats(
                dao.allWorkouts().map { it.startedAt },
                dao.allSetsWithContext().map { it.toRecord() },
                LocalDate.now(zone),
                zone,
            )
        }
        val workouts = rows(EcosystemContract.Workouts.URI + allTime)
        assertEquals(overall.totalWorkouts, workouts.size)
        assertEquals(overall.totalSets.toLong(), workouts.sumOf { it["working_sets"] as Long })
        assertEquals(overall.totalTonnage, workouts.sumOf { it["tonnage_kg"] as Double }, 1e-6)

        // Подход засчитывается каждой мышце упражнения: тоннаж квадрицепсов — тоннаж приседаний.
        val squats = runBlocking {
            val dao = db.dao()
            val squat = dao.allExercises().first { it.name == "Приседания" }
            computeOverallStats(emptyList(), dao.setsForExercise(squat.id).map { it.toRecord() }, LocalDate.now(zone), zone)
        }
        val quads = rows(EcosystemContract.MuscleVolume.URI + allTime).single { it["muscle"] == "QUADS" }
        assertEquals(squats.totalTonnage, quads["tonnage_kg"] as Double, 1e-6)
        assertEquals(squats.totalSets.toLong(), quads["working_sets"])
        assertEquals(20L * 3 + 25 * 3, quads["reps"])
        assertEquals(2L, quads["workouts"])
    }

    @Test
    fun muscleVolumeMatchesWeeklyVolumeOnBodyMap() {
        seed()
        val now = System.currentTimeMillis()
        val weekly = runBlocking {
            weeklySets(db.dao().muscleWork().map { MuscleSets(it.time, Muscle.parse(it.muscles), it.sets) }, now)
        }
        val volume = rows(EcosystemContract.MuscleVolume.URI).associate { it["muscle"] to it["working_sets"] }
        assertEquals(weekly.filterValues { it > 0 }.mapKeys { it.key.name }.mapValues { it.value.toLong() }, volume)
        assertEquals(Muscle.entries.filter { it.name in volume }.map { it.name }, volume.keys.toList())

        val statuses = runBlocking { muscleStatuses(db.dao().muscleWork()) }
        rows(EcosystemContract.Recovery.URI).forEach { r ->
            assertEquals(statuses.getValue(Muscle.valueOf(r["muscle"] as String)).state(now).name, r["state"])
        }
    }

    @Test
    fun periodAndProjectionParameters() {
        seed()
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        assertEquals(1, rows("${EcosystemContract.Workouts.URI}?from=${now - 2 * day}&to=${now - day}").size)
        assertEquals(0, rows("${EcosystemContract.Workouts.URI}?from=$now&to=0").size)
        assertEquals(0, rows("${EcosystemContract.Records.URI}?since=${now + day}").size)
        query(EcosystemContract.Recovery.URI, arrayOf("state", "muscle")).use { c ->
            assertArrayEquals(arrayOf("state", "muscle"), c.columnNames)
        }
        expectFailure<IllegalArgumentException> { query("${EcosystemContract.Workouts.URI}?from=yesterday") }
        expectFailure<IllegalArgumentException> { query(EcosystemContract.BASE_URI + "sets") }
        expectFailure<IllegalArgumentException> { query(EcosystemContract.Info.URI, arrayOf("note")) }
    }

    @Test
    fun writesAreRejected() {
        seed()
        val resolver = context.contentResolver
        val uri = Uri.parse(EcosystemContract.Workouts.URI)
        expectFailure<UnsupportedOperationException> { resolver.insert(uri, ContentValues().apply { put("id", "x") }) }
        expectFailure<UnsupportedOperationException> { resolver.update(uri, ContentValues(), null, null) }
        expectFailure<UnsupportedOperationException> { resolver.delete(uri, null, null) }
        expectFailure<UnsupportedOperationException> { resolver.bulkInsert(uri, arrayOf(ContentValues())) }
        assertEquals(3, rows(EcosystemContract.Workouts.URI).size)
    }

    @Test
    fun manifestProtectsProviderWithSignaturePermission() {
        val pm = context.packageManager
        val provider = pm.resolveContentProvider(EcosystemContract.AUTHORITY, 0)!!
        assertTrue(provider.exported)
        assertEquals(EcosystemContract.PERMISSION_READ, provider.readPermission)
        assertNull(provider.writePermission)
        val permission = pm.getPermissionInfo(EcosystemContract.PERMISSION_READ, 0)
        assertEquals(PermissionInfo.PROTECTION_SIGNATURE, permission.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE)
    }

    @Test
    fun emptyDatabaseGivesEmptyCursors() {
        listOf(
            EcosystemContract.Workouts.URI,
            EcosystemContract.MuscleVolume.URI,
            EcosystemContract.BodyWeight.URI,
            EcosystemContract.Records.URI,
        ).forEach { uri -> assertEquals(uri, 0, rows(uri).size) }
        assertEquals(1, rows(EcosystemContract.Info.URI).size)
        assertEquals(setOf("NEVER"), rows(EcosystemContract.Recovery.URI).map { it["state"] }.toSet())
    }

    @Test
    fun changesNotifySubscribers() {
        val calls = AtomicInteger()
        repo.observeTrainingChanges { calls.incrementAndGet() }
        runBlocking { db.dao().upsertWorkout(WorkoutEntity()) }
        val deadline = System.currentTimeMillis() + 5_000
        while (calls.get() == 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("observer was not called", calls.get() > 0)

        EcosystemChanges.onDataChanged(context, now = 1234L)
        assertEquals(1234L, EcosystemChanges.lastChangedAt(context))
        assertTrue(rows(EcosystemContract.Info.URI).single()["last_changed_at"] as Long >= 1234L)
        val notified = shadowOf(context.contentResolver).notifiedUris.map { it.uri.toString() }
        assertTrue(notified.toString(), EcosystemContract.BASE_URI in notified)
    }

    private inline fun <reified T : Throwable> expectFailure(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return
            throw AssertionError("expected ${T::class.simpleName}, got $e", e)
        }
        fail("expected ${T::class.simpleName}")
    }
}

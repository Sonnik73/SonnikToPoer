package com.sonnik.topower.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.data.AppDatabase
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.Repository
import com.sonnik.topower.data.WorkoutEntity
import com.sonnik.topower.ecosystem.EcosystemChanges
import com.sonnik.topower.updates.UpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** Копии в памяти вместо Google Диска. */
private class MemoryStore : BackupStore {
    val files = mutableListOf<Pair<RemoteBackup, String>>()
    var failUpload = false
    private var clock = 1_000L

    override suspend fun list() = files.map { it.first }.sortedByDescending { it.modified }
    override suspend fun upload(name: String, json: String): RemoteBackup {
        if (failUpload) throw IOException("offline")
        val b = RemoteBackup("id${files.size}-$clock", name, clock++, json.length.toLong())
        files += b to json
        return b
    }
    override suspend fun download(backup: RemoteBackup) = files.first { it.first.id == backup.id }.second
    override suspend fun delete(backup: RemoteBackup) {
        files.removeAll { it.first.id == backup.id }
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DriveBackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private val store = MemoryStore()

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After fun tearDown() = db.close()

    /** Подключённое хранилище без входа в Google и без расписания WorkManager. */
    private fun backup(connected: Boolean = true): DriveBackup {
        context.getSharedPreferences("drive_backup", Context.MODE_PRIVATE).edit().putBoolean("connected", connected).commit()
        return DriveBackup(context, repo) { store }
    }

    @Test fun backupUploadsJsonAndKeepsLastTen() = runBlocking {
        db.dao().insertExercises(listOf(ExerciseEntity(name = "Планка")))
        val b = backup()
        repeat(12) { assertTrue(b.backupNow(now = 5_000L + it)) }
        assertEquals(BackupPolicy.KEEP, store.files.size)
        assertTrue(store.files.all { it.first.name.startsWith(BackupPolicy.PREFIX) && it.first.name.endsWith(".json") })
        assertTrue(store.files.last().second.contains("Планка"))
        assertEquals(5_011L, b.state.value.lastBackupAt)
        assertNull(b.state.value.lastError)
    }

    @Test fun restoreReplacesData() = runBlocking {
        db.dao().insertExercises(listOf(ExerciseEntity(name = "Из копии")))
        val b = backup()
        b.backupNow()
        db.dao().clearExercises()
        db.dao().insertExercises(listOf(ExerciseEntity(name = "Новое")))
        b.restore(b.list().first())
        assertEquals(listOf("Из копии"), db.dao().allExercises().map { it.name })
    }

    @Test fun failureIsShownAndNothingIsRecorded() = runBlocking {
        val b = backup()
        store.failUpload = true
        assertFalse(b.backupNow(now = 9_000L))
        assertEquals(0L, b.state.value.lastBackupAt)
        assertTrue(b.state.value.lastError!!.contains("offline"))
        store.failUpload = false
        assertTrue(b.backupNow(now = 9_500L))
        assertNull(b.state.value.lastError)
    }

    @Test fun autoBackupOnlyWhenConnectedAndChanged() = runBlocking {
        val off = backup(connected = false)
        EcosystemChanges.onDataChanged(context, now = 2_000L)
        off.backupIfChanged()
        assertEquals(0, store.files.size)

        val b = backup()
        b.backupIfChanged()
        assertEquals(1, store.files.size)
        b.backupIfChanged() // изменений после копии не было
        assertEquals(1, store.files.size)
        db.dao().upsertWorkout(WorkoutEntity())
        EcosystemChanges.onDataChanged(context, now = System.currentTimeMillis() + 60_000)
        b.backupIfChanged()
        assertEquals(2, store.files.size)
    }

    @Test fun policyKeepsForeignFiles() {
        val list = (1..12).map { RemoteBackup("$it", BackupPolicy.PREFIX + it, it.toLong(), null) } +
            RemoteBackup("x", "notes.txt", 0, null)
        assertEquals(listOf("2", "1"), BackupPolicy.toDelete(list).map { it.id })
    }

    @Test fun parsesGithubRelease() {
        val r = UpdateChecker.parseRelease(
            """{"tag_name":"v1.28","html_url":"https://github.com/x/releases/tag/v1.28","body":"## 1.28\n- Обновления",
               "published_at":"2026-11-01T10:00:00Z",
               "assets":[{"name":"notes.txt","browser_download_url":"t","size":3},
                         {"name":"SonnikToPower-1.28.apk","browser_download_url":"https://x/a.apk","size":7586445}]}"""
        )
        assertEquals("1.28", r.label)
        assertEquals("https://x/a.apk", r.apkUrl)
        assertEquals(7_586_445L, r.apkBytes)
        assertEquals("- Обновления", r.notes)
        assertEquals(1_793_527_200_000L, r.publishedAt)
    }
}

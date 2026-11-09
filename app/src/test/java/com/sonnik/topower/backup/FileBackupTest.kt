package com.sonnik.topower.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sonnik.topower.data.AppDatabase
import com.sonnik.topower.data.ExerciseEntity
import com.sonnik.topower.data.Repository
import com.sonnik.topower.ecosystem.EcosystemChanges
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

/** Файлы в памяти вместо Google Диска. */
private class MemoryIo : FileIo {
    val files = mutableMapOf<Uri, ByteArray>()
    var writes = 0
    var denied = false
    override suspend fun read(uri: Uri) = files[uri]
    override suspend fun write(uri: Uri, bytes: ByteArray) {
        if (denied) throw SecurityException("revoked")
        files[uri] = bytes
        writes++
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FileBackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private val io = MemoryIo()
    private val uri = Uri.parse("content://com.google.android.apps.docs.storage/document/acc%3D1%3Bdoc%3D42")

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db, CoroutineScope(Dispatchers.Unconfined))
    }

    @After fun tearDown() = db.close()

    private fun text() = io.files.getValue(uri).toString(Charsets.UTF_8)

    @Test fun writesBackupOnlyWhenDataChanged() = runBlocking {
        db.dao().insertExercises(listOf(ExerciseEntity(name = "Жим")))
        val backup = FileBackup(context, repo, io)
        assertTrue(backup.write()) // не подключён — нечего делать
        assertEquals(0, io.writes)

        backup.attach(uri)
        assertTrue(backup.state.value.enabled)
        assertTrue(backup.write(now = 10_000L)) // первая запись — всегда
        assertTrue(text().contains("Жим"))
        assertEquals(10_000L, backup.state.value.lastAt)

        EcosystemChanges.onDataChanged(context, now = 5_000L) // изменение раньше записи
        backup.write(now = 11_000L)
        assertEquals(1, io.writes)
        EcosystemChanges.onDataChanged(context, now = 12_000L)
        backup.write(now = 13_000L)
        assertEquals(2, io.writes)
        backup.write(force = true, now = 14_000L)
        assertEquals(3, io.writes)
        // Правка шаблонов или инвентаря тоже требует новой копии.
        EcosystemChanges.onOtherDataChanged(context, now = 15_000L)
        backup.write(now = 16_000L)
        assertEquals(4, io.writes)
    }

    @Test fun lostAccessIsReported() = runBlocking {
        val backup = FileBackup(context, repo, io)
        backup.attach(uri)
        io.denied = true
        assertFalse(backup.write(force = true))
        assertTrue(backup.state.value.error!!.isNotBlank())
        io.denied = false
        assertTrue(backup.write(force = true))
        assertNull(backup.state.value.error)
    }

    @Test fun existingFileCanBeRestored() = runBlocking {
        db.dao().insertExercises(listOf(ExerciseEntity(name = "С прежнего телефона")))
        val old = FileBackup(context, repo, io)
        old.attach(uri)
        old.write(force = true)

        db.dao().clearExercises()
        val fresh = FileBackup(context, repo, io)
        val found = fresh.readBackup(uri)!!
        fresh.restore(found)
        assertEquals(listOf("С прежнего телефона"), db.dao().allExercises().map { it.name })

        io.files[uri] = "not json".toByteArray()
        assertNull(fresh.readBackup(uri))
        assertNull(fresh.readBackup(Uri.parse("content://x/missing")))
    }

    @Test fun detachForgetsFile() {
        val backup = FileBackup(context, repo, io)
        backup.attach(uri)
        backup.detach()
        assertFalse(backup.state.value.enabled)
        assertFalse(FileBackup(context, repo, io).state.value.enabled)
    }
}

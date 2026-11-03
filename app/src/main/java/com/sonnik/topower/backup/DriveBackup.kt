package com.sonnik.topower.backup

import android.content.Context
import com.sonnik.topower.R
import com.sonnik.topower.Flavor
import com.sonnik.topower.data.Repository
import com.sonnik.topower.ecosystem.EcosystemChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime

/** Состояние копий на Диске для экрана настроек. */
data class DriveBackupState(
    val connected: Boolean = false,
    val lastBackupAt: Long = 0L,
    val lastError: String? = null,
    val running: Boolean = false,
)

/**
 * Резервные копии в Google Диске: та же JSON-копия, что «Экспорт» в настройках, файлом в папку
 * «SonnikToPower». Автоматически — раз в [BackupPolicy.AUTO_INTERVAL_HOURS] ч, если данные менялись,
 * вручную — кнопкой. Хранятся последние [BackupPolicy.KEEP] копий.
 * [storeFactory] даёт хранилище (в сборке full — Google Диск с токеном входа; в сборке для F-Droid
 * входа в Google нет) — в тестах подменяется.
 */
class DriveBackup(
    private val context: Context,
    private val repository: Repository,
    private val storeFactory: suspend () -> BackupStore = Flavor.driveStore(context),
) {
    private val prefs = context.getSharedPreferences("drive_backup", Context.MODE_PRIVATE)
    private val lock = Mutex()

    private val _state = MutableStateFlow(
        DriveBackupState(
            connected = prefs.getBoolean(KEY_CONNECTED, false),
            lastBackupAt = prefs.getLong(KEY_LAST, 0L),
            lastError = prefs.getString(KEY_ERROR, null),
        )
    )
    val state: StateFlow<DriveBackupState> = _state.asStateFlow()

    /** После входа в Google: включает автокопии и сразу делает первую. */
    suspend fun connect(): Boolean {
        prefs.edit().putBoolean(KEY_CONNECTED, true).remove(KEY_ERROR).apply()
        _state.value = _state.value.copy(connected = true, lastError = null)
        BackupWorker.schedule(context)
        return backupNow()
    }

    /** Выключает автокопии. Файлы на Диске остаются. */
    fun disconnect() {
        prefs.edit().putBoolean(KEY_CONNECTED, false).remove(KEY_ERROR).apply()
        _state.value = _state.value.copy(connected = false, lastError = null)
        BackupWorker.cancel(context)
    }

    /** Автокопия из фона: только при подключении и если данные менялись после прошлой копии. */
    suspend fun backupIfChanged(): Boolean {
        val s = _state.value
        if (!s.connected || !BackupPolicy.autoDue(s.lastBackupAt, EcosystemChanges.lastChangedAt(context))) return true
        return backupNow()
    }

    /** Делает копию сейчас и удаляет лишние старые. false — не удалось (причина в [state]). */
    suspend fun backupNow(now: Long = System.currentTimeMillis()): Boolean = guarded {
        val json = repository.exportBackup()
        val store = storeFactory()
        store.upload(BackupPolicy.fileName(LocalDateTime.now()), json)
        // Старые копии чистим после успешной загрузки: ошибка удаления копию не отменяет.
        runCatching { BackupPolicy.toDelete(store.list()).forEach { store.delete(it) } }
        prefs.edit().putLong(KEY_LAST, now).remove(KEY_ERROR).apply()
        _state.value = _state.value.copy(lastBackupAt = now, lastError = null)
    }

    /** Копии на Диске, от новой к старой. */
    suspend fun list(): List<RemoteBackup> = storeFactory().list()

    /** Заменяет все данные содержимым копии [backup]. */
    suspend fun restore(backup: RemoteBackup) {
        val store = storeFactory()
        repository.importBackup(store.download(backup))
    }

    private suspend fun guarded(block: suspend () -> Unit): Boolean = lock.withLock {
        _state.value = _state.value.copy(running = true)
        try {
            block()
            true
        } catch (e: NeedsSignInException) {
            fail(e.message)
        } catch (e: IOException) {
            fail(context.getString(R.string.drive_error_io, e.message?.take(160).orEmpty()))
        } catch (e: Exception) {
            fail(e.message ?: e.javaClass.simpleName)
        } finally {
            _state.value = _state.value.copy(running = false)
        }
    }

    private fun fail(message: String?): Boolean {
        prefs.edit().putString(KEY_ERROR, message).apply()
        _state.value = _state.value.copy(lastError = message)
        return false
    }

    private companion object {
        const val KEY_CONNECTED = "connected"
        const val KEY_LAST = "last_backup_at"
        const val KEY_ERROR = "last_error"
    }
}

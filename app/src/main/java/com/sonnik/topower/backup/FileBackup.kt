package com.sonnik.topower.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sonnik.topower.R
import com.sonnik.topower.data.BackupCodec
import com.sonnik.topower.data.Repository
import com.sonnik.topower.ecosystem.EcosystemChanges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/** Копия одним файлом: где он и как прошла последняя запись. */
data class FileBackupState(val uri: String? = null, val lastAt: Long = 0L, val error: String? = null) {
    val enabled: Boolean get() = uri != null
}

/** Чтение и запись файла по адресу из системного окна — в тестах подменяется. */
interface FileIo {
    suspend fun read(uri: Uri): ByteArray?
    suspend fun write(uri: Uri, bytes: ByteArray)
}

/** Файл через ContentResolver: Google Диск, «Файлы», карта памяти — что выбрали в окне «Сохранить в…». */
class ResolverFileIo(private val context: Context) : FileIo {
    override suspend fun read(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    override suspend fun write(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        // «wt» — перезаписать целиком; не все хранилища его понимают, тогда обычная запись.
        val out = runCatching { context.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: context.contentResolver.openOutputStream(uri, "w")
            ?: throw FileNotFoundException(uri.toString())
        out.use { it.write(bytes) }
    }
}

/**
 * Копия одним JSON-файлом, как хранение файлом в Pin: файл создаётся через системное окно «Сохранить в…»
 * (например, на Google Диске), и приложение само перезаписывает его после изменений. Вход в Google
 * и Google Cloud не нужны — в облако файл отправляет приложение Диска. Формат — тот же, что «Сохранить в файл».
 */
class FileBackup(
    private val context: Context,
    private val repository: Repository,
    private val io: FileIo = ResolverFileIo(context),
) {
    private val prefs = context.getSharedPreferences("file_backup", Context.MODE_PRIVATE)
    private val lock = Mutex()

    private val _state = MutableStateFlow(
        FileBackupState(prefs.getString(KEY_URI, null), prefs.getLong(KEY_LAST, 0L), prefs.getString(KEY_ERROR, null))
    )
    val state: StateFlow<FileBackupState> = _state.asStateFlow()

    private fun update(change: (FileBackupState) -> FileBackupState) {
        val s = change(_state.value)
        prefs.edit().putString(KEY_URI, s.uri).putLong(KEY_LAST, s.lastAt).putString(KEY_ERROR, s.error).apply()
        _state.value = s
    }

    /** Запоминает выбранный файл; доступ к нему сохраняется и после перезапуска телефона. */
    fun attach(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        update { FileBackupState(uri = uri.toString()) }
    }

    /** Перестаёт писать в файл. Сам файл остаётся. */
    fun detach() {
        _state.value.uri?.let { u ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(u),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        update { FileBackupState() }
    }

    /**
     * Записывает копию в файл, если данные менялись после прошлой записи (или [force]).
     * false — не удалось, причина в [state].
     */
    suspend fun write(force: Boolean = false, now: Long = System.currentTimeMillis()): Boolean = lock.withLock {
        val s = _state.value
        val uri = s.uri?.let(Uri::parse) ?: return@withLock true
        if (!force && s.lastAt != 0L && !BackupPolicy.autoDue(s.lastAt, EcosystemChanges.lastChangedAt(context))) {
            return@withLock true
        }
        try {
            io.write(uri, repository.exportBackup().toByteArray(Charsets.UTF_8))
            update { it.copy(lastAt = now, error = null) }
            true
        } catch (e: SecurityException) {
            update { it.copy(error = context.getString(R.string.file_backup_no_access)) }
            false
        } catch (e: Exception) {
            update { it.copy(error = e.message ?: e.javaClass.simpleName) }
            false
        }
    }

    /** Копия из файла [uri], если это копия SonnikToPower с тренировками или упражнениями; иначе null. */
    suspend fun readBackup(uri: Uri): String? {
        val text = runCatching { io.read(uri)?.toString(Charsets.UTF_8) }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val backup = runCatching { BackupCodec.decode(text) }.getOrNull() ?: return null
        return text.takeIf { backup.workouts.isNotEmpty() || backup.exercises.isNotEmpty() }
    }

    /** Заменяет данные содержимым файла и запоминает время, чтобы сразу не перезаписывать его тем же. */
    suspend fun restore(text: String, now: Long = System.currentTimeMillis()) {
        repository.importBackup(text)
        update { it.copy(lastAt = now, error = null) }
    }

    private companion object {
        const val KEY_URI = "uri"
        const val KEY_LAST = "last_at"
        const val KEY_ERROR = "error"
    }
}

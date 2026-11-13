package com.sonnik.topower.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sonnik.topower.domain.ReleaseInfo
import com.sonnik.topower.domain.Updates
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Состояние ручной проверки обновлений. */
sealed interface UpdateCheckState {
    data object Checking : UpdateCheckState
    data class UpToDate(val current: String, val latest: ReleaseInfo) : UpdateCheckState
    data class Failed(val reason: UpdateFailure, val detail: String = "") : UpdateCheckState
}

enum class UpdateFailure { NOT_FOUND, RATE_LIMITED, NETWORK, OTHER }

class UpdateException(val reason: UpdateFailure, message: String) : Exception(message)

/** Скачивание обновления в фоне: приложение качает APK и ставит его штатным установщиком (как в Pin). */
sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val bytes: Long, val total: Long?) : UpdateDownloadState
    data class Ready(val bytes: Long) : UpdateDownloadState
    data class Failed(val reason: String) : UpdateDownloadState
}

/**
 * Проверка обновлений как в Pin: один запрос к GitHub за последним выпуском, без входа и ключей.
 * О пользователе ничего не отправляется. Автопроверку можно выключить в настройках.
 * Найденный выпуск и ход ручной проверки — в [offer] и [check]: окна показывает AppRoot.
 * Новая версия скачивается в фон в файлы приложения и ставится штатным установщиком Android.
 */
class UpdateChecker(private val context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Новая версия, которую стоит предложить. */
    val offer = MutableStateFlow<ReleaseInfo?>(null)

    /** Окно ручной проверки: «Проверяем…», «Обновлений нет», «Не удалось». */
    val check = MutableStateFlow<UpdateCheckState?>(null)

    /** Скачивание обновления в фоне — прогресс в том же окне предложения. */
    val download = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)

    val currentVersion: String
        get() = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    /** Версия скачанного и ждущего установки APK (null — скачивания не было). Живёт между запусками. */
    var pendingVersion: String?
        get() = prefs.getString(KEY_PENDING, null)
        private set(value) {
            prefs.edit().putString(KEY_PENDING, value).apply()
        }

    /** Скачивание идёт в фоне в собственном скоупе: окно можно закрыть, а приложение — свернуть. */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    /** Раз за запуск: отказ от установщика не зацикливается, следующая попытка — при следующем входе. */
    private var installOffered: String? = null

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    /** При входе в приложение: не чаще раза в 6 часов, без пропущенной версии; ошибки молча. */
    suspend fun checkIfDue(now: Long = System.currentTimeMillis()) {
        if (!_enabled.value || !Updates.isDue(prefs.getLong(KEY_LAST, 0), now)) return
        val release = runCatching { fetch() }.getOrNull() ?: return
        prefs.edit().putLong(KEY_LAST, now).apply()
        if (Updates.shouldOffer(release, currentVersion, prefs.getString(KEY_SKIPPED, null))) {
            offer.value = release
            startDownload(release)
        }
    }

    /** «Проверить обновления»: всегда спрашивает GitHub и показывает даже пропущенную версию. */
    suspend fun checkNow() {
        check.value = UpdateCheckState.Checking
        try {
            val release = fetch()
            prefs.edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            if (Updates.isNewer(release.version, currentVersion)) {
                check.value = null
                offer.value = release
                startDownload(release)
            } else {
                check.value = UpdateCheckState.UpToDate(currentVersion, release)
            }
        } catch (e: UpdateException) {
            check.value = UpdateCheckState.Failed(e.reason, e.message.orEmpty())
        } catch (e: Exception) {
            check.value = UpdateCheckState.Failed(UpdateFailure.NETWORK, e.message ?: e.javaClass.simpleName)
        }
    }

    fun skip(release: ReleaseInfo) {
        downloadJob?.cancel()
        downloadJob = null
        download.value = UpdateDownloadState.Idle
        prefs.edit().putString(KEY_SKIPPED, release.version).apply()
        offer.value = null
    }

    /** Начать скачивание APK в фоне (окно можно закрыть — скачивание продолжится). */
    fun startDownload(release: ReleaseInfo) {
        downloaded(release.version)?.let {
            download.value = UpdateDownloadState.Ready(it.length())
            return
        }
        if (release.apkUrl == null || downloadJob?.isActive == true) return
        download.value = UpdateDownloadState.Downloading(0, release.apkBytes)
        downloadJob = scope.launch {
            val file = download(release) { bytes, total ->
                download.value = UpdateDownloadState.Downloading(bytes, total)
            }
            // Скачивание отменили («Пропустить») — состояние уже сброшено, не пишем поверх «Не получилось».
            if (!isActive) return@launch
            download.value = if (file == null) {
                UpdateDownloadState.Failed(UpdateFailure.NETWORK.name)
            } else {
                UpdateDownloadState.Ready(file.length())
            }
        }
    }

    /** Скачанный файл версии [version], если он цел; битые и устаревшие файлы подчищает. */
    fun downloaded(version: String): File? {
        val dir = File(context.filesDir, UPDATES_DIR)
        val target = File(dir, Updates.apkName(version))
        // Остатки других версий не копятся: обновились — старый APK больше не нужен.
        dir.listFiles()?.forEach { if (it.isFile && it != target) it.delete() }
        return target.takeIf { it.isFile && it.length() > 0 }
    }

    /**
     * Скачать APK в файлы приложения (фоном); прогресс — байтами в [onProgress].
     * null — не скачалось (нет сети, оборвалось, размер не сошёлся с GitHub). Частичный файл удаляется.
     */
    private suspend fun download(release: ReleaseInfo, onProgress: (bytes: Long, total: Long?) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val url = release.apkUrl ?: return@withContext null
            val dir = File(context.filesDir, UPDATES_DIR).apply { mkdirs() }
            val target = File(dir, Updates.apkName(release.version))
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "SonnikToPower-Android")
                if (connection.responseCode != 200) return@withContext null
                val total = connection.contentLengthLong.takeIf { it > 0 }
                try {
                    connection.inputStream.use { input ->
                        target.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            var read = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                read += n
                                onProgress(read, total)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    target.delete()
                    throw e
                } catch (e: Exception) {
                    target.delete()
                    return@withContext null
                }
            } finally {
                connection.disconnect()
            }
            // Размер из GitHub — контроль целостности скачивания.
            val sizeOk = release.apkBytes == null || target.length() == release.apkBytes
            if (!sizeOk || target.length() == 0L) {
                target.delete()
                return@withContext null
            }
            pendingVersion = release.version
            target
        }

    /** Открыть штатный установщик Android со скачанным APK. Без разрешения «неизвестные источники» — в настройки. */
    fun install(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settings) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Обновились или файл потерялся — ожидание установки больше неактуально. */
    fun clearPendingIfStale() {
        val version = pendingVersion ?: return
        if (!Updates.isNewer(version, currentVersion) || downloaded(version) == null) pendingVersion = null
    }

    /**
     * При входе в приложение: если обновление уже скачано — открыть штатный установщик.
     * Раз за запуск: отказ от установщика не зацикливается, следующая попытка — при следующем входе.
     */
    fun offerPendingInstall() {
        clearPendingIfStale()
        val version = pendingVersion ?: return
        if (installOffered == version) return
        val file = downloaded(version) ?: return
        installOffered = version
        install(file)
    }

    /** Кнопка «Установить» в окне обновления. */
    fun installDownloaded(release: ReleaseInfo) {
        downloaded(release.version)?.let {
            installOffered = release.version
            install(it)
        }
    }

    private suspend fun fetch(): ReleaseInfo = withContext(Dispatchers.IO) {
        val connection = URL(Updates.LATEST_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "SonnikToPower-Android")
            when (val code = connection.responseCode) {
                200 -> parseRelease(connection.inputStream.bufferedReader().use { it.readText() })
                404 -> throw UpdateException(UpdateFailure.NOT_FOUND, "404")
                403, 429 -> throw UpdateException(UpdateFailure.RATE_LIMITED, code.toString())
                else -> throw UpdateException(UpdateFailure.OTHER, "HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST = "last_check"
        private const val KEY_SKIPPED = "skipped"
        private const val KEY_PENDING = "pending"
        private const val UPDATES_DIR = "updates"

        /**
         * Ответ GitHub о выпуске → [ReleaseInfo]: .apk обычной сборки, текст без заголовка версии.
         * Сборка для F-Droid (`-fdroid` в имени) лежит в том же выпуске, но без обновлений и Диска — её не предлагаем.
         */
        fun parseRelease(json: String): ReleaseInfo {
            val o = JSONObject(json)
            val assets = o.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0))
                .map { assets!!.getJSONObject(it) }
                .firstOrNull {
                    val name = it.optString("name")
                    name.endsWith(".apk", ignoreCase = true) && !name.contains("fdroid", ignoreCase = true)
                }
            return ReleaseInfo(
                version = o.getString("tag_name"),
                pageUrl = o.optString("html_url").ifBlank { Updates.RELEASES_PAGE },
                apkUrl = apk?.optString("browser_download_url")?.takeIf { it.isNotBlank() },
                notes = Updates.cleanNotes(o.optString("body")),
                publishedAt = o.optString("published_at").takeIf { it.isNotBlank() }
                    ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
                apkBytes = apk?.optLong("size")?.takeIf { it > 0 },
            )
        }
    }
}

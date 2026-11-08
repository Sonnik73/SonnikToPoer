package com.sonnik.topower.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sonnik.topower.domain.ReleaseInfo
import com.sonnik.topower.domain.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * Проверка обновлений как в Pin: один запрос к GitHub за последним выпуском, без входа и ключей.
 * О пользователе ничего не отправляется. Автопроверку можно выключить в настройках.
 * Найденный выпуск и ход ручной проверки — в [offer] и [check]: окна показывает AppRoot.
 */
class UpdateChecker(private val context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Новая версия, которую стоит предложить. */
    val offer = MutableStateFlow<ReleaseInfo?>(null)

    /** Окно ручной проверки: «Проверяем…», «Обновлений нет», «Не удалось». */
    val check = MutableStateFlow<UpdateCheckState?>(null)

    val currentVersion: String
        get() = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    /** При входе в приложение: не чаще раза в 6 часов, без пропущенной версии; ошибки молча. */
    suspend fun checkIfDue(now: Long = System.currentTimeMillis()) {
        if (!_enabled.value || !Updates.isDue(prefs.getLong(KEY_LAST, 0), now)) return
        val release = runCatching { fetch() }.getOrNull() ?: return
        prefs.edit().putLong(KEY_LAST, now).apply()
        if (Updates.shouldOffer(release, currentVersion, prefs.getString(KEY_SKIPPED, null))) offer.value = release
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
        prefs.edit().putString(KEY_SKIPPED, release.version).apply()
        offer.value = null
    }

    /** Открывает APK (его скачает браузер) или, если файла нет, страницу выпуска. */
    fun download(release: ReleaseInfo) {
        offer.value = null
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(release.apkUrl ?: release.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
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

package com.sonnik.topower.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.sonnik.topower.domain.Muscle
import com.sonnik.topower.domain.RecoveryNotifyMode
import com.sonnik.topower.domain.ReminderSettings
import com.sonnik.topower.ui.theme.ThemeMode
import com.sonnik.topower.ui.theme.ThemePalette

/** Настройки приложения в SharedPreferences с наблюдаемыми значениями. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _defaultRestSec = MutableStateFlow(prefs.getInt(KEY_REST, DEFAULT_REST_SEC))
    val defaultRestSec: StateFlow<Int> = _defaultRestSec.asStateFlow()

    private val _plateauEnabled = MutableStateFlow(prefs.getBoolean(KEY_PLATEAU_ON, true))
    val plateauEnabled: StateFlow<Boolean> = _plateauEnabled.asStateFlow()

    private val _plateauSessions = MutableStateFlow(prefs.getInt(KEY_PLATEAU_N, DEFAULT_PLATEAU_SESSIONS))
    val plateauSessions: StateFlow<Int> = _plateauSessions.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME_MODE, null) } ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _themePalette = MutableStateFlow(
        ThemePalette.available.firstOrNull { it.name == prefs.getString(KEY_THEME_PALETTE, null) } ?: ThemePalette.STEEL
    )
    val themePalette: StateFlow<ThemePalette> = _themePalette.asStateFlow()

    /** Женский силуэт на схемах мышц. */
    private val _femaleBody = MutableStateFlow(prefs.getBoolean(KEY_FEMALE_BODY, false))
    val femaleBody: StateFlow<Boolean> = _femaleBody.asStateFlow()

    /** Вопрос «пол для схемы мышц» уже задан (один раз — при первом запуске или после обновления). */
    private val _bodyTypeAsked = MutableStateFlow(prefs.getBoolean(KEY_BODY_ASKED, false))
    val bodyTypeAsked: StateFlow<Boolean> = _bodyTypeAsked.asStateFlow()

    /** Чёрный фон в тёмной теме (AMOLED). */
    private val _pureBlack = MutableStateFlow(prefs.getBoolean(KEY_PURE_BLACK, false))
    val pureBlack: StateFlow<Boolean> = _pureBlack.asStateFlow()

    private val _reminders = MutableStateFlow(readReminders())
    val reminders: StateFlow<ReminderSettings> = _reminders.asStateFlow()

    /** Ключи уже показанных уведомлений (см. [com.sonnik.topower.domain.plannedReminders]). */
    var notifiedRecovery: Long
        get() = prefs.getLong(KEY_NOTIFIED_RECOVERY, 0)
        set(value) = prefs.edit().putLong(KEY_NOTIFIED_RECOVERY, value).apply()

    var notifiedInactivity: Long
        get() = prefs.getLong(KEY_NOTIFIED_INACTIVITY, 0)
        set(value) = prefs.edit().putLong(KEY_NOTIFIED_INACTIVITY, value).apply()

    private fun readReminders(): ReminderSettings {
        val d = ReminderSettings()
        return ReminderSettings(
            recoveryOn = prefs.getBoolean(KEY_R_ON, d.recoveryOn),
            mode = RecoveryNotifyMode.entries.firstOrNull { it.name == prefs.getString(KEY_R_MODE, null) } ?: d.mode,
            groups = Muscle.parse(prefs.getString(KEY_R_GROUPS, "") ?: ""),
            dailyMinutes = prefs.getInt(KEY_R_DAILY, d.dailyMinutes),
            quietOn = prefs.getBoolean(KEY_R_QUIET_ON, d.quietOn),
            quietStart = prefs.getInt(KEY_R_QUIET_START, d.quietStart),
            quietEnd = prefs.getInt(KEY_R_QUIET_END, d.quietEnd),
            inactivityOn = prefs.getBoolean(KEY_R_IDLE_ON, d.inactivityOn),
            inactivityDays = prefs.getInt(KEY_R_IDLE_DAYS, d.inactivityDays),
        )
    }

    fun setReminders(value: ReminderSettings) {
        val v = value.copy(inactivityDays = value.inactivityDays.coerceIn(MIN_IDLE_DAYS, MAX_IDLE_DAYS))
        prefs.edit()
            .putBoolean(KEY_R_ON, v.recoveryOn)
            .putString(KEY_R_MODE, v.mode.name)
            .putString(KEY_R_GROUPS, Muscle.format(v.groups))
            .putInt(KEY_R_DAILY, v.dailyMinutes)
            .putBoolean(KEY_R_QUIET_ON, v.quietOn)
            .putInt(KEY_R_QUIET_START, v.quietStart)
            .putInt(KEY_R_QUIET_END, v.quietEnd)
            .putBoolean(KEY_R_IDLE_ON, v.inactivityOn)
            .putInt(KEY_R_IDLE_DAYS, v.inactivityDays)
            .apply()
        _reminders.value = v
    }

    /** Версия, новинки которой пользователь уже видел (окно «Что нового»). */
    var lastSeenVersion: String?
        get() = prefs.getString(KEY_LAST_SEEN_VERSION, null)
        set(value) = prefs.edit().putString(KEY_LAST_SEEN_VERSION, value).apply()

    /** Упражнения по умолчанию уже добавлены (или пользователь начал со своих). */
    var defaultsSeeded: Boolean
        get() = prefs.getBoolean(KEY_DEFAULTS_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULTS_SEEDED, value).apply()

    /** Номер последнего добавленного набора упражнений по умолчанию. */
    var exercisePack: Int
        get() = prefs.getInt(KEY_EXERCISE_PACK, 0)
        set(value) = prefs.edit().putInt(KEY_EXERCISE_PACK, value).apply()

    fun setDefaultRestSec(value: Int) {
        val v = value.coerceIn(MIN_REST_SEC, MAX_REST_SEC)
        prefs.edit().putInt(KEY_REST, v).apply()
        _defaultRestSec.value = v
    }

    fun setThemeMode(value: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        _themeMode.value = value
    }

    fun setThemePalette(value: ThemePalette) {
        prefs.edit().putString(KEY_THEME_PALETTE, value.name).apply()
        _themePalette.value = value
    }

    fun setFemaleBody(value: Boolean) {
        prefs.edit().putBoolean(KEY_FEMALE_BODY, value).putBoolean(KEY_BODY_ASKED, true).apply()
        _femaleBody.value = value
        _bodyTypeAsked.value = true
    }

    fun setBodyTypeAsked(value: Boolean) {
        prefs.edit().putBoolean(KEY_BODY_ASKED, value).apply()
        _bodyTypeAsked.value = value
    }

    fun setPureBlack(value: Boolean) {
        prefs.edit().putBoolean(KEY_PURE_BLACK, value).apply()
        _pureBlack.value = value
    }

    fun setPlateauEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PLATEAU_ON, value).apply()
        _plateauEnabled.value = value
    }

    fun setPlateauSessions(value: Int) {
        val v = value.coerceIn(MIN_PLATEAU_SESSIONS, MAX_PLATEAU_SESSIONS)
        prefs.edit().putInt(KEY_PLATEAU_N, v).apply()
        _plateauSessions.value = v
    }

    companion object {
        const val DEFAULT_REST_SEC = 90
        const val REST_STEP_SEC = 15
        const val MIN_REST_SEC = 15
        const val MAX_REST_SEC = 15 * 60
        const val DEFAULT_PLATEAU_SESSIONS = 7
        const val MIN_PLATEAU_SESSIONS = 2
        const val MAX_PLATEAU_SESSIONS = 30
        const val MIN_IDLE_DAYS = 1
        const val MAX_IDLE_DAYS = 30

        private const val KEY_REST = "default_rest_sec"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_PALETTE = "theme_palette"
        private const val KEY_PURE_BLACK = "theme_pure_black"
        private const val KEY_FEMALE_BODY = "female_body"
        private const val KEY_BODY_ASKED = "body_type_asked"
        private const val KEY_PLATEAU_ON = "plateau_enabled"
        private const val KEY_PLATEAU_N = "plateau_sessions"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        private const val KEY_DEFAULTS_SEEDED = "defaults_seeded"
        private const val KEY_EXERCISE_PACK = "exercise_pack"
        private const val KEY_R_ON = "reminder_recovery_on"
        private const val KEY_R_MODE = "reminder_recovery_mode"
        private const val KEY_R_GROUPS = "reminder_recovery_groups"
        private const val KEY_R_DAILY = "reminder_daily_minutes"
        private const val KEY_R_QUIET_ON = "reminder_quiet_on"
        private const val KEY_R_QUIET_START = "reminder_quiet_start"
        private const val KEY_R_QUIET_END = "reminder_quiet_end"
        private const val KEY_R_IDLE_ON = "reminder_idle_on"
        private const val KEY_R_IDLE_DAYS = "reminder_idle_days"
        private const val KEY_NOTIFIED_RECOVERY = "notified_recovery"
        private const val KEY_NOTIFIED_INACTIVITY = "notified_inactivity"
    }
}

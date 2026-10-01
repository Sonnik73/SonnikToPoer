package com.sonnik.topower.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Настройки приложения в SharedPreferences с наблюдаемыми значениями. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _defaultRestSec = MutableStateFlow(prefs.getInt(KEY_REST, DEFAULT_REST_SEC))
    val defaultRestSec: StateFlow<Int> = _defaultRestSec.asStateFlow()

    private val _plateauEnabled = MutableStateFlow(prefs.getBoolean(KEY_PLATEAU_ON, true))
    val plateauEnabled: StateFlow<Boolean> = _plateauEnabled.asStateFlow()

    private val _plateauSessions = MutableStateFlow(prefs.getInt(KEY_PLATEAU_N, DEFAULT_PLATEAU_SESSIONS))
    val plateauSessions: StateFlow<Int> = _plateauSessions.asStateFlow()

    fun setDefaultRestSec(value: Int) {
        val v = value.coerceIn(MIN_REST_SEC, MAX_REST_SEC)
        prefs.edit().putInt(KEY_REST, v).apply()
        _defaultRestSec.value = v
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

        private const val KEY_REST = "default_rest_sec"
        private const val KEY_PLATEAU_ON = "plateau_enabled"
        private const val KEY_PLATEAU_N = "plateau_sessions"
    }
}

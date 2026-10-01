package com.sonnik.topower

import android.app.Application
import com.sonnik.topower.data.AppDatabase
import com.sonnik.topower.data.AppSettings
import com.sonnik.topower.data.Repository
import com.sonnik.topower.timer.RestTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class SonnikApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val repository: Repository by lazy { Repository(AppDatabase.build(this), appScope) }
    val settings: AppSettings by lazy { AppSettings(this) }
    val restTimer: RestTimer by lazy { RestTimer(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.seedDefaultsIfNeeded(settings, russian = Locale.getDefault().language == "ru")
        }
    }
}

package com.sonnik.topower

import android.app.Application
import com.sonnik.topower.data.AppDatabase
import com.sonnik.topower.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SonnikApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val repository: Repository by lazy { Repository(AppDatabase.build(this), appScope) }
}

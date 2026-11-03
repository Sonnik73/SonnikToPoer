package com.sonnik.topower

import android.content.Context
import com.sonnik.topower.backup.BackupStore
import com.sonnik.topower.backup.DriveAuth
import com.sonnik.topower.backup.DriveRest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Сборка full (GitHub, IzzyOnDroid): вход в Google Диск через сервисы Google Play и проверка обновлений. */
object Flavor {
    /** Проверять обновления на GitHub — у F-Droid обновлениями занимается его клиент. */
    const val UPDATE_CHECKS = true

    /** Копии по датам на Google Диске с токеном входа в Google. */
    fun driveStore(context: Context): suspend () -> BackupStore = {
        val token = withContext(Dispatchers.IO) { DriveAuth.tokenBlocking(context) }
        DriveRest(token, context.getString(R.string.drive_sign_in_again))
    }
}

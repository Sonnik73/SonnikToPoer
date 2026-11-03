package com.sonnik.topower

import android.content.Context
import com.sonnik.topower.backup.BackupStore

/**
 * Сборка для F-Droid: без сервисов Google Play и без сети. Входа в Google и проверки обновлений нет —
 * обновляет клиент F-Droid, а копию на Google Диск можно хранить файлом через окно «Сохранить в…».
 */
object Flavor {
    const val UPDATE_CHECKS = false

    fun driveStore(context: Context): suspend () -> BackupStore = {
        throw UnsupportedOperationException("Google sign-in is not available in the F-Droid build")
    }
}

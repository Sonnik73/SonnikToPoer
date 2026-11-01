package com.sonnik.topower.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sonnik.topower.SonnikApp
import java.util.concurrent.TimeUnit

/** Автокопия на Google Диск в фоне при наличии сети, если данные менялись. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ok = (applicationContext as SonnikApp).driveBackup.backupIfChanged()
        // Сбой сети — повторим попозже; ошибку входа повтор не исправит, она видна в настройках.
        return if (ok || runAttemptCount >= 3) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC = "drive-backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(BackupPolicy.AUTO_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        }
    }
}

package com.sonnik.topower.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sonnik.topower.SonnikApp
import java.util.concurrent.TimeUnit

/** Запись копии в файл в фоне: вскоре после изменений и раз в несколько часов на всякий случай. */
class FileBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ok = (applicationContext as SonnikApp).fileBackup.write()
        return if (ok || runAttemptCount >= 2) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC = "file-backup-periodic"
        private const val SOON = "file-backup-soon"

        /** Через сколько секунд после изменения писать: подходы подряд схлопываются в одну запись. */
        const val DELAY_SECONDS = 60L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FileBackupWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun requestSoon(context: Context, delaySeconds: Long = DELAY_SECONDS) {
            val request = OneTimeWorkRequestBuilder<FileBackupWorker>().setInitialDelay(delaySeconds, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(SOON, ExistingWorkPolicy.REPLACE, request)
        }

        /** Подключает файл и сразу пишет в него копию (если [writeNow]). */
        fun attach(context: Context, uri: Uri, writeNow: Boolean = true) {
            (context.applicationContext as SonnikApp).fileBackup.attach(uri)
            schedule(context)
            if (writeNow) requestSoon(context, delaySeconds = 0)
        }

        fun detach(context: Context) {
            (context.applicationContext as SonnikApp).fileBackup.detach()
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(SOON)
        }
    }
}

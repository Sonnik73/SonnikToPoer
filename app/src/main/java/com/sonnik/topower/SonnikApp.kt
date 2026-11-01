package com.sonnik.topower

import android.app.Application
import com.sonnik.topower.backup.BackupWorker
import com.sonnik.topower.backup.DriveBackup
import com.sonnik.topower.backup.FileBackup
import com.sonnik.topower.backup.FileBackupWorker
import com.sonnik.topower.data.AppDatabase
import com.sonnik.topower.data.AppSettings
import com.sonnik.topower.data.Repository
import com.sonnik.topower.ecosystem.EcosystemChanges
import com.sonnik.topower.reminders.Reminders
import com.sonnik.topower.reminders.muscleStatuses
import com.sonnik.topower.timer.RestTimer
import com.sonnik.topower.updates.UpdateChecker
import com.sonnik.topower.widget.MuscleWidget
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    val reminders: Reminders by lazy { Reminders(this, settings) }
    val updates: UpdateChecker by lazy { UpdateChecker(this) }
    val driveBackup: DriveBackup by lazy { DriveBackup(this, repository) }
    val fileBackup: FileBackup by lazy { FileBackup(this, repository) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.seedDefaultsIfNeeded(settings, russian = Locale.getDefault().language == "ru")
        }
        // Автокопии на Диск: расписание восстанавливается, если пользователь их включил.
        if (driveBackup.state.value.connected) BackupWorker.schedule(this)
        if (fileBackup.state.value.enabled) FileBackupWorker.schedule(this)
        // Приложения экосистемы обновляются сами после любого изменения тренировок.
        // Файл копии на Диске перезаписывается вскоре после изменений.
        repository.observeTrainingChanges {
            EcosystemChanges.onDataChanged(this)
            if (fileBackup.state.value.enabled) FileBackupWorker.requestSoon(this)
        }
        // Уведомления перепланируются при любом изменении тренировок или их настроек.
        appScope.launch {
            combine(
                settings.reminders,
                repository.dao.observeMuscleWork(),
                repository.dao.observeLastWorkoutStart(),
            ) { _, rows, last -> muscleStatuses(rows) to last }
                .collectLatest { (statuses, last) ->
                    reminders.reschedule(statuses, last)
                    MuscleWidget.update(this@SonnikApp, statuses)
                }
        }
    }
}

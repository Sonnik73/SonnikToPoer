package com.sonnik.topower.backup

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Копия в папке «SonnikToPower» на Диске. */
data class RemoteBackup(val id: String, val name: String, val modified: Long, val bytes: Long?)

/** Где лежат копии: Google Диск или (в тестах) память. */
interface BackupStore {
    /** Копии, от новой к старой. */
    suspend fun list(): List<RemoteBackup>
    suspend fun upload(name: String, json: String): RemoteBackup
    suspend fun download(backup: RemoteBackup): String
    suspend fun delete(backup: RemoteBackup)
}

/** Правила копий: имя файла, сколько хранить, когда пора делать новую. */
object BackupPolicy {
    /** Столько последних копий храним на Диске, более старые удаляем. */
    const val KEEP = 10

    /** Автокопия — не чаще, чем раз в столько, и только если данные менялись. */
    const val AUTO_INTERVAL_HOURS = 12L

    const val PREFIX = "SonnikToPower-backup-"

    fun fileName(now: LocalDateTime): String =
        PREFIX + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".json"

    /** Копии сверх [keep] самых новых — на удаление. Чужие файлы (не по [PREFIX]) не трогаем. */
    fun toDelete(backups: List<RemoteBackup>, keep: Int = KEEP): List<RemoteBackup> =
        backups.filter { it.name.startsWith(PREFIX) }.sortedByDescending { it.modified }.drop(keep)

    /** Нужна ли автокопия: данные менялись после последней успешной копии. */
    fun autoDue(lastBackupAt: Long, lastChangedAt: Long): Boolean = lastChangedAt > lastBackupAt
}

/** Нужен повторный вход в Google (доступ отозван или истёк). */
class NeedsSignInException(message: String) : Exception(message)

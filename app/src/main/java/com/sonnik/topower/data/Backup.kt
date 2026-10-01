package com.sonnik.topower.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val BACKUP_APP_ID = "SonnikToPower"
const val BACKUP_FORMAT_VERSION = 2

@Serializable
data class BackupFile(
    val app: String = BACKUP_APP_ID,
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val exportedAt: Long = now(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
    val sets: List<SetEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val templateExercises: List<TemplateExerciseEntity> = emptyList(),
    val equipment: List<EquipmentEntity> = emptyList(),
)

class InvalidBackupException(message: String) : Exception(message)

object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    fun decode(text: String): BackupFile {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw InvalidBackupException("Not a valid backup file: ${e.message}")
        }
        if (backup.app != BACKUP_APP_ID) throw InvalidBackupException("Unknown backup source: ${backup.app}")
        if (backup.formatVersion > BACKUP_FORMAT_VERSION) {
            throw InvalidBackupException("Backup was made by a newer app version")
        }
        return backup
    }
}

package com.sonnik.topower.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sonnik.topower.domain.Muscle

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetEntity::class,
        TemplateEntity::class,
        TemplateExerciseEntity::class,
        EquipmentEntity::class,
        MeasurementEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        const val NAME = "sonnik_to_power.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}

/** v2: инвентарь, тип инвентаря у упражнения, выбранный снаряд в подходе. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `equipment` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, `weight` REAL, `minResistance` REAL, `maxResistance` REAL, " +
                "`color` INTEGER, `archived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `equipmentType` TEXT")
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `bandAssist` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `sets` ADD COLUMN `equipmentId` TEXT")
        db.execSQL("ALTER TABLE `sets` ADD COLUMN `equipment2Id` TEXT")
        db.execSQL("ALTER TABLE `sets` ADD COLUMN `pair` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `sets` ADD COLUMN `bandMin` REAL")
    }
}

/** v3: своё время отдыха у упражнения. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `restSec` INTEGER")
    }
}

/** v4: данные со смарт-часов у тренировки. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `maxHeartRate` INTEGER")
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `avgHeartRate` INTEGER")
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `watchDurationSec` INTEGER")
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `calories` INTEGER")
    }
}

/** v5: основные мышцы упражнения и срок восстановления после упражнения. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `muscles` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `workout_exercises` ADD COLUMN `recoveryHours` INTEGER")
        // Уже созданным упражнениям проставляем мышцы по известным названиям.
        MUSCLES_BY_NAME.forEach { (name, muscles) ->
            db.execSQL(
                "UPDATE `exercises` SET `muscles` = ? WHERE `name` = ? AND `muscles` = ''",
                arrayOf<Any>(Muscle.format(muscles), name),
            )
        }
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `repMin` INTEGER")
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `repMax` INTEGER")
    }
}

/** v7: техника выполнения упражнения; тексты по умолчанию проставляет [Repository.seedDefaultsIfNeeded]. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `technique` TEXT NOT NULL DEFAULT ''")
    }
}

/** v8: замеры тела (обхваты в сантиметрах с датой). */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `measurements` (`id` TEXT NOT NULL, `takenAt` INTEGER NOT NULL, " +
                "`part` TEXT NOT NULL, `valueCm` REAL NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_measurements_takenAt` ON `measurements` (`takenAt`)")
    }
}

val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)

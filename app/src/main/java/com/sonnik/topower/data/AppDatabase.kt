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
    ],
    version = 5,
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

val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

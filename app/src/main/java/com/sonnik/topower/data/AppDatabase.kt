package com.sonnik.topower.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 3,
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

val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

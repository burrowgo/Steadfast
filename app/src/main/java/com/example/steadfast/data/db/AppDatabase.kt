package com.example.steadfast.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, StreakEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun streakDao(): StreakDao

    companion object {
        private const val DB_NAME = "steadfast.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create habit table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // 2. If streak table exists and has rows, insert default habit with id = 1
                db.execSQL(
                    """
                    INSERT INTO `habit` (`id`, `name`, `icon`, `color`, `createdAt`, `isArchived`, `sortOrder`)
                    SELECT 1,
                           COALESCE((SELECT `habitName` FROM `streak` WHERE `habitName` IS NOT NULL AND `habitName` != '' ORDER BY `id` DESC LIMIT 1), 'Steadfast'),
                           'shield',
                           4283204907,
                           strftime('%s', 'now') * 1000,
                           0,
                           0
                    WHERE EXISTS (SELECT 1 FROM `streak`);
                    """.trimIndent()
                )

                // 3. Recreate streak table to ensure foreign keys and indices match Room schema
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `streak_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `habitName` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        `endDate` INTEGER,
                        `lengthDays` INTEGER,
                        `reason` TEXT,
                        FOREIGN KEY(`habitId`) REFERENCES `habit`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // 4. Copy existing data into streak_new with habitId = 1
                db.execSQL(
                    """
                    INSERT INTO `streak_new` (`id`, `habitId`, `habitName`, `startDate`, `startedAt`, `endedAt`, `endDate`, `lengthDays`, `reason`)
                    SELECT `id`, 1, `habitName`, `startDate`, `startedAt`, `endedAt`, `endDate`, `lengthDays`, `reason`
                    FROM `streak`
                    """.trimIndent()
                )

                // 5. Drop old table and rename
                db.execSQL("DROP TABLE `streak`")
                db.execSQL("ALTER TABLE `streak_new` RENAME TO `streak`")

                // 6. Create index on habitId
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_streak_habitId` ON `streak` (`habitId`)")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
        }
    }
}

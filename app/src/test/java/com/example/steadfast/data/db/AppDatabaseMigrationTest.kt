package com.example.steadfast.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    @Test
    fun migrate1To2_withExistingStreaks_preservesDataAndCreatesHabit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_with_data.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `streak` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `habitName` TEXT NOT NULL,
                                `startDate` INTEGER NOT NULL,
                                `startedAt` INTEGER NOT NULL,
                                `endedAt` INTEGER,
                                `endDate` INTEGER,
                                `lengthDays` INTEGER,
                                `reason` TEXT
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = helper.writableDatabase

        // Insert mock v1 streaks
        db.execSQL(
            """
            INSERT INTO `streak` (`id`, `habitName`, `startDate`, `startedAt`, `endedAt`, `endDate`, `lengthDays`, `reason`)
            VALUES (1, 'No Sugar', 19000, 1600000000000, 1600086400000, 19001, 1, 'Forgot')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `streak` (`id`, `habitName`, `startDate`, `startedAt`, `endedAt`, `endDate`, `lengthDays`, `reason`)
            VALUES (2, 'No Sugar', 19001, 1600086400000, NULL, NULL, NULL, NULL)
            """.trimIndent()
        )

        // Run Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify habit table was created and populated
        val habitCursor = db.query("SELECT id, name, icon, isArchived FROM habit")
        assertTrue(habitCursor.moveToFirst())
        assertEquals(1L, habitCursor.getLong(0))
        assertEquals("No Sugar", habitCursor.getString(1))
        assertEquals("shield", habitCursor.getString(2))
        assertEquals(0, habitCursor.getInt(3))
        assertEquals(1, habitCursor.count)
        habitCursor.close()

        // Verify streak table has habitId and preserved data
        val streakCursor = db.query("SELECT id, habitId, habitName, startDate, lengthDays, reason FROM streak ORDER BY id ASC")
        assertTrue(streakCursor.moveToFirst())
        assertEquals(1L, streakCursor.getLong(0))
        assertEquals(1L, streakCursor.getLong(1))
        assertEquals("No Sugar", streakCursor.getString(2))
        assertEquals(19000L, streakCursor.getLong(3))
        assertEquals(1, streakCursor.getInt(4))
        assertEquals("Forgot", streakCursor.getString(5))

        assertTrue(streakCursor.moveToNext())
        assertEquals(2L, streakCursor.getLong(0))
        assertEquals(1L, streakCursor.getLong(1))
        assertEquals("No Sugar", streakCursor.getString(2))
        assertEquals(19001L, streakCursor.getLong(3))
        assertTrue(streakCursor.isNull(4))
        assertTrue(streakCursor.isNull(5))
        streakCursor.close()

        db.close()
    }

    @Test
    fun migrate1To2_withEmptyDatabase_createsSchemaWithoutError() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_empty.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `streak` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `habitName` TEXT NOT NULL,
                                `startDate` INTEGER NOT NULL,
                                `startedAt` INTEGER NOT NULL,
                                `endedAt` INTEGER,
                                `endDate` INTEGER,
                                `lengthDays` INTEGER,
                                `reason` TEXT
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = helper.writableDatabase

        // Run Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify habit table exists and is empty
        val habitCursor = db.query("SELECT COUNT(*) FROM habit")
        assertTrue(habitCursor.moveToFirst())
        assertEquals(0, habitCursor.getInt(0))
        habitCursor.close()

        // Verify streak table exists and is empty
        val streakCursor = db.query("SELECT COUNT(*) FROM streak")
        assertTrue(streakCursor.moveToFirst())
        assertEquals(0, streakCursor.getInt(0))
        streakCursor.close()

        db.close()
    }
}

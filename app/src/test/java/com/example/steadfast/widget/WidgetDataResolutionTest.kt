package com.example.steadfast.widget

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.data.db.HabitEntity
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.WidgetConfigurationRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class WidgetDataResolutionTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ─── resolveForHabitId tests ───────────────────────────────────────

    @Test
    fun `resolveForHabitId returns correct habit and streak for specific habitId`() = runTest {
        val habit1 = HabitEntity(id = 1, name = "Exercise")
        val habit2 = HabitEntity(id = 2, name = "Reading")
        database.habitDao().insert(habit1)
        database.habitDao().insert(habit2)

        val today = LocalDate.now().toEpochDay()
        database.streakDao().insert(StreakEntity(habitId = 1, habitName = "Exercise", startDate = today - 10, startedAt = System.currentTimeMillis()))
        database.streakDao().insert(StreakEntity(habitId = 2, habitName = "Reading", startDate = today - 5, startedAt = System.currentTimeMillis()))

        val (resolvedHabit1, resolvedStreak1) = SteadfastWidget.resolveForHabitId(database, 1L)
        assertEquals("Exercise", resolvedHabit1?.name)
        assertEquals(1L, resolvedStreak1?.habitId)
        assertEquals(today - 10, resolvedStreak1?.startDate)

        val (resolvedHabit2, resolvedStreak2) = SteadfastWidget.resolveForHabitId(database, 2L)
        assertEquals("Reading", resolvedHabit2?.name)
        assertEquals(2L, resolvedStreak2?.habitId)
        assertEquals(today - 5, resolvedStreak2?.startDate)
    }

    @Test
    fun `resolveForHabitId with three habits returns distinct data for each`() = runTest {
        // This is the critical test for the bug: three different habits
        // must resolve to three different streak data sets
        val habit1 = HabitEntity(id = 1, name = "Meditation")
        val habit2 = HabitEntity(id = 2, name = "Exercise")
        val habit3 = HabitEntity(id = 3, name = "Reading")
        database.habitDao().insert(habit1)
        database.habitDao().insert(habit2)
        database.habitDao().insert(habit3)

        val today = LocalDate.now().toEpochDay()
        database.streakDao().insert(StreakEntity(habitId = 1, habitName = "Meditation", startDate = today - 30, startedAt = System.currentTimeMillis()))
        database.streakDao().insert(StreakEntity(habitId = 2, habitName = "Exercise", startDate = today - 15, startedAt = System.currentTimeMillis()))
        database.streakDao().insert(StreakEntity(habitId = 3, habitName = "Reading", startDate = today - 5, startedAt = System.currentTimeMillis()))

        val (h1, s1) = SteadfastWidget.resolveForHabitId(database, 1L)
        val (h2, s2) = SteadfastWidget.resolveForHabitId(database, 2L)
        val (h3, s3) = SteadfastWidget.resolveForHabitId(database, 3L)

        // Each resolution must return the CORRECT habit and streak, not the first one
        assertEquals("Meditation", h1?.name)
        assertEquals(1L, s1?.habitId)
        assertEquals(today - 30, s1?.startDate)

        assertEquals("Exercise", h2?.name)
        assertEquals(2L, s2?.habitId)
        assertEquals(today - 15, s2?.startDate)

        assertEquals("Reading", h3?.name)
        assertEquals(3L, s3?.habitId)
        assertEquals(today - 5, s3?.startDate)

        // Verify they are ALL DIFFERENT (the original bug was all returning same data)
        assertNotNull(s1)
        assertNotNull(s2)
        assertNotNull(s3)
        assert(s1!!.startDate != s2!!.startDate) { "Streak 1 and 2 should have different start dates" }
        assert(s2.startDate != s3!!.startDate) { "Streak 2 and 3 should have different start dates" }
        assert(s1.startDate != s3.startDate) { "Streak 1 and 3 should have different start dates" }
    }

    @Test
    fun `resolveForHabitId with null habitId falls back to first habit`() = runTest {
        val habit1 = HabitEntity(id = 1, name = "First", sortOrder = 0)
        val habit2 = HabitEntity(id = 2, name = "Second", sortOrder = 1)
        database.habitDao().insert(habit1)
        database.habitDao().insert(habit2)

        val today = LocalDate.now().toEpochDay()
        database.streakDao().insert(StreakEntity(habitId = 1, habitName = "First", startDate = today, startedAt = System.currentTimeMillis()))

        val (habit, streak) = SteadfastWidget.resolveForHabitId(database, null)
        assertEquals("First", habit?.name)
        assertEquals(1L, streak?.habitId)
    }

    @Test
    fun `resolveForHabitId with deleted habitId falls back to first habit`() = runTest {
        val habit1 = HabitEntity(id = 1, name = "Existing")
        database.habitDao().insert(habit1)

        val today = LocalDate.now().toEpochDay()
        database.streakDao().insert(StreakEntity(habitId = 1, habitName = "Existing", startDate = today, startedAt = System.currentTimeMillis()))

        // Resolve for a habit ID that doesn't exist
        val (habit, streak) = SteadfastWidget.resolveForHabitId(database, 999L)
        assertEquals("Existing", habit?.name) // Falls back to first
        assertEquals(1L, streak?.habitId)
    }

    @Test
    fun `resolveForHabitId with no habits returns nulls`() = runTest {
        val (habit, streak) = SteadfastWidget.resolveForHabitId(database, null)
        assertNull(habit)
        assertNull(streak)
    }

    @Test
    fun `resolveForHabitId returns null streak when habit exists but no active streak`() = runTest {
        val habit = HabitEntity(id = 1, name = "NoStreak")
        database.habitDao().insert(habit)

        val (resolvedHabit, resolvedStreak) = SteadfastWidget.resolveForHabitId(database, 1L)
        assertEquals("NoStreak", resolvedHabit?.name)
        assertNull(resolvedStreak)
    }

    // ─── Widget Configuration + Resolution integration tests ──────────

    @Test
    fun `three widgets configured with different habits resolve to different data`() = runTest {
        // This simulates the exact user scenario:
        // Widget 1 -> Habit A, Widget 2 -> Habit B, Widget 3 -> Habit C
        val widgetConfigRepo = WidgetConfigurationRepository(context)

        val habit1 = HabitEntity(id = 1, name = "Meditation")
        val habit2 = HabitEntity(id = 2, name = "Exercise")
        val habit3 = HabitEntity(id = 3, name = "Reading")
        database.habitDao().insert(habit1)
        database.habitDao().insert(habit2)
        database.habitDao().insert(habit3)

        val today = LocalDate.now().toEpochDay()
        database.streakDao().insert(StreakEntity(habitId = 1, habitName = "Meditation", startDate = today - 100, startedAt = System.currentTimeMillis()))
        database.streakDao().insert(StreakEntity(habitId = 2, habitName = "Exercise", startDate = today - 50, startedAt = System.currentTimeMillis()))
        database.streakDao().insert(StreakEntity(habitId = 3, habitName = "Reading", startDate = today - 10, startedAt = System.currentTimeMillis()))

        // Configure 3 widgets
        widgetConfigRepo.setHabitIdForWidget(100, 1L) // Widget 100 -> Meditation
        widgetConfigRepo.setHabitIdForWidget(101, 2L) // Widget 101 -> Exercise
        widgetConfigRepo.setHabitIdForWidget(102, 3L) // Widget 102 -> Reading

        // Simulate what WidgetUpdater does: resolve per widget
        val habitId1 = widgetConfigRepo.getHabitIdForWidget(100)
        val habitId2 = widgetConfigRepo.getHabitIdForWidget(101)
        val habitId3 = widgetConfigRepo.getHabitIdForWidget(102)

        val (h1, s1) = SteadfastWidget.resolveForHabitId(database, habitId1)
        val (h2, s2) = SteadfastWidget.resolveForHabitId(database, habitId2)
        val (h3, s3) = SteadfastWidget.resolveForHabitId(database, habitId3)

        // The critical assertion: each widget gets its OWN habit data
        assertEquals("Meditation", h1?.name)
        assertEquals(today - 100, s1?.startDate)

        assertEquals("Exercise", h2?.name)
        assertEquals(today - 50, s2?.startDate)

        assertEquals("Reading", h3?.name)
        assertEquals(today - 10, s3?.startDate)
    }

    @Test
    fun `widget config returns correct habitId per widget after multiple saves`() = runTest {
        val widgetConfigRepo = WidgetConfigurationRepository(context)

        // Save different habits for different widget IDs
        widgetConfigRepo.setHabitIdForWidget(1, 10L)
        widgetConfigRepo.setHabitIdForWidget(2, 20L)
        widgetConfigRepo.setHabitIdForWidget(3, 30L)

        assertEquals(10L, widgetConfigRepo.getHabitIdForWidget(1))
        assertEquals(20L, widgetConfigRepo.getHabitIdForWidget(2))
        assertEquals(30L, widgetConfigRepo.getHabitIdForWidget(3))

        // Update one widget
        widgetConfigRepo.setHabitIdForWidget(2, 25L)
        assertEquals(10L, widgetConfigRepo.getHabitIdForWidget(1))
        assertEquals(25L, widgetConfigRepo.getHabitIdForWidget(2))
        assertEquals(30L, widgetConfigRepo.getHabitIdForWidget(3))
    }

    @Test
    fun `only active streaks are resolved, ended streaks are ignored`() = runTest {
        val habit = HabitEntity(id = 1, name = "Test")
        database.habitDao().insert(habit)

        val today = LocalDate.now().toEpochDay()
        // Ended streak
        database.streakDao().insert(StreakEntity(
            habitId = 1, habitName = "Test",
            startDate = today - 100, startedAt = System.currentTimeMillis(),
            endedAt = System.currentTimeMillis(), endDate = today - 50, lengthDays = 50
        ))
        // Active streak
        database.streakDao().insert(StreakEntity(
            habitId = 1, habitName = "Test",
            startDate = today - 5, startedAt = System.currentTimeMillis()
        ))

        val (_, streak) = SteadfastWidget.resolveForHabitId(database, 1L)
        assertNotNull(streak)
        assertEquals(today - 5, streak?.startDate) // Should be the active one
        assertNull(streak?.endedAt) // Must be active (no end date)
    }
}

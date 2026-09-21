package com.example.steadfast.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.steadfast.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class HabitRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var habitRepository: HabitRepository
    private lateinit var streakRepository: StreakRepository

    private val testClock = StreakRepositoryTest.MutableClock(
        Instant.parse("2026-09-22T10:00:00Z"),
        ZoneId.of("UTC")
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        habitRepository = HabitRepository(database.habitDao(), database.streakDao(), testClock)
        streakRepository = StreakRepository(database.streakDao(), testClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createHabit_createsHabitAndActiveStreak() = runTest {
        val id = habitRepository.createHabit(
            name = "Reading",
            icon = "book",
            color = 0xFF123456L,
            startDate = LocalDate.of(2026, 9, 22)
        )

        assertTrue(id > 0)
        val habit = habitRepository.getHabitById(id)
        assertNotNull(habit)
        assertEquals("Reading", habit?.name)
        assertEquals("book", habit?.icon)
        assertEquals(0xFF123456L, habit?.color)

        val activeStreak = streakRepository.getActiveStreak(id)
        assertNotNull(activeStreak)
        assertEquals(id, activeStreak?.habitId)
        assertEquals("Reading", activeStreak?.habitName)
        assertEquals(LocalDate.of(2026, 9, 22).toEpochDay(), activeStreak?.startDate)
    }

    @Test
    fun multipleHabits_trackIndependentStreaks() = runTest {
        val habit1Id = habitRepository.createHabit("Habit 1", startDate = LocalDate.of(2026, 9, 15))
        val habit2Id = habitRepository.createHabit("Habit 2", startDate = LocalDate.of(2026, 9, 20))

        val habitsWithStreaks = habitRepository.activeHabitsWithStreaks.first()
        assertEquals(2, habitsWithStreaks.size)

        val item1 = habitsWithStreaks.first { it.habit.id == habit1Id }
        val item2 = habitsWithStreaks.first { it.habit.id == habit2Id }

        assertEquals(7, item1.currentStreakDays) // Sep 15 to Sep 22
        assertEquals(2, item2.currentStreakDays) // Sep 20 to Sep 22

        // Reset habit 1 only
        streakRepository.resetStreak(habit1Id, "Fell behind")

        val updatedItems = habitRepository.activeHabitsWithStreaks.first()
        val updated1 = updatedItems.first { it.habit.id == habit1Id }
        val updated2 = updatedItems.first { it.habit.id == habit2Id }

        assertEquals(0, updated1.currentStreakDays) // Reset to today
        assertEquals(2, updated2.currentStreakDays) // Habit 2 is unaffected!
        assertEquals(2, updated1.totalAttempts)
        assertEquals(1, updated2.totalAttempts)
    }

    @Test
    fun archiveAndUnarchive_togglesVisibility() = runTest {
        val id = habitRepository.createHabit("Meditation")

        assertEquals(1, habitRepository.activeHabits.first().size)
        assertEquals(0, habitRepository.archivedHabits.first().size)

        habitRepository.setArchived(id, true)
        assertEquals(0, habitRepository.activeHabits.first().size)
        assertEquals(1, habitRepository.archivedHabits.first().size)

        habitRepository.setArchived(id, false)
        assertEquals(1, habitRepository.activeHabits.first().size)
        assertEquals(0, habitRepository.archivedHabits.first().size)
    }

    @Test
    fun deleteHabit_cascadesAndRemovesStreaks() = runTest {
        val id = habitRepository.createHabit("Temporary Habit")
        assertNotNull(streakRepository.getActiveStreak(id))

        habitRepository.deleteHabit(id)

        assertNull(habitRepository.getHabitById(id))
        assertNull(streakRepository.getActiveStreak(id))
    }
}

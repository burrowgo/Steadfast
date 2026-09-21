package com.example.steadfast.ui.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.steadfast.data.HabitRepository
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.AppDatabase
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var streakRepository: StreakRepository
    private lateinit var habitRepository: HabitRepository
    private val clock = Clock.fixed(Instant.parse("2026-09-22T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        streakRepository = StreakRepository(database.streakDao(), clock)
        habitRepository = HabitRepository(database.habitDao(), database.streakDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initial state shows all items and allows filtering by habit`() = runTest {
        val habitId1 = habitRepository.createHabit("Habit 1")
        val habitId2 = habitRepository.createHabit("Habit 2")

        streakRepository.resetStreak(habitId1, "Slipped on 1")
        streakRepository.resetStreak(habitId2, "Slipped on 2")

        val viewModel = HistoryViewModel(
            streakRepository = streakRepository,
            habitRepository = habitRepository,
            clock = clock,
            coroutineScope = backgroundScope,
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )

        // All habits initially
        val allState = viewModel.uiState.filter { it.habits.isNotEmpty() }.first()
        assertEquals(2, allState.items.size)
        assertEquals(2, allState.habits.size)
        assertNull(allState.selectedHabitId)

        // Filter by Habit 1
        viewModel.selectHabit(habitId1)
        val habit1State = viewModel.uiState.filter { it.selectedHabitId == habitId1 }.first()
        assertEquals(1, habit1State.items.size)
        assertEquals("Habit 1", habit1State.items.first().habitName)
        assertEquals(habitId1, habit1State.selectedHabitId)
    }
}

package com.example.steadfast.ui.ranks

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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class RanksViewModelTest {

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
    fun `ranks view model switches between active habits`() = runTest {
        val habitId1 = habitRepository.createHabit("Habit 1")
        val habitId2 = habitRepository.createHabit("Habit 2")

        val viewModel = RanksViewModel(
            streakRepository = streakRepository,
            habitRepository = habitRepository,
            clock = clock,
            coroutineScope = backgroundScope,
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )

        // Default selects first habit
        val initialState = viewModel.uiState.filter { it.habits.isNotEmpty() }.first()
        assertEquals(2, initialState.habits.size)
        assertNotNull(initialState.selectedHabit)
        assertEquals(habitId1, initialState.selectedHabit?.id)

        // Select habit 2
        viewModel.selectHabit(habitId2)
        val habit2State = viewModel.uiState.filter { it.selectedHabit?.id == habitId2 }.first()
        assertEquals(habitId2, habit2State.selectedHabit?.id)
    }
}

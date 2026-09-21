package com.example.steadfast.ui.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.steadfast.data.HabitRepository
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.WidgetConfigurationRepository
import com.example.steadfast.data.prefs.dataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var streakRepository: StreakRepository
    private lateinit var habitRepository: HabitRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var widgetConfigRepo: WidgetConfigurationRepository
    private lateinit var context: Context
    private val clock = Clock.fixed(Instant.parse("2026-09-22T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        streakRepository = StreakRepository(database.streakDao(), clock)
        habitRepository = HabitRepository(database.habitDao(), database.streakDao(), clock)
        settingsRepository = SettingsRepository(context.dataStore)
        widgetConfigRepo = WidgetConfigurationRepository(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `uiState displays all active habits`() = runTest {
        habitRepository.createHabit("Habit 1")
        habitRepository.createHabit("Habit 2")

        val viewModel = SettingsViewModel(
            streakRepository = streakRepository,
            settingsRepository = settingsRepository,
            context = context,
            habitRepository = habitRepository,
            widgetConfigurationRepository = widgetConfigRepo,
            coroutineScope = backgroundScope,
            sharingStarted = SharingStarted.Eagerly
        )

        val state = viewModel.uiState.filter { it.habits.size == 2 }.first()
        assertEquals(2, state.habits.size)
        assertEquals("Habit 1", state.habits[0].habit.name)
        assertEquals("Habit 2", state.habits[1].habit.name)
    }

    @Test
    fun `updateHabit updates details and start date for specified habit`() = runTest {
        val habitId1 = habitRepository.createHabit("Reading")
        val habitId2 = habitRepository.createHabit("Running")

        val viewModel = SettingsViewModel(
            streakRepository = streakRepository,
            settingsRepository = settingsRepository,
            context = context,
            habitRepository = habitRepository,
            widgetConfigurationRepository = widgetConfigRepo,
            coroutineScope = backgroundScope,
            sharingStarted = SharingStarted.Eagerly
        )

        viewModel.uiState.filter { it.habits.size == 2 }.first()

        val newStartDate = LocalDate.of(2026, 9, 10)
        viewModel.updateHabit(
            id = habitId2,
            newName = "Jogging",
            icon = "flame",
            color = 0xFFFF5722L,
            startDate = newStartDate
        )

        val updatedState = viewModel.uiState.filter {
            it.habits.any { h ->
                h.habit.id == habitId2 &&
                h.habit.name == "Jogging" &&
                h.activeStreak?.startDate == newStartDate.toEpochDay()
            }
        }.first()

        val runningHabit = updatedState.habits.first { it.habit.id == habitId2 }
        assertEquals("Jogging", runningHabit.habit.name)
        assertEquals("flame", runningHabit.habit.icon)
        assertEquals(0xFFFF5722L, runningHabit.habit.color)
        assertEquals(newStartDate.toEpochDay(), runningHabit.activeStreak?.startDate)
    }

    @Test
    fun `createHabit adds a new habit to settings`() = runTest {
        val viewModel = SettingsViewModel(
            streakRepository = streakRepository,
            settingsRepository = settingsRepository,
            context = context,
            habitRepository = habitRepository,
            widgetConfigurationRepository = widgetConfigRepo,
            coroutineScope = backgroundScope,
            sharingStarted = SharingStarted.Eagerly
        )

        viewModel.createHabit("Meditation", "lotus", 0xFF9C27B0L, LocalDate.of(2026, 9, 1))

        val state = viewModel.uiState.filter { it.habits.isNotEmpty() }.first()
        val habit = state.habits.first()
        assertEquals("Meditation", habit.habit.name)
        assertEquals("lotus", habit.habit.icon)
        assertEquals(0xFF9C27B0L, habit.habit.color)
    }

    @Test
    fun `eraseAllData clears habits, streaks, and widget mappings`() = runTest {
        habitRepository.createHabit("Habit 1")
        widgetConfigRepo.setHabitIdForWidget(101, 1L)

        val viewModel = SettingsViewModel(
            streakRepository = streakRepository,
            settingsRepository = settingsRepository,
            context = context,
            habitRepository = habitRepository,
            widgetConfigurationRepository = widgetConfigRepo,
            coroutineScope = backgroundScope,
            sharingStarted = SharingStarted.Eagerly
        )

        viewModel.eraseAllData().join()

        assertNull(widgetConfigRepo.getHabitIdForWidget(101))
    }

    @Test
    fun `importCsv handles multi-habit import without constraint violations`() = runTest {
        val csvData = """
            id,habit_name,start_date,end_date,length_days,reason
            1,"Habit A",2026-09-01,2026-09-10,9,"Reason A"
            2,"Habit A",2026-09-10,,12,
            3,"Habit B",2026-09-05,,17,
        """.trimIndent()

        val viewModel = SettingsViewModel(
            streakRepository = streakRepository,
            settingsRepository = settingsRepository,
            context = context,
            habitRepository = habitRepository,
            widgetConfigurationRepository = widgetConfigRepo,
            coroutineScope = backgroundScope,
            sharingStarted = SharingStarted.Eagerly
        )

        var importSuccess = false
        var importedCount = 0
        viewModel.importCsv(ByteArrayInputStream(csvData.toByteArray())) { success, count ->
            importSuccess = success
            importedCount = count
        }.join()

        assertTrue(importSuccess)
        assertEquals(3, importedCount)

        val allStreaks = streakRepository.getAllStreaks()
        assertEquals(3, allStreaks.size)
    }
}

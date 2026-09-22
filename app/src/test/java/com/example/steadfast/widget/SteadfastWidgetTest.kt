package com.example.steadfast.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.steadfast.data.HabitRepository
import com.example.steadfast.data.db.AppDatabase
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
class SteadfastWidgetTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var habitRepo: HabitRepository
    private lateinit var widgetConfigRepo: WidgetConfigurationRepository
    private lateinit var widget: SteadfastWidget

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitRepo = HabitRepository(database.habitDao(), database.streakDao())
        widgetConfigRepo = WidgetConfigurationRepository(context)
        widget = SteadfastWidget()
    }

    @After
    fun tearDown() {
        widgetConfigRepo.clearAll()
        database.close()
    }

    @Test
    fun extractAppWidgetId_worksForAppWidgetId() {
        val glanceId = AppWidgetId(101)
        val extracted = SteadfastWidget.extractAppWidgetId(context, glanceId)
        assertEquals(101, extracted)
    }

    @Test
    fun extractAppWidgetId_handlesUnknownGlanceId() {
        val fakeGlanceId = object : GlanceId {
            override fun toString(): String = "UnknownGlanceId"
        }
        val extracted = SteadfastWidget.extractAppWidgetId(context, fakeGlanceId)
        assertEquals(-1, extracted)
    }

    @Test
    fun testHabitSelectionForMultipleWidgets() = runTest {
        val h1 = habitRepo.createHabit("Habit 1", startDate = LocalDate.now().minusDays(10))
        val h2 = habitRepo.createHabit("Habit 2", startDate = LocalDate.now().minusDays(3))
        val h3 = habitRepo.createHabit("Habit 3", startDate = LocalDate.now().minusDays(7))

        widgetConfigRepo.setHabitIdForWidget(101, h1)
        widgetConfigRepo.setHabitIdForWidget(102, h2)
        widgetConfigRepo.setHabitIdForWidget(103, h3)

        assertEquals(h1, widgetConfigRepo.getHabitIdForWidget(101))
        assertEquals(h2, widgetConfigRepo.getHabitIdForWidget(102))
        assertEquals(h3, widgetConfigRepo.getHabitIdForWidget(103))

        // Verify resolution for Widget 1 (Habit 1)
        val habitId1 = widgetConfigRepo.getHabitIdForWidget(101)
        val (habit1, streak1) = SteadfastWidget.resolveForHabitId(database, habitId1)
        assertNotNull(habit1)
        assertEquals(h1, habit1?.id)
        assertEquals("Habit 1", habit1?.name)
        assertNotNull(streak1)
        assertEquals(h1, streak1?.habitId)

        // Verify resolution for Widget 2 (Habit 2) - must resolve Habit 2 without error
        val habitId2 = widgetConfigRepo.getHabitIdForWidget(102)
        val (habit2, streak2) = SteadfastWidget.resolveForHabitId(database, habitId2)
        assertNotNull(habit2)
        assertEquals(h2, habit2?.id)
        assertEquals("Habit 2", habit2?.name)
        assertNotNull(streak2)
        assertEquals(h2, streak2?.habitId)

        // Verify resolution for Widget 3 (Habit 3) - must resolve Habit 3, NOT Habit 1
        val habitId3 = widgetConfigRepo.getHabitIdForWidget(103)
        val (habit3, streak3) = SteadfastWidget.resolveForHabitId(database, habitId3)
        assertNotNull(habit3)
        assertEquals(h3, habit3?.id)
        assertEquals("Habit 3", habit3?.name)
        assertNotNull(streak3)
        assertEquals(h3, streak3?.habitId)
    }

    @Test
    fun testDeletedHabitFallsBackToFirstActiveHabit() = runTest {
        val h1 = habitRepo.createHabit("Habit 1", startDate = LocalDate.now().minusDays(10))
        val h2 = habitRepo.createHabit("Habit 2", startDate = LocalDate.now())

        widgetConfigRepo.setHabitIdForWidget(102, h2)
        // Delete Habit 2
        habitRepo.deleteHabit(h2)

        val habitId = widgetConfigRepo.getHabitIdForWidget(102)
        val (habit, streak) = SteadfastWidget.resolveForHabitId(database, habitId)
        assertNotNull(habit)
        assertEquals(h1, habit?.id)
        assertEquals("Habit 1", habit?.name)
        assertNotNull(streak)
        assertEquals(h1, streak?.habitId)
    }

    @Test
    fun testUnconfiguredWidgetFallsBackToFirstActiveHabit() = runTest {
        val h1 = habitRepo.createHabit("Habit 1", startDate = LocalDate.now().minusDays(10))

        // No widget configuration exists for widget 999
        val habitId = widgetConfigRepo.getHabitIdForWidget(999)
        val (habit, streak) = SteadfastWidget.resolveForHabitId(database, habitId)
        assertNotNull(habit)
        assertEquals(h1, habit?.id)
        assertEquals("Habit 1", habit?.name)
        assertNotNull(streak)
        assertEquals(h1, streak?.habitId)
    }

    @Test
    fun testOnDeleteRemovesConfiguration() = runTest {
        val h1 = habitRepo.createHabit("Habit 1", startDate = LocalDate.now())
        widgetConfigRepo.setHabitIdForWidget(101, h1)
        assertEquals(h1, widgetConfigRepo.getHabitIdForWidget(101))

        widget.onDelete(context, AppWidgetId(101))
        assertNull(widgetConfigRepo.getHabitIdForWidget(101))
    }
}

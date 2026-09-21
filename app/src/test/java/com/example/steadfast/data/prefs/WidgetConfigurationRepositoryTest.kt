package com.example.steadfast.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetConfigurationRepositoryTest {

    private lateinit var repository: WidgetConfigurationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = WidgetConfigurationRepository(context)
    }

    @Test
    fun `save and retrieve habitId for appWidgetId`() {
        assertNull(repository.getHabitIdForWidget(101))

        repository.setHabitIdForWidget(101, 42L)
        assertEquals(42L, repository.getHabitIdForWidget(101))

        repository.setHabitIdForWidget(102, 99L)
        assertEquals(42L, repository.getHabitIdForWidget(101))
        assertEquals(99L, repository.getHabitIdForWidget(102))

        repository.removeWidget(101)
        assertNull(repository.getHabitIdForWidget(101))
        assertEquals(99L, repository.getHabitIdForWidget(102))
    }

    @Test
    fun `clearAll removes all widget mappings`() {
        repository.setHabitIdForWidget(101, 42L)
        repository.setHabitIdForWidget(102, 99L)
        repository.clearAll()
        assertNull(repository.getHabitIdForWidget(101))
        assertNull(repository.getHabitIdForWidget(102))
    }
}

package com.example.steadfast.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.WidgetBgTheme
import com.example.steadfast.data.prefs.WidgetFontColor
import com.example.steadfast.data.prefs.WidgetShape
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test_settings.preferences_pb") }
        )
        repository = SettingsRepository(testDataStore)
    }

    @Test
    fun `default widget settings are correct`() = runTest {
        val settings = repository.settingsFlow.first()
        assertEquals(WidgetShape.ROUNDED, settings.widgetShape)
        assertEquals(100, settings.widgetBackgroundOpacity)
        assertEquals(WidgetFontColor.DEFAULT, settings.widgetFontColor)
        assertEquals(WidgetBgTheme.DEFAULT, settings.widgetBgTheme)
        assertEquals(true, settings.widgetShowHabitName)
    }

    @Test
    fun `setting widget opacity persists and coerces to 0-100 range`() = runTest {
        repository.setWidgetBackgroundOpacity(50)
        assertEquals(50, repository.settingsFlow.first().widgetBackgroundOpacity)

        repository.setWidgetBackgroundOpacity(0)
        assertEquals(0, repository.settingsFlow.first().widgetBackgroundOpacity)

        repository.setWidgetBackgroundOpacity(150)
        assertEquals(100, repository.settingsFlow.first().widgetBackgroundOpacity)

        repository.setWidgetBackgroundOpacity(-20)
        assertEquals(0, repository.settingsFlow.first().widgetBackgroundOpacity)
    }

    @Test
    fun `setting widget font color persists`() = runTest {
        repository.setWidgetFontColor(WidgetFontColor.WHITE)
        assertEquals(WidgetFontColor.WHITE, repository.settingsFlow.first().widgetFontColor)

        repository.setWidgetFontColor(WidgetFontColor.BLACK)
        assertEquals(WidgetFontColor.BLACK, repository.settingsFlow.first().widgetFontColor)

        repository.setWidgetFontColor(WidgetFontColor.BRAND)
        assertEquals(WidgetFontColor.BRAND, repository.settingsFlow.first().widgetFontColor)

        repository.setWidgetFontColor(WidgetFontColor.DEFAULT)
        assertEquals(WidgetFontColor.DEFAULT, repository.settingsFlow.first().widgetFontColor)
    }

    @Test
    fun `setting widget bg theme persists`() = runTest {
        repository.setWidgetBgTheme(WidgetBgTheme.BLACK)
        assertEquals(WidgetBgTheme.BLACK, repository.settingsFlow.first().widgetBgTheme)

        repository.setWidgetBgTheme(WidgetBgTheme.CHARCOAL)
        assertEquals(WidgetBgTheme.CHARCOAL, repository.settingsFlow.first().widgetBgTheme)

        repository.setWidgetBgTheme(WidgetBgTheme.WHITE)
        assertEquals(WidgetBgTheme.WHITE, repository.settingsFlow.first().widgetBgTheme)

        repository.setWidgetBgTheme(WidgetBgTheme.DEFAULT)
        assertEquals(WidgetBgTheme.DEFAULT, repository.settingsFlow.first().widgetBgTheme)
    }

    @Test
    fun `setting widget shape persists`() = runTest {
        repository.setWidgetShape(WidgetShape.CIRCLE)
        assertEquals(WidgetShape.CIRCLE, repository.settingsFlow.first().widgetShape)

        repository.setWidgetShape(WidgetShape.ROUNDED)
        assertEquals(WidgetShape.ROUNDED, repository.settingsFlow.first().widgetShape)
    }

    @Test
    fun `setting widget show habit name persists`() = runTest {
        assertEquals(true, repository.settingsFlow.first().widgetShowHabitName)

        repository.setWidgetShowHabitName(false)
        assertEquals(false, repository.settingsFlow.first().widgetShowHabitName)

        repository.setWidgetShowHabitName(true)
        assertEquals(true, repository.settingsFlow.first().widgetShowHabitName)
    }
}

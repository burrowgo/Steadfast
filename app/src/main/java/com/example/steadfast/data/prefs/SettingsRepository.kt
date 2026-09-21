package com.example.steadfast.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.steadfast.data.updater.UpdateCheckResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "steadfast_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromString(value: String?): ThemeMode = when (value?.lowercase()) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}

enum class WidgetShape {
    ROUNDED,
    CIRCLE;

    companion object {
        fun fromString(value: String?): WidgetShape = when (value?.lowercase()) {
            "circle" -> CIRCLE
            else -> ROUNDED
        }
    }
}

enum class WidgetFontColor {
    DEFAULT,
    WHITE,
    BLACK,
    BRAND;

    companion object {
        fun fromString(value: String?): WidgetFontColor = when (value?.lowercase()) {
            "white" -> WHITE
            "black" -> BLACK
            "brand" -> BRAND
            else -> DEFAULT
        }
    }
}

enum class WidgetBgTheme {
    DEFAULT,
    BLACK,
    CHARCOAL,
    WHITE;

    companion object {
        fun fromString(value: String?): WidgetBgTheme = when (value?.lowercase()) {
            "black" -> BLACK
            "charcoal" -> CHARCOAL
            "white" -> WHITE
            else -> DEFAULT
        }
    }
}

enum class AutoUpdateFrequency {
    WEEKLY,
    DAILY,
    MANUAL;

    companion object {
        fun fromString(value: String?): AutoUpdateFrequency = when (value?.lowercase()) {
            "daily" -> DAILY
            "manual" -> MANUAL
            else -> WEEKLY
        }
    }
}

enum class FirstDayOfWeek {
    MONDAY,
    SUNDAY;

    companion object {
        fun fromString(value: String?): FirstDayOfWeek = when (value?.lowercase()) {
            "sunday" -> SUNDAY
            else -> MONDAY
        }
    }
}

data class UserSettings(
    val habitName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val lastCelebratedRankIndex: Int = 0,
    val widgetShape: WidgetShape = WidgetShape.ROUNDED,
    val widgetBackgroundOpacity: Int = 100,
    val widgetFontColor: WidgetFontColor = WidgetFontColor.DEFAULT,
    val widgetBgTheme: WidgetBgTheme = WidgetBgTheme.DEFAULT,
    val widgetShowHabitName: Boolean = true,
    val autoUpdateFrequency: AutoUpdateFrequency = AutoUpdateFrequency.WEEKLY,
    val lastUpdateCheckTime: Long = 0L,
    val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_HABIT_NAME = stringPreferencesKey("habit_name")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_TIME = stringPreferencesKey("reminder_time")
        val KEY_LAST_CELEBRATED_RANK = intPreferencesKey("last_celebrated_rank_index")
        val KEY_WIDGET_SHAPE = stringPreferencesKey("widget_shape")
        val KEY_WIDGET_OPACITY = intPreferencesKey("widget_background_opacity")
        val KEY_WIDGET_FONT_COLOR = stringPreferencesKey("widget_font_color")
        val KEY_WIDGET_BG_THEME = stringPreferencesKey("widget_bg_theme")
        val KEY_WIDGET_SHOW_HABIT_NAME = booleanPreferencesKey("widget_show_habit_name")
        val KEY_LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
        val KEY_AUTO_UPDATE_FREQUENCY = stringPreferencesKey("auto_update_frequency")
        val KEY_LAST_UPDATE_CHECK_TIME = longPreferencesKey("last_update_check_time")
        val KEY_PENDING_UPDATE_VERSION = stringPreferencesKey("pending_update_version")
        val KEY_PENDING_UPDATE_NOTES = stringPreferencesKey("pending_update_notes")
        val KEY_PENDING_UPDATE_URL = stringPreferencesKey("pending_update_url")
        val KEY_PENDING_UPDATE_PAGE = stringPreferencesKey("pending_update_page")
        val KEY_FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            habitName = preferences[KEY_HABIT_NAME] ?: "",
            themeMode = ThemeMode.fromString(preferences[KEY_THEME_MODE]),
            useDynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true,
            reminderEnabled = preferences[KEY_REMINDER_ENABLED] ?: false,
            reminderTime = preferences[KEY_REMINDER_TIME] ?: "20:00",
            lastCelebratedRankIndex = preferences[KEY_LAST_CELEBRATED_RANK] ?: 0,
            widgetShape = WidgetShape.fromString(preferences[KEY_WIDGET_SHAPE]),
            widgetBackgroundOpacity = (preferences[KEY_WIDGET_OPACITY] ?: 100).coerceIn(0, 100),
            widgetFontColor = WidgetFontColor.fromString(preferences[KEY_WIDGET_FONT_COLOR]),
            widgetBgTheme = WidgetBgTheme.fromString(preferences[KEY_WIDGET_BG_THEME]),
            widgetShowHabitName = preferences[KEY_WIDGET_SHOW_HABIT_NAME] ?: true,
            autoUpdateFrequency = AutoUpdateFrequency.fromString(preferences[KEY_AUTO_UPDATE_FREQUENCY]),
            lastUpdateCheckTime = preferences[KEY_LAST_UPDATE_CHECK_TIME] ?: 0L,
            firstDayOfWeek = FirstDayOfWeek.fromString(preferences[KEY_FIRST_DAY_OF_WEEK])
        )
    }

    suspend fun setFirstDayOfWeek(firstDay: FirstDayOfWeek) {
        dataStore.edit { preferences ->
            preferences[KEY_FIRST_DAY_OF_WEEK] = firstDay.name.lowercase()
        }
    }

    suspend fun setHabitName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_HABIT_NAME] = name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name.lowercase()
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderTime(time: String) {
        dataStore.edit { preferences ->
            preferences[KEY_REMINDER_TIME] = time
        }
    }

    suspend fun setLastCelebratedRankIndex(index: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_CELEBRATED_RANK] = index
        }
    }

    suspend fun setWidgetShape(shape: WidgetShape) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_SHAPE] = shape.name.lowercase()
        }
    }

    suspend fun setWidgetBackgroundOpacity(opacity: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_OPACITY] = opacity.coerceIn(0, 100)
        }
    }

    suspend fun setWidgetFontColor(color: WidgetFontColor) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_FONT_COLOR] = color.name.lowercase()
        }
    }

    suspend fun setWidgetBgTheme(theme: WidgetBgTheme) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_BG_THEME] = theme.name.lowercase()
        }
    }

    suspend fun setWidgetShowHabitName(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_SHOW_HABIT_NAME] = show
        }
    }

    val lastSeenVersionFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_SEEN_VERSION]
    }

    suspend fun setLastSeenVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SEEN_VERSION] = version
        }
    }

    suspend fun setAutoUpdateFrequency(frequency: AutoUpdateFrequency) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_UPDATE_FREQUENCY] = frequency.name.lowercase()
        }
    }

    val lastUpdateCheckTimeFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_UPDATE_CHECK_TIME] ?: 0L
    }

    suspend fun setLastUpdateCheckTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_UPDATE_CHECK_TIME] = timestamp
        }
    }

    val pendingUpdateFlow: Flow<UpdateCheckResult.UpdateAvailable?> = dataStore.data.map { preferences ->
        val version = preferences[KEY_PENDING_UPDATE_VERSION]
        val downloadUrl = preferences[KEY_PENDING_UPDATE_URL]
        val notes = preferences[KEY_PENDING_UPDATE_NOTES]
        val page = preferences[KEY_PENDING_UPDATE_PAGE]
        if (!version.isNullOrBlank() && !downloadUrl.isNullOrBlank()) {
            UpdateCheckResult.UpdateAvailable(
                version = version,
                releaseNotes = notes.orEmpty(),
                downloadUrl = downloadUrl,
                releasePageUrl = page ?: "https://github.com/burrowgo/Steadfast/releases"
            )
        } else {
            null
        }
    }

    suspend fun setPendingUpdate(update: UpdateCheckResult.UpdateAvailable?) {
        dataStore.edit { preferences ->
            if (update != null) {
                preferences[KEY_PENDING_UPDATE_VERSION] = update.version
                preferences[KEY_PENDING_UPDATE_NOTES] = update.releaseNotes
                preferences[KEY_PENDING_UPDATE_URL] = update.downloadUrl
                preferences[KEY_PENDING_UPDATE_PAGE] = update.releasePageUrl
            } else {
                preferences.remove(KEY_PENDING_UPDATE_VERSION)
                preferences.remove(KEY_PENDING_UPDATE_NOTES)
                preferences.remove(KEY_PENDING_UPDATE_URL)
                preferences.remove(KEY_PENDING_UPDATE_PAGE)
            }
        }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

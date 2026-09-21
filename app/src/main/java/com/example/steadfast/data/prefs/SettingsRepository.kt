package com.example.steadfast.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

data class UserSettings(
    val habitName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val lastCelebratedRankIndex: Int = 0
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_HABIT_NAME = stringPreferencesKey("habit_name")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_TIME = stringPreferencesKey("reminder_time")
        val KEY_LAST_CELEBRATED_RANK = intPreferencesKey("last_celebrated_rank_index")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            habitName = preferences[KEY_HABIT_NAME] ?: "",
            themeMode = ThemeMode.fromString(preferences[KEY_THEME_MODE]),
            useDynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true,
            reminderEnabled = preferences[KEY_REMINDER_ENABLED] ?: false,
            reminderTime = preferences[KEY_REMINDER_TIME] ?: "20:00",
            lastCelebratedRankIndex = preferences[KEY_LAST_CELEBRATED_RANK] ?: 0
        )
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

    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

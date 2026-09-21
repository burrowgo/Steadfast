package com.example.steadfast.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.ThemeMode
import com.example.steadfast.data.prefs.UserSettings
import com.example.steadfast.data.prefs.WidgetShape
import com.example.steadfast.notifications.NotificationHelper
import com.example.steadfast.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.time.LocalDate

data class SettingsUiState(
    val habitName: String = "",
    val activeHabitExists: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val widgetShape: WidgetShape = WidgetShape.ROUNDED
)

class SettingsViewModel(
    private val streakRepository: StreakRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        streakRepository.activeStreak
    ) { settings, active ->
        val effectiveName = active?.habitName ?: settings.habitName
        SettingsUiState(
            habitName = effectiveName,
            activeHabitExists = active != null,
            themeMode = settings.themeMode,
            useDynamicColor = settings.useDynamicColor,
            reminderEnabled = settings.reminderEnabled,
            reminderTime = settings.reminderTime,
            widgetShape = settings.widgetShape
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun renameHabit(newName: String) {
        val trimmed = newName.trim().take(40)
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                streakRepository.updateActiveHabitName(trimmed)
                settingsRepository.setHabitName(trimmed)
                WidgetUpdater.updateAll(context)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setWidgetShape(shape: WidgetShape) {
        viewModelScope.launch {
            settingsRepository.setWidgetShape(shape)
            WidgetUpdater.updateAll(context)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            if (enabled) {
                NotificationHelper.scheduleDailyReminder(context, uiState.value.reminderTime)
            } else {
                NotificationHelper.cancelDailyReminder(context)
            }
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(time)
            if (uiState.value.reminderEnabled) {
                NotificationHelper.scheduleDailyReminder(context, time)
            }
        }
    }

    fun exportCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            try {
                val streaks = streakRepository.getAllStreaks()
                outputStream.bufferedWriter().use { writer ->
                    writer.write("id,habit_name,start_date,end_date,length_days,reason\n")
                    for (s in streaks) {
                        val startStr = LocalDate.ofEpochDay(s.startDate).toString()
                        val endStr = s.endDate?.let { LocalDate.ofEpochDay(it).toString() } ?: ""
                        val lengthStr = s.lengthDays?.toString() ?: ""
                        val cleanReason = s.reason?.replace("\"", "\"\"") ?: ""
                        writer.write("${s.id},\"${s.habitName}\",$startStr,$endStr,$lengthStr,\"$cleanReason\"\n")
                    }
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun eraseAllData() {
        viewModelScope.launch {
            NotificationHelper.cancelDailyReminder(context)
            streakRepository.clearAllData()
            settingsRepository.clearAll()
            WidgetUpdater.updateAll(context)
        }
    }

    companion object {
        fun provideFactory(
            streakRepository: StreakRepository,
            settingsRepository: SettingsRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(streakRepository, settingsRepository, context) as T
            }
        }
    }
}

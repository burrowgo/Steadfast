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
import com.example.steadfast.data.updater.DefaultUpdateChecker
import com.example.steadfast.data.updater.UpdateCheckResult
import com.example.steadfast.data.updater.UpdateChecker
import com.example.steadfast.domain.ChangelogRelease
import com.example.steadfast.domain.ChangelogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

data class SettingsUiState(
    val habitName: String = "",
    val activeHabitExists: Boolean = false,
    val activeStartDate: LocalDate? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val widgetShape: WidgetShape = WidgetShape.ROUNDED,
    val isCheckingForUpdate: Boolean = false,
    val updateResult: UpdateCheckResult? = null,
    val showWhatsNew: ChangelogRelease? = null
)

class SettingsViewModel(
    private val streakRepository: StreakRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context,
    private val updateChecker: UpdateChecker = DefaultUpdateChecker()
) : ViewModel() {

    private val isCheckingForUpdate = MutableStateFlow(false)
    private val updateResult = MutableStateFlow<UpdateCheckResult?>(null)
    private val showWhatsNew = MutableStateFlow<ChangelogRelease?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        streakRepository.activeStreak,
        isCheckingForUpdate,
        updateResult,
        showWhatsNew
    ) { settings, active, checking, updateRes, whatsNew ->
        val effectiveName = active?.habitName ?: settings.habitName
        val startDate = active?.let { LocalDate.ofEpochDay(it.startDate) }
        SettingsUiState(
            habitName = effectiveName,
            activeHabitExists = active != null,
            activeStartDate = startDate,
            themeMode = settings.themeMode,
            useDynamicColor = settings.useDynamicColor,
            reminderEnabled = settings.reminderEnabled,
            reminderTime = settings.reminderTime,
            widgetShape = settings.widgetShape,
            isCheckingForUpdate = checking,
            updateResult = updateRes,
            showWhatsNew = whatsNew
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

    fun updateStartDate(newStartDate: LocalDate) {
        viewModelScope.launch {
            streakRepository.updateActiveStartDate(newStartDate)
            WidgetUpdater.updateAll(context)
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

    fun importCsv(inputStream: InputStream, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = inputStream.bufferedReader().readLines()
                if (lines.isEmpty()) {
                    onComplete(false, 0)
                    return@launch
                }
                val streaks = mutableListOf<com.example.steadfast.data.db.StreakEntity>()
                var activeHabitName: String? = null

                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isBlank()) continue
                    val parts = parseCsvLine(line)
                    if (parts.size < 3) continue
                    val habitName = parts.getOrNull(1)?.ifBlank { "Habit" } ?: "Habit"
                    val startStr = parts.getOrNull(2) ?: continue
                    val endStr = parts.getOrNull(3)?.ifBlank { null }
                    val lengthStr = parts.getOrNull(4)?.ifBlank { null }
                    val reason = parts.getOrNull(5)?.ifBlank { null }

                    val startDate = LocalDate.parse(startStr).toEpochDay()
                    val endDate = endStr?.let { LocalDate.parse(it).toEpochDay() }
                    val lengthDays = lengthStr?.toIntOrNull()
                    val isEnded = endDate != null

                    val streak = com.example.steadfast.data.db.StreakEntity(
                        id = 0,
                        habitName = habitName,
                        startDate = startDate,
                        startedAt = startDate * 86400000L,
                        endDate = endDate,
                        endedAt = if (isEnded) (endDate!! * 86400000L) else null,
                        lengthDays = lengthDays,
                        reason = reason
                    )
                    streaks.add(streak)
                    if (!isEnded) {
                        activeHabitName = habitName
                    }
                }

                if (streaks.isNotEmpty()) {
                    streakRepository.restoreStreaks(streaks)
                    if (activeHabitName != null) {
                        settingsRepository.setHabitName(activeHabitName)
                    }
                    WidgetUpdater.updateAll(context)
                    onComplete(true, streaks.size)
                } else {
                    onComplete(false, 0)
                }
            } catch (e: Exception) {
                onComplete(false, 0)
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    fun eraseAllData() {
        viewModelScope.launch {
            NotificationHelper.cancelDailyReminder(context)
            streakRepository.clearAllData()
            settingsRepository.clearAll()
            WidgetUpdater.updateAll(context)
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            isCheckingForUpdate.value = true
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.2.0"
            } catch (e: Exception) {
                "0.2.0"
            }
            val result = updateChecker.checkForUpdate(currentVersion)
            isCheckingForUpdate.value = false
            updateResult.value = result
        }
    }

    fun dismissUpdateResult() {
        updateResult.value = null
    }

    fun showWhatsNew(version: String) {
        showWhatsNew.value = ChangelogRepository.getRelease(version)
    }

    fun dismissWhatsNew() {
        showWhatsNew.value = null
    }

    companion object {
        fun provideFactory(
            streakRepository: StreakRepository,
            settingsRepository: SettingsRepository,
            context: Context,
            updateChecker: UpdateChecker = DefaultUpdateChecker()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(streakRepository, settingsRepository, context, updateChecker) as T
            }
        }
    }
}

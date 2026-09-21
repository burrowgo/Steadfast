package com.example.steadfast.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.prefs.AutoUpdateFrequency
import com.example.steadfast.data.prefs.FirstDayOfWeek
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.ThemeMode
import com.example.steadfast.data.prefs.UserSettings
import com.example.steadfast.data.prefs.WidgetShape
import com.example.steadfast.data.prefs.WidgetFontColor
import com.example.steadfast.data.prefs.WidgetBgTheme
import com.example.steadfast.data.updater.AutoUpdateScheduler
import com.example.steadfast.data.updater.DefaultUpdateChecker
import com.example.steadfast.data.updater.UpdateCheckResult
import com.example.steadfast.data.updater.UpdateChecker
import com.example.steadfast.domain.ChangelogRelease
import com.example.steadfast.domain.ChangelogRepository
import com.example.steadfast.R
import com.example.steadfast.notifications.NotificationHelper
import com.example.steadfast.widget.WidgetUpdater
import com.example.steadfast.data.prefs.WidgetConfigurationRepository
import com.example.steadfast.domain.model.HabitWithStreak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

data class SettingsUiState(
    val habits: List<HabitWithStreak> = emptyList(),
    val habitName: String = "",
    val activeHabitExists: Boolean = false,
    val activeStartDate: LocalDate? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "20:00",
    val widgetShape: WidgetShape = WidgetShape.ROUNDED,
    val widgetBackgroundOpacity: Int = 100,
    val widgetFontColor: WidgetFontColor = WidgetFontColor.DEFAULT,
    val widgetBgTheme: WidgetBgTheme = WidgetBgTheme.DEFAULT,
    val widgetShowHabitName: Boolean = true,
    val autoUpdateFrequency: AutoUpdateFrequency = AutoUpdateFrequency.WEEKLY,
    val lastUpdateCheckTime: Long = 0L,
    val isCheckingForUpdate: Boolean = false,
    val updateResult: UpdateCheckResult? = null,
    val showWhatsNew: ChangelogRelease? = null,
    val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY
)

class SettingsViewModel(
    private val streakRepository: StreakRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context,
    private val habitRepository: com.example.steadfast.data.HabitRepository? = null,
    private val updateChecker: UpdateChecker = DefaultUpdateChecker(),
    private val widgetConfigurationRepository: WidgetConfigurationRepository? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private data class UpdateState(
        val checking: Boolean = false,
        val result: UpdateCheckResult? = null,
        val whatsNew: ChangelogRelease? = null
    )

    private val isCheckingForUpdate = MutableStateFlow(false)
    private val updateResult = MutableStateFlow<UpdateCheckResult?>(null)
    private val showWhatsNew = MutableStateFlow<ChangelogRelease?>(null)

    private val updateStateFlow = combine(
        isCheckingForUpdate,
        updateResult,
        showWhatsNew
    ) { checking, result, whatsNew ->
        UpdateState(checking, result, whatsNew)
    }

    private val habitsFlow = habitRepository?.activeHabitsWithStreaks ?: flowOf(emptyList())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settingsFlow,
        habitsFlow,
        updateStateFlow
    ) { settings, habitsList, updateState ->
        val firstHabit = habitsList.firstOrNull()
        val effectiveName = firstHabit?.habit?.name ?: settings.habitName
        val startDate = firstHabit?.activeStreak?.startDate?.let { LocalDate.ofEpochDay(it) }
        SettingsUiState(
            habits = habitsList,
            habitName = effectiveName,
            activeHabitExists = habitsList.isNotEmpty(),
            activeStartDate = startDate,
            themeMode = settings.themeMode,
            useDynamicColor = settings.useDynamicColor,
            reminderEnabled = settings.reminderEnabled,
            reminderTime = settings.reminderTime,
            widgetShape = settings.widgetShape,
            widgetBackgroundOpacity = settings.widgetBackgroundOpacity,
            widgetFontColor = settings.widgetFontColor,
            widgetBgTheme = settings.widgetBgTheme,
            widgetShowHabitName = settings.widgetShowHabitName,
            autoUpdateFrequency = settings.autoUpdateFrequency,
            lastUpdateCheckTime = settings.lastUpdateCheckTime,
            isCheckingForUpdate = updateState.checking,
            updateResult = updateState.result,
            showWhatsNew = updateState.whatsNew,
            firstDayOfWeek = settings.firstDayOfWeek
        )
    }.stateIn(
        scope = scope,
        started = sharingStarted,
        initialValue = SettingsUiState()
    )

    fun setFirstDayOfWeek(firstDay: FirstDayOfWeek) {
        scope.launch {
            settingsRepository.setFirstDayOfWeek(firstDay)
        }
    }

    fun updateHabit(
        id: Long,
        newName: String,
        icon: String,
        color: Long,
        startDate: LocalDate
    ) {
        val trimmed = newName.trim().take(40)
        if (trimmed.isNotBlank()) {
            scope.launch {
                habitRepository?.updateHabit(id, trimmed, icon, color)
                streakRepository.updateActiveStartDate(habitId = id, newStartDate = startDate)
                streakRepository.updateActiveHabitName(habitId = id, newName = trimmed)
                settingsRepository.setHabitName(trimmed)
                WidgetUpdater.updateAll(context)
            }
        }
    }

    fun createHabit(
        name: String,
        icon: String,
        color: Long,
        startDate: LocalDate
    ) {
        val trimmed = name.trim().take(40)
        if (trimmed.isNotBlank()) {
            scope.launch {
                habitRepository?.createHabit(trimmed, icon, color, startDate)
                WidgetUpdater.updateAll(context)
            }
        }
    }

    fun deleteHabit(id: Long) {
        scope.launch {
            habitRepository?.deleteHabit(id)
            WidgetUpdater.updateAll(context)
        }
    }

    fun renameHabit(newName: String) {
        val trimmed = newName.trim().take(40)
        if (trimmed.isNotBlank()) {
            scope.launch {
                val firstHabit = uiState.value.habits.firstOrNull()
                val targetId = firstHabit?.habit?.id ?: 1L
                habitRepository?.updateHabit(
                    id = targetId,
                    name = trimmed,
                    icon = firstHabit?.habit?.icon ?: "shield",
                    color = firstHabit?.habit?.color ?: 0xFF4C662BL
                )
                streakRepository.updateActiveHabitName(targetId, trimmed)
                settingsRepository.setHabitName(trimmed)
                WidgetUpdater.updateAll(context)
            }
        }
    }

    fun updateStartDate(newStartDate: LocalDate) {
        scope.launch {
            val targetId = uiState.value.habits.firstOrNull()?.habit?.id ?: 1L
            streakRepository.updateActiveStartDate(targetId, newStartDate)
            WidgetUpdater.updateAll(context)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        scope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        scope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setWidgetShape(shape: WidgetShape) {
        scope.launch {
            settingsRepository.setWidgetShape(shape)
            WidgetUpdater.updateAll(context)
        }
    }

    fun setWidgetBackgroundOpacity(opacity: Int) {
        scope.launch {
            settingsRepository.setWidgetBackgroundOpacity(opacity)
            WidgetUpdater.updateAll(context)
        }
    }

    fun setWidgetFontColor(color: WidgetFontColor) {
        scope.launch {
            settingsRepository.setWidgetFontColor(color)
            WidgetUpdater.updateAll(context)
        }
    }

    fun setWidgetBgTheme(theme: WidgetBgTheme) {
        scope.launch {
            settingsRepository.setWidgetBgTheme(theme)
            WidgetUpdater.updateAll(context)
        }
    }

    fun setWidgetShowHabitName(show: Boolean) {
        scope.launch {
            settingsRepository.setWidgetShowHabitName(show)
            WidgetUpdater.updateAll(context)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setReminderEnabled(enabled)
            if (enabled) {
                NotificationHelper.scheduleDailyReminder(context, uiState.value.reminderTime)
            } else {
                NotificationHelper.cancelDailyReminder(context)
            }
        }
    }

    fun setReminderTime(time: String) {
        scope.launch {
            settingsRepository.setReminderTime(time)
            if (uiState.value.reminderEnabled) {
                NotificationHelper.scheduleDailyReminder(context, time)
            }
        }
    }

    fun exportCsv(outputStream: OutputStream) {
        scope.launch {
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

    data class CsvStreakRow(
        val habitName: String,
        val startDate: Long,
        val endDate: Long?,
        val lengthDays: Int?,
        val reason: String?
    )

    fun importCsv(inputStream: InputStream, onComplete: (Boolean, Int) -> Unit): kotlinx.coroutines.Job = scope.launch {
        try {
            val lines = inputStream.bufferedReader().readLines()
            if (lines.isEmpty()) {
                onComplete(false, 0)
                return@launch
            }
            val rawRows = mutableListOf<CsvStreakRow>()
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
                rawRows.add(CsvStreakRow(habitName, startDate, endDate, lengthDays, reason))
            }

            if (rawRows.isEmpty()) {
                onComplete(false, 0)
                return@launch
            }

            // Ensure habits exist for all imported habit names
            val habitMap = mutableMapOf<String, Long>()
            for (row in rawRows) {
                if (!habitMap.containsKey(row.habitName)) {
                    val habitId = habitRepository?.createHabit(
                        name = row.habitName,
                        startDate = LocalDate.ofEpochDay(row.startDate)
                    ) ?: 1L
                    habitMap[row.habitName] = habitId
                }
            }

            val streaks = rawRows.map { row ->
                val hId = habitMap[row.habitName] ?: 1L
                val isEnded = row.endDate != null
                com.example.steadfast.data.db.StreakEntity(
                    id = 0,
                    habitId = hId,
                    habitName = row.habitName,
                    startDate = row.startDate,
                    startedAt = row.startDate * 86400000L,
                    endDate = row.endDate,
                    endedAt = if (isEnded) (row.endDate!! * 86400000L) else null,
                    lengthDays = row.lengthDays,
                    reason = row.reason
                )
            }

            streakRepository.restoreStreaks(streaks)
            val activeHabitName = rawRows.lastOrNull { it.endDate == null }?.habitName
            if (activeHabitName != null) {
                settingsRepository.setHabitName(activeHabitName)
            }
            WidgetUpdater.updateAll(context)
            onComplete(true, streaks.size)
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false, 0)
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

    fun eraseAllData(): kotlinx.coroutines.Job = scope.launch {
        NotificationHelper.cancelDailyReminder(context)
        streakRepository.clearAllData()
        settingsRepository.clearAll()
        habitRepository?.clearAllData()
        widgetConfigurationRepository?.clearAll()
        val defaultName = try {
            context.getString(R.string.app_name)
        } catch (e: Exception) {
            "Steadfast"
        }
        habitRepository?.createHabit(defaultName)
        WidgetUpdater.updateAll(context)
    }

    fun checkForUpdates() {
        scope.launch {
            isCheckingForUpdate.value = true
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.7.4"
            } catch (e: Exception) {
                "0.7.4"
            }
            val result = updateChecker.checkForUpdate(currentVersion)
            settingsRepository.setLastUpdateCheckTime(System.currentTimeMillis())
            if (result is UpdateCheckResult.UpdateAvailable) {
                settingsRepository.setPendingUpdate(result)
            }
            isCheckingForUpdate.value = false
            updateResult.value = result
        }
    }

    fun setAutoUpdateFrequency(frequency: AutoUpdateFrequency) {
        scope.launch {
            settingsRepository.setAutoUpdateFrequency(frequency)
            AutoUpdateScheduler.schedule(context, frequency)
        }
    }

    fun dismissUpdateResult() {
        updateResult.value = null
        scope.launch {
            settingsRepository.setPendingUpdate(null)
        }
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
            habitRepository: com.example.steadfast.data.HabitRepository? = null,
            context: Context,
            updateChecker: UpdateChecker = DefaultUpdateChecker(),
            widgetConfigurationRepository: WidgetConfigurationRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    streakRepository,
                    settingsRepository,
                    context,
                    habitRepository,
                    updateChecker,
                    widgetConfigurationRepository
                ) as T
            }
        }
    }
}

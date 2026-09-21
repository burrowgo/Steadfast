package com.example.steadfast.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.HabitRepository
import com.example.steadfast.data.prefs.AutoUpdateFrequency
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.updater.DefaultUpdateChecker
import com.example.steadfast.data.updater.UpdateCheckResult
import com.example.steadfast.data.updater.UpdateChecker
import com.example.steadfast.domain.ChangelogRelease
import com.example.steadfast.domain.ChangelogRepository
import com.example.steadfast.domain.Quote
import com.example.steadfast.domain.QuoteRepository
import com.example.steadfast.domain.model.HabitWithStreak
import com.example.steadfast.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

enum class HabitTab {
    ACTIVE,
    ARCHIVED
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeHabits: List<HabitWithStreak> = emptyList(),
    val archivedHabits: List<HabitWithStreak> = emptyList(),
    val selectedTab: HabitTab = HabitTab.ACTIVE,
    val isAddHabitDialogOpen: Boolean = false,
    val quote: Quote? = null
)

class HomeViewModel(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val quoteRepository: QuoteRepository,
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val updateChecker: UpdateChecker = DefaultUpdateChecker()
) : ViewModel() {

    private val selectedTab = MutableStateFlow(HabitTab.ACTIVE)
    private val isAddHabitDialogOpen = MutableStateFlow(false)
    private val quoteOffset = MutableStateFlow(0)

    private val _whatsNewRelease = MutableStateFlow<ChangelogRelease?>(null)
    val whatsNewRelease: StateFlow<ChangelogRelease?> = _whatsNewRelease.asStateFlow()

    private val _updateAvailable = MutableStateFlow<UpdateCheckResult.UpdateAvailable?>(null)
    val updateAvailable: StateFlow<UpdateCheckResult.UpdateAvailable?> = _updateAvailable.asStateFlow()

    init {
        checkVersionAndUpdates()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        habitRepository.activeHabitsWithStreaks,
        habitRepository.archivedHabits,
        selectedTab,
        isAddHabitDialogOpen,
        quoteOffset
    ) { activeList, archivedList, tab, isDialogOpen, offset ->
        val quote = quoteRepository.getCurrentQuote(isComeback = false, userOffset = offset)
        val archivedWithStreaks = archivedList.map { habit ->
            HabitWithStreak(
                habit = habit,
                activeStreak = null,
                currentStreakDays = 0,
                rankProgress = com.example.steadfast.domain.RankLadder.getRankProgress(0),
                highestRankAchieved = com.example.steadfast.domain.RankLadder.ranks.first(),
                totalAttempts = 0
            )
        }

        HomeUiState(
            isLoading = false,
            activeHabits = activeList,
            archivedHabits = archivedWithStreaks,
            selectedTab = tab,
            isAddHabitDialogOpen = isDialogOpen,
            quote = quote
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectTab(tab: HabitTab) {
        selectedTab.value = tab
    }

    fun openAddHabitDialog() {
        isAddHabitDialogOpen.value = true
    }

    fun closeAddHabitDialog() {
        isAddHabitDialogOpen.value = false
    }

    fun createHabit(
        name: String,
        icon: String,
        color: Long,
        startDate: LocalDate
    ) {
        viewModelScope.launch {
            habitRepository.createHabit(
                name = name,
                icon = icon,
                color = color,
                startDate = startDate
            )
            isAddHabitDialogOpen.value = false
            WidgetUpdater.updateAll(context)
        }
    }

    fun nextQuote() {
        quoteOffset.value += 1
    }

    fun dismissWhatsNew() {
        _whatsNewRelease.value = null
    }

    fun dismissUpdateDialog() {
        _updateAvailable.value = null
    }

    private fun checkVersionAndUpdates() {
        viewModelScope.launch {
            val lastSeen = settingsRepository.lastSeenVersionFlow.first()
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.7.4"
            } catch (e: Exception) {
                "0.7.4"
            }
            if (lastSeen == null) {
                val habits = habitRepository.allHabits.first()
                if (habits.isNotEmpty()) {
                    val release = ChangelogRepository.getRelease(currentVersion)
                    if (release != null) {
                        _whatsNewRelease.value = release
                    }
                }
                settingsRepository.setLastSeenVersion(currentVersion)
            } else if (lastSeen != currentVersion) {
                val release = ChangelogRepository.getRelease(currentVersion)
                if (release != null) {
                    _whatsNewRelease.value = release
                }
                settingsRepository.setLastSeenVersion(currentVersion)
            }

            try {
                val pending = settingsRepository.pendingUpdateFlow.first()
                if (pending != null) {
                    if (DefaultUpdateChecker.isNewerVersion(pending.version, currentVersion)) {
                        _updateAvailable.value = pending
                    } else {
                        settingsRepository.setPendingUpdate(null)
                    }
                } else {
                    val userSettings = settingsRepository.settingsFlow.first()
                    if (userSettings.autoUpdateFrequency != AutoUpdateFrequency.MANUAL) {
                        val intervalMillis = when (userSettings.autoUpdateFrequency) {
                            AutoUpdateFrequency.DAILY -> 24 * 3600 * 1000L
                            AutoUpdateFrequency.WEEKLY -> 7 * 24 * 3600 * 1000L
                            AutoUpdateFrequency.MANUAL -> Long.MAX_VALUE
                        }
                        val now = System.currentTimeMillis()
                        if (now - userSettings.lastUpdateCheckTime >= intervalMillis) {
                            val result = updateChecker.checkForUpdate(currentVersion)
                            settingsRepository.setLastUpdateCheckTime(now)
                            if (result is UpdateCheckResult.UpdateAvailable) {
                                settingsRepository.setPendingUpdate(result)
                                _updateAvailable.value = result
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    companion object {
        fun provideFactory(
            habitRepository: HabitRepository,
            settingsRepository: SettingsRepository,
            quoteRepository: QuoteRepository,
            context: Context,
            clock: Clock,
            updateChecker: UpdateChecker
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    habitRepository,
                    settingsRepository,
                    quoteRepository,
                    context,
                    clock,
                    updateChecker
                ) as T
            }
        }
    }
}

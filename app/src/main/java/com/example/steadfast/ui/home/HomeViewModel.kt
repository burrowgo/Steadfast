package com.example.steadfast.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.AutoUpdateFrequency
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.updater.DefaultUpdateChecker
import com.example.steadfast.data.updater.UpdateCheckResult
import com.example.steadfast.data.updater.UpdateChecker
import com.example.steadfast.domain.ChangelogRelease
import com.example.steadfast.domain.ChangelogRepository
import com.example.steadfast.domain.Quote
import com.example.steadfast.domain.QuoteRepository
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.RankProgress
import com.example.steadfast.domain.StreakCalculator
import com.example.steadfast.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

import com.example.steadfast.data.prefs.FirstDayOfWeek

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object FirstRun : HomeUiState
    data class Active(
        val streak: StreakEntity,
        val habitName: String,
        val days: Int,
        val rankProgress: RankProgress,
        val quote: Quote,
        val isResetSheetOpen: Boolean = false,
        val rankUpToCelebrate: Rank? = null,
        val history: List<StreakEntity> = emptyList(),
        val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY
    ) : HomeUiState
}

sealed interface HomeEvent {
    data object ShowResetSuccessSnackbar : HomeEvent
}

private data class StreakData(
    val active: StreakEntity?,
    val history: List<StreakEntity>
)

class HomeViewModel(
    application: android.app.Application,
    private val streakRepository: StreakRepository,
    private val settingsRepository: SettingsRepository,
    private val quoteRepository: QuoteRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val updateChecker: UpdateChecker = DefaultUpdateChecker()
) : androidx.lifecycle.AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val isResetSheetOpen = MutableStateFlow(false)
    private val rankToCelebrate = MutableStateFlow<Rank?>(null)
    private val quoteOffset = MutableStateFlow(0)
    private val _whatsNewRelease = MutableStateFlow<ChangelogRelease?>(null)
    val whatsNewRelease: StateFlow<ChangelogRelease?> = _whatsNewRelease.asStateFlow()

    private val _updateAvailable = MutableStateFlow<UpdateCheckResult.UpdateAvailable?>(null)
    val updateAvailable: StateFlow<UpdateCheckResult.UpdateAvailable?> = _updateAvailable.asStateFlow()

    init {
        val app = getApplication<android.app.Application>()

        viewModelScope.launch {
            val lastSeen = settingsRepository.lastSeenVersionFlow.first()
            val currentVersion = try {
                app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }
            if (lastSeen == null) {
                // Check if user is upgrading from a previous version without last_seen_version set
                val hasExistingHabit = streakRepository.activeStreak.first() != null ||
                        streakRepository.history.first().isNotEmpty()
                if (hasExistingHabit) {
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

            // Periodic / background update checking
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
                // Silent fail for background check on launch
            }
        }

        val streakDataFlow = combine(
            streakRepository.activeStreak,
            streakRepository.history
        ) { active, history -> StreakData(active, history) }

        // Observe rank progression independently to trigger celebration without modifying state inside combine transform
        viewModelScope.launch {
            combine(streakDataFlow, settingsRepository.settingsFlow) { data, settings ->
                data to settings
            }.collect { (data, settings) ->
                val active = data.active ?: return@collect
                val nowMillis = clock.millis()
                val days = StreakCalculator.calculateActiveStreakDays(active, nowMillis, clock)
                val currentRank = RankLadder.getRankForDays(days)
                if (currentRank.level > settings.lastCelebratedRankIndex && rankToCelebrate.value == null) {
                    rankToCelebrate.value = currentRank
                    settingsRepository.setLastCelebratedRankIndex(currentRank.level)
                }
            }
        }

        viewModelScope.launch {
            combine(
                streakDataFlow,
                settingsRepository.settingsFlow,
                isResetSheetOpen,
                rankToCelebrate,
                quoteOffset
            ) { data, settings, isSheetOpen, celebrationRank, offset ->
                val active = data.active
                if (active == null) {
                    HomeUiState.FirstRun
                } else {
                    val nowMillis = clock.millis()
                    val days = StreakCalculator.calculateActiveStreakDays(active, nowMillis, clock)
                    val progress = RankLadder.getRankProgress(days)

                    // Check if a reset occurred within the comeback window
                    val latestEnded = data.history.firstOrNull()
                    val isWithin24HoursOfReset = latestEnded?.endedAt?.let {
                        (nowMillis - it) < StreakCalculator.COMEBACK_QUOTE_WINDOW_MILLIS
                    } ?: false

                    val quote = quoteRepository.getPeriodicQuote(
                        isComeback = isWithin24HoursOfReset,
                        nowMillis = nowMillis,
                        userOffset = offset
                    )

                    HomeUiState.Active(
                        streak = active,
                        habitName = active.habitName,
                        days = days,
                        rankProgress = progress,
                        quote = quote,
                        isResetSheetOpen = isSheetOpen,
                        rankUpToCelebrate = celebrationRank,
                        history = data.history,
                        firstDayOfWeek = settings.firstDayOfWeek
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setFirstDayOfWeek(firstDay: FirstDayOfWeek) {
        viewModelScope.launch {
            settingsRepository.setFirstDayOfWeek(firstDay)
        }
    }

    fun startHabit(name: String, startDate: LocalDate = LocalDate.now(clock)) {
        viewModelScope.launch {
            streakRepository.startHabit(name, startDate)
            settingsRepository.setHabitName(name)
            settingsRepository.setLastCelebratedRankIndex(0)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun openResetSheet() {
        isResetSheetOpen.value = true
    }

    fun closeResetSheet() {
        isResetSheetOpen.value = false
    }

    fun confirmReset(reason: String?) {
        viewModelScope.launch {
            isResetSheetOpen.value = false
            streakRepository.resetStreak(reason)
            settingsRepository.setLastCelebratedRankIndex(0)
            quoteOffset.value = 0
            WidgetUpdater.updateAll(getApplication())
            _events.emit(HomeEvent.ShowResetSuccessSnackbar)
        }
    }

    fun undoReset() {
        viewModelScope.launch {
            streakRepository.undoLastReset()
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun nextQuote() {
        quoteOffset.value += 1
    }

    fun dismissCelebration() {
        rankToCelebrate.value = null
    }

    fun dismissWhatsNew() {
        _whatsNewRelease.value = null
    }

    fun dismissUpdateDialog() {
        _updateAvailable.value = null
        viewModelScope.launch {
            settingsRepository.setPendingUpdate(null)
        }
    }

    companion object {
        fun provideFactory(
            application: android.app.Application,
            streakRepository: StreakRepository,
            settingsRepository: SettingsRepository,
            quoteRepository: QuoteRepository,
            clock: Clock = Clock.systemDefaultZone(),
            updateChecker: UpdateChecker = DefaultUpdateChecker()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(application, streakRepository, settingsRepository, quoteRepository, clock, updateChecker) as T
            }
        }
    }
}

package com.example.steadfast.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.domain.Quote
import com.example.steadfast.domain.QuoteRepository
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.RankProgress
import com.example.steadfast.domain.StreakCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

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
        val rankUpToCelebrate: Rank? = null
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
    private val streakRepository: StreakRepository,
    private val settingsRepository: SettingsRepository,
    private val quoteRepository: QuoteRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private val isResetSheetOpen = MutableStateFlow(false)
    private val rankToCelebrate = MutableStateFlow<Rank?>(null)
    private val quoteOffset = MutableStateFlow(0)

    init {
        val streakDataFlow = combine(
            streakRepository.activeStreak,
            streakRepository.history
        ) { active, history -> StreakData(active, history) }

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
                    val today = StreakCalculator.today(clock)
                    val days = StreakCalculator.streakDays(LocalDate.ofEpochDay(active.startDate), today)
                    val progress = RankLadder.getRankProgress(days)

                    // Rank-up celebration trigger
                    if (progress.currentRank.level > settings.lastCelebratedRankIndex && celebrationRank == null) {
                        viewModelScope.launch {
                            rankToCelebrate.value = progress.currentRank
                            settingsRepository.setLastCelebratedRankIndex(progress.currentRank.level)
                        }
                    }

                    // Check if a reset occurred within the last 24 hours
                    val latestEnded = data.history.firstOrNull()
                    val nowMillis = clock.millis()
                    val isWithin24HoursOfReset = latestEnded?.endedAt?.let {
                        (nowMillis - it) < (24 * 60 * 60 * 1000L)
                    } ?: false

                    val quote = quoteRepository.getQuoteForDay(
                        isComeback = isWithin24HoursOfReset,
                        date = today,
                        userOffset = offset
                    )

                    HomeUiState.Active(
                        streak = active,
                        habitName = active.habitName,
                        days = days,
                        rankProgress = progress,
                        quote = quote,
                        isResetSheetOpen = isSheetOpen,
                        rankUpToCelebrate = celebrationRank
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startHabit(name: String) {
        viewModelScope.launch {
            streakRepository.startHabit(name)
            settingsRepository.setHabitName(name)
            settingsRepository.setLastCelebratedRankIndex(0)
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
            _events.emit(HomeEvent.ShowResetSuccessSnackbar)
        }
    }

    fun undoReset() {
        viewModelScope.launch {
            streakRepository.undoLastReset()
        }
    }

    fun nextQuote() {
        quoteOffset.value += 1
    }

    fun dismissCelebration() {
        rankToCelebrate.value = null
    }

    companion object {
        fun provideFactory(
            streakRepository: StreakRepository,
            settingsRepository: SettingsRepository,
            quoteRepository: QuoteRepository,
            clock: Clock
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(streakRepository, settingsRepository, quoteRepository, clock) as T
            }
        }
    }
}

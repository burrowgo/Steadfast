package com.example.steadfast.ui.ranks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.HabitRepository
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.RankProgress
import com.example.steadfast.domain.StreakCalculator
import com.example.steadfast.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

data class RanksUiState(
    val habits: List<Habit> = emptyList(),
    val selectedHabit: Habit? = null,
    val currentStreakDays: Int = 0,
    val currentRank: Rank = RankLadder.ranks.first(),
    val highestRankAchieved: Rank = RankLadder.ranks.first(),
    val rankProgress: RankProgress = RankLadder.getRankProgress(0)
)

class RanksViewModel(
    private val streakRepository: StreakRepository,
    private val habitRepository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val selectedHabitIdState = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<RanksUiState> = combine(
        habitRepository.activeHabits,
        selectedHabitIdState,
        streakRepository.allActiveStreaks,
        streakRepository.allHistory
    ) { activeHabits, selectedHabitId, activeStreaks, historyList ->
        val selected = activeHabits.find { it.id == selectedHabitId } ?: activeHabits.firstOrNull()

        if (selected != null) {
            val active = activeStreaks.find { it.habitId == selected.id }
            val habitHistory = historyList.filter { it.habitId == selected.id }
            val stats = streakRepository.computeStats(active, habitHistory)

            val today = StreakCalculator.today(clock)
            val days = if (active != null) {
                StreakCalculator.streakDays(LocalDate.ofEpochDay(active.startDate), today)
            } else {
                0
            }
            val progress = RankLadder.getRankProgress(days)

            RanksUiState(
                habits = activeHabits,
                selectedHabit = selected,
                currentStreakDays = days,
                currentRank = progress.currentRank,
                highestRankAchieved = stats.highestRankAchieved,
                rankProgress = progress
            )
        } else {
            RanksUiState(
                habits = activeHabits,
                selectedHabit = null,
                currentStreakDays = 0,
                currentRank = RankLadder.ranks.first(),
                highestRankAchieved = RankLadder.ranks.first(),
                rankProgress = RankLadder.getRankProgress(0)
            )
        }
    }.stateIn(
        scope = scope,
        started = sharingStarted,
        initialValue = RanksUiState()
    )

    fun selectHabit(habitId: Long) {
        selectedHabitIdState.value = habitId
    }

    companion object {
        fun provideFactory(
            streakRepository: StreakRepository,
            habitRepository: HabitRepository,
            clock: Clock
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RanksViewModel(streakRepository, habitRepository, clock) as T
            }
        }
    }
}

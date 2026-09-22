package com.example.steadfast.ui.ranks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.RankProgress
import com.example.steadfast.domain.StreakCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

data class RanksUiState(
    val currentStreakDays: Int = 0,
    val currentRank: Rank = RankLadder.ranks.first(),
    val highestRankAchieved: Rank = RankLadder.ranks.first(),
    val rankProgress: RankProgress = RankLadder.getRankProgress(0)
)

class RanksViewModel(
    streakRepository: StreakRepository,
    clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    val uiState: StateFlow<RanksUiState> = combine(
        streakRepository.activeStreak,
        streakRepository.statsFlow
    ) { active, stats ->
        val nowMillis = clock.millis()
        val days = if (active != null) {
            if (active.startedAt > 0L) {
                StreakCalculator.streakDays(active.startedAt, nowMillis)
            } else {
                StreakCalculator.streakDays(LocalDate.ofEpochDay(active.startDate), StreakCalculator.today(clock))
            }
        } else {
            0
        }
        val progress = RankLadder.getRankProgress(days)

        RanksUiState(
            currentStreakDays = days,
            currentRank = progress.currentRank,
            highestRankAchieved = stats.highestRankAchieved,
            rankProgress = progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RanksUiState()
    )

    companion object {
        fun provideFactory(
            streakRepository: StreakRepository,
            clock: Clock
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RanksViewModel(streakRepository, clock) as T
            }
        }
    }
}

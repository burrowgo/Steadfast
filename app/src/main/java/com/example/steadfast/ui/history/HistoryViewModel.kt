package com.example.steadfast.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.StreakHistoryStats
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankLadder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class HistoryItem(
    val entity: StreakEntity,
    val lengthDays: Int,
    val rankAchieved: Rank,
    val dateRangeText: String,
    val reason: String?
)

data class HistoryUiState(
    val stats: StreakHistoryStats = StreakHistoryStats(0, 0, 0, RankLadder.ranks.first()),
    val items: List<HistoryItem> = emptyList(),
    val editingStreak: StreakEntity? = null
)

class HistoryViewModel(
    private val streakRepository: StreakRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val editingStreakState = MutableStateFlow<StreakEntity?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        streakRepository.statsFlow,
        streakRepository.history,
        editingStreakState
    ) { stats, historyList, editing ->
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

        val items = historyList.map { entity ->
            val days = entity.lengthDays ?: 0
            val rank = RankLadder.getRankForDays(days)
            val startLocalDate = LocalDate.ofEpochDay(entity.startDate)
            val endLocalDate = entity.endDate?.let { LocalDate.ofEpochDay(it) } ?: startLocalDate

            val dateRange = "${formatter.format(startLocalDate)} – ${formatter.format(endLocalDate)}"

            HistoryItem(
                entity = entity,
                lengthDays = days,
                rankAchieved = rank,
                dateRangeText = dateRange,
                reason = entity.reason
            )
        }

        HistoryUiState(
            stats = stats,
            items = items,
            editingStreak = editing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun openEditReason(entity: StreakEntity) {
        editingStreakState.value = entity
    }

    fun dismissEditReason() {
        editingStreakState.value = null
    }

    fun saveReason(id: Long, newReason: String?) {
        viewModelScope.launch {
            streakRepository.updateReason(id, newReason)
            editingStreakState.value = null
        }
    }

    companion object {
        fun provideFactory(
            streakRepository: StreakRepository,
            clock: Clock
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(streakRepository, clock) as T
            }
        }
    }
}

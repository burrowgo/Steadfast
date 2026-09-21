package com.example.steadfast.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.HabitRepository
import com.example.steadfast.data.StreakHistoryStats
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class HistoryItem(
    val entity: StreakEntity,
    val habitName: String,
    val habitIcon: String?,
    val habitColor: Long?,
    val lengthDays: Int,
    val rankAchieved: Rank,
    val dateRangeText: String,
    val reason: String?
)

data class HistoryUiState(
    val habits: List<Habit> = emptyList(),
    val selectedHabitId: Long? = null,
    val stats: StreakHistoryStats = StreakHistoryStats(0, 0, 0, RankLadder.ranks.first()),
    val items: List<HistoryItem> = emptyList(),
    val editingStreak: StreakEntity? = null
)

class HistoryViewModel(
    private val streakRepository: StreakRepository,
    private val habitRepository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val selectedHabitIdState = MutableStateFlow<Long?>(null)
    private val editingStreakState = MutableStateFlow<StreakEntity?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        habitRepository.allHabits,
        selectedHabitIdState,
        streakRepository.allActiveStreaks,
        streakRepository.allHistory,
        editingStreakState
    ) { allHabits, selectedHabitId, activeStreaks, historyList, editing ->
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val habitsMap = allHabits.associateBy { it.id }

        val filteredHistory = if (selectedHabitId != null) {
            historyList.filter { it.habitId == selectedHabitId }
        } else {
            historyList
        }

        val filteredActive = if (selectedHabitId != null) {
            activeStreaks.filter { it.habitId == selectedHabitId }
        } else {
            activeStreaks
        }

        val stats = if (selectedHabitId != null) {
            streakRepository.computeStats(filteredActive.firstOrNull(), filteredHistory)
        } else {
            streakRepository.computeAllStats(activeStreaks, historyList)
        }

        val items = filteredHistory.map { entity ->
            val days = entity.lengthDays ?: 0
            val rank = RankLadder.getRankForDays(days)
            val startLocalDate = LocalDate.ofEpochDay(entity.startDate)
            val endLocalDate = entity.endDate?.let { LocalDate.ofEpochDay(it) } ?: startLocalDate

            val dateRange = "${formatter.format(startLocalDate)} – ${formatter.format(endLocalDate)}"
            val habit = habitsMap[entity.habitId]

            HistoryItem(
                entity = entity,
                habitName = habit?.name ?: entity.habitName.ifBlank { "Habit #${entity.habitId}" },
                habitIcon = habit?.icon,
                habitColor = habit?.color,
                lengthDays = days,
                rankAchieved = rank,
                dateRangeText = dateRange,
                reason = entity.reason
            )
        }

        HistoryUiState(
            habits = allHabits,
            selectedHabitId = selectedHabitId,
            stats = stats,
            items = items,
            editingStreak = editing
        )
    }.stateIn(
        scope = scope,
        started = sharingStarted,
        initialValue = HistoryUiState()
    )

    fun selectHabit(habitId: Long?) {
        selectedHabitIdState.value = habitId
    }

    fun openEditReason(entity: StreakEntity) {
        editingStreakState.value = entity
    }

    fun dismissEditReason() {
        editingStreakState.value = null
    }

    fun saveReason(id: Long, newReason: String?) {
        scope.launch {
            streakRepository.updateReason(id, newReason)
            editingStreakState.value = null
        }
    }

    companion object {
        fun provideFactory(
            streakRepository: StreakRepository,
            habitRepository: HabitRepository,
            clock: Clock
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(streakRepository, habitRepository, clock) as T
            }
        }
    }
}

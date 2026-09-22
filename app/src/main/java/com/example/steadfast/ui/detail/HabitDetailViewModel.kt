package com.example.steadfast.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steadfast.data.HabitRepository
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.FirstDayOfWeek
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.domain.Quote
import com.example.steadfast.domain.QuoteRepository
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.model.HabitWithStreak
import com.example.steadfast.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

sealed interface HabitDetailEvent {
    data object ShowResetSuccessSnackbar : HabitDetailEvent
    data object HabitDeleted : HabitDetailEvent
}

data class HabitDetailUiState(
    val habitWithStreak: HabitWithStreak? = null,
    val history: List<StreakEntity> = emptyList(),
    val quote: Quote? = null,
    val isResetSheetOpen: Boolean = false,
    val isEditDialogOpen: Boolean = false,
    val isDeleteDialogOpen: Boolean = false,
    val editingStreakReason: StreakEntity? = null,
    val rankUpToCelebrate: Rank? = null,
    val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY
)

class HabitDetailViewModel(
    private val habitId: Long,
    private val habitRepository: HabitRepository,
    private val streakRepository: StreakRepository,
    private val quoteRepository: QuoteRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val isResetSheetOpen = MutableStateFlow(false)
    private val isEditDialogOpen = MutableStateFlow(false)
    private val isDeleteDialogOpen = MutableStateFlow(false)
    private val editingStreakReason = MutableStateFlow<StreakEntity?>(null)
    private val quoteOffset = MutableStateFlow(0)
    private val rankToCelebrate = MutableStateFlow<Rank?>(null)

    private val _events = MutableSharedFlow<HabitDetailEvent>()
    val events: SharedFlow<HabitDetailEvent> = _events.asSharedFlow()

    val uiState: StateFlow<HabitDetailUiState> = combine(
        habitRepository.observeHabitWithStreak(habitId),
        streakRepository.observeHistory(habitId),
        isResetSheetOpen,
        isEditDialogOpen,
        isDeleteDialogOpen,
        editingStreakReason,
        quoteOffset,
        settingsRepository.settingsFlow.map { it.firstDayOfWeek }
    ) { params: Array<Any?> ->
        val habitWithStreak = params[0] as? HabitWithStreak
        @Suppress("UNCHECKED_CAST")
        val history = params[1] as List<StreakEntity>
        val resetOpen = params[2] as Boolean
        val editOpen = params[3] as Boolean
        val deleteOpen = params[4] as Boolean
        val editReasonStreak = params[5] as? StreakEntity
        val offset = params[6] as Int
        val firstDay = params[7] as FirstDayOfWeek

        val quote = quoteRepository.getCurrentQuote(
            isComeback = false,
            userOffset = offset,
            seedModifier = habitId
        )

        HabitDetailUiState(
            habitWithStreak = habitWithStreak,
            history = history,
            quote = quote,
            isResetSheetOpen = resetOpen,
            isEditDialogOpen = editOpen,
            isDeleteDialogOpen = deleteOpen,
            editingStreakReason = editReasonStreak,
            rankUpToCelebrate = rankToCelebrate.value,
            firstDayOfWeek = firstDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitDetailUiState()
    )

    fun openResetSheet() { isResetSheetOpen.value = true }
    fun closeResetSheet() { isResetSheetOpen.value = false }

    fun confirmReset(reason: String?) {
        viewModelScope.launch {
            streakRepository.resetStreak(habitId, reason)
            isResetSheetOpen.value = false
            WidgetUpdater.updateAll(context)
            _events.emit(HabitDetailEvent.ShowResetSuccessSnackbar)
        }
    }

    fun undoReset() {
        viewModelScope.launch {
            streakRepository.undoLastReset(habitId)
            WidgetUpdater.updateAll(context)
        }
    }

    fun openEditDialog() { isEditDialogOpen.value = true }
    fun closeEditDialog() { isEditDialogOpen.value = false }

    fun saveEdit(name: String, icon: String, color: Long, startDate: LocalDate) {
        viewModelScope.launch {
            habitRepository.updateHabit(habitId, name, icon, color)
            val currentActive = streakRepository.getActiveStreak(habitId)
            if (currentActive != null && currentActive.startDate != startDate.toEpochDay()) {
                streakRepository.updateActiveStartDate(habitId, startDate)
            }
            isEditDialogOpen.value = false
            WidgetUpdater.updateAll(context)
        }
    }

    fun openDeleteDialog() { isDeleteDialogOpen.value = true }
    fun closeDeleteDialog() { isDeleteDialogOpen.value = false }

    fun confirmDelete() {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
            isDeleteDialogOpen.value = false
            WidgetUpdater.updateAll(context)
            _events.emit(HabitDetailEvent.HabitDeleted)
        }
    }

    fun toggleArchive() {
        val current = uiState.value.habitWithStreak?.habit ?: return
        viewModelScope.launch {
            habitRepository.setArchived(habitId, !current.isArchived)
            WidgetUpdater.updateAll(context)
        }
    }

    fun openEditReason(streak: StreakEntity) { editingStreakReason.value = streak }
    fun closeEditReason() { editingStreakReason.value = null }

    fun saveReason(streakId: Long, newReason: String?) {
        viewModelScope.launch {
            streakRepository.updateReason(streakId, newReason)
            editingStreakReason.value = null
        }
    }

    fun nextQuote() { quoteOffset.value += 1 }

    fun setFirstDayOfWeek(firstDay: FirstDayOfWeek) {
        viewModelScope.launch {
            settingsRepository.setFirstDayOfWeek(firstDay)
        }
    }

    companion object {
        fun provideFactory(
            habitId: Long,
            habitRepository: HabitRepository,
            streakRepository: StreakRepository,
            quoteRepository: QuoteRepository,
            settingsRepository: SettingsRepository,
            context: Context,
            clock: Clock
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HabitDetailViewModel(
                    habitId,
                    habitRepository,
                    streakRepository,
                    quoteRepository,
                    settingsRepository,
                    context,
                    clock
                ) as T
            }
        }
    }
}

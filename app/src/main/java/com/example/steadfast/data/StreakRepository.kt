package com.example.steadfast.data

import com.example.steadfast.data.db.StreakDao
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.domain.Rank
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate

data class StreakHistoryStats(
    val longestStreakDays: Int,
    val totalAttempts: Int,
    val currentAttemptNumber: Int,
    val highestRankAchieved: Rank
)

class StreakRepository(
    private val streakDao: StreakDao,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    val activeStreak: Flow<StreakEntity?> = streakDao.observeActiveStreak()
    val history: Flow<List<StreakEntity>> = streakDao.observeHistory()
    val allHistory: Flow<List<StreakEntity>> = streakDao.observeAllHistory()

    val allActiveStreaks: Flow<List<StreakEntity>> = streakDao.observeAllActiveStreaks()

    val statsFlow: Flow<StreakHistoryStats> = combine(activeStreak, history) { active, historyList ->
        computeStats(active, historyList)
    }

    val allStatsFlow: Flow<StreakHistoryStats> = combine(allActiveStreaks, allHistory) { activeList, historyList ->
        computeAllStats(activeList, historyList)
    }

    fun observeActiveStreak(habitId: Long): Flow<StreakEntity?> =
        streakDao.observeActiveStreak(habitId)

    fun observeHistory(habitId: Long): Flow<List<StreakEntity>> =
        streakDao.observeHistory(habitId)

    fun observeStats(habitId: Long): Flow<StreakHistoryStats> =
        combine(observeActiveStreak(habitId), observeHistory(habitId)) { active, historyList ->
            computeStats(active, historyList)
        }

    fun computeStats(active: StreakEntity?, historyList: List<StreakEntity>): StreakHistoryStats {
        val today = StreakCalculator.today(clock)
        val activeLength = if (active != null) {
            StreakCalculator.streakDays(LocalDate.ofEpochDay(active.startDate), today)
        } else {
            0
        }

        val pastLengths = historyList.map {
            it.lengthDays ?: StreakCalculator.streakDays(
                LocalDate.ofEpochDay(it.startDate),
                LocalDate.ofEpochDay(it.endDate ?: it.startDate)
            )
        }

        val allLengths = if (active != null) pastLengths + activeLength else pastLengths
        val longest = allLengths.maxOrNull() ?: 0
        val totalAttempts = historyList.size + (if (active != null) 1 else 0)
        val currentAttemptNumber = if (active != null) totalAttempts else 0
        val highestRank = RankLadder.getRankForDays(longest)

        return StreakHistoryStats(
            longestStreakDays = longest,
            totalAttempts = totalAttempts,
            currentAttemptNumber = currentAttemptNumber,
            highestRankAchieved = highestRank
        )
    }

    fun computeAllStats(activeList: List<StreakEntity>, historyList: List<StreakEntity>): StreakHistoryStats {
        val today = StreakCalculator.today(clock)
        val activeLengths = activeList.map {
            StreakCalculator.streakDays(LocalDate.ofEpochDay(it.startDate), today)
        }

        val pastLengths = historyList.map {
            it.lengthDays ?: StreakCalculator.streakDays(
                LocalDate.ofEpochDay(it.startDate),
                LocalDate.ofEpochDay(it.endDate ?: it.startDate)
            )
        }

        val allLengths = activeLengths + pastLengths
        val longest = allLengths.maxOrNull() ?: 0
        val totalAttempts = historyList.size + activeList.size
        val highestRank = RankLadder.getRankForDays(longest)

        return StreakHistoryStats(
            longestStreakDays = longest,
            totalAttempts = totalAttempts,
            currentAttemptNumber = 0,
            highestRankAchieved = highestRank
        )
    }

    suspend fun getActiveStreak(habitId: Long = 1L): StreakEntity? =
        streakDao.getActiveStreak(habitId)

    suspend fun startHabit(
        habitId: Long,
        habitName: String,
        startDate: LocalDate = StreakCalculator.today(clock)
    ): Long {
        val nowMillis = clock.millis()
        return streakDao.startNewRun(
            habitId = habitId,
            habitName = habitName.trim(),
            startDateEpochDay = startDate.toEpochDay(),
            startedAtMillis = nowMillis
        )
    }

    suspend fun startHabit(
        habitName: String,
        startDate: LocalDate = StreakCalculator.today(clock)
    ): Long = startHabit(1L, habitName, startDate)

    suspend fun updateActiveStartDate(habitId: Long, newStartDate: LocalDate) {
        streakDao.updateActiveStartDate(habitId, newStartDate.toEpochDay())
    }

    suspend fun updateActiveStartDate(newStartDate: LocalDate) {
        updateActiveStartDate(1L, newStartDate)
    }

    suspend fun resetStreak(habitId: Long, reason: String?): Long? {
        val today = StreakCalculator.today(clock)
        val nowMillis = clock.millis()
        return streakDao.resetActiveRun(
            habitId = habitId,
            reason = reason,
            todayEpochDay = today.toEpochDay(),
            nowMillis = nowMillis
        )
    }

    suspend fun resetStreak(reason: String?): Long? =
        resetStreak(1L, reason)

    suspend fun undoLastReset(habitId: Long): Boolean {
        return streakDao.undoLastReset(habitId)
    }

    suspend fun undoLastReset(): Boolean =
        undoLastReset(1L)

    suspend fun updateReason(id: Long, reason: String?) {
        val cleaned = reason?.trim()?.ifBlank { null }?.take(200)
        streakDao.updateReason(id, cleaned)
    }

    suspend fun updateActiveHabitName(habitId: Long, newName: String) {
        val trimmed = newName.trim()
        streakDao.updateHabitNameForHabit(habitId, trimmed)
    }

    suspend fun updateActiveHabitName(newName: String) {
        updateActiveHabitName(1L, newName)
    }

    suspend fun getAllStreaks(): List<StreakEntity> {
        return streakDao.getAllStreaks()
    }

    suspend fun restoreStreaks(streaks: List<StreakEntity>) {
        streakDao.deleteAll()
        streakDao.insertAll(streaks)
    }

    suspend fun clearAllData() {
        streakDao.deleteAll()
    }
}

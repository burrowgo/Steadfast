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

    val statsFlow: Flow<StreakHistoryStats> = combine(activeStreak, history) { active, historyList ->
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

        StreakHistoryStats(
            longestStreakDays = longest,
            totalAttempts = totalAttempts,
            currentAttemptNumber = currentAttemptNumber,
            highestRankAchieved = highestRank
        )
    }

    suspend fun getActiveStreak(): StreakEntity? = streakDao.getActiveStreak()

    suspend fun startHabit(
        habitName: String,
        startDate: LocalDate = StreakCalculator.today(clock)
    ): Long {
        val nowMillis = clock.millis()
        return streakDao.startNewRun(
            habitName = habitName.trim(),
            startDateEpochDay = startDate.toEpochDay(),
            startedAtMillis = nowMillis
        )
    }

    suspend fun updateActiveStartDate(newStartDate: LocalDate) {
        streakDao.updateActiveStartDate(newStartDate.toEpochDay())
    }

    suspend fun resetStreak(reason: String?): Long? {
        val today = StreakCalculator.today(clock)
        val nowMillis = clock.millis()
        return streakDao.resetActiveRun(
            reason = reason,
            todayEpochDay = today.toEpochDay(),
            nowMillis = nowMillis
        )
    }

    suspend fun undoLastReset(): Boolean {
        return streakDao.undoLastReset()
    }

    suspend fun updateReason(id: Long, reason: String?) {
        val cleaned = reason?.trim()?.ifBlank { null }?.take(200)
        streakDao.updateReason(id, cleaned)
    }

    suspend fun updateActiveHabitName(newName: String) {
        streakDao.updateActiveHabitName(newName.trim())
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

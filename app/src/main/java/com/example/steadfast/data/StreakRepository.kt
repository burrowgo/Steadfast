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
        val nowMillis = clock.millis()
        val activeLength = if (active != null) {
            if (active.startedAt > 0L) {
                StreakCalculator.streakDays(active.startedAt, nowMillis)
            } else {
                StreakCalculator.streakDays(LocalDate.ofEpochDay(active.startDate), StreakCalculator.today(clock))
            }
        } else {
            0
        }

        val pastLengths = historyList.map {
            it.lengthDays ?: (
                if (it.startedAt > 0L && (it.endedAt ?: 0L) > 0L) {
                    StreakCalculator.streakDays(it.startedAt, it.endedAt!!)
                } else {
                    StreakCalculator.streakDays(
                        LocalDate.ofEpochDay(it.startDate),
                        LocalDate.ofEpochDay(it.endDate ?: it.startDate)
                    )
                }
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
        val today = StreakCalculator.today(clock)
        val startedAtMillis = if (startDate == today) {
            nowMillis
        } else {
            startDate.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        }
        return streakDao.startNewRun(
            habitName = habitName.trim(),
            startDateEpochDay = startDate.toEpochDay(),
            startedAtMillis = startedAtMillis
        )
    }

    suspend fun updateActiveStartDate(newStartDate: LocalDate) {
        val active = streakDao.getActiveStreak()
        val newStartedAtMillis = if (active != null && active.startedAt > 0L) {
            val zone = clock.zone
            val previousLocalTime = java.time.Instant.ofEpochMilli(active.startedAt).atZone(zone).toLocalTime()
            newStartDate.atTime(previousLocalTime).atZone(zone).toInstant().toEpochMilli()
        } else {
            newStartDate.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        }
        streakDao.updateActiveStartDate(newStartDate.toEpochDay(), newStartedAtMillis)
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

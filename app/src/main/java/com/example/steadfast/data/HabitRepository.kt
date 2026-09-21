package com.example.steadfast.data

import com.example.steadfast.data.db.HabitDao
import com.example.steadfast.data.db.HabitEntity
import com.example.steadfast.data.db.StreakDao
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import com.example.steadfast.domain.model.Habit
import com.example.steadfast.domain.model.HabitWithStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class HabitRepository(
    private val habitDao: HabitDao,
    private val streakDao: StreakDao,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    val activeHabits: Flow<List<Habit>> = habitDao.observeActiveHabits().map { list ->
        list.map { it.toDomain() }
    }

    val archivedHabits: Flow<List<Habit>> = habitDao.observeArchivedHabits().map { list ->
        list.map { it.toDomain() }
    }

    val allHabits: Flow<List<Habit>> = habitDao.observeAllHabits().map { list ->
        list.map { it.toDomain() }
    }

    val activeHabitsWithStreaks: Flow<List<HabitWithStreak>> = combine(
        habitDao.observeActiveHabits(),
        streakDao.observeAllActiveStreaks(),
        streakDao.observeAllHistory()
    ) { habits, activeStreaks, allHistory ->
        val today = StreakCalculator.today(clock)
        val activeByHabit = activeStreaks.associateBy { it.habitId }
        val historyByHabit = allHistory.groupBy { it.habitId }

        habits.map { entity ->
            val habit = entity.toDomain()
            val activeStreak = activeByHabit[habit.id]
            val habitHistory = historyByHabit[habit.id] ?: emptyList()

            val currentDays = if (activeStreak != null) {
                StreakCalculator.streakDays(LocalDate.ofEpochDay(activeStreak.startDate), today)
            } else {
                0
            }

            val pastLengths = habitHistory.map {
                it.lengthDays ?: StreakCalculator.streakDays(
                    LocalDate.ofEpochDay(it.startDate),
                    LocalDate.ofEpochDay(it.endDate ?: it.startDate)
                )
            }
            val allLengths = if (activeStreak != null) pastLengths + currentDays else pastLengths
            val longest = allLengths.maxOrNull() ?: 0
            val highestRank = RankLadder.getRankForDays(longest)
            val rankProgress = RankLadder.getRankProgress(currentDays)
            val totalAttempts = habitHistory.size + (if (activeStreak != null) 1 else 0)

            HabitWithStreak(
                habit = habit,
                activeStreak = activeStreak,
                currentStreakDays = currentDays,
                rankProgress = rankProgress,
                highestRankAchieved = highestRank,
                totalAttempts = totalAttempts
            )
        }
    }

    fun observeHabitWithStreak(habitId: Long): Flow<HabitWithStreak?> = combine(
        habitDao.observeHabitById(habitId),
        streakDao.observeActiveStreak(habitId),
        streakDao.observeHistory(habitId)
    ) { habitEntity, activeStreak, historyList ->
        if (habitEntity == null) return@combine null

        val habit = habitEntity.toDomain()
        val today = StreakCalculator.today(clock)
        val currentDays = if (activeStreak != null) {
            StreakCalculator.streakDays(LocalDate.ofEpochDay(activeStreak.startDate), today)
        } else {
            0
        }

        val pastLengths = historyList.map {
            it.lengthDays ?: StreakCalculator.streakDays(
                LocalDate.ofEpochDay(it.startDate),
                LocalDate.ofEpochDay(it.endDate ?: it.startDate)
            )
        }
        val allLengths = if (activeStreak != null) pastLengths + currentDays else pastLengths
        val longest = allLengths.maxOrNull() ?: 0
        val highestRank = RankLadder.getRankForDays(longest)
        val rankProgress = RankLadder.getRankProgress(currentDays)
        val totalAttempts = historyList.size + (if (activeStreak != null) 1 else 0)

        HabitWithStreak(
            habit = habit,
            activeStreak = activeStreak,
            currentStreakDays = currentDays,
            rankProgress = rankProgress,
            highestRankAchieved = highestRank,
            totalAttempts = totalAttempts
        )
    }

    suspend fun getHabitById(id: Long): Habit? {
        return habitDao.getHabitById(id)?.toDomain()
    }

    suspend fun createHabit(
        name: String,
        icon: String = "shield",
        color: Long = 0xFF4C662BL,
        startDate: LocalDate = StreakCalculator.today(clock)
    ): Long {
        val trimmedName = name.trim().take(40)
        val habitId = habitDao.insert(
            HabitEntity(
                name = trimmedName,
                icon = icon,
                color = color,
                createdAt = clock.millis()
            )
        )
        // Start initial streak for the habit
        streakDao.startNewRun(
            habitId = habitId,
            habitName = trimmedName,
            startDateEpochDay = startDate.toEpochDay(),
            startedAtMillis = clock.millis()
        )
        return habitId
    }

    suspend fun updateHabit(
        id: Long,
        name: String,
        icon: String,
        color: Long
    ) {
        val trimmedName = name.trim().take(40)
        habitDao.updateHabitDetails(id, trimmedName, icon, color)
        streakDao.updateHabitNameForHabit(id, trimmedName)
    }

    suspend fun setArchived(id: Long, isArchived: Boolean) {
        habitDao.setArchived(id, isArchived)
    }

    suspend fun deleteHabit(id: Long) {
        habitDao.deleteHabitById(id)
    }

    suspend fun clearAllData() {
        streakDao.deleteAll()
        habitDao.deleteAll()
    }

    private fun HabitEntity.toDomain() = Habit(
        id = id,
        name = name,
        icon = icon,
        color = color,
        createdAt = createdAt,
        isArchived = isArchived,
        sortOrder = sortOrder
    )
}

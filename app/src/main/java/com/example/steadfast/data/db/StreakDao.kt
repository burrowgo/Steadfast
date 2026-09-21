package com.example.steadfast.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak WHERE habitId = :habitId AND endedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeActiveStreak(habitId: Long): Flow<StreakEntity?>

    @Query("SELECT * FROM streak WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeActiveStreak(): Flow<StreakEntity?>

    @Query("SELECT * FROM streak WHERE endedAt IS NULL ORDER BY id DESC")
    fun observeAllActiveStreaks(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streak WHERE habitId = :habitId AND endedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getActiveStreak(habitId: Long): StreakEntity?

    @Query("SELECT * FROM streak WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getActiveStreak(): StreakEntity?

    @Query("SELECT * FROM streak WHERE habitId = :habitId AND endedAt IS NOT NULL ORDER BY id DESC")
    fun observeHistory(habitId: Long): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streak WHERE endedAt IS NOT NULL ORDER BY id DESC")
    fun observeHistory(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streak WHERE endedAt IS NOT NULL ORDER BY id DESC")
    fun observeAllHistory(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streak WHERE habitId = :habitId AND endedAt IS NOT NULL ORDER BY id DESC")
    suspend fun getHistory(habitId: Long): List<StreakEntity>

    @Query("SELECT * FROM streak WHERE endedAt IS NOT NULL ORDER BY id DESC")
    suspend fun getHistory(): List<StreakEntity>

    @Query("SELECT * FROM streak ORDER BY id DESC")
    suspend fun getAllStreaks(): List<StreakEntity>

    @Query("SELECT * FROM streak WHERE habitId = :habitId ORDER BY id DESC")
    suspend fun getStreaksForHabit(habitId: Long): List<StreakEntity>

    @Query("SELECT * FROM streak WHERE id = :id")
    suspend fun getStreakById(id: Long): StreakEntity?

    @Query("SELECT * FROM streak WHERE habitId = :habitId AND endedAt IS NOT NULL ORDER BY id DESC LIMIT 1")
    suspend fun getLatestEndedStreak(habitId: Long): StreakEntity?

    @Query("SELECT * FROM streak WHERE endedAt IS NOT NULL ORDER BY id DESC LIMIT 1")
    suspend fun getLatestEndedStreak(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: StreakEntity): Long

    @Update
    suspend fun update(streak: StreakEntity)

    @Delete
    suspend fun delete(streak: StreakEntity)

    @Query("DELETE FROM streak WHERE habitId = :habitId")
    suspend fun deleteStreaksForHabit(habitId: Long)

    @Query("DELETE FROM streak")
    suspend fun deleteAll()

    @Query("INSERT OR IGNORE INTO habit (id, name, icon, color, createdAt, isArchived, sortOrder) VALUES (:habitId, :habitName, 'shield', 4283204907, :startedAtMillis, 0, 0)")
    suspend fun ensureHabitExists(habitId: Long, habitName: String, startedAtMillis: Long)

    @Transaction
    suspend fun startNewRun(
        habitId: Long,
        habitName: String,
        startDateEpochDay: Long,
        startedAtMillis: Long
    ): Long {
        ensureHabitExists(habitId, habitName, startedAtMillis)
        val currentActive = getActiveStreak(habitId)
        if (currentActive != null) {
            val length = (startDateEpochDay - currentActive.startDate).coerceAtLeast(0).toInt()
            update(
                currentActive.copy(
                    endedAt = startedAtMillis,
                    endDate = startDateEpochDay,
                    lengthDays = length,
                    reason = null
                )
            )
        }
        return insert(
            StreakEntity(
                habitId = habitId,
                habitName = habitName,
                startDate = startDateEpochDay,
                startedAt = startedAtMillis
            )
        )
    }

    @Transaction
    suspend fun startNewRun(
        habitName: String,
        startDateEpochDay: Long,
        startedAtMillis: Long
    ): Long = startNewRun(1L, habitName, startDateEpochDay, startedAtMillis)

    @Transaction
    suspend fun resetActiveRun(
        habitId: Long,
        reason: String?,
        todayEpochDay: Long,
        nowMillis: Long
    ): Long? {
        val active = getActiveStreak(habitId) ?: return null
        val length = (todayEpochDay - active.startDate).coerceAtLeast(0).toInt()
        val cleanedReason = reason?.trim()?.ifBlank { null }?.take(200)

        // Close active streak
        update(
            active.copy(
                endedAt = nowMillis,
                endDate = todayEpochDay,
                lengthDays = length,
                reason = cleanedReason
            )
        )

        // Start new run immediately starting today
        return insert(
            StreakEntity(
                habitId = habitId,
                habitName = active.habitName,
                startDate = todayEpochDay,
                startedAt = nowMillis
            )
        )
    }

    @Transaction
    suspend fun resetActiveRun(
        reason: String?,
        todayEpochDay: Long,
        nowMillis: Long
    ): Long? = resetActiveRun(1L, reason, todayEpochDay, nowMillis)

    @Transaction
    suspend fun undoLastReset(habitId: Long): Boolean {
        val currentActive = getActiveStreak(habitId) ?: return false
        val lastEnded = getLatestEndedStreak(habitId) ?: return false

        // Remove the active run that was created upon reset
        delete(currentActive)

        // Reopen the last ended run
        update(
            lastEnded.copy(
                endedAt = null,
                endDate = null,
                lengthDays = null,
                reason = null
            )
        )
        return true
    }

    @Transaction
    suspend fun undoLastReset(): Boolean = undoLastReset(1L)

    @Query("UPDATE streak SET reason = :reason WHERE id = :id")
    suspend fun updateReason(id: Long, reason: String?)

    @Query("UPDATE streak SET habitName = :habitName WHERE habitId = :habitId")
    suspend fun updateHabitNameForHabit(habitId: Long, habitName: String)

    @Query("UPDATE streak SET habitName = :habitName WHERE endedAt IS NULL")
    suspend fun updateActiveHabitName(habitName: String)

    @Query("UPDATE streak SET startDate = :newStartDateEpochDay WHERE endedAt IS NULL")
    suspend fun updateActiveStartDate(newStartDateEpochDay: Long)

    @Query("UPDATE streak SET startDate = :newStartDateEpochDay WHERE habitId = :habitId AND endedAt IS NULL")
    suspend fun updateActiveStartDate(habitId: Long, newStartDateEpochDay: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(streaks: List<StreakEntity>)
}

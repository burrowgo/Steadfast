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
    @Query("SELECT * FROM streak WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeActiveStreak(): Flow<StreakEntity?>

    @Query("SELECT * FROM streak WHERE endedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getActiveStreak(): StreakEntity?

    @Query("SELECT * FROM streak WHERE endedAt IS NOT NULL ORDER BY id DESC")
    fun observeHistory(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streak WHERE endedAt IS NOT NULL ORDER BY id DESC")
    suspend fun getHistory(): List<StreakEntity>

    @Query("SELECT * FROM streak ORDER BY id DESC")
    suspend fun getAllStreaks(): List<StreakEntity>

    @Query("SELECT * FROM streak WHERE id = :id")
    suspend fun getStreakById(id: Long): StreakEntity?

    @Query("SELECT * FROM streak WHERE endedAt IS NOT NULL ORDER BY id DESC LIMIT 1")
    suspend fun getLatestEndedStreak(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: StreakEntity): Long

    @Update
    suspend fun update(streak: StreakEntity)

    @Delete
    suspend fun delete(streak: StreakEntity)

    @Query("DELETE FROM streak")
    suspend fun deleteAll()

    @Transaction
    suspend fun startNewRun(
        habitName: String,
        startDateEpochDay: Long,
        startedAtMillis: Long
    ): Long {
        // Enforce at most one active streak: close any existing active streak
        val currentActive = getActiveStreak()
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
                habitName = habitName,
                startDate = startDateEpochDay,
                startedAt = startedAtMillis
            )
        )
    }

    @Transaction
    suspend fun resetActiveRun(
        reason: String?,
        todayEpochDay: Long,
        nowMillis: Long
    ): Long? {
        val active = getActiveStreak() ?: return null
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
                habitName = active.habitName,
                startDate = todayEpochDay,
                startedAt = nowMillis
            )
        )
    }

    @Transaction
    suspend fun undoLastReset(): Boolean {
        val currentActive = getActiveStreak() ?: return false
        val lastEnded = getLatestEndedStreak() ?: return false

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

    @Query("UPDATE streak SET reason = :reason WHERE id = :id")
    suspend fun updateReason(id: Long, reason: String?)

    @Query("UPDATE streak SET habitName = :habitName WHERE endedAt IS NULL")
    suspend fun updateActiveHabitName(habitName: String)

    @Query("UPDATE streak SET startDate = :newStartDateEpochDay WHERE endedAt IS NULL")
    suspend fun updateActiveStartDate(newStartDateEpochDay: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(streaks: List<StreakEntity>)
}

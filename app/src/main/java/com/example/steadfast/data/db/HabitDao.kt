package com.example.steadfast.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habit ORDER BY sortOrder ASC, id ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit WHERE isArchived = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeArchivedHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit WHERE id = :id LIMIT 1")
    fun observeHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habit WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habit ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun getActiveHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("DELETE FROM habit WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("UPDATE habit SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE habit SET name = :name, icon = :icon, color = :color WHERE id = :id")
    suspend fun updateHabitDetails(id: Long, name: String, icon: String, color: Long)

    @Query("DELETE FROM habit")
    suspend fun deleteAll()
}

package com.example.steadfast.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streak",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"])
    ]
)
data class StreakEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long = 1,
    val habitName: String,
    val startDate: Long, // LocalDate.toEpochDay()
    val startedAt: Long, // Epoch millis
    val endedAt: Long? = null, // Epoch millis, null = active
    val endDate: Long? = null, // LocalDate.toEpochDay() of reset
    val lengthDays: Int? = null,
    val reason: String? = null
)

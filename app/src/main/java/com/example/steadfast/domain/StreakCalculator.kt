package com.example.steadfast.domain

import com.example.steadfast.data.db.StreakEntity
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

object StreakCalculator {
    const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    const val MAX_HABIT_NAME_LENGTH = 40
    const val MAX_REASON_LENGTH = 200
    const val DEFAULT_REMINDER_TIME = "20:00"
    const val COMEBACK_QUOTE_WINDOW_MILLIS = 24 * 60 * 60 * 1000L

    fun streakDays(startedAtMillis: Long, nowMillis: Long): Int {
        if (startedAtMillis <= 0L || nowMillis <= startedAtMillis) return 0
        return ((nowMillis - startedAtMillis) / MILLIS_PER_DAY).toInt()
    }

    fun streakDays(startDate: LocalDate, today: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(startDate, today).toInt()
        return max(0, days)
    }

    fun today(clock: Clock = Clock.systemDefaultZone()): LocalDate = LocalDate.now(clock)

    /**
     * Calculates the completed days for an active streak.
     * Uses 24-hour cycle from [StreakEntity.startedAt] if > 0, otherwise
     * falls back to the day difference from [StreakEntity.startDate].
     */
    fun calculateActiveStreakDays(
        streak: StreakEntity?,
        nowMillis: Long,
        clock: Clock = Clock.systemDefaultZone()
    ): Int {
        if (streak == null) return 0
        return if (streak.startedAt > 0L) {
            streakDays(streak.startedAt, nowMillis)
        } else {
            val start = LocalDate.ofEpochDay(streak.startDate)
            val todayDate = today(clock)
            streakDays(start, todayDate)
        }
    }

    /**
     * Calculates the completed run length for an ended streak.
     * Prefers precomputed [StreakEntity.lengthDays], then 24-hour cycle between
     * [StreakEntity.startedAt] and [StreakEntity.endedAt], then day difference.
     */
    fun calculateEndedStreakDays(streak: StreakEntity): Int {
        streak.lengthDays?.let { return it }

        val endedAt = streak.endedAt
        if (streak.startedAt > 0L && endedAt != null && endedAt > 0L) {
            return streakDays(streak.startedAt, endedAt)
        }

        val start = LocalDate.ofEpochDay(streak.startDate)
        val end = LocalDate.ofEpochDay(streak.endDate ?: streak.startDate)
        return streakDays(start, end)
    }
}


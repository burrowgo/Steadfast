package com.example.steadfast.domain

import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

object StreakCalculator {
    const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    fun streakDays(startedAtMillis: Long, nowMillis: Long): Int {
        if (startedAtMillis <= 0L || nowMillis <= startedAtMillis) return 0
        return ((nowMillis - startedAtMillis) / MILLIS_PER_DAY).toInt()
    }

    fun streakDays(startDate: LocalDate, today: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(startDate, today).toInt()
        return max(0, days)
    }

    fun today(clock: Clock): LocalDate = LocalDate.now(clock)
}

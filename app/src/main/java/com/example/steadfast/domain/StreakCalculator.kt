package com.example.steadfast.domain

import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

object StreakCalculator {
    fun streakDays(startDate: LocalDate, today: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(startDate, today).toInt()
        return max(0, days)
    }

    fun today(clock: Clock): LocalDate = LocalDate.now(clock)
}

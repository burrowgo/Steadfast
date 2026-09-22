package com.example.steadfast.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class StreakCalculatorTest {

    @Test
    fun `same day returns 0`() {
        val date = LocalDate.of(2026, 9, 21)
        assertEquals(0, StreakCalculator.streakDays(date, date))
    }

    @Test
    fun `next day returns 1`() {
        val start = LocalDate.of(2026, 9, 20)
        val today = LocalDate.of(2026, 9, 21)
        assertEquals(1, StreakCalculator.streakDays(start, today))
    }

    @Test
    fun `exactly 7 days returns 7`() {
        val start = LocalDate.of(2026, 9, 14)
        val today = LocalDate.of(2026, 9, 21)
        assertEquals(7, StreakCalculator.streakDays(start, today))
    }

    @Test
    fun `exactly 30 days returns 30`() {
        val start = LocalDate.of(2026, 8, 22)
        val today = LocalDate.of(2026, 9, 21)
        assertEquals(30, StreakCalculator.streakDays(start, today))
    }

    @Test
    fun `across month boundary`() {
        val startNonLeap = LocalDate.of(2025, 2, 28)
        val endNonLeap = LocalDate.of(2025, 3, 1)
        assertEquals(1, StreakCalculator.streakDays(startNonLeap, endNonLeap))

        val startLeap = LocalDate.of(2024, 2, 28)
        val endLeap = LocalDate.of(2024, 3, 1)
        assertEquals(2, StreakCalculator.streakDays(startLeap, endLeap))
    }

    @Test
    fun `across year boundary`() {
        val start = LocalDate.of(2025, 12, 30)
        val today = LocalDate.of(2026, 1, 2)
        assertEquals(3, StreakCalculator.streakDays(start, today))
    }

    @Test
    fun `across DST transition dates does not affect day count`() {
        // Fall DST transition (e.g. Nov 1 to Nov 2 in US)
        val start = LocalDate.of(2026, 10, 31)
        val today = LocalDate.of(2026, 11, 2)
        assertEquals(2, StreakCalculator.streakDays(start, today))

        // Spring DST transition (e.g. Mar 7 to Mar 9 in US)
        val springStart = LocalDate.of(2026, 3, 7)
        val springToday = LocalDate.of(2026, 3, 9)
        assertEquals(2, StreakCalculator.streakDays(springStart, springToday))
    }

    @Test
    fun `future start date clamps to 0`() {
        val futureStart = LocalDate.of(2026, 9, 25)
        val today = LocalDate.of(2026, 9, 21)
        assertEquals(0, StreakCalculator.streakDays(futureStart, today))
    }

    @Test
    fun `timezone change does not produce negative days`() {
        // Even if local date shifts due to timezone
        val startZoned = ZonedDateTime.of(2026, 9, 21, 23, 0, 0, 0, ZoneId.of("America/New_York")).toLocalDate()
        val todayZoned = ZonedDateTime.of(2026, 9, 21, 1, 0, 0, 0, ZoneId.of("Pacific/Auckland")).toLocalDate()
        // Auckland is a day ahead of New York
        val days = StreakCalculator.streakDays(startZoned, todayZoned)
        assertEquals(0, days) // clamped if negative, or proper difference
    }

    @Test
    fun `24-hour counting - less than 24 hours returns 0`() {
        val startMillis = 1_000_000L
        val hour23 = startMillis + (23 * 3600 * 1000L) + (59 * 60 * 1000L)
        assertEquals(0, StreakCalculator.streakDays(startMillis, hour23))
    }

    @Test
    fun `24-hour counting - exactly 24 hours returns 1`() {
        val startMillis = 1_000_000L
        val exactly24Hours = startMillis + (24 * 3600 * 1000L)
        assertEquals(1, StreakCalculator.streakDays(startMillis, exactly24Hours))
    }

    @Test
    fun `24-hour counting - 47 hours returns 1 and 48 hours returns 2`() {
        val startMillis = 1_000_000L
        val hour47 = startMillis + (47 * 3600 * 1000L)
        assertEquals(1, StreakCalculator.streakDays(startMillis, hour47))

        val exactly48Hours = startMillis + (48 * 3600 * 1000L)
        assertEquals(2, StreakCalculator.streakDays(startMillis, exactly48Hours))
    }

    @Test
    fun `24-hour counting - future or invalid start time returns 0`() {
        val startMillis = 5_000_000L
        val pastNow = 2_000_000L
        assertEquals(0, StreakCalculator.streakDays(startMillis, pastNow))
        assertEquals(0, StreakCalculator.streakDays(0L, pastNow))
        assertEquals(0, StreakCalculator.streakDays(-100L, pastNow))
    }
}

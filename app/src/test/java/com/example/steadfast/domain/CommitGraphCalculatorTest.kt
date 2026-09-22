package com.example.steadfast.domain

import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.FirstDayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CommitGraphCalculatorTest {

    private val today = LocalDate.of(2026, 9, 22) // A Tuesday

    @Test
    fun `monday first aligns week start to Monday and end to Sunday`() {
        val grid = CommitGraphCalculator.calculateGrid(
            history = emptyList(),
            activeStreak = null,
            today = today,
            firstDayOfWeek = FirstDayOfWeek.MONDAY,
            numWeeks = 4
        )

        assertEquals(4, grid.columns.size)
        grid.columns.forEach { col ->
            assertEquals(7, col.days.size)
            assertEquals(DayOfWeek.MONDAY, col.days.first().date.dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, col.days.last().date.dayOfWeek)
        }

        // Today (Tuesday Sep 22) is row 1 of the last column
        val lastCol = grid.columns.last()
        assertEquals(today, lastCol.days[1].date)
        // Future days
        assertEquals(DayCommitStatus.FUTURE, lastCol.days[2].status)
        assertEquals(DayCommitStatus.FUTURE, lastCol.days[6].status)
    }

    @Test
    fun `sunday first aligns week start to Sunday and end to Saturday`() {
        val grid = CommitGraphCalculator.calculateGrid(
            history = emptyList(),
            activeStreak = null,
            today = today,
            firstDayOfWeek = FirstDayOfWeek.SUNDAY,
            numWeeks = 4
        )

        assertEquals(4, grid.columns.size)
        grid.columns.forEach { col ->
            assertEquals(7, col.days.size)
            assertEquals(DayOfWeek.SUNDAY, col.days.first().date.dayOfWeek)
            assertEquals(DayOfWeek.SATURDAY, col.days.last().date.dayOfWeek)
        }

        // Today (Tuesday Sep 22) is row 2 of the last column (Sunday=0, Monday=1, Tuesday=2)
        val lastCol = grid.columns.last()
        assertEquals(today, lastCol.days[2].date)
        // Future days
        assertEquals(DayCommitStatus.FUTURE, lastCol.days[3].status)
        assertEquals(DayCommitStatus.FUTURE, lastCol.days[6].status)
    }

    @Test
    fun `maintained days are marked correctly with intensity and in-progress day is not marked done`() {
        // Active streak started 5 days ago (Sep 17) at startedAt
        val startEpoch = today.minusDays(5).toEpochDay()
        val startedAt = 1_000_000L
        // Exactly 5 full 24-hour cycles have completed: Sep 17, 18, 19, 20, 21 completed.
        // Today (Sep 22) has not completed its 24 hours yet.
        val nowMillis = startedAt + (5 * 24 * 3600 * 1000L) + (10 * 3600 * 1000L) // 5 days 10 hours in
        val active = StreakEntity(
            id = 1,
            habitName = "Fitness",
            startDate = startEpoch,
            startedAt = startedAt
        )

        val grid = CommitGraphCalculator.calculateGrid(
            history = emptyList(),
            activeStreak = active,
            today = today,
            firstDayOfWeek = FirstDayOfWeek.MONDAY,
            numWeeks = 4,
            nowMillis = nowMillis
        )

        // Count maintained days: exactly 5 completed days, today is in-progress
        assertEquals(5, grid.totalActiveDays) // Sep 17, 18, 19, 20, 21 = 5 days
        assertEquals(0, grid.totalResetDays)

        // Yesterday (Sep 21, day 5) is MAINTAINED
        val yesterdayInfo = CommitGraphCalculator.calculateDayStatus(today.minusDays(1), today, nowMillis, emptyList(), active)
        assertEquals(DayCommitStatus.MAINTAINED, yesterdayInfo.status)
        assertEquals(5, yesterdayInfo.streakDayNumber)
        assertEquals(1, yesterdayInfo.intensityLevel) // 1..6 -> 1

        // Today (Sep 22, day 6 in progress) is IN_PROGRESS, not MAINTAINED
        val todayInfo = CommitGraphCalculator.calculateDayStatus(today, today, nowMillis, emptyList(), active)
        assertEquals(DayCommitStatus.IN_PROGRESS, todayInfo.status)
        assertEquals(6, todayInfo.streakDayNumber)
        assertEquals(0, todayInfo.intensityLevel)

        // When 24 hours complete for day 6 (6 full days completed)
        val completed6DaysMillis = startedAt + (6 * 24 * 3600 * 1000L)
        val todayCompletedInfo = CommitGraphCalculator.calculateDayStatus(today, today, completed6DaysMillis, emptyList(), active)
        assertEquals(DayCommitStatus.MAINTAINED, todayCompletedInfo.status)
        assertEquals(6, todayCompletedInfo.streakDayNumber)
    }

    @Test
    fun `day zero is marked as IN_PROGRESS until 24 hours complete`() {
        val startEpoch = today.toEpochDay()
        val startedAt = 10_000_000L
        val active = StreakEntity(
            id = 1,
            habitName = "Meditation",
            startDate = startEpoch,
            startedAt = startedAt
        )

        // 5 hours after start
        val fiveHoursIn = startedAt + (5 * 3600 * 1000L)
        val infoBefore24h = CommitGraphCalculator.calculateDayStatus(today, today, fiveHoursIn, emptyList(), active)
        assertEquals(DayCommitStatus.IN_PROGRESS, infoBefore24h.status)
        assertEquals(1, infoBefore24h.streakDayNumber)

        // 24 hours after start
        val exactly24h = startedAt + (24 * 3600 * 1000L)
        val infoAt24h = CommitGraphCalculator.calculateDayStatus(today, today, exactly24h, emptyList(), active)
        assertEquals(DayCommitStatus.MAINTAINED, infoAt24h.status)
        assertEquals(1, infoAt24h.streakDayNumber)
    }

    @Test
    fun `reset days are marked as RESET and grey with reason`() {
        // Streak started 10 days ago, ended 3 days ago with reset
        val streak1Start = today.minusDays(10).toEpochDay()
        val resetDate = today.minusDays(3)
        val resetEpoch = resetDate.toEpochDay()
        val streak1StartedAt = 1000L
        val resetMillis = streak1StartedAt + (7 * 24 * 3600 * 1000L) // 7 full 24h days completed

        val endedStreak = StreakEntity(
            id = 1,
            habitName = "Reading",
            startDate = streak1Start,
            startedAt = streak1StartedAt,
            endDate = resetEpoch,
            endedAt = resetMillis,
            lengthDays = 7,
            reason = "Late flight"
        )

        // New streak started on reset day, now 2 full 24h days completed
        val nowMillis = resetMillis + (2 * 24 * 3600 * 1000L) + (1 * 3600 * 1000L)
        val activeStreak = StreakEntity(
            id = 2,
            habitName = "Reading",
            startDate = resetEpoch,
            startedAt = resetMillis
        )

        val grid = CommitGraphCalculator.calculateGrid(
            history = listOf(endedStreak),
            activeStreak = activeStreak,
            today = today,
            firstDayOfWeek = FirstDayOfWeek.MONDAY,
            numWeeks = 4,
            nowMillis = nowMillis
        )

        assertEquals(1, grid.totalResetDays)

        // Verify the reset date specifically
        val resetDayInfo = CommitGraphCalculator.calculateDayStatus(resetDate, today, nowMillis, listOf(endedStreak), activeStreak)
        assertEquals(DayCommitStatus.RESET, resetDayInfo.status)
        assertEquals("Late flight", resetDayInfo.resetReason)
        assertEquals(0, resetDayInfo.intensityLevel)

        // Day before reset should be MAINTAINED
        val dayBeforeReset = resetDate.minusDays(1)
        val dayBeforeInfo = CommitGraphCalculator.calculateDayStatus(dayBeforeReset, today, nowMillis, listOf(endedStreak), activeStreak)
        assertEquals(DayCommitStatus.MAINTAINED, dayBeforeInfo.status)

        // Day after reset should be MAINTAINED
        val dayAfterReset = resetDate.plusDays(1)
        val dayAfterInfo = CommitGraphCalculator.calculateDayStatus(dayAfterReset, today, nowMillis, listOf(endedStreak), activeStreak)
        assertEquals(DayCommitStatus.MAINTAINED, dayAfterInfo.status)
    }

    @Test
    fun `calculateMonthHeaders produces non-colliding headers`() {
        val grid = CommitGraphCalculator.calculateGrid(
            history = emptyList(),
            activeStreak = null,
            today = today,
            firstDayOfWeek = FirstDayOfWeek.MONDAY,
            numWeeks = 24
        )

        val headers = CommitGraphCalculator.calculateMonthHeaders(grid.columns)
        assertTrue(headers.isNotEmpty())
        for (i in 1 until headers.size) {
            assertTrue(headers[i].columnIndex - headers[i - 1].columnIndex >= 3)
        }
    }
}

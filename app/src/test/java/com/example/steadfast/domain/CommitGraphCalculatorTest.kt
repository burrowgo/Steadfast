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
    fun `maintained days are marked correctly with intensity`() {
        // Active streak started 5 days ago (Sep 17)
        val startEpoch = today.minusDays(5).toEpochDay()
        val active = StreakEntity(
            id = 1,
            habitName = "Fitness",
            startDate = startEpoch,
            startedAt = 1000L
        )

        val grid = CommitGraphCalculator.calculateGrid(
            history = emptyList(),
            activeStreak = active,
            today = today,
            firstDayOfWeek = FirstDayOfWeek.MONDAY,
            numWeeks = 4
        )

        // Count maintained days
        assertEquals(6, grid.totalActiveDays) // Sep 17, 18, 19, 20, 21, 22 = 6 days
        assertEquals(0, grid.totalResetDays)

        val todayInfo = CommitGraphCalculator.calculateDayStatus(today, today, emptyList(), active)
        assertEquals(DayCommitStatus.MAINTAINED, todayInfo.status)
        assertEquals(6, todayInfo.streakDayNumber)
        assertEquals(1, todayInfo.intensityLevel) // 1..6 -> 1
    }

    @Test
    fun `reset days are marked as RESET and grey with reason`() {
        // Streak started 10 days ago, ended 3 days ago with reset
        val streak1Start = today.minusDays(10).toEpochDay()
        val resetDate = today.minusDays(3)
        val resetEpoch = resetDate.toEpochDay()

        val endedStreak = StreakEntity(
            id = 1,
            habitName = "Reading",
            startDate = streak1Start,
            startedAt = 1000L,
            endDate = resetEpoch,
            endedAt = 2000L,
            lengthDays = 7,
            reason = "Late flight"
        )

        // New streak started on reset day
        val activeStreak = StreakEntity(
            id = 2,
            habitName = "Reading",
            startDate = resetEpoch,
            startedAt = 2000L
        )

        val grid = CommitGraphCalculator.calculateGrid(
            history = listOf(endedStreak),
            activeStreak = activeStreak,
            today = today,
            firstDayOfWeek = FirstDayOfWeek.MONDAY,
            numWeeks = 4
        )

        assertEquals(1, grid.totalResetDays)

        // Verify the reset date specifically
        val resetDayInfo = CommitGraphCalculator.calculateDayStatus(resetDate, today, listOf(endedStreak), activeStreak)
        assertEquals(DayCommitStatus.RESET, resetDayInfo.status)
        assertEquals("Late flight", resetDayInfo.resetReason)
        assertEquals(0, resetDayInfo.intensityLevel)

        // Day before reset should be MAINTAINED
        val dayBeforeReset = resetDate.minusDays(1)
        val dayBeforeInfo = CommitGraphCalculator.calculateDayStatus(dayBeforeReset, today, listOf(endedStreak), activeStreak)
        assertEquals(DayCommitStatus.MAINTAINED, dayBeforeInfo.status)

        // Day after reset should be MAINTAINED
        val dayAfterReset = resetDate.plusDays(1)
        val dayAfterInfo = CommitGraphCalculator.calculateDayStatus(dayAfterReset, today, listOf(endedStreak), activeStreak)
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

package com.example.steadfast.domain

import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.FirstDayOfWeek
import java.time.DayOfWeek
import java.time.LocalDate

enum class DayCommitStatus {
    FUTURE,
    NOT_STARTED,
    IN_PROGRESS,
    INACTIVE,
    RESET,
    MAINTAINED
}

data class DayCommitInfo(
    val date: LocalDate,
    val status: DayCommitStatus,
    val streakDayNumber: Int? = null,
    val streakTotalDays: Int? = null,
    val resetReason: String? = null,
    val intensityLevel: Int = 0
)

data class CommitGraphColumn(
    val weekStartDate: LocalDate,
    val days: List<DayCommitInfo>
)

data class CommitGraphData(
    val columns: List<CommitGraphColumn>,
    val totalActiveDays: Int,
    val totalResetDays: Int,
    val firstDayOfWeek: FirstDayOfWeek
)

data class MonthHeader(
    val columnIndex: Int,
    val monthName: String
)

object CommitGraphCalculator {

    fun calculateDayStatus(
        date: LocalDate,
        today: LocalDate,
        history: List<StreakEntity>,
        activeStreak: StreakEntity?
    ): DayCommitInfo = calculateDayStatus(date, today, System.currentTimeMillis(), history, activeStreak)

    fun calculateDayStatus(
        date: LocalDate,
        today: LocalDate,
        nowMillis: Long,
        history: List<StreakEntity>,
        activeStreak: StreakEntity?
    ): DayCommitInfo {
        if (date.isAfter(today)) {
            return DayCommitInfo(date = date, status = DayCommitStatus.FUTURE)
        }

        val dateEpoch = date.toEpochDay()

        // 1. Check if a reset occurred on this date
        val resetStreak = history.firstOrNull { it.endDate == dateEpoch }
        if (resetStreak != null) {
            return DayCommitInfo(
                date = date,
                status = DayCommitStatus.RESET,
                resetReason = resetStreak.reason,
                intensityLevel = 0
            )
        }

        // 2. Check if this date is part of the active streak
        if (activeStreak != null && dateEpoch >= activeStreak.startDate && dateEpoch <= today.toEpochDay()) {
            val completedDays = StreakCalculator.calculateActiveStreakDays(activeStreak, nowMillis)
            val dayOffset = (dateEpoch - activeStreak.startDate).toInt()

            return if (dayOffset < completedDays) {
                // Completed a full 24-hour cycle
                val dayNumber = dayOffset + 1
                val intensity = getIntensityLevel(dayNumber)
                DayCommitInfo(
                    date = date,
                    status = DayCommitStatus.MAINTAINED,
                    streakDayNumber = dayNumber,
                    streakTotalDays = completedDays,
                    intensityLevel = intensity
                )
            } else {
                // In progress - 24 hours have not yet completed for this day
                val inProgressDayNumber = dayOffset + 1
                DayCommitInfo(
                    date = date,
                    status = DayCommitStatus.IN_PROGRESS,
                    streakDayNumber = inProgressDayNumber,
                    streakTotalDays = completedDays,
                    intensityLevel = 0
                )
            }
        }

        // 3. Check if this date was part of an ended streak (before its reset date)
        val matchingEndedStreak = history.firstOrNull { streak ->
            val endEpoch = streak.endDate ?: streak.startDate
            dateEpoch >= streak.startDate && dateEpoch < endEpoch
        }
        if (matchingEndedStreak != null) {
            val completedDays = StreakCalculator.calculateEndedStreakDays(matchingEndedStreak)
            val dayOffset = (dateEpoch - matchingEndedStreak.startDate).toInt()
            return if (dayOffset < completedDays) {
                val dayNumber = dayOffset + 1
                val intensity = getIntensityLevel(dayNumber)
                DayCommitInfo(
                    date = date,
                    status = DayCommitStatus.MAINTAINED,
                    streakDayNumber = dayNumber,
                    streakTotalDays = matchingEndedStreak.lengthDays ?: completedDays,
                    intensityLevel = intensity
                )
            } else {
                DayCommitInfo(date = date, status = DayCommitStatus.INACTIVE, intensityLevel = 0)
            }
        }

        // 4. If neither, check if habit had started before this date
        val allStreaks = history + listOfNotNull(activeStreak)
        val earliestStartDate = allStreaks.minOfOrNull { it.startDate }

        return if (earliestStartDate != null && dateEpoch >= earliestStartDate) {
            DayCommitInfo(date = date, status = DayCommitStatus.INACTIVE, intensityLevel = 0)
        } else {
            DayCommitInfo(date = date, status = DayCommitStatus.NOT_STARTED, intensityLevel = 0)
        }
    }

    private fun getIntensityLevel(streakDay: Int): Int = when {
        streakDay <= 0 -> 0
        streakDay in 1..6 -> 1
        streakDay in 7..20 -> 2
        streakDay in 21..49 -> 3
        else -> 4
    }

    fun calculateGrid(
        history: List<StreakEntity>,
        activeStreak: StreakEntity?,
        today: LocalDate,
        firstDayOfWeek: FirstDayOfWeek,
        numWeeks: Int = 24,
        nowMillis: Long = System.currentTimeMillis()
    ): CommitGraphData {
        val todayDayIndex = when (firstDayOfWeek) {
            FirstDayOfWeek.MONDAY -> today.dayOfWeek.value - 1
            FirstDayOfWeek.SUNDAY -> if (today.dayOfWeek == DayOfWeek.SUNDAY) 0 else today.dayOfWeek.value
        }

        val currentWeekStart = today.minusDays(todayDayIndex.toLong())
        val gridStartDate = currentWeekStart.minusWeeks((numWeeks - 1).toLong())

        val columns = (0 until numWeeks).map { colIndex ->
            val weekStart = gridStartDate.plusWeeks(colIndex.toLong())
            val days = (0..6).map { dayOffset ->
                val cellDate = weekStart.plusDays(dayOffset.toLong())
                calculateDayStatus(cellDate, today, nowMillis, history, activeStreak)
            }
            CommitGraphColumn(weekStartDate = weekStart, days = days)
        }

        val totalActiveDays = columns.flatMap { it.days }.count { it.status == DayCommitStatus.MAINTAINED }
        val totalResetDays = columns.flatMap { it.days }.count { it.status == DayCommitStatus.RESET }

        return CommitGraphData(
            columns = columns,
            totalActiveDays = totalActiveDays,
            totalResetDays = totalResetDays,
            firstDayOfWeek = firstDayOfWeek
        )
    }

    fun calculateMonthHeaders(columns: List<CommitGraphColumn>): List<MonthHeader> {
        val headers = mutableListOf<MonthHeader>()
        var lastMonth = -1
        var lastCol = -10
        columns.forEachIndexed { index, col ->
            val month = col.weekStartDate.monthValue
            if (month != lastMonth && (index - lastCol) >= 3) {
                val monthName = col.weekStartDate.month.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    java.util.Locale.getDefault()
                )
                headers.add(MonthHeader(index, monthName))
                lastMonth = month
                lastCol = index
            }
        }
        return headers
    }
}

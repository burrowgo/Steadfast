package com.example.steadfast.domain

import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.FirstDayOfWeek
import java.time.DayOfWeek
import java.time.LocalDate

enum class DayCommitStatus {
    FUTURE,
    NOT_STARTED,
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
            val dayNumber = (dateEpoch - activeStreak.startDate + 1).toInt()
            val totalDaysSoFar = (today.toEpochDay() - activeStreak.startDate + 1).toInt()
            val intensity = getIntensityLevel(dayNumber)
            return DayCommitInfo(
                date = date,
                status = DayCommitStatus.MAINTAINED,
                streakDayNumber = dayNumber,
                streakTotalDays = totalDaysSoFar,
                intensityLevel = intensity
            )
        }

        // 3. Check if this date was part of an ended streak (before its reset date)
        val matchingEndedStreak = history.firstOrNull { streak ->
            val endEpoch = streak.endDate ?: streak.startDate
            dateEpoch >= streak.startDate && dateEpoch < endEpoch
        }
        if (matchingEndedStreak != null) {
            val dayNumber = (dateEpoch - matchingEndedStreak.startDate + 1).toInt()
            val intensity = getIntensityLevel(dayNumber)
            return DayCommitInfo(
                date = date,
                status = DayCommitStatus.MAINTAINED,
                streakDayNumber = dayNumber,
                streakTotalDays = matchingEndedStreak.lengthDays,
                intensityLevel = intensity
            )
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
        numWeeks: Int = 24
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
                calculateDayStatus(cellDate, today, history, activeStreak)
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

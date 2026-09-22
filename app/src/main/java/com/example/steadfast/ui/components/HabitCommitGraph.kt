package com.example.steadfast.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.steadfast.R
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.FirstDayOfWeek
import com.example.steadfast.domain.CommitGraphCalculator
import com.example.steadfast.domain.CommitGraphData
import com.example.steadfast.domain.DayCommitInfo
import com.example.steadfast.domain.DayCommitStatus
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val GitHubDarkEmpty = Color(0xFF161B22)
private val GitHubDarkReset = Color(0xFF383E47)
private val GitHubDarkGreenLvl1 = Color(0xFF0E4429)
private val GitHubDarkGreenLvl2 = Color(0xFF006D32)
private val GitHubDarkGreenLvl3 = Color(0xFF26A641)
private val GitHubDarkGreenLvl4 = Color(0xFF39D353)

private val GitHubLightEmpty = Color(0xFFEBEDF0)
private val GitHubLightReset = Color(0xFFCFD5DC)
private val GitHubLightGreenLvl1 = Color(0xFF9BE9A8)
private val GitHubLightGreenLvl2 = Color(0xFF40C463)
private val GitHubLightGreenLvl3 = Color(0xFF30A14E)
private val GitHubLightGreenLvl4 = Color(0xFF216E39)

@Composable
fun HabitCommitGraph(
    history: List<StreakEntity>,
    activeStreak: StreakEntity?,
    firstDayOfWeek: FirstDayOfWeek,
    modifier: Modifier = Modifier,
    onFirstDayOfWeekChange: ((FirstDayOfWeek) -> Unit)? = null,
    clock: Clock = Clock.systemDefaultZone()
) {
    val today = remember(clock) { LocalDate.now(clock) }
    val nowMillis = remember(clock, activeStreak) { clock.millis() }
    val graphData = remember(history, activeStreak, today, nowMillis, firstDayOfWeek) {
        CommitGraphCalculator.calculateGrid(
            history = history,
            activeStreak = activeStreak,
            today = today,
            firstDayOfWeek = firstDayOfWeek,
            numWeeks = 24,
            nowMillis = nowMillis
        )
    }

    val monthHeaders = remember(graphData) {
        CommitGraphCalculator.calculateMonthHeaders(graphData.columns)
    }

    var selectedDay by remember { mutableStateOf<DayCommitInfo?>(null) }
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    // Auto-scroll to the latest week (today) on launch
    LaunchedEffect(graphData.columns.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Title & Total Active Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.commit_graph_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.commit_graph_summary, graphData.totalActiveDays),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Heatmap Area
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left Weekday Labels Column
                WeekdayLabelsColumn(firstDayOfWeek = firstDayOfWeek)

                Spacer(modifier = Modifier.width(6.dp))

                // Scrollable Columns (Month Headers + 7-row Grid)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                ) {
                    Column {
                        // Month Headers Row
                        MonthHeadersRow(
                            numColumns = graphData.columns.size,
                            monthHeaders = monthHeaders
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Heatmap Grid: Columns of 7 days
                        Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                            graphData.columns.forEach { column ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                    column.days.forEach { dayInfo ->
                                        CommitCell(
                                            dayInfo = dayInfo,
                                            isSelected = selectedDay?.date == dayInfo.date,
                                            isDark = isDark,
                                            onClick = {
                                                if (dayInfo.status != DayCommitStatus.FUTURE) {
                                                    selectedDay = if (selectedDay?.date == dayInfo.date) null else dayInfo
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Day Details or Tap Hint
            CommitGraphDetailBox(
                selectedDay = selectedDay,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Legend
            CommitGraphLegend(isDark = isDark)
        }
    }
}

@Composable
private fun WeekdayLabelsColumn(firstDayOfWeek: FirstDayOfWeek) {
    Column {
        Spacer(modifier = Modifier.height(17.dp))
        val labels = when (firstDayOfWeek) {
            FirstDayOfWeek.MONDAY -> listOf("Mon", "", "Wed", "", "Fri", "", "")
            FirstDayOfWeek.SUNDAY -> listOf("Sun", "", "Tue", "", "Thu", "", "")
        }
        labels.forEach { label ->
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 11.5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.5.dp))
        }
    }
}

@Composable
private fun MonthHeadersRow(
    numColumns: Int,
    monthHeaders: List<com.example.steadfast.domain.MonthHeader>
) {
    Box(
        modifier = Modifier
            .height(13.dp)
            .width((numColumns * 14).dp)
    ) {
        monthHeaders.forEach { header ->
            val offsetDp = (header.columnIndex * 14).dp
            Text(
                text = header.monthName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = offsetDp)
            )
        }
    }
}

@Composable
private fun CommitCell(
    dayInfo: DayCommitInfo,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cellColor = when (dayInfo.status) {
        DayCommitStatus.FUTURE -> Color.Transparent
        DayCommitStatus.NOT_STARTED,
        DayCommitStatus.INACTIVE,
        DayCommitStatus.IN_PROGRESS -> if (isDark) GitHubDarkEmpty else GitHubLightEmpty
        DayCommitStatus.RESET -> if (isDark) GitHubDarkReset else GitHubLightReset
        DayCommitStatus.MAINTAINED -> when (dayInfo.intensityLevel) {
            1 -> if (isDark) GitHubDarkGreenLvl1 else GitHubLightGreenLvl1
            2 -> if (isDark) GitHubDarkGreenLvl2 else GitHubLightGreenLvl2
            3 -> if (isDark) GitHubDarkGreenLvl3 else GitHubLightGreenLvl3
            else -> if (isDark) GitHubDarkGreenLvl4 else GitHubLightGreenLvl4
        }
    }

    val borderModifier = when {
        isSelected -> Modifier.border(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(2.5.dp)
        )
        dayInfo.status == DayCommitStatus.RESET -> Modifier.border(
            width = 0.8.dp,
            color = if (isDark) Color(0xFF505660) else Color(0xFFA0A6B0),
            shape = RoundedCornerShape(2.5.dp)
        )
        dayInfo.status == DayCommitStatus.IN_PROGRESS -> Modifier.border(
            width = 0.8.dp,
            color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            shape = RoundedCornerShape(2.5.dp)
        )
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .size(11.5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(cellColor)
            .then(borderModifier)
            .clickable(
                enabled = dayInfo.status != DayCommitStatus.FUTURE,
                onClick = onClick
            )
    )
}

@Composable
private fun CommitGraphDetailBox(
    selectedDay: DayCommitInfo?,
    isDark: Boolean
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedDay != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatter.format(selectedDay.date),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val statusColor = when (selectedDay.status) {
                        DayCommitStatus.MAINTAINED -> if (isDark) GitHubDarkGreenLvl4 else GitHubLightGreenLvl3
                        DayCommitStatus.RESET -> MaterialTheme.colorScheme.error
                        DayCommitStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }

                    val statusText = when (selectedDay.status) {
                        DayCommitStatus.MAINTAINED -> stringResource(
                            R.string.commit_graph_maintained,
                            selectedDay.streakDayNumber ?: 1
                        )
                        DayCommitStatus.IN_PROGRESS -> stringResource(
                            R.string.commit_graph_in_progress,
                            selectedDay.streakDayNumber ?: 1
                        )
                        DayCommitStatus.RESET -> {
                            if (!selectedDay.resetReason.isNullOrBlank()) {
                                stringResource(R.string.commit_graph_reset_with_reason, selectedDay.resetReason)
                            } else {
                                stringResource(R.string.commit_graph_reset)
                            }
                        }
                        DayCommitStatus.NOT_STARTED,
                        DayCommitStatus.INACTIVE -> stringResource(R.string.commit_graph_inactive)
                        DayCommitStatus.FUTURE -> ""
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.commit_graph_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun CommitGraphLegend(isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) GitHubDarkReset else GitHubLightReset)
                    .border(
                        0.5.dp,
                        if (isDark) Color(0xFF505660) else Color(0xFFA0A6B0),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.commit_graph_legend_reset),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.commit_graph_less),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(4.dp))

            val greens = if (isDark) {
                listOf(GitHubDarkGreenLvl1, GitHubDarkGreenLvl2, GitHubDarkGreenLvl3, GitHubDarkGreenLvl4)
            } else {
                listOf(GitHubLightGreenLvl1, GitHubLightGreenLvl2, GitHubLightGreenLvl3, GitHubLightGreenLvl4)
            }

            greens.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = stringResource(R.string.commit_graph_more),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

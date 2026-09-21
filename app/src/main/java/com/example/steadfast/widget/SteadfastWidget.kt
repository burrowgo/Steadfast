package com.example.steadfast.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.steadfast.MainActivity
import com.example.steadfast.R
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.WidgetShape
import com.example.steadfast.data.prefs.dataStore
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

open class SteadfastWidget(
    private val forceCircle: Boolean = false
) : GlanceAppWidget() {

    companion object {
        val TINY_SIZE = DpSize(50.dp, 50.dp) // 1x1
        val SMALL_SIZE = DpSize(100.dp, 90.dp) // 2x2
        val WIDE_SIZE = DpSize(200.dp, 90.dp) // 4x2
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(TINY_SIZE, SMALL_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val active = database.streakDao().getActiveStreak()
        val settings = SettingsRepository(context.dataStore).settingsFlow.first()
        val isCircle = forceCircle || (settings.widgetShape == WidgetShape.CIRCLE)

        provideContent {
            GlanceTheme {
                WidgetRoot(activeStreak = active, isCircle = isCircle)
            }
        }
    }

    @Composable
    private fun WidgetRoot(activeStreak: StreakEntity?, isCircle: Boolean) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isTiny = size.width < 100.dp || size.height < 80.dp
        val isWide = size.width >= 190.dp

        val cornerRadius = if (isCircle) 500.dp else 24.dp
        val padding = if (isTiny) 6.dp else 12.dp

        val backgroundModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(cornerRadius)
            .padding(padding)
            .clickable(actionStartActivity<MainActivity>())

        if (activeStreak == null) {
            // Empty state
            Box(
                modifier = backgroundModifier.semantics {
                    contentDescription = context.getString(R.string.widget_tap_to_start)
                },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTiny) {
                        Text(
                            text = context.getString(R.string.start_habit_button),
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    } else {
                        Text(
                            text = context.getString(R.string.app_name),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = context.getString(R.string.widget_tap_to_start),
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            val today = LocalDate.now()
            val start = LocalDate.ofEpochDay(activeStreak.startDate)
            val days = StreakCalculator.streakDays(start, today)
            val elapsedHours = ((System.currentTimeMillis() - activeStreak.startedAt) / (1000 * 3600)).coerceAtLeast(0L)
            val rankProgress = RankLadder.getRankProgress(days)
            val rankName = context.getString(rankProgress.currentRank.nameRes)
            val talkBackDesc = "Steadfast: $days days, rank $rankName"

            Box(
                modifier = backgroundModifier.semantics { contentDescription = talkBackDesc }
            ) {
                when {
                    isTiny -> {
                        TinyWidgetContent(days = days, elapsedHours = elapsedHours)
                    }
                    isWide -> {
                        WideWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days,
                            elapsedHours = elapsedHours,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext
                        )
                    }
                    else -> {
                        SmallWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days,
                            elapsedHours = elapsedHours
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TinyWidgetContent(days: Int, elapsedHours: Long) {
        val context = LocalContext.current
        val isDayZero = days == 0
        val displayText = if (isDayZero) "${elapsedHours}h" else days.toString()
        val displayLabel = if (isDayZero) "DAY 0" else context.getString(R.string.days_label).uppercase()

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fontSize = when {
                displayText.length >= 4 -> 18.sp
                displayText.length >= 3 -> 22.sp
                else -> 28.sp
            }
            Text(
                text = displayText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = displayLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun SmallWidgetContent(
        habitName: String,
        days: Int,
        elapsedHours: Long
    ) {
        val context = LocalContext.current
        val isDayZero = days == 0
        val displayText = if (isDayZero) "${elapsedHours}h" else days.toString()
        val displayLabel = if (isDayZero) "Day 0 Streak" else context.getString(R.string.days_label)

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habitName,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = displayText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = if (displayText.length >= 4) 34.sp else 44.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = displayLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun WideWidgetContent(
        habitName: String,
        days: Int,
        elapsedHours: Long,
        rankName: String,
        nextRankName: String?,
        daysToNext: Int,
        progressToNext: Float
    ) {
        val context = LocalContext.current
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: Day counter
            Column(
                modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habitName,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
                val isDayZero = days == 0
                val displayText = if (isDayZero) "${elapsedHours}h" else days.toString()
                val displayLabel = if (isDayZero) "Day 0 Streak" else context.getString(R.string.days_label)

                Text(
                    text = displayText,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = if (displayText.length >= 4) 30.sp else 38.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = displayLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Right column: Rank & Next rank progress
            Column(
                modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rankName,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progressToNext,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.surfaceVariant
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                val subtitle = if (nextRankName != null) {
                    "$daysToNext d to $nextRankName"
                } else {
                    context.getString(R.string.highest_rank_reached)
                }
                Text(
                    text = subtitle,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

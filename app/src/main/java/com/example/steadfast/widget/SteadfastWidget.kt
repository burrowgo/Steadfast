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
import androidx.glance.text.TextStyle
import com.example.steadfast.MainActivity
import com.example.steadfast.R
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import java.time.LocalDate

class SteadfastWidget : GlanceAppWidget() {

    companion object {
        val SMALL_SIZE = DpSize(100.dp, 100.dp)
        val WIDE_SIZE = DpSize(220.dp, 100.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val active = database.streakDao().getActiveStreak()

        provideContent {
            GlanceTheme {
                WidgetRoot(activeStreak = active)
            }
        }
    }

    @Composable
    private fun WidgetRoot(activeStreak: com.example.steadfast.data.db.StreakEntity?) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isWide = size.width >= 200.dp

        val backgroundModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(24.dp)
            .padding(14.dp)
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
                    Text(
                        text = context.getString(R.string.app_name),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.widget_tap_to_start),
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        } else {
            val today = LocalDate.now()
            val start = LocalDate.ofEpochDay(activeStreak.startDate)
            val days = StreakCalculator.streakDays(start, today)
            val rankProgress = RankLadder.getRankProgress(days)
            val rankName = context.getString(rankProgress.currentRank.nameRes)
            val talkBackDesc = "Steadfast: $days days, rank $rankName"

            Box(
                modifier = backgroundModifier.semantics { contentDescription = talkBackDesc }
            ) {
                if (isWide) {
                    WideWidgetContent(
                        habitName = activeStreak.habitName,
                        days = days,
                        rankName = rankName,
                        nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                        daysToNext = rankProgress.daysToNextRank,
                        progressToNext = rankProgress.progressToNext
                    )
                } else {
                    SmallWidgetContent(
                        habitName = activeStreak.habitName,
                        days = days
                    )
                }
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(
        habitName: String,
        days: Int
    ) {
        val context = LocalContext.current
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
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = days.toString(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = if (days >= 1000) 36.sp else 46.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = context.getString(R.string.days_label),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun WideWidgetContent(
        habitName: String,
        days: Int,
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
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = days.toString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = context.getString(R.string.days_label),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
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

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
        val TINY_SIZE = DpSize(40.dp, 40.dp) // 1x1
        val WIDE_SHORT_SIZE = DpSize(180.dp, 40.dp) // 4x1, 3x1
        val SMALL_SIZE = DpSize(100.dp, 75.dp) // 2x2
        val WIDE_SIZE = DpSize(180.dp, 75.dp) // 4x2
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(TINY_SIZE, WIDE_SHORT_SIZE, SMALL_SIZE, WIDE_SIZE)
    )

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
        val isWideShort = size.width >= 170.dp && size.height < 75.dp
        val isWideTall = size.width >= 170.dp && size.height >= 75.dp
        val isSmall = size.width < 170.dp && size.height >= 75.dp
        val isTiny = !isWideShort && !isWideTall && !isSmall

        val cornerRadius = if (isCircle) 500.dp else 24.dp
        val padding = when {
            isTiny -> 4.dp
            isWideShort -> 8.dp
            isSmall -> 8.dp
            else -> 10.dp
        }

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
                when {
                    isTiny -> {
                        Text(
                            text = context.getString(R.string.start_habit_button),
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    isWideShort -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.app_name),
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = context.getString(R.string.widget_tap_to_start),
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.app_name),
                                maxLines = 1,
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
                                maxLines = 1,
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
                when {
                    isTiny -> {
                        TinyWidgetContent(days = days)
                    }
                    isWideShort -> {
                        WideShortWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext
                        )
                    }
                    isWideTall -> {
                        WideWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext
                        )
                    }
                    else -> {
                        SmallWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TinyWidgetContent(days: Int) {
        val context = LocalContext.current
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fontSize = when {
                days >= 1000 -> 16.sp
                days >= 100 -> 18.sp
                else -> 22.sp
            }
            Text(
                text = days.toString(),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = context.getString(R.string.days_label).uppercase(),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
            val fontSize = when {
                days >= 1000 -> 24.sp
                days >= 100 -> 28.sp
                else -> 32.sp
            }
            Text(
                text = days.toString(),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = context.getString(R.string.days_label),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    @Composable
    private fun WideShortWidgetContent(
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
            // Left column: Habit name & day counter side-by-side
            Column(
                modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habitName,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fontSize = when {
                        days >= 1000 -> 18.sp
                        else -> 22.sp
                    }
                    Text(
                        text = days.toString(),
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.days_label),
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Right column: Rank & Next rank progress
            Column(
                modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rankName,
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val subtitle = if (nextRankName != null) {
                        "$daysToNext d"
                    } else {
                        context.getString(R.string.highest_rank_reached)
                    }
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressToNext,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.surfaceVariant
                )
            }
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
                val fontSize = when {
                    days >= 1000 -> 26.sp
                    days >= 100 -> 28.sp
                    else -> 32.sp
                }
                Text(
                    text = days.toString(),
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = context.getString(R.string.days_label),
                    maxLines = 1,
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressToNext,
                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
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

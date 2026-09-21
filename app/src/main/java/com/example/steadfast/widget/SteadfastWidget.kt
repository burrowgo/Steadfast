package com.example.steadfast.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.unit.ColorProvider
import com.example.steadfast.MainActivity
import com.example.steadfast.R
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.WidgetBgTheme
import com.example.steadfast.data.prefs.WidgetFontColor
import com.example.steadfast.data.prefs.WidgetShape
import com.example.steadfast.data.prefs.dataStore
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

data class WidgetThemeColors(
    val primaryText: ColorProvider,
    val secondaryText: ColorProvider,
    val accentText: ColorProvider,
    val progressTrack: ColorProvider,
    val background: ColorProvider
)

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
                WidgetRoot(
                    activeStreak = active,
                    isCircle = isCircle,
                    opacity = settings.widgetBackgroundOpacity,
                    fontColor = settings.widgetFontColor,
                    bgTheme = settings.widgetBgTheme
                )
            }
        }
    }

    @Composable
    private fun resolveWidgetColors(
        opacity: Int,
        fontColor: WidgetFontColor,
        bgTheme: WidgetBgTheme
    ): WidgetThemeColors {
        val alpha = (opacity.coerceIn(0, 100) / 100f)

        val background: ColorProvider = if (alpha <= 0.01f) {
            ColorProvider(Color.Transparent)
        } else {
            when (bgTheme) {
                WidgetBgTheme.BLACK -> ColorProvider(Color.Black.copy(alpha = alpha))
                WidgetBgTheme.CHARCOAL -> ColorProvider(Color(0xFF1E1E1E).copy(alpha = alpha))
                WidgetBgTheme.WHITE -> ColorProvider(Color.White.copy(alpha = alpha))
                WidgetBgTheme.DEFAULT -> {
                    if (opacity >= 100) {
                        GlanceTheme.colors.surface
                    } else {
                        androidx.glance.color.ColorProvider(
                            day = Color(0xFFF3F4F6).copy(alpha = alpha),
                            night = Color(0xFF1E201E).copy(alpha = alpha)
                        )
                    }
                }
            }
        }

        val primaryText: ColorProvider
        val secondaryText: ColorProvider
        val accentText: ColorProvider
        val progressTrack: ColorProvider

        when (fontColor) {
            WidgetFontColor.WHITE -> {
                primaryText = ColorProvider(Color.White)
                secondaryText = ColorProvider(Color(0xFFD0D0D0))
                accentText = ColorProvider(Color(0xFFCDEDA3))
                progressTrack = ColorProvider(Color(0x40FFFFFF))
            }
            WidgetFontColor.BLACK -> {
                primaryText = ColorProvider(Color.Black)
                secondaryText = ColorProvider(Color(0xFF4A4A4A))
                accentText = ColorProvider(Color(0xFF2E441E))
                progressTrack = ColorProvider(Color(0x33000000))
            }
            WidgetFontColor.BRAND -> {
                primaryText = androidx.glance.color.ColorProvider(day = Color(0xFF2E441E), night = Color(0xFFCDEDA3))
                secondaryText = androidx.glance.color.ColorProvider(day = Color(0xFF556B2F), night = Color(0xFFAAB49F))
                accentText = androidx.glance.color.ColorProvider(day = Color(0xFF4C662B), night = Color(0xFFB1D18A))
                progressTrack = androidx.glance.color.ColorProvider(day = Color(0x334C662B), night = Color(0x33B1D18A))
            }
            WidgetFontColor.DEFAULT -> {
                primaryText = GlanceTheme.colors.onSurface
                secondaryText = GlanceTheme.colors.onSurfaceVariant
                accentText = GlanceTheme.colors.primary
                progressTrack = GlanceTheme.colors.surfaceVariant
            }
        }

        return WidgetThemeColors(
            primaryText = primaryText,
            secondaryText = secondaryText,
            accentText = accentText,
            progressTrack = progressTrack,
            background = background
        )
    }

    @Composable
    private fun WidgetRoot(
        activeStreak: StreakEntity?,
        isCircle: Boolean,
        opacity: Int,
        fontColor: WidgetFontColor,
        bgTheme: WidgetBgTheme
    ) {
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

        val colors = resolveWidgetColors(opacity = opacity, fontColor = fontColor, bgTheme = bgTheme)

        val backgroundModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.background)
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
                                color = colors.accentText,
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
                                    color = colors.primaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Text(
                                text = context.getString(R.string.widget_tap_to_start),
                                maxLines = 1,
                                style = TextStyle(
                                    color = colors.accentText,
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
                                    color = colors.primaryText,
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
                                    color = colors.accentText,
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
                        TinyWidgetContent(days = days, colors = colors)
                    }
                    isWideShort -> {
                        WideShortWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext,
                            colors = colors
                        )
                    }
                    isWideTall -> {
                        WideWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext,
                            colors = colors
                        )
                    }
                    else -> {
                        SmallWidgetContent(
                            habitName = activeStreak.habitName,
                            days = days,
                            colors = colors
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TinyWidgetContent(days: Int, colors: WidgetThemeColors) {
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
                    color = colors.primaryText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = context.getString(R.string.days_label).uppercase(),
                maxLines = 1,
                style = TextStyle(
                    color = colors.accentText,
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
        days: Int,
        colors: WidgetThemeColors
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
                    color = colors.secondaryText,
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
                    color = colors.primaryText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = context.getString(R.string.days_label),
                maxLines = 1,
                style = TextStyle(
                    color = colors.accentText,
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
        progressToNext: Float,
        colors: WidgetThemeColors
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
                        color = colors.secondaryText,
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
                            color = colors.primaryText,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.days_label),
                        maxLines = 1,
                        style = TextStyle(
                            color = colors.accentText,
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
                            color = colors.primaryText,
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
                            color = colors.secondaryText,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressToNext,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = colors.accentText,
                    backgroundColor = colors.progressTrack
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
        progressToNext: Float,
        colors: WidgetThemeColors
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
                        color = colors.secondaryText,
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
                        color = colors.primaryText,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = context.getString(R.string.days_label),
                    maxLines = 1,
                    style = TextStyle(
                        color = colors.accentText,
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
                        color = colors.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressToNext,
                    modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                    color = colors.accentText,
                    backgroundColor = colors.progressTrack
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
                        color = colors.secondaryText,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

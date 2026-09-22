package com.example.steadfast.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.currentState
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.steadfast.MainActivity
import com.example.steadfast.R
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.data.db.HabitEntity
import com.example.steadfast.data.db.StreakEntity
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.WidgetBgTheme
import com.example.steadfast.data.prefs.WidgetConfigurationRepository
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
        val KEY_HABIT_ID = longPreferencesKey("widget_habit_id")
        val KEY_HABIT_NAME = stringPreferencesKey("widget_habit_name")
        val KEY_HAS_ACTIVE_STREAK = booleanPreferencesKey("widget_has_active_streak")
        val KEY_STREAK_START_DATE = longPreferencesKey("widget_streak_start_date")
        val KEY_IS_CIRCLE = booleanPreferencesKey("widget_is_circle")
        val KEY_OPACITY = intPreferencesKey("widget_opacity")
        val KEY_FONT_COLOR = stringPreferencesKey("widget_font_color")
        val KEY_BG_THEME = stringPreferencesKey("widget_bg_theme")
        val KEY_SHOW_HABIT_NAME = booleanPreferencesKey("widget_show_habit_name")
        const val EXTRA_HABIT_ID = "com.example.steadfast.extra.HABIT_ID"
        val TINY_SIZE = DpSize(40.dp, 40.dp) // 1x1
        val SMALL_SIZE = DpSize(100.dp, 100.dp) // 2x2
        val WIDE_SHORT_SIZE = DpSize(220.dp, 40.dp) // 4x1, 3x1
        val WIDE_SIZE = DpSize(240.dp, 90.dp) // 4x2

        @SuppressLint("RestrictedApi")
        fun extractAppWidgetId(context: Context, id: GlanceId): Int {
            if (id is androidx.glance.appwidget.AppWidgetId) {
                return id.appWidgetId
            }
            return try {
                GlanceAppWidgetManager(context).getAppWidgetId(id)
            } catch (e: Throwable) {
                val match = Regex("""appWidgetId=(\d+)""").find(id.toString())
                match?.groupValues?.get(1)?.toIntOrNull() ?: -1
            }
        }

        /**
         * Resolve habit and streak from DB given a configured habitId.
         * Falls back to the first active habit if habitId is null or not found.
         */
        suspend fun resolveForHabitId(
            database: AppDatabase,
            habitId: Long?
        ): Pair<HabitEntity?, StreakEntity?> {
            return if (habitId != null && habitId > 0) {
                val h = database.habitDao().getHabitById(habitId)
                val s = database.streakDao().getActiveStreak(habitId)
                if (h != null) {
                    Pair(h, s)
                } else {
                    val fallbackH = database.habitDao().getActiveHabits().firstOrNull()
                    val fallbackS = fallbackH?.let { database.streakDao().getActiveStreak(it.id) }
                    Pair(fallbackH, fallbackS)
                }
            } else {
                val firstH = database.habitDao().getActiveHabits().firstOrNull()
                val s = firstH?.let { database.streakDao().getActiveStreak(it.id) }
                Pair(firstH, s)
            }
        }

        /**
         * Write all display data for a widget into its per-instance Glance DataStore.
         * This is the ONLY way widget data should be updated — provideContent reads reactively
         * from this state via currentState<Preferences>().
         */
        suspend fun writeWidgetState(
            context: Context,
            glanceId: GlanceId,
            habit: HabitEntity?,
            streak: StreakEntity?,
            isCircle: Boolean,
            opacity: Int,
            fontColor: WidgetFontColor,
            bgTheme: WidgetBgTheme,
            showHabitName: Boolean
        ) {
            val habitName = if (habit != null && habit.name.isNotBlank()) {
                habit.name
            } else {
                ""
            }
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[KEY_HABIT_ID] = habit?.id ?: -1L
                    this[KEY_HABIT_NAME] = habitName
                    this[KEY_HAS_ACTIVE_STREAK] = streak != null
                    if (streak != null) {
                        this[KEY_STREAK_START_DATE] = streak.startDate
                    } else {
                        remove(KEY_STREAK_START_DATE)
                    }
                    this[KEY_IS_CIRCLE] = isCircle
                    this[KEY_OPACITY] = opacity
                    this[KEY_FONT_COLOR] = fontColor.name
                    this[KEY_BG_THEME] = bgTheme.name
                    this[KEY_SHOW_HABIT_NAME] = showHabitName
                }
            }
        }
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(TINY_SIZE, SMALL_SIZE, WIDE_SHORT_SIZE, WIDE_SIZE)
    )

    override suspend fun onDelete(context: Context, id: GlanceId) {
        super.onDelete(context, id)
        val appWidgetId = extractAppWidgetId(context, id)
        if (appWidgetId > 0) {
            WidgetConfigurationRepository(context).removeWidget(appWidgetId)
        }
    }

    /**
     * Bootstrap the Glance state for a widget if it hasn't been populated yet
     * or if the configured habit has changed.
     */
    internal suspend fun ensureStatePopulated(
        context: Context,
        id: GlanceId,
        database: AppDatabase = AppDatabase.getInstance(context)
    ) {
        val prefs = try {
            getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        } catch (e: Throwable) {
            return
        }

        val appWidgetId = extractAppWidgetId(context, id)
        val widgetConfigRepo = WidgetConfigurationRepository(context)
        val configuredHabitId = if (appWidgetId > 0) {
            widgetConfigRepo.getHabitIdForWidget(appWidgetId)
        } else {
            null
        }
        val currentHabitIdInPrefs = prefs[KEY_HABIT_ID]?.takeIf { it > 0 }

        // If state is already populated and matches the configured habit, skip
        val isUpToDate = if (configuredHabitId != null) {
            currentHabitIdInPrefs == configuredHabitId && prefs[KEY_HABIT_NAME] != null
        } else {
            prefs[KEY_HABIT_NAME] != null
        }
        if (isUpToDate) return

        val targetHabitId = configuredHabitId ?: currentHabitIdInPrefs
        val (habit, streak) = resolveForHabitId(database, targetHabitId)
        val settings = SettingsRepository(context.dataStore).settingsFlow.first()

        writeWidgetState(
            context, id, habit, streak,
            isCircle = forceCircle || (settings.widgetShape == WidgetShape.CIRCLE),
            opacity = settings.widgetBackgroundOpacity,
            fontColor = settings.widgetFontColor,
            bgTheme = settings.widgetBgTheme,
            showHabitName = settings.widgetShowHabitName
        )
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ensureStatePopulated(context, id)

        provideContent {
            // Read ALL data reactively from per-widget Glance DataStore.
            // This ensures each widget instance displays its own habit's data,
            // even when provideGlance is not re-called on update().
            val prefs = currentState<Preferences>()
            val habitId = prefs[KEY_HABIT_ID] ?: -1L
            val habitName = prefs[KEY_HABIT_NAME] ?: ""
            val hasActiveStreak = prefs[KEY_HAS_ACTIVE_STREAK] ?: false
            val startDateEpoch = prefs[KEY_STREAK_START_DATE]
            val isCircle = forceCircle || (prefs[KEY_IS_CIRCLE] ?: false)
            val opacity = prefs[KEY_OPACITY] ?: 100
            val fontColor = prefs[KEY_FONT_COLOR]?.let {
                try { WidgetFontColor.valueOf(it) } catch (_: Exception) { null }
            } ?: WidgetFontColor.DEFAULT
            val bgTheme = prefs[KEY_BG_THEME]?.let {
                try { WidgetBgTheme.valueOf(it) } catch (_: Exception) { null }
            } ?: WidgetBgTheme.DEFAULT
            val showHabitName = prefs[KEY_SHOW_HABIT_NAME] ?: true

            val days = if (hasActiveStreak && startDateEpoch != null) {
                StreakCalculator.streakDays(LocalDate.ofEpochDay(startDateEpoch), LocalDate.now())
            } else {
                0
            }

            GlanceTheme {
                WidgetRoot(
                    habitId = habitId,
                    habitName = habitName,
                    days = days,
                    hasActiveStreak = hasActiveStreak,
                    isCircle = isCircle,
                    opacity = opacity,
                    fontColor = fontColor,
                    bgTheme = bgTheme,
                    showHabitName = showHabitName
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
        habitId: Long,
        habitName: String,
        days: Int,
        hasActiveStreak: Boolean,
        isCircle: Boolean,
        opacity: Int,
        fontColor: WidgetFontColor,
        bgTheme: WidgetBgTheme,
        showHabitName: Boolean = true
    ) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isTiny = size.width < 90.dp && size.height < 90.dp
        val isWideShort = size.width >= 180.dp && size.height < 80.dp
        val isWideTall = size.width >= 230.dp && size.height >= 80.dp && (size.width / size.height >= 1.35f)
        val isSmall = !isTiny && !isWideShort && !isWideTall

        val cornerRadius = if (isCircle) 500.dp else 24.dp
        val padding = when {
            isTiny -> 4.dp
            isWideShort -> 8.dp
            isSmall -> 8.dp
            else -> 10.dp
        }

        val colors = resolveWidgetColors(opacity = opacity, fontColor = fontColor, bgTheme = bgTheme)

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (habitId > 0) {
                putExtra(EXTRA_HABIT_ID, habitId)
            }
        }

        val backgroundModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.background)
            .cornerRadius(cornerRadius)
            .padding(padding)
            .clickable(actionStartActivity(clickIntent))

        if (!hasActiveStreak) {
            // Empty state
            val habitTitle = habitName.takeIf { it.isNotBlank() && showHabitName } ?: context.getString(R.string.app_name)
            val desc = if (habitName.isNotBlank()) {
                "$habitName: ${context.getString(R.string.widget_tap_to_start)}"
            } else {
                context.getString(R.string.widget_tap_to_start)
            }

            Box(
                modifier = backgroundModifier.semantics {
                    contentDescription = desc
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
                                text = habitTitle,
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
                                text = habitTitle,
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
                            habitName = habitName,
                            days = days,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext,
                            colors = colors,
                            showHabitName = showHabitName
                        )
                    }
                    isWideTall -> {
                        WideWidgetContent(
                            habitName = habitName,
                            days = days,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext,
                            colors = colors,
                            showHabitName = showHabitName
                        )
                    }
                    else -> {
                        SmallWidgetContent(
                            habitName = habitName,
                            days = days,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { context.getString(it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progressToNext = rankProgress.progressToNext,
                            colors = colors,
                            showHabitName = showHabitName
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
        rankName: String,
        nextRankName: String?,
        daysToNext: Int,
        progressToNext: Float,
        colors: WidgetThemeColors,
        showHabitName: Boolean = true
    ) {
        val context = LocalContext.current
        val size = LocalSize.current
        val showRankDetails = size.height >= 95.dp

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showHabitName && habitName.isNotBlank()) {
                Text(
                    text = habitName,
                    maxLines = 1,
                    style = TextStyle(
                        color = colors.secondaryText,
                        fontSize = if (showRankDetails) 12.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }

            if (showRankDetails) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fontSize = when {
                        days >= 1000 -> 26.sp
                        days >= 100 -> 30.sp
                        else -> 36.sp
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
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.days_label).uppercase(),
                        maxLines = 1,
                        style = TextStyle(
                            color = colors.accentText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = rankName,
                    maxLines = 1,
                    style = TextStyle(
                        color = colors.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressToNext,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp).padding(horizontal = 8.dp),
                    color = colors.accentText,
                    backgroundColor = colors.progressTrack
                )

                Spacer(modifier = GlanceModifier.height(2.dp))
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
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                )
            } else {
                val fontSize = when {
                    days >= 1000 -> 22.sp
                    days >= 100 -> 26.sp
                    else -> 30.sp
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
    }

    @Composable
    private fun WideShortWidgetContent(
        habitName: String,
        days: Int,
        rankName: String,
        nextRankName: String?,
        daysToNext: Int,
        progressToNext: Float,
        colors: WidgetThemeColors,
        showHabitName: Boolean = true
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
                if (showHabitName && habitName.isNotBlank()) {
                    Text(
                        text = habitName,
                        maxLines = 1,
                        style = TextStyle(
                            color = colors.secondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
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
        colors: WidgetThemeColors,
        showHabitName: Boolean = true
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
                if (showHabitName && habitName.isNotBlank()) {
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
                }
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

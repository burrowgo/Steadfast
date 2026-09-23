package com.example.steadfast.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steadfast.R
import com.example.steadfast.SteadfastApp
import com.example.steadfast.data.prefs.WidgetBgTheme
import com.example.steadfast.data.prefs.WidgetFontColor
import com.example.steadfast.data.prefs.WidgetShape
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.StreakCalculator
import com.example.steadfast.ui.theme.CardShape
import java.time.LocalDate

private enum class PreviewSize {
    STANDARD_2X2,
    WIDE_4X1,
    TINY_1X1
}

private enum class WallpaperPreview {
    DARK,
    LIGHT,
    PATTERN
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SteadfastApp
    val container = app.container
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            application = app,
            streakRepository = container.streakRepository,
            settingsRepository = container.settingsRepository,
            updateChecker = container.updateChecker
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var previewSize by remember { mutableStateOf(PreviewSize.STANDARD_2X2) }
    var wallpaper by remember { mutableStateOf(WallpaperPreview.DARK) }

    val activeDays = remember(uiState.activeStartedAt, uiState.activeStartDate) {
        val startDate = uiState.activeStartDate
        if (uiState.activeStartedAt > 0L) {
            StreakCalculator.streakDays(uiState.activeStartedAt, System.currentTimeMillis())
        } else if (startDate != null) {
            StreakCalculator.streakDays(startDate, LocalDate.now())
        } else {
            14
        }
    }
    val habitName = if (uiState.habitName.isNotBlank()) uiState.habitName else "Meditation"
    val rankProgress = remember(activeDays) { RankLadder.getRankProgress(activeDays) }
    val rankName = stringResource(id = rankProgress.currentRank.nameRes)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_customization_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.edit_reason_cancel)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Live Preview Card
            Text(
                text = stringResource(R.string.widget_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Size selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilterChip(
                            selected = previewSize == PreviewSize.STANDARD_2X2,
                            onClick = { previewSize = PreviewSize.STANDARD_2X2 },
                            label = { Text(stringResource(R.string.widget_preview_size_2x2)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = previewSize == PreviewSize.WIDE_4X1,
                            onClick = { previewSize = PreviewSize.WIDE_4X1 },
                            label = { Text(stringResource(R.string.widget_preview_size_4x1)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = previewSize == PreviewSize.TINY_1X1,
                            onClick = { previewSize = PreviewSize.TINY_1X1 },
                            label = { Text(stringResource(R.string.widget_preview_size_1x1)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated Wallpaper Container
                    val wallpaperBrush = when (wallpaper) {
                        WallpaperPreview.DARK -> Brush.verticalGradient(
                            listOf(Color(0xFF101820), Color(0xFF263238), Color(0xFF101820))
                        )
                        WallpaperPreview.LIGHT -> Brush.verticalGradient(
                            listOf(Color(0xFFE8EEF5), Color(0xFFC7D3E0), Color(0xFFE8EEF5))
                        )
                        WallpaperPreview.PATTERN -> Brush.linearGradient(
                            listOf(Color(0xFF2D3748), Color(0xFF1A202C))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(wallpaperBrush)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WidgetMockView(
                            size = previewSize,
                            shape = uiState.widgetShape,
                            opacity = uiState.widgetBackgroundOpacity,
                            fontColor = uiState.widgetFontColor,
                            bgTheme = uiState.widgetBgTheme,
                            habitName = habitName,
                            showHabitName = uiState.widgetShowHabitName,
                            days = activeDays,
                            rankName = rankName,
                            nextRankName = rankProgress.nextRank?.let { stringResource(id = it.nameRes) },
                            daysToNext = rankProgress.daysToNextRank,
                            progress = rankProgress.progressToNext
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wallpaper selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wallpaper:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = wallpaper == WallpaperPreview.DARK,
                            onClick = { wallpaper = WallpaperPreview.DARK },
                            label = { Text(stringResource(R.string.widget_wallpaper_dark)) }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = wallpaper == WallpaperPreview.LIGHT,
                            onClick = { wallpaper = WallpaperPreview.LIGHT },
                            label = { Text(stringResource(R.string.widget_wallpaper_light)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Background Opacity Section
            Card(shape = CardShape, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.widget_opacity_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.widget_opacity_percent, uiState.widgetBackgroundOpacity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = uiState.widgetBackgroundOpacity.toFloat(),
                        onValueChange = { viewModel.setWidgetBackgroundOpacity(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 19
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(0, 25, 50, 75, 100)
                        presets.forEach { preset ->
                            val label = when (preset) {
                                0 -> stringResource(R.string.widget_opacity_glass)
                                100 -> stringResource(R.string.widget_opacity_solid)
                                else -> "$preset%"
                            }
                            FilterChip(
                                selected = uiState.widgetBackgroundOpacity == preset,
                                onClick = { viewModel.setWidgetBackgroundOpacity(preset) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Font / Text Color Section
            Card(shape = CardShape, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.widget_font_color_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WidgetFontColor.entries.forEach { option ->
                            val label = when (option) {
                                WidgetFontColor.DEFAULT -> stringResource(R.string.widget_font_color_default)
                                WidgetFontColor.WHITE -> stringResource(R.string.widget_font_color_white)
                                WidgetFontColor.BLACK -> stringResource(R.string.widget_font_color_black)
                                WidgetFontColor.BRAND -> stringResource(R.string.widget_font_color_brand)
                            }
                            FilterChip(
                                selected = uiState.widgetFontColor == option,
                                onClick = { viewModel.setWidgetFontColor(option) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Background Color Theme Section
            Card(shape = CardShape, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.widget_bg_theme_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WidgetBgTheme.entries.forEach { option ->
                            val label = when (option) {
                                WidgetBgTheme.DEFAULT -> stringResource(R.string.widget_bg_theme_default)
                                WidgetBgTheme.BLACK -> stringResource(R.string.widget_bg_theme_black)
                                WidgetBgTheme.CHARCOAL -> stringResource(R.string.widget_bg_theme_charcoal)
                                WidgetBgTheme.WHITE -> stringResource(R.string.widget_bg_theme_white)
                            }
                            FilterChip(
                                selected = uiState.widgetBgTheme == option,
                                onClick = { viewModel.setWidgetBgTheme(option) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Widget Shape Section
            Card(shape = CardShape, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_widget_shape),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.widgetShape == WidgetShape.ROUNDED,
                            onClick = { viewModel.setWidgetShape(WidgetShape.ROUNDED) },
                            label = { Text(stringResource(R.string.widget_shape_rounded)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.widgetShape == WidgetShape.CIRCLE,
                            onClick = { viewModel.setWidgetShape(WidgetShape.CIRCLE) },
                            label = { Text(stringResource(R.string.widget_shape_circle)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Habit Title Visibility (Privacy) Section
            Card(shape = CardShape, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(R.string.widget_show_habit_name_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.widget_show_habit_name_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.widgetShowHabitName,
                        onCheckedChange = { viewModel.setWidgetShowHabitName(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WidgetMockView(
    size: PreviewSize,
    shape: WidgetShape,
    opacity: Int,
    fontColor: WidgetFontColor,
    bgTheme: WidgetBgTheme,
    habitName: String,
    showHabitName: Boolean = true,
    days: Int,
    rankName: String,
    nextRankName: String?,
    daysToNext: Int,
    progress: Float
) {
    val alpha = opacity.coerceIn(0, 100) / 100f

    val bgColor = if (alpha <= 0.01f) {
        Color.Transparent
    } else {
        when (bgTheme) {
            WidgetBgTheme.BLACK -> Color.Black.copy(alpha = alpha)
            WidgetBgTheme.CHARCOAL -> Color(0xFF1E1E1E).copy(alpha = alpha)
            WidgetBgTheme.WHITE -> Color.White.copy(alpha = alpha)
            WidgetBgTheme.DEFAULT -> MaterialTheme.colorScheme.surface.copy(alpha = alpha)
        }
    }

    val primaryTextColor: Color
    val secondaryTextColor: Color
    val accentTextColor: Color
    val trackColor: Color

    when (fontColor) {
        WidgetFontColor.WHITE -> {
            primaryTextColor = Color.White
            secondaryTextColor = Color(0xFFD0D0D0)
            accentTextColor = Color(0xFFCDEDA3)
            trackColor = Color(0x40FFFFFF)
        }
        WidgetFontColor.BLACK -> {
            primaryTextColor = Color.Black
            secondaryTextColor = Color(0xFF4A4A4A)
            accentTextColor = Color(0xFF2E441E)
            trackColor = Color(0x33000000)
        }
        WidgetFontColor.BRAND -> {
            primaryTextColor = Color(0xFFCDEDA3)
            secondaryTextColor = Color(0xFFAAB49F)
            accentTextColor = Color(0xFFB1D18A)
            trackColor = Color(0x334C662B)
        }
        WidgetFontColor.DEFAULT -> {
            primaryTextColor = MaterialTheme.colorScheme.onSurface
            secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            accentTextColor = MaterialTheme.colorScheme.primary
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        }
    }

    val cornerRadius = if (shape == WidgetShape.CIRCLE || size == PreviewSize.TINY_1X1) 500.dp else 24.dp
    val containerModifier = Modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(bgColor)
        .border(
            width = if (alpha <= 0.1f) 1.dp else 0.dp,
            color = Color(0x22FFFFFF),
            shape = RoundedCornerShape(cornerRadius)
        )

    when (size) {
        PreviewSize.STANDARD_2X2 -> {
            Box(
                modifier = containerModifier
                    .size(width = 110.dp, height = 110.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showHabitName && habitName.isNotBlank()) {
                        Text(
                            text = habitName,
                            maxLines = 1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = days.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.days_label),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        PreviewSize.WIDE_4X1 -> {
            Box(
                modifier = containerModifier
                    .fillMaxWidth(0.95f)
                    .height(56.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (showHabitName && habitName.isNotBlank()) {
                            Text(
                                text = habitName,
                                maxLines = 1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryTextColor
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = days.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.days_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rankName,
                                maxLines = 1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                modifier = Modifier.weight(1f)
                            )
                            val subtitle = if (nextRankName != null) "$daysToNext d" else stringResource(R.string.highest_rank_reached)
                            Text(
                                text = subtitle,
                                maxLines = 1,
                                fontSize = 10.sp,
                                color = secondaryTextColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = accentTextColor,
                            trackColor = trackColor
                        )
                    }
                }
            }
        }
        PreviewSize.TINY_1X1 -> {
            Box(
                modifier = containerModifier
                    .size(60.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = days.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.days_label).uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

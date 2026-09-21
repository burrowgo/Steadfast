package com.example.steadfast.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steadfast.R
import com.example.steadfast.SteadfastApp
import com.example.steadfast.domain.model.Habit
import com.example.steadfast.domain.model.HabitVisuals
import com.example.steadfast.ui.components.RankBadge
import com.example.steadfast.ui.theme.CardShape
import com.example.steadfast.ui.theme.LocalRankColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as SteadfastApp
    val container = context.container
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.provideFactory(
            streakRepository = container.streakRepository,
            habitRepository = container.habitRepository,
            clock = container.clock
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Habit Filter Chips (shown when user has more than 1 habit)
            if (uiState.habits.size > 1) {
                HabitFilterRow(
                    habits = uiState.habits,
                    selectedHabitId = uiState.selectedHabitId,
                    onSelectHabit = { viewModel.selectHabit(it) },
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Top Summary Card
            SummaryStatsCard(
                longest = uiState.stats.longestStreakDays,
                totalAttempts = uiState.stats.totalAttempts,
                currentAttempt = uiState.stats.currentAttemptNumber,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val showHabitTag = uiState.selectedHabitId == null && uiState.habits.size > 1

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.items, key = { it.entity.id }) { item ->
                        HistoryItemCard(
                            item = item,
                            showHabitTag = showHabitTag,
                            onClick = { viewModel.openEditReason(item.entity) }
                        )
                    }
                }
            }
        }

        // Edit Reason Dialog
        uiState.editingStreak?.let { entity ->
            EditReasonDialog(
                initialReason = entity.reason,
                onSave = { newReason -> viewModel.saveReason(entity.id, newReason) },
                onDismiss = { viewModel.dismissEditReason() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitFilterRow(
    habits: List<Habit>,
    selectedHabitId: Long?,
    onSelectHabit: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedHabitId == null,
                onClick = { onSelectHabit(null) },
                label = { Text(stringResource(R.string.habit_all_habits)) }
            )
        }
        items(habits, key = { it.id }) { habit ->
            val iconRes = HabitVisuals.getIconResource(habit.icon)
            val iconTint = androidx.compose.ui.graphics.Color(habit.color)
            FilterChip(
                selected = selectedHabitId == habit.id,
                onClick = { onSelectHabit(habit.id) },
                label = { Text(habit.name) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun SummaryStatsCard(
    longest: Int,
    totalAttempts: Int,
    currentAttempt: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(
                label = stringResource(R.string.history_longest_streak),
                value = "$longest d"
            )
            StatDivider()
            StatColumn(
                label = stringResource(R.string.history_total_attempts),
                value = totalAttempts.toString()
            )
            StatDivider()
            StatColumn(
                label = stringResource(R.string.history_current_attempt),
                value = if (currentAttempt > 0) "#$currentAttempt" else "—"
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 36.dp)
            .padding(vertical = 4.dp)
            .padding(horizontal = 0.dp)
    )
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    showHabitTag: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (showHabitTag && item.habitName.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        item.habitIcon?.let { iconName ->
                            val iconColor = item.habitColor?.let { androidx.compose.ui.graphics.Color(it) } ?: MaterialTheme.colorScheme.primary
                            Icon(
                                painter = painterResource(id = HabitVisuals.getIconResource(iconName)),
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = item.habitName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(
                        rank = item.rankAchieved,
                        size = 32.dp,
                        tint = LocalRankColors.current.accent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${item.lengthDays} days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(item.rankAchieved.nameRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.edit_reason_title),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.dateRangeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            val reasonText = item.reason ?: stringResource(R.string.no_reason_given)
            Text(
                text = reasonText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.reason != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
        }
    }
}

package com.example.steadfast.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steadfast.R
import com.example.steadfast.SteadfastApp
import com.example.steadfast.domain.RankLadder
import com.example.steadfast.domain.model.HabitVisuals
import com.example.steadfast.ui.components.AddEditHabitDialog
import com.example.steadfast.ui.components.DayCounter
import com.example.steadfast.ui.components.DeleteHabitDialog
import com.example.steadfast.ui.components.HabitCommitGraph
import com.example.steadfast.ui.components.QuoteCard
import com.example.steadfast.ui.components.QuoteDisplay
import com.example.steadfast.ui.components.RankBadge
import com.example.steadfast.ui.components.ResetSheet
import com.example.steadfast.ui.history.EditReasonDialog
import com.example.steadfast.ui.theme.CardShape
import com.example.steadfast.ui.theme.LocalRankColors
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToRanks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as SteadfastApp
    val container = context.container
    val viewModel: HabitDetailViewModel = viewModel(
        factory = HabitDetailViewModel.provideFactory(
            habitId = habitId,
            habitRepository = container.habitRepository,
            streakRepository = container.streakRepository,
            quoteRepository = container.quoteRepository,
            settingsRepository = container.settingsRepository,
            context = context,
            clock = container.clock
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resetSnackbarMsg = stringResource(R.string.reset_snackbar_message)
    val undoMsg = stringResource(R.string.reset_snackbar_undo)

    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HabitDetailEvent.ShowResetSuccessSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resetSnackbarMsg,
                        actionLabel = undoMsg,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoReset()
                    }
                }
                is HabitDetailEvent.HabitDeleted -> {
                    onNavigateBack()
                }
            }
        }
    }

    val item = uiState.habitWithStreak

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (item != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(item.habit.color), shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = HabitVisuals.getIconResource(item.habit.icon)),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.habit_menu_edit)) },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showMenu = false
                                viewModel.openEditDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (item?.habit?.isArchived == true) R.string.habit_menu_unarchive
                                        else R.string.habit_menu_archive
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painterResource(
                                        if (item?.habit?.isArchived == true) R.drawable.ic_unarchive
                                        else R.drawable.ic_archive
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                viewModel.toggleArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.habit_menu_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                viewModel.openDeleteDialog()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (item == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // 1. Day Counter Hero
                    DayCounter(
                        days = item.currentStreakDays,
                        progressToNext = item.rankProgress.progressToNext,
                        startedAtMillis = item.activeStreak?.startedAt ?: System.currentTimeMillis()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Rank & Progression Card
                    val nextRank = item.rankProgress.nextRank
                    val rankSubtitle = if (nextRank != null) {
                        stringResource(
                            R.string.days_to_next_rank,
                            item.rankProgress.daysToNextRank,
                            stringResource(nextRank.nameRes)
                        )
                    } else {
                        stringResource(R.string.highest_rank_reached)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .clickable(onClick = onNavigateToRanks),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RankBadge(
                                    rank = item.currentRank,
                                    size = 40.dp,
                                    tint = LocalRankColors.current.accent
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(item.currentRank.nameRes),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = rankSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (nextRank != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { item.rankProgress.progressToNext },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = LocalRankColors.current.accent,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2.5 Consistency Graph (GitHub-styled Commit Heatmap)
                    HabitCommitGraph(
                        history = uiState.history,
                        activeStreak = item.activeStreak,
                        firstDayOfWeek = uiState.firstDayOfWeek,
                        onFirstDayOfWeekChange = { viewModel.setFirstDayOfWeek(it) },
                        clock = container.clock
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Reset Button
                    OutlinedButton(
                        onClick = { viewModel.openResetSheet() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_history),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.reset_button),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 4. Recent Streak History for this habit
                    if (uiState.history.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        )

                        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        uiState.history.take(3).forEach { streak ->
                            val days = streak.lengthDays ?: 0
                            val rank = RankLadder.getRankForDays(days)
                            val startLocalDate = LocalDate.ofEpochDay(streak.startDate)
                            val endLocalDate = streak.endDate?.let { LocalDate.ofEpochDay(it) } ?: startLocalDate
                            val dateRange = "${formatter.format(startLocalDate)} – ${formatter.format(endLocalDate)}"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.openEditReason(streak) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RankBadge(
                                        rank = rank,
                                        size = 32.dp,
                                        tint = LocalRankColors.current.accent
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$days ${stringResource(R.string.days_label)} • ${stringResource(rank.nameRes)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = dateRange,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        if (!streak.reason.isNullOrBlank()) {
                                            Text(
                                                text = "“${streak.reason}”",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_edit),
                                        contentDescription = stringResource(R.string.edit_reason_title),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Quote Card at bottom
                if (uiState.quote != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp, top = 24.dp)
                    ) {
                        QuoteCard(
                            quote = QuoteDisplay(
                                text = uiState.quote!!.text,
                                author = uiState.quote!!.author
                            ),
                            onNextQuote = { viewModel.nextQuote() }
                        )
                    }
                }
            }

            // Reset Sheet
            if (uiState.isResetSheetOpen) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ResetSheet(
                    sheetState = sheetState,
                    days = item.currentStreakDays,
                    currentRank = item.currentRank,
                    onConfirmReset = { reason -> viewModel.confirmReset(reason) },
                    onDismiss = { viewModel.closeResetSheet() }
                )
            }

            // Edit Dialog
            if (uiState.isEditDialogOpen) {
                AddEditHabitDialog(
                    onDismiss = { viewModel.closeEditDialog() },
                    onConfirm = { name, icon, color, startDate ->
                        viewModel.saveEdit(name, icon, color, startDate)
                    },
                    initialName = item.habit.name,
                    initialIcon = item.habit.icon,
                    initialColor = item.habit.color,
                    initialStartDate = item.activeStreak?.startDate?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now(),
                    isEditing = true
                )
            }

            // Delete Dialog
            if (uiState.isDeleteDialogOpen) {
                DeleteHabitDialog(
                    habitName = item.habit.name,
                    onConfirm = { viewModel.confirmDelete() },
                    onDismiss = { viewModel.closeDeleteDialog() }
                )
            }

            // Edit Reason Dialog
            if (uiState.editingStreakReason != null) {
                EditReasonDialog(
                    initialReason = uiState.editingStreakReason!!.reason,
                    onDismiss = { viewModel.closeEditReason() },
                    onSave = { reason ->
                        viewModel.saveReason(uiState.editingStreakReason!!.id, reason)
                    }
                )
            }
        }
    }
}

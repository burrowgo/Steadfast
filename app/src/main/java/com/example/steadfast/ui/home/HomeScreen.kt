package com.example.steadfast.ui.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.steadfast.ui.components.DayCounter
import com.example.steadfast.ui.components.EmptyState
import com.example.steadfast.ui.components.QuoteCard
import com.example.steadfast.ui.components.QuoteDisplay
import android.content.Intent
import android.net.Uri
import com.example.steadfast.ui.components.RankBadge
import com.example.steadfast.ui.components.RankUpDialog
import com.example.steadfast.ui.components.ResetSheet
import com.example.steadfast.ui.components.UpdateAvailableDialog
import com.example.steadfast.ui.components.WhatsNewDialog
import com.example.steadfast.ui.components.HabitCommitGraph
import com.example.steadfast.data.prefs.FirstDayOfWeek
import com.example.steadfast.ui.theme.CardShape
import com.example.steadfast.ui.theme.LocalRankColors
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as SteadfastApp
    val container = context.container
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            streakRepository = container.streakRepository,
            settingsRepository = container.settingsRepository,
            quoteRepository = container.quoteRepository,
            context = context,
            clock = container.clock,
            updateChecker = container.updateChecker
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val whatsNew by viewModel.whatsNewRelease.collectAsStateWithLifecycle()
    val updateAvailable by viewModel.updateAvailable.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resetSnackbarMsg = stringResource(R.string.reset_snackbar_message)
    val undoMsg = stringResource(R.string.reset_snackbar_undo)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeEvent.ShowResetSuccessSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resetSnackbarMsg,
                        actionLabel = undoMsg,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoReset()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState is HomeUiState.Active) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.openResetSheet() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_history),
                                contentDescription = stringResource(R.string.reset_button)
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.action_settings)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is HomeUiState.FirstRun -> {
                    EmptyState(
                        onStartHabit = { name, date -> viewModel.startHabit(name, date) }
                    )
                }

                is HomeUiState.Active -> {
                    ActiveHomeContent(
                        state = state,
                        onOpenResetSheet = { viewModel.openResetSheet() },
                        onNextQuote = { viewModel.nextQuote() },
                        clock = container.clock
                    )

                    if (state.isResetSheetOpen) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ResetSheet(
                            sheetState = sheetState,
                            days = state.days,
                            currentRank = state.rankProgress.currentRank,
                            onConfirmReset = { reason -> viewModel.confirmReset(reason) },
                            onDismiss = { viewModel.closeResetSheet() }
                        )
                    }

                    if (state.rankUpToCelebrate != null) {
                        RankUpDialog(
                            rank = state.rankUpToCelebrate,
                            onDismiss = { viewModel.dismissCelebration() }
                        )
                    }
                }
            }

            if (whatsNew != null) {
                WhatsNewDialog(
                    release = whatsNew!!,
                    onDismiss = { viewModel.dismissWhatsNew() }
                )
            }

            if (updateAvailable != null) {
                UpdateAvailableDialog(
                    update = updateAvailable!!,
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }
        }
    }
}

@Composable
private fun ActiveHomeContent(
    state: HomeUiState.Active,
    onOpenResetSheet: () -> Unit,
    onNextQuote: () -> Unit,
    modifier: Modifier = Modifier,
    clock: java.time.Clock = java.time.Clock.systemDefaultZone()
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Habit Name Heading
            Text(
                text = state.habitName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 10.dp)
            )

            // 1. Day Counter Hero
            DayCounter(
                days = state.days,
                progressToNext = state.rankProgress.progressToNext,
                startedAtMillis = state.streak.startedAt,
                size = 180.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Rank & Progression Card
            val currentRank = state.rankProgress.currentRank
            val nextRank = state.rankProgress.nextRank
            val rankSubtitle = if (nextRank != null) {
                stringResource(
                    R.string.days_to_next_rank,
                    state.rankProgress.daysToNextRank,
                    stringResource(nextRank.nameRes)
                )
            } else {
                stringResource(R.string.highest_rank_reached)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RankBadge(
                            rank = currentRank,
                            size = 26.dp,
                            tint = LocalRankColors.current.accent
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(currentRank.nameRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = rankSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (nextRank != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.rankProgress.progressToNext },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = LocalRankColors.current.accent,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Reset Button (Directly reachable without scrolling!)
            OutlinedButton(
                onClick = onOpenResetSheet,
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_history),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.reset_button),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Consistency Heatmap Graph
            HabitCommitGraph(
                history = state.history,
                activeStreak = state.streak,
                firstDayOfWeek = state.firstDayOfWeek,
                clock = clock
            )
        }

        // 5. Quote Card at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 12.dp)
        ) {
            QuoteCard(
                quote = QuoteDisplay(
                    text = state.quote.text,
                    author = state.quote.author
                ),
                onNextQuote = onNextQuote
            )
        }
    }
}

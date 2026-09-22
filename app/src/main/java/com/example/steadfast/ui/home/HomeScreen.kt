package com.example.steadfast.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNextQuote: () -> Unit,
    modifier: Modifier = Modifier,
    clock: java.time.Clock = java.time.Clock.systemDefaultZone()
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        val availableHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .heightIn(min = availableHeight)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = distributedVerticalArrangement(
                minSpace = 10.dp,
                maxSpace = 38.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Day Counter Hero & Habit Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                DayCounter(
                    days = state.days,
                    progressToNext = state.rankProgress.progressToNext,
                    startedAtMillis = state.streak.startedAt,
                    size = 152.dp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = state.habitName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Redesigned Rank & Progression Card
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Framed Badge Emblem
                    Surface(
                        shape = CircleShape,
                        color = LocalRankColors.current.accent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LocalRankColors.current.accent.copy(alpha = 0.25f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            RankBadge(
                                rank = currentRank,
                                size = 22.dp,
                                tint = LocalRankColors.current.accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Rank Title, Subtitle, and Progress Bar
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(currentRank.nameRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = rankSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (nextRank != null) {
                            Spacer(modifier = Modifier.height(6.dp))
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
            }

            // 3. Consistency Heatmap Graph
            HabitCommitGraph(
                history = state.history,
                activeStreak = state.streak,
                firstDayOfWeek = state.firstDayOfWeek,
                clock = clock
            )

            // 4. Quote Card at bottom
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

/**
 * An arrangement that distributes vertical space between items (`Arrangement.SpaceBetween`),
 * while guaranteeing a minimum gap [minSpace] so items never crumble together on small screens,
 * and capping the gap at [maxSpace] with balanced centering on ultra-tall screens.
 */
fun distributedVerticalArrangement(
    minSpace: Dp = 10.dp,
    maxSpace: Dp = 38.dp
): Arrangement.Vertical = object : Arrangement.Vertical {
    override val spacing = minSpace

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray
    ) {
        if (sizes.isEmpty()) return
        if (sizes.size == 1) {
            outPositions[0] = (totalSize - sizes[0]).coerceAtLeast(0) / 2
            return
        }

        val minSpacePx = minSpace.roundToPx()
        val maxSpacePx = maxSpace.roundToPx()
        val consumedChildren = sizes.sum()
        val totalAvailable = totalSize - consumedChildren
        val gapsCount = sizes.size - 1

        val idealSpace = if (totalAvailable > 0) totalAvailable / gapsCount else 0
        val actualGap = idealSpace.coerceIn(minSpacePx, maxSpacePx)

        val totalUsed = consumedChildren + actualGap * gapsCount
        val remainingSpace = totalSize - totalUsed
        val startOffset = if (remainingSpace > 0) remainingSpace / 2 else 0

        var current = startOffset
        for (i in sizes.indices) {
            outPositions[i] = current
            current += sizes[i] + actualGap
        }
    }
}

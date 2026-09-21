package com.example.steadfast.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.steadfast.ui.components.AddEditHabitDialog
import com.example.steadfast.ui.components.HabitCard
import com.example.steadfast.ui.components.QuoteCard
import com.example.steadfast.ui.components.QuoteDisplay
import com.example.steadfast.ui.components.UpdateAvailableDialog
import com.example.steadfast.ui.components.WhatsNewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHabitDetail: (habitId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as SteadfastApp
    val container = context.container
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            habitRepository = container.habitRepository,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddHabitDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_habit_title)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.activeHabits.isEmpty() && uiState.archivedHabits.isEmpty() -> {
                    HomeEmptyState(
                        onAddHabit = { viewModel.openAddHabitDialog() }
                    )
                }

                else -> {
                    val displayedHabits = if (uiState.selectedTab == HabitTab.ACTIVE) {
                        uiState.activeHabits
                    } else {
                        uiState.archivedHabits
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Optional Active/Archived Filter Chips
                        if (uiState.archivedHabits.isNotEmpty()) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    FilterChip(
                                        selected = uiState.selectedTab == HabitTab.ACTIVE,
                                        onClick = { viewModel.selectTab(HabitTab.ACTIVE) },
                                        label = {
                                            Text("${stringResource(R.string.habit_tab_active)} (${uiState.activeHabits.size})")
                                        }
                                    )
                                    FilterChip(
                                        selected = uiState.selectedTab == HabitTab.ARCHIVED,
                                        onClick = { viewModel.selectTab(HabitTab.ARCHIVED) },
                                        label = {
                                            Text("${stringResource(R.string.habit_tab_archived)} (${uiState.archivedHabits.size})")
                                        }
                                    )
                                }
                            }
                        }

                        // Habits list
                        items(
                            items = displayedHabits,
                            key = { it.habit.id }
                        ) { habitItem ->
                            HabitCard(
                                item = habitItem,
                                onClick = { onNavigateToHabitDetail(habitItem.habit.id) }
                            )
                        }

                        // Quote Card anchored at the bottom of the content
                        if (uiState.quote != null) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                QuoteCard(
                                    quote = QuoteDisplay(
                                        text = uiState.quote!!.text,
                                        author = uiState.quote!!.author
                                    ),
                                    onNextQuote = { viewModel.nextQuote() },
                                    modifier = Modifier.padding(bottom = 60.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isAddHabitDialogOpen) {
                AddEditHabitDialog(
                    onDismiss = { viewModel.closeAddHabitDialog() },
                    onConfirm = { name, icon, color, startDate ->
                        viewModel.createHabit(name, icon, color, startDate)
                    }
                )
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
private fun HomeEmptyState(
    onAddHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_habit_shield),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.habit_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.habit_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onAddHabit,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(R.string.habit_empty_button))
        }
    }
}

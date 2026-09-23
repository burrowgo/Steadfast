package com.example.steadfast.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steadfast.R
import com.example.steadfast.SteadfastApp
import com.example.steadfast.data.prefs.AutoUpdateFrequency
import com.example.steadfast.data.prefs.FirstDayOfWeek
import com.example.steadfast.data.prefs.ThemeMode
import com.example.steadfast.data.prefs.WidgetShape
import com.example.steadfast.data.updater.UpdateCheckResult
import com.example.steadfast.ui.components.UpdateAvailableDialog
import com.example.steadfast.ui.components.WhatsNewDialog
import com.example.steadfast.ui.settings.dialogs.AutoUpdateFrequencySelectionDialog
import com.example.steadfast.ui.settings.dialogs.EraseDataDialog
import com.example.steadfast.ui.settings.dialogs.FirstDayOfWeekSelectionDialog
import com.example.steadfast.ui.settings.dialogs.LicensesDialog
import com.example.steadfast.ui.settings.dialogs.RenameHabitDialog
import com.example.steadfast.ui.settings.dialogs.ThemeSelectionDialog
import com.example.steadfast.ui.settings.dialogs.WidgetShapeSelectionDialog
import com.example.steadfast.ui.theme.CardShape
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToWidgetSettings: () -> Unit = {}
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

    var showRenameDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showWidgetShapeDialog by remember { mutableStateOf(false) }
    var showFirstDayOfWeekDialog by remember { mutableStateOf(false) }
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // CSV Document Creation launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                viewModel.exportCsv(outputStream)
            }
        }
    }

    // CSV Document Import launcher
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    viewModel.importCsv(inputStream) { success, count ->
                        coroutineScope.launch {
                            val msg = if (success) {
                                context.getString(R.string.import_success, count)
                            } else {
                                context.getString(R.string.import_failed)
                            }
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                }
            }
        }
    }

    // Notification permission launcher (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setReminderEnabled(true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.edit_reason_cancel)
                        )
                    }
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Habit Section
            if (uiState.activeHabitExists) {
                SettingsSectionHeader(stringResource(R.string.settings_section_habit))
                Card(
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRenameDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_habit_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = uiState.habitName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }

                        val currentStartDate = uiState.activeStartDate
                        if (currentStartDate != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                                                if (!selected.isAfter(LocalDate.now())) {
                                                    viewModel.updateStartDate(selected)
                                                }
                                            },
                                            currentStartDate.year,
                                            currentStartDate.monthValue - 1,
                                            currentStartDate.dayOfMonth
                                        ).apply {
                                            datePicker.maxDate = System.currentTimeMillis()
                                        }.show()
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_start_date),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentStartDate.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Appearance Section
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Theme Row
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showThemeDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_theme),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val themeName = when (uiState.themeMode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                            Text(
                                text = themeName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Widget Customization Row
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToWidgetSettings() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_widget_customization),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val shapeName = when (uiState.widgetShape) {
                                WidgetShape.ROUNDED -> stringResource(R.string.widget_shape_rounded)
                                WidgetShape.CIRCLE -> stringResource(R.string.widget_shape_circle)
                            }
                            val fontName = when (uiState.widgetFontColor) {
                                com.example.steadfast.data.prefs.WidgetFontColor.DEFAULT -> stringResource(R.string.widget_font_color_default)
                                com.example.steadfast.data.prefs.WidgetFontColor.WHITE -> stringResource(R.string.widget_font_color_white)
                                com.example.steadfast.data.prefs.WidgetFontColor.BLACK -> stringResource(R.string.widget_font_color_black)
                                com.example.steadfast.data.prefs.WidgetFontColor.BRAND -> stringResource(R.string.widget_font_color_brand)
                            }
                            Text(
                                text = "$shapeName • ${uiState.widgetBackgroundOpacity}% • $fontName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    // First Day of Week Row
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFirstDayOfWeekDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_first_day_of_week),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val firstDayText = when (uiState.firstDayOfWeek) {
                                FirstDayOfWeek.MONDAY -> stringResource(R.string.first_day_monday)
                                FirstDayOfWeek.SUNDAY -> stringResource(R.string.first_day_sunday)
                            }
                            Text(
                                text = firstDayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Dynamic Color Toggle (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_dynamic_color),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.settings_dynamic_color_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.useDynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Section
            SettingsSectionHeader(stringResource(R.string.settings_section_notifications))
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_reminder),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setReminderEnabled(enabled)
                                }
                            }
                        )
                    }

                    if (uiState.reminderEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parts = uiState.reminderTime.split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            val formatted = "%02d:%02d".format(h, m)
                                            viewModel.setReminderTime(formatted)
                                        },
                                        hour,
                                        minute,
                                        true
                                    ).show()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_reminder_time),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.reminderTime,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Section
            SettingsSectionHeader(stringResource(R.string.settings_section_data))
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextButton(
                        onClick = { csvLauncher.launch("steadfast_history.csv") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_export_csv),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = {
                            importCsvLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/*",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_import_csv),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = { showEraseDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.settings_erase_all),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            SettingsSectionHeader(stringResource(R.string.settings_section_about))
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val versionName = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                        } catch (e: Exception) {
                            "1.0.0"
                        }
                    }
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Check for updates row
                    val lastCheckedSubtitle = if (uiState.isCheckingForUpdate) {
                        stringResource(R.string.settings_checking_updates)
                    } else if (uiState.lastUpdateCheckTime > 0L) {
                        val diff = System.currentTimeMillis() - uiState.lastUpdateCheckTime
                        val days = (diff / (1000 * 3600 * 24)).toInt()
                        val timeStr = when {
                            days == 0 -> stringResource(R.string.settings_last_checked_today)
                            days == 1 -> stringResource(R.string.settings_last_checked_yesterday)
                            else -> stringResource(R.string.settings_last_checked_days_ago, days)
                        }
                        stringResource(R.string.settings_last_checked_format, timeStr)
                    } else {
                        stringResource(R.string.settings_never_checked)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isCheckingForUpdate) {
                                viewModel.checkForUpdates()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_check_updates),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = lastCheckedSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (uiState.isCheckingForUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Auto-update frequency row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAutoUpdateDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_auto_update_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val frequencyText = when (uiState.autoUpdateFrequency) {
                                AutoUpdateFrequency.WEEKLY -> stringResource(R.string.settings_frequency_weekly)
                                AutoUpdateFrequency.DAILY -> stringResource(R.string.settings_frequency_daily)
                                AutoUpdateFrequency.MANUAL -> stringResource(R.string.settings_frequency_manual)
                            }
                            Text(
                                text = frequencyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = { viewModel.showWhatsNew(versionName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_whats_new))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = { showLicensesDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_licenses))
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Rename Dialog
        if (showRenameDialog) {
            RenameHabitDialog(
                currentName = uiState.habitName,
                onSave = {
                    viewModel.renameHabit(it)
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        // Theme Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = uiState.themeMode,
                onSelectTheme = {
                    viewModel.setThemeMode(it)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // Widget Shape Dialog
        if (showWidgetShapeDialog) {
            WidgetShapeSelectionDialog(
                currentShape = uiState.widgetShape,
                onSelectShape = {
                    viewModel.setWidgetShape(it)
                    showWidgetShapeDialog = false
                },
                onDismiss = { showWidgetShapeDialog = false }
            )
        }

        // Auto-Update Frequency Dialog
        if (showAutoUpdateDialog) {
            AutoUpdateFrequencySelectionDialog(
                currentFrequency = uiState.autoUpdateFrequency,
                onSelectFrequency = {
                    viewModel.setAutoUpdateFrequency(it)
                    showAutoUpdateDialog = false
                },
                onDismiss = { showAutoUpdateDialog = false }
            )
        }

        // First Day of Week Dialog
        if (showFirstDayOfWeekDialog) {
            FirstDayOfWeekSelectionDialog(
                currentFirstDay = uiState.firstDayOfWeek,
                onSelectFirstDay = {
                    viewModel.setFirstDayOfWeek(it)
                    showFirstDayOfWeekDialog = false
                },
                onDismiss = { showFirstDayOfWeekDialog = false }
            )
        }

        // Erase Confirmation Dialog
        if (showEraseDialog) {
            EraseDataDialog(
                onConfirmErase = { viewModel.eraseAllData() },
                onDismiss = { showEraseDialog = false }
            )
        }

        // Licenses Dialog
        if (showLicensesDialog) {
            LicensesDialog(
                context = context,
                onDismiss = { showLicensesDialog = false }
            )
        }

        // What's New Dialog
        val currentWhatsNew = uiState.showWhatsNew
        if (currentWhatsNew != null) {
            WhatsNewDialog(
                release = currentWhatsNew,
                onDismiss = { viewModel.dismissWhatsNew() }
            )
        }

        // Update Available Dialog
        val currentUpdate = uiState.updateResult
        if (currentUpdate is UpdateCheckResult.UpdateAvailable) {
            UpdateAvailableDialog(
                update = currentUpdate,
                onDismiss = { viewModel.dismissUpdateResult() }
            )
        }

        // Up to date Notification
        if (currentUpdate is UpdateCheckResult.UpToDate) {
            val upToDateMsg = stringResource(R.string.settings_up_to_date, currentUpdate.currentVersion)
            androidx.compose.runtime.LaunchedEffect(currentUpdate) {
                snackbarHostState.showSnackbar(upToDateMsg)
                viewModel.dismissUpdateResult()
            }
        }

        // Update Error Dialog
        if (currentUpdate is UpdateCheckResult.Error) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateResult() },
                title = { Text(stringResource(R.string.settings_check_updates)) },
                text = { Text(stringResource(R.string.settings_update_error, currentUpdate.message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, currentUpdate.releasePageUrl.toUri()).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            viewModel.dismissUpdateResult()
                        }
                    ) {
                        Text(stringResource(R.string.settings_view_releases))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateResult() }) {
                        Text(stringResource(R.string.edit_reason_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}


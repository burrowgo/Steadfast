package com.example.steadfast.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.steadfast.R
import com.example.steadfast.SteadfastApp
import com.example.steadfast.data.updater.ApkInstaller
import com.example.steadfast.data.updater.AppUpdateDownloader
import com.example.steadfast.data.updater.DefaultAppUpdateDownloader
import com.example.steadfast.data.updater.DownloadState
import com.example.steadfast.data.updater.UpdateCheckResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun UpdateAvailableDialog(
    update: UpdateCheckResult.UpdateAvailable,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    downloader: AppUpdateDownloader? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val actualDownloader = downloader
        ?: (context.applicationContext as? SteadfastApp)?.container?.updateDownloader
        ?: remember { DefaultAppUpdateDownloader(context) }

    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var canInstall by remember { mutableStateOf(ApkInstaller.canRequestPackageInstalls(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val updatedCanInstall = ApkInstaller.canRequestPackageInstalls(context)
                canInstall = updatedCanInstall
                val current = downloadState
                if (updatedCanInstall && current is DownloadState.Success) {
                    ApkInstaller.installApk(context, current.apkFile)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun openInBrowser() {
        val targetUrl = update.downloadUrl.ifBlank { update.releasePageUrl }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun startDownload() {
        if (update.downloadUrl.isBlank()) {
            openInBrowser()
            onDismiss()
            return
        }

        downloadJob?.cancel()
        downloadJob = coroutineScope.launch {
            actualDownloader.downloadUpdate(
                downloadUrl = update.downloadUrl,
                version = update.version
            ).collect { state ->
                downloadState = state
                if (state is DownloadState.Success) {
                    canInstall = ApkInstaller.canRequestPackageInstalls(context)
                    if (canInstall) {
                        ApkInstaller.installApk(context, state.apkFile)
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloadState = DownloadState.Idle
    }

    val onDismissRequest = {
        if (downloadState is DownloadState.Downloading) {
            cancelDownload()
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val icon = when (downloadState) {
                    is DownloadState.Idle -> "🚀"
                    is DownloadState.Downloading -> "⏳"
                    is DownloadState.Success -> "📦"
                    is DownloadState.Error -> "⚠️"
                }
                val title = when (downloadState) {
                    is DownloadState.Idle -> stringResource(R.string.settings_update_available)
                    is DownloadState.Downloading -> stringResource(R.string.update_downloading_title)
                    is DownloadState.Success -> stringResource(R.string.update_ready_title)
                    is DownloadState.Error -> stringResource(R.string.update_error_title)
                }
                Text(text = icon, fontSize = 24.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (val state = downloadState) {
                    is DownloadState.Idle -> {
                        Text(
                            text = stringResource(R.string.settings_update_available_desc, update.version),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (update.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = update.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { openInBrowser() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = stringResource(R.string.update_open_browser),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    is DownloadState.Downloading -> {
                        Text(
                            text = stringResource(R.string.update_downloading_desc, update.version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.totalBytes > 0L) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                strokeCap = StrokeCap.Round
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                strokeCap = StrokeCap.Round
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val progressText = if (state.totalBytes > 0L) {
                            val percent = (state.progress * 100).toInt().coerceIn(0, 100)
                            stringResource(
                                R.string.update_download_progress,
                                formatBytes(state.bytesDownloaded),
                                formatBytes(state.totalBytes),
                                percent
                            )
                        } else {
                            stringResource(
                                R.string.update_download_progress_indeterminate,
                                formatBytes(state.bytesDownloaded)
                            )
                        }
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is DownloadState.Success -> {
                        val desc = if (canInstall) {
                            stringResource(R.string.update_ready_desc, update.version)
                        } else {
                            stringResource(R.string.update_permission_desc)
                        }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    is DownloadState.Error -> {
                        Text(
                            text = stringResource(R.string.update_error_desc, state.message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { openInBrowser() },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(stringResource(R.string.update_open_browser))
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    Button(onClick = { startDownload() }) {
                        Text(stringResource(R.string.settings_update_now))
                    }
                }
                is DownloadState.Downloading -> {
                    // No confirm button during active download
                }
                is DownloadState.Success -> {
                    if (canInstall) {
                        Button(onClick = { ApkInstaller.installApk(context, state.apkFile) }) {
                            Text(stringResource(R.string.update_install_now))
                        }
                    } else {
                        Button(onClick = {
                            try {
                                context.startActivity(ApkInstaller.getManageUnknownAppSourcesIntent(context))
                            } catch (_: Exception) {}
                        }) {
                            Text(stringResource(R.string.update_grant_permission))
                        }
                    }
                }
                is DownloadState.Error -> {
                    Button(onClick = { startDownload() }) {
                        Text(stringResource(R.string.update_retry))
                    }
                }
            }
        },
        dismissButton = {
            when (downloadState) {
                is DownloadState.Idle -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_update_later))
                    }
                }
                is DownloadState.Downloading -> {
                    TextButton(onClick = { cancelDownload() }) {
                        Text(stringResource(R.string.update_cancel))
                    }
                }
                is DownloadState.Success -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.update_done))
                    }
                }
                is DownloadState.Error -> {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.update_cancel))
                    }
                }
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "UpdateAvailable Light")
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "UpdateAvailable Dark")
@Composable
private fun UpdateAvailableDialogPreview() {
    com.example.steadfast.ui.theme.SteadfastTheme {
        UpdateAvailableDialog(
            update = UpdateCheckResult.UpdateAvailable(
                version = "0.7.2",
                releaseNotes = "• In-app update downloads with live progress bar\n• Direct APK installation prompts\n• Rounded quote touch overlay",
                downloadUrl = "",
                releasePageUrl = ""
            ),
            onDismiss = {}
        )
    }
}

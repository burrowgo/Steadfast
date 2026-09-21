package com.example.steadfast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.steadfast.R
import com.example.steadfast.domain.Rank
import com.example.steadfast.ui.theme.LocalRankColors

@Composable
fun RankUpDialog(
    rank: Rank,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(rank) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val rankName = stringResource(rank.nameRes)
    val rankAccent = LocalRankColors.current.accent

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.celebration_continue))
            }
        },
        dismissButton = {
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val shareText = context.getString(
                        R.string.share_achievement_text,
                        rank.minDays,
                        rankName
                    )
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            ) {
                Text(stringResource(R.string.share_button))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.rank_achieved_banner),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(LocalRankColors.current.container, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    RankBadge(
                        rank = rank,
                        size = 56.dp,
                        tint = rankAccent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = rankName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.rank_time_approx, rank.timeDescription, rank.minDays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

package com.example.steadfast.ui.settings.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.steadfast.R
import com.example.steadfast.data.prefs.AutoUpdateFrequency

@Composable
fun AutoUpdateFrequencySelectionDialog(
    currentFrequency: AutoUpdateFrequency,
    onSelectFrequency: (AutoUpdateFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    val frequencies = listOf(
        AutoUpdateFrequency.WEEKLY to R.string.settings_frequency_weekly,
        AutoUpdateFrequency.DAILY to R.string.settings_frequency_daily,
        AutoUpdateFrequency.MANUAL to R.string.settings_frequency_manual
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_auto_update_title)) },
        text = {
            Column {
                frequencies.forEach { (freq, nameRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFrequency(freq) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFrequency == freq,
                            onClick = { onSelectFrequency(freq) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(nameRes))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.edit_reason_cancel))
            }
        }
    )
}

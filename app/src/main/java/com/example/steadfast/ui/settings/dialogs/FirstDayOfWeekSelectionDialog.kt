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
import com.example.steadfast.data.prefs.FirstDayOfWeek

@Composable
fun FirstDayOfWeekSelectionDialog(
    currentFirstDay: FirstDayOfWeek,
    onSelectFirstDay: (FirstDayOfWeek) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        FirstDayOfWeek.MONDAY to R.string.first_day_monday,
        FirstDayOfWeek.SUNDAY to R.string.first_day_sunday
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_first_day_of_week)) },
        text = {
            Column {
                options.forEach { (firstDay, nameRes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFirstDay(firstDay) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFirstDay == firstDay,
                            onClick = { onSelectFirstDay(firstDay) }
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

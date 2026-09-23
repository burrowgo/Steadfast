package com.example.steadfast.ui.settings.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.steadfast.R
import java.io.InputStreamReader

@Composable
fun LicensesDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val barlowLicense = remember {
        try {
            context.assets.open("licenses/BARLOW_OFL.txt").use {
                InputStreamReader(it).readText()
            }
        } catch (e: Exception) {
            "SIL Open Font License (Barlow Condensed)"
        }
    }

    val manropeLicense = remember {
        try {
            context.assets.open("licenses/MANROPE_OFL.txt").use {
                InputStreamReader(it).readText()
            }
        } catch (e: Exception) {
            "SIL Open Font License (Manrope)"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_licenses)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Barlow Condensed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(barlowLicense, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Manrope", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(manropeLicense, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.celebration_dismiss))
            }
        }
    )
}

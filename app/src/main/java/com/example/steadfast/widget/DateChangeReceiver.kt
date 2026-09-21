package com.example.steadfast.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetUpdater.updateAll(context)
                WidgetUpdater.scheduleMidnightWorker(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

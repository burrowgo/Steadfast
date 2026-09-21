package com.example.steadfast.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Will trigger widget update and schedule midnight worker
    }
}

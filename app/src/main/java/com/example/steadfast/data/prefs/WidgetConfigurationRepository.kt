package com.example.steadfast.data.prefs

import android.content.Context
import android.content.SharedPreferences

class WidgetConfigurationRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHabitIdForWidget(appWidgetId: Int): Long? {
        val key = keyFor(appWidgetId)
        return if (prefs.contains(key)) {
            val id = prefs.getLong(key, -1L)
            if (id > 0) id else null
        } else {
            null
        }
    }

    fun setHabitIdForWidget(appWidgetId: Int, habitId: Long) {
        prefs.edit().putLong(keyFor(appWidgetId), habitId).apply()
    }

    fun removeWidget(appWidgetId: Int) {
        prefs.edit().remove(keyFor(appWidgetId)).apply()
    }

    private fun keyFor(appWidgetId: Int): String = "widget_habit_$appWidgetId"

    companion object {
        private const val PREFS_NAME = "steadfast_widget_configurations"
    }
}

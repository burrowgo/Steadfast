package com.example.steadfast

import android.app.Application
import com.example.steadfast.data.updater.AutoUpdateScheduler
import com.example.steadfast.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SteadfastApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Schedule widget midnight worker and perform initial widget update
        WidgetUpdater.scheduleMidnightWorker(this)
        CoroutineScope(Dispatchers.Default).launch {
            WidgetUpdater.updateAll(this@SteadfastApp)
            val settings = container.settingsRepository.settingsFlow.first()
            AutoUpdateScheduler.schedule(this@SteadfastApp, settings.autoUpdateFrequency)
        }
    }
}

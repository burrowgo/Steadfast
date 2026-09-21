package com.example.steadfast

import android.content.Context
import com.example.steadfast.data.StreakRepository
import com.example.steadfast.data.db.AppDatabase
import com.example.steadfast.data.prefs.SettingsRepository
import com.example.steadfast.data.prefs.dataStore
import com.example.steadfast.data.updater.AppUpdateDownloader
import com.example.steadfast.data.updater.DefaultAppUpdateDownloader
import com.example.steadfast.data.updater.DefaultUpdateChecker
import com.example.steadfast.data.updater.UpdateChecker
import com.example.steadfast.domain.QuoteRepository
import java.time.Clock

interface AppContainer {
    val clock: Clock
    val streakRepository: StreakRepository
    val settingsRepository: SettingsRepository
    val quoteRepository: QuoteRepository
    val updateChecker: UpdateChecker
    val updateDownloader: AppUpdateDownloader
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val clock: Clock = Clock.systemDefaultZone()

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val streakRepository: StreakRepository by lazy {
        StreakRepository(database.streakDao(), clock)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.dataStore)
    }

    override val quoteRepository: QuoteRepository by lazy {
        QuoteRepository.loadFromRaw(context, clock)
    }

    override val updateChecker: UpdateChecker by lazy {
        DefaultUpdateChecker()
    }

    override val updateDownloader: AppUpdateDownloader by lazy {
        DefaultAppUpdateDownloader(context)
    }
}

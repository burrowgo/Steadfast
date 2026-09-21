package com.example.steadfast

import android.content.Context
import java.time.Clock

interface AppContainer {
    val clock: Clock
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val clock: Clock = Clock.systemDefaultZone()
}

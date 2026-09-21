package com.example.steadfast.domain

data class ChangelogRelease(
    val version: String,
    val title: String,
    val highlights: List<String>
)

object ChangelogRepository {
    val releases = listOf(
        ChangelogRelease(
            version = "0.4.0",
            title = "What’s New in v0.4.0",
            highlights = listOf(
                "Simplified Widgets: Home screen widgets now cleanly display habit streak days without showing hours.",
                "Cleaner Home Screen: Focused, distraction-free widget presentation that stays battery-efficient."
            )
        ),
        ChangelogRelease(
            version = "0.3.0",
            title = "What’s New in v0.3.0",
            highlights = listOf(
                "In-App Updates: Check for and download new releases directly from GitHub within Settings.",
                "What’s New Dialog: Automatically discover new features and highlights after updating.",
                "Direct Download: Easily download updated release APKs with one tap."
            )
        ),
        ChangelogRelease(
            version = "0.2.0",
            title = "What’s New in v0.2.0",
            highlights = listOf(
                "Custom Start Date: Choose a past start date during setup or adjust it anytime in Settings without losing your streak.",
                "Data Backup & Restore: Import previous Steadfast CSV backups to restore active and past streak history.",
                "Milestone Sharing: Easily share unlocked rank achievements directly to your favorite apps.",
                "Subtle Haptics: Tactile feedback on quote shuffles, ticker taps, and streak reset confirmations.",
                "Live Elapsed Ticker: Real-time hours, minutes, and seconds counter on Day 0 and beyond.",
                "Quote Rotation & Shuffling: Motivational quotes refresh periodically throughout the day and can be shuffled on tap."
            )
        ),
        ChangelogRelease(
            version = "0.1.0",
            title = "Welcome to Steadfast v0.1.0",
            highlights = listOf(
                "Single-Purpose Tracker: 100% offline, privacy-first habit tracker with zero tracking and zero bloat.",
                "Home Screen Widgets: 1x1 circular and rectangular quick-glance widgets.",
                "Milestone Ranks: Progression ladder with rank badges and celebration dialogs.",
                "Daily Reminders: Scheduled reminder notifications.",
                "CSV Export: Backup your streak history anytime."
            )
        )
    )

    fun getRelease(version: String): ChangelogRelease? {
        val clean = version.removePrefix("v").substringBefore("-").trim()
        return releases.firstOrNull { it.version.removePrefix("v").substringBefore("-").trim() == clean }
            ?: releases.firstOrNull()
    }
}

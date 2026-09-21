package com.example.steadfast.domain

data class ChangelogRelease(
    val version: String,
    val title: String,
    val highlights: List<String>
)

object ChangelogRepository {
    val releases = listOf(
        ChangelogRelease(
            version = "0.7.0",
            title = "What’s New in v0.7.0",
            highlights = listOf(
                "Widget Customization: Full control over widget background opacity/transparency (from 0% glass to 100% solid).",
                "Text & Background Color Themes: Choose from Theme Default, Pure White, Pure Black, and Brand Olive text, paired with customizable background colors.",
                "Live Interactive Widget Preview: Preview your customized widget in real-time across 2x2, 4x1, and 1x1 sizes with simulated dark and light wallpapers directly in Settings."
            )
        ),
        ChangelogRelease(
            version = "0.6.0",
            title = "What’s New in v0.6.0",
            highlights = listOf(
                "Widget Days Label Visibility: Fixed layout sizing so the 'days' label is always clearly visible on 2x2 and 4x1 home screen widgets without clipping.",
                "Dedicated 4x1 Widget Layout: Introduced a streamlined horizontal layout for 4x1 widgets with habit streak and milestone progress.",
                "Responsive Widget Sizing: Refined widget padding and font scaling across all grid configurations."
            )
        ),
        ChangelogRelease(
            version = "0.5.0",
            title = "What’s New in v0.5.0",
            highlights = listOf(
                "Automatic Update Checks: Steadfast now checks for updates periodically in the background (Weekly by default, with Daily & Manual options).",
                "Release APK Priority: Fixed update downloads to always fetch production release APK builds.",
                "Background & Launch Alerts: Surface updates via system notifications and in-app alerts on open."
            )
        ),
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

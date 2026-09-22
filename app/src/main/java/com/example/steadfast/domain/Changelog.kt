package com.example.steadfast.domain

data class ChangelogRelease(
    val version: String,
    val title: String,
    val highlights: List<String>
)

object ChangelogRepository {
    val releases = listOf(
        ChangelogRelease(
            version = "0.8.0-alpha.6",
            title = "What’s New in v0.8.0-alpha.6",
            highlights = listOf(
                "Widget Habit Selection Solved: Fixed persistent issue where selecting second or third habits displayed data from the first habit.",
                "Reactive Glance State: Moved widget rendering to reactive Glance DataStore state so each widget independently retains and displays its own habit.",
                "Atomic State Updates: Full habit and streak data is synchronized per-widget across all update and configuration lifecycles."
            )
        ),
        ChangelogRelease(
            version = "0.8.0-alpha.5",
            title = "What’s New in v0.8.0-alpha.5",
            highlights = listOf(
                "Widget Habit Selection Fix: Fixed errors when configuring secondary habits and resolved fallback to habit 1.",
                "Glance Stability: Eliminated composition-time state mutation to prevent widget error screens.",
                "Atomic Widget Setup: Synchronous persistence and background state synchronization for immediate reliability.",
                "Direct Widget Navigation: Tapping an unstarted habit widget opens the app directly focused on that habit."
            )
        ),
        ChangelogRelease(
            version = "0.8.0-alpha.4",
            title = "What’s New in v0.8.0-alpha.4",
            highlights = listOf(
                "Per-Widget Habit Selection: Overhauled widget creation and binding to ensure every widget retains its selected habit.",
                "2x2 Widget Layout Fix: Balanced vertical layout with prominent day count, rank name, and progress bar without horizontal squishing.",
                "Habit-Specific Quotes: Randomized quotes per habit so each habit displays its own rotating motivational quote.",
                "Cleaner Home Screen: Removed redundant quote card from the multi-habit home screen."
            )
        ),
        ChangelogRelease(
            version = "0.8.0-alpha.3",
            title = "What’s New in v0.8.0-alpha.3",
            highlights = listOf(
                "Independent Multi-Habit Widgets: Each home-screen widget now correctly displays its selected habit with independent streak tracking.",
                "Direct Widget Navigation: Tapping any widget opens directly into that habit's detail view.",
                "Full Habit Management in Settings: View, edit (name, icon, color, start date), and add all habits directly from Settings.",
                "Backup Import & Lifecycle Polish: Multi-habit CSV restore with database integrity and automatic cleanup for removed widgets."
            )
        ),
        ChangelogRelease(
            version = "0.8.0-alpha.2",
            title = "What’s New in v0.8.0-alpha.2",
            highlights = listOf(
                "GitHub-Styled Consistency Heatmap: Interactive 24-week contribution graph on each habit's detail screen.",
                "Maintained vs Reset Day Highlights: Maintained streak days are displayed in vibrant green shades, and reset/streak-broken days in neutral grey.",
                "Interactive Inspection: Tap any square to view date, streak day number, or reset reason.",
                "Customizable Week Start: Default first day of week is Monday, with option to choose Sunday directly from graph or Settings."
            )
        ),
        ChangelogRelease(
            version = "0.8.0-alpha.1",
            title = "What’s New in v0.8.0-alpha.1",
            highlights = listOf(
                "Multi-Habit Tracking: Track multiple habits simultaneously with independent streaks, ranks, history, and reset reasons.",
                "Category Icons & Color Themes: Customize habits with 10 icons and unique color accents.",
                "Habit Detail Screen: Dedicated view per habit featuring circular counter, milestones, and streak history.",
                "Side-by-Side Alpha Install: Installs as Steadfast Alpha without replacing your stable app."
            )
        ),
        ChangelogRelease(
            version = "0.7.4",
            title = "What’s New in v0.7.4",
            highlights = listOf(
                "Polished Home UI & Standard App Title: Replaced the habit name in the top app bar with Steadfast for consistent app navigation and habit privacy.",
                "Rank Progression Card: Encased current rank and next milestone in a unified Material 3 card with a smooth linear progress bar indicator.",
                "Clean Counter Layout: Removed the Day 0 hint message below the day counter ring for a clean, symmetric presentation.",
                "Widget Habit Title Privacy: Added a toggle in Widget Settings to hide or show habit names on home screen widgets for discrete tracking."
            )
        ),
        ChangelogRelease(
            version = "0.7.3",
            title = "What’s New in v0.7.3",
            highlights = listOf(
                "Widget Customization Preset Fix: Fixed an issue where the '100% (Solid)' opacity preset button was compressed and displayed as a long vertical button.",
                "Responsive Chip Flow: Preset buttons now wrap cleanly as horizontal pills across all display densities and screen sizes."
            )
        ),
        ChangelogRelease(
            version = "0.7.2",
            title = "What’s New in v0.7.2",
            highlights = listOf(
                "Rounded Quote Touch Overlay: Fixed the click overlay on the home screen quote card to follow the card's 24dp rounded corners instead of appearing as a sharp rectangle.",
                "Polished Touch Feedback: Consistent shape-bounded ripple animations across quote and history cards."
            )
        ),
        ChangelogRelease(
            version = "0.7.1",
            title = "What’s New in v0.7.1",
            highlights = listOf(
                "In-App Update Downloader: Seamlessly download release APK updates directly inside the app with live download progress indicators.",
                "Direct Package Installation: Automatically prompt the Android package installer when download finishes without redirecting to an external browser.",
                "Permission & Fallback Handling: Guided 'Install unknown apps' permission management with fallback browser download support."
            )
        ),
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

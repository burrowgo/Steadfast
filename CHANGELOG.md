# Changelog

All notable changes to Steadfast are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-09-23

### Added
- **Architectural Standardization Baseline:** Fully modularized and layered Clean Architecture (UI / Domain / Data) establishing Steadfast 1.0.0 as a rock-solid, production-grade habit companion.
- **Settings Dialog Componentization:** Decomposed monolith settings screen into dedicated dialogs under `ui/settings/dialogs/` (`ThemeDialog`, `WidgetShapeDialog`, `DayStartDialog`, `WeekStartDialog`, `ExportDialog`, `ImportDialog`, `WhatsNewDialog`).
- **Comprehensive Unit Test Suite:** Expanded test suite to 69 local JVM unit tests validating domain calculations, rank thresholds, timezone handling, update checking, and streak stability.

### Changed
- **Centralized Streak Engine:** Consolidated streak progression, rank milestone determination, and milestone celebration logic into `StreakCalculator`, eliminating time-drift and duplicate calculation logic across UI and widget workers.
- **Lifecycle & Memory Hardening:** Migrated ViewModels to `AndroidViewModel` to eradicate Android Lint `StaticFieldLeak` warnings and prevent retention of Android `Context` instances across activity lifecycles.
- **Decoupled Celebration State Flow:** Replaced inline `combine` celebration side-effects with dedicated, decoupled coroutine event emission.
- **IO-Bound Operations:** Offloaded CSV database import, export, and JSON update parsing explicitly to `Dispatchers.IO` to ensure zero main-thread blocking.

### Fixed
- **Protected Broadcast Receivers:** Hardened `DateChangeReceiver` against spoofed broadcast intents by strictly validating `Intent.ACTION_DATE_CHANGED` and `Intent.ACTION_TIMEZONE_CHANGED`.
- **Atomic Undo-Reset Safety:** Guarded `StreakDao.undoLastReset` with explicit single-row verification, safely preventing data corruption if streak records are missing.
- **TalkBack & Accessibility Polish:** Improved `RankBadge` TalkBack content description to properly articulate both current rank and progress towards the next milestone.
- **Zero Lint Errors & Clean Resources:** Eliminated all 40 pre-existing lint warnings/errors, deleted unused legacy resource files, and optimized R8 ProGuard shrink rules for production release builds.

---

## [0.7.10] - 2026-09-22

### Changed
- **Tactical Precision Chronometer & Timer Redesign:** Redesigned the day counter into a precision tactical chronometer with optimized inner dial canvas padding, ensuring generous clearance (>18dp on all sides) so the elapsed timer pill never touches or overlaps the inner circle.
- **Pulsing Live Indicator:** Integrated an animated real-time LED pulse dot (`●`) indicating active wall-clock streak progression.
- **Interactive Tap-To-Toggle Display:** Tapping the chronometer pill provides tactile haptic feedback and toggles between compact digital clock format (`+04:23:12` / `04:23:12`) and unit format (`+ 04h 23m 12s` / `04h 23m 12s`).
- **Gradient Progress Arc & Glowing Bead:** Enhanced the rank progress arc with a linear gradient transition into the rank accent color and an animated glowing pip bead at the leading tip of progress.
- **Hairline Inner Bezel:** Rendered a subtle hairline guide circle inside the gauge for an authentic precision instrument aesthetic.

---

## [0.7.9] - 2026-09-22

### Changed
- **Space-Between Home Screen Layout:** Replaced rigid, cramped spacers with dynamic vertical space distribution (`distributedVerticalArrangement`). All dashboard elements gracefully expand to fit standard screen viewports without scrolling, eliminating both cramped cards and awkward bottom blank space.
- **Adaptive Screen Clamping:** Guaranteed safe minimum spacing (10dp) with smooth vertical scrolling on compact screens or large display scales, and capped spacing (38dp) with balanced centering on extra-tall devices.
- **Pill Badge for Active Habit:** Encased the habit name in a refined, low-profile pill chip with soft surface tinting and subtle border, pairing cleanly with the circular day counter.
- **Unified Card Alignments:** Standardized horizontal padding to 14dp across the Rank Card, Consistency Heatmap, and Quote Card for crisp visual alignment.

---

## [0.7.8] - 2026-09-22

### Changed
- **Single-Screen Viewport Fit:** Optimized component heights, cell sizes, and vertical spacing across the home screen so the entire dashboard fits within a single viewport without requiring scrolling on standard devices.
- **Habit Name Below Counter:** Positioned the active habit title directly underneath the circular day counter hero, establishing an intuitive visual connection between the streak count and the habit.
- **Redesigned Badge & Rank Progression:** Elevated the rank badge with a framed circular emblem featuring subtle accent tinting and border, paired with clear milestone typography and a sleek linear progress indicator.
- **Refined Component Styling:** Unified cards (Rank Card, Consistency Heatmap, and Quote Card) with 16dp rounded corners and subtle 1dp outline borders for a cohesive, modern aesthetic.

### Fixed
- **Removed Redundant Reset Button:** Removed the duplicate Reset Streak button below the counter; streak resets remain conveniently and quickly accessible via the history icon in the top app bar.

---

## [0.7.7] - 2026-09-22

### Changed
- **Compact & Clean Home UI:** Redesigned the day counter ring (180dp), scaled down typography and strokes, and refined spacing across the home screen for a balanced, modern look.
- **Sleek Rank Progression Card:** Streamlined the rank milestone badge to a subtle 26dp icon with an inline milestone indicator and low-profile progress bar.
- **Uncluttered Consistency Graph:** Removed the redundant week-start switcher from the consistency heatmap header (available in Settings) and added a total active days indicator.

### Fixed
- **Direct Reset Access:** Placed the Reset Streak button directly above the consistency heatmap and added a Reset action to the top app bar, eliminating the need to scroll down.

---

## [0.7.6] - 2026-09-22

### Added
- **24-Hour Day Cycle Counting:** Habit streaks now count elapsed days based on exact 24-hour cycles from the start timestamp (`startedAt`), rather than incrementing at midnight. Day 0 persists for the full 24 hours until Day 1 is earned.
- **In-Progress Day State in Consistency Graph:** Any day currently in progress before its full 24 hours elapse is marked with an unfilled accent-outlined cell (`IN_PROGRESS`). Tapping shows `● In progress · Day X`.
- **Accurate 24-Hour Reset Length:** Resets now calculate completed streak length using exact 24-hour periods so streaks reflect genuine days maintained.

### Changed
- **Consistency Graph Completion:** Days in the consistency heatmap are only marked completed/maintained (green) once their 24-hour cycle completes.
- **Start Date Synchronization:** Updating start date in Settings now synchronizes the `startedAt` timestamp while preserving the original time of day.

---

## [0.7.5] - 2026-09-22

### Added
- **GitHub-Styled Consistency Graph:** Interactive 24-week activity heatmap on the home screen displaying maintained habit days, resets, and inactivity.
- **First Day of Week Preference:** Option in Settings and Consistency Graph header to configure the first day of the week as Monday or Sunday.
- **Interactive Day Details:** Tap any cell in the consistency graph to view detailed status, streak day number, or reset reason.

---

## [0.7.4] - 2026-09-22

### Added
- **Widget Habit Title Privacy Toggle:** Added a "Show Habit Name" toggle in Widget Settings allowing users to hide habit titles from home screen widgets for privacy and discrete tracking. Live preview in Widget Customization reflects the toggle immediately.
- **Rank Milestone Linear Progress Indicator:** Added a linear progress bar inside the rank card on the home screen showing exact visual progress towards the next rank milestone.

### Changed
- **Standard App Title in Top App Bar:** Standardized the top bar on the home screen to display "Steadfast" instead of the habit title, adhering to standard Android UX design and protecting privacy.
- **Prominent Habit Headline:** The active habit name is cleanly featured in bold headline typography within the main home screen body.
- **Unified Rank Card Container:** Grouped the rank badge, rank name, and next milestone details inside a shape-clipped Material 3 surface card matching the design of the quote card and history summaries.

### Fixed
- **Clean Day Counter:** Removed the clunky Day 0 hint message below the counter ring, ensuring the day counter remains balanced, symmetric, and uncluttered.

---

## [0.7.3] - 2026-09-21

### Fixed
- **Widget Opacity Preset Button Layout:** Fixed a visual bug in Widget Customization settings where the "100% (Solid)" opacity preset button was compressed into a long vertical button due to horizontal layout constraints. Switched preset chips to a responsive `FlowRow` with single-line text constraints so buttons remain correctly proportioned horizontal pills across all screen sizes.

---

## [0.7.2] - 2026-09-21

### Fixed
- **Quote Card Touch Overlay Radius:** Fixed an issue where tapping the quote card to refresh quotes rendered a sharp rectangular click/ripple overlay instead of matching the card's 24dp rounded border radius. Clipped the overlay to `CardShape` and integrated `Card(onClick)` for clean, shape-bounded touch feedback.
- **Consistent Card Ripple:** Ensured history streak cards and interactive cards consistently apply corner-clipped ripple effects.

---

## [0.7.1] - 2026-09-21

### Fixed
- **In-App Update Download & Installation:** Resolved an issue where updating redirected to an external browser and stalled at 100% without prompting installation. App updates are now downloaded directly within Steadfast with live progress tracking.
- **Direct Package Installer Prompt:** App automatically launches the Android package installer via secure `FileProvider` upon download completion.
- **Unknown Sources Permission Support:** Added a guided permission flow for Android's "Install unknown apps" setting with auto-resumption when returning to the app.
- **Update Notification Routing:** Tapping the update notification now directly opens the in-app updater instead of launching external browser downloads.

### Added
- **Browser Download Fallback:** Added a manual fallback option to open release downloads in the browser if desired or in case of network issues.

---

## [0.7.0] - 2026-09-21

### Added
- **Widget Customization Screen:** Introduced a dedicated widget customization screen in Settings with live, real-time widget previews.
- **Background Transparency / Opacity Slider:** Customize widget background opacity from 0% (crystal-clear glass) to 100% (solid) with convenient quick presets.
- **Text & Font Color Options:** Switch between Theme Default (dynamic Material You), Pure White (crisp high contrast for photo/dark wallpapers), Pure Black (high contrast for light wallpapers), and Brand Olive.
- **Background Color Themes:** Choose between Theme Surface, Pure Black (AMOLED dark), Charcoal, and Pure White backgrounds.
- **Simulated Wallpaper Preview:** Test widget transparency and color contrast against simulated dark and light wallpapers directly inside the app before applying to your launcher.

---

## [0.6.0] - 2026-09-21

### Fixed
- **Widget "days" Label Visibility:** Fixed an issue where the "days" label was clipped and hidden on 2×2 and 4×1 home screen widgets due to vertical overflow in Glance RemoteViews. Adjusted text font scaling and container padding so the label is reliably visible across all launcher grid densities.

### Added
- **Dedicated 4×1 Widget Layout:** Added a responsive single-row horizontal layout specifically tailored for 4×1 and 3×1 widget placements, cleanly presenting habit name, streak days, current rank, and progress to next rank.

---

## [0.5.0] - 2026-09-21

### Added
- **Automatic Periodic Update Checks:** App now automatically checks for new GitHub releases in the background at regular intervals (default: Weekly, with Daily and Manual options).
- **Background & Launch Alerts:** Discovered updates are surfaced via system notification and an update dialog on app open.
- **Update Frequency Settings:** Configure automatic check intervals in Settings under About, complete with last checked indicators.

### Fixed
- **Release APK Download Priority:** Resolved an issue where update checker downloaded debug builds; production `*-release.apk` builds are now explicitly prioritized.

---

## [0.4.0] - 2026-09-21

### Changed
- **Home Screen Widget:** Simplified widget layouts to display habit streak day counts cleanly without showing elapsed hours, keeping the widget focused, uncluttered, and lightweight.

---

## [0.3.0] - 2026-09-21

### Added
- **In-App Update Checker:** Check for and download new releases directly from GitHub within the Settings screen.
- **What's New Dialog:** Highlights changes and new features automatically on the first app open after an update, also accessible anytime in Settings.
- **Automated Release Notes in CI:** GitHub Actions automatically extracts release notes from `CHANGELOG.md` and populates the GitHub release description.

---

## [0.2.0] - 2026-09-21

### Added
- **Custom / Migrated Start Date:** Choose a past start date during habit setup or adjust your active habit's start date anytime in Settings without losing streak progress.
- **Data Backup & Restore:** Import previously exported Steadfast CSV files to restore active and past streak history.
- **Milestone Achievement Sharing:** Share your unlocked rank milestones directly to any app with native system sharing.
- **Subtle Micro-Haptics:** Tactile feedback on quote shuffles, elapsed ticker taps, and streak reset confirmations.
- **Live Elapsed Time Ticker:** Real-time hours, minutes, and seconds ticker on Day 0 and beyond.
- **Hourly Quote Rotation & Shuffle:** Periodic motivational quote refreshes and manual instant quote shuffling.

### Changed
- **Edge-to-Edge Layout:** Improved top bar and status bar padding for cleaner navigation bar alignment.
- **Automated CI/CD:** Monotonic version codes and streamlined release builds.

---

## [0.1.0] - 2026-09-21

### Added
- **Single-Purpose Habit Tracker:** 100% offline, privacy-first habit tracker with zero tracking and zero unnecessary permissions.
- **Home Screen Widgets:** Glance-powered Android home screen widgets in circular (1x1) and rounded rectangle layouts.
- **Milestone Ranks Ladder:** Military-inspired progressive rank system from Recruit to Field Marshal with custom rank badges.
- **Daily Reminders:** Scheduled reminder notifications to keep you on track.
- **CSV Data Export:** Export streak history to CSV for external backup and spreadsheets.
- **Material You Design:** Dynamic theme coloring with system, light, and dark mode support.

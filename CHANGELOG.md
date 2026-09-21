# Changelog

All notable changes to Steadfast are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.8.0-alpha.1] - 2026-09-22

### Added
- **Multi-Habit Support:** Track multiple habits simultaneously with independent streaks, ranks, history logs, and reset reasons.
- **Habit Customization:** Choose from 10 category icons (Shield, Flame, Fitness, Book, Water, Moon, Star, Heart, Smile, Block) and distinct Material 3 color themes for each habit.
- **Habit Detail Screen:** Dedicated view per habit featuring circular day streak counters, rank progression milestones, reset bottom sheets, and past reset reason editing.
- **Multi-Habit Home Screen:** View all your active and archived habits in one place with current streaks and rank badges. Includes quick-add FAB and motivational quotes.
- **History & Ranks Filtering:** Filter streak history and rank ladders by specific habits or view aggregate progress across all habits.
- **Multi-Habit Widgets:** Glance home-screen widgets can be configured to display any specific habit. Supports multiple widgets on the home screen tracking different habits simultaneously.
- **Side-by-Side Alpha Installation:** Installs as `Steadfast Alpha` with a distinct package ID (`com.example.steadfast.alpha`) and amber shield icon, allowing concurrent installation alongside the stable release without overwriting user data.
- **Room Migration 1 ➔ 2:** Seamlessly upgrades existing single-habit data into Habit #1 with 100% data preservation.

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

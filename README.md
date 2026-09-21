# Steadfast

**Steadfast** is a small, completely offline and private Android app that does one thing exceptionally well: counts consecutive days of a habit, logs every reset with a kind reflection reason, and rewards progress with military ranks. It includes a responsive home-screen widget built with Jetpack Glance.

---

## Features

- **Streak Tracker:** The day counter is computed dynamically at render time from the start date (never drifted or stored as a ticking integer).
- **Kind, Non-Shaming Resets:** Every reset is logged with an optional reason and suggestion chips. Undo is available via snackbar. A reset starts a new run immediately at day 0.
- **Army Ranks Ladder:** 20 data-driven ranks from Recruit to General of the Army based on streak length. Badge art is drawn geometrically in Compose. Shows progress to next rank and preserves the all-time highest rank achieved.
- **Motivational Quotes:** Deterministic daily quote selection from an offline pool of 60+ curated original quotes. Automatically switches to a "comeback" pool for 24 hours following a reset.
- **Glance Home-Screen Widget:** Highly customizable and responsive across 1×1 (compact mini-counter), 2×2 (standard counter), 4×1 (streamlined horizontal bar), and 4×2 (wide counter with rank progress). Supports both **Rounded Rectangle** and **Circular** shapes, selectable via in-app Settings or directly from the launcher widget picker. Fully supports dynamic color (Android 12+) with brand fallback.
- **Privacy First:** No accounts, no analytics, no ads, no cloud sync, and **no `INTERNET` permission**. Full Android Auto Backup for local Room database and DataStore settings.
- **Customizable:** System/Light/Dark theme, rounded/circular widget shape, dynamic wallpaper colors toggle, optional gentle evening check-in notification, and CSV export.

---

## Tech Stack

- **Platform:** Kotlin 2.1 (K2), minSdk 26, compileSdk/targetSdk 35
- **UI:** Jetpack Compose, Material 3, Navigation Compose
- **Widget:** Jetpack Glance (`glance-appwidget`, `glance-material3`)
- **Storage:** Room 2.6 with KSP + DataStore Preferences
- **Background:** WorkManager (midnight widget refresh, optional reminder)
- **Architecture:** Unidirectional Data Flow (UDF), MVVM, pure domain logic (`StreakCalculator`, `RankLadder`)
- **Typography:** Barlow Condensed & Manrope (bundled offline with SIL OFL licenses)

---

## How to Build and Run

### Prerequisites
- JDK 21 (configured in `gradle.properties` or via `JAVA_HOME`)
- Android SDK Platform API 35 and Build-Tools 35.0.0

### Build Commands

```bash
# Compile and package debug APK
./gradlew assembleDebug

# Run all domain and data unit tests
./gradlew testDebugUnitTest

# Run Android Lint checks
./gradlew lintDebug

# Build optimized release APK with R8 and resource shrinking
./gradlew assembleRelease
```

The generated APKs will be located at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`
- App Bundle: `app/build/outputs/bundle/release/app-release.aab`

---

## Automated CI/CD & Releases (GitHub Actions)

Steadfast includes automated GitHub Actions workflows:

### 1. Pull Request & Commit Verification (`.github/workflows/ci.yml`)
- Automatically runs on every push and pull request to `master`/`main`.
- Validates code style and rules with `./gradlew lintDebug`.
- Executes all unit tests with `./gradlew testDebugUnitTest`.
- Builds and uploads the debug APK as an artifact for quick testing.

### 2. Automated Semantic Releases (`.github/workflows/release.yml`)
Releases are triggered automatically by pushing any semantic version tag:

```bash
# Option A: Using the release helper script (verifies, tests, tags, and pushes)
./scripts/release.sh 0.7.2

# Option B: Using standard git commands
git tag -a v0.7.2 -m "Release v0.7.2"
git push origin v0.7.2
```

Or manually triggered in GitHub Actions UI: **Actions → Release → Run workflow** (enter version name, e.g. `0.7.2`).

**What the pipeline produces on each release:**
- `steadfast-v0.7.2-release.apk`: Production-ready, R8-minified, and resource-shrunk APK for end-user installation.
- `steadfast-v0.7.2-debug.apk`: Debug APK with logging and developer inspection enabled.
- `steadfast-v0.7.2-release.aab`: Android App Bundle for Google Play distribution.
- `checksums.txt` and `.sha256`: SHA-256 cryptographic verification checksums.
- Automatic GitHub Release notes with commit changelog.

#### Optional: Setting up Production Release Signing Secrets
In your GitHub Repository **Settings → Secrets and variables → Actions**, add:
- `KEYSTORE_BASE64`: `base64 -w 0 your-release-key.jks`
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Key alias
- `KEY_PASSWORD`: Key password
*(If omitted, builds safely fallback to debug signing).*
---

## Customization Guide

### Changing the App Name and Package
1. **App Name:** Open `app/src/main/res/values/strings.xml` and update the `<string name="app_name">Steadfast</string>` value.
2. **Package / Application ID:** Open `app/build.gradle.kts` and update `namespace` and `defaultConfig.applicationId` to your desired package name.

### Configuring Ranks (`RankLadder.kt`)
The ranks ladder is completely data-driven in [`domain/RankLadder.kt`](app/src/main/java/com/example/steadfast/domain/RankLadder.kt):
```kotlin
val ranks = listOf(
    Rank(0, R.string.rank_recruit, 0, "start", RankTier.ENLISTED),
    Rank(1, R.string.rank_private, 7, "1 week", RankTier.ENLISTED),
    Rank(2, R.string.rank_private_first_class, 30, "1 month", RankTier.ENLISTED),
    ...
)
```
To adjust thresholds, add ranks, or change the time descriptions, simply modify this list. The UI, badge rendering, and progress calculations adapt automatically.

### Adding Quotes
Quotes are stored in JSON format at [`app/src/main/res/raw/quotes.json`](app/src/main/res/raw/quotes.json):
```json
[
  {
    "text": "Small days stack into big streaks.",
    "author": null,
    "type": "general"
  },
  {
    "text": "You do not need perfect. You need again.",
    "author": null,
    "type": "comeback"
  }
]
```
- Set `type` to `"general"` for standard daily quotes or `"comeback"` for the 24-hour post-reset pool.
- Quotes should stay under ~90 characters to ensure clean two-line rendering.
- Leave `author` as `null` for anonymous or original aphorisms.

---

## OEM Battery Note (Widget Refresh)

Steadfast relies on a three-tier architecture to keep the widget accurate without draining battery:
1. **Render-Time Computation:** The widget calculates the day count directly from `startDate` and the current date on each render.
2. **Event Refreshes:** The widget refreshes instantly upon starting a streak, resetting, undoing, renaming, and whenever the app is launched.
3. **Background Sync:** `MidnightUpdateWorker` (via WorkManager) and `DateChangeReceiver` trigger refreshes at local midnight and on timezone/date adjustments.

> [!NOTE]
> Aggressive OEM battery-optimization software (found on devices from Samsung, Xiaomi, Huawei, etc.) may delay or batch background WorkManager jobs. If background execution is delayed by the OS, opening the app refreshes the widget immediately. Users experiencing widget delays can exempt Steadfast from battery optimization in device system settings.

---

## License

Steadfast is open-source software. Bundled typefaces:
- **Barlow Condensed:** Designed by Jeremy Tribby (SIL Open Font License 1.1)
- **Manrope:** Designed by Mikhail Sharanda (SIL Open Font License 1.1)
Full license texts are bundled in `app/src/main/assets/licenses/`.

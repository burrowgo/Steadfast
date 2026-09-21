<p align="center">
  <img src="assets/icon.png" width="128" height="128" alt="Steadfast App Icon" />
</p>

<h1 align="center">Steadfast</h1>

<p align="center">
  <strong>A mindful, privacy-first Android habit tracker with military rank progression, curated daily quotes, and customizable Jetpack Glance home screen widgets.</strong>
</p>

<p align="center">
  <a href="https://github.com/burrowgo/Steadfast/releases/latest">
    <img src="assets/badge_github.png" alt="Download on GitHub" height="50" />
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.example.steadfast%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fburrowgo%2FSteadfast%22%2C%22author%22%3A%22burrowgo%22%2C%22name%22%3A%22Steadfast%22%7D">
    <img src="assets/badge_obtainium.png" alt="Get it on Obtainium" height="50" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/burrowgo/Steadfast/releases/latest">
    <img src="https://img.shields.io/github/v/release/burrowgo/Steadfast?style=flat-square&label=Release&color=4C662B" alt="Latest Release" />
  </a>
  <a href="https://github.com/burrowgo/Steadfast/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="License: MIT" />
  </a>
  <a href="https://android-arsenal.com/api?level=26">
    <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-brightgreen.svg?style=flat-square&logo=android&logoColor=white" alt="API 26+" />
  </a>
  <img src="https://img.shields.io/badge/Offline--First-100%25-success?style=flat-square" alt="100% Offline-First" />
</p>

---

## 📖 Overview

**Steadfast** is an offline-first habit tracker designed with focus and intention. It tracks consecutive days of an active habit, encourages compassionate reflection during resets, and celebrates perseverance through military ranks and curated aphorisms.

With full Jetpack Glance widget support, you can place responsive, glanceable habit counters on your home screen and customize their transparency, text colors, and shapes directly from within the app.

---

## 🚀 Download & Installation

<p align="center">
  <a href="https://github.com/burrowgo/Steadfast/releases/latest">
    <img src="assets/badge_github.png" alt="Download on GitHub" height="55" />
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.example.steadfast%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fburrowgo%2FSteadfast%22%2C%22author%22%3A%22burrowgo%22%2C%22name%22%3A%22Steadfast%22%7D">
    <img src="assets/badge_obtainium.png" alt="Get it on Obtainium" height="55" />
  </a>
</p>

### 1. Obtainium (Recommended for Seamless Updates)

Track and receive automatic background updates directly through [Obtainium](https://github.com/ImranR98/Obtainium):

<p align="center">
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.example.steadfast%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fburrowgo%2FSteadfast%22%2C%22author%22%3A%22burrowgo%22%2C%22name%22%3A%22Steadfast%22%7D">
    <img src="assets/badge_obtainium.png" alt="Add to Obtainium" height="48" />
  </a>
</p>

- **One-Click Install:** Tap the badge above on your Android device to automatically import and track Steadfast.
- **Manual App Source:** Add `https://github.com/burrowgo/Steadfast` in the Obtainium app.

### 2. GitHub Releases (Direct APK)

Download pre-compiled binaries directly from [GitHub Releases](https://github.com/burrowgo/Steadfast/releases/latest):

| Package | Description |
| :--- | :--- |
| **`steadfast-v*-release.apk`** | Production build with R8 optimization, minification, and resource shrinking *(recommended)* |
| **`steadfast-v*-debug.apk`** | Developer build with debuggable logging enabled |
| **`steadfast-v*-release.aab`** | Android App Bundle for distribution |

### 3. In-App Updates

Steadfast features a built-in update manager in **Settings → About → Check for updates**:
- **Automatic Checks:** Periodically checks for GitHub releases in the background (Weekly, Daily, or Manual).
- **Direct In-App Download:** Streams release APK updates with a live download progress bar.
- **Native Installation:** Directly launches the system package installer sheet via `FileProvider`.

---

## ✨ Features

- 🎯 **Accurate, Drift-Free Day Counter:** The streak is computed dynamically at render time from your habit's start date—never drifted by skipped alarms or stored as an unstable ticking integer.
- ⏱️ **Day 0 Live Ticker:** Real-time hours, minutes, and seconds counter during your first day to motivate early momentum until midnight unlocks Day 1.
- 🤝 **Kind, Non-Shaming Resets:** Streak resets are approached constructively. Log optional reflection reasons with quick suggestion chips ("Stress", "Tired", "Social", "Boredom") and keep a complete historical record.
- 🎖️ **20-Tier Military Ranks Ladder:** Progress from *Recruit* (Day 0) to *General of the Army* (30 Years) across Enlisted, Officer, and General tiers. Art is geometrically rendered with celebratory unlock dialogs and achievement sharing.
- 💬 **Curated Motivational Quotes:** Curated library of 60+ original aphorisms. Deterministic rotation provides one thought-provoking quote each day, with a dedicated "comeback" pool for 24 hours following a reset. Shuffle quotes on tap with subtle haptics.
- 📱 **Customizable Glance Home-Screen Widgets:**
  - **2×2 Standard Grid:** Full habit counter, rank badge, and motivational quote.
  - **4×1 Horizontal Bar:** Compact single-row layout displaying habit streak, rank badge, and progress toward next milestone.
  - **1×1 Circular / Square Badge:** Minimalist glanceable counter.
  - **In-App Customization:** Live preview with adjustable background opacity (0% crystal glass to 100% solid), custom text colors (Theme Default, Pure White, Pure Black, Brand Olive), and background themes (AMOLED Black, Charcoal, Surface, Pure White).
- 🔒 **100% Privacy & Offline-First:** No accounts, no ads, no analytics, and no tracking. All habit data remains on your device in a local Room database. The `INTERNET` permission is used strictly to query GitHub Releases and download updates.
- 💾 **Data Ownership (Export & Import):** Export your complete streak and reset history to standard CSV at any time, or restore from previous Steadfast CSV backups.
- 🔔 **Gentle Daily Reminders:** Optional scheduled evening check-in notification via WorkManager.

---

## 🎖️ Rank Progression Ladder

| Rank | Required Streak | Tier | Milestone |
| :--- | :---: | :---: | :--- |
| **Recruit** | 0 Days | Enlisted | Journey begins |
| **Private** | 7 Days | Enlisted | 1 Week |
| **Private First Class** | 30 Days | Enlisted | 1 Month |
| **Corporal** | 60 Days | Enlisted | 2 Months |
| **Sergeant** | 90 Days | Enlisted | 3 Months |
| **Staff Sergeant** | 120 Days | Enlisted | 4 Months |
| **Sergeant First Class** | 180 Days | Enlisted | 6 Months |
| **Master Sergeant** | 270 Days | Enlisted | 9 Months |
| **Sergeant Major** | 365 Days | Enlisted | 1 Year |
| **Second Lieutenant** | 545 Days | Officer | 18 Months |
| **First Lieutenant** | 730 Days | Officer | 2 Years |
| **Captain** | 1,095 Days | Officer | 3 Years |
| **Major** | 1,460 Days | Officer | 4 Years |
| **Lieutenant Colonel** | 1,825 Days | Officer | 5 Years |
| **Colonel** | 2,555 Days | Officer | 7 Years |
| **Brigadier General** | 3,650 Days | General | 10 Years |
| **Major General** | 5,475 Days | General | 15 Years |
| **Lieutenant General** | 7,300 Days | General | 20 Years |
| **General** | 9,125 Days | General | 25 Years |
| **General of the Army** | 10,950 Days | General | 30 Years |

---

## 🛠️ Tech Stack & Architecture

- **Language & Runtime:** Kotlin 2.1 (K2 Mode), minSdk 26, targetSdk / compileSdk 35
- **UI Framework:** Jetpack Compose with Material Design 3 and Dynamic Color
- **Home Screen Widgets:** Jetpack Glance (`androidx.glance`, `androidx.glance.appwidget`, `androidx.glance.material3`)
- **Architecture:** MVVM with Unidirectional Data Flow (UDF) & Clean Architecture
- **Local Persistence:** Room Database 2.6 (KSP) + Jetpack DataStore Preferences
- **Background Tasks:** WorkManager 2.10 (midnight date rollover & reminder notifications)
- **Typography:** Barlow Condensed & Manrope (bundled offline with SIL OFL licenses)
- **Package Installer:** Android `FileProvider` with in-app APK streaming downloader and deep-linked unknown-source permission handling

---

## 🔨 Building from Source

### Prerequisites
- JDK 21 (Temurin, OpenJDK, or Android Studio bundled JDK)
- Android SDK with Platform 35 and Build-Tools 35.0.0

### Build Commands
```bash
# Clone the repository
git clone https://github.com/burrowgo/Steadfast.git
cd Steadfast

# Run unit tests
./gradlew testDebugUnitTest

# Run Android Lint checks
./gradlew lintDebug

# Build release APK (R8-minified and resource-shrunk)
./gradlew assembleRelease
```

Generated builds are located in:
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

---

## 🔄 CI/CD & Semantic Releases

Releases are automated using GitHub Actions (`.github/workflows/release.yml`):
- To create a release, tag the commit:
  ```bash
  ./scripts/release.sh 0.7.3
  # or
  git tag -a v0.7.3 -m "Release v0.7.3" && git push origin v0.7.3
  ```
- The pipeline verifies code with lint and unit tests, generates production APKs (`release` and `debug`), builds the AAB bundle, computes SHA-256 checksums, and publishes GitHub release notes.

---

## 🔋 OEM Battery Optimization Note

Android manufacturers (Samsung, Xiaomi, OnePlus, Huawei) frequently employ aggressive battery managers that can delay background `WorkManager` jobs.

Steadfast mitigates this via a 3-pillar strategy:
1. **Dynamic Render Computation:** Day counts calculate from start date on every render.
2. **Event-Driven Updates:** Instant widget refreshes upon app open, reset, undo, and settings change.
3. **Midnight Alarms:** Midnight rollover workers and date change broadcast receivers.

If widget updates are delayed by your device, exempt Steadfast from battery restrictions in your device settings (**Settings → Apps → Steadfast → Battery → Unrestricted**).

---

## 📄 License & Credits

Steadfast is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

### Font Attributions
- **Barlow Condensed:** Designed by Jeremy Tribby ([SIL Open Font License 1.1](app/src/main/assets/licenses/BARLOW_OFL.txt))
- **Manrope:** Designed by Mikhail Sharanda ([SIL Open Font License 1.1](app/src/main/assets/licenses/MANROPE_OFL.txt))

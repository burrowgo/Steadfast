<p align="center">
  <img src="assets/icon.png" width="128" height="128" alt="Steadfast App Icon" />
</p>

<h1 align="center">Steadfast</h1>

<p align="center">
  <strong>A mindful, privacy-first Android habit tracker with military rank progression, curated daily quotes, and customizable Jetpack Glance home screen widgets.</strong>
</p>

<p align="center">
  <a href="https://github.com/burrowgo/Steadfast/releases/latest">
    <img src="assets/badge_github.png" alt="Download on GitHub" height="48" />
  </a>
  &nbsp;&nbsp;
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.example.steadfast%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fburrowgo%2FSteadfast%22%2C%22author%22%3A%22burrowgo%22%2C%22name%22%3A%22Steadfast%22%7D">
    <img src="assets/badge_obtainium.png" alt="Get it on Obtainium" height="48" />
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
  <a href="https://github.com/burrowgo/Steadfast/issues">
    <img src="https://img.shields.io/github/issues/burrowgo/Steadfast?style=flat-square&color=orange" alt="Issues" />
  </a>
</p>

<p align="center">
  <a href="#-overview">Overview</a> •
  <a href="#-features">Features</a> •
  <a href="#-home-screen-widgets">Widgets</a> •
  <a href="#-tech-stack--architecture">Tech Stack</a> •
  <a href="#-building-from-source">Building</a> •
  <a href="#-license--credits">License</a>
</p>

---

## 📖 Overview

**Steadfast** is an offline-first habit tracker designed with focus, intention, and longevity in mind. It calculates consecutive days of an active habit directly from your start timestamp—ensuring drift-free tracking that never desynchronizes.

Resets are approached constructively with non-shaming reflections, while consistency is celebrated through a 20-tier military rank progression system and curated daily aphorisms. With Jetpack Glance widgets, your habit progress stays visible on your home screen with custom transparency and color theming.

| 🔒 Privacy First | ⚡ Drift-Free Engine | 📱 Jetpack Glance | 🔄 Built-In Updates |
| :---: | :---: | :---: | :---: |
| 100% local Room DB. No accounts, ads, or analytics. | Dynamically computed from start date. No ticking counters. | 2×2, 4×1 & 1×1 widgets with opacity and color customization. | Direct in-app APK downloads & Obtainium deep linking. |

---

## ✨ Features

- 🎯 **Accurate, Drift-Free Day Counter:** The streak is computed dynamically at render time from your habit's start date—never drifted by skipped alarms, time changes, or stored as an unstable ticking integer.
- ⏱️ **Day 0 Live Ticker:** Real-time hours, minutes, and seconds counter during your first day to motivate early momentum until midnight unlocks Day 1.
- 🤝 **Kind, Non-Shaming Resets:** Streak resets are approached constructively. Log optional reflection reasons with quick suggestion chips ("Stress", "Tired", "Social", "Boredom") and keep a complete historical record.
- 🎖️ **20-Tier Military Ranks Ladder:** Progress from *Recruit* (Day 0) to *General of the Army* (30 Years) across Enlisted, Officer, and General tiers with celebratory unlock dialogs and achievement card sharing.
- 💬 **Curated Motivational Quotes:** Curated library of 60+ original aphorisms. Deterministic rotation provides one thought-provoking quote each day, with a dedicated "comeback" pool for 24 hours following a reset. Shuffle quotes on tap with subtle haptic feedback.
- 📱 **Customizable Glance Home-Screen Widgets:** Place glanceable habit counters on your home screen with adjustable opacity (0% crystal glass to 100% solid), custom text colors, and background preview themes.
- 🔄 **In-App Updater & Background Checks:** Built-in update manager that automatically checks GitHub Releases (weekly, daily, or manual), streams APK updates with real-time download progress, and prompts native package installation directly within the app.
- 🔒 **100% Privacy & Offline-First:** No accounts, no ads, no analytics, and no tracking. All habit data remains on your device in a local Room database. The `INTERNET` permission is used strictly to query GitHub Releases and download updates.
- 💾 **Data Ownership (Export & Import):** Export your complete streak and reset history to standard CSV at any time, or restore from previous Steadfast CSV backups.
- 🔔 **Gentle Daily Reminders:** Optional scheduled evening check-in notification via WorkManager.

---

## 📱 Home Screen Widgets

Steadfast provides three native Android widgets powered by Jetpack Glance:

| Widget | Size | Contents |
| :--- | :---: | :--- |
| **Standard Counter** | `2×2` | Habit streak days, current military rank badge, and daily motivational quote |
| **Progress Bar** | `4×1` | Single-row bar with streak count, rank badge, and visual progress toward next promotion |
| **Minimal Badge** | `1×1` | Ultra-compact glanceable streak counter |

### Widget Customization
Customize all widgets directly in **Settings → Widget Customization**:
- **Background Opacity:** Continuous slider or quick presets: `0% (Glass)`, `25%`, `50%`, `75%`, `100% (Solid)`.
- **Text & Accent Colors:** Choose from *Theme Default*, *Pure White*, *Pure Black*, or *Brand Olive*.
- **Privacy & Stealth Tracking:** Toggle *Show Habit Title* off to hide your habit's name on your home screen for discreet habit building.
- **Live Wallpaper Preview:** Test your widget styling against *AMOLED Black*, *Charcoal*, *Material Surface*, and *Pure White* backgrounds.

---

## 🛠️ Tech Stack & Architecture

- **Language & Tooling:** Kotlin 2.1 (K2 Mode), Gradle 8.11, minSdk 26, targetSdk / compileSdk 35
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
- JDK 21 (Eclipse Temurin, OpenJDK, or Android Studio bundled JDK)
- Android SDK with Platform 35 and Build-Tools 35.0.0

### Build Commands
```bash
# Clone the repository
git clone https://github.com/burrowgo/Steadfast.git
cd Steadfast

# Run unit tests and Android Lint
./gradlew testDebugUnitTest lintDebug

# Build production APK (R8-minified and resource-shrunk)
./gradlew assembleRelease

# Install directly to connected device
adb install app/build/outputs/apk/release/app-release.apk
```

Output binaries are generated at:
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

---

## 🔄 CI/CD & Semantic Releases

Releases are automated via GitHub Actions (`.github/workflows/release.yml`):
```bash
# Create and push release tag
./scripts/release.sh 0.7.6
# or
git tag -a v0.7.6 -m "Release v0.7.6" && git push origin v0.7.6
```
The automated CI pipeline runs unit tests and lint checks, builds production and debug APKs and the AAB bundle, generates SHA-256 checksums, and publishes release assets directly to GitHub Releases.

---

## 🔋 OEM Battery Optimization Note

Android manufacturers (Samsung, Xiaomi, OnePlus, Huawei) frequently apply aggressive battery management that can delay background `WorkManager` jobs.

Steadfast mitigates this via a 3-pillar strategy:
1. **Dynamic Render Computation:** Day counts calculate from start date on every render.
2. **Event-Driven Updates:** Instant widget refreshes upon app open, reset, undo, and settings change.
3. **Midnight Alarms:** Midnight rollover workers and date change broadcast receivers.

If widget updates are delayed on your device, exempt Steadfast from battery restrictions (**Settings → Apps → Steadfast → Battery → Unrestricted**).

---

## 📄 License & Credits

Steadfast is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

### Font Attributions
- **Barlow Condensed:** Designed by Jeremy Tribby ([SIL Open Font License 1.1](app/src/main/assets/licenses/BARLOW_OFL.txt))
- **Manrope:** Designed by Mikhail Sharanda ([SIL Open Font License 1.1](app/src/main/assets/licenses/MANROPE_OFL.txt))

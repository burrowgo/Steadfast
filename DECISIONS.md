# Decisions Log — Steadfast

This file records architecture and tooling choices not explicitly settled by `AGENT.md`.

## 1. Local Unit Tests with Robolectric
- **Decision:** Added `org.robolectric:robolectric:4.14.1` and `androidx.test:core-ktx:1.6.1` to `testImplementation`.
- **Rationale:** `AGENT.md` Section 11 specifies: *"Use an in-memory Room database and an injected Clock in tests"* for repository unit tests. In-memory Room requires an Android `Context`, which Robolectric provides for local JVM unit test execution (`./gradlew testDebugUnitTest`) without needing an emulator or physical device.

## 2. Compose BOM 2025.02.00
- **Decision:** Used Compose BOM `2025.02.00` (Compose 1.7.8).
- **Rationale:** Compose BOM `2026.09.00` required unreleased `compileSdk = 37` and AGP 9.1+, whereas `compileSdk = 35` is the latest stable release target specified in `AGENT.md`.

## 3. Font Loading Strategy
- **Decision:** Bundled official Google Fonts `barlow_condensed_bold.ttf`, `barlow_condensed_semibold.ttf`, and variable `manrope.ttf` into `res/font/`, and bundled license texts into `assets/licenses/`.
- **Rationale:** Meets Section 9.3 offline bundling requirement while remaining fully OFL licensed and self-contained.

## 4. Release Signing Fallback
- **Decision:** Configured release build to use debug signing configuration as fallback when custom signing credentials are not supplied in `local.properties`.
- **Rationale:** Enables `./gradlew assembleRelease` to succeed immediately out-of-the-box for verification without requiring a local production keystore.

## 5. Midnight Worker Grace Buffer
- **Decision:** Scheduled `MidnightUpdateWorker` for 00:01 local time (1 minute after midnight) instead of 00:00:00.
- **Rationale:** Ensures device clock and time zone transitions have settled past midnight before recalculating day counts.

## 6. Widget Shapes and 1×1 Size Support
- **Decision:** Added a 1×1 (`TINY_SIZE`, 50×50dp) responsive size bucket to `SteadfastWidget`, added a user-selectable `WidgetShape` (Rounded Rectangle vs. Circle) in Settings, and provided a dedicated `SteadfastCircleWidget` provider so circular widgets can also be placed directly from the launcher widget picker.
- **Rationale:** Gives users flexibility to customize home-screen aesthetics between rounded square cards and circular dials across 1×1, 2×2, and 4×2 sizes while keeping the codebase unified via an inherited Glance widget implementation.


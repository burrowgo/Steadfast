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

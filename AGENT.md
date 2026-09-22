# AGENT.md — Steadfast (working title)

A small, offline Android app that does one thing well: **count consecutive days of a habit, log every reset with a reason, and reward progress with army ranks.** It ships with a home-screen widget showing the current day count.

Read this whole file before writing code. Every decision that could stall you is already made below. Do not ask the user about choices this file settles; if something is genuinely missing, pick the simplest option that fits the principles, and record it in `DECISIONS.md`.

---

## 1. Product principles

1. **One habit, one number.** The day counter is the hero of the app. Everything else supports it.
2. **Simple beats featureful.** If a feature isn't in Section 2, do not build it.
3. **Kind, not shaming.** A reset is data, not failure. Copy and visuals after a reset stay encouraging.
4. **Fully offline and private.** No accounts, no analytics, no ads, no network. Do **not** declare the `INTERNET` permission.
5. **Native and polished.** Material 3, dynamic color, edge-to-edge, smooth motion, proper dark mode, accessible.

---

## 2. Scope

### In scope
- **Streak tracker:** user names the habit and starts the counter. On failure they reset it and give a reason. History of all past runs is kept with reasons.
- **Ranks:** army-style ranks earned by current streak length (Section 5.2), with progress toward the next rank, a full ranks screen, and a "highest rank achieved" record.
- **Motivational quotes** shown at the bottom of the home screen.
- **Widget:** shows current day count (and rank on the wider size).
- **Small extras (already approved):** undo after reset, edit a past reset reason, optional daily reminder (off by default), CSV export of history, light/dark/system theme, Android Auto Backup.

### Out of scope (do NOT build)
Multiple simultaneous habits, accounts or cloud sync, social/sharing/leaderboards, ads, analytics or crash-reporting SDKs, in-app purchases, calendars/heatmaps, achievements beyond ranks, AI features, resetting from the widget, import from file.

---

## 3. Tech stack and constraints

| Area | Decision |
|---|---|
| Platform | Native Android, **Kotlin** (latest stable, K2) |
| UI | **Jetpack Compose** + **Material 3** (`androidx.compose.material3`). Use Material 3 Expressive components/motion only if they are in a **stable** release; never depend on alpha/beta/RC artifacts |
| Widget | **Jetpack Glance** (`glance-appwidget`, `glance-material3`) |
| Storage | **Room** (streak history) + **DataStore Preferences** (settings). KSP for Room |
| Async | Kotlin Coroutines + Flow |
| Background | **WorkManager** (midnight widget refresh, optional reminder) |
| DI | None or manual: a tiny `AppContainer` on the `Application` class. No Hilt/Koin |
| Navigation | Navigation Compose (type-safe routes) |
| Architecture | Single module, MVVM + unidirectional data flow (immutable `UiState`, `ViewModel`, repository) |
| SDK | `minSdk 26`, `compileSdk`/`targetSdk` = latest stable API level |
| Build | Gradle Kotlin DSL, version catalog at `gradle/libs.versions.toml`, Gradle wrapper committed |
| Release | R8 minify + resource shrinking on `release`; signing config left as a placeholder read from `local.properties` / env |

**Versions:** look up the latest *stable* AGP, Kotlin, Compose BOM, Room, Glance, WorkManager, DataStore and Navigation at build time instead of guessing. If you have no network access, use versions you are confident are stable and note it in `README.md`.

**Working title/package:** app name `Steadfast`, package `com.example.steadfast`. Keep the name in one string resource and the package in one place so the user can rename easily.

---

## 4. Project structure

```
app/src/main/java/com/example/steadfast/
  SteadfastApp.kt              // Application, holds AppContainer
  MainActivity.kt              // enableEdgeToEdge(), sets content
  data/
    db/  (StreakEntity, StreakDao, AppDatabase)
    prefs/ (SettingsRepository — DataStore)
    StreakRepository.kt
  domain/
    StreakCalculator.kt        // pure functions, injectable Clock
    Rank.kt, RankLadder.kt     // data-driven rank table
    Quote.kt, QuoteRepository.kt
  ui/
    theme/ (Color.kt, Type.kt, Shape.kt, Theme.kt)
    components/ (DayCounter, RankBadge, QuoteCard, ResetSheet, EmptyState)
    home/ (HomeScreen, HomeViewModel)
    ranks/ (RanksScreen, RanksViewModel)
    history/ (HistoryScreen, HistoryViewModel, EditReasonDialog)
    settings/ (SettingsScreen, SettingsViewModel)
    nav/ (AppNavGraph)
  widget/
    SteadfastWidget.kt         // GlanceAppWidget
    SteadfastWidgetReceiver.kt // GlanceAppWidgetReceiver
    WidgetUpdater.kt           // updateAll + schedules midnight work
    MidnightUpdateWorker.kt
    DateChangeReceiver.kt
  notifications/ (ReminderWorker, NotificationHelper)
app/src/main/res/ (font/, raw/quotes.json, values/, drawable/, xml/ widget info + backup rules)
app/src/test/  (unit tests)   app/src/androidTest/ (few UI tests)
README.md   DECISIONS.md
```

---

## 5. Domain rules (get these exactly right)

### 5.1 Counting days — 24-hour completion rule
Store the **started timestamp** (`startedAt`). Compute completed days whenever you render based on completed 24-hour cycles. A day passes when 24 hours complete from the start time, rather than incrementing at midnight.

```kotlin
// StreakCalculator.kt (pure, unit-tested)
fun streakDays(startedAtMillis: Long, nowMillis: Long): Int {
    if (startedAtMillis <= 0L || nowMillis <= startedAtMillis) return 0
    return ((nowMillis - startedAtMillis) / (24 * 60 * 60 * 1000L)).toInt()
}
```

- A day only increments and marks as maintained/completed when full 24 hours have elapsed.
- Starting today shows **0 days**. Show a friendly subtitle on day 0: "Day one. Make it to tomorrow."
- In the consistency graph, the current/uncompleted 24-hour period is marked `IN_PROGRESS` (unfilled) and not marked done (`MAINTAINED`) until 24 hours have completed.
- Length of a finished run = `streakDays(startedAt, endedAt)`.

### 5.2 Rank ladder (data-driven, in `RankLadder.kt`)
Rank = highest row where `minDays <= currentStreakDays`. Ranks follow the current streak, so a reset drops the rank back to Recruit. The app separately remembers the **highest rank ever achieved** (derived from the longest streak across current + history).

| # | Rank | Min days | ≈ Time |
|---|---|---|---|
| 0 | Recruit | 0 | start |
| 1 | Private | 7 | 1 week |
| 2 | Private First Class | 30 | 1 month |
| 3 | Corporal | 60 | 2 months |
| 4 | Sergeant | 90 | 3 months |
| 5 | Staff Sergeant | 120 | 4 months |
| 6 | Sergeant First Class | 180 | 6 months |
| 7 | Master Sergeant | 270 | 9 months |
| 8 | Sergeant Major | 365 | 1 year |
| 9 | Second Lieutenant | 545 | 18 months |
| 10 | First Lieutenant | 730 | 2 years |
| 11 | Captain | 1095 | 3 years |
| 12 | Major | 1460 | 4 years |
| 13 | Lieutenant Colonel | 1825 | 5 years |
| 14 | Colonel | 2555 | 7 years |
| 15 | Brigadier General | 3650 | 10 years |
| 16 | Major General | 5475 | 15 years |
| 17 | Lieutenant General | 7300 | 20 years |
| 18 | General | 9125 | 25 years |
| 19 | General of the Army | 10950 | 30 years |

- Provide `currentRank`, `nextRank?`, `daysToNextRank`, and `progressToNext: Float (0..1)` from `RankLadder`.
- Rank names are string resources.
- **Badge art:** draw simple, original geometric badges in Compose (`RankBadge`): chevrons for enlisted ranks (more chevrons and a rocker as rank rises), bars for lieutenants/captain, a diamond/leaf shape for Major/Lt Col, an eagle-like simple shape for Colonel, and 1–5 stars for generals. Do not download or embed official insignia artwork. Recruit uses an empty outlined shield.
- **Rank-up celebration:** store `lastCelebratedRankIndex` in DataStore. When the app opens and the current rank index is higher, show a one-time celebratory dialog/animation with haptic feedback, then update the stored value. On reset, set it back to 0.

### 5.3 Reset behavior
1. User taps **Reset** → a modal bottom sheet appears (Section 7.1).
2. On confirm: the active run is closed (`endedAt = now`, `endDate = today`, `lengthDays`, `reason`), and a **new run starts immediately at 0 days today**. This keeps the counter always running, which is friendlier than an idle state.
3. Show a snackbar: "Streak reset. Fresh start." with an **Undo** action for ~10 seconds. Undo deletes the closed row's end data and restores it as the active run (do it in one Room transaction).
4. Update the widget after every change (Section 8.3).
5. After a reset, the quote card draws from the "comeback" quote pool for 24 hours (Section 5.4).

### 5.4 Quotes
- Local file `res/raw/quotes.json`: `[{ "text": "...", "author": "..." | null, "type": "general" | "comeback" }]`.
- Ship **at least 60** quotes (about 45 general, 15 comeback). Keep each under ~90 characters so it fits two lines.
- **Do not invent attributions.** Write original one-liners about persistence, small steps, streaks and fresh starts, and leave `author` null. Only attribute a quote to a person if it is well documented and you are sure (many popular quotes are misattributed; when in doubt, use an original line). Don't reproduce long or copyrighted passages.
- Tone examples (original): "Small days stack into big streaks." / "You don't need perfect. You need again." (comeback) / "A slip is a data point, not a verdict." (comeback)
- Selection: deterministic per day for a given pool (`dayOfYear % pool.size`), so it doesn't flicker on recomposition. **Tapping** the card moves to the next quote with a crossfade. No auto-rotation.

---

## 6. Data model

**Room entity `streak`** (one row per run):

| Column | Type | Notes |
|---|---|---|
| `id` | Long PK, autogenerate | |
| `habitName` | String | Snapshot of the habit name at run start |
| `startDate` | Long | `LocalDate.toEpochDay()` (local zone) — source of truth for counting |
| `startedAt` | Long | epoch millis, informational |
| `endedAt` | Long? | null = **active run** (there must be at most one) |
| `endDate` | Long? | epoch day of reset |
| `lengthDays` | Int? | computed on reset |
| `reason` | String? | user text, ≤ 200 chars; null/blank shown as "No reason given" |

- Provide `TypeConverter`s or store primitives directly (preferred: primitives + mapping in the repository).
- DAO exposes `Flow<StreakEntity?> activeStreak()`, `Flow<List<StreakEntity>> history()` (ended, newest first), and transactional `startNew`, `resetActive(reason)`, `undoLastReset()`, `updateReason(id, reason)`.
- Enforce "at most one active run" in the repository (single transaction), and write a test for it.
- Database name/version 1, export schema, and add a migration placeholder folder for the future.

**DataStore keys:** `habitName`, `themeMode` (system/light/dark), `useDynamicColor` (default true), `reminderEnabled` (default false), `reminderTime` (default 20:00), `lastCelebratedRankIndex`.

---

## 7. Screens and UX

Navigation: bottom **NavigationBar** with 3 tabs — **Home**, **Ranks**, **History**. **Settings** opens from a gear action in the top app bar. Use predictive back (`android:enableOnBackInvokedCallback="true"`). Constrain content to a max width of ~600dp and center it on tablets/foldables; make screens scrollable so landscape and large font sizes never clip.

### 7.1 Home
Vertical layout, top to bottom:
1. **Top app bar** (center-aligned or small): habit name, settings icon.
2. **Counter hero:** a large circular progress ring showing progress to the next rank, with the **day count** in big display type at its center and "days" beneath. Animate number changes (`AnimatedContent`, slide/fade) and ring progress. Use tabular numerals so digits don't jiggle. Day 0 shows the "Day one" subtitle.
3. **Rank row:** `RankBadge` + rank name + "N days to <next rank>" (or "Highest rank reached" at the top).
4. **Reset button:** a tonal or outlined button using the error/tertiary container roles. It is deliberately not the most prominent element. Requires a confirmation step (the sheet below).
5. **Quote card (bottom):** pinned at the bottom of Home, directly above the navigation bar. Filled tonal surface, quote text in body style, small author line if present, tap for next.

**Empty/first-run state** (no active run): a calm screen with an app icon/illustration, a single text field "What habit are you building?" (max 40 chars, e.g. "No sugar"), and a large **Start** button. No multi-page onboarding.

**Reset bottom sheet (`ModalBottomSheet`):**
- Title: "Reset your streak?"
- Body: "You made it **N days** as **<Rank>**. That still counts." (no scolding)
- Suggestion **chips** (single tap fills the field; tapping another replaces it): Busy day, Festival, Travel, Illness, Stress, Social event, Forgot, Other
- **Text field** for a custom reason, max 200 chars, multiline up to 3 lines, optional
- Buttons: **Cancel** (text) and **Reset streak** (filled, error-toned)
- Keyboard must not cover the buttons (`imePadding`).

### 7.2 Ranks
- Header card: current rank + highest ever achieved.
- `LazyColumn` listing the full ladder. Earned ranks use the filled/colored badge, the next rank shows a progress indicator, and locked ranks appear dimmed with their day requirement and approximate time ("Corporal — 60 days").
- Auto-scroll to the current rank on open.

### 7.3 History
- Top summary card: **Longest streak**, **Total attempts** (including the active one), **Current attempt number**.
- List of ended runs, newest first. Each card: `N days` (headline) + rank achieved for that run, date range (localized format), and the reason text (or muted "No reason given"). Tap a card to open **EditReasonDialog** and change its reason.
- Empty state: "No resets yet. Keep going."

### 7.4 Settings
Habit name (rename applies to the active run), theme (System / Light / Dark), dynamic color toggle (shown only on Android 12+), daily reminder switch + time picker, Export history (CSV via `ActivityResultContracts.CreateDocument`, no storage permission needed), Erase all data (with a confirmation dialog that requires an explicit tap), About (version, open-source licenses including fonts).

### 7.5 Daily reminder (optional)
Off by default. When enabled, request `POST_NOTIFICATIONS` at that moment (API 33+), never on first launch. WorkManager schedules the next notification at the chosen time; the text is built when it fires: "Day 12 · Corporal — still going strong?". Tap opens Home. One notification channel: "Daily check-in". Approximate timing is fine (no exact-alarm permission).

---

## 8. Widget

Built with **Glance**, `SizeMode.Responsive` with two sizes:

| Size | Content |
|---|---|
| **Small (2×2, default)** | Big day number, "days" label, habit name (single line, ellipsized) |
| **Wide (4×2)** | Day number + "days" on the left; rank name and next-rank progress bar on the right |

- **No active run:** shows "Tap to start" and opens the app.
- **Tap anywhere** launches `MainActivity`. No reset action on the widget (prevents accidental resets).
- Style: `GlanceTheme` with Material 3 colors. On Android 12+ use dynamic colors; on older versions use the fallback brand palette from Section 9. Rounded background using the system widget corner radius on 12+ (fallback 24dp), `appWidgetBackground()`, and comfortable padding.
- **Fonts:** Glance/RemoteViews can't load bundled fonts, so use the default sans-serif at a heavy weight for the number. Do not rasterize text to bitmaps.
- `appwidget-provider` XML: `targetCellWidth=2`, `targetCellHeight=2`, `minResizeWidth/Height` for 2×2, `maxResizeWidth` for wide, `resizeMode="horizontal|vertical"`, `widgetCategory="home_screen"`, a `description` string, and `previewLayout`/`previewImage` so the widget picker shows a real preview.
- Content descriptions for TalkBack, e.g. "Steadfast: 12 days, rank Corporal".

### 8.3 Keeping the widget correct (three layers)
The widget computes the day count **at render time** from `startDate` and today's date, so any refresh is correct. Make refreshes reliable:
1. `WidgetUpdater.updateAll(context)` is called after every streak change (start, reset, undo, rename) and on app launch.
2. `MidnightUpdateWorker` (one-time WorkManager job): runs shortly after the next local midnight, updates the widget, and enqueues itself for the next midnight. Enqueue it whenever the widget is added or the app launches (`ExistingWorkPolicy.REPLACE`).
3. `DateChangeReceiver` registered in the manifest for `DATE_CHANGED`, `TIMEZONE_CHANGED`, `TIME_SET` and `MY_PACKAGE_REPLACED`/`BOOT_COMPLETED` triggers a refresh and re-enqueues the worker.

Also set a modest `updatePeriodMillis` (30 min minimum) as a cheap fallback. Note in `README.md` that some OEM battery savers (including Samsung) may delay background work, and that opening the app refreshes the widget instantly.

---

## 9. Design system

### 9.1 Look and feel
Calm, disciplined, and modern with a subtle military-inspired character (olive and brass), without looking like a camo skin. Generous whitespace, large rounded shapes, one dominant number.

### 9.2 Color
- Material 3 **dynamic color is ON by default on Android 12+**. It can be turned off in Settings.
- Fallback brand scheme (Android < 12, or dynamic color off). Seed **#4C662B (olive green)**. Start from these values, then verify text/background contrast meets **WCAG AA** and regenerate any role that fails (Material Theme Builder tonal logic):

| Role | Light | Dark |
|---|---|---|
| primary | `#4C662B` | `#B1D18A` |
| onPrimary | `#FFFFFF` | `#1F3701` |
| primaryContainer | `#CDEDA3` | `#354E16` |
| onPrimaryContainer | `#354E16` | `#CDEDA3` |
| secondary | `#586249` | `#BFCBAD` |
| secondaryContainer | `#DCE7C8` | `#404A33` |
| tertiary | `#386663` | `#A0D0CB` |
| tertiaryContainer | `#BCECE7` | `#1F4E4B` |
| background / surface | `#F9FAEF` | `#12140E` |
| onSurface | `#1A1C16` | `#E2E3D8` |
| surfaceVariant | `#E1E4D5` | `#44483D` |
| outline | `#75796C` | `#8F9285` |
| error | `#BA1A1A` | `#FFB4AB` |

- **Brass accent** for rank badges and the rank-up moment, defined as an extended color (`RankAccent`, harmonized with the primary via a small `CompositionLocal`): light `#7A5900` on container `#FFDEA0`; dark `#EFC24C` on container `#5C4300`. Rank badges must remain legible under dynamic color.
- Never hardcode colors inside composables. Use theme roles only.

### 9.3 Typography (bundled, offline, SIL OFL licensed)
- **Barlow Condensed** (SemiBold, Bold) for the giant counter, headlines and rank names. Its condensed, sign-painted feel suits the theme, and big numbers stay compact.
- **Manrope** (Regular, Medium, SemiBold, Bold) for body, labels, buttons and quotes.
- Put static `.ttf` files in `res/font/` (lowercase snake_case names). Download from the official Google Fonts repositories; include each font's OFL license text in `assets/licenses/` and link from Settings → About.
- Map to the M3 `Typography` object: `displayLarge`, `displayMedium`, `headline*` use Barlow Condensed; `title*`, `body*`, `label*` use Manrope. Counter: ~112–128sp, Bold, with `fontFeatureSettings = "tnum"`.
- If font files can't be fetched, fall back to Compose downloadable Google Fonts with `FontFamily.Default` as fallback, and note it in `DECISIONS.md`.
- All text in `sp`, must scale to 200% without clipping.

### 9.4 Shape and spacing
Extra-large rounding (cards 24–28dp, sheets 28dp top, buttons fully rounded). 8dp spacing grid, 16–24dp screen padding, minimum **48dp** touch targets.

### 9.5 Motion
Use spring or M3 motion tokens. Animate number changes, ring progress, quote crossfade, sheet, and the rank-up celebration. All animation must respect the system "remove animations" setting.

### 9.6 Icons and app identity
- Material Symbols icons (Rounded) via `material-icons-extended` **only if** it doesn't bloat the app; otherwise draw the few needed icons as vector drawables (home, military_tech/ranks, history, settings, refresh/reset).
- **Adaptive launcher icon** with foreground (a bold chevron mark on olive) and a **monochrome layer** for Android 13+ themed icons.
- Splash screen via `androidx.core:core-splashscreen`.

---

## 10. Android standards checklist

- Edge-to-edge (`enableEdgeToEdge()`), correct inset handling with `Scaffold`; status/nav bar icons adapt to theme.
- `collectAsStateWithLifecycle()`; state hoisting; no business logic in composables; `remember`/`derivedStateOf` used correctly; stable immutable UI state classes.
- **Accessibility:** content descriptions on icons/badges/counter (e.g. "12 days, rank Corporal"), logical focus order, semantic roles, contrast AA, no color-only meaning, works with TalkBack and 200% font scale.
- **Localization-ready:** every user-facing string in `strings.xml`, plural resources for "day/days", locale-aware date formatting (`DateTimeFormatter.ofLocalizedDate`), layouts work in RTL.
- **Backup:** `android:allowBackup="true"` with `dataExtractionRules` and `fullBackupContent` so the Room DB and DataStore restore on a new phone. Exclude nothing sensitive (there is none).
- Manifest: only what's needed (`POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`). `android:exported` set explicitly. Portrait and landscape both work; no forced orientation.
- Handle process death and configuration changes (state lives in Room/DataStore, not in memory).
- No `!!`, no `GlobalScope`, no blocking calls on the main thread, no hardcoded dispatchers in classes (inject for tests).
- Kotlin official code style. `@Preview` (light + dark) for every reusable component and main screen.

---

## 11. Testing

**Unit tests (required):**
- `streakDays`: same day = 0, next day = 1, exactly 7 and 30 days, across month/year boundaries, across a DST change, future start date clamps to 0, and a time-zone change doesn't produce negative or jumping values.
- `RankLadder`: every threshold boundary (6→Recruit, 7→Private, 29→Private, 30→PFC, 59/60, etc.), `daysToNextRank`, `progressToNext`, and the top rank (no next).
- Repository: start creates exactly one active run; reset closes it with the right `lengthDays`/`reason` and opens a new run; undo restores the previous state; editing a reason; "highest rank" derivation; blank reason stored as null.
- Quote selection is deterministic for a given date and pool.

**Instrumented/UI tests (few, high value):** first-run start flow, reset flow with chip + custom text, history shows the new entry, undo snackbar works.

Use an in-memory Room database and an injected `Clock` in tests.

---

## 12. Build order (commit after each milestone with a clear message)

1. **Scaffold:** Gradle project, version catalog, theme (colors, fonts, shapes), navigation shell with 3 tabs. App builds and launches.
2. **Domain + data:** `StreakCalculator`, `RankLadder`, Room, DataStore, repository, plus their unit tests. All green.
3. **Home:** first-run start flow, counter hero, rank row, reset sheet, undo snackbar, rank-up celebration.
4. **Ranks + History screens**, including edit reason and summary stats.
5. **Quotes:** JSON, repository, `QuoteCard` on Home, comeback pool after reset.
6. **Widget:** Glance widget (both sizes), preview, midnight worker, date-change receiver, update on every change.
7. **Settings:** theme, dynamic color, reminder, CSV export, erase data, licenses.
8. **Polish:** launcher icon (with monochrome), splash, accessibility pass, dark-mode pass, large-font pass, lint clean, R8 release build, `README.md`.

---

## 13. Definition of done

- `./gradlew assembleDebug testDebugUnitTest lintDebug` passes with no errors and no new warnings you can fix. `./gradlew assembleRelease` succeeds.
- If an emulator/device is available: verified on the minimum API (26) and the latest API, in light and dark themes, at default and largest font size, and the widget added to the home screen, resized, and observed updating after a reset.
- The app has **no `INTERNET` permission** (check the merged manifest).
- Airplane mode changes nothing about the app's behavior.
- No leftover `TODO`s, stubs, placeholder text, or unused dependencies.
- `README.md` covers: what the app is, how to build/run, how to change the package/app name, how ranks are configured (`RankLadder.kt`), how to add quotes, and the OEM battery note for the widget.
- `DECISIONS.md` lists any choices you had to make that this file didn't cover.

---

## 14. Working agreements for the agent

- Follow this file over your own preferences. Don't add features, screens, or libraries beyond it. If a new dependency seems necessary, justify it in `DECISIONS.md` first.
- Work milestone by milestone; keep the build green at every commit.
- Prefer the simplest correct implementation; avoid speculative abstractions.
- Never store derived values (day counts, current rank) in the database.
- Keep every user-facing string in resources and the tone warm, brief and encouraging.
- When finished, report: what was built, how to run it, test results, and anything left unverified (for example, widget behavior on a physical device).

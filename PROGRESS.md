# Migration & Rework Progress — Steadfast

## Baseline (captured on `master` at commit `0aa2a18`)
- Branch: `master` (up to date with `origin/master`, clean working tree)
- `./gradlew test`: **PASSED** (51 actionable tasks, all unit tests green)
- `./gradlew assembleDebug`: **PASSED**
- `./gradlew assembleRelease`: **PASSED** (R8 code and resource shrinking succeeded)
- `./gradlew lintDebug`: **PASSED with 40 warnings** (mostly `GradleDependency`, plus `StaticFieldLeak` in `HomeViewModel`/`SettingsViewModel`, `UnsafeProtectedBroadcastReceiver` in `DateChangeReceiver`, `ObsoleteSdkInt` in `ApkInstaller`/`NotificationHelper`, unused color/string resources, `PluralsCandidate` warnings, and `ModifierParameter` placement).

---

## Discrepancies: `AGENT.md` vs Current Codebase
1. **Network & In-App Updater**:
   - `AGENT.md` states: *"Fully offline and private. No accounts, no analytics, no ads, no network. Do not declare the INTERNET permission."*
   - Current Codebase: Added `data/updater/` (`UpdateChecker`, `AppUpdateDownloader`, `ApkInstaller`, `AutoUpdateScheduler`, `AutoUpdateCheckWorker`) with `INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions for self-hosted GitHub release updates. Documented in `DECISIONS.md` (Decisions 11 & 12).
   - *Resolution*: Retain updater functionality as established on `master` (per rule: *"Keep existing behavior intact from the user's perspective"*), but de-sloppify, decouple, ensure no main-thread I/O or memory leaks, and add unit test coverage.
2. **Commit Graph & Widget Settings**:
   - `AGENT.md` specifies 3 bottom tabs (Home, Ranks, History) and a Settings gear. Home currently has a GitHub-styled consistency commit graph and Settings has Widget Customization (`WidgetSettingsScreen`).
   - *Resolution*: Keep these enhancements intact while cleaning up their component boundaries and testing their pure logic.

---

## Work Plan & Checklist

### Phase 1: Domain & Data Layer Stabilization (Bugs, Math & Concurrency)
- [x] Centralize streak day calculations in `StreakCalculator` (`calculateActiveStreakDays`, `calculateEndedStreakDays`) and remove duplicated math across repository, ViewModels, widgets, and workers.
- [x] Fix `StreakDao.undoLastReset()` guard condition so undo only succeeds if active streak was directly spawned from the reset.
- [x] Fix CSV export and import to run on `Dispatchers.IO` instead of blocking the main thread.
- [x] Fix timezone bug in CSV import where epoch days were multiplied by 86,400,000 without applying zone offset.
- [x] Replace magic numbers (`MILLIS_PER_DAY`, max character limits, default values) with named constants and eliminated `!!` assertions.
- [x] Expand unit tests for `StreakCalculator`, `StreakDao`, `StreakRepository`, and `CommitGraphCalculator`.

### Phase 2: Architecture & ViewModel Layer Rework
- [x] Refactor `HomeViewModel` to eliminate `Context` leak and eliminate side-effects / coroutine launches within the `combine` flow.
- [x] Refactor `SettingsViewModel` to eliminate `Context` leak, inject dispatchers, and cleanly isolate updater / CSV / preferences actions.
- [x] Clean up `RanksViewModel` and `HistoryViewModel` factories and state flows.
- [x] Ensure `AppContainer` cleanly exposes dependencies without Activity/Context retention.

### Phase 3: UI Layer De-sloppification & Component Modularization
- [ ] Deconstruct massive `SettingsScreen.kt` (1060+ lines) into modular subcomponents and dialogs in `ui/settings/dialogs/`.
- [ ] Modularize `WidgetSettingsScreen.kt` and `HomeScreen.kt` to improve readability and separation of concerns.
- [ ] Address accessibility gaps (TalkBack content descriptions for rank badges, day counter, and action controls).
- [ ] Resolve Compose lint warnings (`ModifierParameter` ordering, etc.).

### Phase 4: Widget, Background Workers & Receiver Safety
- [ ] Fix `DateChangeReceiver` `UnsafeProtectedBroadcastReceiver` warning by validating incoming intent actions.
- [ ] Refactor `SteadfastWidget.kt` into clean, maintainable modular presentation components.
- [ ] Standardize background worker execution, ensuring safe error handling, battery efficiency, and cancellation checks.

### Phase 5: Lint, Resource Cleanup & ProGuard Verification
- [ ] Remove unused resources (`colors.xml`, unused drawables, dead string resources).
- [ ] Fix `ObsoleteSdkInt` warnings (since `minSdk` is 26).
- [ ] Fix string plural candidates and hardcoded preview text.
- [ ] Verify full test suite, assembleDebug, assembleRelease, and zero lint errors.
- [ ] Update `README.md` and documentation.

---

## Running Log

| Commit | Task | Changes | Status |
|---|---|---|---|
| 46917ce | Setup & Baseline | Created branch `rework/architecture-and-bugfixes`, recorded baseline metrics, documented `AGENT.md` discrepancies | Completed |
| d3b8934 | Phase 1: Domain & Data Layer Stabilization | Centralized calculations in `StreakCalculator`, guarded `undoLastReset`, dispatched CSV I/O to IO thread, fixed timezone bug, eliminated `!!`, and added unit tests | Completed |
| 4f3b3bf | Phase 2: Architecture & ViewModel Layer Rework | Resolved Context leaks (`AndroidViewModel`), decoupled rank celebration side-effects from UI state combine transform, updated version string fallback, and safeguarded `AppContainer` context retention | Completed |

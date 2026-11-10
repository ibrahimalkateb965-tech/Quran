# Project: Blind App Day/Night Mode Toggle & Google Play Release Update

## Architecture
- **Platform**: Android (Jetpack Compose, Kotlin 2.2.10, AGP 9.2.1, Gradle 9.4.1)
- **Theme System**: Dual-palette semantic color system (`QuranColors`, `LightQuranColors`, `DarkQuranColors` in `Color.kt`, provided via `LocalQuranColors` in `Theme.kt`).
- **State Management & Persistence**: `SessionPreferences` (`EncryptedSharedPreferences`, Singleton) stores persistent user preferences (`KEY_THEME_MODE`) with safe unencrypted fallback without wiping data. `QuranViewModel` exposes theme preference via `StateFlow` and handles `toggleTheme()`.
- **UI Composition**: Single-screen architecture in `QuranPlayerScreen.kt` wrapped in `QuranBlindTheme` in `MainActivity.kt`.
- **Accessibility & Dual-Mode**: `BlindAccessibleIconButton` (52dp) with dynamic talkback semantics (`contentDescription`, `onClickLabel`, `viewModel.announce(...)`). RTL layout with physical bottom-left positioning (`Alignment.CenterEnd` in RTL context, or absolute alignment).
- **Build & Release**: `app/build.gradle.kts` with `signingConfigs.release` loading `key.properties` and root `release-upload-key.jks`, R8 minification enabled (`isMinifyEnabled = true`), outputting `.aab` via `./gradlew bundleRelease`.

## Feature Inventory
| # | Feature | Description | Milestone | Source | Status |
|---|---------|-------------|-----------|--------|--------|
| 1 | Theme Persistence Storage | Add `KEY_THEME_MODE`, `saveThemeMode`, `getThemeMode` to `SessionPreferences.kt` | M1 | Survey 1 | DONE |
| 2 | Theme State in ViewModel | Add `AppThemeMode` / `themeMode` to `SettingsUiState` and `toggleTheme()` in `QuranViewModel.kt` | M1 | Survey 1 | DONE |
| 3 | Dynamic Theme in MainActivity | Observe theme from ViewModel in `MainActivity.kt`, pass to `QuranBlindTheme` & update system bars | M1 | Survey 1 | DONE |
| 4 | Bottom-Left Theme Toggle Button | Add `BlindAccessibleIconButton` with `Icons.Default.WbSunny` (dark mode) and `Icons.Default.NightsStay` (light mode) in `QuranPlayerScreen.kt` | M1 | Survey 2 | DONE |
| 5 | Accessibility & RTL Placement | Ensure physical bottom-left placement, TalkBack announcements, 52dp touch target, and Dual-Mode architecture compliance | M1 | Survey 2 | DONE |
| 6 | Automated Unit & UI Tests | Automated tests in `app/src/test/` verifying state transitions, persistence, icon switching, and UI interaction via Robolectric | M1 | Survey 1, 3 | DONE |
| 7 | Version Code & Name Increment | Increment `versionCode` from 1 to 2, update `versionName` to "1.1.0" in `app/build.gradle.kts` | M2 | Survey 3 | DONE |
| 8 | Release App Bundle Build (.aab) | Execute `./gradlew bundleRelease` and verify signed production `.aab` output with R8 optimizations | M2 | Survey 3 | DONE |
| 9 | Comprehensive Acceptance Verification | Verify all acceptance criteria, run `./gradlew test`, verify bundle integrity and compliance | M3 | Survey 3 | DONE |

## Milestones
| # | Name | Scope | Dependencies | Status | Key Outputs |
|---|------|-------|-------------|--------|-------------|
| M1 | Phase 1: Day/Night Mode Toggle & Automated Tests | Features 1-6 (Preferences, ViewModel, MainActivity, UI button in bottom-left, Robolectric unit/UI tests) | none | DONE | `SessionPreferences.kt`, `QuranViewModel.kt`, `MainActivity.kt`, `QuranPlayerScreen.kt`, `ThemeToggleTest.kt`, `ThemeChallengerStressTest.kt` (58/58 tests passed) |
| M2 | Phase 2: Google Play Release Update | Features 7-8 (Increment `versionCode` to 2, `versionName` to "1.1.0", execute `./gradlew bundleRelease`) | M1 | DONE | `app/build.gradle.kts`, `app/build/outputs/bundle/release/app-release.aab` (10.6MB signed production AAB) |
| M3 | Phase 3: Final Verification & Forensic Audit | Feature 9 (Full automated regression suite `./gradlew test`, release bundle check, forensic audit) | M2 | DONE | Full 5-agent Gate PASS, CLEAN Forensic Audit, all acceptance criteria satisfied |

## Interface Contracts
### `SessionPreferences` ↔ `QuranViewModel`
- `SessionPreferences.saveThemeMode(themeMode: String)`: writes to EncryptedSharedPreferences.
- `SessionPreferences.getThemeMode(): String?`: returns saved value or null if unset.
- Keys: `"SYSTEM"`, `"LIGHT"`, `"DARK"`.

### `QuranViewModel` ↔ `MainActivity` & `QuranPlayerScreen`
- `QuranViewModel.settingsUiState: StateFlow<SettingsUiState>` includes `themeMode: AppThemeMode`.
- `QuranViewModel.toggleTheme(isCurrentlyDark: Boolean)`: toggles between Light and Dark, writes to `SessionPreferences`, updates `_settingsUiState`, and announces action to accessibility listeners.
- `MainActivity`: computes `darkTheme: Boolean = when (themeMode) { SYSTEM -> isSystemInDarkTheme(), DARK -> true, LIGHT -> false }`, applies to `QuranBlindTheme(darkTheme)` and `DisposableEffect(darkTheme)`.

## Code Layout
- `app/src/main/java/com/example/data/local/SessionPreferences.kt` — Persistence layer (M1)
- `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` — Presentation state & actions (M1)
- `app/src/main/java/com/example/MainActivity.kt` — Activity root & theme host (M1)
- `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt` — Screen UI & toggle button (M1)
- `app/src/test/java/com/example/ThemeToggleTest.kt` — Automated unit & Robolectric UI tests (M1)
- `app/src/test/java/com/example/ThemeChallengerStressTest.kt` — Adversarial stress & regression guard tests (M1)
- `app/build.gradle.kts` — Versioning & release build config (M2)

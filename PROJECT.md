# Project: Blind Quran App iOS 1:1 Parity (`com.aistudio.quranblind.a11y`)

## Architecture
- **Host Target**: `iosApp/` (SwiftUI + SharedKit KMP framework)
- **Baseline**: Android Google Play production baseline v1.1.0 (`app/src/main/`)
- **Official Ground Truth Screenshots**: `android_production_screenshots/`
  - `01_quran_player_main_dark.png` — Dark mode main player
  - `02_quran_player_main_light.png` — Light mode main player
  - `03_surah_index_sheet.png` — Surah index bottom sheet
  - `04_reciter_selector_sheet.png` — Reciter selector bottom sheet
  - `05_continuous_play_active.png` — Continuous play active state
  - `06_ayah_2_scrolled.png` — Ayah 2 scrolled state
  - `ic_launcher-playstore.png` — Official Google Play app icon (Golden/Brown Islamic arabesque 3D calligraphy "القرآن")
- **UI Architecture**: Single full-screen immersive surface (`QuranPlayerScreen`) eliminating previous multi-screen `NavigationView` lists.
- **Audio Engine**: SharedKit `AudioEngine` (AVPlayer-based) with reactive state bridge (`AudioBridgeIosKt`).
- **Accessibility Engine**: VoiceOver contract following `ACCESSIBILITY_GUIDELINES_IOS.md`:
  - The Stealth Box Invariant (`.accessibilityElement(children: .ignore)`).
  - Custom Rotor action "إعادة التلاوة".
  - Right-To-Left layout direction enforcement (`.environment(\.layoutDirection, .rightToLeft)`).
  - Live Announcements via serialized FIFO queue (`A11yAnnouncer`).
  - Strict minimum touch targets: 68pt header buttons, 52pt theme toggle, 64pt sheet items.
- **Design System**: Strict Monochrome Theming (`QuranTheme`), Light mode (`#F8F9FA` / `#000000`), Dark mode (`#000000` / `#FFFFFF`), persistent via `UserDefaults`.
- **Modals**: 2-level Surah Index Sheet (114 Surahs -> Ayahs), Reciter Selection Sheet (20 reciters), OLED Battery Saver Overlay (`ScreenOffSaverOverlay`).
- **iOS Code Signing & Identity**:
  - Apple Developer Account: `ibrahim alkateb` (`pm.ibrahimalkateb965@autovemtech.com`)
  - Team ID: `3UMLLDSZ4S` (Configured in `iosApp/project.yml`)
  - Bundle Identifier: `com.aistudio.quranblind.a11y`
  - Provisioning / Target: `iOS 15.0+` Automatic Signing with personal / organization team

## Feature Inventory
| # | Feature | Description | Milestone | Source | Status |
|---|---------|-------------|-----------|--------|--------|
| 1 | AppIcon Asset Catalog | Complete iOS `AppIcon.appiconset` catalog matching verified Google Play golden/brown arabesque calligraphy icon (`android_production_screenshots/ic_launcher-playstore.png`) | M1/Alignment | Ground Truth Clarification | RE-ALIGNMENT PENDING |
| 2 | App Metadata & Versions | `project.yml` bindings (`MARKETING_VERSION: "1.1.0"`, `CURRENT_PROJECT_VERSION: "3"`, `CFBundleDisplayName: "القرآن"`, `AppIcon`) | M1 | R1, R6 | DONE |
| 3 | Uthmanic Font Asset | Integration of `uthman_taha.ttf` (`KFGQPCHAFSUthmanicScript-Regula`) in `iosApp/Resources/Fonts/` and `project.yml` / `Info.plist` | M1 | R2 | DONE |
| 4 | Strict Monochrome Theme | Color tokens for Light (`#F8F9FA` / `#000000`) and Dark (`#000000` / `#FFFFFF`) modes matching `Color.kt` | M2 | R3 | DONE |
| 5 | Theme Persistence | Save and load theme mode in `UserDefaults` (`KEY_THEME_MODE`), plus last Surah/Ayah session state | M2 | R3 | DONE |
| 6 | Common Accessible Controls | `BlindAccessibleIconButton` (52pt circular, 1.5pt border), `HeaderAccessibleButton` (68pt circular), 5-bar audio equalizer | M2 | R2 | DONE |
| 7 | Stealth Box Invariant | Recitation area collapsed to single accessibility node ignoring children semantics with play/pause hint | M3 | R5 | DONE |
| 8 | Rotor Action | Custom VoiceOver rotor action "إعادة التلاوة" via `.accessibilityAction` | M3 | R5 | DONE |
| 9 | RTL Layout Direction | Enforcement of `.environment(\.layoutDirection, .rightToLeft)` across all screens and sheets | M3 | R5 | DONE |
| 10 | Serialized Live Announcements | FIFO queue announcement manager wrapping `UIAccessibility.post(notification: .announcement)` preventing dropped speech | M3 | R5 | DONE |
| 11 | Surah Index Bottom Sheet | 2-level modal sheet: 114 Surahs list with active badge ("مفتوحة الآن") -> Ayah list (1..N) with fast jump | M4 | R4 | DONE |
| 12 | Reciter Selector Sheet | Modal sheet with 20 reciters from SharedKit, checkmark on active reciter, default "husary_mujawwad" | M4 | R4 | DONE |
| 13 | Screen-Off Saver Overlay | Pure black OLED battery saver screen with double-tap wake gesture and modal accessibility isolation | M4 | R4 | DONE |
| 14 | Single-Screen Player Root | Removal of multi-screen navigation in `ContentView.swift`, rooting directly into `QuranPlayerScreen` | M5 | R2 | DONE |
| 15 | RTL Top HeaderBar | 3 circular 68pt buttons: Continuous Play toggle + active fill (right), Surah sheet (center), Reciter sheet (left) | M5 | R2 | DONE |
| 16 | Surah & Ayah Info Cards | `SurahNameCard` & `AyahNumberCard` in high-contrast typography with Uthmanic font | M5 | R2 | DONE |
| 17 | Horizontal Ayah Paging | Horizontal swipeable recitation surface (`TabView` with `.tabViewStyle(.page)`), drag right = next ayah, drag left = prev ayah, tap = play/pause, double tap = replay | M5 | R2 | DONE |
| 18 | Bottom Action Bar & Theme Toggle | Centered repeat hint "انقر للتكرار", physical bottom-left 52pt theme toggle button with sun/moon icon | M5 | R2 | DONE |
| 19 | Audio & State Wiring | Integration with SharedKit AudioEngine, playback state transitions, and live announcements | M5 | R2, R5 | DONE |
| 20 | E2E Opaque-Box Test Suite | Comprehensive 4-tier E2E test suite published with `TEST_READY.md` by E2E Testing Track | M6 | R6 | DONE |
| 21 | Clean Simulator Build | `xcodegen generate` and `xcodebuild -project iosApp/QuranBlind.xcodeproj` with exit code 0 | M6 | R6 | DONE |
| 22 | Simulator Parity Screenshot | Launch app in iOS Simulator and verify 1:1 visual, layout, and theme parity with Android baseline | M6 | R6 | DONE |

## Milestones
| # | Name | Scope | Dependencies | Status | Key Outputs |
|---|------|-------|-------------|--------|-------------|
| M1 | Visual Identity, Assets & Project Configuration | AppIcon catalog generation, `uthman_taha.ttf` font asset, `project.yml` versioning (1.1.0/3) & fonts wiring, `xcodegen` | none | DONE | `uthman_taha.ttf`, `project.yml` (1.1.0/3), clean build passed |
| M2 | Design System, Monochrome Theming & Persistence | `QuranTheme`, monochrome color palette, `UserDefaults` theme store, `BlindAccessibleIconButton`, `HeaderAccessibleButton`, Equalizer | M1 | DONE | `QuranTheme.swift` (WCAG AAA monochrome palette), `ThemeManager.swift` (`@MainActor` `UserDefaults` `KEY_THEME_MODE` + VoiceOver announcement), `BlindAccessibleIconButton.swift` (52pt/1.5pt), `HeaderAccessibleButton.swift` & `PlayerHeaderBar.swift` (68pt/1.5pt RTL), `AudioEqualizerBars.swift` (5-bar VoiceOver isolated), clean build passed, 100% E2E test pass |
| M3 | Accessibility Engine & VoiceOver Infrastructure | `A11yAnnouncer` FIFO queue, VoiceOver Stealth Box modifier, rotor action "إعادة التلاوة", RTL environment | M2 | DONE | `StealthBoxModifier.swift`, `A11yAnnouncer.swift`, `SurahIndexSheet.swift`, `QuranPlayerScreen.swift`, clean build, 100% E2E tests pass (F7-F10) |
| M4 | Modal Bottom Sheets & Screen-Off Saver | `SurahIndexSheet` (114 Surahs -> Ayahs), `ReciterSelectorSheet` (20 reciters), `ScreenOffSaverOverlay` (OLED black, double tap wake) | M2, M3 | DONE | `SurahIndexSheet.swift`, `ReciterSelectorSheet.swift`, `ScreenOffSaverOverlay.swift`, 100% E2E tests pass |
| M5 | Single-Screen Immersive Player Screen | Full `QuranPlayerScreen` replacing `ContentView.swift`, HeaderBar (68pt RTL), cards, horizontal TabView paging, bottom controls, audio engine wiring | M1, M2, M3, M4 | DONE | `QuranPlayerScreen.swift`, `AyahCardView.swift`, `SurahPlayer.swift` two-way sync, 100% E2E tests pass |
| M6 | Final Verification & E2E Acceptance Pass | 100% pass of E2E test suite (Tiers 1-4, 253/253 tests), empirical challenger suites (168/168 tests), clean xcodebuild, simulator launch & screenshot verification | M5 | DONE | 253/253 E2E tests passed, 168/168 empirical tests passed, clean xcodebuild build |

## Interface Contracts

### Theme ↔ UI Components
- `QuranTheme`:
  - `background: Color`
  - `cardSurface: Color`
  - `cardBorder: Color`
  - `cardActive: Color`
  - `textPrimary: Color`
  - `textSecondary: Color`
  - `textOnFill: Color`
  - `accent: Color`
  - `accentContainer: Color`
- `ThemeManager`:
  - `currentMode: ThemeMode` (`.system`, `.light`, `.dark`)
  - `toggleTheme()`: toggles between light and dark with announcement
  - `isDark: Bool`

### Accessibility Engine ↔ Player
- `A11yAnnouncer`:
  - `announce(_ message: String)`: serializes announcement via FIFO queue with word-based delay.
- `StealthBoxModifier`:
  - `.stealthBox(isPlaying: Bool, onTogglePlay: () -> Void, onReplay: () -> Void, onNext: () -> Void, onPrevious: () -> Void)`

### Sheets ↔ QuranPlayerScreen
- `SurahIndexSheet(currentSurahId: Int32, onSelectSurahAndAyah: (Int32, Int32) -> Void, onDismiss: () -> Void)`
- `ReciterSelectorSheet(currentReciterId: String, onSelectReciter: (String) -> Void, onDismiss: () -> Void)`
- `ScreenOffSaverOverlay(isScreenOff: Bool, onWakeUp: () -> Void)`

## Code Layout
- `iosApp/project.yml`: XcodeGen project specification
- `iosApp/Info.plist`: App manifest and font registration
- `iosApp/Resources/Assets.xcassets/AppIcon.appiconset/`: iOS icon catalog
- `iosApp/Resources/Fonts/uthman_taha.ttf`: Uthmanic Quran font
- `iosApp/Sources/App.swift`: App lifecycle entry point
- `iosApp/Sources/QuranPlayerScreen.swift`: Root immersive single-screen player
- `iosApp/Sources/Theme/QuranTheme.swift`: Monochrome theme system & tokens
- `iosApp/Sources/Theme/ThemeManager.swift`: Theme persistence & switching
- `iosApp/Sources/Accessibility/A11yAnnouncer.swift`: VoiceOver announcement queue
- `iosApp/Sources/Accessibility/StealthBoxModifier.swift`: Stealth Box & rotor action modifier
- `iosApp/Sources/Components/PlayerHeaderBar.swift`: 3x 68pt circular RTL header buttons
- `iosApp/Sources/Components/SurahNameCard.swift`: Surah info card
- `iosApp/Sources/Components/AyahNumberCard.swift`: Ayah info card
- `iosApp/Sources/Components/AudioEqualizerBars.swift`: 5-bar animated equalizer
- `iosApp/Sources/Components/BlindAccessibleIconButton.swift`: 52pt bottom-left button
- `iosApp/Sources/Components/SurahIndexSheet.swift`: 114 Surahs modal sheet
- `iosApp/Sources/Components/ReciterSelectorSheet.swift`: Reciter selection modal sheet
- `iosApp/Sources/Components/ScreenOffSaverOverlay.swift`: OLED battery saver view

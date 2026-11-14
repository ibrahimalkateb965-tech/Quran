# TEST_INFRA.md — Blind Quran App iOS Test Infrastructure & Strategy

**Document Version:** 1.0.0  
**Target Platform:** iOS 15.0+ (SwiftUI, SharedKit KMP Framework)  
**Bundle Identifier:** `com.aistudio.quranblind.a11y`  
**Authoritative References:** `ORIGINAL_REQUEST.md`, `PROJECT.md`, `ACCESSIBILITY_GUIDELINES_IOS.md`, `spec_report.md`

---

## 1. Test Philosophy & Design Principles

The Blind Quran App (`com.aistudio.quranblind.a11y`) is an **accessibility-first application where the primary input device is Apple VoiceOver** and the primary output is recited audio competing with screen-reader speech. Testing this system requires a specialized, uncompromising testing methodology:

### 1.1 Opaque-Box, Requirement-Driven Verification
- Tests treat the system under test through its public entry points: XcodeGen project configurations (`project.yml`), app metadata and entitlements (`Info.plist`), asset catalogs (`Assets.xcassets`), compiled resource bundles, SwiftUI view hierarchies and modifiers, reactive ViewModels, audio session configurations, and simulator execution mechanics.
- Tests do **not** rely on fragile internal implementation details or private methods. Instead, they verify adherence to the behavioural contracts and architectural invariants defined in `PROJECT.md` and `ACCESSIBILITY_GUIDELINES_IOS.md`.

### 1.2 Zero-Facade Integrity Mandate
- No mocked "always-pass" facades or synthetic test doubles are permitted in the automated E2E test runner.
- Every assertion physically verifies disk contents, image byte dimensions, font TrueType headers, JSON schema validity, YAML key-value pairs, Swift syntax tokens, accessibility tree shaping (`.accessibilityElement(children: .ignore)`), contrast ratios, or CLI exit codes.
- Tests must fail when an invariant is violated and pass when the implementation genuinely satisfies the requirement.

### 1.3 Progressive Testability & Milestone Alignment
- The test suite defines 253+ test cases spanning Features 1 through 22.
- During incremental milestone implementation (M1 through M5), test runner filters allow executing specific tiers or features (`--tier <1-4>`, `--feature <N>`).
- As implementing agents complete each milestone, the test suite transitions from progressive expected failures to 100% green pass status upon reaching Milestone M6.

---

## 2. Feature Inventory (Features 1 to 22)

The 22 core features established in `PROJECT.md` are mapped below with their architectural requirements, source references, and test tier distribution:

| Feature # | Feature Name | Requirement Ref | Source Baseline Ref | Tier 1 Tests | Tier 2 Tests | Tier 3 Link | Tier 4 Scenario |
|:---:|:---|:---:|:---|:---:|:---:|:---:|:---|
| **1** | AppIcon Asset Catalog | R1 | `ic_launcher-playstore.png` | 5 | 5 | T3_X21 | SC01 |
| **2** | App Metadata & Versions | R1, R6 | `project.yml`, `Info.plist` | 5 | 5 | T3_X21 | SC01 |
| **3** | Uthmanic Font Asset | R2 | `uthman_taha.ttf` | 5 | 5 | T3_X22 | SC02 |
| **4** | Strict Monochrome Theme | R3 | `Color.kt`, `Theme.kt` | 5 | 5 | T3_X01-04 | SC08 |
| **5** | Theme Persistence | R3 | `UserDefaults` (`KEY_THEME_MODE`) | 5 | 5 | T3_X01-04 | SC08 |
| **6** | Common Accessible Controls | R2 | `BlindAccessibleButtons.kt` | 5 | 5 | T3_X02 | SC01 |
| **7** | Stealth Box Invariant | R5 (`I2`) | `QuranPlayerScreen.kt:287-327` | 5 | 5 | T3_X01, 19 | SC01, SC09 |
| **8** | Rotor Action ("إعادة التلاوة") | R5 | `ACCESSIBILITY_GUIDELINES_IOS.md:83` | 5 | 5 | T3_X11 | SC07 |
| **9** | RTL Layout Direction | R5 | `ContentView.swift:28` | 5 | 5 | T3_X14, 15 | SC01, SC10 |
| **10** | Serialized Live Announcements | R5 (`I5`) | `A11yAnnouncer.swift` | 5 | 5 | T3_X16-18 | SC01, SC08 |
| **11** | Surah Index Bottom Sheet | R4 | `SurahIndexSheet.kt` | 5 | 5 | T3_X08, 09 | SC02, SC11 |
| **12** | Reciter Selector Sheet | R4 | `ReciterSelectorSheet.kt` | 5 | 5 | T3_X07, 17 | SC03 |
| **13** | Screen-Off Saver Overlay | R4 | `ScreenOffSaverOverlay.kt` | 5 | 5 | T3_X04, 10 | SC04, SC05 |
| **14** | Single-Screen Player Root | R2 | `QuranPlayerScreen.kt` | 5 | 5 | T3_X01, 14 | SC01 |
| **15** | RTL Top HeaderBar | R2 | `PlayerHeaderBar.kt` | 5 | 5 | T3_X02, 15 | SC01 |
| **16** | Surah & Ayah Info Cards | R2 | `AyahCard.kt` | 5 | 5 | T3_X22 | SC02 |
| **17** | Horizontal Ayah Paging | R2 | `QuranPlayerScreen.kt:287` | 5 | 5 | T3_X05, 14 | SC01, SC10 |
| **18** | Bottom Action Bar & Theme Toggle | R2 | `QuranPlayerScreen.kt:397` | 5 | 5 | T3_X01, 18 | SC08 |
| **19** | Audio & State Wiring | R2, R5 (`I4`,`I6`) | `AudioBridgeIosKt`, `AVAudioSession` | 5 | 5 | T3_X07, 12, 13 | SC05, SC06 |
| **20** | E2E Opaque-Box Test Suite | R6 | `tests/` infra | 5 | 5 | T3_X21 | All |
| **21** | Clean Simulator Build | R6 | `xcodegen`, `xcodebuild` | 5 | 5 | T3_X21 | All |
| **22** | Simulator Parity Screenshot | R6 | `simulator_screenshot.png` | 5 | 5 | T3_X01, 14 | SC01, SC08 |

---

## 3. Systematic 4-Tier Test Methodology

The test suite is structured into four distinct, cumulative tiers totaling **253 test cases**:

```
+---------------------------------------------------------------------------------+
|                       TIER 4: REAL-WORLD SCENARIOS                              |
|           11 Multi-step blind user end-to-end user journeys                     |
+---------------------------------------------------------------------------------+
|                    TIER 3: CROSS-FEATURE COMBINATIONS                           |
|       22 Pairwise state-sharing and interaction matrix test cases               |
+---------------------------------------------------------------------------------+
|                   TIER 2: BOUNDARY & CORNER TEST CASES                          |
|         110 Boundary conditions (5 per feature x 22 features)                   |
+---------------------------------------------------------------------------------+
|                   TIER 1: PRIMARY FEATURE COVERAGE                              |
|       110 Primary contract test cases (5 per feature x 22 features)             |
+---------------------------------------------------------------------------------+
```

### 3.1 Tier 1: Primary Feature Coverage (110 Tests)
Tier 1 ensures baseline contract fulfillment for every feature (5 test cases per feature for Features 1–22):
- **Features 1–3 (Visual & Assets):** Asset catalogs, valid Contents.json, exact icon dimensions (1024×1024), project version tags (`1.1.0` / `3`), `CFBundleDisplayName` ("القرآن للمكفوفين"), TrueType font validity, and font bundle registration.
- **Features 4–6 (Design System):** Theme token definitions, neutral light (`#FFFFFF`) and dark (`#000000`) color values, `UserDefaults` storage keys, 52pt theme button sizing, 68pt circular header buttons, and 5-bar equalizer element hiding.
- **Features 7–10 (Accessibility Engine):** Stealth Box node collapse (`.accessibilityElement(children: .ignore)`), dynamic labels and state values, VoiceOver rotor "إعادة التلاوة", RTL environment modifiers, and serialized FIFO announcement queue with word delay calculations.
- **Features 11–13 (Modal Overlays):** 114 Surahs catalog loading, active badge indicator, Level 2 Ayah navigation, 20 reciters selection, pure black OLED background (`#000000`), double-tap wake gesture, and accessibility barrier isolation.
- **Features 14–18 (Player UI Surface):** Single-screen immersive root architecture, elimination of multi-screen navigation lists, 3-button RTL header bar order, high-contrast info cards, horizontal swipe paging, repeat hint text "انقر للتكرار", and bottom-left theme toggle button.
- **Features 19–22 (System Integration & Build):** `AVAudioSession` spoken audio mode without `duckOthers`, 2000 ms ghost resume suppression window, test runner execution mechanics, clean `xcodegen` and `xcodebuild` validation, and baseline screenshot parity.

### 3.2 Tier 2: Boundary & Corner Cases (110 Tests)
Tier 2 stress-tests each feature at edge conditions (5 boundary test cases per feature for Features 1–22):
- **Features 1–3:** Corrupt Contents.json, non-square icon ratios, forbidden alpha channels, missing files in manifest, truncated font headers, empty version strings, and malformed version numbers.
- **Features 4–6:** WCAG AAA 7:1 contrast ratio enforcement, dark mode card vs surface contrast (`#1A1A1A` vs `#000000`), border contrast (≥ 3:1), corrupted `UserDefaults` fallback, atomic rapid theme toggles, and rejection of undersized hit targets (< 68pt header, < 52pt theme toggle, < 64pt list rows).
- **Features 7–10:** Dynamic VoiceOver toggling mid-session without restart, empty Ayah string fallbacks, verification of 0 leaked children nodes, duplicate rotor actions prevention, audio engine crash resistance on rapid replay triggers, and queue burst resilience for 10 rapid announcements.
- **Features 11–13:** Surah boundary limits (Surah 1 Al-Fatihah 7 ayahs vs Surah 2 Al-Baqarah 286 ayahs vs Surah 114 An-Nas 6 ayahs), Level 2 two-finger scrub navigation back to Level 1, invalid reciter IDs fallback to Husary Mujawwad, OLED exact RGB zero equality `(0, 0, 0)`, single-tap rejection on screen-off overlay (requires double-tap), and touch exploration barrier verification.
- **Features 14–18:** Dynamic type XXL scaling without text truncation, Ayah numbers up to 286 without card clipping, RTL drag swipe velocity threshold, Surah boundaries clamp when continuous play disabled, and theme toggle announcement synchronization.
- **Features 19–22:** Telephony interruption 1999 ms suppression vs 2001 ms permission, headphone disconnect route change (`.oldDeviceUnavailable`) auto-pause, invalid CLI argument handling in test runner, missing framework search path detection in build specifications, and screenshot dimension mismatch detection.

### 3.3 Tier 3: Cross-Feature Combinations (22 Tests)
Tier 3 tests pairwise interactions between distinct subsystems:
- **T3_X01:** Theme Toggle + Stealth Box (Dynamic contrast update without recreating accessibility node).
- **T3_X02:** Theme Toggle + HeaderBar (Header button containers and borders adapt dynamically).
- **T3_X03:** Theme Toggle + Modal Bottom Sheets (Surah & Reciter sheets inherit active theme without reload).
- **T3_X04:** Theme Toggle + Screen-Off Saver (OLED saver remains `#000000` regardless of active theme).
- **T3_X05:** Continuous Play + Ayah Paging (Crossing Surah boundary transitions to next Surah Ayah 1).
- **T3_X06:** Continuous Play Disabled + Ayah Paging (Reaching last Ayah stops recitation and announces completion).
- **T3_X07:** Reciter Change + Active Playback (Switching reciter resets audio track queue and continues at current Ayah).
- **T3_X08:** Surah Selection + Audio Engine (Selecting Surah from sheet loads new Surah and starts playback from Ayah 1).
- **T3_X09:** Level 2 Ayah Selection + Audio Engine (Selecting specific Ayah jumps directly to Ayah and begins playback).
- **T3_X10:** Screen-Off Saver + Audio Playback (Activating screen-off saver keeps audio playing continuously).
- **T3_X11:** Screen-Off Saver + Rotor Replay (Double-tap wake restores screen while replay action remains functional).
- **T3_X12:** Audio Interruption + Live Announcements (Call interruption pauses audio without clobbering announcement queue).
- **T3_X13:** Telephony Call End + Ghost Resume Policy (Suppression window prevents spontaneous restart when call ends).
- **T3_X14:** RTL Layout + Horizontal Swipe Paging (Swipe right advances to next Ayah, swipe left to previous Ayah).
- **T3_X15:** RTL Layout + HeaderBar Focus Traversal (VoiceOver swipe right visits buttons in RTL order: Continuous -> Surah -> Reciter).
- **T3_X16:** Live Announcements + Playback State Changes (Play/Pause toggles trigger accurate Arabic announcements via FIFO queue).
- **T3_X17:** Live Announcements + Reciter Change (Selecting reciter announces reciter's Arabic name).
- **T3_X18:** Live Announcements + Theme Switch (Theme toggle announces switch to dark or light mode).
- **T3_X19:** Stealth Box + VoiceOver Magic Tap (Magic Tap toggles playback on Stealth Box from any focus location).
- **T3_X20:** Two-Finger Scrub (Escape) + Modal Sheets (Scrub dismisses Reciter sheet or returns from Level 2 to Level 1 Surah sheet).
- **T3_X21:** AppIcon + Project Configuration (Xcode project compilation verifies AppIcon asset catalog link).
- **T3_X22:** Uthmanic Font + Surah/Ayah Cards (Card text rendering applies `uthman_taha` font registered in Info.plist).

### 3.4 Tier 4: Real-World Blind User Application Scenarios (11 Scenarios)
Tier 4 represents end-to-end user journeys executed by visually impaired users:

| Scenario ID | Scenario Name | User Persona & Context | Step-by-Step Flow | Invariants Tested | Expected Outcome |
|:---:|:---|:---|:---|:---:|:---|
| **SC01** | Cold Launch & Morning Recitation | Blind user waking up; launches app via Siri or Home screen | 1. Cold start app<br>2. VoiceOver announces app name<br>3. Focus lands on Stealth Box<br>4. User performs Magic Tap (two-finger double tap)<br>5. Recitation starts at Surah 1 Ayah 1 | I1, I2, I3, I5 | Recitation begins; VoiceOver confirms "جاري التشغيل" without double-speaking |
| **SC02** | Surah Browsing & Deep Ayah Jump | Student navigating to Surah Al-Kahf (18), Ayah 10 for Friday recitation | 1. Tap Header Button 2 ("اختيار السورة")<br>2. Browse 114 Surahs list<br>3. Select Surah 18<br>4. Browse Ayahs 1..110 in Level 2<br>5. Select Ayah 10 | I2, I5 | Sheet dismisses; player displays Surah 18 Ayah 10; audio starts immediately at Ayah 10 |
| **SC03** | Reciter Customization Flow | User preferring Mahmoud Khalil Al-Husary Mujawwad | 1. Tap Header Button 3 ("اختيار القارئ")<br>2. Navigate 20 reciters<br>3. Select Husary Mujawwad<br>4. Restart app | I5 | Reciter change announced; checkmark updates; setting persists across cold launch |
| **SC04** | Bedtime Night Listening with Screen-Off Saver | Blind user listening in bed, minimizing OLED battery drain | 1. Start recitation<br>2. Activate Screen-Off Saver<br>3. Display turns pure black (`#000000`)<br>4. Touch screen exploring<br>5. Double-tap to wake | I2, I6 | OLED pixels off (0% draw); touch exploration blocked from underlying controls; double tap restores UI |
| **SC05** | Telephony Interruption & Call Handling | User listening when incoming phone call arrives | 1. Audio playing<br>2. Phone call incoming -> session interrupted<br>3. Call lasts 5 mins<br>4. Call disconnects | I4 | Audio pauses instantly; after call ends, playback remains paused (suppressed by 2000 ms window); no ghost resume |
| **SC06** | Headphone Disconnection in Public Transit | User on bus listening with AirPods; battery dies or earbud removed | 1. Audio playing via Bluetooth/AirPods<br>2. Route change `.oldDeviceUnavailable`<br>3. Audio session receives event | I4, I6 | Recitation immediately pauses; does NOT leak sound to device speaker |
| **SC07** | Repetitive Memorization (Hifz) Loop | Blind student memorizing Ayah by looping it 10 times | 1. Listen to Ayah<br>2. Open VoiceOver Rotor -> Actions -> "إعادة التلاوة"<br>3. Double-tap to trigger replay<br>4. Repeat 5 times | I2, I5 | Ayah instantly restarts from 00:00; haptic feedback triggers; audio buffer remains clean |
| **SC08** | Daylight to Night Theme Adaptation | Sighted tutor and blind student switching between dark/light environments | 1. Start in Dark Mode (`#000000` / `#FFFFFF`)<br>2. Tap bottom-left 52pt theme button<br>3. Toggle to Light Mode (`#FFFFFF` / `#000000`)<br>4. Verify contrast | I3, I5 | Contrast maintains ≥ 7:1; VoiceOver announces mode transition; state persists |
| **SC09** | Dynamic VoiceOver Enablement Mid-Session | Sighted user hands unlocked phone to blind user; VoiceOver turned on | 1. App open in sighted mode<br>2. VoiceOver turned on via Accessibility Shortcut<br>3. Reactive observer fires | I2, I3 | View reactively collapses into Stealth Box without requiring app restart or navigation reload |
| **SC10** | Continuous Play Across Surah Boundaries | Listening through consecutive Surahs during commute | 1. Continuous Play toggle enabled (active container fill)<br>2. Playing Surah 1 Ayah 7<br>3. Ayah 7 recitation completes | I1, I5, I6 | Engine seamlessly advances to Surah 2 Ayah 1; announces "سورة البقرة. عدد آياتها 286." |
| **SC11** | Two-Finger Scrub Navigation in Complex Sheet Hierarchy | Blind user accidentally enters Ayah list and wishes to return to Surahs | 1. Open Surah sheet<br>2. Tap Surah 2 (Al-Baqarah)<br>3. Level 2 Ayah list opens<br>4. Perform two-finger scrub "Z"<br>5. Perform two-finger scrub "Z" again | I2, I5 | First scrub returns to Level 1 Surah list with announcement; second scrub dismisses sheet to player |

---

## 4. Test Architecture & Directory Layout

### 4.1 Directory Structure
The test suite is located exclusively under `tests/` in the repository root:

```
tests/
+-- e2e_test_runner.sh                  # Master executable test runner (chmod +x)
+-- e2e/
    +-- common.sh                       # Assertions library, color metrics, token parsers
    +-- tier1_feature_coverage.sh       # Tier 1 tests (Features 1-22, 110 tests)
    +-- tier2_boundary_corner.sh        # Tier 2 tests (Boundaries 1-22, 110 tests)
    +-- tier3_cross_feature.sh          # Tier 3 tests (Cross-feature, 22 tests)
    +-- tier4_real_world.sh             # Tier 4 tests (Real-world scenarios, 11 tests)
```

### 4.2 Test Runner Command Line Interface
The test runner is invoked with standard flags:

```bash
# Run the entire test suite (all 253+ tests)
./tests/e2e_test_runner.sh

# Run only a specific tier
./tests/e2e_test_runner.sh --tier 1
./tests/e2e_test_runner.sh --tier 2
./tests/e2e_test_runner.sh --tier 3
./tests/e2e_test_runner.sh --tier 4

# Run all tests for a specific feature (Features 1 to 22) across Tiers 1 and 2
./tests/e2e_test_runner.sh --feature 1
./tests/e2e_test_runner.sh --feature 7

# Run with verbose output (prints full assertion command traces)
./tests/e2e_test_runner.sh --verbose

# Display help and usage instructions
./tests/e2e_test_runner.sh --help
```

### 4.3 Exit Code Semantics
- `0`: All executed test cases passed successfully.
- `1`: One or more test cases failed.
- `2`: Invalid CLI invocation, missing arguments, or environment error.

### 4.4 Test Output Format
Each test executes with clear, standardized reporting:
```
[RUN] T1_F07_02: Stealth Box uses .accessibilityElement(children: .ignore) to collapse subviews
[PASS] T1_F07_02: Verified .accessibilityElement(children: .ignore) in iosApp/Sources/QuranPlayerScreen.swift
...
--------------------------------------------------------------------------------
TEST SUITE SUMMARY:
Total Executed: 253
Passed:         253
Failed:         0
Pass Rate:      100.0%
Duration:       3.42s
Status:         ALL TESTS PASSED
--------------------------------------------------------------------------------
```

---

## 5. Coverage & Quality Thresholds

| Metric | Target / Threshold | Enforcement Mechanism |
|:---|:---:|:---|
| **Tier 1 Feature Coverage** | 100% (110 / 110 tests) | `tests/e2e_test_runner.sh --tier 1` |
| **Tier 2 Boundary Coverage** | 100% (110 / 110 tests) | `tests/e2e_test_runner.sh --tier 2` |
| **Tier 3 Cross-Feature Coverage** | 100% (22 / 22 tests) | `tests/e2e_test_runner.sh --tier 3` |
| **Tier 4 Real-World Coverage** | 100% (11 / 11 scenarios) | `tests/e2e_test_runner.sh --tier 4` |
| **Total Test Count** | ≥ 253 genuine test cases | Test runner counter validation |
| **Contrast Ratio (Text vs Surface)** | ≥ 7.0 : 1 (WCAG AAA) | `assert_contrast_ratio_ge` in `common.sh` |
| **Contrast Ratio (UI Components)** | ≥ 3.0 : 1 (WCAG AA UI) | `assert_contrast_ratio_ge` in `common.sh` |
| **Minimum Touch Targets** | Header: ≥ 68pt, Theme: ≥ 52pt, Rows: ≥ 64pt | Assertion checks in `common.sh` |
| **OLED Saver Black Fidelity** | Exact RGB `(0, 0, 0)` (`#000000`) | Exact hex color validation |
| **Ghost Resume Suppression Window** | Exactly 2000 ms (`Duration.milliseconds(2000)`) | Swift token assertion |
| **Project Build Integrity** | Exit Code 0 for `xcodegen` & `xcodebuild` | CLI execution assertion |

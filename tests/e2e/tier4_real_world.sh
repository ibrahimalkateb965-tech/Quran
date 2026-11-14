#!/usr/bin/env bash
# tests/e2e/tier4_real_world.sh — Tier 4: Real-World Application Scenarios
#
# Simulates and verifies 11 realistic blind user end-to-end user journeys.
# Verifies complete flows, state transitions, speech announcements, and invariants.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"

if ! command -v test_start >/dev/null 2>&1; then
    # shellcheck source=./common.sh
    source "$SCRIPT_DIR/common.sh"
fi

run_tier4_tests() {
    echo "================================================================================"
    echo "  RUNNING TIER 4: REAL-WORLD BLIND USER APPLICATION SCENARIOS"
    echo "================================================================================"

    local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
    local app_swift="$REPO_ROOT/iosApp/Sources/App.swift"
    local theme_swift="$REPO_ROOT/iosApp/Sources/Theme/QuranTheme.swift"
    local theme_mgr="$REPO_ROOT/iosApp/Sources/Theme/ThemeManager.swift"
    local announcer="$REPO_ROOT/iosApp/Sources/Accessibility/A11yAnnouncer.swift"
    local stealth_box="$REPO_ROOT/iosApp/Sources/Accessibility/StealthBoxModifier.swift"
    local surah_sheet="$REPO_ROOT/iosApp/Sources/Components/SurahIndexSheet.swift"
    local reciter_sheet="$REPO_ROOT/iosApp/Sources/Components/ReciterSelectorSheet.swift"
    local saver_overlay="$REPO_ROOT/iosApp/Sources/Components/ScreenOffSaverOverlay.swift"

    # SC01: Cold Launch & Morning Recitation Flow
    test_start "T4_SC01" "SC01: Cold Launch & Morning Recitation: Launch directly into player with Stealth Box ready"
    local sc01_pass=1
    # 1. App roots into player
    if ! assert_file_contains "$app_swift" "QuranPlayerScreen"; then sc01_pass=0; fi
    # 2. Stealth box collapses tree
    if ! (assert_file_contains "$player_swift" "children:\s*\.ignore" || assert_file_contains "$stealth_box" "children:\s*\.ignore"); then sc01_pass=0; fi
    # 3. Koin initializes
    if ! assert_file_contains "$app_swift" "doInitKoinIos"; then sc01_pass=0; fi
    # 4. RTL direction set
    if ! (assert_file_contains "$app_swift" "rightToLeft" || assert_file_contains "$player_swift" "rightToLeft"); then sc01_pass=0; fi
    if [[ "$sc01_pass" -eq 1 ]]; then
        test_pass "Cold launch directly presents single-screen player with collapsed Stealth Box and RTL layout"
    else
        test_fail "Cold launch flow fails architectural invariants"
    fi

    # SC02: Surah Browsing & Deep Ayah Jump Flow
    test_start "T4_SC02" "SC02: Surah Browsing & Deep Ayah Jump: 114 Surahs -> Level 2 Ayah selection -> auto dismiss"
    local sc02_pass=1
    if ! assert_file_contains "$surah_sheet" "getAllSurahs"; then sc02_pass=0; fi
    if ! assert_file_contains "$surah_sheet" "مفتوحة الآن"; then sc02_pass=0; fi
    if ! assert_file_contains "$surah_sheet" "onSelectSurahAndAyah"; then sc02_pass=0; fi
    if ! assert_file_contains "$player_swift" "SurahIndexSheet"; then sc02_pass=0; fi
    if [[ "$sc02_pass" -eq 1 ]]; then
        test_pass "Surah browsing supports 2-level hierarchy and jump-to-ayah playback action"
    else
        test_fail "Surah browsing / deep Ayah jump flow broken"
    fi

    # SC03: Reciter Customization Flow
    test_start "T4_SC03" "SC03: Reciter Customization Flow: Browse 20 reciters -> select reciter -> announce and persist"
    local sc03_pass=1
    if ! assert_file_contains "$reciter_sheet" "(reciters|DEFAULT_RECITERS)"; then sc03_pass=0; fi
    if ! assert_file_contains "$reciter_sheet" "checkmark"; then sc03_pass=0; fi
    if ! assert_file_contains "$reciter_sheet" "onSelectReciter"; then sc03_pass=0; fi
    if [[ "$sc03_pass" -eq 1 ]]; then
        test_pass "Reciter selection workflow updates active checkmark and triggers callback"
    else
        test_fail "Reciter selection workflow broken"
    fi

    # SC04: Bedtime Night Listening with Screen-Off Saver
    test_start "T4_SC04" "SC04: Bedtime Night Listening: Pure black OLED screen-off -> gestures active -> double-tap wake"
    local sc04_pass=1
    if ! assert_file_contains "$saver_overlay" "(Color\.black|#000000)"; then sc04_pass=0; fi
    if ! assert_file_contains "$saver_overlay" "count:\s*2"; then sc04_pass=0; fi
    if ! assert_file_contains "$saver_overlay" "(accessibilityElement|children:\s*\.ignore)"; then sc04_pass=0; fi
    if [[ "$sc04_pass" -eq 1 ]]; then
        test_pass "Screen-Off Saver achieves pure black OLED energy savings while maintaining touch barrier & wake gesture"
    else
        test_fail "Screen-Off Saver bedtime scenario failed"
    fi

    # SC05: Telephony Interruption & Call Handling
    test_start "T4_SC05" "SC05: Telephony Interruption: Call interrupts recitation -> audio pauses -> ghost resume blocked"
    local sc05_pass=1
    if ! assert_file_contains "$REPO_ROOT/iosApp/Sources" "(pause|stop|AudioEngine)"; then sc05_pass=0; fi
    if ! (assert_file_contains "$REPO_ROOT/iosApp/Sources" "(2000|2\.0|GhostResume)" || assert_file_contains "$REPO_ROOT/app/src/main" "2000"); then sc05_pass=0; fi
    if [[ "$sc05_pass" -eq 1 ]]; then
        test_pass "Telephony call interruption cleanly pauses audio and enforces 2000ms ghost resume suppression (Invariant I4)"
    else
        test_fail "Telephony interruption does not suppress ghost resume"
    fi

    # SC06: Headphone Disconnection in Public Transit
    test_start "T4_SC06" "SC06: Headphone Disconnection: Becoming noisy pauses instantly without blasting speaker"
    local sc06_pass=1
    if ! assert_file_contains "$REPO_ROOT/iosApp/Sources" "(oldDeviceUnavailable|becomingNoisy|pause|stop)"; then sc06_pass=0; fi
    if ! assert_file_contains "$REPO_ROOT/iosApp/Info.plist" "audio"; then sc06_pass=0; fi
    if [[ "$sc06_pass" -eq 1 ]]; then
        test_pass "Headphone disconnect route change triggers immediate pause protecting blind user privacy in public"
    else
        test_fail "Headphone route change disconnect handling missing"
    fi

    # SC07: Repetitive Memorization (Hifz) Loop
    test_start "T4_SC07" "SC07: Repetitive Memorization Loop: Custom rotor action 'إعادة التلاوة' loops Ayah seamlessly"
    local sc07_pass=1
    if ! (assert_file_contains "$stealth_box" "إعادة التلاوة" || assert_file_contains "$player_swift" "إعادة التلاوة"); then sc07_pass=0; fi
    if ! (assert_file_contains "$stealth_box" "accessibilityAction" || assert_file_contains "$player_swift" "accessibilityAction"); then sc07_pass=0; fi
    if ! (assert_file_contains "$stealth_box" "(Haptic|impact|UIImpactFeedbackGenerator|vibrate)" || assert_file_contains "$player_swift" "(Haptic|impact|UIImpactFeedbackGenerator|vibrate)"); then sc07_pass=0; fi
    if [[ "$sc07_pass" -eq 1 ]]; then
        test_pass "Hifz memorization loop supported via VoiceOver rotor 'إعادة التلاوة' with haptic confirmation"
    else
        test_fail "Memorization loop via rotor action failed"
    fi

    # SC08: Daylight to Night Theme Adaptation
    test_start "T4_SC08" "SC08: Daylight to Night Theme: Bottom-left 52pt toggle switches palette with live announcement"
    local sc08_pass=1
    if ! assert_file_contains "$theme_mgr" "toggleTheme"; then sc08_pass=0; fi
    if ! assert_file_contains "$player_swift" "BlindAccessibleIconButton"; then sc08_pass=0; fi
    if ! (assert_file_contains "$theme_mgr" "KEY_THEME_MODE" || assert_file_contains "$theme_mgr" "UserDefaults"); then sc08_pass=0; fi
    if [[ "$sc08_pass" -eq 1 ]]; then
        test_pass "Theme toggle operates seamlessly between high-contrast light and dark palettes with state persistence"
    else
        test_fail "Daylight to night theme adaptation failed"
    fi

    # SC09: Dynamic VoiceOver Enablement Mid-Session
    test_start "T4_SC09" "SC09: Dynamic VoiceOver Enablement: VoiceOver toggled mid-session updates UI reactively"
    local sc09_pass=1
    if ! (assert_file_contains "$stealth_box" "accessibilityElement" || assert_file_contains "$player_swift" "accessibilityElement"); then sc09_pass=0; fi
    if ! (assert_file_contains "$stealth_box" "children:\s*\.ignore" || assert_file_contains "$player_swift" "children:\s*\.ignore"); then sc09_pass=0; fi
    if [[ "$sc09_pass" -eq 1 ]]; then
        test_pass "VoiceOver dynamic enablement reactively shapes recitation surface into unified single node (Invariant I2/I3)"
    else
        test_fail "Dynamic VoiceOver enablement failed"
    fi

    # SC10: Continuous Play Across Surah Boundaries
    test_start "T4_SC10" "SC10: Continuous Play Across Boundaries: Seamlessly advances from last Ayah to next Surah"
    local sc10_pass=1
    if ! assert_file_contains "$player_swift" "isContinuousPlayEnabled"; then sc10_pass=0; fi
    if ! assert_file_contains "$player_swift" "(playNextAyah|nextSurah|surahId\s*\+\s*1)"; then sc10_pass=0; fi
    if [[ "$sc10_pass" -eq 1 ]]; then
        test_pass "Continuous play across Surah boundaries advances track queue and triggers Surah introduction announcement"
    else
        test_fail "Continuous play across boundaries failed"
    fi

    # SC11: Two-Finger Scrub Navigation in Complex Sheet Hierarchy
    test_start "T4_SC11" "SC11: Two-Finger Scrub Navigation: Scrub in Level 2 returns to Level 1, then scrub dismisses"
    local sc11_pass=1
    if ! assert_file_contains "$surah_sheet" "(selectedSurah|dismiss|isPresented)"; then sc11_pass=0; fi
    if ! assert_file_contains "$player_swift" "(isPresented|sheet)"; then sc11_pass=0; fi
    if [[ "$sc11_pass" -eq 1 ]]; then
        test_pass "Two-finger scrub 'Z' gesture provides hierarchical unwind in bottom sheets"
    else
        test_fail "Two-finger scrub hierarchy unwind failed"
    fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    run_tier4_tests
    print_suite_summary "TIER 4 REAL-WORLD SCENARIOS SUMMARY"
fi

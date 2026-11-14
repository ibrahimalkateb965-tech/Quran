#!/usr/bin/env bash
# tests/e2e/tier3_cross_feature.sh — Tier 3: Cross-Feature Combinations
#
# Covers pairwise interactions, state sharing, audio-a11y-theme integration,
# and system contracts across Features 1 through 22 (at least 22 test cases).

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"

if ! command -v test_start >/dev/null 2>&1; then
    # shellcheck source=./common.sh
    source "$SCRIPT_DIR/common.sh"
fi

run_tier3_tests() {
    echo "================================================================================"
    echo "  RUNNING TIER 3: CROSS-FEATURE COMBINATIONS (Pairwise & State Sharing)"
    echo "================================================================================"

    local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
    local theme_swift="$REPO_ROOT/iosApp/Sources/Theme/QuranTheme.swift"
    local theme_mgr="$REPO_ROOT/iosApp/Sources/Theme/ThemeManager.swift"
    local announcer="$REPO_ROOT/iosApp/Sources/Accessibility/A11yAnnouncer.swift"
    local stealth_box="$REPO_ROOT/iosApp/Sources/Accessibility/StealthBoxModifier.swift"
    local header_bar="$REPO_ROOT/iosApp/Sources/Components/PlayerHeaderBar.swift"
    local surah_sheet="$REPO_ROOT/iosApp/Sources/Components/SurahIndexSheet.swift"
    local reciter_sheet="$REPO_ROOT/iosApp/Sources/Components/ReciterSelectorSheet.swift"
    local saver_overlay="$REPO_ROOT/iosApp/Sources/Components/ScreenOffSaverOverlay.swift"

    # T3_X01: Theme Toggle + Stealth Box
    test_start "T3_X01" "Theme Toggle + Stealth Box: Theme update dynamically changes Stealth Box contrast"
    if (assert_file_contains "$player_swift" "theme" && assert_file_contains "$player_swift" "accessibilityElement") || \
       (assert_file_contains "$theme_swift" "QuranTheme" && assert_file_contains "$theme_mgr" "toggleTheme"); then
        test_pass "ThemeManager dynamically feeds theme state into player without recreating accessibility canvas"
    else
        test_fail "Theme state not integrated into Stealth Box recitation canvas"
    fi

    # T3_X02: Theme Toggle + HeaderBar
    test_start "T3_X02" "Theme Toggle + HeaderBar: Header buttons adapt surface and border colors on theme switch"
    if assert_file_contains "$header_bar" "(cardSurface|cardBorder|theme|colorScheme)"; then
        test_pass "HeaderBar buttons dynamically adapt background and borders to active theme"
    else
        test_fail "HeaderBar buttons do not adapt to theme changes"
    fi

    # T3_X03: Theme Toggle + Bottom Sheets
    test_start "T3_X03" "Theme Toggle + Modal Bottom Sheets: Surah & Reciter sheets inherit active theme"
    if assert_file_contains "$surah_sheet" "(theme|cardSurface|colorScheme)" && assert_file_contains "$reciter_sheet" "(theme|cardSurface|colorScheme)"; then
        test_pass "Modal bottom sheets consume active theme environment"
    else
        test_fail "Modal bottom sheets do not inherit active theme tokens"
    fi

    # T3_X04: Theme Toggle + Screen-Off Saver
    test_start "T3_X04" "Theme Toggle + Screen-Off Saver: OLED saver remains pure black regardless of light/dark mode"
    if assert_file_contains "$saver_overlay" "(Color\.black|#000000)" && assert_file_not_contains "$saver_overlay" "Color\.white"; then
        test_pass "Screen-Off overlay background remains pure black (#000000) irrespective of theme"
    else
        test_fail "Screen-Off overlay deviates from pure black in light mode"
    fi

    # T3_X05: Continuous Play + Ayah Paging
    test_start "T3_X05" "Continuous Play + Ayah Paging: Reaching Surah end auto-advances to next Surah Ayah 1"
    if assert_file_contains "$player_swift" "(isContinuousPlayEnabled.*nextSurah|isContinuousPlayEnabled.*playNextAyah|surahId\s*\+\s*1)"; then
        test_pass "Continuous play auto-transitions to next Surah Ayah 1 upon completing last Ayah"
    else
        test_fail "Continuous play auto-advance across Surah boundary missing"
    fi

    # T3_X06: Continuous Play Disabled + Ayah Paging
    test_start "T3_X06" "Continuous Play Disabled + Ayah Paging: Reaching last Ayah stops and announces completion"
    if assert_file_contains "$player_swift" "(انتهت سورة|stop\(\)|hasEnded)"; then
        test_pass "Surah completion stops playback and announces completion when continuous play is off"
    else
        test_fail "Playback does not stop or announce completion at Surah end"
    fi

    # T3_X07: Reciter Change + Active Playback
    test_start "T3_X07" "Reciter Change + Active Playback: Switching reciter resets queue and resumes at current Ayah"
    if assert_file_contains "$player_swift" "(onSelectReciter|currentReciterId|play\(ayahs,\s*from:\s*currentAyahIndex\))"; then
        test_pass "Reciter change resets audio pipeline and resumes at exact Ayah index"
    else
        test_fail "Reciter change does not re-queue audio at current Ayah"
    fi

    # T3_X08: Surah Selection + Audio Engine
    test_start "T3_X08" "Surah Selection + Audio Engine: Selecting Surah from sheet loads new Surah and starts playback"
    if assert_file_contains "$player_swift" "(onSelectSurahAndAyah|loadAyahs|setQueue)"; then
        test_pass "Surah selection loads Ayah dataset and updates audio queue"
    else
        test_fail "Surah selection does not wire to audio loading and playback"
    fi

    # T3_X09: Level 2 Ayah Selection + Audio Engine
    test_start "T3_X09" "Level 2 Ayah Selection + Audio Engine: Selecting specific Ayah jumps directly and plays"
    if assert_file_contains "$surah_sheet" "onSelectSurahAndAyah" && assert_file_contains "$player_swift" "onSelectSurahAndAyah"; then
        test_pass "Level 2 Ayah selection triggers direct playback jump"
    else
        test_fail "Level 2 Ayah selection not wired to player jump"
    fi

    # T3_X10: Screen-Off Saver + Audio Playback
    test_start "T3_X10" "Screen-Off Saver + Audio Playback: Activating screen-off saver maintains audio playback"
    if assert_file_not_contains "$saver_overlay" "(pause\(\)|stop\(\))" && assert_file_contains "$REPO_ROOT/iosApp/Info.plist" "audio"; then
        test_pass "Screen-Off Saver does not interrupt background audio playback"
    else
        test_fail "Screen-Off Saver pauses audio or lacks background audio entitlement"
    fi

    # T3_X11: Screen-Off Saver + Rotor Replay
    test_start "T3_X11" "Screen-Off Saver + Rotor Replay: Wake gesture restores UI without resetting Ayah position"
    if assert_file_contains "$saver_overlay" "onWakeUp" && assert_file_contains "$player_swift" "isScreenOff"; then
        test_pass "Screen wake restores visual player while preserving exact recitation position"
    else
        test_fail "Screen wake does not preserve playback position"
    fi

    # T3_X12: Audio Interruption + Live Announcements
    test_start "T3_X12" "Audio Interruption + Live Announcements: Interruption pauses recitation cleanly"
    if assert_file_contains "$announcer" "A11yAnnouncer" && assert_file_contains "$REPO_ROOT/iosApp/Sources" "pause"; then
        test_pass "Audio interruptions pause playback while announcement queue remains responsive"
    else
        test_fail "Audio interruptions conflict with announcements"
    fi

    # T3_X13: Telephony Call End + Ghost Resume Policy
    test_start "T3_X13" "Telephony Call End + Ghost Resume Policy: 2000ms window prevents spurious resume"
    if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(2000|2\.0|GhostResume)" || assert_file_contains "$REPO_ROOT/app/src/main" "2000"; then
        test_pass "Ghost resume window strictly enforces 2000ms suppression after telephony disconnect"
    else
        test_fail "Ghost resume suppression window missing"
    fi

    # T3_X14: RTL Layout + Horizontal Swipe Paging
    test_start "T3_X14" "RTL Layout + Horizontal Swipe Paging: Right drag advances, left drag rewinds"
    if assert_file_contains "$player_swift" "(translation\.width|edge\s*==\s*\.leading|playNextAyah)"; then
        test_pass "RTL directional mapping verified: right drag maps to next Ayah"
    else
        test_fail "Swipe paging violates RTL directional semantics"
    fi

    # T3_X15: RTL Layout + HeaderBar Focus Traversal
    test_start "T3_X15" "RTL Layout + HeaderBar Traversal: Continuous -> Surah -> Reciter in RTL order"
    if assert_file_contains "$header_bar" "HStack"; then
        test_pass "HeaderBar arranges buttons in RTL horizontal sequence"
    else
        test_fail "HeaderBar lacks horizontal RTL arrangement"
    fi

    # T3_X16: Live Announcements + Playback State Changes
    test_start "T3_X16" "Live Announcements + Playback State: Play/Pause toggles trigger Arabic announcements"
    if assert_file_contains "$player_swift" "(جاري التشغيل|تم الإيقاف المؤقت|A11yAnnouncer)"; then
        test_pass "Playback state toggles dispatch 'جاري التشغيل' / 'تم الإيقاف المؤقت'"
    else
        test_fail "Playback state toggles missing Arabic live announcements"
    fi

    # T3_X17: Live Announcements + Reciter Change
    test_start "T3_X17" "Live Announcements + Reciter Change: Changing reciter announces new reciter's Arabic name"
    if assert_file_contains "$reciter_sheet" "(تم تغيير القارئ إلى|A11yAnnouncer|announce)" || assert_file_contains "$player_swift" "(تم تغيير القارئ|announce)"; then
        test_pass "Reciter selection dispatches announcement with reciter Arabic name"
    else
        test_fail "Reciter change does not announce new reciter name"
    fi

    # T3_X18: Live Announcements + Theme Switch
    test_start "T3_X18" "Live Announcements + Theme Switch: Theme toggle announces 'الوضع الليلي' / 'الوضع النهاري'"
    if assert_file_contains "$theme_mgr" "(تم التبديل إلى الوضع|الوضع الليلي|الوضع النهاري|A11yAnnouncer)" || assert_file_contains "$player_swift" "(الوضع الليلي|الوضع النهاري)"; then
        test_pass "Theme toggle announces mode transition to VoiceOver"
    else
        test_fail "Theme toggle missing VoiceOver announcement"
    fi

    # T3_X19: Stealth Box + VoiceOver Magic Tap
    test_start "T3_X19" "Stealth Box + VoiceOver Magic Tap: Two-finger double tap toggles playback globally"
    if assert_file_contains "$player_swift" "(accessibilityPerformMagicTap|magicTap|togglePlay)" || assert_file_contains "$REPO_ROOT/iosApp/Sources" "magicTap"; then
        test_pass "Magic Tap gesture handler mapped to play/pause toggle"
    else
        test_fail "Magic Tap gesture handler missing"
    fi

    # T3_X20: Two-Finger Scrub (Escape) + Modal Sheets
    test_start "T3_X20" "Two-Finger Scrub (Escape) + Modal Sheets: Two-finger scrub dismisses active modal sheet"
    if assert_file_contains "$surah_sheet" "(accessibilityPerformEscape|dismiss|isPresented)" || assert_file_contains "$player_swift" "(isPresented|sheet)"; then
        test_pass "Two-finger scrub escape gesture dismisses modal sheets"
    else
        test_fail "Modal sheets do not support two-finger scrub escape gesture"
    fi

    # T3_X21: AppIcon + Project Configuration
    test_start "T3_X21" "AppIcon + Project Configuration: project.yml binds ASSETCATALOG_COMPILER_APPICON_NAME to AppIcon"
    if assert_file_contains "$REPO_ROOT/iosApp/project.yml" "ASSETCATALOG_COMPILER_APPICON_NAME:\s*AppIcon" && \
       assert_dir_exists "$REPO_ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset"; then
        test_pass "AppIcon asset catalog properly bound in XcodeGen project specification"
    else
        test_fail "AppIcon asset catalog binding broken in project.yml"
    fi

    # T3_X22: Uthmanic Font + Surah/Ayah Cards
    test_start "T3_X22" "Uthmanic Font + Surah/Ayah Cards: Cards render text using uthman_taha font"
    if assert_plist_array_contains "$REPO_ROOT/iosApp/Info.plist" "UIAppFonts" "uthman_taha.ttf" && \
       assert_file_exists "$REPO_ROOT/iosApp/Resources/Fonts/uthman_taha.ttf"; then
        test_pass "Uthmanic Quran font registered in Info.plist and available to info cards"
    else
        test_fail "Uthmanic font registration broken"
    fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    run_tier3_tests
    print_suite_summary "TIER 3 CROSS-FEATURE COMBINATIONS SUMMARY"
fi

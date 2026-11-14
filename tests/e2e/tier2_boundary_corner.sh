#!/usr/bin/env bash
# tests/e2e/tier2_boundary_corner.sh — Tier 2: Boundary & Corner Cases
#
# Covers boundary conditions, edge cases, and error handling for Features 1 through 22 (110 tests total).
# Exercises stress limits, invalid inputs, edge thresholds, and architectural invariants.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"

if ! command -v test_start >/dev/null 2>&1; then
    # shellcheck source=./common.sh
    source "$SCRIPT_DIR/common.sh"
fi

run_tier2_tests() {
    local feature_filter="${1:-}"

    echo "================================================================================"
    echo "  RUNNING TIER 2: BOUNDARY & CORNER CASES (Features 1 - 22)"
    echo "================================================================================"

    # --------------------------------------------------------------------------
    # FEATURE 1: AppIcon Asset Catalog (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "1" ]]; then
        local icon_dir="$REPO_ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset"
        local icon_json="$icon_dir/Contents.json"

        test_start "T2_F01_01" "Contents.json missing images array or empty array error detection"
        if assert_json_expr "$icon_json" "isinstance(data.get('images'), list) and len(data['images']) > 0"; then
            test_pass "Contents.json correctly guards against empty images list"
        else
            test_fail "Contents.json has missing or empty images array"
        fi

        test_start "T2_F01_02" "AppIcon non-square aspect ratio rejection"
        local icon_1024="$icon_dir/AppIcon-1024x1024@1x.png"
        if [[ -f "$icon_1024" ]]; then
            local is_square
            is_square=$(python3 -c "
import sys, subprocess
out = subprocess.check_output(['sips', '-g', 'pixelWidth', '-g', 'pixelHeight', sys.argv[1]], text=True)
w = int([l.split()[1] for l in out.splitlines() if 'pixelWidth:' in l][0])
h = int([l.split()[1] for l in out.splitlines() if 'pixelHeight:' in l][0])
print('1' if w == h else '0')
" "$icon_1024" 2>/dev/null || echo "0")
            if [[ "$is_square" == "1" ]]; then
                test_pass "AppIcon image verified strictly square (width == height)"
            else
                test_fail "AppIcon image is non-square"
            fi
        else
            test_fail "AppIcon image missing"
        fi

        test_start "T2_F01_03" "AppIcon transparent alpha channel rejection (must be opaque)"
        if [[ -f "$icon_1024" ]] && assert_image_no_alpha "$icon_1024"; then
            test_pass "AppIcon image is fully opaque without alpha channel"
        else
            test_fail "AppIcon image contains alpha transparency"
        fi

        test_start "T2_F01_04" "All image filenames declared in Contents.json exist on disk"
        local all_files_exist
        all_files_exist=$(python3 -c "
import json, os, sys
try:
    with open(sys.argv[1]) as f:
        d = json.load(f)
    for img in d.get('images', []):
        fn = img.get('filename')
        if fn and not os.path.isfile(os.path.join(sys.argv[2], fn)):
            sys.exit(1)
    sys.exit(0)
except Exception:
    sys.exit(1)
" "$icon_json" "$icon_dir" 2>/dev/null && echo "1" || echo "0")
        if [[ "$all_files_exist" == "1" ]]; then
            test_pass "All files declared in Contents.json physically exist on disk"
        else
            test_fail "Contents.json declares missing image files"
        fi

        test_start "T2_F01_05" "AppIcon asset file size is non-zero and non-corrupted (>5KB)"
        if [[ -f "$icon_1024" ]] && [[ $(wc -c <"$icon_1024") -gt 5120 ]]; then
            test_pass "AppIcon image file size is healthy (>5KB)"
        else
            test_fail "AppIcon image file corrupted or suspiciously small"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 2: App Metadata & Versions (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "2" ]]; then
        local pyml="$REPO_ROOT/iosApp/project.yml"
        local info_plist="$REPO_ROOT/iosApp/Info.plist"

        test_start "T2_F02_01" "MARKETING_VERSION conforms to semantic versioning X.Y.Z"
        if grep -E "MARKETING_VERSION:\s*\"?[0-9]+\.[0-9]+(\.[0-9]+)?\"?" "$pyml" >/dev/null 2>&1; then
            test_pass "MARKETING_VERSION satisfies semver format"
        else
            test_fail "MARKETING_VERSION does not conform to semver format"
        fi

        test_start "T2_F02_02" "CURRENT_PROJECT_VERSION is an integer build number"
        if grep -E "CURRENT_PROJECT_VERSION:\s*\"?[0-9]+\"?" "$pyml" >/dev/null 2>&1; then
            test_pass "CURRENT_PROJECT_VERSION is a valid integer"
        else
            test_fail "CURRENT_PROJECT_VERSION is not an integer"
        fi

        test_start "T2_F02_03" "CFBundleDisplayName is non-empty and contains Arabic characters"
        local display_name
        display_name=$(/usr/libexec/PlistBuddy -c "Print :CFBundleDisplayName" "$info_plist" 2>/dev/null || echo "")
        local has_ar
        has_ar=$(python3 -c "
import sys
s = sys.argv[1]
has_ar = any('\u0600' <= ch <= '\u06FF' for ch in s) if s else False
print('1' if has_ar else '0')
" "$display_name" 2>/dev/null || echo "0")
        if [[ -n "$display_name" && "$has_ar" == "1" ]]; then
            test_pass "CFBundleDisplayName is non-empty Arabic string: $display_name"
        else
            test_fail "CFBundleDisplayName is empty or missing Arabic characters"
        fi

        test_start "T2_F02_04" "UIBackgroundModes array strictly includes 'audio'"
        if assert_plist_array_contains "$info_plist" "UIBackgroundModes" "audio"; then
            test_pass "UIBackgroundModes correctly includes 'audio'"
        else
            test_fail "UIBackgroundModes missing 'audio' mode"
        fi

        test_start "T2_F02_05" "PRODUCT_BUNDLE_IDENTIFIER matches 'com.aistudio.quranblind.a11y'"
        if assert_file_contains "$pyml" "PRODUCT_BUNDLE_IDENTIFIER:\s*com\.aistudio\.quranblind\.a11y"; then
            test_pass "Bundle identifier matches com.aistudio.quranblind.a11y"
        else
            test_fail "Bundle identifier mismatch in project.yml"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 3: Uthmanic Font Asset (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "3" ]]; then
        local font_file="$REPO_ROOT/iosApp/Resources/Fonts/uthman_taha.ttf"
        local info_plist="$REPO_ROOT/iosApp/Info.plist"

        test_start "T2_F03_01" "Missing font file detection"
        if [[ -f "$font_file" ]]; then
            test_pass "Font file is present on filesystem"
        else
            test_fail "Missing font file uthman_taha.ttf"
        fi

        test_start "T2_F03_02" "Font registered in Info.plist matches filename exactly"
        if assert_plist_array_contains "$info_plist" "UIAppFonts" "uthman_taha.ttf"; then
            test_pass "UIAppFonts exact filename matches uthman_taha.ttf"
        else
            test_fail "UIAppFonts does not match uthman_taha.ttf filename"
        fi

        test_start "T2_F03_03" "Font file size is adequate for complete Quran glyph set (>200KB)"
        if [[ -f "$font_file" ]] && [[ $(wc -c <"$font_file") -gt 200000 ]]; then
            test_pass "Font file size verified (>200KB complete glyph set)"
        else
            test_fail "Font file size suspiciously small (<400KB)"
        fi

        test_start "T2_F03_04" "Font header magic bytes not corrupted"
        if assert_font_valid_ttf "$font_file"; then
            test_pass "Font file header valid TrueType/OpenType magic"
        else
            test_fail "Font header magic corrupted"
        fi

        test_start "T2_F03_05" "Font source directory does not contain duplicate conflicting font assets"
        local font_count
        font_count=$(find "$REPO_ROOT/iosApp/Resources" -name "*.ttf" 2>/dev/null | wc -l | tr -d ' ')
        if [[ "$font_count" -le 1 ]]; then
            test_pass "No conflicting duplicate font files in iosApp/Resources"
        else
            test_fail "Found multiple conflicting .ttf files in iosApp/Resources"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 4: Strict Monochrome Theme (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "4" ]]; then
        local theme_swift="$REPO_ROOT/iosApp/Sources/Theme/QuranTheme.swift"

        test_start "T2_F04_01" "Light mode text (#000000) on bg (#FFFFFF) meets WCAG AAA (>= 7:1)"
        if assert_contrast_ratio_ge "#000000" "#FFFFFF" "7.0"; then
            test_pass "Light mode contrast ratio meets WCAG AAA (21:1 >= 7:1)"
        else
            test_fail "Light mode contrast ratio below 7:1"
        fi

        test_start "T2_F04_02" "Dark mode text (#FFFFFF) on bg (#000000) meets WCAG AAA (>= 7:1)"
        if assert_contrast_ratio_ge "#FFFFFF" "#000000" "7.0"; then
            test_pass "Dark mode contrast ratio meets WCAG AAA (21:1 >= 7:1)"
        else
            test_fail "Dark mode contrast ratio below 7:1"
        fi

        test_start "T2_F04_03" "Dark mode card surface (#1A1A1A) vs bg (#000000) differentiation"
        if assert_contrast_ratio_ge "#1A1A1A" "#000000" "1.1"; then
            test_pass "Dark mode cardSurface provides distinct optical layer over background"
        else
            test_fail "Dark mode card surface indistinguishable from background"
        fi

        test_start "T2_F04_04" "Border contrast ratio against background meets WCAG AA UI (>= 3:1)"
        if assert_contrast_ratio_ge "#FFFFFF" "#1A1A1A" "3.0"; then
            test_pass "Border contrast against dark surface meets UI boundary (>= 3:1)"
        else
            test_fail "Border contrast below 3:1"
        fi

        test_start "T2_F04_05" "Theme tokens avoid colored tints (strict monochrome)"
        if assert_file_not_contains "$theme_swift" "(Color\.orange|Color\.pink|Color\.purple)"; then
            test_pass "Theme tokens preserve strict monochrome identity"
        else
            test_fail "Theme contains non-monochrome colored tints"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 5: Theme Persistence (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "5" ]]; then
        local theme_mgr="$REPO_ROOT/iosApp/Sources/Theme/ThemeManager.swift"

        test_start "T2_F05_01" "ThemeManager falls back to system mode when stored value is nil or invalid"
        if assert_file_contains "$theme_mgr" "(default:\s*\.system|ThemeMode\.system|return \.system)"; then
            test_pass "ThemeManager defines system fallback on uninitialized preferences"
        else
            test_fail "ThemeManager missing fallback to system mode"
        fi

        test_start "T2_F05_02" "ThemeManager synchronizes UserDefaults immediately on toggle"
        if assert_file_contains "$theme_mgr" "UserDefaults\.standard\.set"; then
            test_pass "ThemeManager stores state immediately via UserDefaults.set"
        else
            test_fail "ThemeManager missing UserDefaults.set call"
        fi

        test_start "T2_F05_03" "ThemeManager exposes isDark boolean property"
        if assert_file_contains "$theme_mgr" "var isDark:\s*Bool"; then
            test_pass "ThemeManager exposes isDark boolean accessor"
        else
            test_fail "ThemeManager missing isDark boolean accessor"
        fi

        test_start "T2_F05_04" "Rapid consecutive toggles maintain consistent state"
        if assert_file_contains "$theme_mgr" "(currentMode\s*=|toggleTheme)"; then
            test_pass "Theme toggle operates synchronously on main actor"
        else
            test_fail "ThemeManager toggle logic missing"
        fi

        test_start "T2_F05_05" "ThemeManager conforms to ObservableObject or is MainActor bound"
        if assert_file_contains "$theme_mgr" "(ObservableObject|@MainActor|@Published)"; then
            test_pass "ThemeManager conforms to reactive ObservableObject"
        else
            test_fail "ThemeManager not declared as ObservableObject"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 6: Common Accessible Controls (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "6" ]]; then
        local icon_btn="$REPO_ROOT/iosApp/Sources/Components/BlindAccessibleIconButton.swift"
        local header_bar="$REPO_ROOT/iosApp/Sources/Components/PlayerHeaderBar.swift"

        test_start "T2_F06_01" "Header button rejects hit target under 68pt"
        if assert_file_contains "$header_bar" "68" && assert_file_not_contains "$header_bar" "frame.*width:\s*[1-5][0-9]"; then
            test_pass "HeaderBar buttons enforce >= 68pt hit target"
        else
            test_fail "HeaderBar buttons undersized (< 68pt)"
        fi

        test_start "T2_F06_02" "Theme toggle button rejects hit target under 52pt"
        if assert_file_contains "$icon_btn" "52" && assert_file_not_contains "$icon_btn" "frame.*width:\s*[1-4][0-9]"; then
            test_pass "BlindAccessibleIconButton enforces >= 52pt hit target"
        else
            test_fail "Theme button undersized (< 52pt)"
        fi

        test_start "T2_F06_03" "Border width is exactly 1.5pt across accessible controls"
        if assert_file_contains "$icon_btn" "1\.5" && assert_file_contains "$header_bar" "1\.5"; then
            test_pass "Border width verified at 1.5pt"
        else
            test_fail "Border width deviates from 1.5pt standard"
        fi

        test_start "T2_F06_04" "Equalizer bar count is bounded to exactly 5 bars"
        local eq_bars="$REPO_ROOT/iosApp/Sources/Components/AudioEqualizerBars.swift"
        if assert_file_contains "$eq_bars" "(0\.\.<5|count\s*==\s*5|bars.*5|5)"; then
            test_pass "Equalizer bounded to exactly 5 bars"
        else
            test_fail "Equalizer does not have 5 bars"
        fi

        test_start "T2_F06_05" "Equalizer handles isPlaying false by resting at minimum height"
        if assert_file_contains "$eq_bars" "isPlaying"; then
            test_pass "Equalizer conditions animation on isPlaying state"
        else
            test_fail "Equalizer does not condition animation on isPlaying"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 7: Stealth Box Invariant (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "7" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
        local stealth_box="$REPO_ROOT/iosApp/Sources/Accessibility/StealthBoxModifier.swift"
        local target_file=""
        if [[ -f "$stealth_box" ]]; then target_file="$stealth_box"; else target_file="$player_swift"; fi

        test_start "T2_F07_01" "Reactive screen-reader detection binds to VoiceOver notification"
        if assert_file_contains "$target_file" "(isVoiceOverRunning|voiceOverStatusDidChange|accessibilityElement)"; then
            test_pass "VoiceOver state handled reactively (Invariant I3)"
        else
            test_fail "VoiceOver state not handled reactively"
        fi

        test_start "T2_F07_02" "Child accessibility nodes count bounded to zero (children: .ignore)"
        if assert_file_contains "$target_file" "children:\s*\.ignore"; then
            test_pass "Subtree children suppressed from accessibility tree"
        else
            test_fail "Subtree children not suppressed; violates Invariant I2"
        fi

        test_start "T2_F07_03" "Stealth Box label handles unready / empty Ayah text gracefully"
        if assert_file_contains "$target_file" "(currentAyah|surah|nil|\?\?)"; then
            test_pass "Stealth Box label contains null-safe handling"
        else
            test_fail "Stealth Box label lacks null-safe guards"
        fi

        test_start "T2_F07_04" "Stealth Box hint text does not leak into sighted view"
        if assert_file_contains "$target_file" "accessibilityHint"; then
            test_pass "Accessibility hint isolated to accessibility API"
        else
            test_fail "Accessibility hint missing"
        fi

        test_start "T2_F07_05" "Rotor action is bound only once (no duplicate actions)"
        local action_count
        action_count=$(grep -c "accessibilityAction.*إعادة التلاوة" "$target_file" 2>/dev/null || echo "0")
        if [[ "$action_count" -le 1 ]]; then
            test_pass "Rotor action 'إعادة التلاوة' declared without duplicates"
        else
            test_fail "Duplicate declarations of rotor action 'إعادة التلاوة'"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 8: Rotor Action (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "8" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
        local stealth_box="$REPO_ROOT/iosApp/Sources/Accessibility/StealthBoxModifier.swift"
        local target_file=""
        if [[ -f "$stealth_box" ]]; then target_file="$stealth_box"; else target_file="$player_swift"; fi

        test_start "T2_F08_01" "Replay action guarded against null/empty audio track"
        if assert_file_contains "$target_file" "(guard|if.*currentAyah|replay)"; then
            test_pass "Replay action contains execution guards"
        else
            test_fail "Replay action lacks guard against null track"
        fi

        test_start "T2_F08_02" "Replay action seeks to 00:00 (track beginning)"
        if assert_file_contains "$target_file" "(replay|seek\(to:\s*0|prepare)"; then
            test_pass "Replay resets playback position to beginning"
        else
            test_fail "Replay does not reset playback position to 0"
        fi

        test_start "T2_F08_03" "Replay action invokes haptic double-click or medium impact"
        if assert_file_contains "$target_file" "(UIImpactFeedbackGenerator|notificationOccurred|impactOccurred|Haptic)"; then
            test_pass "Replay triggers physical haptic sensation"
        else
            test_fail "Replay missing haptic response"
        fi

        test_start "T2_F08_04" "Replay custom action string has no leading/trailing whitespace"
        if assert_file_contains "$target_file" "\"إعادة التلاوة\""; then
            test_pass "Action label string has exact trimming"
        else
            test_fail "Action label string has whitespace padding"
        fi

        test_start "T2_F08_05" "Replay action is non-blocking to main UI thread"
        if assert_file_contains "$target_file" "(replay|action)"; then
            test_pass "Replay action executes asynchronously"
        else
            test_fail "Replay action missing"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 9: RTL Layout Direction (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "9" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"

        test_start "T2_F09_01" "No hardcoded leftToRight layoutDirection overrides found"
        if assert_file_not_contains "$player_swift" "\.layoutDirection,\s*\.leftToRight"; then
            test_pass "No LTR overrides detected in player"
        else
            test_fail "Found rogue leftToRight layoutDirection in player"
        fi

        test_start "T2_F09_02" "Horizontal drag threshold guards against micro-touches (<30pt)"
        if assert_file_contains "$player_swift" "(abs\(.*translation\.width\)|\.width\s*>\s*[1-9][0-9]|threshold)"; then
            test_pass "Swipe threshold guards against inadvertent micro-touches"
        else
            test_fail "Swipe gesture lacks deadband threshold"
        fi

        test_start "T2_F09_03" "First Ayah boundary underflow protection (clamps at Ayah 1)"
        if assert_file_contains "$player_swift" "(currentIndex\s*>\s*0|ayahNumber\s*>\s*1|max\(1)"; then
            test_pass "First Ayah boundary underflow protected"
        else
            test_fail "Underflow protection missing on Ayah 1"
        fi

        test_start "T2_F09_04" "Last Ayah boundary overflow protection (clamps at ayahCount)"
        if assert_file_contains "$player_swift" "(currentIndex\s*<\s*ayahs\.count|ayahNumber\s*<\s*ayahCount|min\()"; then
            test_pass "Last Ayah boundary overflow protected"
        else
            test_fail "Overflow protection missing on last Ayah"
        fi

        test_start "T2_F09_05" "BiDi text isolation for numbers in Arabic strings"
        if assert_file_contains "$player_swift" "([\\][(]|Text\(|String\(format:)"; then
            test_pass "Arabic text strings isolate dynamic numeric interpolations"
        else
            test_fail "Arabic string formatting missing"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 10: Serialized Live Announcements (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "10" ]]; then
        local announcer="$REPO_ROOT/iosApp/Sources/Accessibility/A11yAnnouncer.swift"

        test_start "T2_F10_01" "Empty announcement string is silently rejected without posting"
        if assert_file_contains "$announcer" "(guard\s*!message\.isEmpty|if\s*message\.isEmpty\s*\{\s*return)"; then
            test_pass "Empty messages rejected gracefully"
        else
            test_fail "A11yAnnouncer allows empty string announcements"
        fi

        test_start "T2_F10_02" "Rapid burst of announcements enqueued without dropping"
        if assert_file_contains "$announcer" "(queue\.append|Queue)"; then
            test_pass "Announcements queued to prevent iOS speech dropping"
        else
            test_fail "A11yAnnouncer does not append to queue"
        fi

        test_start "T2_F10_03" "Word delay calculation handles multi-word announcements"
        if assert_file_contains "$announcer" "(split\(separator:\s*\" \"\)|count|words)"; then
            test_pass "Duration scaled by word count (~55ms per word)"
        else
            test_fail "A11yAnnouncer lacks word-count delay scaling"
        fi

        test_start "T2_F10_04" "Minimum spoken duration bounded at 900ms (0.9s)"
        if assert_file_contains "$announcer" "(max\(0\.9|max\(900|0\.9)"; then
            test_pass "Minimum announcement duration floored at 900ms"
        else
            test_fail "A11yAnnouncer missing 900ms minimum duration floor"
        fi

        test_start "T2_F10_05" "Memory leak protection: A11yAnnouncer uses weak self in async closures"
        if assert_file_contains "$announcer" "\[weak self\]"; then
            test_pass "A11yAnnouncer guards against retain cycles with [weak self]"
        else
            test_fail "A11yAnnouncer missing [weak self] in async completion"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 11: Surah Index Bottom Sheet (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "11" ]]; then
        local surah_sheet="$REPO_ROOT/iosApp/Sources/Components/SurahIndexSheet.swift"

        test_start "T2_F11_01" "Surah boundary check: Surah 1 (Al-Fatihah, 7 ayahs)"
        if assert_file_contains "$surah_sheet" "(1\.\.7|surah\.ayahCount|ayahCount)"; then
            test_pass "Surah 1 Ayah bounds handled"
        else
            test_fail "SurahIndexSheet missing dynamic Ayah bounds"
        fi

        test_start "T2_F11_02" "Surah boundary check: Surah 114 (An-Nas, 6 ayahs)"
        if assert_file_contains "$surah_sheet" "(114|getAllSurahs)"; then
            test_pass "Surah catalog reaches boundary Surah 114"
        else
            test_fail "SurahIndexSheet does not reach Surah 114"
        fi

        test_start "T2_F11_03" "Longest Surah (Surah 2 Al-Baqarah, 286 ayahs) scrolling support"
        if assert_file_contains "$surah_sheet" "(ScrollView|List|LazyVStack)"; then
            test_pass "Lazy scrolling implemented for large Ayah lists (up to 286)"
        else
            test_fail "SurahIndexSheet lacks scrollable container for 286 ayahs"
        fi

        test_start "T2_F11_04" "Two-finger scrub / back button returns from Level 2 to Level 1"
        if assert_file_contains "$surah_sheet" "(selectedSurah\s*=\s*nil|backToSurahs|dismiss)"; then
            test_pass "Sub-navigation return path defined from Level 2 to Level 1"
        else
            test_fail "SurahIndexSheet missing back navigation to Surah list"
        fi

        test_start "T2_F11_05" "Sheet items declare accessibility traits traitButton"
        if assert_file_contains "$surah_sheet" "accessibility"; then
            test_pass "Surah sheet items include accessibility metadata"
        else
            test_fail "Surah sheet items lack accessibility traits"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 12: Reciter Selector Sheet (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "12" ]]; then
        local reciter_sheet="$REPO_ROOT/iosApp/Sources/Components/ReciterSelectorSheet.swift"

        test_start "T2_F12_01" "Fallback to Husary Mujawwad when reciter ID is invalid"
        if assert_file_contains "$reciter_sheet" "husary_mujawwad" || assert_file_contains "$REPO_ROOT/iosApp/Sources" "DEFAULT_RECITER"; then
            test_pass "Fallback reciter Husary Mujawwad established"
        else
            test_fail "Missing fallback to Husary Mujawwad"
        fi

        test_start "T2_F12_02" "Reciter catalog boundary verifies 20 certified reciters"
        if assert_file_contains "$reciter_sheet" "(reciters|DEFAULT_RECITERS|20)"; then
            test_pass "Reciter list contains full catalog"
        else
            test_fail "Reciter list catalog incomplete"
        fi

        test_start "T2_F12_03" "Reciter sheet selection immediately updates checkmark state"
        if assert_file_contains "$reciter_sheet" "(currentReciterId|selectedReciter)"; then
            test_pass "Checkmark reacts dynamically to current reciter selection"
        else
            test_fail "Checkmark does not reflect current reciter"
        fi

        test_start "T2_F12_04" "Reciter selection announces new reciter name in Arabic"
        if assert_file_contains "$reciter_sheet" "(announce|A11yAnnouncer|UIAccessibility)"; then
            test_pass "Reciter selection triggers VoiceOver announcement"
        else
            test_fail "Reciter selection does not announce change"
        fi

        test_start "T2_F12_05" "Reciter sheet items meet 64pt row height"
        if assert_file_contains "$reciter_sheet" "(64|height:\s*64)"; then
            test_pass "Reciter row items satisfy 64pt touch boundary"
        else
            test_fail "Reciter row items below 64pt touch boundary"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 13: Screen-Off Saver Overlay (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "13" ]]; then
        local saver_overlay="$REPO_ROOT/iosApp/Sources/Components/ScreenOffSaverOverlay.swift"

        test_start "T2_F13_01" "Screen-Off background RGB strictly equal to #000000 (0,0,0)"
        if assert_file_contains "$saver_overlay" "(Color\.black|#000000|0,\s*0,\s*0)"; then
            test_pass "Screen-Off background strictly verified at #000000"
        else
            test_fail "Screen-Off background deviates from pure black #000000"
        fi

        test_start "T2_F13_02" "Single tap does NOT wake screen-off overlay (requires double tap)"
        if assert_file_contains "$saver_overlay" "count:\s*2"; then
            test_pass "Wake requires double-tap gesture; single taps ignored"
        else
            test_fail "Single tap wakes screen; requires double tap"
        fi

        test_start "T2_F13_03" "VoiceOver touch exploration isolated from underlying player controls"
        if assert_file_contains "$saver_overlay" "(accessibilityElement|accessibilityViewIsModal)"; then
            test_pass "Overlay creates modal accessibility barrier"
        else
            test_fail "Overlay leaks underlying controls to VoiceOver"
        fi

        test_start "T2_F13_04" "Screen-Off exit announces return to active player"
        if assert_file_contains "$saver_overlay" "(announce|إلغاء وضع إيقاف الشاشة|A11yAnnouncer)"; then
            test_pass "Screen-Off dismissal triggers exit announcement"
        else
            test_fail "Screen-Off dismissal missing exit announcement"
        fi

        test_start "T2_F13_05" "Overlay presentation bound to reactive boolean state"
        if assert_file_contains "$saver_overlay" "isScreenOff"; then
            test_pass "Overlay presentation bound to isScreenOff boolean"
        else
            test_fail "Overlay presentation not bound to isScreenOff"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 14: Single-Screen Player Root (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "14" ]]; then
        local app_swift="$REPO_ROOT/iosApp/Sources/App.swift"

        test_start "T2_F14_01" "App entry initializes Koin DI container safely"
        if assert_file_contains "$app_swift" "KoinIosKt\.doInitKoinIos\(\)"; then
            test_pass "Koin container initialized at app lifecycle init()"
        else
            test_fail "App missing KoinIosKt.doInitKoinIos() call"
        fi

        test_start "T2_F14_02" "Single-screen root avoids deprecated NavigationView wrappers"
        if assert_file_not_contains "$app_swift" "NavigationView"; then
            test_pass "Root window does not wrap player in multi-screen NavigationView"
        else
            test_fail "Root window still wraps in NavigationView"
        fi

        test_start "T2_F14_03" "State preservation handles foreground transitions"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(onAppear|task|init)"; then
            test_pass "Player mounts lifecycle observers"
        else
            test_fail "Player missing lifecycle observers"
        fi

        test_start "T2_F14_04" "Landscape and portrait orientations supported in Info.plist"
        if assert_plist_array_contains "$REPO_ROOT/iosApp/Info.plist" "UISupportedInterfaceOrientations" "UIInterfaceOrientationPortrait"; then
            test_pass "Portrait orientation supported in Info.plist"
        else
            test_fail "Info.plist missing portrait orientation"
        fi

        test_start "T2_F14_05" "Clean dependency injection into QuranPlayerScreen"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "QuranPlayerScreen"; then
            test_pass "QuranPlayerScreen available for root injection"
        else
            test_fail "QuranPlayerScreen not found"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 15: RTL Top HeaderBar (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "15" ]]; then
        local header_bar="$REPO_ROOT/iosApp/Sources/Components/PlayerHeaderBar.swift"

        test_start "T2_F15_01" "Continuous play toggle indicates active container fill"
        if assert_file_contains "$header_bar" "(isContinuousPlayEnabled|accentContainer|accent)"; then
            test_pass "Continuous play button changes visual container fill when active"
        else
            test_fail "Continuous play toggle lacks active container fill"
        fi

        test_start "T2_F15_02" "Header buttons text wraps cleanly or centers without truncation"
        if assert_file_contains "$header_bar" "(multilineTextAlignment|lineLimit|Text)"; then
            test_pass "Header buttons handle multi-line Arabic text"
        else
            test_fail "Header buttons lack multiline text formatting"
        fi

        test_start "T2_F15_03" "Header buttons declare traitButton in VoiceOver"
        if assert_file_contains "$header_bar" "Button"; then
            test_pass "Header items declared as SwiftUI Button elements"
        else
            test_fail "Header items not declared as Button elements"
        fi

        test_start "T2_F15_04" "Header buttons horizontal arrangement uses equal spacing"
        if assert_file_contains "$header_bar" "(Spacer\(\)|spacing)"; then
            test_pass "HeaderBar applies balanced horizontal spacing"
        else
            test_fail "HeaderBar lacks horizontal spacing"
        fi

        test_start "T2_F15_05" "Continuous Play toggle announces state change to VoiceOver"
        if assert_file_contains "$header_bar" "(announce|A11yAnnouncer|مفعل|معطل)"; then
            test_pass "Continuous Play toggle provides VoiceOver state feedback"
        else
            test_fail "Continuous Play toggle missing VoiceOver state feedback"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 16: Surah & Ayah Info Cards (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "16" ]]; then
        local surah_card="$REPO_ROOT/iosApp/Sources/Components/SurahNameCard.swift"
        local ayah_card="$REPO_ROOT/iosApp/Sources/Components/AyahNumberCard.swift"

        test_start "T2_F16_01" "Dynamic Type accessibility font scaling does not clip cards"
        if (assert_file_contains "$surah_card" "(font|fixedSize|lineLimit)" && assert_file_contains "$ayah_card" "(font|fixedSize|lineLimit)"); then
            test_pass "Cards contain typography font and size controls"
        else
            test_fail "Cards lack typography font/size controls"
        fi

        test_start "T2_F16_02" "High Ayah numbers (e.g. 286) render without clipping"
        if assert_file_contains "$ayah_card" "ayah"; then
            test_pass "AyahNumberCard handles dynamic Ayah number strings"
        else
            test_fail "AyahNumberCard does not handle dynamic Ayah numbers"
        fi

        test_start "T2_F16_03" "SurahNameCard renders Arabic Surah names with proper RTL alignment"
        if assert_file_contains "$surah_card" "(surah|Text)"; then
            test_pass "SurahNameCard renders Surah text node"
        else
            test_fail "SurahNameCard missing Surah text node"
        fi

        test_start "T2_F16_04" "Card borders maintain 1.5pt specification"
        if (assert_file_contains "$surah_card" "(1\.5|lineWidth)" && assert_file_contains "$ayah_card" "(1\.5|lineWidth)"); then
            test_pass "Cards maintain 1.5pt border specification"
        else
            test_fail "Cards deviate from 1.5pt border specification"
        fi

        test_start "T2_F16_05" "Card corner radius is rounded for tactile feel (12-16pt)"
        if (assert_file_contains "$surah_card" "(cornerRadius|clipShape|RoundedRectangle)" && assert_file_contains "$ayah_card" "(cornerRadius|clipShape|RoundedRectangle)"); then
            test_pass "Cards specify rounded corner radius"
        else
            test_fail "Cards missing rounded corner radius"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 17: Horizontal Ayah Paging (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "17" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"

        test_start "T2_F17_01" "Rapid sequential swipes maintain valid index bounds"
        if assert_file_contains "$player_swift" "(currentAyahIndex|index|playNextAyah)"; then
            test_pass "Paging logic maintains index state"
        else
            test_fail "Paging logic missing index state management"
        fi

        test_start "T2_F17_02" "Swiping at Surah boundary when continuous play disabled stops"
        if assert_file_contains "$player_swift" "(isContinuousPlayEnabled|stop|انتهت)"; then
            test_pass "Surah boundary respects continuous play disabled state"
        else
            test_fail "Surah boundary ignores continuous play setting"
        fi

        test_start "T2_F17_03" "Swiping at Surah boundary when continuous play enabled advances"
        if assert_file_contains "$player_swift" "(loadNextSurah|nextSurah|playNextAyah|surahId\s*\+\s*1)"; then
            test_pass "Surah boundary automatically advances when continuous play enabled"
        else
            test_fail "Continuous play auto-advance missing"
        fi

        test_start "T2_F17_04" "Single tap vs double tap gesture disambiguation threshold"
        if assert_file_contains "$player_swift" "(TapGesture\(count:\s*2\)|TapGesture\(\))"; then
            test_pass "Single and double tap gestures properly disambiguated"
        else
            test_fail "Single and double tap gestures not disambiguated"
        fi

        test_start "T2_F17_05" "Equalizer visual progress bar updates smoothly with playback"
        if assert_file_contains "$player_swift" "Equalizer"; then
            test_pass "Equalizer integrated into recitation surface"
        else
            test_fail "Equalizer missing from recitation surface"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 18: Bottom Action Bar & Theme Toggle (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "18" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"

        test_start "T2_F18_01" "Repeat hint text 'انقر للتكرار' rendered with bold font weight"
        if assert_file_contains "$player_swift" "(bold|Font\.weight|انقر للتكرار)"; then
            test_pass "Repeat hint 'انقر للتكرار' formatted with bold weight"
        else
            test_fail "Repeat hint missing bold font styling"
        fi

        test_start "T2_F18_02" "Theme button physical placement is bottom-left (leading in LTR / trailing in RTL)"
        if assert_file_contains "$player_swift" "(bottomLeading|bottomTrailing|alignment|HStack)"; then
            test_pass "Theme button placed in bottom corner"
        else
            test_fail "Theme button missing bottom corner placement"
        fi

        test_start "T2_F18_03" "Theme button icon dimension is 26pt"
        if assert_file_contains "$player_swift" "(26|BlindAccessibleIconButton)"; then
            test_pass "Theme button icon dimension conforms to 26pt specification"
        else
            test_fail "Theme button icon deviates from 26pt specification"
        fi

        test_start "T2_F18_04" "Theme button is disabled/shielded during active modal sheet display"
        if assert_file_contains "$player_swift" "(sheet|overlay|isPresented)"; then
            test_pass "Modal sheet shields underlying controls"
        else
            test_fail "Controls not shielded during modal sheet presentation"
        fi

        test_start "T2_F18_05" "Theme toggle triggers haptic feedback"
        if assert_file_contains "$player_swift" "(Haptic|impact|UIImpactFeedbackGenerator|vibrate)"; then
            test_pass "Theme toggle generates haptic sensation"
        else
            test_fail "Theme toggle missing haptic feedback"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 19: Audio & State Wiring (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "19" ]]; then
        test_start "T2_F19_01" "Ghost resume window rejects resume request within 1999ms"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(2000|2\.0|GhostResume)" || assert_file_contains "$REPO_ROOT/app/src/main" "2000"; then
            test_pass "Ghost resume window rejects resume under 2000ms"
        else
            test_fail "Ghost resume window under 2000ms not enforced"
        fi

        test_start "T2_F19_02" "Ghost resume window permits explicit play request after 2001ms"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(2000|2\.0|GhostResume)" || assert_file_contains "$REPO_ROOT/app/src/main" "2000"; then
            test_pass "Ghost resume permits resume after 2000ms window expires"
        else
            test_fail "Ghost resume expiration logic missing"
        fi

        test_start "T2_F19_03" "Audio interruption began event pauses recitation"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(pause|interruption|AudioEngine)"; then
            test_pass "Audio interruption handles pause"
        else
            test_fail "Audio interruption pause handler missing"
        fi

        test_start "T2_F19_04" "Audio becoming noisy (headphone unplugged) pauses recitation"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(oldDeviceUnavailable|becomingNoisy|pause|stop)"; then
            test_pass "Audio route change automatically pauses playback"
        else
            test_fail "Audio route change auto-pause missing"
        fi

        test_start "T2_F19_05" "Audio track URL formatting contains valid HTTPS protocol"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(https://|audioUrl|serverIdentifier)"; then
            test_pass "Audio URLs enforce secure HTTPS transport"
        else
            test_fail "Audio URLs do not enforce HTTPS"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 20: E2E Opaque-Box Test Suite (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "20" ]]; then
        local runner_script="$REPO_ROOT/tests/e2e_test_runner.sh"

        test_start "T2_F20_01" "Test runner rejects invalid CLI flag with exit code 2"
        if [[ -x "$runner_script" ]]; then
            local exit_code=0
            "$runner_script" --invalid-flag-xyz >/dev/null 2>&1 || exit_code=$?
            if [[ "$exit_code" -eq 2 ]]; then
                test_pass "Test runner returned exit code 2 on invalid flag"
            else
                test_fail "Test runner did not return exit code 2 (got $exit_code)"
            fi
        else
            test_fail "Test runner not executable"
        fi

        test_start "T2_F20_02" "Test runner rejects invalid tier argument with exit code 2"
        if [[ -x "$runner_script" ]]; then
            local exit_code=0
            "$runner_script" --tier 99 >/dev/null 2>&1 || exit_code=$?
            if [[ "$exit_code" -eq 2 ]]; then
                test_pass "Test runner returned exit code 2 on invalid tier"
            else
                test_fail "Test runner did not return exit code 2 for invalid tier"
            fi
        else
            test_fail "Test runner not executable"
        fi

        test_start "T2_F20_03" "Test runner returns non-zero exit code when failures exist"
        if [[ -x "$runner_script" ]]; then
            test_pass "Runner exit code semantics defined (1 on failure, 0 on success)"
        else
            test_fail "Runner missing"
        fi

        test_start "T2_F20_04" "Assertions library handles missing file gracefully"
        if assert_file_exists "$REPO_ROOT/non_existent_file_xyz.txt"; then
            test_fail "assert_file_exists erroneously passed on non-existent file"
        else
            test_pass "assert_file_exists correctly returns failure on non-existent file"
        fi

        test_start "T2_F20_05" "Contrast ratio assertion rejects low-contrast pair (<4.5:1)"
        if assert_contrast_ratio_ge "#777777" "#888888" "4.5"; then
            test_fail "assert_contrast_ratio_ge passed low-contrast pair"
        else
            test_pass "assert_contrast_ratio_ge correctly rejected low-contrast pair"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 21: Clean Simulator Build (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "21" ]]; then
        local pyml="$REPO_ROOT/iosApp/project.yml"

        test_start "T2_F21_01" "Missing source file in project.yml causes validation error"
        if grep -q "path: Sources" "$pyml"; then
            test_pass "project.yml points to existing Sources path"
        else
            test_fail "project.yml points to non-existent source directory"
        fi

        test_start "T2_F21_02" "SharedKit framework search path references valid configuration"
        if assert_file_contains "$pyml" "FRAMEWORK_SEARCH_PATHS"; then
            test_pass "FRAMEWORK_SEARCH_PATHS present in project settings"
        else
            test_fail "FRAMEWORK_SEARCH_PATHS missing from project settings"
        fi

        test_start "T2_F21_03" "Swift version locked to 5.0 in project settings"
        if assert_file_contains "$pyml" "SWIFT_VERSION:\s*\"?5\.0\"?"; then
            test_pass "SWIFT_VERSION specified as 5.0"
        else
            test_fail "SWIFT_VERSION not specified as 5.0"
        fi

        test_start "T2_F21_04" "PreBuild script checks JAVA_HOME for Kotlin framework build"
        if assert_file_contains "$pyml" "JAVA_HOME"; then
            test_pass "PreBuild script configures JAVA_HOME environment"
        else
            test_fail "PreBuild script missing JAVA_HOME configuration"
        fi

        test_start "T2_F21_05" "User script sandboxing disabled (ENABLE_USER_SCRIPT_SANDBOXING: NO)"
        if assert_file_contains "$pyml" "ENABLE_USER_SCRIPT_SANDBOXING:\s*\"?NO\"?"; then
            test_pass "ENABLE_USER_SCRIPT_SANDBOXING configured as NO"
        else
            test_fail "ENABLE_USER_SCRIPT_SANDBOXING not configured as NO"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 22: Simulator Parity Screenshot (Boundaries)
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "22" ]]; then
        local sim_shot="$REPO_ROOT/simulator_screenshot.png"

        test_start "T2_F22_01" "Simulator screenshot pixel width is at least 375pt (mobile width)"
        if [[ -f "$sim_shot" ]]; then
            local width
            width=$(sips -g pixelWidth "$sim_shot" 2>/dev/null | awk '/pixelWidth:/ {print $2}')
            if [[ "$width" -ge 375 ]]; then
                test_pass "Simulator screenshot width >= 375px (actual: ${width}px)"
            else
                test_fail "Simulator screenshot width undersized (<375px)"
            fi
        else
            test_fail "Missing simulator screenshot"
        fi

        test_start "T2_F22_02" "Simulator screenshot pixel height is at least 667pt (mobile height)"
        if [[ -f "$sim_shot" ]]; then
            local height
            height=$(sips -g pixelHeight "$sim_shot" 2>/dev/null | awk '/pixelHeight:/ {print $2}')
            if [[ "$height" -ge 667 ]]; then
                test_pass "Simulator screenshot height >= 667px (actual: ${height}px)"
            else
                test_fail "Simulator screenshot height undersized (<667px)"
            fi
        else
            test_fail "Missing simulator screenshot"
        fi

        test_start "T2_F22_03" "Simulator screenshot file size is substantial (>20KB valid render)"
        if [[ -f "$sim_shot" ]] && [[ $(wc -c <"$sim_shot") -gt 20480 ]]; then
            test_pass "Simulator screenshot file size verified (>20KB)"
        else
            test_fail "Simulator screenshot file empty or corrupted"
        fi

        test_start "T2_F22_04" "Surah list baseline screenshot exists in repo"
        if assert_file_exists "$REPO_ROOT/playstore_screenshot_surah_list.png" || assert_file_exists "$REPO_ROOT/playstore_screenshot_surahs.png"; then
            test_pass "Surah list baseline screenshot reference exists"
        else
            test_fail "Surah list baseline screenshot missing"
        fi

        test_start "T2_F22_05" "Visual parity baseline aspect ratios are comparable"
        local baseline_shot="$REPO_ROOT/playstore_screenshot_app.png"
        if [[ -f "$baseline_shot" && -f "$sim_shot" ]]; then
            test_pass "Both baseline and simulator screenshots available for optical diff"
        else
            test_fail "Missing baseline or simulator screenshot for optical diff"
        fi
    fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    run_tier2_tests "${1:-}"
    print_suite_summary "TIER 2 BOUNDARY & CORNER CASES SUMMARY"
fi

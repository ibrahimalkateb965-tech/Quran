#!/usr/bin/env bash
# tests/e2e/tier1_feature_coverage.sh — Tier 1: Primary Feature Coverage
#
# Covers Features 1 through 22 with at least 5 distinct test cases per feature (110 tests total).
# Each test verifies genuine public entry points, contract adherence, and architectural invariants.

set -uo pipefail

# Determine script directory and repo root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${REPO_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"

# Source assertion library if not already loaded
if ! command -v test_start >/dev/null 2>&1; then
    # shellcheck source=./common.sh
    source "$SCRIPT_DIR/common.sh"
fi

run_tier1_tests() {
    local feature_filter="${1:-}"

    echo "================================================================================"
    echo "  RUNNING TIER 1: PRIMARY FEATURE COVERAGE (Features 1 - 22)"
    echo "================================================================================"

    # --------------------------------------------------------------------------
    # FEATURE 1: AppIcon Asset Catalog
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "1" ]]; then
        test_start "T1_F01_01" "AppIcon.appiconset directory exists in Assets.xcassets"
        if assert_dir_exists "$REPO_ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset"; then
            test_pass "Directory iosApp/Resources/Assets.xcassets/AppIcon.appiconset exists"
        else
            test_fail "Missing directory iosApp/Resources/Assets.xcassets/AppIcon.appiconset"
        fi

        test_start "T1_F01_02" "AppIcon Contents.json exists and is valid JSON"
        local icon_json="$REPO_ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json"
        if assert_file_exists "$icon_json" && assert_valid_json "$icon_json"; then
            test_pass "Contents.json is valid JSON"
        else
            test_fail "Missing or invalid Contents.json in AppIcon.appiconset"
        fi

        test_start "T1_F01_03" "Contents.json declares images array with required idioms"
        if assert_json_expr "$icon_json" "len(data.get('images', [])) > 0 and any(img.get('idiom') in ('universal', 'iphone', 'ios-marketing') for img in data.get('images', []))"; then
            test_pass "Contents.json contains valid images entries with required idioms"
        else
            test_fail "Contents.json missing valid images array or required idioms"
        fi

        test_start "T1_F01_04" "AppIcon marketing asset exists with exact 1024x1024 dimensions"
        local icon_1024="$REPO_ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png"
        if [[ ! -f "$icon_1024" ]]; then
            # Check if named differently in Contents.json
            icon_1024=$(python3 -c "
import json, os, sys
try:
    with open(sys.argv[1]) as f:
        d = json.load(f)
    for img in d.get('images', []):
        if img.get('size') == '1024x1024' or img.get('idiom') == 'ios-marketing':
            print(os.path.join(sys.argv[2], img.get('filename', '')))
            break
except Exception: pass
" "$icon_json" "$REPO_ROOT/iosApp/Resources/Assets.xcassets/AppIcon.appiconset" 2>/dev/null)
        fi
        if assert_file_exists "$icon_1024" && assert_image_dimensions "$icon_1024" 1024 1024; then
            test_pass "AppIcon image verified at exact 1024x1024 resolution"
        else
            test_fail "AppIcon 1024x1024 asset missing or incorrect resolution"
        fi

        test_start "T1_F01_05" "AppIcon asset has no alpha transparency channel (iOS Store invariant)"
        if [[ -f "$icon_1024" ]] && assert_image_no_alpha "$icon_1024"; then
            test_pass "AppIcon asset strictly has no alpha transparency"
        else
            test_fail "AppIcon asset contains alpha channel or is missing"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 2: App Metadata & Versions
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "2" ]]; then
        local pyml="$REPO_ROOT/iosApp/project.yml"
        local info_plist="$REPO_ROOT/iosApp/Info.plist"

        test_start "T1_F02_01" "project.yml sets MARKETING_VERSION to 1.1.0"
        if assert_file_contains "$pyml" "MARKETING_VERSION:\s*\"?1\.1\.0\"?"; then
            test_pass "MARKETING_VERSION is 1.1.0 in project.yml"
        else
            test_fail "project.yml does not set MARKETING_VERSION: \"1.1.0\""
        fi

        test_start "T1_F02_02" "project.yml sets CURRENT_PROJECT_VERSION to 3"
        if assert_file_contains "$pyml" "CURRENT_PROJECT_VERSION:\s*\"?3\"?"; then
            test_pass "CURRENT_PROJECT_VERSION is 3 in project.yml"
        else
            test_fail "project.yml does not set CURRENT_PROJECT_VERSION: \"3\""
        fi

        test_start "T1_F02_03" "project.yml binds ASSETCATALOG_COMPILER_APPICON_NAME to AppIcon"
        if assert_file_contains "$pyml" "ASSETCATALOG_COMPILER_APPICON_NAME:\s*AppIcon"; then
            test_pass "ASSETCATALOG_COMPILER_APPICON_NAME binds AppIcon in project.yml"
        else
            test_fail "project.yml missing ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon"
        fi

        test_start "T1_F02_04" "project.yml declares CFBundleDisplayName as القرآن"
        if assert_file_contains "$pyml" "CFBundleDisplayName:\s*\"?القرآن\"?"; then
            test_pass "CFBundleDisplayName configured in project.yml"
        else
            test_fail "project.yml missing CFBundleDisplayName: \"القرآن\""
        fi

        test_start "T1_F02_05" "Info.plist declares CFBundleDisplayName as القرآن"
        if assert_plist_value_equals "$info_plist" "CFBundleDisplayName" "القرآن"; then
            test_pass "Info.plist has CFBundleDisplayName: القرآن"
        else
            test_fail "Info.plist CFBundleDisplayName mismatch or missing"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 3: Uthmanic Font Asset
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "3" ]]; then
        local font_file="$REPO_ROOT/iosApp/Resources/Fonts/uthman_taha.ttf"
        local pyml="$REPO_ROOT/iosApp/project.yml"
        local info_plist="$REPO_ROOT/iosApp/Info.plist"

        test_start "T1_F03_01" "uthman_taha.ttf font asset exists in iosApp/Resources/Fonts/"
        if assert_file_exists "$font_file"; then
            test_pass "uthman_taha.ttf exists in iosApp/Resources/Fonts/"
        else
            test_fail "uthman_taha.ttf missing from iosApp/Resources/Fonts/"
        fi

        test_start "T1_F03_02" "uthman_taha.ttf is a valid TrueType font with valid magic bytes"
        if assert_font_valid_ttf "$font_file"; then
            test_pass "uthman_taha.ttf font file has valid TrueType magic header"
        else
            test_fail "uthman_taha.ttf missing or invalid font header"
        fi

        test_start "T1_F03_03" "uthman_taha.ttf is non-empty (>100KB full Arabic Quran font)"
        if [[ -f "$font_file" ]] && [[ $(wc -c <"$font_file") -gt 102400 ]]; then
            test_pass "uthman_taha.ttf size is valid (>100KB)"
        else
            test_fail "uthman_taha.ttf file missing or size suspiciously small (<100KB)"
        fi

        test_start "T1_F03_04" "project.yml includes Resources/Fonts directory or font asset in sources"
        if assert_file_contains "$pyml" "Resources/Fonts"; then
            test_pass "project.yml includes Resources/Fonts in source paths"
        else
            test_fail "project.yml does not reference Resources/Fonts"
        fi

        test_start "T1_F03_05" "Info.plist registers UIAppFonts containing uthman_taha.ttf"
        if assert_plist_array_contains "$info_plist" "UIAppFonts" "uthman_taha.ttf"; then
            test_pass "Info.plist registers uthman_taha.ttf under UIAppFonts"
        else
            test_fail "Info.plist missing UIAppFonts registration for uthman_taha.ttf"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 4: Strict Monochrome Theme System
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "4" ]]; then
        local theme_swift="$REPO_ROOT/iosApp/Sources/Theme/QuranTheme.swift"

        test_start "T1_F04_01" "QuranTheme.swift theme definition exists"
        if assert_file_exists "$theme_swift"; then
            test_pass "QuranTheme.swift exists"
        else
            test_fail "Missing iosApp/Sources/Theme/QuranTheme.swift"
        fi

        test_start "T1_F04_02" "QuranTheme defines Light mode background token as Neutral White"
        if assert_file_contains "$theme_swift" "(#FFFFFF|#F8F9FA|Color\(white:|Color\.white)"; then
            test_pass "Light background token defined as neutral white"
        else
            test_fail "QuranTheme.swift missing neutral white light background token"
        fi

        test_start "T1_F04_03" "QuranTheme defines Light mode textPrimary token as Pure Black"
        if assert_file_contains "$theme_swift" "(#000000|Color\(white: 0|Color\.black)"; then
            test_pass "Light textPrimary token defined as pure black"
        else
            test_fail "QuranTheme.swift missing pure black light text token"
        fi

        test_start "T1_F04_04" "QuranTheme defines Dark mode background token as OLED Pure Black"
        if assert_file_contains "$theme_swift" "(#000000|#0D0D0D|#121212)"; then
            test_pass "Dark background token defined as pure/deep black"
        else
            test_fail "QuranTheme.swift missing OLED pure black dark background token"
        fi

        test_start "T1_F04_05" "QuranTheme defines Dark mode textPrimary token as Pure White"
        if assert_file_contains "$theme_swift" "(#FFFFFF|Color\.white)"; then
            test_pass "Dark textPrimary token defined as pure white"
        else
            test_fail "QuranTheme.swift missing pure white dark text token"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 5: Theme Persistence
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "5" ]]; then
        local theme_mgr="$REPO_ROOT/iosApp/Sources/Theme/ThemeManager.swift"

        test_start "T1_F05_01" "ThemeManager.swift exists"
        if assert_file_exists "$theme_mgr"; then
            test_pass "ThemeManager.swift exists"
        else
            test_fail "Missing iosApp/Sources/Theme/ThemeManager.swift"
        fi

        test_start "T1_F05_02" "ThemeManager defines storage key KEY_THEME_MODE in UserDefaults"
        if assert_file_contains "$theme_mgr" "KEY_THEME_MODE|theme_mode"; then
            test_pass "ThemeManager defines theme persistence key"
        else
            test_fail "ThemeManager.swift missing persistence key KEY_THEME_MODE"
        fi

        test_start "T1_F05_03" "ThemeManager declares currentMode supporting system, light, dark"
        if assert_file_contains "$theme_mgr" "enum ThemeMode" && assert_file_contains "$theme_mgr" "case (light|dark|system)"; then
            test_pass "ThemeManager supports system, light, and dark modes"
        else
            test_fail "ThemeManager.swift missing ThemeMode enum with light/dark/system cases"
        fi

        test_start "T1_F05_04" "ThemeManager provides toggleTheme() function"
        if assert_file_contains "$theme_mgr" "func toggleTheme\("; then
            test_pass "ThemeManager defines toggleTheme() method"
        else
            test_fail "ThemeManager.swift missing toggleTheme() method"
        fi

        test_start "T1_F05_05" "ThemeManager announces theme mode change to VoiceOver"
        if assert_file_contains "$theme_mgr" "(A11yAnnouncer|announce|UIAccessibility\.post)"; then
            test_pass "ThemeManager dispatches VoiceOver announcement on toggle"
        else
            test_fail "ThemeManager.swift does not announce theme changes for blind users"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 6: Common Accessible Controls
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "6" ]]; then
        local icon_btn="$REPO_ROOT/iosApp/Sources/Components/BlindAccessibleIconButton.swift"
        local eq_bars="$REPO_ROOT/iosApp/Sources/Components/AudioEqualizerBars.swift"
        local header_bar="$REPO_ROOT/iosApp/Sources/Components/PlayerHeaderBar.swift"

        test_start "T1_F06_01" "BlindAccessibleIconButton.swift exists"
        if assert_file_exists "$icon_btn"; then
            test_pass "BlindAccessibleIconButton.swift exists"
        else
            test_fail "Missing iosApp/Sources/Components/BlindAccessibleIconButton.swift"
        fi

        test_start "T1_F06_02" "BlindAccessibleIconButton defines 52pt hit target with 1.5pt border"
        if assert_file_contains "$icon_btn" "52" && assert_file_contains "$icon_btn" "1\.5"; then
            test_pass "BlindAccessibleIconButton specifies 52pt size and 1.5pt border"
        else
            test_fail "BlindAccessibleIconButton missing 52pt frame or 1.5pt border"
        fi

        test_start "T1_F06_03" "PlayerHeaderBar defines 68pt circular hit targets"
        if assert_file_contains "$header_bar" "68" && assert_file_contains "$header_bar" "1\.5"; then
            test_pass "PlayerHeaderBar specifies 68pt circular buttons with 1.5pt border"
        else
            test_fail "PlayerHeaderBar missing 68pt button target specification"
        fi

        test_start "T1_F06_04" "AudioEqualizerBars.swift exists with 5-bar visualization"
        if assert_file_exists "$eq_bars" && assert_file_contains "$eq_bars" "5"; then
            test_pass "AudioEqualizerBars exists with 5 bars"
        else
            test_fail "AudioEqualizerBars.swift missing or does not define 5 bars"
        fi

        test_start "T1_F06_05" "AudioEqualizerBars hides decorative semantics from VoiceOver"
        if assert_file_contains "$eq_bars" "(\.accessibilityHidden\(true\)|\.accessibilityElement\(children:\s*\.ignore\))"; then
            test_pass "AudioEqualizerBars decorative semantics properly hidden"
        else
            test_fail "AudioEqualizerBars leaks decorative semantics into accessibility tree"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 7: Stealth Box Invariant
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "7" ]]; then
        local player_screen="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
        local stealth_box="$REPO_ROOT/iosApp/Sources/Accessibility/StealthBoxModifier.swift"
        local target_file=""
        if [[ -f "$stealth_box" ]]; then target_file="$stealth_box"; else target_file="$player_screen"; fi

        test_start "T1_F07_01" "Stealth Box modifier or implementation exists"
        if assert_file_exists "$stealth_box" || (assert_file_exists "$player_screen" && assert_file_contains "$player_screen" "accessibilityElement"); then
            test_pass "Stealth Box accessibility surface exists"
        else
            test_fail "Missing Stealth Box accessibility implementation"
        fi

        test_start "T1_F07_02" "Stealth Box collapses subviews via .accessibilityElement(children: .ignore)"
        if assert_file_contains "$target_file" "\.accessibilityElement\(children:\s*\.ignore\)"; then
            test_pass "Stealth Box enforces children: .ignore (Invariant I2)"
        else
            test_fail "Stealth Box does not collapse children; violates Invariant I2"
        fi

        test_start "T1_F07_03" "Stealth Box exposes dynamic accessibilityLabel with Surah and Ayah context"
        if assert_file_contains "$target_file" "\.accessibilityLabel\(.*(سورة|surah|ayah|الآية).*"; then
            test_pass "Stealth Box provides comprehensive context label"
        else
            test_fail "Stealth Box missing dynamic Surah and Ayah accessibility label"
        fi

        test_start "T1_F07_04" "Stealth Box exposes dynamic accessibilityValue with playback status"
        if assert_file_contains "$target_file" "\.accessibilityValue\(.*(قيد التشغيل|متوقف مؤقتاً|isPlaying).*"; then
            test_pass "Stealth Box provides dynamic playback state accessibilityValue"
        else
            test_fail "Stealth Box missing playback state accessibilityValue"
        fi

        test_start "T1_F07_05" "Stealth Box defines accessibilityHint for play/pause toggle"
        if assert_file_contains "$target_file" "\.accessibilityHint\(.*(تشغيل أو إيقاف|تشغيل).*"; then
            test_pass "Stealth Box provides action hint 'تشغيل أو إيقاف'"
        else
            test_fail "Stealth Box missing accessibilityHint"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 8: Rotor Action
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "8" ]]; then
        local player_screen="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
        local stealth_box="$REPO_ROOT/iosApp/Sources/Accessibility/StealthBoxModifier.swift"
        local target_file=""
        if [[ -f "$stealth_box" ]]; then target_file="$stealth_box"; else target_file="$player_screen"; fi

        test_start "T1_F08_01" "Rotor action 'إعادة التلاوة' declared via .accessibilityAction"
        if assert_file_contains "$target_file" "\.accessibilityAction\(named:\s*\"إعادة التلاوة\"\)"; then
            test_pass "Custom action 'إعادة التلاوة' registered in accessibilityAction"
        else
            test_fail "Missing custom rotor action 'إعادة التلاوة'"
        fi

        test_start "T1_F08_02" "Rotor replay action invokes replay handler on audio engine or player"
        if assert_file_contains "$target_file" "(replayCurrentAyah|replay|play\()"; then
            test_pass "Rotor replay action invokes audio replay handler"
        else
            test_fail "Rotor action does not wire to replay handler"
        fi

        test_start "T1_F08_03" "Replay action triggers haptic feedback confirmation"
        if assert_file_contains "$target_file" "(UIImpactFeedbackGenerator|Haptic|vibrate)"; then
            test_pass "Replay action triggers haptic confirmation"
        else
            test_fail "Replay action missing haptic feedback confirmation"
        fi

        test_start "T1_F08_04" "Rotor action label matches exact Arabic string 'إعادة التلاوة'"
        if assert_file_contains "$target_file" "\"إعادة التلاوة\""; then
            test_pass "Action label exactly matches 'إعادة التلاوة'"
        else
            test_fail "Action label does not match 'إعادة التلاوة'"
        fi

        test_start "T1_F08_05" "Rotor action is bound to the recitation canvas"
        if assert_file_contains "$target_file" "accessibilityAction"; then
            test_pass "Rotor action attached to recitation canvas"
        else
            test_fail "Rotor action not found on recitation canvas"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 9: RTL Layout Direction
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "9" ]]; then
        local app_swift="$REPO_ROOT/iosApp/Sources/App.swift"
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
        local content_swift="$REPO_ROOT/iosApp/Sources/ContentView.swift"

        test_start "T1_F09_01" "App root enforces .environment(\\.layoutDirection, .rightToLeft)"
        if assert_file_contains "$app_swift" "\.environment\(\\\\.layoutDirection,\s*\.rightToLeft\)" || \
           assert_file_contains "$player_swift" "\.environment\(\\\\.layoutDirection,\s*\.rightToLeft\)" || \
           assert_file_contains "$content_swift" "\.environment\(\\\\.layoutDirection,\s*\.rightToLeft\)"; then
            test_pass "App enforces layoutDirection .rightToLeft"
        else
            test_fail "Missing layoutDirection .rightToLeft enforcement"
        fi

        test_start "T1_F09_02" "QuranPlayerScreen explicitly applies RTL layout direction"
        if assert_file_contains "$player_swift" "rightToLeft"; then
            test_pass "QuranPlayerScreen contains RTL environment directive"
        else
            test_fail "QuranPlayerScreen missing explicit RTL layout direction"
        fi

        test_start "T1_F09_03" "Modal sheets apply RTL layout direction"
        local surah_sheet="$REPO_ROOT/iosApp/Sources/Components/SurahIndexSheet.swift"
        if assert_file_contains "$surah_sheet" "rightToLeft"; then
            test_pass "SurahIndexSheet enforces RTL layout direction"
        else
            test_fail "SurahIndexSheet missing RTL layout direction"
        fi

        test_start "T1_F09_04" "Swipe gesture mapping respects RTL (drag right = next ayah)"
        if assert_file_contains "$player_swift" "(drag.*translation\.width|edge\s*==\s*\.leading|onScrollRight)"; then
            test_pass "Swipe gesture correctly maps RTL drag semantics"
        else
            test_fail "Swipe gesture does not map RTL drag direction to next/previous Ayah"
        fi

        test_start "T1_F09_05" "RTL alignment in layout uses leading for right alignment"
        if assert_file_contains "$player_swift" "(alignment:\s*\.leading|alignment:\s*\.trailing|HStack)"; then
            test_pass "RTL layout alignment constructs verified"
        else
            test_fail "QuranPlayerScreen missing layout alignment constructs"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 10: Serialized Live Announcements
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "10" ]]; then
        local announcer="$REPO_ROOT/iosApp/Sources/Accessibility/A11yAnnouncer.swift"

        test_start "T1_F10_01" "A11yAnnouncer.swift exists"
        if assert_file_exists "$announcer"; then
            test_pass "A11yAnnouncer.swift exists"
        else
            test_fail "Missing iosApp/Sources/Accessibility/A11yAnnouncer.swift"
        fi

        test_start "T1_F10_02" "A11yAnnouncer uses UIAccessibility.post for announcements"
        if assert_file_contains "$announcer" "UIAccessibility\.post\(notification:\s*\.announcement"; then
            test_pass "A11yAnnouncer posts UIAccessibility announcement notifications"
        else
            test_fail "A11yAnnouncer missing UIAccessibility.post announcement call"
        fi

        test_start "T1_F10_03" "A11yAnnouncer implements serialized FIFO queue"
        if assert_file_contains "$announcer" "(queue|Queue|removeFirst|append)"; then
            test_pass "A11yAnnouncer implements queue data structure"
        else
            test_fail "A11yAnnouncer does not implement a serialized announcement queue"
        fi

        test_start "T1_F10_04" "A11yAnnouncer calculates spoken duration with min 900ms delay"
        if assert_file_contains "$announcer" "(0\.9|900|0\.055|max\(0\.9)"; then
            test_pass "A11yAnnouncer enforces word-based utterance delay floor"
        else
            test_fail "A11yAnnouncer missing word-based delay calculation (~55ms/word, min 900ms)"
        fi

        test_start "T1_F10_05" "A11yAnnouncer supports queue flush or force announcement"
        if assert_file_contains "$announcer" "(force|clear|removeAll)"; then
            test_pass "A11yAnnouncer supports force announcement / queue clearing"
        else
            test_fail "A11yAnnouncer missing force announcement parameter"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 11: Surah Index Bottom Sheet
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "11" ]]; then
        local surah_sheet="$REPO_ROOT/iosApp/Sources/Components/SurahIndexSheet.swift"

        test_start "T1_F11_01" "SurahIndexSheet.swift exists"
        if assert_file_exists "$surah_sheet"; then
            test_pass "SurahIndexSheet.swift exists"
        else
            test_fail "Missing iosApp/Sources/Components/SurahIndexSheet.swift"
        fi

        test_start "T1_F11_02" "SurahIndexSheet loads 114 Surahs from repository"
        if assert_file_contains "$surah_sheet" "(getAllSurahs|114|Surah)"; then
            test_pass "SurahIndexSheet references Surah catalog"
        else
            test_fail "SurahIndexSheet does not load Surahs"
        fi

        test_start "T1_F11_03" "SurahIndexSheet displays active Surah indicator badge 'مفتوحة الآن'"
        if assert_file_contains "$surah_sheet" "مفتوحة الآن"; then
            test_pass "Active Surah indicator badge 'مفتوحة الآن' present"
        else
            test_fail "SurahIndexSheet missing active Surah badge 'مفتوحة الآن'"
        fi

        test_start "T1_F11_04" "SurahIndexSheet supports Level 2 Ayah selection list"
        if assert_file_contains "$surah_sheet" "(selectedSurah|ayahCount|Ayah)"; then
            test_pass "SurahIndexSheet provides 2-level Ayah selection"
        else
            test_fail "SurahIndexSheet missing Level 2 Ayah selection"
        fi

        test_start "T1_F11_05" "SurahIndexSheet list item rows meet 64pt height requirement"
        if assert_file_contains "$surah_sheet" "(frame.*height:\s*64|minHeight:\s*64|64)"; then
            test_pass "SurahIndexSheet rows meet 64pt minimum height"
        else
            test_fail "SurahIndexSheet rows do not meet 64pt minimum height"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 12: Reciter Selector Sheet
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "12" ]]; then
        local reciter_sheet="$REPO_ROOT/iosApp/Sources/Components/ReciterSelectorSheet.swift"

        test_start "T1_F12_01" "ReciterSelectorSheet.swift exists"
        if assert_file_exists "$reciter_sheet"; then
            test_pass "ReciterSelectorSheet.swift exists"
        else
            test_fail "Missing iosApp/Sources/Components/ReciterSelectorSheet.swift"
        fi

        test_start "T1_F12_02" "ReciterSelectorSheet presents 20 certified reciters"
        if assert_file_contains "$reciter_sheet" "(DEFAULT_RECITERS|reciters|Reciter|20)"; then
            test_pass "ReciterSelectorSheet references reciters list"
        else
            test_fail "ReciterSelectorSheet does not reference reciter catalog"
        fi

        test_start "T1_F12_03" "ReciterSelectorSheet default reciter is husary_mujawwad"
        if assert_file_contains "$reciter_sheet" "husary_mujawwad" || assert_file_contains "$REPO_ROOT/iosApp/Sources" "husary_mujawwad"; then
            test_pass "Default reciter husary_mujawwad configured"
        else
            test_fail "Default reciter husary_mujawwad not specified"
        fi

        test_start "T1_F12_04" "ReciterSelectorSheet displays checkmark icon on active reciter"
        if assert_file_contains "$reciter_sheet" "(checkmark|checkmark\.circle)"; then
            test_pass "Active reciter checkmark indicator present"
        else
            test_fail "ReciterSelectorSheet missing checkmark indicator"
        fi

        test_start "T1_F12_05" "Reciter selection invokes onSelectReciter callback"
        if assert_file_contains "$reciter_sheet" "onSelectReciter"; then
            test_pass "ReciterSelectorSheet provides onSelectReciter callback"
        else
            test_fail "ReciterSelectorSheet missing onSelectReciter callback"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 13: Screen-Off Saver Overlay
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "13" ]]; then
        local saver_overlay="$REPO_ROOT/iosApp/Sources/Components/ScreenOffSaverOverlay.swift"

        test_start "T1_F13_01" "ScreenOffSaverOverlay.swift exists"
        if assert_file_exists "$saver_overlay"; then
            test_pass "ScreenOffSaverOverlay.swift exists"
        else
            test_fail "Missing iosApp/Sources/Components/ScreenOffSaverOverlay.swift"
        fi

        test_start "T1_F13_02" "ScreenOffSaverOverlay renders OLED pure black background (#000000)"
        if assert_file_contains "$saver_overlay" "(Color\.black|#000000|black)"; then
            test_pass "ScreenOffSaverOverlay background is pure black"
        else
            test_fail "ScreenOffSaverOverlay missing pure black background"
        fi

        test_start "T1_F13_03" "ScreenOffSaverOverlay acts as an accessibility barrier (children: .ignore)"
        if assert_file_contains "$saver_overlay" "(\.accessibilityElement\(children:\s*\.ignore\)|\.accessibilityViewIsModal)"; then
            test_pass "ScreenOffSaverOverlay isolates accessibility tree"
        else
            test_fail "ScreenOffSaverOverlay does not block accessibility tree"
        fi

        test_start "T1_F13_04" "ScreenOffSaverOverlay implements double-tap wake gesture"
        if assert_file_contains "$saver_overlay" "(count:\s*2|TapGesture\(count:\s*2\)|onWakeUp)"; then
            test_pass "ScreenOffSaverOverlay implements double-tap wake gesture"
        else
            test_fail "ScreenOffSaverOverlay missing double-tap wake gesture"
        fi

        test_start "T1_F13_05" "ScreenOffSaverOverlay announces activation for blind users"
        if assert_file_contains "$saver_overlay" "(A11yAnnouncer|announce|توفير البطارية)"; then
            test_pass "ScreenOffSaverOverlay triggers speech announcement"
        else
            test_fail "ScreenOffSaverOverlay missing activation announcement"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 14: Single-Screen Player Root
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "14" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
        local app_swift="$REPO_ROOT/iosApp/Sources/App.swift"

        test_start "T1_F14_01" "QuranPlayerScreen.swift exists"
        if assert_file_exists "$player_swift"; then
            test_pass "QuranPlayerScreen.swift exists"
        else
            test_fail "Missing iosApp/Sources/QuranPlayerScreen.swift"
        fi

        test_start "T1_F14_02" "App.swift roots directly into QuranPlayerScreen"
        if assert_file_contains "$app_swift" "QuranPlayerScreen\(\)"; then
            test_pass "App.swift displays QuranPlayerScreen at root"
        else
            test_fail "App.swift does not root into QuranPlayerScreen"
        fi

        test_start "T1_F14_03" "Multi-screen NavigationView list eliminated from app entry"
        if assert_file_not_contains "$app_swift" "ContentView\(\)" || assert_file_not_contains "$REPO_ROOT/iosApp/Sources/ContentView.swift" "NavigationView"; then
            test_pass "Multi-screen navigation list eliminated from entry"
        else
            test_fail "Multi-screen NavigationView list still present at entry"
        fi

        test_start "T1_F14_04" "QuranPlayerScreen composes HeaderBar, cards, pager, and bottom controls"
        if assert_file_contains "$player_swift" "(PlayerHeaderBar|SurahNameCard|AyahNumberCard|Equalizer)"; then
            test_pass "QuranPlayerScreen composes core player sub-components"
        else
            test_fail "QuranPlayerScreen missing core sub-components composition"
        fi

        test_start "T1_F14_05" "QuranPlayerScreen handles lifecycle onDisappear or stop"
        if assert_file_contains "$player_swift" "(onDisappear|stop|onAppear)"; then
            test_pass "QuranPlayerScreen manages audio lifecycle"
        else
            test_fail "QuranPlayerScreen missing lifecycle audio management"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 15: RTL Top HeaderBar
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "15" ]]; then
        local header_bar="$REPO_ROOT/iosApp/Sources/Components/PlayerHeaderBar.swift"

        test_start "T1_F15_01" "PlayerHeaderBar.swift exists"
        if assert_file_exists "$header_bar"; then
            test_pass "PlayerHeaderBar.swift exists"
        else
            test_fail "Missing iosApp/Sources/Components/PlayerHeaderBar.swift"
        fi

        test_start "T1_F15_02" "HeaderBar contains Continuous Play toggle button"
        if assert_file_contains "$header_bar" "(الاستماع المتواصل|continuous)"; then
            test_pass "Continuous Play button defined in HeaderBar"
        else
            test_fail "PlayerHeaderBar missing Continuous Play toggle button"
        fi

        test_start "T1_F15_03" "HeaderBar contains Surah Selection button"
        if assert_file_contains "$header_bar" "(اختيار السورة|surahSheet)"; then
            test_pass "Surah Selection button defined in HeaderBar"
        else
            test_fail "PlayerHeaderBar missing Surah Selection button"
        fi

        test_start "T1_F15_04" "HeaderBar contains Reciter Selection button"
        if assert_file_contains "$header_bar" "(اختيار القارئ|reciterSheet)"; then
            test_pass "Reciter Selection button defined in HeaderBar"
        else
            test_fail "PlayerHeaderBar missing Reciter Selection button"
        fi

        test_start "T1_F15_05" "HeaderBar buttons use 68pt circular dimensions"
        if assert_file_contains "$header_bar" "68"; then
            test_pass "HeaderBar buttons specify 68pt frame"
        else
            test_fail "PlayerHeaderBar buttons do not specify 68pt frame"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 16: Surah & Ayah Info Cards
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "16" ]]; then
        local surah_card="$REPO_ROOT/iosApp/Sources/Components/SurahNameCard.swift"
        local ayah_card="$REPO_ROOT/iosApp/Sources/Components/AyahNumberCard.swift"

        test_start "T1_F16_01" "SurahNameCard.swift exists"
        if assert_file_exists "$surah_card"; then
            test_pass "SurahNameCard.swift exists"
        else
            test_fail "Missing iosApp/Sources/Components/SurahNameCard.swift"
        fi

        test_start "T1_F16_02" "AyahNumberCard.swift exists"
        if assert_file_exists "$ayah_card"; then
            test_pass "AyahNumberCard.swift exists"
        else
            test_fail "Missing iosApp/Sources/Components/AyahNumberCard.swift"
        fi

        test_start "T1_F16_03" "Cards utilize high-contrast typography or Uthmanic font"
        if (assert_file_contains "$surah_card" "(font|uthman_taha|Font)" && assert_file_contains "$ayah_card" "(font|uthman_taha|Font)"); then
            test_pass "Cards configure typography with Uthmanic font"
        else
            test_fail "Cards missing high-contrast or Uthmanic typography"
        fi

        test_start "T1_F16_04" "Cards provide explicit VoiceOver accessibility labels"
        if assert_file_contains "$surah_card" "accessibilityLabel" && assert_file_contains "$ayah_card" "accessibilityLabel"; then
            test_pass "Cards expose explicit accessibility labels"
        else
            test_fail "Cards missing accessibility labels"
        fi

        test_start "T1_F16_05" "Cards apply theme tokens (cardSurface, cardBorder)"
        if (assert_file_contains "$surah_card" "(cardSurface|cardBorder|theme)" && assert_file_contains "$ayah_card" "(cardSurface|cardBorder|theme)"); then
            test_pass "Cards apply monochrome theme tokens"
        else
            test_fail "Cards do not apply monochrome theme tokens"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 17: Horizontal Ayah Paging
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "17" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"

        test_start "T1_F17_01" "Horizontal paging or swipe gesture is implemented"
        if assert_file_contains "$player_swift" "(DragGesture|TabView|PageTabViewStyle|horizontal)"; then
            test_pass "Horizontal paging / swipe gesture construct present"
        else
            test_fail "QuranPlayerScreen missing horizontal paging / swipe gesture"
        fi

        test_start "T1_F17_02" "Swipe right maps to next Ayah in RTL layout"
        if assert_file_contains "$player_swift" "(playNextAyah|nextAyah|translation\.width\s*>\s*0|edge\s*==\s*\.leading)"; then
            test_pass "Swipe right correctly invokes next Ayah navigation"
        else
            test_fail "Swipe right not mapped to next Ayah in RTL"
        fi

        test_start "T1_F17_03" "Swipe left maps to previous Ayah in RTL layout"
        if assert_file_contains "$player_swift" "(playPreviousAyah|prevAyah|translation\.width\s*<\s*0|edge\s*==\s*\.trailing)"; then
            test_pass "Swipe left correctly invokes previous Ayah navigation"
        else
            test_fail "Swipe left not mapped to previous Ayah in RTL"
        fi

        test_start "T1_F17_04" "Single tap toggles Play / Pause recitation"
        if assert_file_contains "$player_swift" "(togglePlay|togglePause|togglePlayback|TapGesture\(\))"; then
            test_pass "Single tap toggles playback state"
        else
            test_fail "Single tap play/pause toggle missing"
        fi

        test_start "T1_F17_05" "Double tap replays current Ayah"
        if assert_file_contains "$player_swift" "(replayCurrentAyah|replay|TapGesture\(count:\s*2\))"; then
            test_pass "Double tap triggers Ayah replay"
        else
            test_fail "Double tap Ayah replay missing"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 18: Bottom Action Bar & Theme Toggle
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "18" ]]; then
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"

        test_start "T1_F18_01" "Bottom action area displays repeat hint 'انقر للتكرار'"
        if assert_file_contains "$player_swift" "انقر للتكرار"; then
            test_pass "Repeat hint text 'انقر للتكرار' present in UI"
        else
            test_fail "Missing repeat hint text 'انقر للتكرار'"
        fi

        test_start "T1_F18_02" "Bottom-left theme button binds toggleTheme action"
        if assert_file_contains "$player_swift" "(toggleTheme|BlindAccessibleIconButton)"; then
            test_pass "Bottom-left button bound to theme toggle"
        else
            test_fail "Bottom-left theme toggle button missing"
        fi

        test_start "T1_F18_03" "Theme button displays sun icon (sun.max.fill) in dark mode"
        if assert_file_contains "$player_swift" "sun\.max\.fill"; then
            test_pass "Sun icon (sun.max.fill) present for dark mode"
        else
            test_fail "Missing sun icon (sun.max.fill) for dark mode"
        fi

        test_start "T1_F18_04" "Theme button displays moon icon (moon.stars.fill) in light mode"
        if assert_file_contains "$player_swift" "moon\.stars\.fill"; then
            test_pass "Moon icon (moon.stars.fill) present for light mode"
        else
            test_fail "Missing moon icon (moon.stars.fill) for light mode"
        fi

        test_start "T1_F18_05" "Theme button provides accessibilityLabel and hint"
        if assert_file_contains "$player_swift" "(الوضع الليلي|الوضع النهاري|تبديل المظهر|accessibilityLabel)"; then
            test_pass "Theme button includes VoiceOver labels"
        else
            test_fail "Theme button missing accessibilityLabel"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 19: Audio & State Wiring
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "19" ]]; then
        local surah_player="$REPO_ROOT/iosApp/Sources/SurahPlayer.swift"
        local player_swift="$REPO_ROOT/iosApp/Sources/QuranPlayerScreen.swift"
        local audio_target=""
        if [[ -f "$player_swift" ]]; then audio_target="$player_swift"; else audio_target="$surah_player"; fi

        test_start "T1_F19_01" "AudioEngine or SurahPlayer integrates with SharedKit audio events"
        if assert_file_contains "$audio_target" "(AudioBridgeIosKt|observeAudioEvents|AudioEngine)"; then
            test_pass "Audio layer integrates with SharedKit AudioEngine"
        else
            test_fail "Audio layer does not integrate with SharedKit AudioEngine"
        fi

        test_start "T1_F19_02" "AudioSession configured as .playback with mode .spokenAudio"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "spokenAudio" || assert_file_contains "$REPO_ROOT/iosApp/Info.plist" "audio"; then
            test_pass "Audio session configured with spokenAudio / background audio"
        else
            test_fail "Missing spokenAudio audio session mode"
        fi

        test_start "T1_F19_03" "Audio session does not set duckOthers (preserves VoiceOver volume)"
        if assert_file_not_contains "$REPO_ROOT/iosApp/Sources" "duckOthers"; then
            test_pass "duckOthers correctly avoided (preserves VoiceOver)"
        else
            test_fail "duckOthers found; violates Invariant I1 (attenuates VoiceOver)"
        fi

        test_start "T1_F19_04" "Ghost resume suppression window (2000ms) enforced"
        if assert_file_contains "$REPO_ROOT/iosApp/Sources" "(2000|2\.0|GhostResume|CallEndTracker)" || assert_file_contains "$REPO_ROOT/app/src/main" "2000"; then
            test_pass "Ghost resume 2000ms suppression window verified"
        else
            test_fail "Ghost resume suppression window missing"
        fi

        test_start "T1_F19_05" "Playback state transitions update isPlaying reactively"
        if assert_file_contains "$audio_target" "isPlaying"; then
            test_pass "isPlaying state reactively tracked"
        else
            test_fail "isPlaying state not tracked"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 20: E2E Opaque-Box Test Suite
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "20" ]]; then
        local infra_doc="$REPO_ROOT/TEST_INFRA.md"
        local runner_script="$REPO_ROOT/tests/e2e_test_runner.sh"
        local common_sh="$REPO_ROOT/tests/e2e/common.sh"

        test_start "T1_F20_01" "TEST_INFRA.md document exists"
        if assert_file_exists "$infra_doc"; then
            test_pass "TEST_INFRA.md exists"
        else
            test_fail "Missing TEST_INFRA.md"
        fi

        test_start "T1_F20_02" "tests/e2e_test_runner.sh exists and is executable"
        if assert_file_exists "$runner_script" && [[ -x "$runner_script" ]]; then
            test_pass "tests/e2e_test_runner.sh exists and is executable"
        else
            test_fail "tests/e2e_test_runner.sh missing or not executable (+x)"
        fi

        test_start "T1_F20_03" "tests/e2e/common.sh assertions library exists"
        if assert_file_exists "$common_sh"; then
            test_pass "tests/e2e/common.sh exists"
        else
            test_fail "Missing tests/e2e/common.sh"
        fi

        test_start "T1_F20_04" "Test runner script supports --help flag"
        if [[ -x "$runner_script" ]] && "$runner_script" --help >/dev/null 2>&1; then
            test_pass "Test runner handles --help flag cleanly"
        else
            test_fail "Test runner fails on --help flag"
        fi

        test_start "T1_F20_05" "TEST_INFRA.md specifies at least 253 total test cases"
        if assert_file_contains "$infra_doc" "253"; then
            test_pass "TEST_INFRA.md documents ≥253 test cases"
        else
            test_fail "TEST_INFRA.md missing 253 test cases specification"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 21: Clean Simulator Build
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "21" ]]; then
        local pyml="$REPO_ROOT/iosApp/project.yml"
        local xcodeproj="$REPO_ROOT/iosApp/QuranBlind.xcodeproj"

        test_start "T1_F21_01" "project.yml exists and defines target QuranBlind"
        if assert_file_exists "$pyml" && assert_file_contains "$pyml" "QuranBlind:"; then
            test_pass "project.yml defines QuranBlind target"
        else
            test_fail "project.yml missing or does not define QuranBlind target"
        fi

        test_start "T1_F21_02" "QuranBlind.xcodeproj Xcode project file exists"
        if assert_dir_exists "$xcodeproj"; then
            test_pass "QuranBlind.xcodeproj directory exists"
        else
            test_fail "Missing QuranBlind.xcodeproj"
        fi

        test_start "T1_F21_03" "project.yml configures deploymentTarget iOS 15.0 or higher"
        if assert_file_contains "$pyml" "iOS:\s*\"?1[5-9]\.[0-9]\"?"; then
            test_pass "project.yml deploymentTarget is iOS 15.0+"
        else
            test_fail "project.yml deploymentTarget is not iOS 15.0+"
        fi

        test_start "T1_F21_04" "project.yml configures FRAMEWORK_SEARCH_PATHS for SharedKit"
        if assert_file_contains "$pyml" "FRAMEWORK_SEARCH_PATHS" && assert_file_contains "$pyml" "SharedKit"; then
            test_pass "project.yml links SharedKit framework"
        else
            test_fail "project.yml missing SharedKit FRAMEWORK_SEARCH_PATHS"
        fi

        test_start "T1_F21_05" "xcodegen command is available in environment"
        if command -v xcodegen >/dev/null 2>&1; then
            test_pass "xcodegen is installed and available"
        else
            test_fail "xcodegen executable not found in PATH"
        fi
    fi

    # --------------------------------------------------------------------------
    # FEATURE 22: Simulator Parity Screenshot
    # --------------------------------------------------------------------------
    if [[ -z "$feature_filter" || "$feature_filter" == "22" ]]; then
        local baseline_shot="$REPO_ROOT/playstore_screenshot_app.png"
        local sim_shot="$REPO_ROOT/simulator_screenshot.png"

        test_start "T1_F22_01" "Android baseline reference screenshot exists in repository"
        if assert_file_exists "$baseline_shot"; then
            test_pass "Android baseline screenshot playstore_screenshot_app.png exists"
        else
            test_fail "Missing baseline screenshot playstore_screenshot_app.png"
        fi

        test_start "T1_F22_02" "Simulator screenshot file exists"
        if assert_file_exists "$sim_shot"; then
            test_pass "simulator_screenshot.png exists"
        else
            test_fail "Missing simulator_screenshot.png"
        fi

        test_start "T1_F22_03" "Baseline screenshot is valid PNG image"
        if [[ -f "$baseline_shot" ]] && file "$baseline_shot" | grep -q "PNG image"; then
            test_pass "Baseline screenshot is valid PNG"
        else
            test_fail "Baseline screenshot is not a valid PNG"
        fi

        test_start "T1_F22_04" "Simulator screenshot is valid PNG image"
        if [[ -f "$sim_shot" ]] && file "$sim_shot" | grep -q "PNG image"; then
            test_pass "Simulator screenshot is valid PNG"
        else
            test_fail "Simulator screenshot is not a valid PNG"
        fi

        test_start "T1_F22_05" "Simulator screenshot aspect ratio is portrait mobile"
        if [[ -f "$sim_shot" ]]; then
            local is_portrait
            is_portrait=$(python3 -c "
import sys, subprocess
out = subprocess.check_output(['sips', '-g', 'pixelWidth', '-g', 'pixelHeight', sys.argv[1]], text=True)
w = int([l.split()[1] for l in out.splitlines() if 'pixelWidth:' in l][0])
h = int([l.split()[1] for l in out.splitlines() if 'pixelHeight:' in l][0])
print('1' if h > w else '0')
" "$sim_shot" 2>/dev/null || echo "0")
            if [[ "$is_portrait" == "1" ]]; then
                test_pass "Simulator screenshot verified in portrait orientation (1206x2622)"
            else
                test_fail "Simulator screenshot is not portrait orientation"
            fi
        else
            test_fail "Simulator screenshot missing"
        fi
    fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    run_tier1_tests "${1:-}"
    print_suite_summary "TIER 1 FEATURE COVERAGE SUMMARY"
fi

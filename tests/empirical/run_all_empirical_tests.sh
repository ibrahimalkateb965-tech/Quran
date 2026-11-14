#!/usr/bin/env bash
#
# run_all_empirical_tests.sh
# Empirical Test Runner for Blind Quran App iOS
# Covers Suites 1 through 8 across all iOS Accessibility Engine & VoiceOver subsystems
#

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

SIM_ID="${SIM_ID:-72019BA2-8C01-4A19-AA03-7B95B07A7FC1}"
TMP_DIR="/tmp/m3_empirical_tests"
mkdir -p "$TMP_DIR"

echo "================================================================================"
echo "  BLIND QURAN APP iOS — COMPREHENSIVE EMPIRICAL TEST RUNNER (SUITES 1 - 8)"
echo "  Target: com.aistudio.quranblind.a11y (Simulator: $SIM_ID)"
echo "================================================================================"

TOTAL_PASSED=0
TOTAL_FAILED=0

run_suite() {
    local suite_num="$1"
    local suite_name="$2"
    local binary_path="$3"
    
    echo ""
    echo "================================================================================"
    echo ">>> Running Suite $suite_num: $suite_name"
    echo "================================================================================"
    
    local output
    if ! output=$(xcrun simctl spawn "$SIM_ID" "$binary_path" 2>&1); then
        echo "$output"
        echo "❌ Suite $suite_num failed execution!"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
        return 1
    fi
    echo "$output"
    
    # Parse passed and failed counts from output
    local passed
    local failed
    passed=$(echo "$output" | grep -E "SUITE [0-9]+ RESULTS:" | sed -E 's/.*: ([0-9]+) passed.*/\1/' || echo "0")
    failed=$(echo "$output" | grep -E "SUITE [0-9]+ RESULTS:" | sed -E 's/.*, ([0-9]+) failed.*/\1/' || echo "0")
    
    if [[ -n "$passed" && "$passed" =~ ^[0-9]+$ ]]; then
        TOTAL_PASSED=$((TOTAL_PASSED + passed))
    fi
    if [[ -n "$failed" && "$failed" =~ ^[0-9]+$ ]]; then
        TOTAL_FAILED=$((TOTAL_FAILED + failed))
    fi
}

echo ""
echo ">>> [1/8] Compiling Suite 1: Stealth Box & R2 Production Invariants"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/tests/empirical/test_suite_stealth_box.swift" \
    -o "$TMP_DIR/suite1_stealth_box"
run_suite "1" "StealthBox & R2 Invariants" "$TMP_DIR/suite1_stealth_box"

echo ""
echo ">>> [2/8] Compiling Suite 2: Tap Gestures & Haptic Deduplication"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/tests/empirical/test_suite_gestures.swift" \
    -o "$TMP_DIR/suite2_gestures"
run_suite "2" "Tap Gestures & Haptics" "$TMP_DIR/suite2_gestures"

echo ""
echo ">>> [3/8] Compiling Suite 3: A11yAnnouncer Serialized FIFO Queue"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/iosApp/Sources/Accessibility/A11yAnnouncer.swift" \
    "$REPO_ROOT/tests/empirical/test_suite_announcer.swift" \
    -o "$TMP_DIR/suite3_announcer"
run_suite "3" "A11yAnnouncer Queue" "$TMP_DIR/suite3_announcer"

echo ""
echo ">>> [4/8] Compiling Suite 4: RTL Drag & Accessibility Paging"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/tests/empirical/test_suite_rtl_drag.swift" \
    -o "$TMP_DIR/suite4_rtl_drag"
run_suite "4" "RTL Drag & Accessibility Paging" "$TMP_DIR/suite4_rtl_drag"

echo ""
echo ">>> [5/8] Compiling Suite 5: Continuous Play Audio Engine Queue"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    -F "$REPO_ROOT/shared/build/bin/iosSimulatorArm64/debugFramework" \
    -framework SharedKit \
    -framework AVFoundation \
    -framework CoreMedia \
    -framework CoreVideo \
    "$REPO_ROOT/tests/empirical/test_suite_continuous_play.swift" \
    -o "$TMP_DIR/suite5_continuous_play"
run_suite "5" "Continuous Play Audio Queue" "$TMP_DIR/suite5_continuous_play"

echo ""
echo ">>> [6/8] Compiling Suite 6: NowPlaying & Interruption Safety (Invariant I4)"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    -F "$REPO_ROOT/shared/build/bin/iosSimulatorArm64/debugFramework" \
    -framework SharedKit \
    -framework AVFoundation \
    -framework MediaPlayer \
    -framework UIKit \
    "$REPO_ROOT/iosApp/Sources/NowPlayingManager.swift" \
    "$REPO_ROOT/tests/empirical/test_suite_nowplaying_mediasession.swift" \
    -o "$TMP_DIR/suite6_nowplaying_mediasession"
run_suite "6" "NowPlaying & MediaSession" "$TMP_DIR/suite6_nowplaying_mediasession"

echo ""
echo ">>> [7/8] Compiling Suite 7: System Gestures, Screen-Off Saver & Dynamic VoiceOver"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/iosApp/Sources/Accessibility/VoiceOverObserver.swift" \
    "$REPO_ROOT/iosApp/Sources/Accessibility/A11yAnnouncer.swift" \
    "$REPO_ROOT/iosApp/Sources/Accessibility/RootHostingController.swift" \
    "$REPO_ROOT/iosApp/Sources/HapticManager.swift" \
    "$REPO_ROOT/iosApp/Sources/AccessibleHapticButtonStyle.swift" \
    "$REPO_ROOT/iosApp/Sources/Accessibility/A11yLabels.swift" \
    "$REPO_ROOT/iosApp/Sources/Accessibility/AccessibilitySpeechLanguage.swift" \
    "$REPO_ROOT/iosApp/Sources/Components/ScreenOffSaverOverlay.swift" \
    "$REPO_ROOT/tests/empirical/test_suite_system_gestures.swift" \
    -o "$TMP_DIR/suite7_system_gestures"
run_suite "7" "System Gestures & Screen-Off Saver" "$TMP_DIR/suite7_system_gestures"

echo ""
echo ">>> [8/8] Compiling Suite 8: VoiceOver Name-Only Source Audit (host)"
mkdir -p "$TMP_DIR/suite8_src"
cp "$REPO_ROOT/tests/empirical/test_suite_voiceover_name_only.swift" "$TMP_DIR/suite8_src/main.swift"
xcrun swiftc \
    "$TMP_DIR/suite8_src/main.swift" \
    "$REPO_ROOT/iosApp/Sources/Accessibility/A11yLabels.swift" \
    -o "$TMP_DIR/suite8_voiceover_name_only"
echo ""
echo "================================================================================"
echo ">>> Running Suite 8: VoiceOver Name-Only Source Audit"
echo "================================================================================"
SUITE8_OUTPUT=$("$TMP_DIR/suite8_voiceover_name_only" "$REPO_ROOT" 2>&1) || true
echo "$SUITE8_OUTPUT"
S8_PASSED=$(echo "$SUITE8_OUTPUT" | grep -E "SUITE 8 RESULTS:" | sed -E 's/.*: ([0-9]+) passed.*/\1/')
S8_FAILED=$(echo "$SUITE8_OUTPUT" | grep -E "SUITE 8 RESULTS:" | sed -E 's/.*, ([0-9]+) failed.*/\1/')
if [[ "$S8_PASSED" =~ ^[0-9]+$ && "$S8_FAILED" =~ ^[0-9]+$ ]]; then
    TOTAL_PASSED=$((TOTAL_PASSED + S8_PASSED))
    TOTAL_FAILED=$((TOTAL_FAILED + S8_FAILED))
else
    echo "❌ Suite 8 failed execution!"
    TOTAL_FAILED=$((TOTAL_FAILED + 1))
fi

echo ""
echo "================================================================================"
echo "  EMPIRICAL TEST SUITE SUMMARY (ALL 8 SUITES)"
echo "================================================================================"
echo "  Total Assertions Passed: $TOTAL_PASSED"
echo "  Total Assertions Failed: $TOTAL_FAILED"
echo "--------------------------------------------------------------------------------"

if [[ $TOTAL_FAILED -eq 0 && $TOTAL_PASSED -gt 0 ]]; then
    echo "STATUS: ALL 8 EMPIRICAL TEST SUITES PASSED SUCCESSFULLY ($TOTAL_PASSED assertions)"
    exit 0
else
    echo "STATUS: EMPIRICAL TEST FAILURES DETECTED ($TOTAL_FAILED failed)"
    exit 1
fi

#!/usr/bin/env bash
#
# run_all_empirical_tests.sh
# Empirical Test Runner for Milestone 3 (Accessibility Engine & VoiceOver Infrastructure)
#

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

SIM_ID="${SIM_ID:-72019BA2-8C01-4A19-AA03-7B95B07A7FC1}"
TMP_DIR="/tmp/m3_empirical_tests"
mkdir -p "$TMP_DIR"

echo "================================================================================"
echo "  BLIND QURAN APP iOS — MILESTONE 3 EMPIRICAL CHALLENGER TEST SUITE"
echo "  Target: com.aistudio.quranblind.a11y (Simulator: $SIM_ID)"
echo "================================================================================"

echo ""
echo ">>> Compiling and Executing: suite1_stealth_box"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/tests/empirical/test_suite_stealth_box.swift" \
    -o "$TMP_DIR/suite1_stealth_box"
xcrun simctl spawn "$SIM_ID" "$TMP_DIR/suite1_stealth_box"

echo ""
echo ">>> Compiling and Executing: suite2_gestures"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/tests/empirical/test_suite_gestures.swift" \
    -o "$TMP_DIR/suite2_gestures"
xcrun simctl spawn "$SIM_ID" "$TMP_DIR/suite2_gestures"

echo ""
echo ">>> Compiling and Executing: suite3_announcer"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/iosApp/Sources/Accessibility/A11yAnnouncer.swift" \
    "$REPO_ROOT/tests/empirical/test_suite_announcer.swift" \
    -o "$TMP_DIR/suite3_announcer"
xcrun simctl spawn "$SIM_ID" "$TMP_DIR/suite3_announcer"

echo ""
echo ">>> Compiling and Executing: suite4_rtl_drag"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    "$REPO_ROOT/tests/empirical/test_suite_rtl_drag.swift" \
    -o "$TMP_DIR/suite4_rtl_drag"
xcrun simctl spawn "$SIM_ID" "$TMP_DIR/suite4_rtl_drag"

echo ""
echo ">>> Compiling and Executing: suite5_continuous_play"
xcrun --sdk iphonesimulator swiftc \
    -target arm64-apple-ios15.0-simulator \
    -F "$REPO_ROOT/shared/build/bin/iosSimulatorArm64/debugFramework" \
    -framework SharedKit \
    -framework AVFoundation \
    -framework CoreMedia \
    -framework CoreVideo \
    "$REPO_ROOT/tests/empirical/test_suite_continuous_play.swift" \
    -o "$TMP_DIR/suite5_continuous_play"
xcrun simctl spawn "$SIM_ID" "$TMP_DIR/suite5_continuous_play"

echo ""
echo ">>> Compiling and Executing: suite6_nowplaying_mediasession"
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
xcrun simctl spawn "$SIM_ID" "$TMP_DIR/suite6_nowplaying_mediasession"

echo ""
echo "================================================================================"
echo "ALL EMPIRICAL TEST SUITES COMPLETED SUCCESSFULLY"
echo "================================================================================"

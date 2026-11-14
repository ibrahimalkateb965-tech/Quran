#!/usr/bin/env bash
# tests/e2e/common.sh — Shared Test Assertion Library for Blind Quran App iOS
#
# Provides standard assertion routines for file verification, JSON parsing,
# image dimensions, contrast ratios, Swift syntax/tokens, accessibility traits,
# and structured test reporting.

set -uo pipefail

# ANSI Color Codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Global counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0
VERBOSE="${VERBOSE:-0}"

# Test tracking
CURRENT_TEST_ID=""
CURRENT_TEST_NAME=""

test_start() {
    local id="$1"
    local name="$2"
    CURRENT_TEST_ID="$id"
    CURRENT_TEST_NAME="$name"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [[ "$VERBOSE" -eq 1 ]]; then
        printf "${BLUE}[RUN ]${NC} %-12s : %s\n" "$id" "$name"
    fi
}

test_pass() {
    local msg="${1:-$CURRENT_TEST_NAME}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    printf "${GREEN}[PASS]${NC} %-12s : %s\n" "$CURRENT_TEST_ID" "$msg"
}

test_fail() {
    local msg="${1:-$CURRENT_TEST_NAME}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    printf "${RED}[FAIL]${NC} %-12s : %s\n" "$CURRENT_TEST_ID" "$msg"
}

test_skip() {
    local msg="${1:-$CURRENT_TEST_NAME}"
    SKIPPED_TESTS=$((SKIPPED_TESTS + 1))
    printf "${YELLOW}[SKIP]${NC} %-12s : %s\n" "$CURRENT_TEST_ID" "$msg"
}

# --- Assertion Library ---

assert_file_exists() {
    local file_path="$1"
    if [[ -f "$file_path" ]]; then
        return 0
    else
        return 1
    fi
}

assert_file_not_exists() {
    local file_path="$1"
    if [[ ! -f "$file_path" ]]; then
        return 0
    else
        return 1
    fi
}

assert_dir_exists() {
    local dir_path="$1"
    if [[ -d "$dir_path" ]]; then
        return 0
    else
        return 1
    fi
}

assert_file_non_empty() {
    local file_path="$1"
    if [[ -s "$file_path" ]]; then
        return 0
    else
        return 1
    fi
}

assert_valid_json() {
    local file_path="$1"
    if [[ ! -f "$file_path" ]]; then
        return 1
    fi
    python3 -c "import json, sys; json.load(open(sys.argv[1]))" "$file_path" >/dev/null 2>&1
}

assert_json_expr() {
    local file_path="$1"
    local python_expr="$2"
    if [[ ! -f "$file_path" ]]; then
        return 1
    fi
    python3 -c "
import json, sys
data = json.load(open(sys.argv[1]))
result = bool($python_expr)
sys.exit(0 if result else 1)
" "$file_path" >/dev/null 2>&1
}

assert_image_dimensions() {
    local image_path="$1"
    local expected_width="$2"
    local expected_height="$3"
    if [[ ! -f "$image_path" ]]; then
        return 1
    fi
    local dims
    dims=$(sips -g pixelWidth -g pixelHeight "$image_path" 2>/dev/null)
    local width height
    width=$(echo "$dims" | awk '/pixelWidth:/ {print $2}')
    height=$(echo "$dims" | awk '/pixelHeight:/ {print $2}')
    if [[ "$width" == "$expected_width" && "$height" == "$expected_height" ]]; then
        return 0
    else
        return 1
    fi
}

assert_image_no_alpha() {
    local image_path="$1"
    if [[ ! -f "$image_path" ]]; then
        return 1
    fi
    local alpha
    alpha=$(sips -g hasAlpha "$image_path" 2>/dev/null | awk '/hasAlpha:/ {print $2}')
    if [[ "$alpha" == "no" ]]; then
        return 0
    else
        return 1
    fi
}

assert_font_valid_ttf() {
    local font_path="$1"
    if [[ ! -f "$font_path" || ! -s "$font_path" ]]; then
        return 1
    fi
    python3 -c "
import sys
with open(sys.argv[1], 'rb') as f:
    hdr = f.read(4)
    # 0x00010000 (TrueType) or OTTO (OpenType) or 'true'
    if hdr in (b'\x00\x01\x00\x00', b'OTTO', b'true'):
        sys.exit(0)
    sys.exit(1)
" "$font_path" >/dev/null 2>&1
}

assert_file_contains() {
    local file_path="$1"
    local pattern="$2"
    if [[ -d "$file_path" ]]; then
        grep -q -r -E "$pattern" "$file_path"
        return $?
    elif [[ -f "$file_path" ]]; then
        grep -q -E "$pattern" "$file_path"
        return $?
    else
        return 1
    fi
}

assert_file_not_contains() {
    local file_path="$1"
    local pattern="$2"
    if [[ -d "$file_path" ]]; then
        if grep -q -r -E "$pattern" "$file_path"; then
            return 1
        else
            return 0
        fi
    elif [[ -f "$file_path" ]]; then
        if grep -q -E "$pattern" "$file_path"; then
            return 1
        else
            return 0
        fi
    else
        return 0
    fi
}

assert_contrast_ratio_ge() {
    local color1="$1"
    local color2="$2"
    local min_ratio="$3"
    python3 -c "
import sys
def lum(c):
    c = c.lstrip('#')
    if len(c) == 3: c = ''.join(x*2 for x in c)
    r, g, b = [int(c[i:i+2], 16)/255.0 for i in (0, 2, 4)]
    srgb = [x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4 for x in (r, g, b)]
    return 0.2126 * srgb[0] + 0.7152 * srgb[1] + 0.0722 * srgb[2]

l1, l2 = lum(sys.argv[1]), lum(sys.argv[2])
ratio = (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
min_r = float(sys.argv[3])
sys.exit(0 if ratio >= min_r else 1)
" "$color1" "$color2" "$min_ratio" >/dev/null 2>&1
}

assert_plist_value_equals() {
    local plist_path="$1"
    local key="$2"
    local expected="$3"
    if [[ ! -f "$plist_path" ]]; then
        return 1
    fi
    local val
    val=$(/usr/libexec/PlistBuddy -c "Print :$key" "$plist_path" 2>/dev/null || true)
    if [[ "$val" == "$expected" ]]; then
        return 0
    else
        return 1
    fi
}

assert_plist_array_contains() {
    local plist_path="$1"
    local array_key="$2"
    local expected_item="$3"
    if [[ ! -f "$plist_path" ]]; then
        return 1
    fi
    python3 -c "
import plistlib, sys
try:
    with open(sys.argv[1], 'rb') as f:
        pl = plistlib.load(f)
    arr = pl.get(sys.argv[2], [])
    sys.exit(0 if sys.argv[3] in arr else 1)
except Exception:
    sys.exit(1)
" "$plist_path" "$array_key" "$expected_item" >/dev/null 2>&1
}

assert_yaml_key_contains() {
    local yaml_path="$1"
    local key="$2"
    local expected_substring="$3"
    if [[ ! -f "$yaml_path" ]]; then
        return 1
    fi
    grep -E "^\s*${key}\s*:\s*.*${expected_substring}" "$yaml_path" >/dev/null 2>&1
}

assert_command_success() {
    local cmd="$1"
    eval "$cmd" >/dev/null 2>&1
}

print_suite_summary() {
    local suite_title="${1:-TEST SUITE SUMMARY}"
    local duration="${2:-0}"
    local pass_pct=0
    if [[ "$TOTAL_TESTS" -gt 0 ]]; then
        pass_pct=$(python3 -c "print(f'{($PASSED_TESTS / $TOTAL_TESTS) * 100:.1f}')")
    fi

    echo ""
    echo "================================================================================"
    printf "${BOLD}%s${NC}\n" "$suite_title"
    echo "================================================================================"
    printf "  Total Executed: %d\n" "$TOTAL_TESTS"
    printf "  ${GREEN}Passed:${NC}         %d\n" "$PASSED_TESTS"
    printf "  ${RED}Failed:${NC}         %d\n" "$FAILED_TESTS"
    printf "  ${YELLOW}Skipped:${NC}        %d\n" "$SKIPPED_TESTS"
    printf "  Pass Rate:      %s%%\n" "$pass_pct"
    printf "  Duration:       %ss\n" "$duration"
    echo "--------------------------------------------------------------------------------"
    if [[ "$FAILED_TESTS" -eq 0 && "$TOTAL_TESTS" -gt 0 ]]; then
        printf "${GREEN}${BOLD}STATUS: ALL TESTS PASSED${NC}\n"
        return 0
    else
        printf "${RED}${BOLD}STATUS: TEST FAILURES DETECTED${NC}\n"
        return 1
    fi
}

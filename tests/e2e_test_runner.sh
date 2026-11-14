#!/usr/bin/env bash
# tests/e2e_test_runner.sh — Master E2E Automated Test Runner for Blind Quran App iOS
#
# Provides an opaque-box, requirement-driven test suite runner covering
# Features 1 through 22 across four systematic verification tiers:
#   Tier 1: Feature Coverage (110 tests)
#   Tier 2: Boundary & Corner Cases (110 tests)
#   Tier 3: Cross-Feature Combinations (22 tests)
#   Tier 4: Real-World Scenarios (11 scenarios)
# Total test cases: 253 test cases
#
# Exit codes:
#   0 = All executed tests passed
#   1 = One or more tests failed
#   2 = Invalid arguments or environment failure

set -uo pipefail

RUNNER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$RUNNER_DIR/.." && pwd)"

# Source shared assertions library and tier suites
source "$RUNNER_DIR/e2e/common.sh"
source "$RUNNER_DIR/e2e/tier1_feature_coverage.sh"
source "$RUNNER_DIR/e2e/tier2_boundary_corner.sh"
source "$RUNNER_DIR/e2e/tier3_cross_feature.sh"
source "$RUNNER_DIR/e2e/tier4_real_world.sh"

show_usage() {
    cat <<EOF
Blind Quran App iOS — Automated E2E Test Suite Runner

USAGE:
  $(basename "$0") [OPTIONS]

OPTIONS:
  --tier <1|2|3|4>      Execute only the specified test tier
                        1 = Tier 1: Primary Feature Coverage (110 tests)
                        2 = Tier 2: Boundary & Corner Cases (110 tests)
                        3 = Tier 3: Cross-Feature Combinations (22 tests)
                        4 = Tier 4: Real-World Application Scenarios (11 scenarios)

  --feature <1-22>      Execute Tier 1 and Tier 2 tests for a specific feature ID (1 to 22)

  --verbose             Enable verbose logging output during test execution

  --help, -h            Display this usage reference and exit

EXAMPLES:
  $(basename "$0")                     # Run entire test suite (all 253 tests)
  $(basename "$0") --tier 1            # Run all Tier 1 feature coverage tests
  $(basename "$0") --tier 2            # Run all Tier 2 boundary and corner tests
  $(basename "$0") --feature 7         # Run tests for Feature 7 (Stealth Box Invariant)
  $(basename "$0") --tier 4 --verbose  # Run real-world scenarios with verbose output

EXIT CODES:
  0 = All tests passed
  1 = One or more test failures
  2 = Invalid CLI invocation or invalid arguments
EOF
}

# Parse CLI arguments
TARGET_TIER=""
TARGET_FEATURE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tier)
            if [[ $# -lt 2 ]]; then
                echo "Error: --tier requires an argument (1, 2, 3, or 4)" >&2
                exit 2
            fi
            TARGET_TIER="$2"
            shift 2
            ;;
        --feature)
            if [[ $# -lt 2 ]]; then
                echo "Error: --feature requires a feature number between 1 and 22" >&2
                exit 2
            fi
            TARGET_FEATURE="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=1
            shift
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            echo "Error: Unknown argument: $1" >&2
            echo "Use $(basename "$0") --help for usage information" >&2
            exit 2
            ;;
    esac
done

# Validate --tier argument if specified
if [[ -n "$TARGET_TIER" ]]; then
    if [[ ! "$TARGET_TIER" =~ ^[1-4]$ ]]; then
        echo "Error: Invalid tier '$TARGET_TIER'. Must be 1, 2, 3, or 4." >&2
        exit 2
    fi
fi

# Validate --feature argument if specified
if [[ -n "$TARGET_FEATURE" ]]; then
    if [[ ! "$TARGET_FEATURE" =~ ^[0-9]+$ ]] || [[ "$TARGET_FEATURE" -lt 1 || "$TARGET_FEATURE" -gt 22 ]]; then
        echo "Error: Invalid feature '$TARGET_FEATURE'. Must be an integer between 1 and 22." >&2
        exit 2
    fi
fi

START_TIME=$(python3 -c "import time; print(time.time())")

echo ""
echo "================================================================================"
echo "  BLIND QURAN APP iOS — AUTOMATED E2E TEST SUITE"
echo "  Target: com.aistudio.quranblind.a11y (iOS 15.0+)"
echo "  Authoritative Base: ACCESSIBILITY_GUIDELINES_IOS.md & PROJECT.md"
echo "================================================================================"

# Execute selected suite
if [[ -n "$TARGET_FEATURE" ]]; then
    echo "Filtering execution to Feature $TARGET_FEATURE across Tiers 1 and 2"
    run_tier1_tests "$TARGET_FEATURE"
    run_tier2_tests "$TARGET_FEATURE"
elif [[ -n "$TARGET_TIER" ]]; then
    case "$TARGET_TIER" in
        1) run_tier1_tests "" ;;
        2) run_tier2_tests "" ;;
        3) run_tier3_tests ;;
        4) run_tier4_tests ;;
    esac
else
    # Default: Run full test suite across all 4 tiers
    run_tier1_tests ""
    run_tier2_tests ""
    run_tier3_tests
    run_tier4_tests
fi

END_TIME=$(python3 -c "import time; print(time.time())")
DURATION=$(python3 -c "print(f'{float($END_TIME) - float($START_TIME):.2f}')")

# Output final suite summary and determine exit code
if print_suite_summary "FINAL E2E TEST SUITE EXECUTION SUMMARY" "$DURATION"; then
    exit 0
else
    exit 1
fi

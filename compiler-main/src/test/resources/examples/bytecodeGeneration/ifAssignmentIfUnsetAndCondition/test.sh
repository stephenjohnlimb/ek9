#!/usr/bin/env bash
#
# E2E test for ifAssignmentIfUnsetAndCondition
# Tests assignment if unset (:=?) with condition
#

set -e
cd "$(dirname "$0")"

# Project-relative paths
EK9_BINARY="../../../../../../../ek9-wrapper/target/bin/ek9"
EK9_HOME="../../../../../../target"

# Validate build artifacts exist
if [ ! -f "$EK9_BINARY" ]; then
    echo "ERROR: ek9 binary not found: $EK9_BINARY"
    exit 1
fi

if [ ! -f "$EK9_HOME/ek9c-jar-with-dependencies.jar" ]; then
    echo "ERROR: Compiler JAR not found: $EK9_HOME/ek9c-jar-with-dependencies.jar"
    exit 1
fi

# Clean previous run
rm -rf .ek9 actual_*.txt

# Compile once
export EK9_HOME
$EK9_BINARY -C *.ek9

# Test case 1: unset, assigns 15, condition passes (> 10)
$EK9_BINARY ifAssignmentIfUnsetAndCondition.ek9 15 > actual_case_high.txt 2>&1
if ! diff -u expected_case_high.txt actual_case_high.txt; then
    echo "FAIL: Case high (unset -> 15 > 10) mismatch"
    exit 1
fi

# Test case 2: unset, assigns 7, condition fails (<= 10)
$EK9_BINARY ifAssignmentIfUnsetAndCondition.ek9 7 > actual_case_low_assigned.txt 2>&1
if ! diff -u expected_case_low_assigned.txt actual_case_low_assigned.txt; then
    echo "FAIL: Case low assigned (unset -> 7 <= 10) mismatch"
    exit 1
fi

# Test case 3: pre-set to 5, skips assignment, condition fails (<= 10)
$EK9_BINARY ifAssignmentIfUnsetAndCondition.ek9 -1 > actual_case_low_preset.txt 2>&1
if ! diff -u expected_case_low_preset.txt actual_case_low_preset.txt; then
    echo "FAIL: Case low preset (already 5 <= 10) mismatch"
    exit 1
fi

# Test case 4: pre-set to 100, skips assignment, condition passes (> 10) (NEW - missing coverage)
$EK9_BINARY ifAssignmentIfUnsetAndCondition.ek9 200 > actual_case_high_preset.txt 2>&1
if ! diff -u expected_case_high_preset.txt actual_case_high_preset.txt; then
    echo "FAIL: Case high preset (already 100 > 10) mismatch"
    exit 1
fi

# Test case 5: unset, assigns unset, condition not evaluated (NEW - missing coverage)
$EK9_BINARY ifAssignmentIfUnsetAndCondition.ek9 0 > actual_case_both_unset.txt 2>&1
if ! diff -u expected_case_both_unset.txt actual_case_both_unset.txt; then
    echo "FAIL: Case both unset mismatch"
    exit 1
fi

echo "PASS: ifAssignmentIfUnsetAndCondition (5 cases)"

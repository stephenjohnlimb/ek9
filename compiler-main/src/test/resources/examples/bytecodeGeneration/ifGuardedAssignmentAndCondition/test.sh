#!/usr/bin/env bash
#
# E2E test for ifGuardedAssignmentAndCondition
# Tests guarded assignment (?=) with condition
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

# Test case 1: value set and > 10 (High)
$EK9_BINARY ifGuardedAssignmentAndCondition.ek9 15 > actual_case_high.txt 2>&1
if ! diff -u expected_case_high.txt actual_case_high.txt; then
    echo "FAIL: Case high (set and > 10) mismatch"
    exit 1
fi

# Test case 2: value set but <= 10 (Low)
$EK9_BINARY ifGuardedAssignmentAndCondition.ek9 5 > actual_case_low.txt 2>&1
if ! diff -u expected_case_low.txt actual_case_low.txt; then
    echo "FAIL: Case low (set but <= 10) mismatch"
    exit 1
fi

# Test case 3: value unset (also Low branch)
$EK9_BINARY ifGuardedAssignmentAndCondition.ek9 0 > actual_case_unset.txt 2>&1
if ! diff -u expected_case_unset.txt actual_case_unset.txt; then
    echo "FAIL: Case unset mismatch"
    exit 1
fi

echo "PASS: ifGuardedAssignmentAndCondition (3 cases)"

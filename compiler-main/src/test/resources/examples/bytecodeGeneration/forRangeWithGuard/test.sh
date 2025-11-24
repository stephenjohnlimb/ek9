#!/usr/bin/env bash
#
# E2E test for forRangeWithGuard
# Multi-case test: Tests for-range loop with guard variable pattern
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

# Test case 1: range 1..3 (guard set, sums 1+2+3=6)
$EK9_BINARY forRangeWithGuard.ek9 3 > actual_case_3.txt 2>&1
if ! diff -u expected_case_3.txt actual_case_3.txt; then
    echo "FAIL: Case 3 (range 1..3) mismatch"
    exit 1
fi

# Test case 2: 0 (guard unset, no iterations)
$EK9_BINARY forRangeWithGuard.ek9 0 > actual_case_0.txt 2>&1
if ! diff -u expected_case_0.txt actual_case_0.txt; then
    echo "FAIL: Case 0 (guard unset) mismatch"
    exit 1
fi

echo "PASS: forRangeWithGuard (2 cases)"

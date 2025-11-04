#!/usr/bin/env bash
#
# E2E test for simpleSwitchFloat
# Multi-case test: Tests float switch with comparison operators
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

# Test case 1: -15.0 (very negative)
$EK9_BINARY simpleSwitchFloat.ek9 -15.0 > actual_case_neg15.txt 2>&1
if ! diff -u expected_case_neg15.txt actual_case_neg15.txt; then
    echo "FAIL: Case -15.0 mismatch"
    exit 1
fi

# Test case 2: 0.0 (zero or negative)
$EK9_BINARY simpleSwitchFloat.ek9 0.0 > actual_case_0.txt 2>&1
if ! diff -u expected_case_0.txt actual_case_0.txt; then
    echo "FAIL: Case 0.0 mismatch"
    exit 1
fi

# Test case 3: 5.0 (low)
$EK9_BINARY simpleSwitchFloat.ek9 5.0 > actual_case_5.txt 2>&1
if ! diff -u expected_case_5.txt actual_case_5.txt; then
    echo "FAIL: Case 5.0 mismatch"
    exit 1
fi

# Test case 4: 25.0 (medium)
$EK9_BINARY simpleSwitchFloat.ek9 25.0 > actual_case_25.txt 2>&1
if ! diff -u expected_case_25.txt actual_case_25.txt; then
    echo "FAIL: Case 25.0 mismatch"
    exit 1
fi

# Test case 5: 75.0 (high)
$EK9_BINARY simpleSwitchFloat.ek9 75.0 > actual_case_75.txt 2>&1
if ! diff -u expected_case_75.txt actual_case_75.txt; then
    echo "FAIL: Case 75.0 mismatch"
    exit 1
fi

# Test case 6: 100.0 (very high)
$EK9_BINARY simpleSwitchFloat.ek9 100.0 > actual_case_100.txt 2>&1
if ! diff -u expected_case_100.txt actual_case_100.txt; then
    echo "FAIL: Case 100.0 mismatch"
    exit 1
fi

echo "PASS: simpleSwitchFloat (6 cases)"

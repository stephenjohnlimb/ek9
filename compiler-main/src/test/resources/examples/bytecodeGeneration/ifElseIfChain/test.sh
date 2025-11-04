#!/usr/bin/env bash
#
# E2E test for ifElseIfChain
# Multi-case test: Tests multi-condition if/else-if/else chain
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

# Test case 1: 25 (very high - value > 20)
$EK9_BINARY ifElseIfChain.ek9 25 > actual_case_25.txt 2>&1
if ! diff -u expected_case_very_high.txt actual_case_25.txt; then
    echo "FAIL: Case 25 (very high) mismatch"
    exit 1
fi

# Test case 2: 15 (high - value > 10 and ≤ 20)
$EK9_BINARY ifElseIfChain.ek9 15 > actual_case_15.txt 2>&1
if ! diff -u expected_case_high.txt actual_case_15.txt; then
    echo "FAIL: Case 15 (high) mismatch"
    exit 1
fi

# Test case 3: 5 (positive - value > 0 and ≤ 10)
$EK9_BINARY ifElseIfChain.ek9 5 > actual_case_5.txt 2>&1
if ! diff -u expected_case_positive.txt actual_case_5.txt; then
    echo "FAIL: Case 5 (positive) mismatch"
    exit 1
fi

# Test case 4: 0 (non-positive - value ≤ 0)
$EK9_BINARY ifElseIfChain.ek9 0 > actual_case_0.txt 2>&1
if ! diff -u expected_case_nonpositive.txt actual_case_0.txt; then
    echo "FAIL: Case 0 (non-positive) mismatch"
    exit 1
fi

echo "PASS: ifElseIfChain (4 cases)"

#!/usr/bin/env bash
#
# E2E test for comprehensiveNestedControlFlow
# Multi-case test: Tests complex 3-level nested control flow (while/if/for/switch/do-while)
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

# Test case 1: threshold=50, iterations=5 (standard large inputs)
$EK9_BINARY comprehensiveNestedControlFlow.ek9 50 5 > actual_case_50_5.txt 2>&1
if ! diff -u expected_case_50_5.txt actual_case_50_5.txt; then
    echo "FAIL: Case (50, 5) mismatch"
    exit 1
fi

# Test case 2: threshold=5, iterations=3 (small inputs)
$EK9_BINARY comprehensiveNestedControlFlow.ek9 5 3 > actual_case_5_3.txt 2>&1
if ! diff -u expected_case_5_3.txt actual_case_5_3.txt; then
    echo "FAIL: Case (5, 3) mismatch"
    exit 1
fi

# Test case 3: threshold=0, iterations=0 (validation triggers - both out-of-range)
$EK9_BINARY comprehensiveNestedControlFlow.ek9 0 0 > actual_case_0_0.txt 2>&1
if ! diff -u expected_case_0_0.txt actual_case_0_0.txt; then
    echo "FAIL: Case (0, 0) validation mismatch"
    exit 1
fi

# Test case 4: threshold=200, iterations=30 (both exceed limits - fallback to defaults)
$EK9_BINARY comprehensiveNestedControlFlow.ek9 200 30 > actual_case_200_30.txt 2>&1
if ! diff -u expected_case_200_30.txt actual_case_200_30.txt; then
    echo "FAIL: Case (200, 30) validation mismatch"
    exit 1
fi

# Test case 5: threshold=10, iterations=1 (single iteration - tests Case 1 do-while)
$EK9_BINARY comprehensiveNestedControlFlow.ek9 10 1 > actual_case_10_1.txt 2>&1
if ! diff -u expected_case_10_1.txt actual_case_10_1.txt; then
    echo "FAIL: Case (10, 1) single iteration mismatch"
    exit 1
fi

# Test case 6: threshold=10, iterations=3 (hits Case 1 and 3)
$EK9_BINARY comprehensiveNestedControlFlow.ek9 10 3 > actual_case_10_3.txt 2>&1
if ! diff -u expected_case_10_3.txt actual_case_10_3.txt; then
    echo "FAIL: Case (10, 3) mismatch"
    exit 1
fi

# Test case 7: threshold=15, iterations=5 (hits Case 1, 3, and 5)
$EK9_BINARY comprehensiveNestedControlFlow.ek9 15 5 > actual_case_15_5.txt 2>&1
if ! diff -u expected_case_15_5.txt actual_case_15_5.txt; then
    echo "FAIL: Case (15, 5) mismatch"
    exit 1
fi

echo "PASS: comprehensiveNestedControlFlow (7 cases)"

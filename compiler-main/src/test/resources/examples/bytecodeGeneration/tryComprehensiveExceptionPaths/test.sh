#!/usr/bin/env bash
#
# E2E test for tryComprehensiveExceptionPaths
# Multi-case test: Tests comprehensive exception handling scenarios
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

# Test case 1: Exception thrown and caught
$EK9_BINARY tryComprehensiveExceptionPaths.ek9 1 > actual_case_1.txt 2>&1
if ! diff -u expected_case_1.txt actual_case_1.txt; then
    echo "FAIL: Case 1 (exception thrown and caught) mismatch"
    exit 1
fi

# Test case 2: Try/catch normal path
$EK9_BINARY tryComprehensiveExceptionPaths.ek9 2 > actual_case_2.txt 2>&1
if ! diff -u expected_case_2.txt actual_case_2.txt; then
    echo "FAIL: Case 2 (normal path) mismatch"
    exit 1
fi

# Test case 3: Exception propagates through finally
$EK9_BINARY tryComprehensiveExceptionPaths.ek9 3 > actual_case_3.txt 2>&1
if ! diff -u expected_case_3.txt actual_case_3.txt; then
    echo "FAIL: Case 3 (exception propagation) mismatch"
    exit 1
fi

# Test case 4: Try/catch/finally exception caught
$EK9_BINARY tryComprehensiveExceptionPaths.ek9 4 > actual_case_4.txt 2>&1
if ! diff -u expected_case_4.txt actual_case_4.txt; then
    echo "FAIL: Case 4 (try/catch/finally) mismatch"
    exit 1
fi

# Test case 5: Finally executes before propagation
$EK9_BINARY tryComprehensiveExceptionPaths.ek9 5 > actual_case_5.txt 2>&1
if ! diff -u expected_case_5.txt actual_case_5.txt; then
    echo "FAIL: Case 5 (finally before propagation) mismatch"
    exit 1
fi

echo "PASS: tryComprehensiveExceptionPaths (5 cases)"

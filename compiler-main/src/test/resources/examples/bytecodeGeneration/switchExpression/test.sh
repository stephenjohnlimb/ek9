#!/usr/bin/env bash
#
# E2E test for switchExpression
# Multi-case test: Tests switch expression with different argument values
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

# Test case 1: value = 1 (should return "One")
$EK9_BINARY switchExpression.ek9 1 > actual_case_1.txt 2>&1
if ! diff -u expected_case_1.txt actual_case_1.txt; then
    echo "FAIL: Case 1 mismatch"
    exit 1
fi

# Test case 2: value = 2 (should return "Two")
$EK9_BINARY switchExpression.ek9 2 > actual_case_2.txt 2>&1
if ! diff -u expected_case_2.txt actual_case_2.txt; then
    echo "FAIL: Case 2 mismatch"
    exit 1
fi

# Test case 3: value = 99 (should return "Other" via default)
$EK9_BINARY switchExpression.ek9 99 > actual_case_99.txt 2>&1
if ! diff -u expected_case_99.txt actual_case_99.txt; then
    echo "FAIL: Case 99 mismatch"
    exit 1
fi

echo "PASS: switchExpression (3 cases)"

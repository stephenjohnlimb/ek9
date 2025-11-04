#!/usr/bin/env bash
#
# E2E test for multipleCaseLiterals
# Multi-case test: Tests switch with multiple literals per case (OR logic)
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

# Test case 1, 2, 3: Low (case 1, 2, 3)
$EK9_BINARY multipleCaseLiterals.ek9 1 > actual_case_1.txt 2>&1
if ! diff -u expected_case_low.txt actual_case_1.txt; then
    echo "FAIL: Case 1 mismatch"
    exit 1
fi

$EK9_BINARY multipleCaseLiterals.ek9 2 > actual_case_2.txt 2>&1
if ! diff -u expected_case_low.txt actual_case_2.txt; then
    echo "FAIL: Case 2 mismatch"
    exit 1
fi

$EK9_BINARY multipleCaseLiterals.ek9 3 > actual_case_3.txt 2>&1
if ! diff -u expected_case_low.txt actual_case_3.txt; then
    echo "FAIL: Case 3 mismatch"
    exit 1
fi

# Test case 4, 5: Medium (case 4, 5)
$EK9_BINARY multipleCaseLiterals.ek9 4 > actual_case_4.txt 2>&1
if ! diff -u expected_case_medium.txt actual_case_4.txt; then
    echo "FAIL: Case 4 mismatch"
    exit 1
fi

$EK9_BINARY multipleCaseLiterals.ek9 5 > actual_case_5.txt 2>&1
if ! diff -u expected_case_medium.txt actual_case_5.txt; then
    echo "FAIL: Case 5 mismatch"
    exit 1
fi

# Test case 99: Other (default)
$EK9_BINARY multipleCaseLiterals.ek9 99 > actual_case_99.txt 2>&1
if ! diff -u expected_case_other.txt actual_case_99.txt; then
    echo "FAIL: Case 99 mismatch"
    exit 1
fi

echo "PASS: multipleCaseLiterals (6 cases)"

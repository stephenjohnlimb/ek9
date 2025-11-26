#!/usr/bin/env bash
#
# E2E test for forRangeExpression
# Tests for-range expression accumulator pattern with different limit values
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

# Test case 1: limit = 5 (sum = 1+2+3+4+5 = 15)
$EK9_BINARY forRangeExpression.ek9 5 > actual_case_5.txt 2>&1
if ! diff -u expected_case_5.txt actual_case_5.txt; then
    echo "FAIL: limit=5 mismatch"
    exit 1
fi

# Test case 2: limit = 3 (sum = 1+2+3 = 6)
$EK9_BINARY forRangeExpression.ek9 3 > actual_case_3.txt 2>&1
if ! diff -u expected_case_3.txt actual_case_3.txt; then
    echo "FAIL: limit=3 mismatch"
    exit 1
fi

# Test case 3: limit = 0 (no iterations, sum = 0)
$EK9_BINARY forRangeExpression.ek9 0 > actual_case_0.txt 2>&1
if ! diff -u expected_case_0.txt actual_case_0.txt; then
    echo "FAIL: limit=0 mismatch"
    exit 1
fi

echo "PASS: forRangeExpression (3 cases)"

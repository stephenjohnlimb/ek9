#!/usr/bin/env bash
#
# E2E test for tryExpression
# Tests try expression accumulator pattern with different input values
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

# Test case 1: input = 10 (result = 20)
$EK9_BINARY tryExpression.ek9 10 > actual_case_10.txt 2>&1
if ! diff -u expected_case_10.txt actual_case_10.txt; then
    echo "FAIL: input=10 mismatch"
    exit 1
fi

# Test case 2: input = 5 (result = 10)
$EK9_BINARY tryExpression.ek9 5 > actual_case_5.txt 2>&1
if ! diff -u expected_case_5.txt actual_case_5.txt; then
    echo "FAIL: input=5 mismatch"
    exit 1
fi

# Test case 3: input = 0 (result = 0)
$EK9_BINARY tryExpression.ek9 0 > actual_case_0.txt 2>&1
if ! diff -u expected_case_0.txt actual_case_0.txt; then
    echo "FAIL: input=0 mismatch"
    exit 1
fi

echo "PASS: tryExpression (3 cases)"

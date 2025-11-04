#!/usr/bin/env bash
#
# E2E test for ifElseStatement
# Multi-case test: Tests binary if/else conditional branching
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

# Test case 1: 15 (high - value > 10)
$EK9_BINARY ifElseStatement.ek9 15 > actual_case_high.txt 2>&1
if ! diff -u expected_case_high.txt actual_case_high.txt; then
    echo "FAIL: Case high (15) mismatch"
    exit 1
fi

# Test case 2: 5 (low - value ≤ 10)
$EK9_BINARY ifElseStatement.ek9 5 > actual_case_low.txt 2>&1
if ! diff -u expected_case_low.txt actual_case_low.txt; then
    echo "FAIL: Case low (5) mismatch"
    exit 1
fi

echo "PASS: ifElseStatement (2 cases)"

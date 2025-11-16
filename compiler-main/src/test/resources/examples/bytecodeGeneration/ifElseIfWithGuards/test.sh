#!/usr/bin/env bash
#
# E2E test for ifElseIfWithGuards
# Multi-case test: Tests multiple guards in if/else-if chain
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

# Test case 1: 0 (both unset - no guards succeed)
$EK9_BINARY ifElseIfWithGuards.ek9 0 > actual_case_neither.txt 2>&1
if ! diff -u expected_case_neither.txt actual_case_neither.txt; then
    echo "FAIL: Case neither (0) mismatch"
    exit 1
fi

# Test case 2: 1 (first set - first guard succeeds)
$EK9_BINARY ifElseIfWithGuards.ek9 1 > actual_case_first.txt 2>&1
if ! diff -u expected_case_first.txt actual_case_first.txt; then
    echo "FAIL: Case first (1) mismatch"
    exit 1
fi

# Test case 3: 2 (second set - second guard succeeds)
$EK9_BINARY ifElseIfWithGuards.ek9 2 > actual_case_second.txt 2>&1
if ! diff -u expected_case_second.txt actual_case_second.txt; then
    echo "FAIL: Case second (2) mismatch"
    exit 1
fi

echo "PASS: ifElseIfWithGuards (3 cases)"

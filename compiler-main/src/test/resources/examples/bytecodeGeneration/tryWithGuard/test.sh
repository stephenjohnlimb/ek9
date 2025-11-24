#!/usr/bin/env bash
#
# E2E test for tryWithGuard
# Multi-case test: Tests try statement with guard variable pattern
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

# Test case 1: value=1 (guard set, try executes)
$EK9_BINARY tryWithGuard.ek9 1 > actual_case_1.txt 2>&1
if ! diff -u expected_case_1.txt actual_case_1.txt; then
    echo "FAIL: Case 1 mismatch"
    exit 1
fi

# Test case 2: value=0 (guard unset, try skipped)
$EK9_BINARY tryWithGuard.ek9 0 > actual_case_0.txt 2>&1
if ! diff -u expected_case_0.txt actual_case_0.txt; then
    echo "FAIL: Case 0 (guard unset) mismatch"
    exit 1
fi

echo "PASS: tryWithGuard (2 cases)"

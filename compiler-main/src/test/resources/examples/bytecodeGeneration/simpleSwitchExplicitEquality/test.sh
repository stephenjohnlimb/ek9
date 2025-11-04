#!/usr/bin/env bash
#
# E2E test for simpleSwitchExplicitEquality
# Multi-case test: Tests both implicit (case 10) and explicit (case == 20) equality
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

# Test case 1: 10 (implicit equality)
$EK9_BINARY simpleSwitchExplicitEquality.ek9 10 > actual_case_10.txt 2>&1
if ! diff -u expected_case_10.txt actual_case_10.txt; then
    echo "FAIL: Case 10 mismatch"
    exit 1
fi

# Test case 2: 20 (explicit equality)
$EK9_BINARY simpleSwitchExplicitEquality.ek9 20 > actual_case_20.txt 2>&1
if ! diff -u expected_case_20.txt actual_case_20.txt; then
    echo "FAIL: Case 20 mismatch"
    exit 1
fi

# Test case 3: 30 (default case)
$EK9_BINARY simpleSwitchExplicitEquality.ek9 30 > actual_case_30.txt 2>&1
if ! diff -u expected_case_30.txt actual_case_30.txt; then
    echo "FAIL: Case 30 mismatch"
    exit 1
fi

echo "PASS: simpleSwitchExplicitEquality (3 cases)"

#!/usr/bin/env bash
#
# E2E test for simpleSwitchString
# Multi-case test: Tests string pattern matching with different arguments
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

# Test case 1: contains "hello"
$EK9_BINARY simpleSwitchString.ek9 "hello" > actual_case_hello.txt 2>&1
if ! diff -u expected_case_hello.txt actual_case_hello.txt; then
    echo "FAIL: Case hello mismatch"
    exit 1
fi

# Test case 2: numeric pattern
$EK9_BINARY simpleSwitchString.ek9 "12345" > actual_case_12345.txt 2>&1
if ! diff -u expected_case_12345.txt actual_case_12345.txt; then
    echo "FAIL: Case 12345 mismatch"
    exit 1
fi

# Test case 3: default
$EK9_BINARY simpleSwitchString.ek9 "other" > actual_case_other.txt 2>&1
if ! diff -u expected_case_other.txt actual_case_other.txt; then
    echo "FAIL: Case other mismatch"
    exit 1
fi

echo "PASS: simpleSwitchString (3 cases)"

#!/usr/bin/env bash
#
# E2E test for simpleSwitchBoolean
# Multi-case test: Tests Boolean switch with true/false cases
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

# Test case 1: true
$EK9_BINARY simpleSwitchBoolean.ek9 true > actual_case_true.txt 2>&1
if ! diff -u expected_case_true.txt actual_case_true.txt; then
    echo "FAIL: Case true mismatch"
    exit 1
fi

# Test case 2: false
$EK9_BINARY simpleSwitchBoolean.ek9 false > actual_case_false.txt 2>&1
if ! diff -u expected_case_false.txt actual_case_false.txt; then
    echo "FAIL: Case false mismatch"
    exit 1
fi

echo "PASS: simpleSwitchBoolean (2 cases)"

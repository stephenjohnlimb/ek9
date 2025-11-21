#!/usr/bin/env bash
#
# E2E test for ifGuardedAssignment
# Tests guarded assignment (?=) without condition
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

# Test case 1: inputValue > 0 (optionalValue is set)
$EK9_BINARY ifGuardedAssignment.ek9 5 > actual_case_set.txt 2>&1
if ! diff -u expected_case_set.txt actual_case_set.txt; then
    echo "FAIL: Case set (value > 0) mismatch"
    exit 1
fi

# Test case 2: inputValue = 0 (optionalValue is unset)
$EK9_BINARY ifGuardedAssignment.ek9 0 > actual_case_unset.txt 2>&1
if ! diff -u expected_case_unset.txt actual_case_unset.txt; then
    echo "FAIL: Case unset (value = 0) mismatch"
    exit 1
fi

echo "PASS: ifGuardedAssignment (2 cases)"

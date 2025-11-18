#!/usr/bin/env bash
#
# E2E test for ifAssignmentIfUnset
# Tests assignment if unset (:=?) without condition
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

# Test case 1: existing starts unset, assigns newValue (42)
$EK9_BINARY ifAssignmentIfUnset.ek9 5 > actual_case_assigned.txt 2>&1
if ! diff -u expected_case_assigned.txt actual_case_assigned.txt; then
    echo "FAIL: Case assigned (unset -> 42) mismatch"
    exit 1
fi

# Test case 2: existing pre-set to 100, skips assignment
$EK9_BINARY ifAssignmentIfUnset.ek9 -1 > actual_case_skipped.txt 2>&1
if ! diff -u expected_case_skipped.txt actual_case_skipped.txt; then
    echo "FAIL: Case skipped (already 100) mismatch"
    exit 1
fi

# Test case 3: both existing and newValue are unset (NEW - missing coverage)
$EK9_BINARY ifAssignmentIfUnset.ek9 0 > actual_case_both_unset.txt 2>&1
if ! diff -u expected_case_both_unset.txt actual_case_both_unset.txt; then
    echo "FAIL: Case both unset mismatch"
    exit 1
fi

echo "PASS: ifAssignmentIfUnset (3 cases)"

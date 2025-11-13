#!/usr/bin/env bash
#
# E2E test for tryWithResourceExceptionPaths
# Multi-case test: Tests resource cleanup under exception scenarios
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

# Test case 1: Exception during resource usage
$EK9_BINARY tryWithResourceExceptionPaths.ek9 1 > actual_case_1.txt 2>&1
if ! diff -u expected_case_1.txt actual_case_1.txt; then
    echo "FAIL: Case 1 (exception during usage) mismatch"
    exit 1
fi

# Test case 2: Normal resource usage
$EK9_BINARY tryWithResourceExceptionPaths.ek9 2 > actual_case_2.txt 2>&1
if ! diff -u expected_case_2.txt actual_case_2.txt; then
    echo "FAIL: Case 2 (normal usage) mismatch"
    exit 1
fi

# Test case 3: Resource close() throws
$EK9_BINARY tryWithResourceExceptionPaths.ek9 3 > actual_case_3.txt 2>&1
if ! diff -u expected_case_3.txt actual_case_3.txt; then
    echo "FAIL: Case 3 (close fails) mismatch"
    exit 1
fi

# Test case 4: Multiple resources with exception
$EK9_BINARY tryWithResourceExceptionPaths.ek9 4 > actual_case_4.txt 2>&1
if ! diff -u expected_case_4.txt actual_case_4.txt; then
    echo "FAIL: Case 4 (multiple resources exception) mismatch"
    exit 1
fi

# Test case 5: First resource close fails
$EK9_BINARY tryWithResourceExceptionPaths.ek9 5 > actual_case_5.txt 2>&1
if ! diff -u expected_case_5.txt actual_case_5.txt; then
    echo "FAIL: Case 5 (first close fails) mismatch"
    exit 1
fi

# Test case 6: Second resource close fails
$EK9_BINARY tryWithResourceExceptionPaths.ek9 6 > actual_case_6.txt 2>&1
if ! diff -u expected_case_6.txt actual_case_6.txt; then
    echo "FAIL: Case 6 (second close fails) mismatch"
    exit 1
fi

# Test case 7: Both try and close throw
$EK9_BINARY tryWithResourceExceptionPaths.ek9 7 > actual_case_7.txt 2>&1
if ! diff -u expected_case_7.txt actual_case_7.txt; then
    echo "FAIL: Case 7 (suppressed exception) mismatch"
    exit 1
fi

echo "PASS: tryWithResourceExceptionPaths (7 cases)"

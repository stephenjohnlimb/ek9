#!/usr/bin/env bash
#
# E2E test for ifWithGuardAndCondition
# Multi-case test: Tests guard with nested condition check
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

# Test case 1: 0 (unset Optional - guard fails)
$EK9_BINARY ifWithGuardAndCondition.ek9 0 > actual_case_unset.txt 2>&1
if ! diff -u expected_case_unset.txt actual_case_unset.txt; then
    echo "FAIL: Case unset (0) mismatch"
    exit 1
fi

# Test case 2: 1 (Alpha < Beta - condition fails)
$EK9_BINARY ifWithGuardAndCondition.ek9 1 > actual_case_alpha.txt 2>&1
if ! diff -u expected_case_alpha.txt actual_case_alpha.txt; then
    echo "FAIL: Case alpha (1) mismatch"
    exit 1
fi

# Test case 3: 2 (Gamma > Beta - condition succeeds)
$EK9_BINARY ifWithGuardAndCondition.ek9 2 > actual_case_gamma.txt 2>&1
if ! diff -u expected_case_gamma.txt actual_case_gamma.txt; then
    echo "FAIL: Case gamma (2) mismatch"
    exit 1
fi

echo "PASS: ifWithGuardAndCondition (3 cases)"

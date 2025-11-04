#!/usr/bin/env bash
#
# E2E test for simpleSwitchCharacterPromotion
# Multi-case test: Tests character switch with type promotion
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

# Test case 1: character A
$EK9_BINARY simpleSwitchCharacterPromotion.ek9 A > actual_case_A.txt 2>&1
if ! diff -u expected_case_A.txt actual_case_A.txt; then
    echo "FAIL: Case A mismatch"
    exit 1
fi

# Test case 2: character D
$EK9_BINARY simpleSwitchCharacterPromotion.ek9 D > actual_case_D.txt 2>&1
if ! diff -u expected_case_D.txt actual_case_D.txt; then
    echo "FAIL: Case D mismatch"
    exit 1
fi

# Test case 3: default
$EK9_BINARY simpleSwitchCharacterPromotion.ek9 X > actual_case_X.txt 2>&1
if ! diff -u expected_case_X.txt actual_case_X.txt; then
    echo "FAIL: Case X mismatch"
    exit 1
fi

echo "PASS: simpleSwitchCharacterPromotion (3 cases)"

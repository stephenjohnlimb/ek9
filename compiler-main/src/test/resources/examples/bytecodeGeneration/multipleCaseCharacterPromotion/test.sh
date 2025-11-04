#!/usr/bin/env bash
#
# E2E test for multipleCaseCharacterPromotion
# Multi-case test: Tests switch with Character promotion (Character -> String)
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

# Test case A, D, Z: Found A, D or Z
$EK9_BINARY multipleCaseCharacterPromotion.ek9 A > actual_case_A.txt 2>&1
if ! diff -u expected_case_adz.txt actual_case_A.txt; then
    echo "FAIL: Case A mismatch"
    exit 1
fi

$EK9_BINARY multipleCaseCharacterPromotion.ek9 D > actual_case_D.txt 2>&1
if ! diff -u expected_case_adz.txt actual_case_D.txt; then
    echo "FAIL: Case D mismatch"
    exit 1
fi

$EK9_BINARY multipleCaseCharacterPromotion.ek9 Z > actual_case_Z.txt 2>&1
if ! diff -u expected_case_adz.txt actual_case_Z.txt; then
    echo "FAIL: Case Z mismatch"
    exit 1
fi

# Test case X, Y: Found X or Y
$EK9_BINARY multipleCaseCharacterPromotion.ek9 X > actual_case_X.txt 2>&1
if ! diff -u expected_case_xy.txt actual_case_X.txt; then
    echo "FAIL: Case X mismatch"
    exit 1
fi

$EK9_BINARY multipleCaseCharacterPromotion.ek9 Y > actual_case_Y.txt 2>&1
if ! diff -u expected_case_xy.txt actual_case_Y.txt; then
    echo "FAIL: Case Y mismatch"
    exit 1
fi

# Test case B: Other letter (default)
$EK9_BINARY multipleCaseCharacterPromotion.ek9 B > actual_case_B.txt 2>&1
if ! diff -u expected_case_other.txt actual_case_B.txt; then
    echo "FAIL: Case B mismatch"
    exit 1
fi

echo "PASS: multipleCaseCharacterPromotion (6 cases)"

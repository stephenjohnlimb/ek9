#!/usr/bin/env bash
#
# E2E test for forInExpression
# Tests for-in expression accumulator pattern with string concatenation
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

# Test case 1: arg = "World" (result = "Hello World")
$EK9_BINARY forInExpression.ek9 World > actual_case_World.txt 2>&1
if ! diff -u expected_case_World.txt actual_case_World.txt; then
    echo "FAIL: World mismatch"
    exit 1
fi

# Test case 2: arg = "EK9" (result = "Hello EK9")
$EK9_BINARY forInExpression.ek9 EK9 > actual_case_EK9.txt 2>&1
if ! diff -u expected_case_EK9.txt actual_case_EK9.txt; then
    echo "FAIL: EK9 mismatch"
    exit 1
fi

echo "PASS: forInExpression (2 cases)"

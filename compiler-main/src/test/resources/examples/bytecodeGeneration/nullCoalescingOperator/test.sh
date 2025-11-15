#!/usr/bin/env bash
#
# E2E test for nullCoalescingOperator
# Compiles and executes with -C flag (standard compilation)
#

set -e
cd "$(dirname "$0")"

# Project-relative paths (8 levels up to project root from nullCoalescingOperator/)
EK9_BINARY="../../../../../../../ek9-wrapper/target/bin/ek9"
EK9_HOME="../../../../../../target"

# Validate build artifacts exist
if [ ! -f "$EK9_BINARY" ]; then
    echo "ERROR: ek9 binary not found: $EK9_BINARY"
    echo "Run 'mvn clean install' from project root"
    exit 1
fi

if [ ! -f "$EK9_HOME/ek9c-jar-with-dependencies.jar" ]; then
    echo "ERROR: Compiler JAR not found: $EK9_HOME/ek9c-jar-with-dependencies.jar"
    echo "Run 'mvn clean install' from project root"
    exit 1
fi

# Clean previous run
rm -rf .ek9 actual_output.txt

# Compile with standard flag
export EK9_HOME
$EK9_BINARY -C *.ek9

# Execute using ek9 binary
$EK9_BINARY nullCoalescingOperator.ek9 > actual_output.txt 2>&1

# Validate output
if [ -f expected_output.txt ]; then
    if ! diff -u expected_output.txt actual_output.txt; then
        echo "FAIL: Output mismatch in $(basename "$(pwd)")"
        exit 1
    fi
    echo "PASS: $(basename "$(pwd)") - output matched"
else
    echo "PASS: $(basename "$(pwd)") - execution succeeded (no expected output)"
fi

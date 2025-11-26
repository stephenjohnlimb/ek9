#!/usr/bin/env bash
#
# E2E test for tryWithResourceNoCatch
# Tests try-with-resource statement (no catch, no finally)
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
rm -rf .ek9 actual_output.txt

# Compile once
export EK9_HOME
$EK9_BINARY -C *.ek9

# Run and compare output
$EK9_BINARY tryWithResourceNoCatch.ek9 > actual_output.txt 2>&1
if ! diff -u expected_output.txt actual_output.txt; then
    echo "FAIL: output mismatch"
    exit 1
fi

echo "PASS: tryWithResourceNoCatch"

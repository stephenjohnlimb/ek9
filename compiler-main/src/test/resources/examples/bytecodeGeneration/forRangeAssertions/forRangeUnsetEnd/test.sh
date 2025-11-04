#!/usr/bin/env bash
#
# E2E test for forRangeUnsetEnd
# Assertion test: Program should throw AssertionError at runtime
#

set -e
cd "$(dirname "$0")"

# Project-relative paths (9 levels up from forRangeAssertions/forRangeUnsetEnd/)
EK9_BINARY="../../../../../../../../ek9-wrapper/target/bin/ek9"
EK9_HOME="../../../../../../../target"

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
rm -rf .ek9 actual_error.txt

# Compile
export EK9_HOME
$EK9_BINARY -C *.ek9

# Execute - should throw AssertionError
$EK9_BINARY forRangeUnsetEnd.ek9 > actual_error.txt 2>&1

# Verify error contains "AssertionError"
if ! grep -q "AssertionError" actual_error.txt; then
    echo "FAIL: Expected AssertionError not found"
    cat actual_error.txt
    exit 1
fi

# Verify the error message was NOT printed (program stopped at assertion)
if grep -q "ERROR: Should have thrown AssertionError" actual_error.txt; then
    echo "FAIL: Program continued past assertion"
    cat actual_error.txt
    exit 1
fi

echo "PASS: forRangeUnsetEnd (assertion verified)"

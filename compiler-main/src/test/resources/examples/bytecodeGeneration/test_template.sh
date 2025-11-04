#!/usr/bin/env bash
#
# Template for EK9 bytecode E2E tests
# Copy this file to your test directory and modify as needed
#
# Usage:
#   cp test_template.sh yourTestDir/test.sh
#   cd yourTestDir/
#   # Edit test.sh to use -C or -Cg flag
#   # Create expected_output.txt with expected program output
#   chmod +x test.sh
#   ./test.sh
#

set -e
cd "$(dirname "$0")"

# Project-relative paths (from bytecodeGeneration/[testdir]/)
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

# Compile
export EK9_HOME

# CHOOSE ONE:
# Option A: Use -Cg for SMAP tests (validates @BYTECODE with SourceDebugExtension)
# Option B: Use -C for standard tests (no SMAP validation needed)

# For SMAP tests (andOperator, helloWorld, isSetOperator, notOperator, orOperator, xorOperator):
$EK9_BINARY -Cg *.ek9

# For standard tests (all others):
# $EK9_BINARY -C *.ek9

# Execute and capture output
# Replace "yourProgram.ek9" with the actual EK9 source filename
$EK9_BINARY yourProgram.ek9 > actual_output.txt 2>&1

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

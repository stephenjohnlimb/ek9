#!/usr/bin/env bash
#
# E2E Test Runner for Bytecode Generation
# Runs the helloWorld E2E test to validate the full ek9 binary toolchain.
#
# Note: Most bytecode tests now run via JUnit (AbstractExecutableBytecodeTest)
# for faster parallel execution. This runner validates only the E2E binary chain.
#

set -e
cd "$(dirname "$0")"

echo "=========================================="
echo "EK9 Bytecode E2E Test Runner"
echo "=========================================="
echo ""

# Validate build artifacts (from bytecodeGeneration/)
EK9_BINARY="../../../../../../ek9-wrapper/target/bin/ek9"
COMPILER_JAR="../../../../../target/ek9c-jar-with-dependencies.jar"

if [ ! -f "$EK9_BINARY" ]; then
    echo "ERROR: ek9 binary not found: $EK9_BINARY"
    echo "Run 'mvn clean install' from project root"
    exit 1
fi

if [ ! -f "$COMPILER_JAR" ]; then
    echo "ERROR: Compiler JAR not found: $COMPILER_JAR"
    echo "Run 'mvn clean install' from project root"
    exit 1
fi

echo "Found ek9 binary: $EK9_BINARY"
echo "Found compiler JAR: $COMPILER_JAR"
echo ""

# Run the E2E test (only helloWorld has test.sh)
echo "Running E2E test: helloWorld"
echo ""

cd helloWorld
if bash test.sh; then
    echo ""
    echo "=========================================="
    echo "E2E test passed!"
    echo "=========================================="
    exit 0
else
    echo ""
    echo "=========================================="
    echo "E2E test FAILED!"
    echo "=========================================="
    exit 1
fi

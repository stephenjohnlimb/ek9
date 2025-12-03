#!/bin/bash
# Script to update a single IR test file
# Usage: ./update_single_ir_test.sh TestClassName

TEST_CLASS=$1
if [ -z "$TEST_CLASS" ]; then
    echo "Usage: $0 TestClassName"
    echo "Example: $0 LoopIRTest"
    exit 1
fi

echo "Running $TEST_CLASS and capturing output..."
mvn test -Dtest="$TEST_CLASS" -pl compiler-main -q 2>&1 | tee /tmp/ir_output_${TEST_CLASS}.txt

echo ""
echo "Output saved to /tmp/ir_output_${TEST_CLASS}.txt"
echo "Now run: python3 update_ir_directives.py /tmp/ir_output_${TEST_CLASS}.txt"

#!/usr/bin/env bash
#
# Parallel E2E Test Runner for Bytecode Generation
# Runs all test.sh scripts with controlled concurrency
#

set -e
cd "$(dirname "$0")"

echo "=========================================="
echo "EK9 Bytecode E2E Test Runner (Parallel)"
echo "=========================================="
echo ""

# Validate build artifacts (from bytecodeGeneration/)
EK9_BINARY="../../../../../../ek9-wrapper/target/bin/ek9"
COMPILER_JAR="../../../../../target/ek9c-jar-with-dependencies.jar"

if [ ! -f "$EK9_BINARY" ]; then
    echo "❌ ERROR: ek9 binary not found: $EK9_BINARY"
    echo "Run 'mvn clean install' from project root"
    exit 1
fi

if [ ! -f "$COMPILER_JAR" ]; then
    echo "❌ ERROR: Compiler JAR not found: $COMPILER_JAR"
    echo "Run 'mvn clean install' from project root"
    exit 1
fi

echo "✓ Found ek9 binary: $EK9_BINARY"
echo "✓ Found compiler JAR: $COMPILER_JAR"
echo ""

# Determine concurrency (default: number of CPU cores)
JOBS=${EK9_TEST_JOBS:-$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)}
echo "Running with concurrency: $JOBS jobs"
echo ""

# Find all test scripts (at depth 2 and 3 to include assertion tests)
TEST_SCRIPTS=$(find . -mindepth 2 -maxdepth 3 -name "test.sh" -type f | sort)
TOTAL_TESTS=$(echo "$TEST_SCRIPTS" | wc -l | tr -d ' ')

echo "Found $TOTAL_TESTS tests"
echo ""

# Create temp directory for results
RESULTS_DIR=$(mktemp -d)
trap "rm -rf $RESULTS_DIR" EXIT

# Function to run a single test and capture result
run_test() {
    local test_script=$1
    local test_dir=$(dirname "$test_script")
    local test_name=$(basename "$test_dir")
    local result_file="$RESULTS_DIR/${test_name}.result"

    # Run test in subshell, capture output and exit code
    {
        if (cd "$test_dir" && bash test.sh 2>&1); then
            echo "PASS" > "$result_file"
            echo "✓ $test_name"
        else
            echo "FAIL" > "$result_file"
            echo "✗ $test_name"
        fi
    } || {
        echo "FAIL" > "$result_file"
        echo "✗ $test_name (crashed)"
    }
}

export -f run_test
export RESULTS_DIR

# Run tests in parallel using xargs
echo "Running tests..."
echo ""

if command -v xargs >/dev/null 2>&1; then
    # Use xargs with -P for parallel execution
    echo "$TEST_SCRIPTS" | xargs -P "$JOBS" -I {} bash -c 'run_test "{}"'
else
    # Fallback: manual parallel execution with background jobs
    job_count=0
    for test_script in $TEST_SCRIPTS; do
        run_test "$test_script" &
        ((job_count++))

        # Wait if we've reached max concurrent jobs
        if [ $job_count -ge $JOBS ]; then
            wait -n  # Wait for any background job to complete
            ((job_count--))
        fi
    done
    wait  # Wait for remaining jobs
fi

echo ""
echo "=========================================="

# Collect results
PASS=0
FAIL=0
FAILED_TESTS=()

for result_file in "$RESULTS_DIR"/*.result; do
    [ -e "$result_file" ] || continue  # Handle case where no tests exist
    test_name=$(basename "$result_file" .result)
    result=$(cat "$result_file")

    if [ "$result" = "PASS" ]; then
        PASS=$((PASS + 1))
    else
        FAIL=$((FAIL + 1))
        FAILED_TESTS+=("$test_name")
    fi
done

echo "Results: $PASS passed, $FAIL failed (out of $TOTAL_TESTS)"
echo "=========================================="

if [ $FAIL -gt 0 ]; then
    echo ""
    echo "Failed tests:"
    printf '  ❌ %s\n' "${FAILED_TESTS[@]}"
    echo ""
    exit 1
fi

echo ""
echo "✅ All tests passed!"
exit 0

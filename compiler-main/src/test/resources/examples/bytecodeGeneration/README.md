# EK9 Bytecode E2E Tests

End-to-end execution tests for EK9 bytecode generation using the native `ek9` binary.

## Overview

These tests validate that compiled EK9 bytecode actually executes correctly by:
1. Compiling EK9 source files with the native `ek9` binary
2. Running the generated executable
3. Comparing actual output against expected output

This complements the existing @BYTECODE directive validation by testing runtime execution.

## Prerequisites

Build the complete EK9 compiler and native binary:

```bash
cd /Users/stevelimb/IdeaProjects/ek9
mvn clean install
```

This creates:
- `ek9-wrapper/target/bin/ek9` - Native binary
- `compiler-main/target/ek9c-jar-with-dependencies.jar` - Compiler JAR

## Running Tests

### Run All Tests (Parallel)

```bash
cd compiler-main/src/test/resources/examples/bytecodeGeneration
./test_runner.sh
```

Control concurrency:
```bash
EK9_TEST_JOBS=4 ./test_runner.sh  # Run 4 tests concurrently
```

Default: Auto-detect CPU cores (`nproc` on Linux, `sysctl -n hw.ncpu` on macOS)

### Run Single Test

```bash
cd compiler-main/src/test/resources/examples/bytecodeGeneration/helloWorld
./test.sh
```

## Test Structure

Each test directory contains:

- `*.ek9` - EK9 source file (existing)
- `test.sh` - Compile and execute script (NEW)
- `expected_output.txt` - Expected stdout output (NEW)
- `actual_output.txt` - Actual output (generated, gitignored)
- `.ek9/` - Compiled bytecode (generated, gitignored)

## Adding New Tests

### 1. Copy Template

```bash
cd yourTestDirectory/
cp ../test_template.sh test.sh
chmod +x test.sh
```

### 2. Choose Compiler Flag

Edit `test.sh` and choose ONE of these options:

**Option A: SMAP Tests** (validates @BYTECODE directives with SourceDebugExtension)
```bash
$EK9_BINARY -Cg *.ek9 > actual_output.txt 2>&1
```

Use `-Cg` for these 6 tests:
- andOperator
- helloWorld
- isSetOperator
- notOperator
- orOperator
- xorOperator

**Option B: Standard Tests** (no SMAP validation)
```bash
$EK9_BINARY -C *.ek9 > actual_output.txt 2>&1
```

Use `-C` for all other tests.

### 3. Create Expected Output

Read the EK9 source file to determine expected output:

```bash
# Example: For a program that prints "Hello, World!"
echo "Hello, World!" > expected_output.txt
```

### 4. Test It

```bash
./test.sh
```

### 5. Add to Test Suite

The `test_runner.sh` automatically finds all `test.sh` scripts in subdirectories.

## Compiler Flags

| Flag | Purpose | SMAP | Use Case |
|------|---------|------|----------|
| `-C` | Force full recompilation | No | Standard bytecode tests |
| `-Cg` | Force recompilation with debug info | Yes | SMAP validation tests |

**SMAP** (Source Map Attribute Protocol) enables debuggers to map JVM bytecode back to EK9 source files.

## Multi-Case Tests

For tests that need multiple runs with different command-line arguments (e.g., switch statements, if/else):

### Example: simpleSwitchString

```bash
#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

# ... (standard setup) ...

# Compile once
export EK9_HOME
$EK9_BINARY -C *.ek9

# Test case 1
./simpleSwitchString.ek9 "test" > actual_case_test.txt 2>&1
diff -u expected_case_test.txt actual_case_test.txt

# Test case 2
./simpleSwitchString.ek9 "12345" > actual_case_12345.txt 2>&1
diff -u expected_case_12345.txt actual_case_12345.txt

# Test case 3
./simpleSwitchString.ek9 "other" > actual_case_other.txt 2>&1
diff -u expected_case_other.txt actual_case_other.txt

echo "PASS: simpleSwitchString (3 cases)"
```

Create `expected_case_*.txt` files for each test case.

## Assertion Tests

For tests that should fail with AssertionError (e.g., forRangeUnsetStart):

```bash
#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

# ... (standard setup) ...

# Compile
export EK9_HOME
$EK9_BINARY -C *.ek9

# Execute - MUST FAIL
if ./forRangeUnsetStart.ek9 > actual_error.txt 2>&1; then
    echo "FAIL: Should have thrown AssertionError"
    exit 1
fi

# Verify error contains "AssertionError"
if ! grep -q "AssertionError" actual_error.txt; then
    echo "FAIL: Expected AssertionError not found"
    exit 1
fi

echo "PASS: forRangeUnsetStart (assertion verified)"
```

## Project-Relative Paths

All paths are relative to the project root, ensuring portability:

```bash
# From: compiler-main/src/test/resources/examples/bytecodeGeneration/[testdir]/
EK9_BINARY="../../../../../../../ek9-wrapper/target/bin/ek9"
EK9_HOME="../../../../../../target"
```

No hardcoded absolute paths or environment dependencies required.

## Test Categories

### Current Tests

- **helloWorld** (1 test) - Basic "Hello, World!" program

### To Be Added

- **SMAP Tests** (5 more) - Boolean operators with debug info
- **Simple Tests** (25 tests) - Single-run programs with deterministic output
- **Multi-Case Tests** (7 tests) - Switch statements, parameterized control flow
- **Assertion Tests** (3 tests) - Programs that should throw AssertionError

Total: 41 tests planned

## Troubleshooting

### "ek9 binary not found"

Build the project first:
```bash
cd /Users/stevelimb/IdeaProjects/ek9
mvn clean install
```

### "Compiler JAR not found"

Same solution - ensure Maven build completes successfully.

### "Output mismatch"

Review the diff output:
```bash
cd testDirectory/
diff -u expected_output.txt actual_output.txt
```

Update `expected_output.txt` if the actual output is correct.

### Tests hang or fail mysteriously

Check that you're using the correct compiler flag (`-C` vs `-Cg`).

## Contributing

When adding new tests:
1. Copy `test_template.sh`
2. Choose appropriate compiler flag
3. Create accurate `expected_output.txt` by reading EK9 source
4. Test locally before committing
5. Keep test.sh scripts simple and focused

## See Also

- `test_template.sh` - Template for creating new tests
- `test_runner.sh` - Parallel test execution
- Java bytecode tests: `compiler-main/src/test/java/org/ek9lang/compiler/bytecode/`

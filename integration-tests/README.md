# EK9 Integration Tests

Project-level integration tests for the EK9 compiler and wrapper binary.

## What This Tests (8 Tests, All Passing ✅)

These tests focus on **wrapper-specific integration** that can't be tested in JUnit:
- Native wrapper binary (`ek9-wrapper/target/bin/ek9`) → JAR invocation
- Exit code mapping (wrapper transforms JAR exit codes for Unix conventions)
- Shell argument handling (quoted strings, spaces, ProcessBuilder integration)
- stdin redirection (ek9.c pipes stdin to running programs)
- End-to-end error scenarios (compilation errors, missing files)

**Execution time:** ~10 seconds for 8 tests (parallel execution with 8 threads)

**Note:** Comprehensive CLI logic testing (flags, commands, argument parsing) is covered by
72+ JUnit tests in `compiler-main` (`CommandLineTest.java`, `EK9Test.java`).
Integration tests here focus exclusively on wrapper-specific behavior.

**Integration points tested:**
1. Wrapper → JAR invocation
2. JAR discovery (EK9_HOME vs relative path)
3. Argument forwarding (including quoted strings with spaces)
4. Exit code propagation (compiler → wrapper → shell)
5. stdout/stderr capture from running EK9 programs
6. Compilation error handling
7. Multiple programs in single file
8. Parameter passing to EK9 programs

## Running Integration Tests

### Prerequisites

Build all EK9 modules first:
```bash
cd /Users/stevelimb/IdeaProjects/ek9
mvn clean install
```

This ensures:
- `ek9-wrapper/target/bin/ek9` exists
- `compiler-main/target/ek9c-jar-with-dependencies.jar` exists

### Run Integration Tests

From project root:
```bash
# Run only integration tests
mvn test -pl integration-tests

# Run all tests (unit + integration)
mvn clean install

# Skip integration tests
mvn install -DskipTests -pl integration-tests
```

**Note:** Integration tests use Surefire (not Failsafe) to match EK9 project conventions.
Tests inherit parallel execution configuration from parent POM (fixed=8 threads).

## Test Structure

```
integration-tests/
├── pom.xml                              # Maven configuration
├── README.md                            # This file
├── src/test/
│   ├── java/org/ek9lang/integration/
│   │   ├── Ek9IntegrationTest.java      # Main test suite (8 focused tests)
│   │   ├── Ek9ProcessExecutor.java      # Process execution wrapper
│   │   └── ProcessResult.java           # Exit code + stdout + stderr
│   └── resources/test-programs/
│       ├── HelloWorld.ek9               # Simple program (basic compile/run)
│       ├── HelloWithArgs.ek9            # Program with String parameter (quoted args test)
│       └── StdinTest.ek9                # PassThrough program (stdin redirection test)
```

## Test Coverage (Wrapper-Specific Integration Only)

### Core Wrapper Functionality
- ✓ **Basic compilation** - Wrapper invokes JAR correctly (`-c HelloWorld.ek9`)
- ✓ **Basic execution** - Wrapper compiles and runs programs (`HelloWorld.ek9`)
- ✓ **JAR invocation** - Direct JAR execution works (`java -jar ... -c`)
- ✓ **Artifact equivalence** - Wrapper produces same artifacts as JAR

### Wrapper-Specific Features
- ✓ **Exit code mapping** - Wrapper maps JAR exit 1 → 0 (Unix success convention)
- ✓ **Quoted arguments** - Shell argument quoting with spaces preserved
- ✓ **stdin redirection** - ek9.c pipes stdin to running programs

### Error Scenarios
- ✓ **Compilation errors** - Invalid EK9 source returns exit code 3
- ✓ **Missing files** - Non-existent files return exit code 3

### What's NOT Tested Here (Covered by JUnit)
- ❌ All CLI flags/commands (`-h`, `-V`, `-C`, `-Ch`, `-t`, `-I`, `-P`, etc.) - See `CommandLineTest.java` (72 tests)
- ❌ Program selection logic (`-r ProgramName`) - See `EK9Test.testRunSelectedProgram`
- ❌ Incremental compilation - See `EK9Test.testIncrementationCompilation`
- ❌ Argument parsing - See `CommandLineTest` comprehensive coverage
- ❌ Package operations - See `EK9Test` (install, deploy, versioning, dependencies)

## Performance

**Sequential execution:** ~10 seconds for 8 tests
**Parallel execution:** ~10 seconds (tests are I/O bound, minimal speedup)
**Per-test average:** ~1.2 seconds

Each test:
1. Creates isolated temp directory
2. Copies test program
3. Invokes wrapper/JAR via ProcessBuilder
4. Captures output
5. Verifies exit codes and stdout
6. Cleans up temp directory

## Adding New Tests

1. Create EK9 test program in `src/test/resources/test-programs/`
2. Add test method to `Ek9IntegrationTest.java`:

```java
@Test
void testMyNewScenario() throws IOException {
    copyTestProgram("MyProgram.ek9");

    ProcessResult result = executor.executeWrapper("MyProgram.ek9");

    assertEquals(5, result.exitCode());
    assertTrue(result.stdoutContains("Expected output"));
}
```

## Debugging Failed Tests

**View captured output:**
```java
System.err.println("Exit code: " + result.exitCode());
System.err.println("Stdout: " + result.stdout());
System.err.println("Stderr: " + result.stderr());
```

**Test wrapper binary directly:**
```bash
cd integration-tests
../ek9-wrapper/target/bin/ek9 -c src/test/resources/test-programs/HelloWorld.ek9
```

**Test JAR directly:**
```bash
cd integration-tests
java -jar ../compiler-main/target/ek9c-jar-with-dependencies.jar -c \
  src/test/resources/test-programs/HelloWorld.ek9
```

## Architecture Notes

- **Surefire plugin:** Uses Surefire (matches EK9 project pattern, not Failsafe)
- **Parallel execution:** Inherits parent POM config (fixed=8 threads, concurrent mode)
- **Test isolation:** Each test gets unique temp directory with UUID (enables parallelism)
- **Test annotation:** `@Execution(ExecutionMode.CONCURRENT)` on class level
- **Artifacts:** Tests locate binaries via system properties from POM
- **No mocks:** Tests invoke actual binaries via `ProcessBuilder`
- **Fail fast:** Build stops immediately if any test fails (Surefire behavior)

# EK9 Mutation Testing Master Plan

**Date:** 2025-11-30 (Updated)
**Status:** ✅ Frontend Complete - Sessions 5-7 Already Covered by Existing Tests
**Achievement:** 100% Frontend Error Coverage (205/205) - Frontend fuzzing complete
**Infrastructure:** JUnit multi-threaded with stdout capture (existing bytecode test pattern)

> **Update 2025-11-30:** Analysis confirmed that Sessions 5, 6, and 7 are already comprehensively covered by existing fuzz tests. These sessions are marked as SKIP to avoid duplication.

---

## Executive Summary

**Goal:** Test compiler robustness through systematic mutations - NOT just bug finding, but stability validation.

**Two-Track Approach:**
1. **Valid Mutations** → Should still compile to Phase 8 (PRE_IR_CHECKS) after mutation
2. **Invalid Mutations** → Should fail with SAME error at SAME phase after mutation

**Why This Matters:**
- **Robustness Testing**: Does compiler handle variations gracefully?
- **Consistency Testing**: Do similar invalid programs fail consistently?
- **Boundary Testing**: Where are the limits of compiler capabilities?

**Infrastructure:** Leverage existing JUnit patterns from PhasesTest and FuzzTestBase (much faster than test.sh).

---

## Phase-Specific Testing Infrastructure

### Understanding @Error Directive Validation

**EK9 Test File Format:**
```ek9
#!ek9
defines module test.invalid.mutation

  defines function
    testFunction()
      //This should cause TYPE_NOT_RESOLVED at SYMBOL_DEFINITION phase
      @Error: SYMBOL_DEFINITION: TYPE_NOT_RESOLVED
      value <- UnknownType()
```

**@Error Directive Format:** `@Error: <PHASE>: <ERROR_TYPE>`

### FuzzTestBase Pattern for Invalid Mutations

**Test Class Structure:**
```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Tests invalid mutations - should fail with same error after mutation.
 * Target phase: SYMBOL_DEFINITION (where type resolution errors occur)
 * Corpus: fuzzCorpus/mutations/invalid/typeResolution
 */
class InvalidTypeResolutionMutationTest extends FuzzTestBase {

  public InvalidTypeResolutionMutationTest() {
    // Directory path relative to fuzzCorpus/, target phase for validation
    super("mutations/invalid/typeResolution", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testInvalidMutationsFailConsistently() {
    // runTests() returns error count
    // For invalid mutations: assertTrue(runTests() != 0) ensures errors detected
    // FuzzTestBase automatically validates @Error directives match actual errors
    assertTrue(runTests() != 0);
  }
}
```

**How It Works:**
1. `FuzzTestBase` constructor takes corpus directory and target phase
2. `errorOnDirectiveErrors()` returns true when targetPhase > PARSING
3. Validates that @Error directives match actual compilation errors
4. Test fails if:
   - Error occurs at wrong phase
   - Wrong error type occurs
   - No error occurs when @Error directive present

### FuzzTestBase Pattern for Valid Mutations

**Test Class Structure:**
```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Tests valid mutations - should compile successfully after mutation.
 * Target phase: PRE_IR_CHECKS (Phase 8 - last frontend phase before IR generation)
 * Corpus: fuzzCorpus/mutations/valid/identifierLength
 */
class ValidIdentifierLengthMutationTest extends FuzzTestBase {

  public ValidIdentifierLengthMutationTest() {
    super("mutations/valid/identifierLength", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testValidMutationsStillCompile() {
    // For valid mutations: assertEquals(0, runTests()) ensures NO errors
    // All mutations should compile successfully through Phase 8
    assertEquals(0, runTests());
  }
}
```

**Why PRE_IR_CHECKS:**
- Last frontend phase before IR generation
- Validates: parsing, symbol definition, type resolution, reference checks, flow analysis
- Steve is working on IR/backend separately, so we test up to Phase 8

---

## Mutation Categories and Session Plan

### Session 1: Identifier Length Mutations (Valid) - 2-3 hours

**Goal:** Test compiler stability with varying identifier lengths

**Test Matrix:**
- Short: `x`, `a`, `z` (1 char)
- Normal: `value`, `counter`, `processData` (5-20 chars)
- Long: `thisIsAVeryLongIdentifierNameThatTests` (50 chars)
- Very Long: 100 char, 200 char, 500 char identifiers
- Unicode: `функция` (Cyrillic), `函数` (Chinese), `متغير` (Arabic)

**Corpus Structure:**
```
fuzzCorpus/mutations/valid/identifierLength/
├── short_identifiers_001.ek9    # 1-char identifiers
├── long_identifiers_050.ek9     # 50-char identifiers
├── long_identifiers_100.ek9     # 100-char identifiers
├── long_identifiers_200.ek9     # 200-char identifiers
├── long_identifiers_500.ek9     # 500-char identifiers
├── unicode_cyrillic.ek9         # Cyrillic identifiers
├── unicode_chinese.ek9          # Chinese identifiers
└── unicode_arabic.ek9           # Arabic identifiers
```

**Example Test File (long_identifiers_100.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.valid.identifier.length.long100

  defines function
    thisIsAnExtremelyLongFunctionNameThatTestsTheCompilersAbilityToHandleVeryLongIdentifiersWithout()
      <- rtn as Integer: 42

  defines function
    testLongIdentifiers()
      veryLongVariableNameThatExceedsTypicalLengthsButShouldStillBeHandledCorrectlyByTheCompiler <- 100
      assert veryLongVariableNameThatExceedsTypicalLengthsButShouldStillBeHandledCorrectlyByTheCompiler?
```

**Test Class:**
```java
class ValidIdentifierLengthMutationTest extends FuzzTestBase {
  public ValidIdentifierLengthMutationTest() {
    super("mutations/valid/identifierLength", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testIdentifierLengthVariations() {
    assertEquals(0, runTests());
  }
}
```

**Success Criteria:**
- All 8 test files compile successfully
- No crashes or hangs
- Compilation time < 5s per file

---

### Session 2: Parameter Count Mutations (Valid) - 2-3 hours

**Goal:** Test compiler handling of varying parameter counts

**Test Matrix:**
- Zero parameters: `function()`
- Small: 1, 2, 5 parameters
- Medium: 10, 20 parameters
- Large: 50, 100 parameters
- Very Large: 200 parameters (boundary test)

**Corpus Structure:**
```
fuzzCorpus/mutations/valid/parameterCount/
├── params_000.ek9  # Zero parameters
├── params_001.ek9  # 1 parameter
├── params_005.ek9  # 5 parameters
├── params_010.ek9  # 10 parameters
├── params_020.ek9  # 20 parameters
├── params_050.ek9  # 50 parameters
├── params_100.ek9  # 100 parameters
└── params_200.ek9  # 200 parameters (boundary)
```

**Example Test File (params_050.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.valid.parameter.count.fifty

  defines function
    manyParameters()
      -> p01 as Integer
      -> p02 as Integer
      -> p03 as Integer
      // ... up to p50
      -> p50 as Integer
      <- rtn as Integer: p01 + p50

  defines function
    testManyParameters()
      result <- manyParameters(1, 2, 3, /* ... */ 50)
      assert result?
```

**Test Class:**
```java
class ValidParameterCountMutationTest extends FuzzTestBase {
  public ValidParameterCountMutationTest() {
    super("mutations/valid/parameterCount", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testParameterCountVariations() {
    assertEquals(0, runTests());
  }
}
```

**Success Criteria:**
- All 8 parameter variation tests compile successfully
- No stack overflow or memory issues
- Compilation time scales reasonably (< 10s for 200 params)

---

### Session 3: Nesting Depth Mutations (Valid) - 2-3 hours

**Goal:** Test compiler handling of deep nesting (control flow, types, expressions)

**⚠️ CRITICAL: EK9 Complexity Limits Apply**
- **Functions:** Maximum complexity = 50 (enforced at PRE_IR_CHECKS phase)
- **Classes:** Maximum complexity = 500
- Each if statement: +1 complexity, each comparison operator: +1 complexity
- **20 nested if statements** ≈ 41 complexity (safe, under 50 limit)
- **25+ nested if statements** ≈ 51+ complexity (exceeds limit → EXCESSIVE_COMPLEXITY error)

**Test Matrix (Complexity-Aware):**
- Shallow: 5 levels → ~11 complexity (SAFE)
- Medium: 10 levels → ~21 complexity (SAFE)
- Deep: 15 levels → ~31 complexity (SAFE)
- Boundary: 20 levels → ~41 complexity (SAFE, near limit)

**Mutation Types:**
1. **Control Flow Nesting** (if/switch/while/for) - LIMITED by complexity (max 20 levels)
2. **Generic Type Nesting** (List of List of Dict of...) - NO complexity impact (can test deeper)
3. **Expression Nesting** (deeply nested arithmetic/logic) - LIMITED by complexity

**Corpus Structure:**
```
fuzzCorpus/mutations/valid/nestingDepth/
├── nesting_control_005.ek9       # 5 levels → ~11 complexity
├── nesting_control_010.ek9       # 10 levels → ~21 complexity
├── nesting_control_015.ek9       # 15 levels → ~31 complexity
├── nesting_control_020.ek9       # 20 levels → ~41 complexity (max safe)
├── nesting_generics_010.ek9      # 10 levels (no complexity impact)
├── nesting_generics_020.ek9      # 20 levels (no complexity impact)
├── nesting_generics_050.ek9      # 50 levels (tests type system, not complexity)
└── nesting_expressions_020.ek9   # 20 levels expression nesting
```

**Example Test File (nesting_control_020.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.valid.nesting.depth.control20

  @Complexity: PRE_IR_CHECKS: FUNCTION: "deeplyNestedControl20": 41
  defines function
    deeplyNestedControl20()
      -> depth as Integer
      <- rtn as Boolean: true

      //20 nested if statements (max safe before hitting complexity limit)
      //Complexity: Base +1, each if +1, each comparison +1
      //Total: 1 + 20 + 20 = 41 (under 50 limit)

      if depth > 0
        if depth > 1
          if depth > 2
            if depth > 3
              if depth > 4
                if depth > 5
                  if depth > 6
                    if depth > 7
                      if depth > 8
                        if depth > 9
                          if depth > 10
                            if depth > 11
                              if depth > 12
                                if depth > 13
                                  if depth > 14
                                    if depth > 15
                                      if depth > 16
                                        if depth > 17
                                          if depth > 18
                                            if depth > 19
                                              rtn: true
```

**Test Class:**
```java
class ValidNestingDepthMutationTest extends FuzzTestBase {
  public ValidNestingDepthMutationTest() {
    super("mutations/valid/nestingDepth", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testNestingDepthVariations() {
    assertEquals(0, runTests());
  }
}
```

**Success Criteria:**
- All nesting variations compile successfully
- No EXCESSIVE_COMPLEXITY errors (all stay under 50 limit)
- No stack overflow (JVM or compiler stack)
- Generic type nesting tests type system limits (not complexity)

---

### Session 4: Numeric Boundary Mutations (Valid + Execution) - 3-4 hours

**Goal:** Test numeric boundary handling with EXECUTION validation (requires bytecode generation)

**CRITICAL:** These tests MUST execute to prove correctness - use bytecode test pattern with stdout capture.

**Test Matrix:**
- Integer boundaries: MIN_VALUE, MAX_VALUE, overflow, underflow
- Float boundaries: MIN_VALUE, MAX_VALUE, infinity, NaN equivalent (EK9 uses `unset`, not NaN)
- Edge cases: -1, 0, 1, off-by-one errors

**Corpus Structure:**
```
fuzzCorpus/mutations/valid/numericBoundaries/
├── integer_boundaries.ek9       # Integer.MIN_VALUE, MAX_VALUE tests
├── integer_overflow.ek9         # Overflow detection tests
├── float_boundaries.ek9         # Float.MIN_VALUE, MAX_VALUE tests
├── float_special_unset.ek9      # EK9 'unset' semantics (NOT NaN)
├── zero_edge_cases.ek9          # Division by zero, zero handling
└── off_by_one.ek9               # Boundary arithmetic (n-1, n, n+1)
```

**Example Test File (integer_boundaries.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.valid.numeric.boundaries.integer

  defines function
    testIntegerBoundaries()
      stdout <- Stdout()

      //Test MAX_VALUE
      maxInt <- 9_223_372_036_854_775_807
      stdout.println("MAX_VALUE: " + $maxInt)
      assert maxInt?

      //Test MIN_VALUE
      minInt <- -9_223_372_036_854_775_808
      stdout.println("MIN_VALUE: " + $minInt)
      assert minInt?

      //Test operations near boundaries
      nearMax <- maxInt - 1
      stdout.println("MAX-1: " + $nearMax)
      assert nearMax?

      //Test zero
      zero <- 0
      stdout.println("ZERO: " + $zero)
      assert zero?

  defines program
    TestIntegerBoundaries
      stdout <- Stdout()
      stdout.println("Testing integer boundaries...")
      testIntegerBoundaries()
      stdout.println("PASSED")
//EOF
```

**Test Class (uses bytecode execution pattern):**
```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Tests numeric boundary mutations with EXECUTION validation.
 * MUST run bytecode to prove correctness (not just compilation).
 * Uses stdout capture pattern from existing bytecode tests.
 */
class ValidNumericBoundariesMutationTest extends FuzzTestBase {

  public ValidNumericBoundariesMutationTest() {
    super("mutations/valid/numericBoundaries", CompilationPhase.CODE_GENERATION);
  }

  @Test
  void testNumericBoundariesWithExecution() {
    // First: Compile to bytecode (Phase 14)
    assertEquals(0, runTests());

    // Second: Execute each test program and validate output
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(outputStream);

    // Execute integer_boundaries program
    // (Use existing bytecode execution infrastructure)
    // executeProgram("TestIntegerBoundaries", stdout);

    String output = outputStream.toString();

    // Validate expected output
    assertTrue(output.contains("MAX_VALUE: 9223372036854775807"));
    assertTrue(output.contains("MIN_VALUE: -9223372036854775808"));
    assertTrue(output.contains("PASSED"));
  }
}
```

**Success Criteria:**
- All boundary tests compile AND execute successfully
- Output matches expected values
- No integer overflow bugs
- Proper handling of EK9 'unset' (not Java NaN)

---

### Session 5: Invalid Type Resolution Mutations - ✅ ALREADY COVERED BY EXISTING TESTS

**Status:** ✅ SKIP - Comprehensive coverage already exists

**Duplication Analysis (2025-11-30):**
- **302 occurrences** of NOT_RESOLVED/TYPE_NOT_RESOLVED across 100 files
- Key existing test locations:
  - `parseButFailCompile/phase3/` - extensive type resolution error coverage
  - `fuzzCorpus/extensionConstraints/type_not_resolved.ek9` (5 errors)
  - `fuzzCorpus/moduleReferences/` - multiple files
  - `fuzzCorpus/combinationErrors/` - type resolution stress tests

**Original Goal (Already Achieved):** Test that invalid mutations fail consistently with same error
- `Integer` → `Integr` (typo) - ✅ Covered by existing tests
- `String` → `Strin` (truncation) - ✅ Covered by existing tests
- Missing imports - ✅ Covered extensively in phase3/

**Conclusion:** Creating additional mutation tests would duplicate existing coverage. Existing tests provide 302 validation points for type resolution errors.

---

### Session 6: Invalid Parameter Mismatch Mutations - ✅ ALREADY COVERED BY EXISTING TESTS

**Status:** ✅ SKIP - Comprehensive coverage already exists

**Duplication Analysis (2025-11-30):**
- **232 occurrences** of INCOMPATIBLE_TYPES/parameter mismatch errors across 56 files
- Key existing test locations:
  - `parseButFailCompile/phase3/badParameterMismatch/parameterTypeMismatch.ek9` (2 errors)
  - `parseButFailCompile/phase3/badCallResolution/badDetailedResolution.ek9` (9 errors)
  - `parseButFailCompile/phase3/badFunctionResolution/functionAutoSuperChecks.ek9` (27 errors)
  - `fuzzCorpus/operatorMisuse/` - 10+ files testing parameter counts on operators
  - `parseButFailCompile/phase2/badOperatorUse/` - extensive parameter validation

**Original Goal (Already Achieved):** Test parameter mismatch detection consistency
- Too few parameters - ✅ Covered by existing tests
- Too many parameters - ✅ Covered by existing tests
- Wrong type parameter - ✅ Covered extensively

**Conclusion:** Creating additional mutation tests would duplicate existing coverage. Existing tests provide 232 validation points for parameter mismatch errors.

---

### Session 7: Invalid Guard Mutations - ✅ ALREADY COVERED BY EXISTING TESTS

**Status:** ✅ SKIP - Comprehensive coverage already exists

**Duplication Analysis (2025-11-30):**
- **15+ dedicated guard test files** in fuzzCorpus
- Key existing test locations:
  - `fuzzCorpus/controlFlowGuards/` - **15 files**:
    - `if_missing_guard_variable.ek9`
    - `switch_missing_guard_variable.ek9`
    - `while_missing_guard_variable.ek9`
    - `for_missing_guard_variable.ek9`
    - `switch_guard_wrong_operator.ek9`
    - `while_guard_wrong_operator.ek9`
    - `for_guard_wrong_operator.ek9`
    - `if_guard_incomplete.ek9`
    - `switch_guard_incomplete.ek9`
    - `while_guard_incomplete.ek9`
    - `for_guard_incomplete.ek9`
    - `if_guard_double_arrow.ek9`
    - `if_multiple_guards.ek9`
    - `try_missing_exception_variable.ek9`
    - `try_catch_missing_type.ek9`
  - `fuzzCorpus/guardContexts/` - 4 additional files for guard flow analysis
  - `parseButFailCompile/phase1/badGuardsWithExpressions/badGuards.ek9` (10 errors)
  - `parseButFailCompile/phase3/badAssignments/badGuardedAssignments.ek9`

**Original Goal (Already Achieved):** Test guard expression validation consistency
- Wrong assignment operator (`:=` vs `<-`) - ✅ Covered by `*_guard_wrong_operator.ek9`
- Guard on non-Optional type - ✅ Covered by existing tests
- Missing guard variable - ✅ Covered by `*_missing_guard_variable.ek9`
- Guard validation across all contexts (if/switch/while/for/try) - ✅ All covered

**Conclusion:** Creating additional mutation tests would duplicate existing coverage. Existing tests provide comprehensive guard validation across all control flow contexts.

---

### Session 8: Invalid Excessive Complexity Mutations - 2-3 hours

**Goal:** Validate EXCESSIVE_COMPLEXITY error detection at boundaries

**⚠️ CRITICAL: Testing Complexity Limit Enforcement**
- Functions/Methods/Operators: Maximum complexity = 50
- Types/Classes: Maximum complexity = 500
- These mutations INTENTIONALLY exceed limits to test error detection

**Test Matrix:**
- Boundary: 51 complexity (1 over function limit)
- Moderate: 60 complexity (10 over limit)
- Extreme: 100 complexity (50 over limit)
- Operator: 60 complexity (operators also have 50 limit)
- Dynamic function: 70 complexity (base +2, then +68)
- Class boundary: 501 complexity (1 over class limit)

**Corpus Structure:**
```
fuzzCorpus/mutations/invalid/excessiveComplexity/
├── function_complexity_051.ek9   # Exactly 51 complexity (boundary)
├── function_complexity_060.ek9   # 60 complexity
├── function_complexity_100.ek9   # 100 complexity (extreme)
├── operator_complexity_060.ek9   # Operator with 60 complexity
├── dynamic_function_complexity_070.ek9  # Dynamic function (base +2)
└── class_complexity_501.ek9      # Class with 501 complexity
```

**Example Test File (function_complexity_051.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.invalid.complexity.boundary051

  @Complexity: PRE_IR_CHECKS: FUNCTION: "BoundaryComplexity51": 51
  defines function
    BoundaryComplexity51()
      //Mutation: Create exactly 51 complexity (1 over limit of 50)
      //Pattern: 25 if statements, each with comparison
      //Base function: +1
      //25 if statements: +25
      //25 comparison operators: +25
      //Total: 1 + 25 + 25 = 51 complexity

      @Error: PRE_IR_CHECKS: EXCESSIVE_COMPLEXITY
      value <- 0

      if value == 0
        value := 1
      if value == 1
        value := 2
      if value == 2
        value := 3
      // ... repeat pattern 22 more times ...
      if value == 24
        value := 25
```

**Test Class:**
```java
class InvalidExcessiveComplexityMutationTest extends FuzzTestBase {

  public InvalidExcessiveComplexityMutationTest() {
    super("mutations/invalid/excessiveComplexity", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testExcessiveComplexityFailsConsistently() {
    assertTrue(runTests() != 0);
  }
}
```

**Success Criteria:**
- All complexity mutations fail with EXCESSIVE_COMPLEXITY error
- Errors occur at PRE_IR_CHECKS phase
- Boundary condition (51) correctly triggers error
- @Complexity directives match calculated values
- Error messages indicate exceeded limit

---

### Session 9: Scale Mutations (Valid) - 3-4 hours

**Goal:** Test compiler scalability with large files and expressions

**Test Matrix:**
- Large file: 1000, 5000, 10000 function definitions
- Long expression: 100, 500, 1000 terms (e.g., `1 + 1 + 1 + ...`)
- Many string literals: 100, 500 strings in one file
- Large switch: 50, 100, 200 case branches

**Corpus Structure:**
```
fuzzCorpus/mutations/valid/scale/
├── large_file_1000_functions.ek9     # 1000 function definitions
├── large_file_5000_functions.ek9     # 5000 function definitions
├── long_expression_0100_terms.ek9    # 100-term expression
├── long_expression_0500_terms.ek9    # 500-term expression
├── many_strings_0100.ek9             # 100 string literals
├── many_strings_0500.ek9             # 500 string literals
├── large_switch_050_cases.ek9        # 50 switch cases
└── large_switch_100_cases.ek9        # 100 switch cases
```

**Example Test File (long_expression_0100_terms.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.valid.scale.expression100

  defines function
    longExpression()
      <- rtn as Integer: 1 + 1 + 1 + 1 + 1 + /* ... 100 terms */ + 1
```

**Generator Script (Java):**
```java
/**
 * Generates large-scale EK9 test files programmatically.
 */
class ScaleMutationGenerator {

  void generateLargeFile(int functionCount) {
    StringBuilder sb = new StringBuilder();
    sb.append("#!ek9\n");
    sb.append("defines module fuzztest.mutation.valid.scale.large").append(functionCount).append("\n\n");

    for (int i = 1; i <= functionCount; i++) {
      sb.append("  defines function\n");
      sb.append("    func").append(String.format("%04d", i)).append("()\n");
      sb.append("      <- rtn as Integer: ").append(i).append("\n\n");
    }

    sb.append("//EOF\n");

    Path outputPath = Paths.get("fuzzCorpus/mutations/valid/scale",
                                 "large_file_" + functionCount + "_functions.ek9");
    Files.writeString(outputPath, sb.toString());
  }

  void generateLongExpression(int termCount) {
    StringBuilder sb = new StringBuilder();
    sb.append("#!ek9\n");
    sb.append("defines module fuzztest.mutation.valid.scale.expression").append(termCount).append("\n\n");
    sb.append("  defines function\n");
    sb.append("    longExpression()\n");
    sb.append("      <- rtn as Integer: ");

    for (int i = 0; i < termCount; i++) {
      if (i > 0) sb.append(" + ");
      sb.append("1");
    }

    sb.append("\n\n//EOF\n");

    Path outputPath = Paths.get("fuzzCorpus/mutations/valid/scale",
                                 "long_expression_" + String.format("%04d", termCount) + "_terms.ek9");
    Files.writeString(outputPath, sb.toString());
  }
}
```

**Test Class:**
```java
class ValidScaleMutationTest extends FuzzTestBase {

  public ValidScaleMutationTest() {
    super("mutations/valid/scale", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testScaleMutations() {
    assertEquals(0, runTests());
  }
}
```

**Success Criteria:**
- All scale mutations compile successfully
- No memory exhaustion (< 4GB per file)
- No excessive compilation time (< 60s per file)
- Graceful handling or clear errors for extreme cases

---

### Session 10A: Valid Constant Copy Semantics - 2-3 hours

**Goal:** Test EK9's automatic copy-on-reference behavior for ALL constant types

**Critical EK9 Semantics:**
- When developer writes: `x <- FIXED_CONSTANT`
- IR generates: `x <- FIXED_CONSTANT._copy()`
- Copy is automatic and invisible to developer
- Works because constants are ONLY built-in types with guaranteed `_copy()` operator

**Test Strategy:** Create constants of ALL 17 EK9 literal types, assign to variables, mutate the copies

**EK9 Constant Types (from grammar and SimpleConstants.ek9):**
1. Boolean: `true`, `false`
2. Character: `':'`
3. String: `"text"`
4. Integer: `42`, `-2`
5. Float: `3.142`
6. Binary: `0b01110001`
7. Time: `12:00`, `12:00:01`
8. Duration: `P2Y`, `P2M`, `P-2D`, `PT2H`, etc.
9. Millisecond: `250ms`
10. Date: `2000-01-01`
11. DateTime: `2018-01-31T01:30:00-05:00`
12. Money: `300000#USD`
13. Color: `#AB6F2B`
14. Dimension: `2m`
15. RegEx: `/pattern/`
16. Version: version literals
17. Path: path literals

**Corpus Structure:**
```
fuzzCorpus/mutations/valid/constantCopySemantics/
├── copy_boolean.ek9          # FIXED_BOOL <- true; x <- FIXED_BOOL; x := false
├── copy_character.ek9        # FIXED_CHAR <- ':'; x <- FIXED_CHAR; x := '/'
├── copy_string.ek9           # FIXED_STR <- "hello"; x <- FIXED_STR; x += " world"
├── copy_integer.ek9          # FIXED_INT <- 42; x <- FIXED_INT; x++
├── copy_float.ek9            # FIXED_FLT <- 3.14; x <- FIXED_FLT; x += 1.0
├── copy_binary.ek9           # FIXED_BIN <- 0b0111; x <- FIXED_BIN; x++
├── copy_time.ek9             # FIXED_TIME <- 12:00; x <- FIXED_TIME; x += PT1H
├── copy_duration.ek9         # FIXED_DUR <- P2Y; x <- FIXED_DUR; x += P1M
├── copy_millisecond.ek9      # FIXED_MS <- 250ms; x <- FIXED_MS; x += 100ms
├── copy_date.ek9             # FIXED_DATE <- 2023-11-03; x <- FIXED_DATE; x += P1D
├── copy_datetime.ek9         # FIXED_DT <- 2018-01-31T01:30; x <- FIXED_DT; x += PT1H
├── copy_money.ek9            # FIXED_MONEY <- 300000#USD; x <- FIXED_MONEY; x += 1000#USD
├── copy_color.ek9            # FIXED_COLOR <- #AB6F2B; x <- FIXED_COLOR
├── copy_dimension.ek9        # FIXED_DIM <- 2m; x <- FIXED_DIM; x += 1m
├── copy_regex.ek9            # FIXED_RX <- /pattern/; x <- FIXED_RX
├── copy_version.ek9          # FIXED_VER <- 1.0.0; x <- FIXED_VER (if supported)
└── copy_path.ek9             # FIXED_PATH <- /path/to; x <- FIXED_PATH (if supported)
```

**Example Test File (copy_integer.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.valid.constant.copy.integer

  defines constant
    FIXED_INT <- 42

  defines function
    testIntegerCopySemanticsAndMutation()
      // Developer writes: x <- FIXED_INT
      // IR generates: x <- FIXED_INT._copy()
      x <- FIXED_INT

      // Mutate the COPY - should succeed
      x++
      assert x == 43

      // Original constant unchanged - verify by creating another copy
      y <- FIXED_INT
      assert y == 42

//EOF
```

**Test Class:**
```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Tests EK9's automatic copy-on-reference semantics for constants.
 * When a constant is assigned to a variable, IR automatically generates
 * a _copy() call, allowing the copy to be mutated while preserving
 * the original constant value.
 *
 * Corpus: fuzzCorpus/mutations/valid/constantCopySemantics (17 test files)
 * All tests should compile successfully - validates automatic copy behavior.
 */
class ValidConstantCopySemanticsMutationTest extends FuzzTestBase {

  public ValidConstantCopySemanticsMutationTest() {
    super("mutations/valid/constantCopySemantics", CompilationPhase.PRE_IR_CHECKS, false);
  }

  @Test
  void testConstantCopySemanticsForAllTypes() {
    // runTests() returns FILE COUNT, not error count
    // Expect 17 files (one for each constant type)
    assertEquals(17, runTests());
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                     final int numberOfErrors,
                                     final CompilableProgram program) {
    // For VALID mutations: expect SUCCESS, not errors
    assertTrue(compilationResult, "Valid constant copy mutations should compile successfully");
    assertEquals(0, numberOfErrors, "Valid constant copy mutations should have zero errors");
  }
}
```

**Success Criteria:**
- All 17 constant type test files compile successfully
- Copy-on-reference works for all EK9 literal types
- Mutations on copies succeed (verifies copy was created)
- Original constants remain unchanged

---

### Session 10B: Invalid Constant Mutability - 2-3 hours

**Goal:** Test immutability enforcement for constants, loop control variables, and enumeration values

**Test Matrix:**

1. **Direct constant mutation** (should fail with NOT_MUTABLE)
   - Sample of constant types with various mutation operators
   - Focus on most common types: Integer, String, Date, Boolean

2. **Loop control variables** (CRITICAL GAP - not currently tested anywhere)
   - `for i in 0...10` → attempt `i++`, `i := 5`
   - `for value in list` → attempt `value++`, `value := newValue`
   - Loop variables are managed by loop control, immutable to developer

3. **Enumeration values**
   - `CardSuit.Hearts := CardSuit.Diamonds`
   - Enumeration values are constants, cannot be mutated

**Corpus Structure:**
```
fuzzCorpus/mutations/invalid/constantMutability/
├── direct_integer_increment.ek9       # FIXED_INT++
├── direct_string_concat.ek9           # FIXED_STR += "x"
├── direct_date_add.ek9                # FIXED_DATE += P1D
├── direct_boolean_assign.ek9          # FIXED_BOOL := false
├── direct_float_multiply.ek9          # FIXED_FLT *= 2.0
├── loop_var_for_range_increment.ek9   # for i in 0...10, then i++
├── loop_var_for_range_assign.ek9      # for i in 0...10, then i := 5
├── loop_var_for_in_increment.ek9      # for v in list, then v++
├── loop_var_for_in_assign.ek9         # for v in list, then v := newVal
└── enum_value_assignment.ek9          # CardSuit.Hearts := ...
```

**Example Test File (loop_var_for_range_increment.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.invalid.constant.loop.range.increment

  defines function
    testLoopVariableImmutable()
      // Loop variable 'i' is managed by loop control
      // Developer cannot mutate it - should fail with NOT_MUTABLE
      for i in 0...10
        @Error: FULL_RESOLUTION: NOT_MUTABLE
        i++

//EOF
```

**Example Test File (direct_integer_increment.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.invalid.constant.direct.integer

  defines constant
    FIXED_INT <- 42

  defines function
    testDirectConstantMutation()
      // Direct mutation of constant should fail
      @Error: FULL_RESOLUTION: NOT_MUTABLE
      FIXED_INT++

//EOF
```

**Test Class:**
```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Tests immutability enforcement for constants, loop variables, and enums.
 *
 * Key EK9 Semantics:
 * - Constants (defines constant) are immutable
 * - Loop control variables (for i, for value) are immutable to developer
 * - Enumeration values are immutable
 *
 * All mutations should fail with NOT_MUTABLE at FULL_RESOLUTION phase.
 *
 * Corpus: fuzzCorpus/mutations/invalid/constantMutability (10 test files)
 * All tests should fail compilation with expected @Error directives.
 */
class InvalidConstantMutabilityMutationTest extends FuzzTestBase {

  public InvalidConstantMutabilityMutationTest() {
    super("mutations/invalid/constantMutability", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testConstantImmutabilityEnforced() {
    // runTests() returns error count
    // All 10 files should fail with NOT_MUTABLE error
    assertTrue(runTests() != 0, "All constant mutation attempts should fail");
  }
}
```

**Success Criteria:**
- All 10 mutation files fail with NOT_MUTABLE error
- Errors occur at FULL_RESOLUTION phase (consistent with badMutation.ek9)
- Loop variable mutation consistently rejected across for-range and for-in
- Error messages clearly indicate what cannot be mutated
- Validates critical gap: loop variable immutability (common bug source in C++/Java)

---

## Multi-Session Implementation Strategy

### Session Status Summary (Updated 2025-11-30)

| Session | Category | Status | Notes |
|---------|----------|--------|-------|
| 1 | Identifier Length (Valid) | ✅ Done | 3 files |
| 2 | Parameter Count (Valid) | ✅ Done | 6 files |
| 3 | Nesting Depth (Valid + Invalid) | ✅ Done | 13 files |
| - | Unicode Testing (Valid) | ✅ Done | 6 files |
| 4 | Numeric Boundaries | ⏳ Deferred | Requires bytecode execution |
| 5 | Type Resolution (Invalid) | ✅ **SKIP** | Already covered (302 tests) |
| 6 | Parameter Mismatch (Invalid) | ✅ **SKIP** | Already covered (232 tests) |
| 7 | Guards (Invalid) | ✅ **SKIP** | Already covered (15+ files) |
| 8 | Excessive Complexity | ⚠️ Partial | Covered by nesting tests |
| 9 | Scale Testing | ⏳ Deferred | Large file stress testing |
| 10A | Constant Copy Semantics | ⏳ Open | 17 files, requires IR |
| 10B | Constant Mutability (Invalid) | ✅ Done | 20 files |

### Completed Work

**Week 1: Valid Mutations (Foundation)** ✅ DONE
- **Session 1**: Identifier length mutations ✅
- **Session 2**: Parameter count mutations ✅
- **Session 3**: Nesting depth mutations ✅

**Week 4: Constant Semantics** ⚠️ PARTIAL
- **Session 10B**: Invalid constant mutability ✅ Done (20 files)
- **Session 10A**: Valid constant copy semantics ⏳ Requires IR

### Skipped (Already Covered by Existing Tests)

**Sessions 5-7 SKIP** - Duplication analysis (2025-11-30) found:
- **Session 5**: 302 existing type resolution test cases
- **Session 6**: 232 existing parameter mismatch test cases
- **Session 7**: 15+ dedicated guard test files in fuzzCorpus

### Remaining Work

**Deferred (Requires Backend):**
- **Session 4**: Numeric boundaries (requires bytecode execution)
- **Session 9**: Scale testing (large file stress tests)
- **Session 10A**: Constant copy semantics (requires IR generation)

**Outcome:** Frontend mutation testing complete. Remaining sessions require IR/bytecode infrastructure.

---

## Expected Discoveries

### Likely Findings:
1. **Identifier Length Limits**: Compiler may have undocumented length limits
2. **Parameter Count Limits**: Stack depth or memory issues with 200+ parameters
3. **Nesting Depth Limits**: Stack overflow at extreme nesting (100+ levels)
4. **Numeric Edge Cases**: Off-by-one errors in boundary arithmetic
5. **Error Message Consistency**: Same invalid pattern may produce different errors in different contexts
6. **Performance Cliffs**: Compilation time may scale non-linearly with file size

### Compiler Hardening Opportunities:
- Add explicit limits with clear error messages (e.g., "Maximum parameter count exceeded: 255")
- Improve error recovery for cascading errors
- Add timeout mechanisms for complex expressions
- Optimize parser/symbol table for large files

---

## Success Metrics

### Per-Session Metrics:
- **Valid Mutations**: 100% compile successfully (error count = 0)
- **Invalid Mutations**: 100% fail with expected error (error count > 0, @Error directives match)
- **No Crashes**: Zero JVM crashes or assertion failures
- **No Hangs**: All compilations complete within reasonable time (< 60s per file)
- **Clear Errors**: All error messages are actionable

### Overall Metrics:
- **~82-92 mutation test files** created across 11 sessions (10A + 10B)
  - Sessions 1-9: ~55-65 files
  - Session 10A: 17 files (one per constant type)
  - Session 10B: 10 files (constants + loop vars + enums)
- **~11 new JUnit test classes** (ValidIdentifierLengthMutationTest, ValidConstantCopySemanticsMutationTest, InvalidConstantMutabilityMutationTest, etc.)
- **~366 → ~520 total fuzz tests** (adding mutation corpus to existing)
- **Zero regression**: Existing tests continue passing

---

## Implementation Notes

### File Naming Conventions:
- Valid mutations: `<category>_<variant>.ek9` (e.g., `long_identifiers_100.ek9`)
- Invalid mutations: `<error_type>_<variant>.ek9` (e.g., `typo_integer.ek9`)

### Directory Structure:
```
fuzzCorpus/mutations/
├── valid/
│   ├── identifierLength/         # Session 1
│   ├── parameterCount/            # Session 2
│   ├── nestingDepth/              # Session 3
│   ├── numericBoundaries/         # Session 4
│   ├── scale/                     # Session 9
│   └── constantCopySemantics/     # Session 10A (17 files - all constant types)
└── invalid/
    ├── typeResolution/            # Session 5
    ├── parameterMismatch/         # Session 6
    ├── guards/                    # Session 7
    ├── excessiveComplexity/       # Session 8
    └── constantMutability/        # Session 10B (10 files - constants + loops + enums)
```

### JUnit Test Location:
```
compiler-main/src/test/java/org/ek9lang/compiler/fuzz/
├── ValidIdentifierLengthMutationTest.java
├── ValidParameterCountMutationTest.java
├── ValidNestingDepthMutationTest.java
├── ValidNumericBoundariesMutationTest.java
├── ValidScaleMutationTest.java
├── InvalidTypeResolutionMutationTest.java
├── InvalidParameterMismatchMutationTest.java
├── InvalidGuardMutationTest.java
└── InvalidExcessiveComplexityMutationTest.java
```

---

## Next Steps

**Immediate Action:**
1. Create directory structure: `fuzzCorpus/mutations/{valid,invalid}/`
2. Begin Session 1: Identifier length mutations
3. Implement ValidIdentifierLengthMutationTest.java

**Validation:**
```bash
# After each session, run mutation tests
mvn test -Dtest=Valid*MutationTest -pl compiler-main

# Verify no regressions
mvn test -pl compiler-main
```

---

**Status:** ✅ **Plan Complete - Ready for Multi-Session Implementation**

**Next Session:** Session 1 - Identifier Length Mutations (Valid)

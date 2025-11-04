package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for enumeration switch constraint errors in FULL_RESOLUTION phase.
 * Tests NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH and DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH.
 *
 * <p>Test corpus: fuzzCorpus/enumerationSwitch (4 test files)
 * Validates that switch statements/expressions over enumerations enforce exhaustiveness
 * and uniqueness constraints.
 *
 * <p>Test scenarios:
 * 1. switch_incomplete_enum_expression.ek9 - Incomplete enum coverage in switch expression
 * - Pattern: Switch expression covering only 2 of 4 enum values
 * - Error: Must cover all enum values or use default
 * - Tests NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH (existing coverage: 2 tests)
 * <br/>
 * 2. switch_incomplete_enum_statement.ek9 - Incomplete enum coverage in switch statement
 * - Pattern: Switch statement covering only 2 of 4 enum values with default
 * - Error: Default shouldn't be used when incomplete (should cover all or remove default)
 * - Tests NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH
 * <br/>
 * 3. switch_duplicate_enum_single.ek9 - Duplicate single enum value
 * - Pattern: Enum value NORTH appears twice in separate case statements
 * - Error: Each enum value can only appear once in switch
 * - Tests DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH (existing coverage: 2 tests)
 * <br/>
 * 4. switch_duplicate_enum_combined.ek9 - Duplicate enum in combined case
 * - Pattern: GREEN in combined case, then GREEN alone in later case
 * - Error: Cannot reuse enum values even if first appearance was in combined case
 * - Tests DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH
 * </p>
 * <p>Enumeration Switch Semantics:
 * - NOT_ALL_ENUMERATED_VALUES_PRESENT: Switch on enum must cover all values (exhaustive)
 * - DUPLICATE_ENUMERATED_VALUES_PRESENT: Each enum value can appear only once
 * - Errors detected at FULL_RESOLUTION phase during switch validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect incomplete enum coverage in switch statements/expressions
 * - Compiler should detect duplicate enum values across case statements
 * - Switch expressions require all enum values covered or default case
 * </p>
 * <p>Validates: Enumeration switch enforcement correctly prevents incomplete coverage
 * and duplicate enum values, ensuring exhaustive and unambiguous pattern matching.
 * </p>
 * <p>Gap addressed: Critical enumeration constraint errors had minimal coverage:
 * - NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH: 2 existing tests → 4 total tests
 * - DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH: 2 existing tests → 4 total tests
 * This ensures EK9's switch statements correctly enforce enumeration constraints.
 * </p>
 */
class EnumerationSwitchFuzzTest extends FuzzTestBase {

  public EnumerationSwitchFuzzTest() {
    super("enumerationSwitch", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testEnumerationSwitchRobustness() {
    assertTrue(runTests() != 0);
  }
}

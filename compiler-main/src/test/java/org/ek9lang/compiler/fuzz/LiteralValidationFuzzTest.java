package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for literal validation robustness.
 * Tests that compiler handles edge-case and invalid literals gracefully without crashing.
 *
 * <p><strong>IMPORTANT:</strong> This is a ROBUSTNESS test, not an error detection test.
 * All 33 test files are EXPECTED TO COMPILE SUCCESSFULLY. These tests verify that the
 * compiler does not crash when processing edge-case literal values.
 *
 * <p>Test corpus: fuzzCorpus/literalValidation (33 test files across 12 literal types)
 *
 * <p><strong>Note:</strong> Two duration literal tests (empty duration "P" and empty time part "PT")
 * were removed from testing as they parse as undefined identifiers, not invalid duration literals.
 *
 * <p><strong>Key Finding:</strong> EK9 currently performs <strong>NO compile-time literal
 * validation</strong>. All literals pass parsing (Phase 0) and symbol definition (Phase 1).
 * Invalid literals become "unset" values at runtime when Java type constructors detect
 * semantic errors.
 *
 * <p>Test scenarios by literal type:
 *
 * <p><strong>Date Literals (5 tests):</strong>
 * - invalid_month_13.ek9: Date with month 13 (valid range: 1-12)
 * - invalid_day_feb_30.ek9: February 30 (February has max 29 days)
 * - non_leap_year_feb_29.ek9: Feb 29 in non-leap year
 * - invalid_day_april_31.ek9: April 31 (April has only 30 days)
 * - zero_components.ek9: 0000-00-00 (invalid date)
 *
 * <p><strong>Time Literals (4 tests):</strong>
 * - invalid_hour_25.ek9: Hour 25 (valid range: 0-23)
 * - invalid_minute_60.ek9: Minute 60 (valid range: 0-59)
 * - invalid_second_60.ek9: Second 60 (valid range: 0-59)
 * - all_components_invalid.ek9: 99:99:99
 *
 * <p><strong>DateTime Literals (3 tests):</strong>
 * - invalid_date_part.ek9: 2024-02-30T12:00:00Z
 * - invalid_hour.ek9: 2024-01-15T25:00:00Z
 * - invalid_timezone.ek9: 2024-01-15T12:00:00+99:99
 *
 * <p><strong>Duration Literals (1 test):</strong>
 * - overflow_years.ek9: P999999999999Y
 * <br/><em>Note: P and PT literals don't parse as durations - they parse as undefined identifiers</em>
 *
 * <p><strong>Money Literals (3 tests):</strong>
 * - invalid_currency_xxx.ek9: 10.50#XXX (XXX is invalid ISO 4217 code)
 * - nonexistent_currency_aaa.ek9: 10.50#AAA
 * - overflow_amount.ek9: 99999999999999.99#USD
 *
 * <p><strong>Integer Literals (3 tests):</strong>
 * - overflow_decimal.ek9: 99999999999999999999999999
 * - overflow_hex.ek9: 0xFFFFFFFFFFFFFFFFFFFF
 * - overflow_octal.ek9: 07777777777777777777777
 *
 * <p><strong>Float Literals (3 tests):</strong>
 * - overflow_max_value.ek9: 1.7976931348623157e309
 * - underflow_zero.ek9: 1e-400
 * - extreme_exponent.ek9: 9e9999
 *
 * <p><strong>Binary Literals (2 tests):</strong>
 * - overflow_65_bits.ek9: 65-bit binary (exceeds 64-bit Long)
 * - overflow_pattern.ek9: 65-bit alternating pattern
 *
 * <p><strong>RegEx Literals (3 tests):</strong>
 * - unbalanced_brackets.ek9: /[/
 * - invalid_pattern.ek9: /(?/
 * - unclosed_group.ek9: /(?P&lt;unclosed/
 *
 * <p><strong>Version Literals (2 tests):</strong>
 * - extreme_values.ek9: 999999.999999.999999-999999
 * - edge_case_zero.ek9: 0.0.0-0
 *
 * <p><strong>Millisecond Literals (2 tests):</strong>
 * - overflow_value.ek9: 999999999999ms
 * - edge_case_zero.ek9: 0ms
 *
 * <p><strong>Path Literals (2 tests):</strong>
 * - large_array_index.ek9: $?[99999]
 * - deep_nesting.ek9: $?.level1.level2...level10
 *
 * <p><strong>Literal Validation Semantics:</strong>
 * - <strong>Current behavior:</strong> All invalid literals compile successfully
 * - <strong>Runtime behavior:</strong> Invalid values become "unset" (Java constructor validation)
 * - <strong>No compiler errors:</strong> Neither parse errors nor semantic errors
 * - <strong>Unused error types:</strong> INVALID_LITERAL, DURATION_NOT_FULLY_SPECIFIED defined but unused
 *
 * <p><strong>Expected behavior:</strong>
 * - All 35 test files should compile without errors (robustness test)
 * - Compiler should not crash or hang on edge-case literals
 * - No exceptions thrown during parsing or compilation
 *
 * <p><strong>Future Enhancement:</strong>
 * When compile-time literal validation is implemented:
 * - Add {@literal @Error: SYMBOL_DEFINITION: INVALID_LITERAL} directives to test files
 * - Convert this test to extend FuzzTestBase (expecting failures)
 * - Tests will transition from robustness verification to error detection validation
 *
 * <p>Validates: Compiler robustness when handling edge-case and semantically invalid
 * literals across all 12 supported literal types.
 *
 * <p>Gap addressed: Literal validation had ZERO fuzzing coverage. These 33 tests provide
 * comprehensive robustness testing and document that compile-time validation does not
 * currently exist. Tests verify the compiler gracefully handles invalid literals without
 * crashing, even though they become unset at runtime.
 *
 * <p><strong>Excluded from testing:</strong>
 * - P literal: Parses as undefined identifier, not as empty duration literal
 * - PT literal: Parses as undefined identifier, not as empty time part literal
 * These don't test literal validation - they test symbol resolution errors.
 *
 * <p>Documentation: All test files include comprehensive comments explaining:
 * - What invalid value is being tested
 * - Expected behavior (should fail validation)
 * - Actual behavior (compiles successfully, becomes unset at runtime)
 */
class LiteralValidationFuzzTest extends PhasesTest {

  private final CompilationPhase toPhase;

  public LiteralValidationFuzzTest() {
    super("/fuzzCorpus/literalValidation", false, false);  // verbose=false, muteErrors=false
    this.toPhase = CompilationPhase.PRE_IR_CHECKS;
  }

  @Test
  void testLiteralValidationRobustness() {
    testToPhase(toPhase);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                     CompilableProgram program) {
    assertTrue(compilationResult,
        "All literal validation files should compile successfully (robustness test)");
    assertEquals(0, numberOfErrors,
        "No compilation errors expected - literals are validated at runtime, not compile time");
  }
}

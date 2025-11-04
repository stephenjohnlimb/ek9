package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for function parameter constraint errors in FULL_RESOLUTION phase.
 * Tests FUNCTION_MUST_HAVE_NO_PARAMETERS constraint.
 *
 * <p>Test corpus: fuzzCorpus/functionParameterConstraints (2 test files)
 * Validates that stream operations (tail, skip) require zero-parameter functions.
 *
 * <p>Test scenarios:
 * 1. stream_tail_with_parameters.ek9 - Function with parameter used in stream tail
 * - Pattern: Function with Integer parameter used in tail operation
 * - Error: tail operation requires zero-parameter function
 * - Tests FUNCTION_MUST_HAVE_NO_PARAMETERS (existing coverage: 1 test)
 * <br/>
 * 2. stream_skip_with_parameters.ek9 - Function with parameter used in stream skip
 * - Pattern: Function with Integer parameter used in skip operation
 * - Error: skip operation requires zero-parameter function
 * - Tests FUNCTION_MUST_HAVE_NO_PARAMETERS
 * </p>
 * <p>Stream Operation Semantics:
 * - FUNCTION_MUST_HAVE_NO_PARAMETERS: Stream operations head/tail/skip require zero-param functions
 * - Functions can return Integer to control operation count
 * - Error detected at FULL_RESOLUTION phase during stream validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect parameter constraint violations at FULL_RESOLUTION phase
 * - Functions used with head/tail/skip must have no parameters
 * </p>
 * <p>Validates: Stream operation enforcement correctly prevents functions with
 * parameters from being used in head/tail/skip operations.
 * </p>
 * <p>Gap addressed: Critical function constraint errors had minimal coverage:
 * - FUNCTION_MUST_HAVE_NO_PARAMETERS: 1 existing test → 3 total tests
 * This ensures EK9's stream operations correctly enforce this constraint.
 * </p>
 */
class FunctionParameterConstraintsFuzzTest extends FuzzTestBase {

  public FunctionParameterConstraintsFuzzTest() {
    super("functionParameterConstraints", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testFunctionParameterConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}

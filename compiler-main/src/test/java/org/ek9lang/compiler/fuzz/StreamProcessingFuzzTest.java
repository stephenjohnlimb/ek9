package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for stream processing in FULL_RESOLUTION phase.
 * Tests stream operator validation, type flow, termination, and edge cases.
 *
 * <p>Test corpus: fuzzCorpus/streamProcessing
 * Covers errors including:
 * - STREAM_TYPE_CANNOT_CONSUME - Type mismatch in stream consumption
 * - STREAM_TYPE_CANNOT_PRODUCE - Type cannot be produced by stream
 * - STREAM_TYPE_NOT_DEFINED - Stream operation on non-streamable type
 * - STREAM_GT_REQUIRES_CLEAR - Redirect operator requires clear() method
 * - UNABLE_TO_FIND_PIPE_FOR_TYPE - Type incompatibility in pipeline
 * - FUNCTION_OR_DELEGATE_REQUIRED - Operator requires function
 * - MUST_RETURN_BOOLEAN - Filter/select must return Boolean
 * - MUST_RETURN_INTEGER - Sort/uniq function must return Integer
 * - OPERATOR_NOT_DEFINED - Missing required operators (iterator, hashcode, comparator)
 * - TYPE_MUST_BE_FUNCTION - Call/async on non-function type
 * - MISSING_ITERATE_METHOD - Type lacks iterator() for cat/for
 *
 * <p>Key Scenarios Tested:
 * 1. Stream sources - CAT with non-iterable types, FOR with invalid ranges
 * 2. Operator requirements - Missing functions, wrong signatures, void returns
 * 3. Type flow - Type mismatches through pipeline, promotion failures
 * 4. Termination - Invalid redirect targets, collect type mismatches
 * 5. Edge cases - Empty pipelines, single operators, multiple sources
 *
 * <p>Critical Language Semantics:
 * - Stream sources (cat/for) must produce types with iterator() method
 * - FILTER/SELECT require T → Boolean functions
 * - MAP requires T → U function, U must have pipe operator
 * - SORT requires comparator (<=> operator) or (T,T) → Integer function
 * - UNIQ requires hashcode (#? operator) or T → Integer function
 * - CALL/ASYNC require () → T function (no parameters)
 * - Statement termination (> >>) requires aggregate with pipe operator
 * - Expression termination (collect as) requires type with pipe operator
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at FULL_RESOLUTION phase
 * - Specific error messages reported for each invalid stream operation
 *
 * <p>Validates: Stream processing type safety, operator requirements, pipeline flow.
 *
 * <p>Gap Analysis: Existing tests (160 in parseButFailCompile + 18 fuzz) are excellent.
 * This suite adds systematic edge case coverage for:
 * - Empty source streams
 * - Single-operator pipelines
 * - Type promotion chains
 * - Multiple MAP sequences
 * - Generic type collection
 * - Abstract function usage
 * - Nested stream expressions
 */
class StreamProcessingFuzzTest extends FuzzTestBase {

  public StreamProcessingFuzzTest() {
    super("streamProcessing", CompilationPhase.FULL_RESOLUTION, true);
  }

  @Test
  void testStreamProcessingRobustness() {
    assertTrue(runTests() != 0);
  }
}

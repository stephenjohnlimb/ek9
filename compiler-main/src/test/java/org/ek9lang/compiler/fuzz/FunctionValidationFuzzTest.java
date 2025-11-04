package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for FUNCTION_MUST_RETURN_VALUE errors in FULL_RESOLUTION phase.
 * Tests stream operation requirements for functions to return values.
 *
 * <p>Test corpus: fuzzCorpus/functionValidation (2 test files)
 * <p>Covers stream call/async operations requiring functions with return values.
 *
 * <p>Test scenarios:
 * <p>1. stream_call_no_return.ek9 - Function without return used in stream call
 * <ul>
 * <li>Pattern: cat [noReturnFunc] | call &gt; collector
 * <li>Error: call operation requires function to produce values
 * <li>Tests FUNCTION_MUST_RETURN_VALUE (existing coverage: 1 test)
 * </ul>
 *
 * <p>2. stream_async_no_return.ek9 - Function without return used in stream async
 * <ul>
 * <li>Pattern: cat [noReturnFunc] | async &gt; collector
 * <li>Error: async operation requires function to produce values
 * <li>Same requirement as call but for asynchronous execution
 * </ul>
 *
 * <p><b>Stream Operation Semantics:</b>
 * <ul>
 * <li>FUNCTION_MUST_RETURN_VALUE: "function must return a value"
 * <li>Triggered in StreamAssemblyOrError.java line 526
 * <li>Applies to: call and async stream pipeline operations
 * <li>Functions in pipelines MUST produce values to flow through
 * </ul>
 *
 * <p><b>Stream Call/Async Requirements:</b>
 * <p>Grammar: cat [function1, function2] | call &gt; collector
 *
 * <p>Functions must meet BOTH requirements:
 * <ol>
 * <li>No parameters (REQUIRE_NO_ARGUMENTS)
 * <li>Must return non-Void value (FUNCTION_MUST_RETURN_VALUE)
 * </ol>
 *
 * <p><b>Valid Pattern:</b>
 * <pre>
 * getString()
 *   &lt;- rtn as String: "value"
 *
 * cat [getString] | call &gt; collector  // ✓ Valid
 * </pre>
 *
 * <p><b>Invalid Pattern (No Return):</b>
 * <pre>
 * doSomething()
 *   value &lt;- 42
 *   // No return statement - returns Void
 *
 * cat [doSomething] | call &gt; collector  // ✗ FUNCTION_MUST_RETURN_VALUE
 * </pre>
 *
 * <p><b>Invalid Pattern (With Parameters):</b>
 * <pre>
 * getString()
 *   -&gt; param as String
 *   &lt;- rtn as String: param
 *
 * cat [getString] | call &gt; collector  // ✗ REQUIRE_NO_ARGUMENTS
 * </pre>
 *
 * <p><b>Expected behavior:</b>
 * <ul>
 * <li>Compiler rejects functions returning Void in call/async operations
 * <li>All control flow paths must return values
 * <li>Stream operations require value production for pipeline flow
 * <li>Error triggered at Phase 3 (FULL_RESOLUTION) in stream validation
 * </ul>
 *
 * <p><b>Validates:</b> Stream pipeline operations correctly enforce function return value
 * requirements for call and async operators, ensuring all functions produce values
 * that can flow through the pipeline.
 *
 * <p><b>Gap addressed:</b> FUNCTION_MUST_RETURN_VALUE had minimal coverage:
 * <ul>
 * <li>Existing: 1 test in badStreamCallAsync.ek9 (call operation only)
 * <li>Added: 2 comprehensive tests covering call and async operations
 * <li>Systematic validation of stream operation return value requirements
 * </ul>
 * This ensures EK9's stream pipeline semantics correctly validate function contracts.
 *
 * <p><b>Note:</b> Operator return value validation uses different error types:
 * <ul>
 * <li>RETURNING_MISSING - for operators that need returns (compare, arithmetic, etc.)
 * <li>RETURN_VALUE_NOT_SUPPORTED - for mutators that shouldn't return (pipe, assignment, etc.)
 * </ul>
 * FUNCTION_MUST_RETURN_VALUE is specifically for stream call/async operations.
 */
class FunctionValidationFuzzTest extends FuzzTestBase {

  public FunctionValidationFuzzTest() {
    super("functionValidation", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testFunctionValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}

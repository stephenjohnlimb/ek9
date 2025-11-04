package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for MUST_RETURN_BOOLEAN errors in FULL_RESOLUTION phase.
 * Tests stream operations requiring predicate functions that return Boolean.
 *
 * <p>Test corpus: fuzzCorpus/returnTypeValidation (3 test files)
 * <p>Covers stream filter/select/split operations requiring Boolean predicates.
 *
 * <p>Test scenarios:
 * <p>1. stream_filter_returns_integer.ek9 - Filter predicate returning Integer
 * <ul>
 * <li>Pattern: cat [...] | filter with intFunc &gt; collector
 * <li>Error: Filter requires Boolean to determine inclusion
 * <li>Tests MUST_RETURN_BOOLEAN (existing coverage: 2 tests)
 * </ul>
 *
 * <p>2. stream_select_returns_string.ek9 - Select predicate returning String
 * <ul>
 * <li>Pattern: cat [...] | select with strFunc &gt; collector
 * <li>Error: Select (alias for filter) requires Boolean predicate
 * <li>Tests type mismatch with string return
 * </ul>
 *
 * <p>3. stream_split_returns_integer.ek9 - Split predicate returning Integer
 * <ul>
 * <li>Pattern: cat [...] | split by intFunc &gt; collector
 * <li>Error: Split requires Boolean to partition stream
 * <li>Tests partitioning operation type requirements
 * </ul>
 *
 * <p><b>Stream Predicate Semantics:</b>
 * <ul>
 * <li>MUST_RETURN_BOOLEAN: "Return type must be Boolean"
 * <li>Triggered in StreamAssemblyOrError.java line 314
 * <li>Applies to: filter, select, and split stream operations
 * <li>Predicates determine stream element inclusion/partitioning
 * </ul>
 *
 * <p><b>Stream Operation Requirements:</b>
 *
 * <p><b>Filter/Select Operations:</b>
 * <p>Grammar: cat [...] | filter with predicateFunc &gt; collector
 *
 * <p>Predicate function must:
 * <ol>
 * <li>Accept single parameter matching stream type
 * <li>Return Boolean (true = include, false = exclude)
 * </ol>
 *
 * <p><b>Valid Pattern:</b>
 * <pre>
 * greaterThan()
 *   -&gt; value as Integer
 *   &lt;- rtn as Boolean: value &gt; 10
 *
 * cat [1, 2, 3] | filter with greaterThan &gt; collector  // ✓ Valid
 * </pre>
 *
 * <p><b>Invalid Pattern (Wrong Return Type):</b>
 * <pre>
 * timesTwo()
 *   -&gt; value as Integer
 *   &lt;- rtn as Integer: value * 2  // Returns Integer, not Boolean
 *
 * cat [1, 2, 3] | filter with timesTwo &gt; collector  // ✗ MUST_RETURN_BOOLEAN
 * </pre>
 *
 * <p><b>Split Operation:</b>
 * <p>Grammar: cat [...] | split by predicateFunc &gt; collector
 *
 * <p>Partitions stream into list based on predicate:
 * <ul>
 * <li>Boolean true = first partition
 * <li>Boolean false = second partition
 * <li>Produces List of elementType
 * </ul>
 *
 * <p><b>Invalid Pattern:</b>
 * <pre>
 * modulo()
 *   -&gt; value as Integer
 *   &lt;- rtn as Integer: value % 2  // Returns 0 or 1, not Boolean
 *
 * cat [1, 2, 3] | split by modulo &gt; collector  // ✗ MUST_RETURN_BOOLEAN
 * </pre>
 *
 * <p><b>Expected behavior:</b>
 * <ul>
 * <li>Compiler rejects non-Boolean return types in filter/select/split predicates
 * <li>Type checking ensures predicates can make true/false decisions
 * <li>Boolean is required semantic contract for stream filtering operations
 * <li>Error triggered at Phase 3 (FULL_RESOLUTION) during stream validation
 * </ul>
 *
 * <p><b>Validates:</b> Stream filtering operations correctly enforce Boolean return type
 * requirement for predicate functions, ensuring type safety in stream pipelines.
 *
 * <p><b>Gap addressed:</b> MUST_RETURN_BOOLEAN had minimal coverage:
 * <ul>
 * <li>Existing: 2 tests in badStreamFilter.ek9 and badStreamSplit.ek9
 * <li>Added: 3 systematic tests covering filter, select, and split with different non-Boolean types
 * <li>Comprehensive validation of predicate return type requirements
 * </ul>
 * This ensures EK9's stream filtering semantics maintain type safety.
 *
 * <p><b>Note:</b> Other Boolean requirements exist in EK9:
 * <ul>
 * <li>Operator `?` (isSet) must return Boolean
 * <li>Comparison operators (`&lt;`, `&gt;`, etc.) must return Boolean
 * <li>Logical operators (`and`, `or`, `xor`) must return Boolean
 * </ul>
 * These use different error types in Phase 2 operator validation.
 */
class ReturnTypeValidationFuzzTest extends FuzzTestBase {

  public ReturnTypeValidationFuzzTest() {
    super("returnTypeValidation", CompilationPhase.FULL_RESOLUTION, false);
  }

  @Test
  void testReturnTypeValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}

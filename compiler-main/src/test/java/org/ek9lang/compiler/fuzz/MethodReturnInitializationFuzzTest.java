package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for method/function return value initialization (PRE_IR_CHECKS phase).
 * Tests RETURN_NOT_ALWAYS_INITIALISED error detection for incomplete return paths.
 *
 * <p>Test corpus: fuzzCorpus/methodReturnInitialization (1 test file)
 * Validates that component methods must initialize return values on ALL code paths,
 * similar to regular functions.
 *
 * <p>Test scenarios:
 * 1. component_method_return_incomplete_init.ek9 - Component method with incomplete return initialization
 * - Pattern: Method declares return value, initializes on if path but not else
 * - Error: Return value not initialized on all paths
 * - Expected: 1 RETURN_NOT_ALWAYS_INITIALISED error
 *
 * <p>Why This Is a Genuine Edge Case:
 * Existing tests cover:
 * - badOverridingComponentMethods2.ek9: Component methods with return value issues
 * - badOperatorReturns.ek9: Operator return value initialization
 * - Basic function return value scenarios
 * <br/>
 * This test adds:
 * - Component inheritance with abstract method override
 * - Conditional return value initialization (if without else)
 * - Focus on component-specific return value semantics
 *
 * <p>Return Value Initialization Rules:
 * - Functions/methods declaring `<- result as Type` must initialize on all paths
 * - Optional return values `<- result as Type?` still require initialization check
 * - Conditional initialization (if without else) leaves result uninitialized on some paths
 * - Component methods follow same rules as regular functions
 *
 * <p>Expected behavior:
 * - RETURN_NOT_ALWAYS_INITIALISED triggered when return missing on any path
 * - Error clearly identifies the return value and location
 * - Works correctly with component inheritance and abstract methods
 *
 * <p>Validates: EK9's return value initialization enforcement ensures methods
 * always provide valid return values, preventing uninitialized value bugs.
 *
 * <p>Total: 1 RETURN_NOT_ALWAYS_INITIALISED error across 1 test file
 */
class MethodReturnInitializationFuzzTest extends FuzzTestBase {

  public MethodReturnInitializationFuzzTest() {
    super("methodReturnInitialization", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testMethodReturnInitializationRobustness() {
    assertTrue(runTests() != 0);
  }
}

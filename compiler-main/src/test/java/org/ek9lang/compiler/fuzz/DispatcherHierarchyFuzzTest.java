package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for dispatcher hierarchy constraint validation.
 * Tests INVALID_NUMBER_OF_PARAMETERS, DISPATCHER_PURE_MISMATCH,
 * DISPATCHER_PRIVATE_IN_SUPER, DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED,
 * INCOMPATIBLE_PARAMETER_GENUS.
 *
 * <p>Test corpus: fuzzCorpus/dispatcherHierarchy (4 test files)
 * Validates dispatcher constraints in class hierarchies.
 *
 * <p>Test scenarios:
 * 1. dispatcher_param_count.ek9 - Parameter count mismatch
 * - Pattern: Dispatcher entry and matching methods have different param counts
 * - Tests INVALID_NUMBER_OF_PARAMETERS (3 errors)
 * <br/>
 * 2. dispatcher_purity_mismatch.ek9 - Purity mismatch
 * - Pattern: Pure dispatcher with non-pure matching methods
 * - Tests DISPATCHER_PURE_MISMATCH (3 errors)
 * <br/>
 * 3. dispatcher_private_super.ek9 - Private in super
 * - Pattern: Dispatcher in child class matching private super method
 * - Tests DISPATCHER_PRIVATE_IN_SUPER (3 errors)
 * <br/>
 * 4. dispatcher_multiple_entry.ek9 - Multiple entry points
 * - Pattern: Multiple methods marked as dispatcher
 * - Tests DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED, INCOMPATIBLE_PARAMETER_GENUS (7 errors)
 * </p>
 * <p>Dispatcher Semantics:
 * - Dispatchers enable polymorphic method dispatch by type
 * - All matching methods must have same parameter count
 * - Purity must be consistent across dispatcher and matched methods
 * - Private methods in super cannot be matched by dispatcher
 * - Only one method can be marked as dispatcher entry point
 * - Error detected at FULL_RESOLUTION phase
 * </p>
 * <p>Gap addressed: Dispatcher constraints had limited fuzz coverage:
 * - Tests focus on specific dispatcher violation patterns
 * - Each file targets distinct violation category
 * - 16 total error scenarios across 4 files
 * </p>
 */
class DispatcherHierarchyFuzzTest extends FuzzTestBase {

  public DispatcherHierarchyFuzzTest() {
    super("dispatcherHierarchy", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testDispatcherHierarchyRobustness() {
    assertTrue(runTests() != 0);
  }
}

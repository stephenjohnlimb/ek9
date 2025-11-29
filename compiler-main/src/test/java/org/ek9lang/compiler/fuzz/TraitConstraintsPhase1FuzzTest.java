package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for trait constraints at SYMBOL_DEFINITION phase.
 * Tests constructor and dispatcher restrictions on traits.
 *
 * <p>Test corpus: fuzzCorpus/traitConstraints/phase1 (2 test files)
 * Validates that traits cannot have constructors or dispatchers.
 *
 * <p>Test scenarios:
 * 1. trait_multiple_constructors.ek9 - Multiple constructors in trait
 * - Pattern: Trait with two constructor overloads
 * - Error: Traits are interfaces, cannot have constructors
 * - Tests TRAITS_DO_NOT_HAVE_CONSTRUCTORS (2 errors)
 * <br/>
 * 2. trait_dispatcher_method.ek9 - Dispatcher methods in trait
 * - Pattern: Trait with multiple dispatcher methods
 * - Error: Dispatchers are class-only feature
 * - Tests DISPATCH_ONLY_SUPPORTED_IN_CLASSES (2 errors)
 * </p>
 * <p>Note: Default constructor case already covered in existing tests.
 * </p>
 * <p>Trait Semantics:
 * - Traits are interfaces with optional method implementations
 * - They cannot be instantiated directly, so constructors are meaningless
 * - Dispatchers require class-level polymorphism
 * - Error detected at SYMBOL_DEFINITION phase
 * </p>
 * <p>Gap addressed:
 * - TRAITS_DO_NOT_HAVE_CONSTRUCTORS: 2 existing tests → 4 total
 * - DISPATCH_ONLY_SUPPORTED_IN_CLASSES in traits: 1 → 3 total
 * </p>
 */
class TraitConstraintsPhase1FuzzTest extends FuzzTestBase {

  public TraitConstraintsPhase1FuzzTest() {
    super("traitConstraints/phase1", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testTraitConstraintsPhase1Robustness() {
    assertTrue(runTests() != 0);
  }
}

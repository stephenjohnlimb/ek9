package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for trait hierarchy constraint validation.
 * Tests METHODS_CONFLICT, DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS.
 *
 * <p>Test corpus: fuzzCorpus/traitHierarchyConstraints (4 test files)
 * Validates trait constraints in class hierarchies.
 *
 * <p>Test scenarios:
 * 1. trait_method_conflict.ek9 - Method conflict from multiple traits
 * - Pattern: Class with multiple traits having same method
 * - Tests METHODS_CONFLICT (2 errors)
 * <br/>
 * 2. dynamic_class_abstracts.ek9 - Dynamic class missing implementations
 * - Pattern: Dynamic class with trait but no implementations
 * - Tests DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS (3 errors)
 * <br/>
 * 3. trait_super_conflict.ek9 - Super and trait method conflict
 * - Pattern: Class extends super with conflicting trait method
 * - Tests METHODS_CONFLICT (2 errors)
 * <br/>
 * 4. trait_delegation_conflict.ek9 - Delegation conflict
 * - Pattern: Trait delegation with conflicting methods
 * - Tests METHODS_CONFLICT (1 error)
 * </p>
 * <p>Trait Semantics:
 * - Traits provide interface and default implementations
 * - Conflicting methods require explicit override
 * - Dynamic classes must implement all abstract methods
 * - Delegation does not resolve method conflicts
 * - Error detected at FULL_RESOLUTION phase
 * </p>
 * <p>Gap addressed: Trait constraints had limited fuzz coverage:
 * - Tests focus on specific trait conflict patterns
 * - Each file targets distinct conflict scenario
 * - 8 total error scenarios across 4 files
 * </p>
 */
class TraitHierarchyConstraintsFuzzTest extends FuzzTestBase {

  public TraitHierarchyConstraintsFuzzTest() {
    super("traitHierarchyConstraints", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testTraitHierarchyConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}

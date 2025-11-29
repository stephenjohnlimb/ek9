package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for trait method modifier constraints at EXPLICIT_TYPE_SYMBOL_DEFINITION phase.
 * Tests access modifier restrictions on trait methods.
 *
 * <p>Test corpus: fuzzCorpus/traitConstraints/phase4 (3 test files)
 * Validates that trait methods cannot have access modifiers.
 *
 * <p>Test scenarios:
 * 1. trait_private_methods.ek9 - Private methods in trait
 * - Pattern: Trait with multiple private methods
 * - Error: All trait methods are implicitly public
 * - Tests METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT (2 errors)
 * <br/>
 * 2. trait_protected_methods.ek9 - Protected methods in trait
 * - Pattern: Trait with multiple protected methods
 * - Error: Protected is meaningless in trait context
 * - Tests METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT (3 errors)
 * <br/>
 * 3. trait_mixed_invalid_modifiers.ek9 - Mixed modifiers in trait
 * - Pattern: Trait with both private and protected methods
 * - Error: No access modifiers allowed on trait methods
 * - Tests METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT (3 errors)
 * </p>
 * <p>Trait Method Semantics:
 * - Trait methods define a contract that implementing classes must fulfill
 * - All trait methods are effectively public (part of the interface)
 * - Private/protected would violate interface contract semantics
 * - Error detected at EXPLICIT_TYPE_SYMBOL_DEFINITION phase
 * </p>
 * <p>Gap addressed:
 * - METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT: 2 existing tests (1 private, 1 protected)
 * - Now: 8 total error scenarios with multiple methods per modifier type
 * </p>
 */
class TraitConstraintsPhase4FuzzTest extends FuzzTestBase {

  public TraitConstraintsPhase4FuzzTest() {
    super("traitConstraints/phase4", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testTraitMethodModifierConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}

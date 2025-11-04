package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for trait composition constraint errors in FULL_RESOLUTION phase.
 * Tests TRAIT_BY_IDENTIFIER_NOT_SUPPORTED constraint.
 *
 * <p>Test corpus: fuzzCorpus/traitComposition (3 test files)
 * Validates that trait composition cannot use "by identifier" delegation syntax.
 *
 * <p>Test scenarios:
 * 1. trait_by_single_identifier.ek9 - Trait composition with single "by" identifier
 * - Pattern: trait of Serializable by serializer
 * - Error: Traits define interfaces without state, cannot delegate by identifier
 * - Tests TRAIT_BY_IDENTIFIER_NOT_SUPPORTED (existing coverage: 1 test)
 * <br/>
 * 2. trait_by_multiple_identifiers.ek9 - Multiple trait "by" identifiers
 * - Pattern: trait of Readable by reader, Writable by writer
 * - Error: Multiple delegation identifiers not supported for traits
 * - Tests TRAIT_BY_IDENTIFIER_NOT_SUPPORTED
 * <br/>
 * 3. trait_by_in_class.ek9 - Class using trait "by" identifier syntax
 * - Pattern: class with trait of Logger by logger
 * - Error: Classes cannot use trait delegation by identifier
 * - Tests TRAIT_BY_IDENTIFIER_NOT_SUPPORTED
 * </p>
 * <p>Type Constraint Semantics:
 * - TRAIT_BY_IDENTIFIER_NOT_SUPPORTED: Trait composition uses "with trait" not "by identifier"
 * - Error detected at FULL_RESOLUTION phase during trait validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect "by identifier" in trait composition at FULL_RESOLUTION phase
 * - Traits must be composed using proper "with trait" syntax
 * </p>
 * <p>Validates: Trait composition enforcement prevents delegation patterns that don't
 * make sense for stateless trait definitions. Traits define contracts, not implementations.
 * </p>
 * <p>Gap addressed: Critical trait composition errors had minimal coverage:
 * - TRAIT_BY_IDENTIFIER_NOT_SUPPORTED: 1 existing test → 4 total tests
 * Covers single identifier, multiple identifiers, and class-level usage.
 * This ensures EK9's trait system correctly enforces composition semantics.
 * </p>
 */
class TraitCompositionFuzzTest extends FuzzTestBase {

  public TraitCompositionFuzzTest() {
    super("traitComposition", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testTraitCompositionRobustness() {
    assertTrue(runTests() != 0);
  }
}

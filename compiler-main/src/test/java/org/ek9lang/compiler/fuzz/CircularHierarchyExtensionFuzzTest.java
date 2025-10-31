package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for circular hierarchy detection in TYPE_HIERARCHY_CHECKS phase.
 * Tests circular dependency detection for construct types not covered by existing tests.
 *
 * <p>Test corpus: fuzzCorpus/circularHierarchyExtensions
 * Covers circular hierarchies for:
 * - Records (direct and indirect circular extension)
 * - Components (direct and indirect circular extension)
 * - Deep circular chains (5+ levels)
 *
 * <p>Existing Coverage: Classes, Traits, Functions already tested in
 * examples/parseButFailCompile/phase2/circularHierarchies/
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at TYPE_HIERARCHY_CHECKS phase
 * - CIRCULAR_HIERARCHY_DETECTED errors reported
 *
 * <p>Validates: Circular hierarchy detection for records and components.
 */
class CircularHierarchyExtensionFuzzTest extends FuzzTestBase {

  public CircularHierarchyExtensionFuzzTest() {
    super("circularHierarchyExtensions", CompilationPhase.TYPE_HIERARCHY_CHECKS, false);
  }

  @Test
  void testCircularHierarchyDetectionRobustness() {
    assertTrue(runTests() != 0);
  }
}

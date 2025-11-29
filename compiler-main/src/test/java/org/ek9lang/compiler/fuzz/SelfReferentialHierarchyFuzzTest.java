package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for self-referential hierarchy detection.
 * Tests CIRCULAR_HIERARCHY_DETECTED for direct self-extension.
 *
 * <p>Test corpus: fuzzCorpus/selfReferentialHierarchy (4 test files)
 * Validates that constructs cannot extend themselves.
 *
 * <p>Test scenarios:
 * 1. class_extends_itself.ek9 - Class extends itself
 * - Pattern: Class C is C as open
 * - Error: Direct self-extension creates circular dependency
 * - Tests CIRCULAR_HIERARCHY_DETECTED (1 error)
 * <br/>
 * 2. trait_extends_itself.ek9 - Trait is itself
 * - Pattern: Trait T is T
 * - Error: Direct self-reference in trait hierarchy
 * - Tests CIRCULAR_HIERARCHY_DETECTED (1 error)
 * <br/>
 * 3. function_super_itself.ek9 - Function is itself
 * - Pattern: Function F is F as open
 * - Error: Function cannot be its own super
 * - Tests CIRCULAR_HIERARCHY_DETECTED (1 error)
 * <br/>
 * 4. multiple_self_references.ek9 - Multiple constructs self-referencing
 * - Pattern: Class, trait, and function all self-referencing
 * - Error: Each construct has circular self-reference
 * - Tests CIRCULAR_HIERARCHY_DETECTED (3 errors)
 * </p>
 * <p>Self-Referential Semantics:
 * - Self-extension is a degenerate case of circular hierarchy
 * - TypeHierarchyChecks detects A->A as circular (A already encountered)
 * - Same error code (E05020) as longer cycles like A->B->A
 * - Error detected at TYPE_HIERARCHY_CHECKS phase
 * </p>
 * <p>Expected behavior:
 * - Compiler should NOT crash on self-referential constructs
 * - Compilation should FAIL with CIRCULAR_HIERARCHY_DETECTED
 * - Clear error message about circular hierarchy
 * </p>
 * <p>Gap addressed: Self-referential cases specifically:
 * - Existing tests cover A->B->C->A cycles
 * - This suite covers direct A->A self-references
 * - Tests all construct types: class, trait, function
 * </p>
 */
class SelfReferentialHierarchyFuzzTest extends FuzzTestBase {

  public SelfReferentialHierarchyFuzzTest() {
    super("selfReferentialHierarchy", CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Test
  void testSelfReferentialHierarchyRobustness() {
    assertTrue(runTests() != 0);
  }
}

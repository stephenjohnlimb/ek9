package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for method override constraint validation.
 * Tests DOES_NOT_OVERRIDE, METHOD_OVERRIDES, METHOD_ACCESS_MODIFIERS_DIFFER.
 *
 * <p>Test corpus: fuzzCorpus/overrideConstraints (8 test files)
 * Validates override keyword usage and method signature matching.
 *
 * <p>Test scenarios:
 * 1. override_no_super_method.ek9 - Override on non-existent method
 * - Pattern: 'override' on method not in parent
 * - Tests DOES_NOT_OVERRIDE (2 errors)
 * <br/>
 * 2. override_private_method.ek9 - Override on private method
 * - Pattern: 'override' on private parent method
 * - Tests DOES_NOT_OVERRIDE (2 errors)
 * <br/>
 * 3. missing_override_keyword.ek9 - Overriding without keyword
 * - Pattern: Method overrides parent but no 'override' keyword
 * - Tests METHOD_OVERRIDES (2 errors)
 * <br/>
 * 4. access_modifier_mismatch.ek9 - Access level changes
 * - Pattern: Method with different access than parent
 * - Tests METHOD_ACCESS_MODIFIERS_DIFFER (3 errors)
 * <br/>
 * 5. signature_mismatch_override.ek9 - Wrong parameter types
 * - Pattern: Override with different parameters
 * - Tests DOES_NOT_OVERRIDE (2 errors)
 * <br/>
 * 6. return_type_mismatch_override.ek9 - Incompatible return type
 * - Pattern: Override with wrong return type
 * - Tests DOES_NOT_OVERRIDE (2 errors)
 * <br/>
 * 7. multiple_override_violations.ek9 - Several errors in one class
 * - Pattern: Multiple override constraint violations
 * - Tests mixed errors (4 errors)
 * <br/>
 * 8. override_with_override_keyword.ek9 - Override keyword with access change
 * - Pattern: Override keyword present but access changed
 * - Tests METHOD_ACCESS_MODIFIERS_DIFFER (2 errors)
 * </p>
 * <p>Override Semantics:
 * - 'override' keyword required when overriding parent methods
 * - Method signature (name + parameters) must match exactly
 * - Access modifiers cannot be changed
 * - Private methods cannot be overridden (invisible to subclass)
 * - Error detected at FULL_RESOLUTION phase
 * </p>
 * <p>Gap addressed: Override constraints had examples but thin fuzz coverage:
 * - Tests focus on specific mutation patterns
 * - Each file targets distinct violation category
 * - 19 total error scenarios across 8 files
 * </p>
 */
class OverrideConstraintsFuzzTest extends FuzzTestBase {

  public OverrideConstraintsFuzzTest() {
    super("overrideConstraints", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testOverrideConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}

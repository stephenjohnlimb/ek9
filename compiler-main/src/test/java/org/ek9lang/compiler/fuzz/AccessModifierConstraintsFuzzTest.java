package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for access modifier constraint validation.
 * Tests protected modifier restrictions in services, components, and closed classes.
 *
 * <p>Test corpus: fuzzCorpus/accessModifierConstraints (4 test files)
 * Validates that protected access modifier is only allowed in open classes.
 *
 * <p>Test scenarios:
 * 1. protected_in_service.ek9 - Protected methods in service
 * - Pattern: Service with multiple protected non-web methods
 * - Error: Services cannot have protected methods
 * - Tests METHOD_MODIFIER_PROTECTED_IN_SERVICE (2 errors)
 * <br/>
 * 2. protected_in_component.ek9 - Protected methods in component
 * - Pattern: Component with multiple protected methods
 * - Error: Components cannot have protected methods
 * - Tests METHOD_MODIFIER_PROTECTED_IN_COMPONENT (3 errors)
 * <br/>
 * 3. protected_in_closed_class.ek9 - Protected methods in closed class
 * - Pattern: Class (without 'as open') with protected methods
 * - Error: Protected only allowed in classes that can be extended
 * - Tests METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS (2 errors)
 * <br/>
 * 4. protected_mixed_constructs.ek9 - Protected across all construct types
 * - Pattern: Service, component, and closed class each with protected
 * - Error: Each construct gets its specific protected error
 * - Tests all three error codes in one file (3 errors)
 * </p>
 * <p>Access Modifier Semantics:
 * - Protected allows subclass access but prevents outside access
 * - Services: Cannot be subclassed, so protected is meaningless
 * - Components: Cannot be subclassed, so protected is meaningless
 * - Closed Classes: Cannot be extended, so protected is meaningless
 * - Error detected at EXPLICIT_TYPE_SYMBOL_DEFINITION phase
 * </p>
 * <p>Expected behavior:
 * - Compiler should NOT crash on invalid protected usage (robustness)
 * - Compilation should FAIL with appropriate protected modifier errors
 * </p>
 * <p>Gap addressed: Protected modifier constraints had minimal coverage:
 * - METHOD_MODIFIER_PROTECTED_IN_SERVICE: 1 existing test
 * - METHOD_MODIFIER_PROTECTED_IN_COMPONENT: 1 existing test
 * - METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS: 1 existing test
 * - Now: 4 test files with 10 total error scenarios
 * Covers multiple methods per construct and cross-construct validation.
 * </p>
 */
class AccessModifierConstraintsFuzzTest extends FuzzTestBase {

  public AccessModifierConstraintsFuzzTest() {
    super("accessModifierConstraints", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testAccessModifierConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}

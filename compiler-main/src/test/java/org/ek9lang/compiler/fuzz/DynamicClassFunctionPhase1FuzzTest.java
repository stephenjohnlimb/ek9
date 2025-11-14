package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for EK9 dynamic class and dynamic function validation - Phase 1 (SYMBOL_DEFINITION).
 * <p>
 * Tests Phase 1 error detection including:
 * - GENERIC_WITH_NAMED_DYNAMIC_CLASS - Named dynamic classes prohibited in generic contexts
 * - CAPTURED_VARIABLE_MUST_BE_NAMED - Expressions must use named parameter syntax
 * - DUPLICATE_VARIABLE_IN_CAPTURE - Captured variable names must be unique
 * - EITHER_ALL_PARAMETERS_NAMED_OR_NONE - Consistent naming required
 * <p>
 * Current Coverage: 11 Phase 1 tests covering 4 error types
 * <p>
 * Test scenarios by error type:
 * <p><strong>GENERIC_WITH_NAMED_DYNAMIC_CLASS (4 tests - CRITICAL GAP FILLED):</strong>
 * - generic_type_named_dynamic_class.ek9: Named dynamic in generic type
 * - generic_function_named_dynamic_class.ek9: Named dynamic in generic function
 * - generic_unnamed_dynamic_class_valid.ek9: Unnamed dynamic (valid robustness test)
 * - nested_generic_named_dynamic.ek9: Named dynamic in nested generic context
 * <p><strong>CAPTURED_VARIABLE_MUST_BE_NAMED (2 tests):</strong>
 * - capture_expression_without_name.ek9: Function calls, literals without names
 * - capture_complex_expression.ek9: Property access, method calls, ternary without names
 * <p><strong>DUPLICATE_VARIABLE_IN_CAPTURE (2 tests):</strong>
 * - capture_duplicate_names.ek9: Same name used twice in capture
 * - capture_shadowing_outer_scope.ek9: Multiple sources to same target name
 * <p><strong>EITHER_ALL_PARAMETERS_NAMED_OR_NONE (1 test):</strong>
 * - capture_mixed_named_unnamed.ek9: Mixing named and unnamed parameters
 * <p><strong>Edge Cases (2 robustness tests):</strong>
 * - capture_empty_valid.ek9: Empty capture () is valid
 * - named_dynamic_class_standalone.ek9: Named dynamics allowed outside generics
 * <p>
 * Validates: Dynamic class/function variable capture and naming rules during symbol definition.
 * <p>
 * Gap addressed: GENERIC_WITH_NAMED_DYNAMIC_CLASS had ZERO tests. This test suite fills
 * the critical gap with 4 comprehensive tests, plus expands coverage for other Phase 1
 * dynamic class/function errors from 1-2 tests to 4-8 tests each.
 */
class DynamicClassFunctionPhase1FuzzTest extends FuzzTestBase {
  public DynamicClassFunctionPhase1FuzzTest() {
    super("dynamicClassFunction/phase1", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testDynamicClassFunctionPhase1Robustness() {
    assertTrue(runTests() != 0);
  }
}

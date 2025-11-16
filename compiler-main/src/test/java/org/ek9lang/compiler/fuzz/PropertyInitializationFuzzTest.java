package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for class/component property initialization (PRE_IR_CHECKS phase).
 * Tests NEVER_INITIALISED, NOT_INITIALISED_BEFORE_USE, and EXPLICIT_CONSTRUCTOR_REQUIRED
 * error detection for aggregate properties.
 *
 * <p>Test corpus: fuzzCorpus/propertyInitialization (3 test files)
 * Validates that class and component properties must be properly initialized
 * before use, with explicit constructors required when properties are never initialized.
 *
 * <p>Test scenarios:
 * 1. class_uninitialized_property.ek9 - Class with single uninitialized property
 * - Pattern: Property declared but never initialized, used without check
 * - Errors: Property never initialized + used before initialization + constructor required
 * - Expected: 3 errors (NEVER_INITIALISED, NOT_INITIALISED_BEFORE_USE, EXPLICIT_CONSTRUCTOR_REQUIRED)
 * <br/>
 * 2. class_multiple_uninitialized_properties.ek9 - Class with multiple uninitialized properties
 * - Pattern: Two properties never initialized, both used without checks
 * - Errors: 2× NEVER_INITIALISED + 2× NOT_INITIALISED_BEFORE_USE + constructor required
 * - Expected: 5 errors total
 * <br/>
 * 3. component_uninitialized_property.ek9 - Component with uninitialized property
 * - Pattern: Component property never initialized, used in method
 * - Errors: Same pattern as class but for component
 * - Expected: 3 errors (NEVER_INITIALISED, NOT_INITIALISED_BEFORE_USE, EXPLICIT_CONSTRUCTOR_REQUIRED)
 *
 * <p>Why These Are Genuine Edge Cases:
 * Existing tests (uninitialisedAggregateProperties.ek9) cover:
 * - Basic property initialization scenarios
 * - Deep copy operators (:=:, :~:) with uninitialized properties
 * - Conditional initialization patterns
 * <br/>
 * These tests add:
 * - Components with uninitialized properties (existing only tests classes)
 * - Multiple uninitialized properties in single aggregate
 * - Focus on NEVER_INITIALISED detection (not just usage errors)
 *
 * <p>Property Initialization Rules:
 * - Properties declared as `prop as Type?` require initialization
 * - Properties must be initialized in constructor OR before each use
 * - Using `:=:` (deep copy) or `:~:` (merge) requires prior initialization check
 * - Classes/components with uninitialized properties need explicit constructors
 *
 * <p>Expected behavior:
 * - NEVER_INITIALISED triggered when property has no initialization anywhere
 * - NOT_INITIALISED_BEFORE_USE triggered when property used without check
 * - EXPLICIT_CONSTRUCTOR_REQUIRED when aggregate has uninitialized properties
 * - Error messages clearly identify problematic properties
 *
 * <p>Validates: EK9's property initialization enforcement ensures aggregate
 * safety by requiring explicit initialization patterns, preventing null access bugs.
 *
 * <p>Total: 11 errors across 3 test files
 * - 4× NEVER_INITIALISED
 * - 4× NOT_INITIALISED_BEFORE_USE
 * - 3× EXPLICIT_CONSTRUCTOR_REQUIRED
 */
class PropertyInitializationFuzzTest extends FuzzTestBase {

  public PropertyInitializationFuzzTest() {
    super("propertyInitialization", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testPropertyInitializationRobustness() {
    assertTrue(runTests() != 0);
  }
}

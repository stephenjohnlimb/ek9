package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for record method constraint validation.
 * Tests RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS constraint.
 *
 * <p>Test corpus: fuzzCorpus/recordMethodConstraints (4 test files)
 * Validates that records only allow constructors and operators, not regular methods.
 *
 * <p>Test scenarios:
 * 1. record_public_method.ek9 - Public methods in record
 * - Pattern: Record with getName() and getAge() public methods
 * - Error: Records cannot have public methods (only constructors/operators)
 * - Tests RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS (2 errors)
 * <br/>
 * 2. record_protected_method.ek9 - Protected methods in record
 * - Pattern: Record with protected formatAddress() and validateStreet() methods
 * - Error: Records cannot have protected methods
 * - Tests RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS (2 errors)
 * <br/>
 * 3. record_abstract_method.ek9 - Abstract methods in record
 * - Pattern: Abstract record with abstract process() and transform() methods
 * - Error: Records cannot have abstract methods
 * - Tests RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS (2 errors)
 * <br/>
 * 4. record_multiple_invalid_methods.ek9 - Multiple invalid method types
 * - Pattern: Record with getter, setter, and validation methods
 * - Error: Multiple method types all invalid in records
 * - Tests RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS (3 errors)
 * </p>
 * <p>Type Constraint Semantics:
 * - Records are value types with only constructors and operators
 * - Regular methods (public, protected, abstract) violate record semantics
 * - Error detected at EXPLICIT_TYPE_SYMBOL_DEFINITION phase
 * </p>
 * <p>Expected behavior:
 * - Compiler should NOT crash on malformed records (robustness)
 * - Compilation should FAIL with RECORDS_ONLY_SUPPORT errors
 * </p>
 * <p>Gap addressed: Record method constraint had minimal coverage:
 * - RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS: 1 existing test
 * - Now: 4 test files with 9 total error scenarios
 * Covers public, protected, abstract, and mixed invalid method types.
 * </p>
 */
class RecordMethodConstraintsFuzzTest extends FuzzTestBase {

  public RecordMethodConstraintsFuzzTest() {
    super("recordMethodConstraints", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testRecordMethodConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}

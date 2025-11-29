package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for enumeration duplicate value validation.
 * Tests POSSIBLE_DUPLICATE_ENUMERATED_VALUE constraint.
 *
 * <p>Test corpus: fuzzCorpus/enumerationDuplicates (4 test files)
 * Validates that enumeration values must be unique within their type.
 *
 * <p>Test scenarios:
 * 1. enum_multiple_duplicates.ek9 - Multiple different values duplicated
 * - Pattern: Priority enum with both Low and High duplicated
 * - Error: Each duplicate produces POSSIBLE_DUPLICATE_ENUMERATED_VALUE
 * - Tests multiple duplicates in single enumeration (2 errors)
 * <br/>
 * 2. enum_duplicate_first_last.ek9 - First value duplicated at end
 * - Pattern: DayOfWeek with Monday at start and end
 * - Error: Position doesn't matter, duplicates detected
 * - Tests duplicate detection across distance (1 error)
 * <br/>
 * 3. enum_multiple_types_with_duplicates.ek9 - Multiple types each with duplicates
 * - Pattern: TrafficLight, Size, LogLevel enums each with duplicate
 * - Error: Each type validated independently
 * - Tests multiple enumerations in single file (3 errors)
 * <br/>
 * 4. enum_consecutive_duplicates.ek9 - Immediately repeated values
 * - Pattern: Currency with EUR and JPY each repeated consecutively
 * - Error: Consecutive duplicates detected
 * - Tests adjacent duplicate detection (2 errors)
 * </p>
 * <p>Enumeration Semantics:
 * - Enumeration values must be unique within their containing type
 * - Duplicates detected at SYMBOL_DEFINITION phase
 * - Error message includes both locations for clarity
 * </p>
 * <p>Expected behavior:
 * - Compiler should NOT crash on duplicate values (robustness)
 * - Compilation should FAIL with POSSIBLE_DUPLICATE_ENUMERATED_VALUE
 * </p>
 * <p>Gap addressed: Enumeration duplicate validation had minimal coverage:
 * - POSSIBLE_DUPLICATE_ENUMERATED_VALUE: 1 existing test
 * - Now: 4 test files with 8 total error scenarios
 * Covers multiple duplicates, position variations, multiple types, and consecutive.
 * </p>
 */
class EnumerationDuplicatesFuzzTest extends FuzzTestBase {

  public EnumerationDuplicatesFuzzTest() {
    super("enumerationDuplicates", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testEnumerationDuplicatesRobustness() {
    assertTrue(runTests() != 0);
  }
}

package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for extension constraint validation.
 * Tests NOT_OPEN_TO_EXTENSION, TYPE_NOT_RESOLVED, INCOMPATIBLE_GENUS.
 *
 * <p>Test corpus: fuzzCorpus/extensionConstraints (5 test files)
 * Validates extension constraints across constructs.
 *
 * <p>Test scenarios:
 * 1. not_open_class.ek9 - Non-open class extension
 * - Pattern: Class extending non-open class
 * - Tests NOT_OPEN_TO_EXTENSION (2 errors)
 * <br/>
 * 2. not_open_function.ek9 - Non-abstract function extension
 * - Pattern: Function extending non-abstract function
 * - Tests NOT_OPEN_TO_EXTENSION (2 errors)
 * <br/>
 * 3. type_not_resolved.ek9 - Missing type extension
 * - Pattern: Extending non-existent types
 * - Tests TYPE_NOT_RESOLVED (4 errors)
 * <br/>
 * 4. incompatible_genus.ek9 - Incompatible construct extension
 * - Pattern: Class extends trait, function extends class
 * - Tests INCOMPATIBLE_GENUS (4 errors)
 * <br/>
 * 5. not_open_record.ek9 - Non-open record extension
 * - Pattern: Record extending non-open record
 * - Tests NOT_OPEN_TO_EXTENSION (2 errors)
 * </p>
 * <p>Extension Semantics:
 * - Classes must be open or abstract to be extended
 * - Functions must be abstract to be extended
 * - Records must be open to be extended
 * - Cannot extend across construct boundaries
 * - Error detected at EXPLICIT_TYPE_SYMBOL_DEFINITION phase
 * </p>
 * <p>Gap addressed: Extension constraints had limited fuzz coverage:
 * - Tests focus on specific extension violation patterns
 * - Each file targets distinct extension constraint
 * - 14 total error scenarios across 5 files
 * </p>
 */
class ExtensionConstraintsFuzzTest extends FuzzTestBase {

  public ExtensionConstraintsFuzzTest() {
    super("extensionConstraints", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testExtensionConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}

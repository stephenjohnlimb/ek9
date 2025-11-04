package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for text method validation errors in FULL_RESOLUTION phase.
 * Tests TEXT_METHOD_MISSING constraint.
 *
 * <p>Test corpus: fuzzCorpus/textMethodValidation (4 test files)
 * Validates that text constructs across different locales have consistent method signatures.
 *
 * <p>Test scenarios:
 * 1. text_missing_method_single.ek9 - Single method missing in one locale
 * - Pattern: Text for "en" missing farewell() that exists in "es"
 * - Error: Text methods must be consistent across all locales
 * - Tests TEXT_METHOD_MISSING (existing coverage: 1 test)
 * <br/>
 * 2. text_missing_overload.ek9 - Missing method overload across locales
 * - Pattern: Text for "en" has describe(String) but "fr" also has describe(Product)
 * - Error: All method overloads must be present in all locales
 * - Tests TEXT_METHOD_MISSING
 * <br/>
 * 3. text_missing_multiple_locales.ek9 - Missing method across multiple locales
 * - Pattern: "en" missing timeoutError() that exists in "de" and "ja"
 * - Error: When 2+ locales have a method, all must have it
 * - Tests TEXT_METHOD_MISSING
 * <br/>
 * 4. text_partial_missing.ek9 - Partial method coverage across 3 locales
 * - Pattern: "en_US" missing error(), "en_GB" missing warning(), "en_AU" has both
 * - Error: Method sets must be complete across all locale variants
 * - Tests TEXT_METHOD_MISSING
 * </p>
 * <p>Type Constraint Semantics:
 * - TEXT_METHOD_MISSING: Text constructs for same name must have identical method signatures
 * - Error detected at FULL_RESOLUTION phase during text construct validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect method signature mismatches at FULL_RESOLUTION phase
 * - All text locales for same construct must have matching methods
 * </p>
 * <p>Validates: Text method consistency enforcement prevents runtime locale errors
 * where some locales are missing methods that other locales provide.
 * </p>
 * <p>Gap addressed: Critical text validation errors had minimal coverage:
 * - TEXT_METHOD_MISSING: 1 existing test → 5 total tests
 * Covers single method missing, overload missing, multiple locales, and partial coverage.
 * This ensures EK9's internationalization system correctly enforces method consistency.
 * </p>
 */
class TextMethodValidationFuzzTest extends FuzzTestBase {

  public TextMethodValidationFuzzTest() {
    super("textMethodValidation", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testTextMethodValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}

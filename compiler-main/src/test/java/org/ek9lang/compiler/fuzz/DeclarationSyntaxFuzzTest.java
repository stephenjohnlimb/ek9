package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for declaration syntax robustness.
 * Tests parser-level handling of malformed class, function, trait, record, and operator declarations.
 *
 * <p>Each test in fuzzCorpus/declarationSyntax contains malformed declaration syntax:
 * - Missing 'defines' keyword
 * - Missing 'as' type declarations
 * - Wrong arrow directions (-> vs <-)
 * - Invalid operator symbols
 * - Malformed trait/class extension syntax
 * - Invalid method/operator modifiers
 * - Broken parameter/return syntax
 * - Invalid field initialization
 *
 * <p>We expect these to fail at PARSING phase but NOT crash the compiler.
 */
class DeclarationSyntaxFuzzTest extends FuzzTestBase {

  public DeclarationSyntaxFuzzTest() {
    super("declarationSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testDeclarationSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}

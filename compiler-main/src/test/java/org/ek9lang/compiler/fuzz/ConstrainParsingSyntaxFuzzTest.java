package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for constraint expression parsing/syntax errors.
 *
 * <p>Tests malformed constraint syntax including:
 * - Missing literal after comparison operator
 * - Unclosed parentheses in constraint expressions
 *
 * <p>These errors occur at PARSING phase when the grammar rules
 * cannot match the malformed constraint syntax.
 */
class ConstrainParsingSyntaxFuzzTest extends FuzzTestBase {

  public ConstrainParsingSyntaxFuzzTest() {
    super("constrainParsingSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testConstrainParsingSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}

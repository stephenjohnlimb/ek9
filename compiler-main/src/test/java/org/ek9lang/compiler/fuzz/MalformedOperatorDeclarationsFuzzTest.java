package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for malformed operator declarations in PARSING phase.
 * Tests syntactically invalid operator syntax that should be rejected during parsing.
 *
 * <p>Test corpus: fuzzCorpus/malformedOperatorDeclarations
 * Covers parsing errors including:
 * - Prefix operators used with postfix syntax (e.g., data#? instead of #?data)
 * - Incorrect indentation in operator return bodies
 * - Malformed operator parameter declarations
 * - Invalid operator keyword placement
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at PARSING phase
 * - Clear syntax error messages reported
 *
 * <p>Validates: Operator parsing robustness in ANTLR4 grammar.
 */
class MalformedOperatorDeclarationsFuzzTest extends FuzzTestBase {

  public MalformedOperatorDeclarationsFuzzTest() {
    super("malformedOperatorDeclarations", CompilationPhase.PARSING);
  }

  @Test
  void testMalformedOperatorRobustness() {
    assertTrue(runTests() != 0);
  }
}

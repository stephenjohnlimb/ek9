package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for malformed syntax variations.
 * Tests compiler robustness when encountering invalid EK9 syntax.
 *
 * <p>Each subdirectory in fuzzCorpus/malformedSyntax contains one invalid .ek9 file.
 * We expect these to fail compilation but NOT crash the compiler.
 */
class MalformedSyntaxFuzzTest extends FuzzTestBase {

  public MalformedSyntaxFuzzTest() {
    super("malformedSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testMalformedSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}

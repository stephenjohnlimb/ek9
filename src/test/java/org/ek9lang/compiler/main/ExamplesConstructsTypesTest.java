package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.symbols.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test simple types all compile.
 */
class ExamplesConstructsTypesTest extends FullCompilationTest {

  public ExamplesConstructsTypesTest() {
    super("/examples/constructs/types");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.enumerations", 8).test(program);
  }
}

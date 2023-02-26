package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.SymbolCheck;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test constants compile.
 */
class ConstantsCompilationTest extends FullCompilationTest {

  public ConstantsCompilationTest() {
    super("/examples/constructs/constants");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    var moduleName = "net.customer";
    new SymbolCountCheck(2,moduleName, 25).test(program);

    SymbolCheck checker = new SymbolCheck(program, moduleName, false, true, ISymbol.SymbolCategory.VARIABLE);
    //Just resolve a couple of these constants (type variable).
    checker.accept("limitNumberOfRetries");
    checker.accept("net.customer::limitNumberOfRetries");
    checker.accept("moreLimitsOnRetries");
    checker.accept("net.customer::moreLimitsOnRetries");

  }
}

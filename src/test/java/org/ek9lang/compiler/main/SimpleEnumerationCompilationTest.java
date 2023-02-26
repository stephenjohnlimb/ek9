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
 * Just test simple enumerations compile.
 */
class SimpleEnumerationCompilationTest extends FullCompilationTest {

  public SimpleEnumerationCompilationTest() {
    super("/examples/constructs/types");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.enumerations", 8).test(program);

    var moduleName = "com.customer.enumerations";
    SymbolCheck typeChecker = new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.TYPE);

    //Check unqualified and qualified in this module
    typeChecker.accept("CardRank");
    typeChecker.accept("com.customer.enumerations::CardRank");
    typeChecker.accept("CardSuit");
    typeChecker.accept("com.customer.enumerations::CardSuit");
    typeChecker.accept("Card");
    typeChecker.accept("com.customer.enumerations::Card");

    SymbolCheck functionChecker = new SymbolCheck(program, moduleName, true, true, ISymbol.SymbolCategory.FUNCTION);

    functionChecker.accept("cardCreator");
    functionChecker.accept("com.customer.enumerations::cardCreator");
    functionChecker.accept("fullRankCreator");
    functionChecker.accept("com.customer.enumerations::fullRankCreator");

  }

}

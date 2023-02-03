package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Just test simple enumerations compile.
 */
class SimpleEnumerationCompilationTest extends FullCompilationTest {

  public SimpleEnumerationCompilationTest() {
    super("/examples/constructs/types");
  }

  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.enumerations", 8).test(program);

    var theModule = program.getParsedModules("com.customer.enumerations");
    var parsedModule = theModule.get(0);

    var cardRank = parsedModule
        .getModuleScope()
        .resolveInThisScopeOnly(new TypeSymbolSearch("com.customer.enumerations::CardRank"));
    assertTrue(cardRank.isPresent());
    assertEquals(ISymbol.SymbolGenus.CLASS_ENUMERATION, cardRank.get().getGenus());

    var cardSuit = parsedModule
        .getModuleScope()
        .resolveInThisScopeOnly(new TypeSymbolSearch("com.customer.enumerations::CardSuit"));
    assertTrue(cardSuit.isPresent());
    assertEquals(ISymbol.SymbolGenus.CLASS_ENUMERATION, cardSuit.get().getGenus());

    var card = parsedModule
        .getModuleScope()
        .resolveInThisScopeOnly(new TypeSymbolSearch("com.customer.enumerations::Card"));
    assertTrue(card.isPresent());
    assertEquals(ISymbol.SymbolGenus.RECORD, card.get().getGenus());

    var cardCreator = parsedModule
        .getModuleScope()
        .resolveInThisScopeOnly(new FunctionSymbolSearch("com.customer.enumerations::cardCreator"));
    assertTrue(cardCreator.isPresent());
    assertEquals(ISymbol.SymbolGenus.FUNCTION_TRAIT, cardCreator.get().getGenus());

    var fullRankCreator = parsedModule
        .getModuleScope()
        .resolveInThisScopeOnly(new FunctionSymbolSearch("com.customer.enumerations::fullRankCreator"));
    assertTrue(fullRankCreator.isPresent());
    assertEquals(ISymbol.SymbolGenus.FUNCTION, fullRankCreator.get().getGenus());
  }

}

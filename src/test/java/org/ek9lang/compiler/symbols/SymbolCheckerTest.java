package org.ek9lang.compiler.symbols;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.support.AggregateSymbolCreator;
import org.ek9lang.compiler.symbols.support.FunctionSymbolCreator;
import org.ek9lang.compiler.symbols.support.SymbolChecker;
import org.ek9lang.compiler.symbols.support.VariableSymbolCreator;
import org.junit.jupiter.api.Test;

class SymbolCheckerTest {

  @Test
  void detectDuplicateTypeSymbols() {
    checkViaCreatorFunction(new AggregateSymbolCreator());
  }

  @Test
  void detectedDuplicateFunctionSymbols() {
    checkViaCreatorFunction(new FunctionSymbolCreator());
  }

  @Test
  void detectedDuplicateVariableSymbols() {
    checkViaCreatorFunction(new VariableSymbolCreator());
  }

  private void checkViaCreatorFunction(BiFunction<String, SymbolTable, ISymbol> creator) {
    ErrorListener errorListener = new ErrorListener("aTest");
    SymbolChecker underTest = new SymbolChecker(errorListener);

    //Just an example of a symbol table we will check for duplicate via SymbolChecker.
    SymbolTable global = new SymbolTable();
    var someType = creator.apply("SomeThing", global);

    global.define(someType);

    //Now do a search via the SymbolChecker
    var someTypeDuplicate = creator.apply("SomeThing", global);
    var expectDuplicate = underTest.errorsIfSymbolAlreadyDefined(global, someTypeDuplicate, true);
    assertTrue(expectDuplicate);
  }
}

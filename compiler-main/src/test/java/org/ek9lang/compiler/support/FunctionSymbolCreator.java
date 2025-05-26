package org.ek9lang.compiler.support;

import java.util.function.BiFunction;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolTable;

/**
 * Just used in testing to create a new function type
 */
public class FunctionSymbolCreator implements BiFunction<String, SymbolTable, ISymbol> {
  @Override
  public ISymbol apply(String functionName, SymbolTable inSymbolTable) {
    var newType = new FunctionSymbol(functionName, inSymbolTable);
    return new SyntheticSymbolCompletion().apply(newType, inSymbolTable.getScopeName());
  }
}

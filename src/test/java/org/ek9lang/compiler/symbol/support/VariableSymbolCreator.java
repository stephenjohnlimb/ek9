package org.ek9lang.compiler.symbol.support;

import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.SymbolTable;
import org.ek9lang.compiler.symbol.VariableSymbol;

/**
 * Just used in testing to create a variable Symbol.
 */
public class VariableSymbolCreator implements BiFunction<String, SymbolTable, ISymbol> {
  @Override
  public ISymbol apply(String variableName, SymbolTable inSymbolTable) {
    var newType = new VariableSymbol(variableName);
    return new SyntheticSymbolCompletion().apply(newType, inSymbolTable.getScopeName());
  }
}

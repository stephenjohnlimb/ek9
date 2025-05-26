package org.ek9lang.compiler.support;

import java.util.function.BiFunction;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolTable;

/**
 * Just used in testing to create a new aggregate type
 */
public class AggregateSymbolCreator implements BiFunction<String, SymbolTable, ISymbol> {
  @Override
  public ISymbol apply(String typeName, SymbolTable inSymbolTable) {
    var newType = new AggregateSymbol(typeName, inSymbolTable);
    return new SyntheticSymbolCompletion().apply(newType, inSymbolTable.getScopeName());
  }
}

package org.ek9lang.compiler.symbols.support;

import java.util.function.BiFunction;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolTable;

/**
 * Just used in testing to add new named types to a symbol table.
 */
public class TypeCreator implements BiFunction<String, SymbolTable, ISymbol> {
  private final AggregateSymbolCreator creator = new AggregateSymbolCreator();

  @Override
  public ISymbol apply(String typeName, SymbolTable inSymbolTable) {
    var newType = creator.apply(typeName, inSymbolTable);
    inSymbolTable.define(newType);
    return newType;
  }
}

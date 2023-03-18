package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.AggregateFactory;
import org.junit.jupiter.api.BeforeEach;

public class AbstractSymbolTestBase {
  protected AggregateFactory support = new AggregateFactory();
  protected IScope symbolTable = new SymbolTable();

  @BeforeEach
  public void setupBasicSymbols() {
    symbolTable = new SymbolTable();
    symbolTable.define(new AggregateSymbol("Integer", symbolTable));
    symbolTable.define(new AggregateSymbol("String", symbolTable));
    symbolTable.define(new AggregateSymbol("Boolean", symbolTable));
    symbolTable.define(new AggregateSymbol("Date", symbolTable));
    symbolTable.define(new AggregateSymbol("Void", symbolTable));
  }
}

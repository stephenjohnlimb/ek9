package org.ek9lang.compiler.symbols;

/**
 * Used by functions and methods as they may return a symbol (variable).
 */
public interface IMayReturnSymbol {
  boolean isReturningSymbolPresent();

  ISymbol getReturningSymbol();

  void setReturningSymbol(final VariableSymbol returningSymbol);
}

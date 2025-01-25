package org.ek9lang.compiler.symbols;

/**
 * Used by functions and methods as they may return a symbol (variable).
 */
public interface IMayReturnSymbol {
  default boolean isReturningSymbolPresent() {
    return false;
  }

  default VariableSymbol getReturningSymbol() {
    return null;
  }

  default void setReturningSymbol(final VariableSymbol returningSymbol) {
    //No operation.
  }
}

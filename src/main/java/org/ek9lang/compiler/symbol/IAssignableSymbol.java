package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.Token;

/**
 * A Symbol that is assignable (as some may not be).
 */
public interface IAssignableSymbol {
  default boolean isInitialised() {
    return getInitialisedBy() != null;
  }

  Token getInitialisedBy();

  void setInitialisedBy(Token initialisedBy);
}

package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.Token;

/**
 * Used in various ways to hold a token from the parse, so we can reference back to source code.
 */
public interface ITokenReference {
  Token getSourceToken();

  void setSourceToken(Token sourceToken);
}

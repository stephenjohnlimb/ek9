package org.ek9lang.compiler.symbols;

import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Used in various ways to hold a token from the parse, so we can reference back to source code.
 */
public interface ITokenReference {
  IToken getSourceToken();

  void setSourceToken(IToken sourceToken);

}

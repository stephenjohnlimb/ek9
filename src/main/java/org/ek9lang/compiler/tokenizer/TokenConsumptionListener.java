package org.ek9lang.compiler.tokenizer;

import org.antlr.v4.runtime.Token;

/**
 * Listen as Tokens are consumed as they are pulled from the Lexer into the parser.
 */
public interface TokenConsumptionListener {
  void tokenConsumed(Token token);
}

package org.ek9lang.compiler.main.phases.definition;

import java.util.function.Function;
import org.antlr.v4.runtime.Token;

/**
 * Creates a suitable scope name for a token position.
 */
public class BlockScopeName implements Function<Token, String> {

  @Override
  public String apply(Token token) {
    return "Line-" + token.getLine() + "-Position-" + token.getCharPositionInLine();
  }
}

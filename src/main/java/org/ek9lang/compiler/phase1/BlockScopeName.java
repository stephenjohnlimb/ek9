package org.ek9lang.compiler.phase1;

import java.util.function.Function;
import org.antlr.v4.runtime.Token;

/**
 * Creates a suitable scope name for a token position.
 */
final class BlockScopeName implements Function<Token, String> {

  BlockScopeName() {

  }

  @Override
  public String apply(final Token token) {
    return "Line-" + token.getLine() + "-Position-" + token.getCharPositionInLine();
  }
}

package org.ek9lang.compiler.phase1;

import java.util.function.Function;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Creates a suitable scope name for a token position.
 */
final class BlockScopeName implements Function<IToken, String> {

  BlockScopeName() {

  }

  @Override
  public String apply(final IToken token) {
    return "Line-" + token.getLine() + "-Position-" + token.getCharPositionInLine();
  }
}

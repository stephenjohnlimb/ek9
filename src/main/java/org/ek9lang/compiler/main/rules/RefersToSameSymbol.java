package org.ek9lang.compiler.main.rules;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Rather than just check if the actual ISymbols are the same, this code checks if two
 * symbols actually refer to the same source token from the same source file.
 */
public class RefersToSameSymbol implements BiPredicate<ISymbol, ISymbol> {
  @Override
  public boolean test(final ISymbol s1, final ISymbol s2) {
    if (s1 != null && s2 != null && s1.getSourceToken() != null && s2.getSourceToken() != null) {
      var tok1 = s1.getSourceToken().getTokenSource();
      var tok2 = s2.getSourceToken().getTokenSource();
      var tok1Index = s1.getSourceToken().getTokenIndex();
      var tok2Index = s2.getSourceToken().getTokenIndex();
      return tok1.equals(tok2) && tok1Index == tok2Index;
    }
    return false;
  }
}

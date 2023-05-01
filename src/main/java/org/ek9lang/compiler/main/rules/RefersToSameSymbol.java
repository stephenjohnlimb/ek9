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
      return s1.getSourceToken().getTokenSource().equals(s2.getSourceToken().getTokenSource())
          && s1.getSourceToken().getTokenIndex() == s2.getSourceToken().getTokenIndex();
    }
    return false;
  }
}

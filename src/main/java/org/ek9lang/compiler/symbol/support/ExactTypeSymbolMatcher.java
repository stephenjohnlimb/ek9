package org.ek9lang.compiler.symbol.support;

import java.util.List;
import java.util.function.BiPredicate;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Just checks if the two lists of 'type' symbols match. i.e. are exactly the same type.
 */
public class ExactTypeSymbolMatcher implements BiPredicate<List<ISymbol>, List<ISymbol>> {

  @Override
  public boolean test(final List<ISymbol> list1, final List<ISymbol> list2) {
    if (list1 != null && list2 != null && list1.size() == list2.size()) {
      for (int i = 0; i < list1.size(); i++) {
        if (!list1.get(i).isExactSameType(list2.get(i))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}

package org.ek9lang.compiler.symbols.support;

import java.util.List;
import java.util.function.BiPredicate;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks that the symbols in list one and list two are each the same for the corresponding position in the list.o
 */
public class TypeListComparator implements BiPredicate<List<ISymbol>, List<ISymbol>> {
  @Override
  public boolean test(List<ISymbol> listOne, List<ISymbol> listTwo) {
    if (listOne.isEmpty() && listTwo.isEmpty()) {
      return true;
    }
    if (listOne.size() != listTwo.size()) {
      return false;
    }
    for (int i = 0; i < listOne.size(); i++) {
      if (!listOne.get(i).isExactSameType(listTwo.get(i))) {
        return false;
      }
    }
    return true;
  }
}

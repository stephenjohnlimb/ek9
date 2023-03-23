package org.ek9lang.compiler.symbol.support;

import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;

/**
 * Search through the list of 'type parameters or arguments' held in the 'generic type'
 * and find the index position of the type that matches. If there is a the same type it will always return the
 * first. If there is no match then -1 will be returned. This is looking for an exact type match.
 */
public class IndexOfType implements BiFunction<PossibleGenericSymbol, ISymbol, Integer> {
  @Override
  public Integer apply(PossibleGenericSymbol genericTypeSymbol, ISymbol theType) {

    for (int i = 0; i < genericTypeSymbol.getTypeParameterOrArguments().size(); i++) {
      if (genericTypeSymbol.getTypeParameterOrArguments().get(i).isExactSameType(theType)) {
        return i;
      }
    }
    return -1;
  }
}

package org.ek9lang.compiler.support;

import java.util.List;
import java.util.function.BiPredicate;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Checks to see if the type parameters are the same as the generic arguments.
 * i.e. are they all still conceptual and constrained in the same way.
 * If this is the case - then we are not really parameterising anything.
 */
class SameGenericConceptualParameters implements BiPredicate<ISymbol, List<ISymbol>> {
  @Override
  public boolean test(final ISymbol typeSymbol, final List<ISymbol> typeArguments) {
    if (typeSymbol instanceof PossibleGenericSymbol genericTypeSymbol) {

      final var genericParameters = genericTypeSymbol.getTypeParameterOrArguments();

      return isMatch(genericParameters, typeArguments);
    }
    return false;
  }

  private boolean isMatch(final List<ISymbol> genericParameters, final List<ISymbol> parameterizingArguments) {
    if (genericParameters.size() == parameterizingArguments.size()) {
      for (int i = 0; i < genericParameters.size(); i++) {
        final var param = genericParameters.get(i);
        final var arg = parameterizingArguments.get(i);
        if (!isMatch(param, arg)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private boolean isMatch(final ISymbol param, ISymbol arg) {
    //Check if really is the same object instance first.
    if (param == arg) {
      return true;
    }
    final var paramParentGeneric = param.getSquirrelledData(CommonValues.GENERIC_PARENT);
    final var argumentParentGeneric = arg.getSquirrelledData(CommonValues.GENERIC_PARENT);

    return (param.isConceptualTypeParameter()
        && arg.isConceptualTypeParameter()
        && paramParentGeneric != null
        && paramParentGeneric.equals(argumentParentGeneric));

  }
}

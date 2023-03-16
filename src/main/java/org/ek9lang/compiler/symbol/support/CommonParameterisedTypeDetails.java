package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.core.utils.Digest;

/**
 * Yes it's a long name.
 * The idea is to type and get the common stuff out of
 * ParameterisedTypeSymbol and ParameterisedFunctionSymbol.
 * Also, general for of Symbol type stuff, it's more just a name space really.
 */
public class CommonParameterisedTypeDetails {

  private CommonParameterisedTypeDetails() {
    //Just to stop instantiation.
  }

  /**
   * Could be either a ParameterisedTypeSymbol or a ParameterisedFunctionSymbol
   * Builds a unique name based on what it has been parameterised with.
   */
  public static String getInternalNameFor(ISymbol parameterisableSymbol,
                                          List<ISymbol> parameterSymbols) {
    var toDigest = parameterisableSymbol.getFullyQualifiedName() + parameterSymbols
        .stream()
        .map(ISymbol::getFullyQualifiedName)
        .collect(Collectors.joining("_"));

    return "_" + parameterisableSymbol.getName() + "_" + Digest.digest(toDigest);
  }

  /**
   * Checks if the types of symbols in the two lists match or not.
   */
  public static boolean doSymbolsMatch(List<ISymbol> list1, List<ISymbol> list2) {
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

  /**
   * Converts a list to a comma separated list, with optional parenthesis.
   * But you have to take care here as a Type is also its own type.
   * So watch recursion.
   */
  public static String asCommaSeparated(List<ISymbol> params, boolean includeParenthesis) {
    List<String> names = new ArrayList<>();

    for (var param : params) {
      var paramType = param.getType();
      if (paramType.isPresent() && !param.getFullyQualifiedName()
          .equals(paramType.get().getFullyQualifiedName())) {
        names.add(param.getFriendlyName());
      } else {
        names.add(param.getName());
      }
    }

    var commaSeparated = String.join(", ", names);
    if (!includeParenthesis) {
      return commaSeparated;
    }
    return "(" + commaSeparated + ")";
  }

  /**
   * For the type passed in - a T or and S whatever we need to know its index.
   * From this we can look at what this has been parameterised with and use that type.
   * If not found returns -1.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static int getIndexOfType(PossibleGenericSymbol genericTypeSymbol, Optional<ISymbol> theType) {
    if (theType.isPresent()) {
      for (int i = 0; i < genericTypeSymbol.getParameterTypes().size(); i++) {
        if (genericTypeSymbol.getParameterTypes().get(i).isExactSameType(theType.get())) {
          return i;
        }
      }
    }
    return -1;
  }
}

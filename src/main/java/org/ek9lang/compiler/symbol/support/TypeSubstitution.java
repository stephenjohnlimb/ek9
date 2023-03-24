package org.ek9lang.compiler.symbol.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.core.exception.AssertValue;

/**
 * Accepts an aggregate or a function that has been parameterized with 'type arguments'.
 * It goes through the 'generic type' with its 'type parameters' and the 'type arguments'
 * supplied and searches for the conceptual types ('T' etc) in what has been defined in the
 * 'generic type' and clones the method or parameter (in case of a function) and the associated
 * returns if there are any. It then replaces the 'T' with the appropriate 'type argument'.
 * This does mutate the 'possibleGenericSymbol' passed in.
 */
public class TypeSubstitution implements UnaryOperator<PossibleGenericSymbol> {
  @Override
  //SonarQube does not seem to see the use of isPresent here!
  @SuppressWarnings("java:S3655")
  public PossibleGenericSymbol apply(PossibleGenericSymbol possibleGenericSymbol) {
    AssertValue.checkNotNull("possibleGenericSymbol cannot be null",
        possibleGenericSymbol);
    var theGenericType = possibleGenericSymbol.getGenericType();
    AssertValue.checkTrue("possibleGenericSymbol does not have a generic type",
        theGenericType.isPresent());
    AssertValue.checkFalse("possibleGenericSymbol does not have any type arguments",
        possibleGenericSymbol.getTypeParameterOrArguments().isEmpty());

    //This will provide all params in the case of a FunctionSymbol and all Methods and Fields in the case of Aggregates
    var toClone = theGenericType.get().getSymbolsForThisScope();
    var cloned = cloneInto(toClone, possibleGenericSymbol);
    var typeMapping = getConceptualLookupMapping(possibleGenericSymbol.getTypeParameterOrArguments(),
        theGenericType.get().getAnyConceptualTypeParameters());
    replaceTypeParametersWithTypeArguments(possibleGenericSymbol, typeMapping, cloned);
    return possibleGenericSymbol;
  }

  /**
   * Now we have a mapping of the 'type parameter' to the 'type argument', we can cycle through
   * all the symbols and replace the types are they are encountered on the cloned symbols.
   */
  private void replaceTypeParametersWithTypeArguments(final PossibleGenericSymbol possibleGenericSymbol, final Map<ISymbol, ISymbol> typeMapping, List<ISymbol> symbols) {
    symbols.forEach(symbol -> {
      if (symbol instanceof MethodSymbol methodSymbol) {
        if(methodSymbol.isConstructor()) {
          //Alter the name of the Method to match the name of the scope (i.e. class)
          //This is similar to Java where the name of the aggregate indicates a constructor.
          methodSymbol.setName(possibleGenericSymbol.getName());
          //Also need to alter the return type as a constructor.
          methodSymbol.setType(possibleGenericSymbol);
        }
        methodSymbol.getMethodParameters().forEach(param -> substituteAsAppropriate(typeMapping, param));
      }
      //This will do any return type on the method and just normal values (for a function).
      substituteAsAppropriate(typeMapping, symbol);
    });
  }

  private void substituteAsAppropriate(final Map<ISymbol, ISymbol> typeMapping, ISymbol value) {
    var paramType = value.getType();
    if (paramType.isPresent() && typeMapping.containsKey(paramType.get())) {
      var toMapTo = typeMapping.get(paramType.get());
      value.setType(toMapTo);
    }
  }

  /**
   * You may think why this mapping.
   * It's because some 'type parameters' in the generic type may be 'concrete' and not 'conceptual'.
   * i.e. 'type arguments' = [Integer, String, Duration] but
   * the  'type parameters' = [Float, X, Y, Boolean, Z] - this might be the can in a generic type that also has
   * a generic type and has been parameterized with a mix of conceptual and concrete arguments.
   */
  private Map<ISymbol, ISymbol> getConceptualLookupMapping(final List<ISymbol> typeArguments,
                                                           final List<ISymbol> typeParameters) {
    Map<ISymbol, ISymbol> rtn = new HashMap<>();
    int index = 0;
    for (ISymbol typeParameter : typeParameters) {
      var typeArgument = typeArguments.get(index);
      if (typeParameter.isConceptualTypeParameter()) {
        rtn.put(typeParameter, typeArgument);
        index++;
      }
    }
    AssertValue.checkTrue("Number of conceptual types to supplied types must match",
        index == typeArguments.size());
    return rtn;
  }

  private List<ISymbol> cloneInto(final List<ISymbol> symbols, IScope enclosingScope) {
    return symbols.stream().map(symbol -> {
      var clonedSymbol = symbol.clone(enclosingScope);
      enclosingScope.define(clonedSymbol);
      return clonedSymbol;
    }).toList();
  }
}

package org.ek9lang.compiler.phase4;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CONSTRAINED_TYPE_CONSTRUCTOR_MISSING;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CONSTRUCTOR_USED_ON_ABSTRACT_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_USED_IN_GENERIC;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_RESOLVED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.OPERATOR_NOT_DEFINED;
import static org.ek9lang.compiler.support.SymbolFactory.ACCESSED;

import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Does the detailed for of checking a specific possible generic symbol.
 */
final class ParameterisedTypeChecker implements Consumer<PossibleGenericSymbol> {
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();
  private final ErrorListener errorListener;

  ParameterisedTypeChecker(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final PossibleGenericSymbol possibleGenericSymbol) {

    possibleGenericSymbol.getGenericType().ifPresent(genericType -> {

      var parameterizingArguments = possibleGenericSymbol.getTypeParameterOrArguments();
      var genericTypeParameters = genericType.getTypeParameterOrArguments();

      //Now tie up the generic T and its corresponding argument type.
      for (int i = 0; i < genericTypeParameters.size(); i++) {
        var typeParameterT = genericTypeParameters.get(i);
        var typeArgumentForT = parameterizingArguments.get(i);
        if (typeParameterT instanceof IAggregateSymbol aggregateT) {
          checkParameterAndArgument(new TypeTuple(possibleGenericSymbol, genericType, typeArgumentForT), aggregateT);
        }
      }
    });

  }

  /**
   * For a given T conceptual generic parameter and a given type argument, check that if the T is
   * constrained that the arg is compatible with that type.
   * If it is not a constrained type then check which methods have been accessed and ensure they exist
   * and are callable on the argType provided.
   */
  private void checkParameterAndArgument(final TypeTuple typeTuple,
                                         final IAggregateSymbol aggregateT) {

    aggregateT.getSuperAggregate().ifPresentOrElse(
        constrainedTType -> compatibleTypeOrError(typeTuple, constrainedTType),
        () -> accessedMethodsExistOrError(typeTuple, aggregateT));

  }

  private void accessedMethodsExistOrError(final TypeTuple typeTuple,
                                           final IAggregateSymbol aggregateT) {

    //Here we just check that if a method was accessed in the generic code we check that the corresponding method
    //exists on the type being used as a parameter.
    aggregateT.getAllMethods().stream()
        .filter(method -> "TRUE".equals(method.getSquirrelledData(ACCESSED)))
        .forEach(method -> methodAvailableOrError(typeTuple, aggregateT, method));
  }

  private void methodAvailableOrError(final TypeTuple typeTuple,
                                      final IAggregateSymbol aggregateT,
                                      final MethodSymbol method) {

    if (typeTuple.typeArgumentForT instanceof IAggregateSymbol aggregateArgType) {

      final var methodSearch = createSearch(method, aggregateT, typeTuple.typeArgumentForT);
      var resolved = aggregateArgType.resolveInThisScopeOnly(methodSearch);
      checkMethodResolutionOrError(typeTuple, methodSearch, resolved, method);

    } else {
      checkFunctionUseOrError(typeTuple, method);
    }
  }

  private void checkFunctionUseOrError(final TypeTuple typeTuple, final MethodSymbol method) {

    var location = locationExtractorFromSymbol.apply(method);
    var msg = "'" + method.getFriendlyName() + "' " + location + ":";
    if (method.isConstructor()) {
      errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg,
          CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC);
    } else if (!"?".equals(method.getName())) {
      errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg, FUNCTION_USED_IN_GENERIC);
    }

  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private void checkMethodResolutionOrError(final TypeTuple typeTuple,
                                            final MethodSymbolSearch methodSearch,
                                            final Optional<ISymbol> resolved,
                                            final MethodSymbol method) {

    resolved.ifPresentOrElse(
        resolvedSymbol -> {
          if (resolvedSymbol instanceof MethodSymbol resolvedMethodSymbol
              && resolvedMethodSymbol.isConstructor()
              && typeTuple.typeArgumentForT.isMarkedAbstract()) {

            var location = locationExtractorFromSymbol.apply(resolvedSymbol);
            var msg = "'" + resolvedSymbol.getFriendlyName() + "' "
                + location + " cannot be used with '"
                + typeTuple.typeArgumentForT.getFriendlyName() + "':";

            errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg,
                CONSTRUCTOR_USED_ON_ABSTRACT_TYPE);
          }
        },
        () -> {
          var classification = method.isConstructor() ? NOT_RESOLVED : OPERATOR_NOT_DEFINED;
          var msg = "'" + method.getFriendlyName() + "' " + "is used in '"
              + typeTuple.genericType.getFriendlyName() + "', "
              + "but '" + methodSearch + "' is not defined in '" + typeTuple.typeArgumentForT + "':";

          errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg, classification);
        });

  }

  /**
   * For parameterised types don't use the source location because that is the generic type.
   * For this scenario we need to use the location where this parameterised type was first initialised.
   * That way we get an error on the line where the first use of this parameterization was made.
   * While we do need to check it is assignable, we also must mandate that the constructors on the
   * constraining type are also present on the parameterizing type.
   */
  private void compatibleTypeOrError(final TypeTuple typeTuple,
                                     final ISymbol constrainedTType) {

    if (typeTuple.typeArgumentForT.isExactSameType(constrainedTType)) {
      return;
    }

    if (!typeTuple.typeArgumentForT.isAssignableTo(constrainedTType)) {
      var msg = "wrt '" + typeTuple.genericType.getFriendlyName()
          + "', '" + constrainedTType.getFriendlyName()
          + "' and '" + typeTuple.typeArgumentForT.getFriendlyName() + "':";

      errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg, INCOMPATIBLE_TYPES);
    } else {
      minimalMatchingConstructorsOrError(typeTuple, constrainedTType);
    }

  }

  private void minimalMatchingConstructorsOrError(final TypeTuple typeTuple, final ISymbol constrainedTType) {
    //Only check if an aggregate, not applicable to functions.
    if (constrainedTType instanceof IAggregateSymbol constrainingAggregate
        && typeTuple.typeArgumentForT instanceof IAggregateSymbol typeArgumentAggregate) {
      var searches = constrainingAggregate.getAllNonAbstractMethodsInThisScopeOnly().stream()
          .filter(MethodSymbol::isConstructor)
          .map(method -> new MethodSymbolSearch(typeTuple.typeArgumentForT.getName(), method))
          .toList();

      //Now have a list of searches, need to ensure they exist on the typeTuple.typeArgumentForT
      searches.forEach(search -> {
        var resolved = typeArgumentAggregate.resolveInThisScopeOnly(search);
        if (resolved.isEmpty()) {
          var msg = "wrt constraining type '" + constrainedTType.getFriendlyName()
              + "' and parameterizing type '" + typeTuple.typeArgumentForT.getFriendlyName() + "',"
              + " constructor '" + search + "' is required:";
          errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg,
              CONSTRAINED_TYPE_CONSTRUCTOR_MISSING);
        }
      });
    }
  }

  /**
   * Just a convenience method to create the search for a method.
   */
  private MethodSymbolSearch createSearch(final MethodSymbol method,
                                          final IAggregateSymbol aggregateT,
                                          final ISymbol typeArgumentForT) {

    var methodName = method.isConstructor() ? typeArgumentForT.getName() : method.getName();
    var methodSearch = new MethodSymbolSearch(methodName);

    method.getCallParameters()
        .forEach(param -> param.getType().ifPresent(paramType -> {
          if (paramType.isExactSameType(aggregateT)) {
            methodSearch.addTypeParameter(typeArgumentForT);
          } else {
            methodSearch.addTypeParameter(paramType);
          }
        }));

    return methodSearch;
  }

  /**
   * Just to reduce the number of parameters - once established.
   */
  private record TypeTuple(PossibleGenericSymbol possibleGenericSymbol,
                           PossibleGenericSymbol genericType,
                           ISymbol typeArgumentForT) {

  }
}

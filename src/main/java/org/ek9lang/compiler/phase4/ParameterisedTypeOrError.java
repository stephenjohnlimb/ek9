package org.ek9lang.compiler.phase4;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CONSTRAINED_TYPE_CONSTRUCTOR_MISSING;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CONSTRUCTOR_USED_ON_ABSTRACT_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_USED_IN_GENERIC;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_RESOLVED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.OPERATOR_NOT_DEFINED;
import static org.ek9lang.compiler.support.SymbolFactory.ACCESSED;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Does the detailed for of checking a specific possible generic symbol.
 */
final class ParameterisedTypeOrError implements Consumer<PossibleGenericSymbol> {
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();
  private final Ek9Types ek9Types;
  private final ErrorListener errorListener;

  ParameterisedTypeOrError(final Ek9Types ek9Types, final ErrorListener errorListener) {
    this.ek9Types = ek9Types;
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final PossibleGenericSymbol possibleGenericSymbol) {

    possibleGenericSymbol.getGenericType().ifPresent(genericType -> {

      final var arguments = possibleGenericSymbol.getTypeParameterOrArguments();
      final var parameters = genericType.getTypeParameterOrArguments();
      parametersAndArgumentValidOrError(genericType, parameters, arguments, possibleGenericSymbol);

    });

  }

  private void parametersAndArgumentValidOrError(final PossibleGenericSymbol genericType,
                                                 final java.util.List<ISymbol> genericTypeParameters,
                                                 final java.util.List<ISymbol> parameterizingArguments,
                                                 final PossibleGenericSymbol possibleGenericSymbol) {

    //Now tie up the generic T and its corresponding argument type.
    for (int i = 0; i < genericTypeParameters.size(); i++) {
      final var argument = parameterizingArguments.get(i);
      final var parameter = genericTypeParameters.get(i);
      if (parameter instanceof IAggregateSymbol aggregateT) {
        final var tuple = new TypeTuple(possibleGenericSymbol, genericType, argument);
        parameterAndArgumentValidOrError(tuple, aggregateT);
      }
    }
  }

  /**
   * For a given T conceptual generic parameter and a given type argument, check that if the T is
   * constrained that the arg is compatible with that type.
   * If it is not a constrained type then check which methods have been accessed and ensure they exist
   * and are callable on the argType provided.
   */
  private void parameterAndArgumentValidOrError(final TypeTuple typeTuple,
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

      //Do not check these built in types, when we generate the output code we will
      //add an 'is-set' code block to these and always return a Boolean that is 'un-set'.

      if (ek9Types.ek9AnyClass().isExactSameType(aggregateArgType)
          || ek9Types.ek9AnyRecord().isExactSameType(aggregateArgType)) {
        return;
      }

      final var methodSearch = createSearch(method, aggregateT, typeTuple.typeArgumentForT);
      final var resolved = aggregateArgType.resolveInThisScopeOnly(methodSearch);

      resolved.ifPresentOrElse(
          resolvedSymbol -> constructorArgumentNotAbstractOrError(typeTuple, resolvedSymbol),
          () -> emitMissingMethodOrOperator(typeTuple, methodSearch, method)
      );

    } else {
      functionUseValidOrError(typeTuple, method);
    }
  }

  private void functionUseValidOrError(final TypeTuple typeTuple, final MethodSymbol method) {

    final var location = locationExtractorFromSymbol.apply(method);
    final var msg = "'" + method.getFriendlyName() + "' " + location + ":";
    if (method.isConstructor()) {
      errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg,
          CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC);
    } else if (!"?".equals(method.getName())) {
      errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg, FUNCTION_USED_IN_GENERIC);
    }

  }

  private void constructorArgumentNotAbstractOrError(final TypeTuple typeTuple,
                                                     final ISymbol resolvedSymbol) {

    if (resolvedSymbol instanceof MethodSymbol resolvedMethodSymbol
        && resolvedMethodSymbol.isConstructor()
        && typeTuple.typeArgumentForT.isMarkedAbstract()) {

      emitConstructorUsedWithAbstractTypeError(typeTuple, resolvedSymbol);

    }
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
      emitIncompatibleTypesError(typeTuple, constrainedTType);
    } else {
      minimalMatchingConstructorsOrError(typeTuple, constrainedTType);
    }

  }

  private void minimalMatchingConstructorsOrError(final TypeTuple typeTuple, final ISymbol constrainedTType) {

    //Only check if an aggregate, not applicable to functions.
    if (constrainedTType instanceof IAggregateSymbol constrainingAggregate
        && typeTuple.typeArgumentForT instanceof IAggregateSymbol typeArgumentAggregate) {

      final var searches = constrainingAggregate.getConstructors().stream()
          .map(method -> new MethodSymbolSearch(typeTuple.typeArgumentForT.getName(), method))
          .toList();

      //Now have a list of searches, need to ensure they exist on the typeTuple.typeArgumentForT
      searches.forEach(search -> {
        if (typeArgumentAggregate.resolveInThisScopeOnly(search).isEmpty()) {
          emitConstrainedTypeConstructorMissingError(typeTuple, search, constrainingAggregate);
        }
      });

    }
  }

  private void emitMissingMethodOrOperator(final TypeTuple typeTuple,
                                           final MethodSymbolSearch methodSearch,
                                           final MethodSymbol method) {

    final var classification = method.isConstructor() ? NOT_RESOLVED : OPERATOR_NOT_DEFINED;
    final var msg = "'" + method.getFriendlyName() + "' " + "is used in '"
        + typeTuple.genericType.getFriendlyName() + "', "
        + "but '" + methodSearch + "' is not defined in '" + typeTuple.typeArgumentForT + "':";

    errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg, classification);

  }

  private void emitConstructorUsedWithAbstractTypeError(final TypeTuple typeTuple,
                                                        final ISymbol resolvedSymbol) {

    final var location = locationExtractorFromSymbol.apply(resolvedSymbol);
    final var msg = "'" + resolvedSymbol.getFriendlyName() + "' "
        + location + " cannot be used with '"
        + typeTuple.typeArgumentForT.getFriendlyName() + "':";

    errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg,
        CONSTRUCTOR_USED_ON_ABSTRACT_TYPE);

  }

  private void emitIncompatibleTypesError(final TypeTuple typeTuple,
                                          final ISymbol constrainedTType) {

    final var msg = "wrt '" + typeTuple.genericType.getFriendlyName()
        + "', '" + constrainedTType.getFriendlyName()
        + "' and '" + typeTuple.typeArgumentForT.getFriendlyName() + "':";
    errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg, INCOMPATIBLE_TYPES);

  }

  private void emitConstrainedTypeConstructorMissingError(final TypeTuple typeTuple,
                                                          final MethodSymbolSearch search,
                                                          final IAggregateSymbol constrainedTType) {

    final var msg = "wrt constraining type '" + constrainedTType.getFriendlyName()
        + "' and parameterizing type '" + typeTuple.typeArgumentForT.getFriendlyName() + "',"
        + " constructor '" + search + "' is required:";
    errorListener.semanticError(typeTuple.possibleGenericSymbol.getInitialisedBy(), msg,
        CONSTRAINED_TYPE_CONSTRUCTOR_MISSING);

  }

  /**
   * Just a convenience method to create the search for a method.
   */
  private MethodSymbolSearch createSearch(final MethodSymbol method,
                                          final IAggregateSymbol aggregateT,
                                          final ISymbol typeArgumentForT) {

    final var methodName = method.isConstructor() ? typeArgumentForT.getName() : method.getName();
    final var methodSearch = new MethodSymbolSearch(methodName);

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

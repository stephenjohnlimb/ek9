package org.ek9lang.compiler.main.resolvedefine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ParameterisedFunctionSymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.ScopedSymbol;
import org.ek9lang.compiler.symbol.support.ParameterizedFunctionCreator;
import org.ek9lang.compiler.symbol.support.ParameterizedTypeCreator;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;

/**
 * Used to see if it is possible to resolve types, both simple and parametric.
 * There's a bit of recursion in here, so take care.
 */
public class GeneralTypeResolver implements Function<SymbolSearchConfiguration, Optional<ISymbol>> {

  private final IScope scopeForResolution;

  public GeneralTypeResolver(final IScope scopeForResolution) {
    this.scopeForResolution = scopeForResolution;
  }

  private final ParameterizedTypeCreator parameterizedTypeCreator = new ParameterizedTypeCreator();

  private final ParameterizedFunctionCreator parameterizedFunctionCreator = new ParameterizedFunctionCreator();

  @Override
  public Optional<ISymbol> apply(final SymbolSearchConfiguration toResolve) {
    var mainSymbol = scopeForResolution.resolve(new AnySymbolSearch(toResolve.mainSymbolName()));

    //For non-parametric stuff that's it.
    if (!toResolve.isParametric()) {
      return mainSymbol;
    }

    List<ISymbol> parameterizingTypeSymbols = getParameterizingSymbols(toResolve.parameterizingArguments());

    //Check it has been found and actually is generic in nature.
    if (mainSymbol.isPresent()
        && mainSymbol.get().isGenericInNature()
        && parameterizingTypeSymbols.size() == toResolve.parameterizingArguments().size()) {

      if (mainSymbol.get() instanceof AggregateSymbol genericAggregateType) {
        var theType = createSymbolTypeToFind(genericAggregateType, parameterizingTypeSymbols);
        if (theType != null) {
          return scopeForResolution.resolve(new TypeSymbolSearch(theType.getFullyQualifiedName()));
        }
      } else if (mainSymbol.get() instanceof FunctionSymbol genericFunction) {
        var theFunction = createSymbolTypeToFind(genericFunction, parameterizingTypeSymbols);
        if (theFunction != null) {
          return scopeForResolution.resolve(new FunctionSymbolSearch(theFunction.getFullyQualifiedName()));
        }
      }
    }

    return Optional.empty();
  }

  private List<ISymbol> getParameterizingSymbols(final List<SymbolSearchConfiguration> parameterizingTypes) {
    List<ISymbol> parameterizingTypeSymbols = new ArrayList<>();
    for (var paramType : parameterizingTypes) {
      //Recursive call to this as SymbolSearchForTest can be nested
      var parameterizingType = this.apply(paramType);
      parameterizingType.ifPresent(parameterizingTypeSymbols::add);
    }
    return parameterizingTypeSymbols;
  }

  private ParameterisedTypeSymbol createSymbolTypeToFind(final AggregateSymbol genericAggregateType,
                                                         final List<ISymbol> parameterizingTypeSymbols) {
    checkParameterCount(genericAggregateType, parameterizingTypeSymbols);
    return parameterizedTypeCreator.apply(genericAggregateType, parameterizingTypeSymbols);
  }

  private ParameterisedFunctionSymbol createSymbolTypeToFind(final FunctionSymbol genericFunction,
                                                             final List<ISymbol> parameterizingTypeSymbols) {
    checkParameterCount(genericFunction, parameterizingTypeSymbols);
    return parameterizedFunctionCreator.apply(genericFunction, parameterizingTypeSymbols);
  }

  private void checkParameterCount(final ScopedSymbol scopedSymbol, final List<ISymbol> parameterizingTypeSymbols) {
    var acceptsNParameters = scopedSymbol.getAnyGenericParameters().size();
    var providedWithNParameters = parameterizingTypeSymbols.size();
    if (acceptsNParameters != providedWithNParameters) {
      var msg = "'"
          + providedWithNParameters + "' parameter(s) were supplied but '"
          + scopedSymbol.getFriendlyName() + "' requires '"
          + acceptsNParameters + "' parameter(s):";
      throw new IllegalArgumentException(msg);
    }
  }
}

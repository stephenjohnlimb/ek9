package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ModuleScope;
import org.ek9lang.compiler.symbol.ParameterisedFunctionSymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;

/**
 * Just used in a testing context to see if it is possible to resolve types, both simple and parametric.
 * There's a bit of recursion in here, so take care.
 */
public class ResolverForTesting implements Function<SymbolSearchForTest, Optional<ISymbol>> {

  private final ModuleScope scopeForResolution;

  public ResolverForTesting(final ModuleScope scopeForResolution) {
    this.scopeForResolution = scopeForResolution;
  }

  @Override
  public Optional<ISymbol> apply(final SymbolSearchForTest toResolve) {
    var mainSymbol = scopeForResolution.resolve(new AnySymbolSearch(toResolve.mainSymbolName()));

    //For non-parametric stuff that's it.
    if(!toResolve.isParametric()) {
      return mainSymbol;
    }

    List<ISymbol> parameterizingTypeSymbols = getParameterizingSymbols(toResolve.parameterizingArguments());

    //Check it has been found and actually is generic in nature.
    if (mainSymbol.isPresent()
        && mainSymbol.get().isGenericInNature()
        && parameterizingTypeSymbols.size() == toResolve.parameterizingArguments().size()) {

      if (mainSymbol.get() instanceof AggregateSymbol genericAggregateType) {
        var theType = createParameterisedTypeSymbolToFind(genericAggregateType, parameterizingTypeSymbols);
        //Now lets see if it resolves
        //So this will be an actual type now.
        return scopeForResolution.resolve(new TypeSymbolSearch(theType.getFullyQualifiedName()));
      } else if (mainSymbol.get() instanceof FunctionSymbol genericFunction) {
        var theFunction = createParameterisedFunctionSymbolToFind(genericFunction, parameterizingTypeSymbols);
        //Again check it resolves and it will be an actual FUNCTION now as it is a new actual function not a template
        return scopeForResolution.resolve(new FunctionSymbolSearch(theFunction.getFullyQualifiedName()));
      }
    }

    return Optional.empty();
  }

  private List<ISymbol> getParameterizingSymbols(final List<SymbolSearchForTest> parameterizingTypes) {
    List<ISymbol> parameterizingTypeSymbols = new ArrayList<>();
    for (var paramType : parameterizingTypes) {
      //Recursive call to this as SymbolSearchForTest can be nested
      var parameterizingType = this.apply(paramType);
      parameterizingType.ifPresent(parameterizingTypeSymbols::add);
    }
    return parameterizingTypeSymbols;
  }

  private ParameterisedTypeSymbol createParameterisedTypeSymbolToFind(final AggregateSymbol genericAggregateType,
                                                                      final List<ISymbol> parameterizingTypeSymbols) {
    var scope = genericAggregateType.getModuleScope();
    var theType = new ParameterisedTypeSymbol(genericAggregateType, parameterizingTypeSymbols, scope);
    theType.setModuleScope(genericAggregateType.getModuleScope());
    theType.setParsedModule(genericAggregateType.getParsedModule());
    theType.setSourceToken(genericAggregateType.getSourceToken());
    return theType;
  }

  private ParameterisedFunctionSymbol createParameterisedFunctionSymbolToFind(final FunctionSymbol genericFunction,
                                                                              final List<ISymbol> parameterizingTypeSymbols) {
    var scope = genericFunction.getModuleScope();
    var theType = new ParameterisedFunctionSymbol(genericFunction, parameterizingTypeSymbols, scope);
    theType.setModuleScope(genericFunction.getModuleScope());
    theType.setParsedModule(genericFunction.getParsedModule());
    theType.setSourceToken(genericFunction.getSourceToken());
    return theType;
  }
}

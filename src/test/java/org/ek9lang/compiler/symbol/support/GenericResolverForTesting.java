package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ParameterisedFunctionSymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;
import org.ek9lang.compiler.symbol.support.search.FunctionSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;

/**
 * Just used in a testing context to see if it is possible to resolve a parameterised type/function.
 * i.e. resolve List, then resolve String - then see if it is possible resolve 'List of String' as a type.
 */
public class GenericResolverForTesting implements BiFunction<String, List<String>, Optional<ISymbol>> {

  private final IScope scopeForResolution;

  public GenericResolverForTesting(final IScope scopeForResolution) {
    this.scopeForResolution = scopeForResolution;
  }

  @Override
  public Optional<ISymbol> apply(String genericName, List<String> parameterizingTypeNames) {
    var genericSymbol = scopeForResolution.resolve(new AnySymbolSearch(genericName));
    List<ISymbol> parameterizingTypeSymbols = getParameterizingSymbols(parameterizingTypeNames);

    //Check it has been found and actually is generic in nature.
    if (genericSymbol.isPresent()
        && genericSymbol.get().isGenericInNature()
        && parameterizingTypeSymbols.size() == parameterizingTypeNames.size()) {

      if (genericSymbol.get() instanceof AggregateSymbol genericAggregateType) {
        var theType = createParameterisedTypeSymbolToFind(genericAggregateType, parameterizingTypeSymbols);
        //Now lets see if it resolves
        //So this will be an actual type now.
        return scopeForResolution.resolve(new TypeSymbolSearch(theType.getFullyQualifiedName()));
      } else if (genericSymbol.get() instanceof FunctionSymbol genericFunction) {
        var theFunction = createParameterisedFunctionSymbolToFind(genericFunction, parameterizingTypeSymbols);
        //Again check it resolves and it will be an actual FUNCTION now as it is a new actual function not a template
        return scopeForResolution.resolve(new FunctionSymbolSearch(theFunction.getFullyQualifiedName()));
      }
    }

    return Optional.empty();
  }

  private List<ISymbol> getParameterizingSymbols(final List<String> parameterizingTypeNames) {
    List<ISymbol> parameterizingTypeSymbols = new ArrayList<>();
    for (var paramTypeName : parameterizingTypeNames) {
      var parameterizingType = scopeForResolution.resolve(new AnySymbolSearch(paramTypeName));
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

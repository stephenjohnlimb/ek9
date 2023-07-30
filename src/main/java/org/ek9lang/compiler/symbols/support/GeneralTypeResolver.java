package org.ek9lang.compiler.symbols.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.symbols.search.SymbolSearch;

/**
 * Used to see if it is possible to resolve types, both simple and parametric.
 * There's a bit of recursion in here, so take care.
 */
public class GeneralTypeResolver implements Function<SymbolSearchConfiguration, Optional<ISymbol>> {

  private final IScope scopeForResolution;
  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator();

  public GeneralTypeResolver(final IScope scopeForResolution) {
    this.scopeForResolution = scopeForResolution;
  }

  @Override
  public Optional<ISymbol> apply(final SymbolSearchConfiguration toResolve) {

    var mainSymbol = scopeForResolution.resolve(new AnyTypeSymbolSearch(toResolve.mainSymbolName()));

    //For non-parametric stuff that's it.
    if (!toResolve.isParametric()) {
      return mainSymbol;
    }

    List<ISymbol> parameterizingTypeSymbols = getParameterizingSymbols(toResolve.parameterizingArguments());

    //Check it has been found and actually is generic in nature.
    if (mainSymbol.isPresent()
        && mainSymbol.get().isGenericInNature()
        && parameterizingTypeSymbols.size() == toResolve.parameterizingArguments().size()) {
      var genericType = mainSymbol.get();

      if (genericType instanceof PossibleGenericSymbol genericSymbol) {
        var theType = creator.apply(genericSymbol, parameterizingTypeSymbols);
        return scopeForResolution.resolve(new SymbolSearch(theType));
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
}

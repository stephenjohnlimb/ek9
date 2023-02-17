package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;

/**
 * Just used in a testing context to see if it is possible to resolve a paramterised type.
 * i.e. resolve List, then resolve String - then see if it is possible resolve 'List of String' as a type.
 */
public class GenericTypeResolverForTesting implements BiFunction<String, List<String>, Optional<ISymbol>> {

  private final IScope scopeForResolution;

  public GenericTypeResolverForTesting(final IScope scopeForResolution) {
    this.scopeForResolution = scopeForResolution;
  }

  @Override
  public Optional<ISymbol> apply(String genericTypeName, List<String> parameterizingTypeNames) {
    var genericType = scopeForResolution.resolve(new AnySymbolSearch(genericTypeName));

    if (genericType.isPresent()
        && genericType.get() instanceof AggregateSymbol genericAggregateType) {
      List<ISymbol> parameterizingTypeSymbols = new ArrayList<>();
      for (var paramTypeName : parameterizingTypeNames) {
        var parameterizingType = scopeForResolution.resolve(new AnySymbolSearch(paramTypeName));
        parameterizingType.ifPresent(parameterizingTypeSymbols::add);
      }

      if (parameterizingTypeSymbols.size() == parameterizingTypeNames.size()) {
        var scope = genericAggregateType.getModuleScope();
        var theType = new ParameterisedTypeSymbol(genericAggregateType, parameterizingTypeSymbols, scope);
        theType.setModuleScope(genericAggregateType.getModuleScope());
        theType.setParsedModule(genericAggregateType.getParsedModule());
        theType.setSourceToken(genericAggregateType.getSourceToken());

        //Now lets see if it resolves
        //So this will be an actual type now.
        return scopeForResolution.resolve(new TypeSymbolSearch(theType.getFullyQualifiedName()));
      }
    }
    return Optional.empty();
  }
}

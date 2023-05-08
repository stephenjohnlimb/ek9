package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.compiler.symbol.support.search.TemplateTypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Used to create new parameterised generic type with a specific type argument, or resolve existing ones.
 */
public class NewParameterizedSymbolOfType implements Function<ISymbol, Optional<ISymbol>> {

  private final String ofGenericType;
  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final SymbolFactory symbolFactory;

  /**
   * Create a new Function that can define or resolve a specific generic type with a single type parameter.
   */
  public NewParameterizedSymbolOfType(final String ofGenericType,
                                      final SymbolAndScopeManagement symbolAndScopeManagement,
                                      final SymbolFactory symbolFactory) {
    this.ofGenericType = ofGenericType;
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.symbolFactory = symbolFactory;
  }

  @Override
  public Optional<ISymbol> apply(final ISymbol theType) {
    var genericType = symbolAndScopeManagement.getTopScope().resolve(new TemplateTypeSymbolSearch(ofGenericType));
    AssertValue.checkTrue("Must be able resolve " + ofGenericType, genericType.isPresent());
    if (genericType.get() instanceof PossibleGenericSymbol genericSymbol) {
      var theNewParameterisedType = symbolFactory.newParameterisedSymbol(genericSymbol, List.of(theType));
      return symbolAndScopeManagement.resolveOrDefine(theNewParameterisedType);
    }
    AssertValue.fail("Must be a PossibleGenericSymbol");
    return Optional.empty();
  }
}

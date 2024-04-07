package org.ek9lang.compiler.search;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Quite a few option to a method search.
 * In some cases you need to be exact but in others you
 * want the return type left open.
 * So there are multiple constructors to support this.
 */
public final class MethodSymbolSearch extends SymbolSearch {

  public MethodSymbolSearch(final SymbolSearch from) {

    this(from.getName(), from);
  }

  /**
   * Create a method search from an existing search, but with a new name.
   */
  public MethodSymbolSearch(final String newName, final SymbolSearch from) {

    this(newName);
    setTypeParameters(from.getTypeParameters());
    if (from.getOfTypeOrReturn().isPresent()) {
      this.setOfTypeOrReturn(from.getOfTypeOrReturn());
    }

  }

  /**
   * Create a method search given a method symbol.
   * But this does not set the expected type.
   * This is very useful for finding overridden methods where the returning type is different
   * but compatible.
   */
  public MethodSymbolSearch(final MethodSymbol methodSymbol) {

    this(methodSymbol.getName(), methodSymbol);

  }

  /**
   * Create a method search given a method symbol, but used a new method name.§
   * But this does not set the expected type.
   * This is very useful for finding overridden methods where the returning type is different
   * but compatible.
   */
  public MethodSymbolSearch(final String newName, final MethodSymbol methodSymbol) {

    this(newName);
    final var theTypes = new SymbolTypeExtractor().apply(methodSymbol.getSymbolsForThisScope());
    this.setTypeParameters(theTypes);

    //don't set the return type leave that open, so we can handle covariance.
  }

  public MethodSymbolSearch(final String name) {

    super(name);
    setSearchType(ISymbol.SymbolCategory.METHOD);

  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbolSearch(final String name, final Optional<ISymbol> ofTypeOrReturn) {

    super(name, ofTypeOrReturn);
    setSearchType(ISymbol.SymbolCategory.METHOD);

  }

  public MethodSymbolSearch(final String name, final ISymbol ofTypeOrReturn) {

    super(name, ofTypeOrReturn);
    setSearchType(ISymbol.SymbolCategory.METHOD);

  }

  @Override
  public MethodSymbolSearch setTypeParameters(final List<ISymbol> typeParameters) {

    super.setTypeParameters(typeParameters);

    return this;
  }

  @Override
  public MethodSymbolSearch addTypeParameter(final Optional<ISymbol> parameter) {

    super.addTypeParameter(parameter);

    return this;
  }

  @Override
  public MethodSymbolSearch addTypeParameter(final ISymbol parameter) {

    super.addTypeParameter(parameter);

    return this;
  }

  @Override
  public MethodSymbolSearch setOfTypeOrReturn(final Optional<ISymbol> ofTypeOrReturn) {

    super.setOfTypeOrReturn(ofTypeOrReturn);

    return this;
  }

  @Override
  public MethodSymbolSearch setOfTypeOrReturn(final ISymbol ofTypeOrReturn) {

    super.setOfTypeOrReturn(ofTypeOrReturn);

    return this;
  }
}
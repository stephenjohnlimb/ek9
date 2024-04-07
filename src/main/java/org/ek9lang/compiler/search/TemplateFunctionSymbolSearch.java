package org.ek9lang.compiler.search;

import org.ek9lang.compiler.symbols.ISymbol;

/**
 * A Search for a TEMPLATE of a FUNCTION.
 * But this would not search for hydrated templated types like
 * 'SomeFunction is abstractFunction of Integer'
 * That would have been defined as an actual FUNCTION now (albeit a parameterised one).
 * So you would need to look that up as a FUNCTION.
 */
public final class TemplateFunctionSymbolSearch extends SymbolSearch {
  public TemplateFunctionSymbolSearch(final String name) {

    super(name);
    setSearchType(ISymbol.SymbolCategory.TEMPLATE_FUNCTION);

  }

  public TemplateFunctionSymbolSearch(final TemplateFunctionSymbolSearch from) {

    this(from.getName());

  }
}

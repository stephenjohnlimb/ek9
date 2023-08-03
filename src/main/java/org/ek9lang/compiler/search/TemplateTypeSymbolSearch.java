package org.ek9lang.compiler.search;

import org.ek9lang.compiler.symbols.ISymbol;

/**
 * A Search for a TEMPLATE of a TYPE like a CLASS.
 * But this would not search for hydrated templated types like 'List of Integer'
 * That would have been defined as an actual TYPE now (albeit a parameterised one).
 * So you would need to look that up as a TYPE.
 */
public final class TemplateTypeSymbolSearch extends SymbolSearch {
  public TemplateTypeSymbolSearch(String name) {
    super(name);
    setSearchType(ISymbol.SymbolCategory.TEMPLATE_TYPE);
  }
}

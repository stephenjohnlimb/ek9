package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.ISymbol;

public class TemplateTypeSymbolSearch extends SymbolSearch
{
	public TemplateTypeSymbolSearch(String name)
	{
		super(name);	
		setSearchType(ISymbol.SymbolCategory.TEMPLATE_TYPE);
	}
}

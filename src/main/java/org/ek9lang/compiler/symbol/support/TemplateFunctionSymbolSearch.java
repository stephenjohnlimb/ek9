package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.ISymbol;

public class TemplateFunctionSymbolSearch extends SymbolSearch
{
	public TemplateFunctionSymbolSearch(String name)
	{
		super(name);	
		setSearchType(ISymbol.SymbolCategory.TEMPLATE_FUNCTION);
	}
}

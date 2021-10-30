package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.ISymbol;

public class FunctionSymbolSearch extends SymbolSearch
{
	public FunctionSymbolSearch(String name)
	{
		super(name);	
		setSearchType(ISymbol.SymbolCategory.FUNCTION);
	}
}

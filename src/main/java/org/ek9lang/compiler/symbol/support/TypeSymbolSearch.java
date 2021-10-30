package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.ISymbol;

public class TypeSymbolSearch extends SymbolSearch
{
	public TypeSymbolSearch(String name)
	{
		super(name);	
		setSearchType(ISymbol.SymbolCategory.TYPE);
	}
}

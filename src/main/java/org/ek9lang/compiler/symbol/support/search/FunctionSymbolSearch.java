package org.ek9lang.compiler.symbol.support.search;

import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Search just for a matching function by name.
 * Not too bad because functions can be scoped in modules
 * but cannot be overloaded. i.e. have multiple method signatures in the same module.
 */
public class FunctionSymbolSearch extends SymbolSearch
{
	public FunctionSymbolSearch(String name)
	{
		super(name);	
		setSearchType(ISymbol.SymbolCategory.FUNCTION);
	}
}

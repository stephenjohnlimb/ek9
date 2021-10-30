package org.ek9lang.compiler.symbol.support;

/**
 * Search for a symbol of any category with the matching name.
 */
public class AnySymbolSearch extends SymbolSearch
{
	public AnySymbolSearch(String name)
	{
		super(name);	
		setSearchType(null);
	}
}

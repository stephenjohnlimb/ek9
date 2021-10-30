package org.ek9lang.compiler.symbol;

import java.util.List;

public interface ParameterisedSymbol extends IScope
{
	default ScopeType getScopeType() { return ScopeType.AGGREGATE; }
	
	List<ISymbol> getParameterSymbols();
	
	ScopedSymbol getParameterisableSymbol();
}

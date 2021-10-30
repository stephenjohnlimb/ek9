package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.Symbol;

import java.util.Optional;

public class MethodSymbolSearch extends SymbolSearch
{
	
	public MethodSymbolSearch(SymbolSearch from)
	{
		this(from.getName(), from);
	}
	
	public MethodSymbolSearch(String newName, SymbolSearch from)
	{
		this(newName);
		setParameters(from.getParameters());
		if(from.getOfTypeOrReturn().isPresent())
			this.setOfTypeOrReturn(from.getOfTypeOrReturn());
	}
	
	public MethodSymbolSearch(MethodSymbol methodSymbol)
	{
		this(methodSymbol.getName());
		this.setParameters(methodSymbol.getSymbolsForThisScope());
		//don't set the return type leave that open.
	}
	
	public MethodSymbolSearch(String name)
	{
		super(name);	
		setSearchType(ISymbol.SymbolCategory.METHOD);
	}
	
	public MethodSymbolSearch(String name, Optional<ISymbol> ofTypeOrReturn)
	{
		super(name, ofTypeOrReturn);
		setSearchType(ISymbol.SymbolCategory.METHOD);
	}
	
	public MethodSymbolSearch(String name, ISymbol ofTypeOrReturn)
	{
		super(name, ofTypeOrReturn);
		setSearchType(ISymbol.SymbolCategory.METHOD);
	}
	
	@Override
	public MethodSymbolSearch setOfTypeOrReturn(Optional<ISymbol> ofTypeOrReturn)
	{
		super.setOfTypeOrReturn(ofTypeOrReturn);
		return this;
	}
	
	@Override
	public MethodSymbolSearch setOfTypeOrReturn(ISymbol ofTypeOrReturn)
	{
		super.setOfTypeOrReturn(ofTypeOrReturn);
		return this;
	}
}
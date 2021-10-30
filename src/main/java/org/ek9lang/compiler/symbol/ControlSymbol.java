package org.ek9lang.compiler.symbol;

import java.util.Optional;

/**
 * EK9 switch/try control type symbol - this can effectively return a value if it is configured with returning part.
 * 
 * When generating out put we need this to create it's own block so variables inside are hidden from later scopes.
 * 
 * So as we have a returning part (optional) we need a scope to put it in (i.e this scope) this then means that when
 * 
 * Finally when coming to generate the output - this symbol will be able to supply the outer variable to set the result to.
 */
public class ControlSymbol extends ScopedSymbol
{
	private Optional<ISymbol> returningSymbol = Optional.ofNullable(null);
	
	/*
	 * This type of switch statement can actually effectively return a value.
	 * This is the symbol we want to set the return to be.
	 */
	private Optional<ISymbol> toBeSetToResult = Optional.ofNullable(null);
	
	public ControlSymbol(String name, IScope enclosingScope)
	{
		super(name, enclosingScope);
		super.setCategory(SymbolCategory.CONTROL);
		//Say function for now
		super.setGenus(SymbolGenus.FUNCTION);
	}

	@Override
	public ControlSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoControlSymbol(new ControlSymbol(this.getName(), withParentAsAppropriate));
	}

	protected ControlSymbol cloneIntoControlSymbol(ControlSymbol newCopy)
	{
		super.cloneIntoScopeSymbol(newCopy);
		if(returningSymbol.isPresent())
			newCopy.returningSymbol = Optional.of(returningSymbol.get());
		if(toBeSetToResult.isPresent())
			newCopy.toBeSetToResult = Optional.of(toBeSetToResult.get());

		return newCopy;
	}

	public Optional<ISymbol> getToBeSetToResult()
	{
		return toBeSetToResult;
	}

	public void setToBeSetToResult(ISymbol toBeSetToResult)
	{
		this.toBeSetToResult = Optional.ofNullable(toBeSetToResult);
	}
	
	public Optional<ISymbol> getReturningSymbol()
	{
		return returningSymbol;
	}

	public void setReturningSymbol(ISymbol returningSymbol)
	{
		this.returningSymbol = Optional.ofNullable(returningSymbol);
		if(returningSymbol != null)
			this.setType(returningSymbol.getType());
	}
}
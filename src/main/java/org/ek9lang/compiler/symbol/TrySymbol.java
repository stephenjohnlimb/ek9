package org.ek9lang.compiler.symbol;

/**
 * EK9 try statement - this can effectively return a value if it is configured with returning part.
 * When generating out put we need this to create its own block so variables inside are hidden from later scopes.
 * 
 * So as we have a returning part (optional) we need a scope to put it in (i.e. this scope) this then means that when
 * we use the catch and finally parts of the try we can find that returning variable.
 */
public class TrySymbol extends ControlSymbol
{
	public TrySymbol(IScope enclosingScope)
	{
		super("Try", enclosingScope);
	}

	@Override
	public TrySymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoTrySymbol(new TrySymbol(withParentAsAppropriate));
	}

	protected TrySymbol cloneIntoTrySymbol(TrySymbol newCopy)
	{
		super.cloneIntoControlSymbol(newCopy);
		return newCopy;
	}
}

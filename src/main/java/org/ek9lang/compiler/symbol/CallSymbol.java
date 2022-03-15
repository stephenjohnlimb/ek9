package org.ek9lang.compiler.symbol;

/**
 * Just re-uses the bulk of method symbol for when we want to make a symbol that is a call to an actual method.
 * This will be used to build up the sort of call we want to make based on the source - we then have to resolve
 * this CallSymbol against a real method symbol.
 */
public class CallSymbol extends MethodSymbol implements IAssignableSymbol
{
	private MethodSymbol resolvedMethodToCall = null;

	public CallSymbol(String name, IScope enclosingScope)
	{
		super(name, enclosingScope);
	}

	@Override
	public CallSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoCallSymbol(new CallSymbol(getName(), withParentAsAppropriate));
	}

	protected CallSymbol cloneIntoCallSymbol(CallSymbol newCopy)
	{
		super.cloneIntoMethodSymbol(newCopy);
		newCopy.resolvedMethodToCall = resolvedMethodToCall;
		return newCopy;
	}

	public MethodSymbol getResolvedMethodToCall()
	{
		return resolvedMethodToCall;
	}

	public void setResolvedMethodToCall(MethodSymbol resolvedMethodToCall)
	{
		this.resolvedMethodToCall = resolvedMethodToCall;
		//make a note if this method ia actually an operator.
		this.setOperator(resolvedMethodToCall.isOperator());
	}

	@Override
	public String getFriendlyScopeName()
	{
		return getFriendlyName();
	}

	@Override
	public String getFriendlyName()
	{
		if(resolvedMethodToCall == null)
			return getName();
		return getName() + " => " + resolvedMethodToCall.getFriendlyName();
	}
}

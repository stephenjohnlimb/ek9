package org.ek9lang.compiler.symbol;

import java.util.Optional;

public class VariableSymbol extends Symbol implements IAssignableSymbol
{
	private boolean loopVariable = false;

	private boolean incomingParameter = false;
	
	private boolean returningParameter = false;
	
	/**
	 * Limited scope of variables from other scopes
	 * fields/properties on classes/components etc are always private
	 * Those on records are public and local variable a visible up the scope tree.
	 * So it depends where you are accessing the variable from.
	 */
	private boolean isPrivate = false;
	
	private boolean isAggregatePropertyField = false;
	
	public VariableSymbol(String name)
	{
		this(name, Optional.ofNullable(null));
	}
	
	public VariableSymbol(String name, ISymbol type)
	{
		this(name, Optional.ofNullable(type));
	}
	
	public VariableSymbol(String name, Optional<ISymbol> type)
	{
		super(name, type);	
		super.setGenus(SymbolGenus.VALUE);
	}	

	@Override
	public VariableSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoVariable(new VariableSymbol(this.getName(), this.getType()));
	}

	protected VariableSymbol cloneIntoVariable(VariableSymbol newCopy)
	{
		cloneIntoSymbol(newCopy);
		newCopy.loopVariable = this.loopVariable;
		newCopy.incomingParameter = this.incomingParameter;
		newCopy.returningParameter = this.returningParameter;
		newCopy.setPrivate(this.isPrivate);
		newCopy.setAggregatePropertyField(this.isAggregatePropertyField);
		return newCopy;
	}

	public boolean isAggregatePropertyField()
	{
		return isAggregatePropertyField;
	}

	public void setAggregatePropertyField(boolean isAggregatePropertyField)
	{
		this.isAggregatePropertyField = isAggregatePropertyField;
	}

	@Override
	public boolean isPrivate()
	{
		return isPrivate;
	}

	public void setPrivate(boolean isPrivate)
	{
		this.isPrivate = isPrivate;
	}

	@Override
	public boolean isPublic()
	{
		return !isPrivate;
	}

	@Override
	public boolean isLoopVariable()
	{
		return loopVariable;
	}

	@Override
	public void setLoopVariable(boolean asLoopVar)
	{
		loopVariable = asLoopVar;
	}

	public boolean isIncomingParameter()
    {
    	return incomingParameter;
    }
	
	public void setIncomingParameter(boolean incomingParameter)
	{
		this.incomingParameter = incomingParameter;
	}
	
	public boolean isReturningParameter()
    {
    	return returningParameter;
    }
	
	public void setReturningParameter(boolean returningParameter)
	{
		this.returningParameter = returningParameter;
	}

	@Override
	public boolean isMutable()
	{
		return true;
	}

	@Override
	public String getFriendlyName()
	{
		StringBuffer rtn = new StringBuffer();
		if(this.isPrivate)
			rtn.append("private ");
		rtn.append(super.getFriendlyName());		
		return rtn.toString();
	}	
}

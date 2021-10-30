package org.ek9lang.compiler.symbol;

import java.util.Optional;

/**
 * A very simple value that hold a single distinct value.
 * For example PI = 3.242 would be a constant.
 * 
 */
public class ConstantSymbol extends Symbol implements SymbolType
{
	private boolean literal = false;
	
	/**
	 * Now we can have constants that are just general constants.
	 * But others that are defined at the module scope level.
	 * We need to distinguish these for various reasons.
	 */
	private boolean atModuleScope = false;
	
	public ConstantSymbol(String name, boolean fromLiteral)
	{
		this(name, Optional.ofNullable(null));
		this.literal = fromLiteral;
	}
	
	public ConstantSymbol(String name)
	{
		this(name, Optional.ofNullable(null));
	}
	
	public ConstantSymbol(String name, ISymbol type, boolean fromLiteral)
	{
		this(name, Optional.ofNullable(type));
		this.literal = fromLiteral;
	}
	
	public ConstantSymbol(String name, ISymbol type)
	{
		this(name, Optional.ofNullable(type));
	}
	
	public ConstantSymbol(String name, Optional<ISymbol> type)
	{
		super(name, type);
		super.setGenus(SymbolGenus.VALUE);
	}

	@Override
	public ConstantSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoConstant(new ConstantSymbol(this.getName(), this.getType()));
	}

	protected ConstantSymbol cloneIntoConstant(ConstantSymbol newCopy)
	{
		cloneIntoSymbol(newCopy);
		newCopy.literal = this.literal;
		newCopy.atModuleScope = this.atModuleScope;
		return newCopy;
	}

	public boolean isAtModuleScope()
	{
		return atModuleScope;
	}

	public void setAtModuleScope(boolean atModuleScope)
	{
		this.atModuleScope = atModuleScope;
		//If this is a module scope level as a constant then we need fully qualified
		//It may have to be further mangled for out put depending on where it is output.
		if(atModuleScope)
			super.setProduceFullyQualifiedName(true);
	}

	public boolean isFromLiteral()
	{
		return literal;
	}

	@Override
	public boolean isAConstant()
	{
		return true;
	}
	
	public String toString()
	{
		StringBuffer buffer = new StringBuffer("const ");
		buffer.append(getFriendlyName());		
		return buffer.toString();
	}
}

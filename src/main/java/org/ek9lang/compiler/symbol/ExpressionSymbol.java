package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.Token;

import java.util.Optional;

/**
 * While we don't add these in the scoped structures when compiling.
 * 
 * We do use these to augment the parse tree for the appropriate context.
 * 
 *  We do this so that we can work out what type of result will be returned from an expression.
 *  
 *  See methods like isExactSameType and isAssignableTo for type checking in the Symbol class.
 *  
 *  The idea is to augment the parse tree with these expression symbols so that once we've been through
 *  sufficient passes of the tree we have sufficient information to add in any promotions/coercions and also check
 *  the type of parameters on operations are compatible. We can then add in explicit IR type conversions if needed.
 *  This information will also be used in the semantic analysis phase.
 */
public class ExpressionSymbol extends Symbol implements IAssignableSymbol
{
	//So could need to promote to get right type
	private boolean promotionRequired = false;
	//Or maybe just need to call the $ _string() operator (this the returning type has it).
	private boolean useStringOperator = false;
	
	public ExpressionSymbol(ISymbol symbol)
	{
		super(symbol.getName());
		super.setGenus(symbol.getGenus());
		setType(symbol.getType());
		setCategory(symbol.getCategory());		
	}
	
	public ExpressionSymbol(String name)
	{
		super(name);
		super.setGenus(SymbolGenus.VALUE);		
	}

	@Override
	public ExpressionSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoExpressionSymbol(new ExpressionSymbol(this.getName()));
	}

	protected ExpressionSymbol cloneIntoExpressionSymbol(ExpressionSymbol newCopy)
	{
		super.cloneIntoSymbol(newCopy);
		newCopy.promotionRequired = promotionRequired;
		newCopy.useStringOperator = useStringOperator;
		return newCopy;
	}
	
	@Override
	public boolean isMutable()
	{
		return false;
	}
	
	@Override
	public boolean isAConstant()
	{
		return true;
	}

	@Override
	public void setNotMutable()
	{
		throw new RuntimeException("Compiler error: Base ExpressionSymbol does not support mutability");
	}

	public boolean isPromotionRequired()
	{
		return promotionRequired;
	}

	public void setPromotionRequired(boolean promotionRequired)
	{
		this.promotionRequired = promotionRequired;
	}	
	
	public boolean isUseStringOperator()
	{
		return useStringOperator;
	}

	public void setUseStringOperator(boolean useStringOperator)
	{
		this.useStringOperator = useStringOperator;
	}

	@Override
	public void setSourceToken(Token sourceToken)
	{
		//This is also where this expression is initialised.
		//If the items making the expression have not been initialised that has to be checked
		//when the expression gets created.
		super.setSourceToken(sourceToken);
		setInitialisedBy(sourceToken);
	}
}

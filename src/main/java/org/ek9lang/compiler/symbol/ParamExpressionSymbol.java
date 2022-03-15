package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * While we don't add these in the scoped structures when compiling.
 * We do use these to augment the parse tree for the appropriate context.
 * We do this so that we can work out what type of result will be returned from an expression.
 * For this call we need the order list of parameters specifically the types of those parameters.
 */
public class ParamExpressionSymbol extends Symbol
{
	private final List<ISymbol> params = new ArrayList<>();

	public ParamExpressionSymbol(String name)
	{
		super(name);
	}

	public ParamExpressionSymbol addParameter(ISymbol symbol)
	{
		params.add(symbol);
		return this;
	}

	@Override
	public ParamExpressionSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoStreamPipeLineSymbol(new ParamExpressionSymbol(getName()));
	}

	protected ParamExpressionSymbol cloneIntoStreamPipeLineSymbol(ParamExpressionSymbol newCopy)
	{
		super.cloneIntoSymbol(newCopy);
		getParameters().forEach(newCopy::addParameter);
		return newCopy;
	}

	public List<ISymbol> getParameters()
	{
		return Collections.unmodifiableList(params);
	}

	@Override
	public String getFriendlyName()
	{
		return CommonParameterisedTypeDetails.asCommaSeparated(params, true);
	}
}

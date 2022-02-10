package org.ek9lang.compiler.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * While we don't add these in the scoped structures when compiling.
 * We do use these to augment the parse tree for the appropriate context.
 * We do this so that we can work out what type of result will be returned from an expression.
 * For this call we need the order list of parameters specifically the types of those parameters.
 */
public class ParamExpressionSymbol extends Symbol
{
	private List<ISymbol> params = new ArrayList<ISymbol>();

	public ParamExpressionSymbol(String name)
	{
		super(name);
	}

	public ParamExpressionSymbol addParameter(ISymbol symbol)
	{
		params.add(symbol);
		return this;
	}

	public List<ISymbol> getParameters()
	{
		return Collections.unmodifiableList(params);
	}

	@Override
	public String getFriendlyName()
	{
		return "(" + params.stream().map(symbol -> symbol.getFriendlyName()).collect(Collectors.joining(", ")) + ")";
	}
}

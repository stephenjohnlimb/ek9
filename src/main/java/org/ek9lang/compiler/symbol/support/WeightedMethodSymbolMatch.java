package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.MethodSymbol;

public class WeightedMethodSymbolMatch
{
	private double weight;
	private MethodSymbol methodSymbol;
	
	public WeightedMethodSymbolMatch(MethodSymbol methodSymbol, double matchWeight)
	{
		this.weight = matchWeight;
		this.methodSymbol = methodSymbol;
	}

	public double getWeight()
	{
		return weight;
	}

	public MethodSymbol getMethodSymbol()
	{
		return methodSymbol;
	}

	@Override
	public String toString()
	{
		//handy for debugging.
		StringBuffer buffer = new StringBuffer(methodSymbol.toString());
		buffer.append(": ");
		buffer.append(weight);
		
		return buffer.toString();
	}
}

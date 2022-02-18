package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.compiler.symbol.support.search.WeightedMethodSymbolMatch;
import org.ek9lang.core.exception.AssertValue;

import java.util.List;
import java.util.Optional;

/**
 * Given some search criteria and a List of Symbols, this class will find the best match.
 * 
 * The main reason for this is to avoid method name mangling and incorporate coercion, and interface/super type matches.
 */
public class SymbolMatcher
{
	
	/**
	 * Match the search criteria against one or more symbol methods.
	 * We use a weighting algorithm to try and find the best match where there are methods of the same name.
	 */
	public MethodSymbolSearchResult addMatchesToResult(MethodSymbolSearchResult result, SymbolSearch criteria, List<MethodSymbol> methodSymbols)
	{	
		AssertValue.checkNotNull("Search cannot be null", criteria);
		if(methodSymbols == null || methodSymbols.isEmpty())
			return result;
		
		methodSymbols.forEach(methodSymbol -> {
			double weight = getWeightOfMethodMatch(criteria, methodSymbol);
			//System.out.println("[" + weight + "] S: " + criteria.toString() + "  M: " + methodSymbol.toString());
			
			if(weight >= 0.0)
				result.add(new WeightedMethodSymbolMatch(methodSymbol, weight));
		});
		return result;
	}
	
	/**
	 * Determines how good a fit the search criteria is against the Method Symbol.
	 * We match (perfect fit for method name), then return types and parameter types.
	 * @param criteria The method criteria we are trying to match
	 * @param methodSymbol The method symbol to check against
	 * @return The weight of the match. 100.0 being perfect fit and 0.0 or negative being not fit at all. If between these values we've had to coerce.
	 */
	private double getWeightOfMethodMatch(SymbolSearch criteria, MethodSymbol methodSymbol)
	{
		double rtn = -1.0;
		//name must match fully
		if(!criteria.getName().equals(methodSymbol.getName()))
			return rtn; //so this is no match at all
		
		//Now need to check on method parameter symbols and match those against the parameters on the method.
		
		double paramCost = getWeightOfParameterMatch(criteria.getParameters(), methodSymbol.getSymbolsForThisScope());
		//System.out.println("Param Cost: " + paramCost);
		if(paramCost < 0.0)
			return rtn;
		
		//Only check if we have a criteria to match
		if(criteria.getOfTypeOrReturn().isPresent())
		{
			double rtnCost = getWeightOfMatch(methodSymbol.getType(), criteria.getOfTypeOrReturn());
			if(rtnCost < 0.0)
				return rtn;
		}
		
		rtn = 100.0 - paramCost;

		return rtn;
	}
	
	public double getWeightOfParameterMatch(List<ISymbol> fromSymbols, List<ISymbol> toSymbols)
	{
		double rtn = -1.0;
		
		int numParams1LookedFor = fromSymbols.size();
		int numParams2lookedFor = toSymbols.size();
		
		//So this cannot be a match
		if(numParams1LookedFor != numParams2lookedFor)
			return rtn ;
		
		double paramCost = 0.0;
		for(int i=0; i<numParams1LookedFor; i++)
		{
			ISymbol from = fromSymbols.get(i);
			if(from.getType().isEmpty())
				throw new RuntimeException("From type is not present for [" + from + "]");
			ISymbol fromType = from.getType().get();
			
			ISymbol to = toSymbols.get(i);
			if(to.getType().isEmpty())
				throw new RuntimeException("To type is not present for [" + to + "]");
			
			ISymbol toType = to.getType().get();
			
			double thisCost = getCostOfSymbolMatch(fromType, toType);
			//System.out.println("P: " + thisCost + " " + from.getName() + " " + to.getName());
			if(thisCost < 0.0)			
				return rtn; //No match
			paramCost += thisCost;
		}
		rtn = paramCost;
		return rtn;
	}
	
	public double getWeightOfMatch(Optional<ISymbol> fromSymbol, Optional<ISymbol> toSymbol)
	{
		double rtn = -1.0;
		
		//So neither is set that's Ok
		if(fromSymbol.isEmpty() && toSymbol.isEmpty())
			return 0.0;
		if(fromSymbol.isPresent() && toSymbol.isEmpty())
			return rtn;
		if(fromSymbol.isEmpty())
			return rtn;
					
		//Ok so both set lets see what the cost is
		
		ISymbol from = fromSymbol.get();
		ISymbol to = toSymbol.get();
		
		double costOfMatch = getCostOfSymbolMatch(from, to);
		if(costOfMatch < 0.0)
			return rtn; //not a match

		return costOfMatch;
	}

	private double getCostOfSymbolMatch(ISymbol toMatch, ISymbol thisSymbolType)
	{		
		return toMatch.getAssignableWeightTo(thisSymbolType);
	}
}
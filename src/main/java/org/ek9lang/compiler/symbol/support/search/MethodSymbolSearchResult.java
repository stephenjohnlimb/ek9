package org.ek9lang.compiler.symbol.support.search;

import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MethodSymbolSearchResult
{
	/**
	 * The actual results of the method search.
	 *
	 * It's a bit tricky matching methods in aggregate hierarchies and with method overloading
	 * but also with class compatibility or parameters and possible coercions.
	 */
	private List<WeightedMethodSymbolMatch> results = new ArrayList<WeightedMethodSymbolMatch>();

	private boolean accessModifierIncompatible = false;
	
	private boolean methodNotMarkedWithOverride = false;
	
	public MethodSymbolSearchResult()
	{
		
	}
	
	public MethodSymbolSearchResult(MethodSymbolSearchResult startWithResults)
	{
		add(startWithResults.results);
		setAccessModifierIncompatible(startWithResults.accessModifierIncompatible);
		setMethodNotMarkedWithOverride(startWithResults.methodNotMarkedWithOverride);
	}
	
	public boolean isAccessModifierIncompatible()
	{
		return accessModifierIncompatible;
	}

	public void setAccessModifierIncompatible(boolean accessModifierIncompatible)
	{
		//once true always true
		this.accessModifierIncompatible |= accessModifierIncompatible;
	}	
	
	public boolean isMethodNotMarkedWithOverride()
	{
		return methodNotMarkedWithOverride;
	}

	public void setMethodNotMarkedWithOverride(boolean methodNotMarkedWithOverride)
	{
		//once true always true		
		this.methodNotMarkedWithOverride |= methodNotMarkedWithOverride;
	}

	/**
	 * Typically used with traits and class/trait combinations so that multiple methods of the same name and parameters.
	 * are merged together this may then mean that the results have two results of the same weight and this indicates ambiguity.
	 * So for example if you had ClassA implementing interfaceZ (lets say default method methodBoo in interfaceZ) all is good
	 * Now say we have interfaceP (also with method methodBoo) and now add ClassK that extends ClassA and implements interfaceP
	 * We now have a situation where 'methodBoo' has two default implementations (diamond-ish); and therefore we must define one in ClassK.
	 * We need to know that there are two methods that would match via different routes - it's not the case where one has overridden the other
	 * that's what default methods in traits/interfaces has introduced.
	 * @param withResults The results to merge in
	 * @return A new set of results - does not alter either of the two sets being merged.
	 */
	public MethodSymbolSearchResult mergePeerToNewResult(MethodSymbolSearchResult withResults)
	{
		MethodSymbolSearchResult rtn = new MethodSymbolSearchResult();
		rtn.setAccessModifierIncompatible(withResults.isAccessModifierIncompatible());
		rtn.setMethodNotMarkedWithOverride(withResults.isMethodNotMarkedWithOverride());
		rtn.add(this.results);
		rtn.add(withResults.results);
		return rtn;
	}
	
	/**
	 * So now let's imagine we have a set of results from various interfaces and classes (maybe using method above to accumulate the methods)
	 * But now you are dealing with a Class or a Trait - now we need to know that for all the methods that could be called we only have one
	 * single one that would be resolved. If this is not the case then it is an indicator that the method should be implemented in the class
	 * so as tobe explicit which implementation is to be used.
	 * 
	 * So we will be checking for matching method name and parameters and compatible covariance return types.
	 * @param withResults The set of methods that can be used to override one or more results.
	 * @return A new set of results - does not alter either of the two sets being merged.
	 */
	public MethodSymbolSearchResult overrideToNewResult(MethodSymbolSearchResult withResults)
	{
		//Always include new results - but we may not always include the set we have already gathered
		MethodSymbolSearchResult buildResult = new MethodSymbolSearchResult(withResults);		
		
		//But if with result is empty then we can just include all our current set or results
		if(withResults.isEmpty())
			buildResult.add(results);
				
		for(WeightedMethodSymbolMatch newResult : withResults.results)
		{			
			for(WeightedMethodSymbolMatch result : results)
			{		
				MethodSymbol methodSymbol = newResult.getMethodSymbol();
				//You need to get this the right way around in terms of checking params and compatible return types.
				//System.out.println("Sig check for [" + newResult + "] against [" + result + "]");
				if(!methodSymbol.isSignatureMatchTo(result.getMethodSymbol()))
				{
					//So not a signature match then just add in result
					//System.out.println("NO Signature match for " + result.getMethodSymbol().getName());
					buildResult.add(result);
				}
				else
				{
					if(!methodSymbol.getAccessModifier().equals(result.getMethodSymbol().getAccessModifier()))
					{
						//OK so signature is a match but the access modifier is different - so this is ambiguous but because the access
						//is either stricter or more lax.
						buildResult.add(result);
						buildResult.setAccessModifierIncompatible(true);
					}
					else if(!methodSymbol.getAccessModifier().equals("private") && !methodSymbol.isOverride())
					{
						//So we are here and the method signatures are the same, so it's an override but has not been marked as such
						buildResult.setMethodNotMarkedWithOverride(true);
					}					
				}				
			}
		}
		
		return buildResult;
	}

	public Optional<MethodSymbol> getSingleBestMatchMethodSymbol()
	{
		if(isSingleBestMatchPresent())
			return Optional.of(results.get(0).getMethodSymbol());
		return Optional.ofNullable(null);
	}

	public Optional<ISymbol> getSingleBestMatchSymbol()
	{
		if(isSingleBestMatchPresent())
			return Optional.of(results.get(0).getMethodSymbol());
		return Optional.ofNullable(null);
	}
	/**
	 * Is there a single best match available.
	 * Could be there are no results, could be a single (very good result - perfect match),
	 * Could be a match but not perfect but acceptable.
	 * But could also be several results all of equal weight; in which case we have an ambiguity.
	 * @return true if there is a single best match.
	 */
	public boolean isSingleBestMatchPresent()
	{
		if(!isEmpty())
		{
			if(results.size() > 1)
			{
				//need to check first and second to see if weight is same.
				double firstWeight = results.get(0).getWeight();
				double secondWeight = results.get(1).getWeight();
				
				//are these two within a tolerance of each other - if so ambiguous.
				if(Math.abs(firstWeight - secondWeight) < 0.001)
					return false;
				//No one is better than the other.
				return true;
			}
			//yes we have one best match.
			return true;
		}
		//no there is no single best match 
		return false;
	}
	
	public boolean isAmbiguous()
	{
		return !isEmpty() && !isSingleBestMatchPresent();
	}
	
	public String getAmbiguousMethodParameters()
	{
		StringBuffer buffer = new StringBuffer();
		if(!isEmpty())
		{
			if(results.size() > 1)
			{
				ISymbol s = results.get(0).getMethodSymbol();
				buffer.append(s.toString() + " line " + s.getSourceToken().getLine());
				//need to check first and second to see if weight is same.
				double firstWeight = results.get(0).getWeight();
				for(int i=1; i<results.size(); i++)
				{	
					if(Math.abs(firstWeight - results.get(i).getWeight()) < 0.001)
					{
						s = results.get(i).getMethodSymbol();
						buffer.append(" , ");
						buffer.append(s.toString() + " line " + s.getSourceToken().getLine());
					}
					else
						break;
				}
			}
		}
		
		return buffer.toString();
	}
	
	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		boolean first = true;
		for(WeightedMethodSymbolMatch result : results)
		{
			if(!first)
				buffer.append(", ");
			buffer.append(result.toString());
			first = false;
		}
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * @return true if there are no results at all.
	 */
	public boolean isEmpty()
	{
		return results.isEmpty();
	}
	
	public MethodSymbolSearchResult add(List<WeightedMethodSymbolMatch> moreResults)
	{
		results.addAll(moreResults);
		sortResults();
		return this;
	}
	
	public MethodSymbolSearchResult add(WeightedMethodSymbolMatch weightedMethodSymbolMatch)
	{
		results.add(weightedMethodSymbolMatch);
		sortResults();
		return this;
	}
	
	private void sortResults()
	{
		results.sort(new Comparator<WeightedMethodSymbolMatch>() {
			@Override
			public int compare(WeightedMethodSymbolMatch o1, WeightedMethodSymbolMatch o2) {
				
				return Double.compare(o2.getWeight(), o1.getWeight());
			}
		});		
	}
}
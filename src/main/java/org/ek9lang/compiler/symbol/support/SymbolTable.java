package org.ek9lang.compiler.symbol.support;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * We need to support simple things like classes which are unique per symbol table
 * Or the fully qualified class names like 'com.some.A and com.other.A' so they won't collide.
 * But we don't want to define constants/variables like 'int A and float A' this is a duplicate
 * We do want method overloading like int A() and int A(String some param)
 * But not int A() and float A() as that is not good.
 * <pre>
 * So for the most part this is straight forward, but this issue is operators and type matching.
 * i.e support for
 * 1. int A()
 * 2. int A(String v1)
 * 3. int A(String v1, int someItem)
 * 4. int A(int item, String other)
 * 5. int A(List<String> list1)
 * 6. int A(List<Integer> list2)
 * 7. float A(List<Integer> list3, String other)
 * 8. int A(List<SpecialString> list4, String other)
 * 9. int A(SomeClass sc)
 * 10. int A(ClassThatExtendsSomeClass supersc)
 * </pre>
 * When we define these we want them all to be defined and available in the appropriate scope.
 * The issue is doing the resolve! We can't just say hey I'm trying to resolve 'A' that's just not enough
 * We need to know:
 * <pre>
 * 1. Are we looking for a class(type), function/method or variable/constant in this context.
 * 2. What is it's name - in this case 'A'
 * 3. What type (or return type) are we expecting - but here we should accept a super class match with some weight match value
 * 4. What is the order and type of parameters - again we need to try and match with some weight using super class matches.
 * </pre>
 * 
 * So what sort of algorithm can we use for this?
 * <pre>
 * The Symbol has a type already when we define it - we know if it is an AggregateSymbol, MethodSymbol etc.
 * We also know its name!
 * We know the Symbol we will use for it's return type or what it is i.e the Symbol of Integer as the return type or what it is.
 * For methods (which are Scoped Symbols) we also have the parameters (in order). So while parameters can be used when making the call
 * this is just sugar - the order still has to match.
 * 
 * Well we could have separate internal tables for methods/functions, classes/types and variables. So we can quickly and simply resolve
 * obvious ones like variables and types. Then use more expensive operations for methods.
 * 
 * Now for methods we can store in a hash map of method names - so in a simple case with just one method of name A we're done!
 * But in that hashmap we store and List of all methods of that name - now we have the costly activity of going through each and getting the
 * weight of each to find a match. in local scopes which ever is the best match we return - but clearly it is possible there is no match
 * then we resort to moving back up the scope tree (as we do now).
 * </pre>
 */
public class SymbolTable implements IScope
{
	private String scopeName = "global";

	/**
	 * We now store the symbols in separate areas for quick access.
	 * It might seem strange but for some symbols like methods we have a single name
	 * but a list of actual symbols. i.e. method overloading.
	 */
	private final Map<ISymbol.SymbolCategory, Map<String, List<ISymbol>>> splitSymbols = new HashMap<>();

	//But also an ordered list - useful for ordered parameters.
	private final List<ISymbol> orderedSymbols = new ArrayList<>();

	private final AggregateSupport aggregateSupport = new AggregateSupport();
	private final SymbolMatcher matcher = new SymbolMatcher();

	/**
	 * If we encounter an exception within a scope we need to note the line number
	 */
	private Token encounteredExceptionToken = null;
	
	public SymbolTable(String scopeName)
	{		
		this.scopeName = scopeName;
	}
	
	public SymbolTable()
	{
	}

	@Override
	public String getScopeName()
	{
		return scopeName;
	}

	public Token getEncounteredExceptionToken()
	{
		return encounteredExceptionToken;
	}

	public void setEncounteredExceptionToken(Token encounteredExceptionToken)
	{
		this.encounteredExceptionToken = encounteredExceptionToken;
	}

	@Override
	public SymbolTable clone(IScope withParentAsAppropriate)
	{
		return cloneIntoSymbolTable(new SymbolTable(this.getScopeName()), withParentAsAppropriate);
	}

	protected SymbolTable cloneIntoSymbolTable(SymbolTable rtn, IScope withParentAsAppropriate)
	{
		rtn.scopeName = this.scopeName;
		orderedSymbols.forEach(symbol -> rtn.define(symbol.clone(withParentAsAppropriate)));

		return rtn;
	}

	@Override
	public void define(ISymbol symbol)
	{
		//Add in the split symbols and also the ordered symbols.
		AssertValue.checkNotNull("Symbol cannot be null", symbol);
		addToSplitSymbols(symbol);
		orderedSymbols.add(symbol);
	}

	/**
	 * So we have a split map for each category
	 * each of those has a map with a list of symbols of the same name.
	 * So this means we can handle multiple methods with same name but different signatures.
	 */
	private void addToSplitSymbols(ISymbol symbol)
	{
		Map<String, List<ISymbol>> table = splitSymbols.computeIfAbsent(symbol.getCategory(), k -> new HashMap<>());
		List<ISymbol> list = table.computeIfAbsent(symbol.getName(), k -> new ArrayList<>());
		if(!symbol.getCategory().equals(ISymbol.SymbolCategory.METHOD) && list.contains(symbol))
			throw new RuntimeException("Compiler Coding Error - Duplicate symbol [" + symbol + "]");
		list.add(symbol);		
	}

	/**
	 * Find symbols in a specific category.
	 */
	public List<ISymbol> getSymbolsForThisScopeOfCategory(ISymbol.SymbolCategory category)
	{
		return orderedSymbols.stream().filter(symbol -> category.equals(symbol.getCategory())).collect(Collectors.toList());
	}

	/**
	 * Get all the symbols in this table.
	 */
	@Override
	public List<ISymbol> getSymbolsForThisScope()
	{
		return Collections.unmodifiableList(orderedSymbols);
	}

	/**
	 * Search and resolve from a symbol search.
	 */
	public Optional<ISymbol> resolve(SymbolSearch search)
	{
		Optional<ISymbol> rtn = resolveInThisScopeOnly(search);
		if(rtn.isEmpty())
			rtn = resolveWithEnclosingScope(search);
		
		return rtn;
	}

	/**
	 * Add all matching methods for a method search.
	 */
	@Override
	public MethodSymbolSearchResult resolveForAllMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		MethodSymbolSearchResult buildResult = new MethodSymbolSearchResult(result);
		
		//Do our enclosing scope first then this scope.
		buildResult = buildResult.mergePeerToNewResult(resolveForAllMatchingMethodsInEnclosingScope(search, new MethodSymbolSearchResult()));
		
		//So override results with anything from this scope
		buildResult = buildResult.overrideToNewResult(resolveForAllMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult()));
				
		return buildResult;
	}

	/**
	 * Add all matching methods for a method search but only in this scope.
	 */
	@Override
	public MethodSymbolSearchResult resolveForAllMatchingMethodsInThisScopeOnly(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		Map<String, List<ISymbol>> table = splitSymbols.get(search.getSearchType());
		//not found
		if(table != null && !table.isEmpty())
		{			
			List<ISymbol> list = table.get(search.getName());
			if(list != null && !list.isEmpty())
			{
				List<MethodSymbol> methodList = list.stream().map(symbol -> (MethodSymbol)symbol).collect(Collectors.toList());
				matcher.addMatchesToResult(result, search, methodList);
			}	
		}
		//System.out.println("SymbolTable: " + result);
		return result;
	}

	/**
	 * There are no supers so this will not add any methods.
	 */
	protected MethodSymbolSearchResult resolveForAllMatchingMethodsInEnclosingScope(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		//nothing needed  here.
		return result;
	}

	/**
	 * Resolve a symbol in this symbol table only.
	 */
	@Override
	public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search)
	{
		AssertValue.checkNotNull("Search cannot be null", search);
		
		String symbolName = search.getName();
		if(symbolName.contains("::"))
		{
			//Then it is a fully qualified search so if the scope name of this table does not match
			//what has been provided then even if the symbol name matches it's a miss.
			if(!this.getScopeName().equals(aggregateSupport.getModuleNameIfPresent(symbolName)))
				return Optional.empty();

			//So now just use the actual symbol name part of com.something:MyClass i.e. use the MyClass bit.
			symbolName = aggregateSupport.getUnqualifiedName(symbolName);
		}
		
		//So if search type is not set then that means search all categories!
		ISymbol.SymbolCategory searchType = search.getSearchType();
		if(searchType == null)
		{
			Optional<ISymbol> rtn = Optional.empty();
			
			for(ISymbol.SymbolCategory key : ISymbol.SymbolCategory.values())
			{
				Map<String, List<ISymbol>> table = splitSymbols.get(key);
				search.setSearchType(key);
				rtn = resolveInThisScopeOnly(table, symbolName, search);
				if(rtn.isPresent())
					break;
			}
			//Set search type back again.
			search.setSearchType(null);
			return rtn;
		}
		else
		{
			Map<String, List<ISymbol>> table = splitSymbols.get(search.getSearchType());
			return resolveInThisScopeOnly(table, symbolName, search);
		}
	}
	
	private Optional<ISymbol> resolveInThisScopeOnly(Map<String, List<ISymbol>> table, String shortSymbolName, SymbolSearch search)
	{
		//not found
		if(table == null || table.isEmpty())
			return Optional.empty();
		
		List<ISymbol> symbolList = table.get(shortSymbolName);
		
		//not found
		if(symbolList == null || symbolList.isEmpty())
			return Optional.empty();
		
		return resolveInThisScopeOnly(symbolList, search);
	}

	/**
	 * This is really the backbone of the symbol table and pretty much the compiler.
	 * Resolving a symbol of a specific type using the symbol search criteria.
	 *
	 */
	private Optional<ISymbol> resolveInThisScopeOnly(List<ISymbol> symbolList, SymbolSearch search)
	{
		Optional<ISymbol> rtn;
		
		if(search.getSearchType().equals(ISymbol.SymbolCategory.METHOD))
		{
			MethodSymbolSearch methodSearch = new MethodSymbolSearch(search);
			MethodSymbolSearchResult result = resolveForAllMatchingMethodsInThisScopeOnly(methodSearch, new MethodSymbolSearchResult());
				
			if(result.isEmpty())
			{
				rtn = Optional.empty();
			}
			else if(result.isSingleBestMatchPresent())
			{
				rtn = result.getSingleBestMatchSymbol();
			}
			else
			{
				//This is ambiguous - i.e. we found more than one method that would match.
				//so report failed to find, calling code then needs
				//to do a fuzzy search and report on ambiguities to make developer choose.
				rtn = Optional.empty();
			}
		}
		else if(search.getSearchType().equals(ISymbol.SymbolCategory.FUNCTION))
		{
			//We can only have one function of a single name in a specific scope - no function method overloading
			//That is the main difference between functional and methods.
			//More like C unique function name.
			rtn = Optional.of(symbolList.get(0));
		}
		else if(search.getSearchType().equals(ISymbol.SymbolCategory.TEMPLATE_TYPE))
		{
			//Search for template type to be used.
			AssertValue.checkRange("Expecting a Single result in the symbol table", symbolList.size(), 1, 1);
			rtn = Optional.of(symbolList.get(0));
		}
		else if(search.getSearchType().equals(ISymbol.SymbolCategory.TEMPLATE_FUNCTION))
		{
			//Search for template function to be used.
			AssertValue.checkRange("Expecting a Single result in the symbol table", symbolList.size(), 1, 1);
			rtn = Optional.of(symbolList.get(0));
		}
		else if(search.getSearchType().equals(ISymbol.SymbolCategory.TYPE))
		{
			//Do a type search
			AssertValue.checkRange("Expecting a Single result in the symbol table", symbolList.size(), 1, 1);
			
			//Now we can also check the types here
			rtn = Optional.of(symbolList.get(0));

			//We provide the scope name so that we can just check the symbol name without scope if appropriate.
			Optional<ISymbol> searchSymbol = search.getNameAsSymbol(this.getScopeName());
			
			//check assignable in some way handles coercion and base/super classes.
			if(searchSymbol.isPresent())
			{
				ISymbol toSet = rtn.get();
				if(!toSet.isAssignableTo(searchSymbol))
					rtn = Optional.empty();				
			}
		}
		else if(search.getSearchType().equals(ISymbol.SymbolCategory.VARIABLE))
		{
			//Do a variable search
			AssertValue.checkRange("Expecting a Single result in the symbol table for " + search, symbolList.size(), 1, 1);
			rtn = Optional.of(symbolList.get(0));
			//So found a variable now need to check that the type is right.
			Optional<ISymbol> foundType = rtn.get().getType();
			
			//We must now check that the found type of the variable is right for what we are searching for.
			Optional<ISymbol> toReceive = search.getOfTypeOrReturn();
			
			if(toReceive.isPresent())
			{
				//So we have found a variable, but it cannot be assigned back to how it can be received.
				if(foundType.isEmpty() || !foundType.get().isAssignableTo(toReceive))
					rtn = Optional.empty();
			}
			//So we'll go with rtn as is.
		}
		else
		{
			throw new RuntimeException("Unknown symbol search type [" + search.getSearchType() + "]");
		}
		return rtn ;
	}

	/**
	 * This class is the root and so there is no enclosing scope
	 * sub-classes will override to provide the scope that encloses them
	 */
	protected Optional<ISymbol> resolveWithEnclosingScope(SymbolSearch search)
	{
		return Optional.empty();
	}	
	
	@Override
	public boolean isScopeAMatchForEnclosingScope(IScope toCheck)
	{
		return false;
	}

	@Override
	public Optional<ScopedSymbol> findNearestAggregateScopeInEnclosingScopes()
	{
		return Optional.empty();
	}
	
	@Override
	public String toString()
	{
		return scopeName;
	}
}
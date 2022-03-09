package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface IScope
{
	
	/**
	 * Two main type of scope in use a block is just like a set of instruction inside an if block or a while block
	 * whereas an aggregate block is as the whole class/component level.
	 * So for variable definition it follows that same sort of logic as java not C/C++.
	 * You can have fields as variables with a name say 'v1' and parameters and block declarations of something as 'v1'.
	 * But once in a block scope then you cannot redefine 'v1'. 
	 */
	enum ScopeType {
		AGGREGATE,
		BLOCK
	}

	Object clone(IScope withParentAsAppropriate);

	default ScopeType getScopeType() { return ScopeType.BLOCK; }
	
	String getScopeName();

	/**
	 * Useful for printing out errors and information.
	 * The scope name might be a complex generated name used internally a bit like symbol names are.
	 * So some items are both scopes and symbols - so ideally we'd want to use a friendly name where possible.
	 * @return The friendly name to be used for the developer.
	 */
	default String getFriendlyScopeName() { return getScopeName();}
	
	/**
	 * Typically used with functions.
	 * Something that is pure cannot have 'side effects'.
	 * To enforce this a bit of logic can have no references to other variables, methods and functions 
	 * else how can no side effects be guaranteed. Hence, a function that just tests a value or calculates a result
	 * is deemed pure. Anything else is questionable.
	 * @return true if marked as pure, false otherwise.
	 */
	boolean isMarkedPure();
	
	/**
	 * Used to keep track of any parameterised types used in a generic type that use some or all of the generic parameters.
	 */
	default List<ParameterisedTypeSymbol> getParameterisedTypeReferences() { return new ArrayList<>();}
	
	/**
	 * Keep track of parameterised functions used.
	 */
	default List<ParameterisedFunctionSymbol> getParameterisedFunctionReferences() { return new ArrayList<>();}
	
	/**
	 * Typically in a scoped block we can encounter situations (like exceptions) that cause the block
	 * to end (terminate) early.
	 */
	default boolean isTerminatedNormally()
	{
		return getEncounteredExceptionToken() == null;
	}
	
	Token getEncounteredExceptionToken();
	
	void setEncounteredExceptionToken(Token encounteredExceptionToken);
	/**
	 * Define a Symbol in this scope.
	 */
	void define(ISymbol symbol);

	/**
	 * Provide a list of all the parameters held in this scope and only this scope.
	 */
	List<ISymbol> getSymbolsForThisScope();
	
	/**
	 * Find the nearest symbol of that name up the scope tree.
	 */
	Optional<ISymbol> resolve(SymbolSearch search);
	
	/**
	 * Looks in scope and parent scopes.
	 */
	MethodSymbolSearchResult resolveForAllMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result);

	/**
	 * Look in own scope just for methods and return all those that could match.
	 * ideally there would be one in the case of ambiguities there will be more.	 
	 */
	MethodSymbolSearchResult resolveForAllMatchingMethodsInThisScopeOnly(MethodSymbolSearch search, MethodSymbolSearchResult result);
	
	/** Just look in own scope. */
	Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search);
	
	boolean isScopeAMatchForEnclosingScope(IScope toCheck);
	
	Optional<ScopedSymbol> findNearestAggregateScopeInEnclosingScopes();
}

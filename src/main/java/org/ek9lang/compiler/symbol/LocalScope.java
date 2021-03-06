package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.SymbolTable;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

import java.util.Optional;

/**
 * Used in many ways via composition.
 * The local scope can be either just a block (if, where, etc.) or aggregate (class, component, etc.).
 */
public class LocalScope extends SymbolTable
{
	private IScope.ScopeType scopeType = IScope.ScopeType.BLOCK;
	private final IScope enclosingScope;

	public LocalScope(String scopeName, IScope enclosingScope)
	{
		super(scopeName);
		AssertValue.checkNotNull("EnclosingScope cannot be null", enclosingScope);
		this.enclosingScope = enclosingScope;
	}

	public LocalScope(IScope.ScopeType scopeType, String scopeName, IScope enclosingScope)
	{
		super(scopeName);
		AssertValue.checkNotNull("EnclosingScope cannot be null", enclosingScope);
		this.enclosingScope = enclosingScope;
		this.scopeType = scopeType;
	}

	public LocalScope(IScope enclosingScope)
	{
		this("localScope", enclosingScope);
	}

	@Override
	public LocalScope clone(IScope withParentAsAppropriate)
	{
		return cloneIntoLocalScope(new LocalScope(this.scopeType, this.getScopeName(), withParentAsAppropriate));
	}

	protected LocalScope cloneIntoLocalScope(LocalScope newCopy)
	{
		super.cloneIntoSymbolTable(newCopy, this.enclosingScope);
		//properties set at construction.
		return newCopy;
	}

	@Override
	public IScope.ScopeType getScopeType()
	{
		return scopeType;
	}

	@Override
	public boolean isMarkedPure()
	{
		return super.isMarkedPure();
	}


	protected IScope getEnclosingScope()
	{
		return enclosingScope;
	}

	/**
	 * Useful to be able to check if the scope you have in hand is the same the enclosing scope for this.
	 * <p>
	 * Typically, used when looking at access modifiers.
	 * Find the scope of the aggregate (assuming not a function) where the call is being made from then call this to see
	 * if that scope is the same enclosing scope.
	 * Then you can determine if access should be allowed.
	 *
	 * @param toCheck The scope to check
	 * @return true if a match false otherwise
	 */
	public boolean isScopeAMatchForEnclosingScope(IScope toCheck)
	{
		boolean rtn = false;
		if(toCheck != null)
		{
			rtn = enclosingScope == toCheck;
			if(!rtn && toCheck instanceof ScopedSymbol)
				rtn = ((ScopedSymbol)toCheck).getActualScope() == enclosingScope;
		}
		return rtn;
	}

	/**
	 * Traverses up the scope tree of enclosing scopes to find the first scope type that is an aggregate.
	 * It might not find anything - if you call this in  a local scope in a function or program there is no
	 * aggregate up the enclosing scopes only in class/component type stuff is there an aggregate scope for things.
	 *
	 * @return An Optional scope of the first encounter with and scope that is an aggregate or empty.
	 */
	public Optional<ScopedSymbol> findNearestAggregateScopeInEnclosingScopes()
	{
		if(enclosingScope.getScopeType().equals(ScopeType.AGGREGATE))
			return Optional.of((ScopedSymbol)enclosingScope);

		return enclosingScope.findNearestAggregateScopeInEnclosingScopes();
	}

	@Override
	protected Optional<ISymbol> resolveWithEnclosingScope(SymbolSearch search)
	{
		if(search.isLimitToBlocks() && !enclosingScope.getScopeType().equals(IScope.ScopeType.BLOCK))
			return Optional.empty();

		return enclosingScope.resolve(search);
	}

	protected MethodSymbolSearchResult resolveForAllMatchingMethodsInEnclosingScope(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		return enclosingScope.resolveForAllMatchingMethods(search, result);
	}
}
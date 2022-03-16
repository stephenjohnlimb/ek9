package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScopedSymbol extends Symbol implements IScopedSymbol
{
	private final LocalScope actualScope;

	/**
	 * If this AggregateSymbol/FunctionSymbol is a generic type then within its code area
	 * either properties, methods or return types it may use another generic type with the same parameters K and V for example.
	 * <p>
	 * So when we come to actually use this generic type class with K=String and V=Float we must also look to replace these
	 * conceptual parameterised type symbols because they would be Item<K, V> and need to be Item<String, Float>.
	 * <p>
	 * A good example is Map<K, V> and MapEntry<K, V> - when you make a Map<String, Float> we replace K->String, V->Float but also
	 * need to replace MapEntry<K, V> with MapEntry<String, Float>. YOu could also imagine a situation where you have to replace
	 * Something<Integer, V> with Something<Integer, Float>!
	 */
	private final List<ParameterisedTypeSymbol> parameterisedTypeReferences = new ArrayList<>();

	/**
	 * Also set up the same but for generic functions.
	 */
	private final List<ParameterisedFunctionSymbol> parameterisedFunctionReferences = new ArrayList<>();

	/**
	 * So this is the list of generic parameters the class/function can accept.
	 * If we were generating a class of A then this would be empty
	 * But if it were a generic type class like 'class B<T>' then this would be set to
	 * Symbol T.
	 * Likewise function zule<K,V> would have K and V symbols in it.
	 * But if class Jaguar<S, T> then this would have S and T a symbols in it.
	 * Note that you need to use ParameterisedTypeSymbol with this and a couple of concrete classes
	 * To have something concrete.
	 */
	private final List<AggregateSymbol> parameterisedTypes = new ArrayList<>();

	/**
	 * Used for parameterised generic types/functions so that we can hang on to the context for phase IR generation
	 * We really do use the code as a template and so need to visit and generate Nodes multiple times but alter the
	 * type of S and T for the concrete types provided.
	 */
	private ParserRuleContext contextForParameterisedType;

	/**
	 * If we encounter an exception within a scope we need to note the line number
	 */
	private Token encounteredExceptionToken = null;

	public ScopedSymbol(String name, IScope enclosingScope)
	{
		super(name);
		actualScope = new LocalScope(ScopeType.BLOCK, name, enclosingScope);
	}

	public ScopedSymbol(IScope.ScopeType scopeType, String scopeName, IScope enclosingScope)
	{
		super(scopeName);
		actualScope = new LocalScope(scopeType, scopeName, enclosingScope);
	}

	public ScopedSymbol(String name, Optional<ISymbol> type, IScope enclosingScope)
	{
		super(name, type);
		actualScope = new LocalScope(ScopeType.BLOCK, name, enclosingScope);
	}

	@Override
	public ScopedSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoScopeSymbol(new ScopedSymbol(this.getName(), withParentAsAppropriate));
	}

	protected ScopedSymbol cloneIntoScopeSymbol(ScopedSymbol newCopy)
	{
		cloneIntoSymbol(newCopy);
		actualScope.cloneIntoLocalScope(newCopy.actualScope);
		newCopy.parameterisedTypeReferences.addAll(parameterisedTypeReferences);
		newCopy.parameterisedFunctionReferences.addAll(parameterisedFunctionReferences);
		newCopy.parameterisedTypes.addAll(parameterisedTypes);
		newCopy.contextForParameterisedType = this.contextForParameterisedType;
		newCopy.encounteredExceptionToken = this.encounteredExceptionToken;

		return newCopy;
	}

	public LocalScope getActualScope()
	{
		return actualScope;
	}

	@Override
	public IScope.ScopeType getScopeType()
	{
		return actualScope.getScopeType();
	}

	@Override
	public List<ParameterisedFunctionSymbol> getParameterisedFunctionReferences()
	{
		return Collections.unmodifiableList(parameterisedFunctionReferences);
	}

	public void addParameterisedFunctionReference(ParameterisedFunctionSymbol parameterisedFunctionReference)
	{
		if(!parameterisedFunctionReferences.contains(parameterisedFunctionReference))
			parameterisedFunctionReferences.add(parameterisedFunctionReference);
	}

	@Override
	public List<ParameterisedTypeSymbol> getParameterisedTypeReferences()
	{
		return Collections.unmodifiableList(parameterisedTypeReferences);
	}

	public void addParameterisedTypeReference(ParameterisedTypeSymbol parameterisedTypeReference)
	{
		//only need to add once but source might have many references to the type.
		if(!parameterisedTypeReferences.contains(parameterisedTypeReference))
		{
			//System.out.println("Added parameterised reference [" + parameterisedTypeReference + "] to  [" + this + "]");
			parameterisedTypeReferences.add(parameterisedTypeReference);
		}
	}

	public ScopedSymbol addParameterisedType(AggregateSymbol parameterisedType)
	{
		AssertValue.checkNotNull("ParameterisedType cannot be null", parameterisedType);
		this.parameterisedTypes.add(parameterisedType);
		return this;
	}

	public ScopedSymbol addParameterisedType(Optional<AggregateSymbol> parameterisedType)
	{
		AssertValue.checkNotNull("Optional parameterisedType cannot be null", parameterisedType);
		parameterisedType.ifPresent(this::addParameterisedType);
		return this;
	}

	protected String getAnyGenericParamsAsFriendlyNames()
	{
		StringBuilder buffer = new StringBuilder();
		if(isGenericInNature())
		{
			buffer.append(" of type ");
			var params = getParameterisedTypes();
			buffer.append(CommonParameterisedTypeDetails.asCommaSeparated(params, params.size() > 1));
		}
		return buffer.toString();
	}

	public List<ISymbol> getAnyGenericParameters()
	{
		return parameterisedTypes.stream().filter(AggregateSymbol::isGenericTypeParameter).collect(Collectors.toList());
	}

	public List<ISymbol> getParameterisedTypes()
	{
		return Collections.unmodifiableList(parameterisedTypes);
	}

	public void setContextForParameterisedType(ParserRuleContext ctx)
	{
		this.contextForParameterisedType = ctx;
	}

	public ParserRuleContext getContextForParameterisedType()
	{
		return contextForParameterisedType;
	}

	@Override
	public boolean isMarkedPure()
	{
		return actualScope.isMarkedPure();
	}

	public void setMarkedPure(boolean markedPure)
	{
		actualScope.setMarkedPure(markedPure);
	}

	@Override
	public boolean isTerminatedNormally()
	{
		return getEncounteredExceptionToken() == null;
	}

	@Override
	public Token getEncounteredExceptionToken()
	{
		return encounteredExceptionToken;
	}

	@Override
	public void setEncounteredExceptionToken(Token encounteredExceptionToken)
	{
		this.encounteredExceptionToken = encounteredExceptionToken;
	}

	@Override
	public String getScopeName()
	{
		return actualScope.getScopeName();
	}

	@Override
	public String getFriendlyScopeName()
	{
		return this.getFriendlyName();
	}

	@Override
	public void define(ISymbol symbol)
	{
		actualScope.define(symbol);
	}

	protected IScope getEnclosingScope()
	{
		return actualScope.getEnclosingScope();
	}

	@Override
	public List<ISymbol> getSymbolsForThisScope()
	{
		return actualScope.getSymbolsForThisScope();
	}

	@Override
	public MethodSymbolSearchResult resolveForAllMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		return actualScope.resolveForAllMatchingMethods(search, result);
	}

	@Override
	public MethodSymbolSearchResult resolveForAllMatchingMethodsInThisScopeOnly(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		return actualScope.resolveForAllMatchingMethodsInThisScopeOnly(search, result);
	}

	@Override
	public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search)
	{
		return actualScope.resolveInThisScopeOnly(search);
	}

	@Override
	public Optional<ISymbol> resolve(SymbolSearch search)
	{
		return actualScope.resolve(search);
	}

	@Override
	public Optional<ScopedSymbol> findNearestAggregateScopeInEnclosingScopes()
	{
		if(getScopeType().equals(ScopeType.AGGREGATE))
			return Optional.of(this);
		return actualScope.findNearestAggregateScopeInEnclosingScopes();
	}

	@Override
	public boolean isScopeAMatchForEnclosingScope(IScope toCheck)
	{
		return actualScope.isScopeAMatchForEnclosingScope(toCheck);
	}
}

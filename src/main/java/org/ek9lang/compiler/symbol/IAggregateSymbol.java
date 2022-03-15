package org.ek9lang.compiler.symbol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

import java.util.List;
import java.util.Optional;

public interface IAggregateSymbol extends ICanCaptureVariables, IScopedSymbol
{
	/**
	 * The module scope this aggregate has been defined in.
	 *
	 * @return The module scope.
	 */
	IScope getModuleScope();

	/**
	 * What sort of scope is this aggregate
	 */
	ScopeType getScopeType();

	/**
	 * Is this aggregate a dispatcher or just a normal class component whatever.
	 *
	 * @return true if marked as a dispatcher.
	 */
	boolean isMarkedAsDispatcher();

	Optional<String> getPipeSinkType();

	/**
	 * To get a full hierarchy you will need to get these subclasses and then get the subclasses of those.
	 *
	 * @return a list of all the subclasses of this class
	 */
	List<IAggregateSymbol> getSubAggregateScopedSymbols();

	/**
	 * used to add back pointers to subclasses.
	 *
	 * @param sub The sub-class to point back to.
	 */
	void addSubAggregateScopedSymbol(IAggregateSymbol sub);

	/**
	 * Get all methods marked as abstract in this or any supers.
	 *
	 * @return The list.
	 */
	List<MethodSymbol> getAllAbstractMethods();

	/**
	 * Get all methods not marked as abstract in this or any supers.
	 *
	 * @return The list.
	 */
	List<MethodSymbol> getAllNonAbstractMethods();

	/**
	 * A list of all the defined constructors.
	 *
	 * @return The list of constructors
	 */
	List<MethodSymbol> getConstructors();

	/**
	 * Get all methods in this scope only that are not abstract.
	 *
	 * @return The list
	 */
	List<MethodSymbol> getAllNonAbstractMethodsInThisScopeOnly();

	/**
	 * Just access to the properties in this aggregate - no supers.
	 *
	 * @return The list
	 */
	List<ISymbol> getProperties();

	/**
	 * Name of this aggregate.
	 */
	String getName();

	boolean isOpenForExtension();

	void setOpenForExtension(boolean open);

	/**
	 * Can be mandated to be virtual even if all methods are implemented.
	 *
	 * @return true if forced to be virtual.
	 */
	boolean isVirtual();

	void setVirtual(boolean virtual);

	/**
	 * Resolve for matching methods and add matches to result.
	 * Idea is to be able to gather all these up and ensure only one single good result i.e.
	 * matching methods does exist and one single method matches best. Else ambiguity or no match.
	 */
	MethodSymbolSearchResult resolveForAllMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result);

	Optional<ISymbol> resolveMember(SymbolSearch search);

	List<ISymbol> getParameterisedTypes();

	void setSuperAggregateScopedSymbol(Optional<IAggregateSymbol> superAggregateScopedSymbol);

	void setSuperAggregateScopedSymbol(IAggregateSymbol superAggregateScopedSymbol);

	Optional<IAggregateSymbol> getSuperAggregateScopedSymbol();

	List<IAggregateSymbol> getTraits();

	List<AggregateWithTraitsSymbol> getAllExtensionConstrainedTraits();

	List<AggregateWithTraitsSymbol> getAllTraits();

	boolean isExtensionConstrained();

	/**
	 * Only really used by aggregates that can have one or more traits.
	 */
	boolean isTraitImplemented(AggregateWithTraitsSymbol thisTraitSymbol);

	/**
	 * Used when a symbol can be defined as a generic/parameterised type.
	 */
	ParserRuleContext getContextForParameterisedType();

	String getAggregateDescription();

	ISymbol clone(IScope withParentAsAppropriate);
}

package org.ek9lang.compiler.symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;

/**
 * Interface for an aggregate, typically a class or something like that.
 */
public interface IAggregateSymbol extends ICanBeGeneric, IScopedSymbol {

  /**
   * What sort of scope is this aggregate.
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
   * To get a full hierarchy you will need to get these subclasses
   * and then get the subclasses of those.
   *
   * @return a list of all the subclasses of this class
   */
  List<IAggregateSymbol> getSubAggregateSymbols();

  /**
   * used to add back pointers to subclasses.
   *
   * @param sub The sub-class to point back to.
   */
  void addSubAggregateSymbol(final IAggregateSymbol sub);

  /**
   * Get all methods on this and any supers or traits.
   *
   * @return the list
   */
  List<MethodSymbol> getAllMethods();

  /**
   * Get all operators on this and any supers or traits.
   *
   * @return the list
   */
  List<MethodSymbol> getAllOperators();

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
   * All methods abstract and non-abstract in this scope.
   */
  List<MethodSymbol> getAllMethodInThisScopeOnly();

  /**
   * Get all methods in this scope only that are not abstract.
   *
   * @return The list
   */
  List<MethodSymbol> getAllNonAbstractMethodsInThisScopeOnly();

  /**
   * Get all methods in this scope only that are abstract.
   *
   * @return The list
   */
  List<MethodSymbol> getAllAbstractMethodsInThisScopeOnly();

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

  /**
   * Resolve for matching methods and add matches to result.
   * Idea is to be able to gather all these up and ensure only one single good result i.e.
   * matching methods does exist and one single method matches best. Else ambiguity or no match.
   */
  MethodSymbolSearchResult resolveMatchingMethods(final MethodSymbolSearch search,
                                                  final MethodSymbolSearchResult result);

  /**
   * Just try and resolve a member in this or super scopes.
   */
  Optional<ISymbol> resolveMember(final SymbolSearch search);

  Optional<IAggregateSymbol> getSuperAggregate();

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  void setSuperAggregate(final Optional<IAggregateSymbol> superAggregate);

  void setSuperAggregate(final IAggregateSymbol superAggregateSymbol);

  boolean isInAggregateHierarchy(final IAggregateSymbol theAggregateToCheck);

  default List<IAggregateSymbol> getTraits() {
    return new ArrayList<>();
  }

  default List<AggregateWithTraitsSymbol> getAllExtensionConstrainedTraits() {

    return new ArrayList<>();
  }

  List<AggregateWithTraitsSymbol> getAllTraits();

  default boolean isExtensionConstrained() {

    return false;
  }

  /**
   * Only really used by aggregates that can have one or more traits.
   * But also super classes or super traits.
   * So can either be implementing directly, super, super - super or traits and supe traits.
   */
  boolean isImplementingInSomeWay(final IAggregateSymbol aggregate);

  String getAggregateDescription();

  IScopedSymbol clone(final IScope withParentAsAppropriate);
}

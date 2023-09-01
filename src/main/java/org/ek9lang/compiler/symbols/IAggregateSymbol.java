package org.ek9lang.compiler.symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;

/**
 * Interface for an aggregate, typically a class or something like that.
 */
public interface IAggregateSymbol extends ICanBeGeneric, IScopedSymbol {
  /**
   * The module scope this aggregate has been defined in.
   *
   * @return The module scope.
   */
  IScope getModuleScope();

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
  void addSubAggregateSymbol(IAggregateSymbol sub);

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
  MethodSymbolSearchResult resolveMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result);

  Optional<ISymbol> resolveMember(SymbolSearch search);

  Optional<IAggregateSymbol> getSuperAggregateSymbol();

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  void setSuperAggregateSymbol(Optional<IAggregateSymbol> superAggregateSymbol);

  void setSuperAggregateSymbol(IAggregateSymbol superAggregateSymbol);

  boolean isInAggregateHierarchy(IAggregateSymbol theAggregateToCheck);

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
  boolean isImplementingInSomeWay(IAggregateSymbol aggregate);

  /**
   * Used when a symbol can be defined as a generic/parameterised type.
   */
  ParserRuleContext getContextForParameterisedType();

  String getAggregateDescription();

  IScopedSymbol clone(IScope withParentAsAppropriate);
}

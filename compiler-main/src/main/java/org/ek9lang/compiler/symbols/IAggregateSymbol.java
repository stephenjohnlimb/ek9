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
  default boolean isMarkedAsDispatcher() {
    return false;
  }

  default void setMarkedAsDispatcher(final boolean markedAsDispatcher) {
    //no operation
  }

  default Optional<String> getPipeSinkType() {
    return Optional.empty();
  }

  /**
   * To get a full hierarchy you will need to get these subclasses
   * and then get the subclasses of those.
   *
   * @return a list of all the subclasses of this class
   */
  default List<IAggregateSymbol> getSubAggregateSymbols() {
    return List.of();
  }

  /**
   * used to add back pointers to subclasses.
   *
   * @param sub The subclass to point back to.
   */
  default void addSubAggregateSymbol(final IAggregateSymbol sub) {
    //No operation
  }

  /**
   * Gets all methods that are effective, by this I mean
   * if supers (classes or traits) have the same method name
   * but, it has been overridden then we would only retain the
   * 'overridden' one as that has taken effect.
   * In the case of a trait that has multiple traits where the same method
   * has been overridden in the trait only one method is now in effect.
   * However, if there are multiple traits (or a super has the same method name)
   * we would end up with 'duplicate' methods of the same name in this list
   * 'CheckConflictingMethods' will need to detect this.
   *
   * @return A List of methods that are in effect.
   */
  default List<MethodSymbol> getAllEffectiveMethods() {
    return List.of();
  }

  /**
   * Get all methods on this and any supers or traits.
   *
   * @return the list
   */
  default List<MethodSymbol> getAllMethods() {
    return List.of();
  }

  /**
   * Get all operators on this and any supers or traits.
   *
   * @return the list
   */
  default List<MethodSymbol> getAllOperators() {
    return List.of();
  }

  /**
   * Get all methods marked as abstract in this or any supers.
   *
   * @return The list.
   */
  default List<MethodSymbol> getAllAbstractMethods() {
    return List.of();
  }

  /**
   * Get all methods not marked as abstract in this or any supers.
   *
   * @return The list.
   */
  default List<MethodSymbol> getAllNonAbstractMethods() {
    return List.of();
  }

  /**
   * A list of all the defined constructors.
   *
   * @return The list of constructors
   */
  List<MethodSymbol> getConstructors();

  /**
   * All methods abstract and non-abstract in this scope.
   */
  default List<MethodSymbol> getAllMethodInThisScopeOnly() {
    return List.of();
  }

  /**
   * Get all methods in this scope only that are not abstract.
   *
   * @return The list
   */
  default List<MethodSymbol> getAllNonAbstractMethodsInThisScopeOnly() {
    return List.of();
  }

  /**
   * Get all methods in this scope only that are abstract.
   *
   * @return The list
   */
  default List<MethodSymbol> getAllAbstractMethodsInThisScopeOnly() {
    return List.of();
  }

  /**
   * Just access to the properties in this aggregate - no supers.
   *
   * @return The list
   */
  default List<ISymbol> getProperties() {
    return List.of();
  }

  /**
   * Name of this aggregate.
   */
  String getName();

  /**
   * Resolve for matching methods and add matches to result.
   * Idea is to be able to gather all these up and ensure only one single good result i.e.
   * matching methods does exist and one single method matches best. Else ambiguity or no match.
   */
  default MethodSymbolSearchResult resolveMatchingMethods(final MethodSymbolSearch search,
                                                          final MethodSymbolSearchResult result) {
    return result;
  }

  /**
   * Just try and resolve a member in this or super scopes.
   */
  default Optional<ISymbol> resolveMember(final SymbolSearch search) {
    return Optional.empty();
  }

  default Optional<IAggregateSymbol> getSuperAggregate() {
    return Optional.empty();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default void setSuperAggregate(final Optional<IAggregateSymbol> superAggregate) {
    //No operation
  }

  default void setSuperAggregate(final IAggregateSymbol superAggregateSymbol) {
    //No Operation
  }

  boolean isInAggregateHierarchy(final IAggregateSymbol theAggregateToCheck);

  default List<IAggregateSymbol> getTraits() {
    return new ArrayList<>();
  }

  default List<AggregateWithTraitsSymbol> getAllExtensionConstrainedTraits() {

    return new ArrayList<>();
  }

  default List<AggregateWithTraitsSymbol> getAllTraits() {
    return List.of();
  }

  default boolean isExtensionConstrained() {

    return false;
  }

  /**
   * Only really used by aggregates that can have one or more traits.
   * But also super classes or super traits.
   * So can either be implementing directly, super, super - super or traits and supe traits.
   */
  default boolean isImplementingInSomeWay(final IAggregateSymbol aggregate) {
    return aggregate == this;
  }

  String getAggregateDescription();

  IScopedSymbol clone(final IScope withParentAsAppropriate);
}

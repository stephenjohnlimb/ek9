package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.core.AssertValue;

/**
 * An aggregate, but one that can have zero or more traits (like interfaces).
 */
public class AggregateWithTraitsSymbol extends AggregateSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The range of traits this aggregate extends/implements.
   */
  private final List<AggregateWithTraitsSymbol> traits = new ArrayList<>();

  /**
   * The range of aggregates that are allowed to extend/implement this aggregate.
   * Currently, used in a trait definition to limit the classes that can implement it.
   * But could clearly be used elsewhere in the future.
   */
  private final List<IAggregateSymbol> allowOnly = new ArrayList<>();

  public AggregateWithTraitsSymbol(final String name, final IScope enclosingScope) {

    super(name, enclosingScope);

  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public AggregateWithTraitsSymbol(final String name, final Optional<ISymbol> type, final IScope enclosingScope) {

    super(name, type, enclosingScope);

  }


  protected AggregateWithTraitsSymbol cloneIntoAggregateWithTraitsSymbol(final AggregateWithTraitsSymbol newCopy) {

    super.cloneIntoAggregateSymbol(newCopy);
    newCopy.traits.addAll(traits);
    newCopy.allowOnly.addAll(allowOnly);

    return newCopy;
  }

  /**
   * mark this aggregate as having additional 'trait'.
   */
  public void addTrait(final AggregateWithTraitsSymbol traitSymbol) {

    AssertValue.checkNotNull("Trait cannot be null", traitSymbol);
    traits.add(traitSymbol);
    //Do we want this to be known as a sub aggregate?
    traitSymbol.addSubAggregateSymbol(this);

  }

  /**
   * Does this aggregate have the trait passed in as an immediate trait.
   * i.e. one it implements itself.
   * Note this is different from being compatible with the trait via hierarchy!
   *
   * @param trait The trait to check.
   * @return true if it does false otherwise.
   */
  public boolean hasImmediateTrait(final IAggregateSymbol trait) {

    return traits.stream().anyMatch(ownTrait -> ownTrait.isExactSameType(trait));
  }

  /**
   * For use with constraining types to be limited to a declared set.
   */
  public void addAllowedExtender(final IAggregateSymbol extenderSymbol) {

    AssertValue.checkNotNull("AllowedExtender cannot be null", extenderSymbol);
    allowOnly.add(extenderSymbol);

  }

  public List<IAggregateSymbol> getAllowedExtenders() {

    return new ArrayList<>(allowOnly);
  }

  /**
   * Is the extender allowed to implement this aggregate (normally a trait).
   *
   * @param extenderSymbol The class (normally the one we are testing)
   * @return true if extending is allowed.
   */
  public boolean isAllowingExtensionBy(final IAggregateSymbol extenderSymbol) {

    if (!isExtensionConstrained()) {
      return true;
    }

    return allowOnly.contains(extenderSymbol);
  }

  private Optional<ISymbol> resolveInTraits(final SymbolSearch search) {

    Optional<ISymbol> rtn = super.resolveMember(search);
    for (AggregateWithTraitsSymbol trait : traits) {
      rtn = trait.resolveMember(search);
      if (rtn.isPresent()) {
        return rtn;
      }
    }

    return rtn;
  }

  @Override
  public List<ISymbol> getAllSymbolsMatchingName(final String symbolName) {

    final List<List<ISymbol>> toBeFlattened = new ArrayList<>();
    toBeFlattened.add(super.getAllSymbolsMatchingName(symbolName));

    for (AggregateWithTraitsSymbol trait : traits) {
      toBeFlattened.add(trait.getAllSymbolsMatchingName(symbolName));
    }

    return toBeFlattened.stream()
        .flatMap(Collection::stream)
        .toList();
  }

  @Override
  public AggregateWithTraitsSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoAggregateWithTraitsSymbol(
        new AggregateWithTraitsSymbol(this.getName(), this.getType(), withParentAsAppropriate));
  }

  @Override
  public double getUnCoercedAssignableWeightTo(final ISymbol s) {

    //easy if same type and parameterization
    double canAssign = super.getUnCoercedAssignableWeightTo(s);
    if (canAssign >= 0.0) {
      return canAssign;
    }

    //if not then we have to cycle through the traits.
    for (IAggregateSymbol trait : traits) {
      canAssign = trait.getUnCoercedAssignableWeightTo(s);
      //Add some weight a bit more because it is a trait
      if (canAssign >= 0.0) {
        return 0.05 + canAssign;
      }
    }
    return NOT_ASSIGNABLE;
  }

  @Override
  public double getAssignableWeightTo(final ISymbol s) {

    //easy if same type and parameterization
    double canAssign = super.getAssignableWeightTo(s);
    if (canAssign >= 0.0) {
      return canAssign;
    }

    //if not then we have to cycle through the traits.
    for (IAggregateSymbol trait : traits) {
      canAssign = trait.getAssignableWeightTo(s);
      //Add some weight a bit more because it is a trait
      if (canAssign >= 0.0) {
        return 0.05 + canAssign;
      }
    }
    return NOT_ASSIGNABLE;
  }

  @Override
  public boolean isImplementingInSomeWay(final IAggregateSymbol traitSymbol) {

    if (this == traitSymbol) {
      return true;
    }

    for (AggregateWithTraitsSymbol trait : traits) {
      if (trait.isImplementingInSomeWay(traitSymbol)) {
        return true;
      }
    }

    return super.isImplementingInSomeWay(traitSymbol);
  }

  /**
   * Get the traits that this aggregates implements.
   *
   * @return The list of traits.
   */
  @Override
  public List<IAggregateSymbol> getTraits() {

    return new ArrayList<>(traits);
  }

  private List<String> getFullyQualifiedTraitNames() {
    return traits.stream().map(IAggregateSymbol::getFullyQualifiedName).toList();
  }

  /**
   * Gets all traits this class implements directly or indirectly through supers that have
   * constraints upon them.
   * The list of constraining traits - no implication or check on what
   * they are constrained to - that's up to the caller.
   */
  @Override
  public List<AggregateWithTraitsSymbol> getAllExtensionConstrainedTraits() {

    return getAllTraits().stream().filter(AggregateWithTraitsSymbol::isExtensionConstrained)
        .toList();
  }

  /**
   * Get all the traits this aggregate implements - this means all the traits any of these
   * traits extends.
   * But also going up the class supers and getting those traits and any of the traits they extend.
   */
  @Override
  public List<AggregateWithTraitsSymbol> getAllTraits() {

    final List<AggregateWithTraitsSymbol> rtn = new ArrayList<>();
    //Add our traits in and those the trait extends - this will then go all the way up the
    //extending list
    traits.forEach(trait -> {
      if (!rtn.contains(trait)) {
        rtn.add(trait);
        addTraitsIfNotPresent(rtn, trait.getAllTraits());
      }
    });
    addTraitsIfNotPresent(rtn, super.getAllTraits());

    return rtn;
  }

  @Override
  public List<MethodSymbol> getAllEffectiveMethods() {

    final List<MethodSymbol> rtn = new ArrayList<>(getAllMethodInThisScopeOnly());

    final List<MethodSymbol> additionalMethods = new ArrayList<>();

    //Now we must treat any super as being on a par with a trait and deal with those
    //possible conflicting methods.
    getSuperAggregate().ifPresent(
        superAggregateSymbol -> superAggregateSymbol.getAllEffectiveMethods().forEach(method -> {
          if (methodNotPresent(rtn, method)) {
            additionalMethods.add(method);
          }
        }));

    //Now must turn to each of the traits in turn, if method is not in list
    //then add to the list of trait methods, we could end up with duplicates
    //if the EK9 developer has defined the same method signature multiple times
    //then combined those traits together.
    traits.forEach(trait -> trait.getAllEffectiveMethods().forEach(method -> {
      if (methodNotPresent(rtn, method)) {
        additionalMethods.add(method);
      }
    }));

    //Only now do we have a set of hopefully unique methods, but if the EK9 developer
    //has not overridden then there will be duplicates.
    rtn.addAll(additionalMethods);

    return rtn;
  }

  @Override
  public List<MethodSymbol> getAllMethods() {

    final List<MethodSymbol> rtn = super.getAllMethods();
    traits.forEach(trait -> rtn.addAll(trait.getAllMethods()));

    return rtn;
  }

  /**
   * Gets all abstract methods in this aggregate and any super classes.
   *
   * @return A list of all the methods marked as abstract.
   */
  @Override
  public List<MethodSymbol> getAllAbstractMethods() {

    List<MethodSymbol> rtn = super.getAllAbstractMethods();
    traits.forEach(trait -> rtn.addAll(trait.getAllAbstractMethods()));

    return rtn;
  }

  @Override
  public boolean isExtensionConstrained() {

    return !allowOnly.isEmpty();
  }

  @Override
  public List<MethodSymbol> getAllNonAbstractMethods() {

    final List<MethodSymbol> rtn = super.getAllNonAbstractMethods();
    for (IAggregateSymbol trait : traits) {
      rtn.addAll(trait.getAllNonAbstractMethods());
    }

    return rtn;
  }

  @Override
  public MethodSymbolSearchResult resolveMatchingMethods(final MethodSymbolSearch search,
                                                         final MethodSymbolSearchResult result) {

    var buildResult = new MethodSymbolSearchResult(result);

    //Do traits first then do own and our supers - so with the traits we consider them 'peers'
    //and so need to merge the result.
    //So this would give us potentially two methods with same signature in our results -
    //which is what we need to see.
    for (IAggregateSymbol trait : traits) {
      buildResult = buildResult.mergePeerToNewResult(
          trait.resolveMatchingMethods(search, new MethodSymbolSearchResult()));
    }
    //But now we need to find the results for our own scope and classes we inherit from
    //But the key here is that when this scope provides an implementation it overrides zero,
    //one or more implementations from the traits

    //No sure it this is right actually the super class is really a peer with the trait
    //in terms of method resolution
    buildResult = buildResult.overrideToNewResult(
        super.resolveMatchingMethods(search, new MethodSymbolSearchResult()));

    return buildResult;
  }

  @Override
  public Optional<ISymbol> resolveMember(final SymbolSearch search) {

    //First check if in immediate class or super class
    Optional<ISymbol> rtn = super.resolveMember(search);
    if (rtn.isEmpty()) {
      rtn = resolveInTraits(search);
    }

    return rtn;
  }

  /**
   * Resolve using super aggregate and or any traits we have.
   * Look in our traits first then go to the normal AggregateSymbol resolution.
   */
  @Override
  public Optional<ISymbol> resolveWithParentScope(final SymbolSearch search) {

    Optional<ISymbol> rtn = resolveInTraits(search);
    if (rtn.isPresent()) {
      return rtn;
    }

    return super.resolveWithParentScope(search);
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }

    return (o instanceof AggregateWithTraitsSymbol that)
        && super.equals(o)
        && getTraits().equals(that.getTraits())
        && allowOnly.equals(that.allowOnly);
  }

  @Override
  public int hashCode() {

    int result = super.hashCode();
    result = 31 * result + getFullyQualifiedTraitNames().hashCode();
    result = 31 * result + allowOnly.hashCode();

    return result;
  }
}

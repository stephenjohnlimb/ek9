package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.SymbolMatcher;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * This is typically a 'class' or an interface type where it can include the definitions of new
 * properties. These can then be made accessible via the symbol table to its
 * methods.
 * But note there is also a single super 'AggregateSymbol' that maps to a
 * super class so here the resolve can access variables in the super class.
 * i.e. we only support single inheritance - but see AggregateWithTraitsSymbol for 'traits' support.
 * I've also added in a mechanism to parameterize the aggregate like Java generics.
 * In general the resolution would be able to resolve variables in the enclosing
 * scope because that will be module level in EK9, there are new global types
 * and global constants that can appear to exist in the package (module) level
 * scope.
 * So for example it is possible to reference a 'constant' or a 'type' that is
 * defined in the same module or if another module is drawn in through
 * 'references'.
 * In reality (when creating the Java code) the constants and types can just be
 * public statics on a class. For example module "the.mod" that defines Constants
 * could have a class called the.mod._Constants with public final statics
 * defined.
 * So there are two ways to resolve references first is module scope and second
 * is super class type scope.
 */
public class AggregateSymbol extends PossibleGenericSymbol implements IAggregateSymbol, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Also keep a back pointer to the direct subclasses.
   * This is really useful for analysing a class or a trait.
   */
  private final List<IAggregateSymbol> subAggregateSymbols = new ArrayList<>();
  /**
   * Might be null if a base 'class' or 'interface', basically the 'super'.
   */

  private IAggregateSymbol superAggregate;
  /**
   * Just used in commented output really - just handy to understand the origin of the aggregate
   * as some are synthetic.
   */
  private String aggregateDescription;
  /**
   * If there is a method that acts as a dispatcher then this aggregate is also a dispatcher.
   */
  private boolean markedAsDispatcher = false;

  /**
   * Can this aggregate be injected by IOC/DI.
   */
  private boolean injectable = false;

  /**
   * When used in pipeline processing, what is the type this aggregate could
   * support to receive types.
   */
  private String pipeSinkType;

  /**
   * When used in pipeline processing, what is the type this aggregate could
   * support to create types.
   */
  private String pipeSourceType;

  /**
   * A simple straight forward aggregate type, like a class or record.
   */
  public AggregateSymbol(final String name, final IScope enclosingScope) {

    this(name, Optional.empty(), enclosingScope);

  }

  /**
   * A simple straight forward aggregate type, like a class or record.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public AggregateSymbol(final String name, final Optional<ISymbol> type, final IScope enclosingScope) {

    super(name, type, enclosingScope);
    //But note that - this could become a TEMPLATE_TYPE if parameterized with a type.
    super.setCategory(SymbolCategory.TYPE);
    super.setGenus(SymbolGenus.CLASS);
    //Also note this could become a DYNAMIC_BLOCK if employed as a dynamic class
    super.setScopeType(ScopeType.NON_BLOCK);
    super.setProduceFullyQualifiedName(true);

  }

  /**
   * An aggregate that can be parameterised, i.e. like a 'List of T'
   * So the name would be 'List' and the parameterTypes would be a single aggregate of a
   * conceptual T.
   */
  public AggregateSymbol(final String name, final IScope enclosingScope, final List<ISymbol> typeParameterOrArguments) {

    this(name, enclosingScope);
    typeParameterOrArguments.forEach(this::addTypeParameterOrArgument);

  }

  @Override
  public AggregateSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoAggregateSymbol(new AggregateSymbol(this.getName(), this.getType(), withParentAsAppropriate));
  }

  protected AggregateSymbol cloneIntoAggregateSymbol(final AggregateSymbol newCopy) {

    super.cloneIntoPossibleGenericSymbol(newCopy);
    if (aggregateDescription != null) {
      newCopy.aggregateDescription = aggregateDescription;
    }


    getSuperAggregate().ifPresent(aggregateSymbol -> newCopy.superAggregate = aggregateSymbol);

    newCopy.markedAsDispatcher = markedAsDispatcher;
    newCopy.subAggregateSymbols.addAll(getSubAggregateSymbols());
    newCopy.injectable = injectable;
    newCopy.pipeSinkType = pipeSinkType;
    newCopy.pipeSourceType = pipeSourceType;

    return newCopy;
  }

  @Override
  public void setName(final String name) {

    super.setName(name);
    getSuperAggregate().ifPresent(superSymbol -> superSymbol.addSubAggregateSymbol(this));

  }

  @Override
  public void define(final ISymbol symbol) {

    if (symbol instanceof VariableSymbol variableSymbol) {
      variableSymbol.setAggregatePropertyField(true);
      variableSymbol.setPrivate(!getGenus().equals(SymbolGenus.RECORD));
    }

    super.define(symbol);
  }

  @Override
  public Optional<IScope> getAnySuperTypeOrFunction() {

    return Optional.ofNullable(superAggregate);
  }

  /**
   * Provides a friendly name of this aggregate that could be presented to the developer.
   *
   * @return The friendly name - especially useful for anonymous dynamic types.
   */
  @Override
  public String getFriendlyName() {

    return doGetFriendlyName();
  }

  /**
   * Typically used for synthetically generated aggregates.
   *
   * @return A description or the friendly name of no description has been set.
   */
  public String getAggregateDescription() {

    return aggregateDescription != null ? aggregateDescription : getFriendlyName();
  }

  /**
   * Typically used for synthetically generated aggregates.
   */
  public void setAggregateDescription(String aggregateDescription) {

    this.aggregateDescription = aggregateDescription;
  }

  @Override
  public Optional<String> getPipeSinkType() {

    return Optional.ofNullable(pipeSinkType);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setPipeSinkType(final Optional<String> pipeSinkType) {

    pipeSinkType.ifPresentOrElse(s -> this.pipeSinkType = s, () -> this.pipeSinkType = null);
  }

  public Optional<String> getPipeSourceType() {

    return Optional.ofNullable(pipeSourceType);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setPipeSourceType(final Optional<String> pipeSourceType) {

    pipeSourceType.ifPresentOrElse(s -> this.pipeSourceType = s, () -> this.pipeSourceType = null);
  }

  @Override
  public boolean isMarkedAsDispatcher() {

    return markedAsDispatcher;
  }

  @Override
  public void setMarkedAsDispatcher(final boolean markedAsDispatcher) {

    this.markedAsDispatcher = markedAsDispatcher;

  }

  @Override
  public boolean isInjectable() {

    return injectable;
  }

  public void setInjectable(final boolean injectable) {

    this.injectable = injectable;

  }

  @Override
  public List<IAggregateSymbol> getSubAggregateSymbols() {

    return Collections.unmodifiableList(subAggregateSymbols);
  }

  /**
   * We note down any subtypes this type is used with.
   */
  @Override
  public void addSubAggregateSymbol(final IAggregateSymbol sub) {

    if (!subAggregateSymbols.contains(sub)) {
      subAggregateSymbols.add(sub);
    }
  }

  /**
   * Gets all abstract methods in this aggregate and any super classes.
   *
   * @return A list of all the methods marked as abstract.
   */
  @Override
  public List<MethodSymbol> getAllAbstractMethods() {

    final ArrayList<MethodSymbol> rtn = new ArrayList<>();
    getSuperAggregate().ifPresent(aggregateSymbol -> rtn.addAll(aggregateSymbol.getAllAbstractMethods()));
    rtn.addAll(filterMethods(MethodSymbol::isMarkedAbstract));

    return rtn;
  }

  /**
   * Provides access to the properties on this aggregate - but only this aggregate.
   *
   * @return The list of properties.
   */
  @Override
  public List<ISymbol> getProperties() {

    return getSymbolsForThisScope().stream().filter(ISymbol::isVariable).toList();
  }

  @Override
  public List<MethodSymbol> getConstructors() {

    return filterMethods(MethodSymbol::isConstructor);
  }

  @Override
  public List<MethodSymbol> getAllEffectiveMethods() {

    //So first get all the methods in this scope and then only add those from super if
    //present and the method is not already recorded.

    final ArrayList<MethodSymbol> rtn = new ArrayList<>(getAllMethodInThisScopeOnly());

    getSuperAggregate().ifPresent(
        superAggregateSymbol -> superAggregateSymbol.getAllEffectiveMethods().forEach(method -> {
          if (methodNotPresent(rtn, method)) {
            rtn.add(method);
          }
        }));

    return rtn;
  }


  @Override
  public List<MethodSymbol> getAllMethods() {

    final ArrayList<MethodSymbol> rtn = new ArrayList<>();
    getSuperAggregate().ifPresent(aggregateSymbol -> rtn.addAll(aggregateSymbol.getAllMethods()));
    rtn.addAll(getAllMethodInThisScopeOnly());

    return rtn;
  }

  @Override
  public List<MethodSymbol> getAllOperators() {

    return getAllMethods().stream().filter(MethodSymbol::isOperator).toList();
  }

  @Override
  public List<MethodSymbol> getAllNonAbstractMethods() {

    final ArrayList<MethodSymbol> rtn = new ArrayList<>();
    getSuperAggregate().ifPresent(aggregateSymbol -> rtn.addAll(aggregateSymbol.getAllNonAbstractMethods()));
    rtn.addAll(getAllNonAbstractMethodsInThisScopeOnly());

    return rtn;
  }

  @Override
  public List<MethodSymbol> getAllNonAbstractMethodsInThisScopeOnly() {

    return filterMethods(MethodSymbol::isNotMarkedAbstract);
  }

  @Override
  public List<MethodSymbol> getAllAbstractMethodsInThisScopeOnly() {

    return filterMethods(MethodSymbol::isMarkedAbstract);
  }

  @Override
  public List<MethodSymbol> getAllMethodInThisScopeOnly() {

    return getSymbolsForThisScope()
        .stream().filter(ISymbol::isMethod)
        .map(MethodSymbol.class::cast)
        .toList();
  }

  private List<MethodSymbol> filterMethods(final Predicate<MethodSymbol> predicate) {

    return getSymbolsForThisScope().stream().filter(ISymbol::isMethod).map(MethodSymbol.class::cast).filter(predicate)
        .toList();
  }

  @Override
  public boolean isImplementingInSomeWay(IAggregateSymbol aggregate) {

    if (this.equals(aggregate)) {
      return true;
    }

    return getSuperAggregate()
        .map(aggregateSymbol -> aggregateSymbol.isImplementingInSomeWay(aggregate))
        .orElse(false);
  }

  @Override
  public List<AggregateWithTraitsSymbol> getAllTraits() {

    //This has no traits but its super might.
    final List<AggregateWithTraitsSymbol> rtn = new ArrayList<>();
    getSuperAggregate().ifPresent(theSuper -> addTraitsIfNotPresent(rtn, theSuper.getAllTraits()));

    return rtn;
  }

  protected void addTraitsIfNotPresent(final List<AggregateWithTraitsSymbol> exiting,
                                       final List<AggregateWithTraitsSymbol> additions) {

    additions.forEach(trait -> {
      if (!exiting.contains(trait)) {
        exiting.add(trait);
      }
    });

  }

  @Override
  public boolean isExtensionOfInjectable() {

    if (injectable) {
      return true;
    }

    return getSuperAggregate().map(ISymbol::isExtensionOfInjectable).orElse(false);
  }

  private double checkParameterisedTypesAssignable(final List<ISymbol> listA, final List<ISymbol> listB) {

    if (listA.size() != listB.size()) {
      return SymbolMatcher.INVALID_COST;
    }

    double totalCost = SymbolMatcher.ZERO_COST;
    for (int i = 0; i < listA.size(); i++) {
      final var cost = listA.get(i).getAssignableCostTo(listB.get(i));

      //If cost less than zero - then it is not assignable.
      if (cost < SymbolMatcher.ZERO_COST) {
        return cost;
      }

      totalCost += cost;
    }

    return totalCost;
  }

  @SuppressWarnings("checkstyle:Indentation")
  @Override
  public double getUnCoercedAssignableCostTo(final ISymbol s) {

    double totalCost = super.getUnCoercedAssignableCostTo(s);

    if (getSuperAggregate().isPresent() && totalCost < SymbolMatcher.ZERO_COST) {
      //now we can check superclass matches. but add some cost because this did not match
      final var theSuperAggregate = getSuperAggregate().get();
      double cost = theSuperAggregate.getUnCoercedAssignableCostTo(s);
      if (cost < SymbolMatcher.ZERO_COST) {
        return cost;
      }

      //We push the cost of being able to assign to Any down.
      if (theSuperAggregate.getFullyQualifiedName().equals("org.ek9.lang::Any")) {
        return SymbolMatcher.HIGH_COST;
      }
      //If we can assign via its super, then we're done.
      totalCost = SymbolMatcher.SUPER_COST + cost;
      return totalCost;
    }

    if (totalCost >= SymbolMatcher.ZERO_COST && s instanceof AggregateSymbol toCheck) {
      final var parameterisedCost =
          checkParameterisedTypesAssignable(getTypeParameterOrArguments(), toCheck.getTypeParameterOrArguments());
      totalCost += parameterisedCost;
    }

    return totalCost;
  }

  @Override
  public Optional<IAggregateSymbol> getSuperAggregate() {

    return Optional.ofNullable(superAggregate);
  }

  /**
   * Set the 'super' of this type.
   */
  @Override
  public void setSuperAggregate(final Optional<IAggregateSymbol> superAggregate) {

    AssertValue.checkNotNull("Optional superAggregateSymbol cannot be null", superAggregate);
    superAggregate.ifPresentOrElse(theSuper -> this.superAggregate = theSuper,
        () -> this.superAggregate = null);
    getSuperAggregate().ifPresent(aggregateSymbol -> aggregateSymbol.addSubAggregateSymbol(this));

  }

  @Override
  public void setSuperAggregate(final IAggregateSymbol baseSymbol) {

    setSuperAggregate(Optional.ofNullable(baseSymbol));

  }

  public boolean hasImmediateSuper(final IAggregateSymbol theSuper) {

    return getSuperAggregate().map(aggregateSymbol -> aggregateSymbol.isExactSameType(theSuper)).orElse(false);
  }

  /**
   * Does the aggregate passed in exist in this type hierarchy.
   * This does include a check that this is the same aggregate.
   */
  @Override
  public boolean isInAggregateHierarchy(final IAggregateSymbol theAggregateToCheck) {

    if (this.isExactSameType(theAggregateToCheck)) {
      return true;
    }

    if (getSuperAggregate().isEmpty()) {
      return false;
    }

    if (!hasImmediateSuper(theAggregateToCheck)) {
      return getSuperAggregate().get().isInAggregateHierarchy(theAggregateToCheck);
    }

    return true;
  }

  @Override
  public ISymbol setType(final Optional<ISymbol> type) {

    if (super.getType().isPresent()) {
      throw new CompilerException("You cannot set the type of an aggregate Symbol");
    }

    return super.setType(type);
  }

  @Override
  public Optional<ISymbol> getType() {

    //This is also a type
    return Optional.of(this);
  }

  @Override
  public List<ISymbol> getAllSymbolsMatchingName(final String symbolName) {

    final var thisMatchingSymbols = super.getAllSymbolsMatchingName(symbolName);

    if (getSuperAggregate().isPresent()) {
      final var superMatchingSymbols = getSuperAggregate().get().getAllSymbolsMatchingName(symbolName);
      return Stream.of(thisMatchingSymbols, superMatchingSymbols).flatMap(Collection::stream).toList();
    }

    return thisMatchingSymbols;
  }

  @Override
  public MethodSymbolSearchResult resolveMatchingMethods(final MethodSymbolSearch search,
                                                         final MethodSymbolSearchResult result) {

    var buildResult = new MethodSymbolSearchResult(result);

    //Do supers first then do own
    //So if we have a super then this will be a peer of anything we already have passed
    //in could be traits/interfaces

    if (getSuperAggregate().isPresent()) {
      buildResult = buildResult.mergePeerToNewResult(
          getSuperAggregate().get().resolveMatchingMethods(search, new MethodSymbolSearchResult()));
    }

    final var thisScopeResult = resolveMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult());
    buildResult = buildResult.overrideToNewResult(thisScopeResult);

    return buildResult;
  }

  /**
   * Just try and resolve a member in this or super scopes.
   */
  @Override
  public Optional<ISymbol> resolveMember(final SymbolSearch search) {

    Optional<ISymbol> rtn = resolveInThisScopeOnly(search);
    if (rtn.isEmpty() && getSuperAggregate().isPresent()) {
      rtn = getSuperAggregate().get().resolveMember(search);
    }

    return rtn;
  }

  protected boolean methodNotPresent(final List<MethodSymbol> defined, final MethodSymbol checkMethod) {

    return defined.stream()
        .noneMatch(method -> methodsMatch(method, checkMethod));

  }

  protected boolean methodsMatch(final MethodSymbol m1, final MethodSymbol m2) {

    return m1.getName().equals(m2.getName()) && m1.isExactSignatureMatchTo(m2);

  }

  private String doGetFriendlyName() {

    final var mainName = super.getFriendlyName();
    final var additionalGenerics = getAnyGenericParamsAsFriendlyNames();

    return mainName + additionalGenerics;
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }

    return (o instanceof AggregateSymbol that) && super.equals(o)
        && isMarkedAsDispatcher() == that.isMarkedAsDispatcher() && isInjectable() == that.isInjectable()
        && isOpenForExtension() == that.isOpenForExtension() && getSuperAggregate().equals(
        that.getSuperAggregate()) && getAggregateDescription().equals(that.getAggregateDescription())
        && getPipeSinkType().equals(that.getPipeSinkType()) && getPipeSourceType().equals(that.getPipeSourceType());
  }

  @Override
  public int hashCode() {

    int result = super.hashCode();
    result = 31 * result + (getSourceToken() != null ? getSourceToken().hashCode() : 0);
    result = 31 * result + getSuperAggregate().hashCode();
    result = 31 * result + getAggregateDescription().hashCode();
    result = 31 * result + (isMarkedAsDispatcher() ? 1 : 0);
    result = 31 * result + (isInjectable() ? 1 : 0);
    result = 31 * result + (isOpenForExtension() ? 1 : 0);
    result = 31 * result + getPipeSinkType().hashCode();
    result = 31 * result + getPipeSourceType().hashCode();

    return result;
  }
}
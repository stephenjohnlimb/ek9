package org.ek9lang.compiler.symbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

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
public class AggregateSymbol extends PossibleGenericSymbol implements IAggregateSymbol {

  /**
   * Might be null if a base 'class' or 'interface', basically the 'super'.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<IAggregateSymbol> superAggregateSymbol = Optional.empty();

  /**
   * Just used in commented output really - just handy to understand the origin of the aggregate
   * as some are synthetic.
   */
  private String aggregateDescription;

  /**
   * Also keep a back pointer to the direct subclasses.
   * This is really useful for analysing a class or a trait.
   */
  private final List<IAggregateSymbol> subAggregateSymbols = new ArrayList<>();

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
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<String> pipeSinkType = Optional.empty();

  /**
   * When used in pipeline processing, what is the type this aggregate could
   * support to create types.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<String> pipeSourceType = Optional.empty();

  /**
   * A simple straight forward aggregate type, like a class or record.
   */
  public AggregateSymbol(String name, IScope enclosingScope) {
    this(name, Optional.empty(), enclosingScope);
  }

  /**
   * A simple straight forward aggregate type, like a class or record.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public AggregateSymbol(String name, Optional<ISymbol> type, IScope enclosingScope) {
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
  public AggregateSymbol(String name, IScope enclosingScope, List<ISymbol> typeParameterOrArguments) {
    this(name, enclosingScope);
    typeParameterOrArguments.forEach(this::addTypeParameterOrArgument);
  }

  @Override
  public AggregateSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoAggregateSymbol(new AggregateSymbol(this.getName(), this.getType(), withParentAsAppropriate));
  }

  protected AggregateSymbol cloneIntoAggregateSymbol(AggregateSymbol newCopy) {
    super.cloneIntoPossibleGenericSymbol(newCopy);
    if (aggregateDescription != null) {
      newCopy.aggregateDescription = aggregateDescription;
    }

    newCopy.markedAsDispatcher = markedAsDispatcher;

    superAggregateSymbol.ifPresent(aggregateSymbol -> newCopy.superAggregateSymbol = Optional.of(aggregateSymbol));

    newCopy.subAggregateSymbols.addAll(getSubAggregateSymbols());
    newCopy.injectable = injectable;

    pipeSinkType.ifPresent(s -> newCopy.pipeSinkType = Optional.of(s));

    pipeSourceType.ifPresent(s -> newCopy.pipeSourceType = Optional.of(s));

    return newCopy;
  }

  @Override
  public void setName(String name) {
    super.setName(name);
    if (superAggregateSymbol != null) {
      //Might not be even created yet.
      superAggregateSymbol.ifPresent(superSymbol -> superSymbol.addSubAggregateSymbol(this));
    }
  }

  @Override
  public void define(ISymbol symbol) {
    if (symbol instanceof VariableSymbol variableSymbol) {
      variableSymbol.setAggregatePropertyField(true);
      variableSymbol.setPrivate(!getGenus().equals(SymbolGenus.RECORD));
    }
    super.define(symbol);
  }

  @Override
  protected Optional<IScope> getAnySuperTypeOrFunction() {
    if (this.superAggregateSymbol.isPresent()) {
      return Optional.of(superAggregateSymbol.get());
    }
    return Optional.empty();
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

  public Optional<String> getPipeSinkType() {
    return pipeSinkType;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setPipeSinkType(Optional<String> pipeSinkType) {
    this.pipeSinkType = pipeSinkType;
  }

  public Optional<String> getPipeSourceType() {
    return pipeSourceType;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setPipeSourceType(Optional<String> pipeSourceType) {
    this.pipeSourceType = pipeSourceType;
  }

  public boolean isMarkedAsDispatcher() {
    return markedAsDispatcher;
  }

  public void setMarkedAsDispatcher(boolean markedAsDispatcher) {
    this.markedAsDispatcher = markedAsDispatcher;
  }

  @Override
  public boolean isInjectable() {
    return injectable;
  }

  public void setInjectable(boolean injectable) {
    this.injectable = injectable;
  }

  public List<IAggregateSymbol> getSubAggregateSymbols() {
    return Collections.unmodifiableList(subAggregateSymbols);
  }

  /**
   * We note down any subtypes this type is used with.
   */
  public void addSubAggregateSymbol(IAggregateSymbol sub) {
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
    ArrayList<MethodSymbol> rtn = new ArrayList<>();
    superAggregateSymbol.ifPresent(aggregateSymbol -> rtn.addAll(aggregateSymbol.getAllAbstractMethods()));

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
  public List<MethodSymbol> getAllNonAbstractMethods() {
    ArrayList<MethodSymbol> rtn = new ArrayList<>();
    superAggregateSymbol.ifPresent(aggregateSymbol -> rtn.addAll(aggregateSymbol.getAllNonAbstractMethods()));

    rtn.addAll(getAllNonAbstractMethodsInThisScopeOnly());
    return rtn;
  }

  @Override
  public List<MethodSymbol> getAllNonAbstractMethodsInThisScopeOnly() {
    return filterMethods(MethodSymbol::isNotMarkedAbstract);
  }

  private List<MethodSymbol> filterMethods(Predicate<MethodSymbol> predicate) {
    return getSymbolsForThisScope().stream().filter(ISymbol::isMethod).map(MethodSymbol.class::cast).filter(predicate)
        .toList();
  }

  @Override
  public boolean isImplementingInSomeWay(IAggregateSymbol aggregate) {
    if (this.equals(aggregate)) {
      return true;
    }
    return superAggregateSymbol.map(aggregateSymbol -> aggregateSymbol.isImplementingInSomeWay(aggregate))
        .orElse(false);
  }

  @Override
  public List<AggregateWithTraitsSymbol> getAllTraits() {
    //This has no traits but its super might.
    List<AggregateWithTraitsSymbol> rtn = new ArrayList<>();
    getSuperAggregateSymbol().ifPresent(theSuper -> addTraitsIfNotPresent(rtn, theSuper.getAllTraits()));

    return rtn;
  }

  protected void addTraitsIfNotPresent(List<AggregateWithTraitsSymbol> exiting,
                                       List<AggregateWithTraitsSymbol> additions) {
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
    return superAggregateSymbol.map(ISymbol::isExtensionOfInjectable).orElse(false);
  }

  private double checkParameterisedTypesAssignable(List<ISymbol> listA, List<ISymbol> listB) {
    if (listA.size() != listB.size()) {
      return -1.0;
    }

    double canAssign = 0.0;
    for (int i = 0; i < listA.size(); i++) {
      canAssign += listA.get(i).getAssignableWeightTo(listB.get(i));
      if (canAssign < 0.0) {
        return canAssign;
      }
    }

    return canAssign;
  }

  @Override
  public double getUnCoercedAssignableWeightTo(ISymbol s) {
    double canAssign = super.getUnCoercedAssignableWeightTo(s);

    if (superAggregateSymbol.isPresent() && canAssign < 0.0) {
      //now we can check superclass matches. but add some weight because this did not match
      double weight = superAggregateSymbol.get().getUnCoercedAssignableWeightTo(s);
      canAssign = 0.05 + weight;

      if (canAssign < 0.0) {
        return canAssign;
      }
    }

    if (canAssign >= 0.0 && s instanceof AggregateSymbol toCheck) {
      double parameterisedWeight =
          checkParameterisedTypesAssignable(getTypeParameterOrArguments(), toCheck.getTypeParameterOrArguments());
      canAssign += parameterisedWeight;
    }

    return canAssign;
  }

  public Optional<IAggregateSymbol> getSuperAggregateSymbol() {
    return superAggregateSymbol;
  }

  /**
   * Set the 'super' of this type.
   */
  public void setSuperAggregateSymbol(Optional<IAggregateSymbol> superAggregateSymbol) {
    AssertValue.checkNotNull("Optional superAggregateSymbol cannot be null", superAggregateSymbol);
    this.superAggregateSymbol = superAggregateSymbol;
    superAggregateSymbol.ifPresent(aggregateSymbol -> aggregateSymbol.addSubAggregateSymbol(this));
  }

  public void setSuperAggregateSymbol(IAggregateSymbol baseSymbol) {
    setSuperAggregateSymbol(Optional.ofNullable(baseSymbol));
  }

  public boolean hasImmediateSuper(IAggregateSymbol theSuper) {
    return superAggregateSymbol.map(aggregateSymbol -> aggregateSymbol.isExactSameType(theSuper)).orElse(false);
  }

  @Override
  public ISymbol setType(Optional<ISymbol> type) {
    //Used when cloning so type can be set if not already populated.
    if (this.isConceptualTypeParameter() && type.isPresent()) {
      this.setSuperAggregateSymbol((IAggregateSymbol) type.get());
    } else if (super.getType().isPresent()) {
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
    var thisMatchingSymbols = super.getAllSymbolsMatchingName(symbolName);
    if (superAggregateSymbol.isPresent()) {
      var superMatchingSymbols = superAggregateSymbol.get().getAllSymbolsMatchingName(symbolName);
      return Stream.of(thisMatchingSymbols, superMatchingSymbols).flatMap(Collection::stream).toList();
    }

    return thisMatchingSymbols;
  }

  @Override
  public MethodSymbolSearchResult resolveMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result) {
    MethodSymbolSearchResult buildResult = new MethodSymbolSearchResult(result);
    //Do supers first then do own
    //So if we have a super then this will bee a peer of anything we already have passed
    //in could be traits/interfaces

    if (superAggregateSymbol.isPresent()) {
      buildResult = buildResult.mergePeerToNewResult(
          superAggregateSymbol.get().resolveMatchingMethods(search, new MethodSymbolSearchResult()));
    }

    MethodSymbolSearchResult res = resolveMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult());
    buildResult = buildResult.overrideToNewResult(res);

    return buildResult;
  }

  /**
   * Just try and resolve a member in this or super scopes.
   */
  @Override
  public Optional<ISymbol> resolveMember(SymbolSearch search) {

    Optional<ISymbol> rtn = resolveInThisScopeOnly(search);
    if (rtn.isEmpty() && superAggregateSymbol.isPresent()) {
      rtn = superAggregateSymbol.get().resolveMember(search);
    }
    return rtn;
  }

  private String doGetFriendlyName() {
    var mainName = super.getFriendlyName();
    var additionalGenerics = getAnyGenericParamsAsFriendlyNames();
    return mainName + additionalGenerics;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof AggregateSymbol that) && super.equals(o)
        && isMarkedAsDispatcher() == that.isMarkedAsDispatcher() && isInjectable() == that.isInjectable()
        && isOpenForExtension() == that.isOpenForExtension() && getSuperAggregateSymbol().equals(
        that.getSuperAggregateSymbol()) && getAggregateDescription().equals(that.getAggregateDescription())
        && getPipeSinkType().equals(that.getPipeSinkType()) && getPipeSourceType().equals(that.getPipeSourceType());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getSuperAggregateSymbol().hashCode();
    result = 31 * result + getAggregateDescription().hashCode();
    result = 31 * result + (isMarkedAsDispatcher() ? 1 : 0);
    result = 31 * result + (isInjectable() ? 1 : 0);
    result = 31 * result + (isOpenForExtension() ? 1 : 0);
    result = 31 * result + getPipeSinkType().hashCode();
    result = 31 * result + getPipeSourceType().hashCode();
    return result;
  }
}
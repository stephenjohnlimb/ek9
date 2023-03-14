package org.ek9lang.compiler.symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.exception.CompilerException;

/**
 * This is typically a 'class' or an interface type where it can include the definitions of new
 * properties. These can then be made accessible via the symbol table to its
 * methods.
 * But note there is also a single super 'AggregateScopedSymbol' that maps to a
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
public class AggregateSymbol extends ScopedSymbol implements IAggregateSymbol {

  //This is the module this aggregate has been defined in.
  private IScope moduleScope;

  /**
   * Might be null if a base 'class' or 'interface', basically the 'super'.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<IAggregateSymbol> superAggregateScopedSymbol = Optional.empty();

  /**
   * Just used in commented output really - just handy to understand the origin of the aggregate
   * as some are synthetic.
   */
  private String aggregateDescription;

  /**
   * Also keep a back pointer to the direct subclasses.
   * This is really useful for analysing a class or a trait.
   */
  private final List<IAggregateSymbol> subAggregateScopedSymbols = new ArrayList<>();

  /**
   * This is actually a 'T' itself - we will need to know this.
   * So we use an aggregate symbol to model the 'S' and 'T' conceptual types.
   * These are 'synthetic' in nature but do have 'synthetic' operators, so that
   * generic/template classes/functions can 'assume' certain operators and operations.
   * Only when the generic type is actually parameterised with a 'concrete' type does the
   * compiler check that the generic class/function that used the operators can be used with
   * the actual concrete type.
   */
  private boolean genericTypeParameter;

  /**
   * If there is a method that acts as a dispatcher then this aggregate is also a dispatcher.
   */
  private boolean markedAsDispatcher = false;

  /**
   * Was the aggregate marked as abstract in the source code.
   */
  private boolean markedAbstract = false;

  /**
   * Can this aggregate be injected by IOC/DI.
   */
  private boolean injectable = false;

  /**
   * Is this aggregate open to be extended.
   * i.e. is it closed so that not other aggregates can extend it.
   */
  private boolean openForExtension = false;

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
    super.setCategory(SymbolCategory.TYPE);
    super.setScopeType(ScopeType.NON_BLOCK);
    super.setProduceFullyQualifiedName(true);
  }

  /**
   * An aggregate that can be parameterised, i.e. like a 'List of T'
   * So the name would be 'List' and the parameterTypes would be a single aggregate of a
   * conceptual T.
   */
  public AggregateSymbol(String name, IScope enclosingScope, List<AggregateSymbol> parameterTypes) {
    this(name, enclosingScope);
    parameterTypes.forEach(this::addParameterisedType);
  }

  @Override
  public AggregateSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoAggregateSymbol(
        new AggregateSymbol(this.getName(), this.getType(), withParentAsAppropriate));
  }

  protected AggregateSymbol cloneIntoAggregateSymbol(AggregateSymbol newCopy) {
    super.cloneIntoScopeSymbol(newCopy);
    newCopy.moduleScope = moduleScope;
    if (aggregateDescription != null) {
      newCopy.aggregateDescription = aggregateDescription;
    }

    newCopy.genericTypeParameter = genericTypeParameter;
    newCopy.markedAsDispatcher = markedAsDispatcher;

    superAggregateScopedSymbol.ifPresent(
        aggregateSymbol -> newCopy.superAggregateScopedSymbol = Optional.of(aggregateSymbol));

    newCopy.subAggregateScopedSymbols.addAll(subAggregateScopedSymbols);
    newCopy.markedAbstract = markedAbstract;
    newCopy.injectable = injectable;
    newCopy.openForExtension = openForExtension;

    pipeSinkType.ifPresent(s -> newCopy.pipeSinkType = Optional.of(s));

    pipeSourceType.ifPresent(s -> newCopy.pipeSourceType = Optional.of(s));

    return newCopy;
  }

  @Override
  protected Optional<IScope> getAnySuperTypeOrFunction() {
    if (this.superAggregateScopedSymbol.isPresent()) {
      return Optional.of(superAggregateScopedSymbol.get());
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

  public boolean isOpenForExtension() {
    return this.openForExtension;
  }

  public void setOpenForExtension(boolean open) {
    this.openForExtension = open;
  }

  public boolean isMarkedAsDispatcher() {
    return markedAsDispatcher;
  }

  public void setMarkedAsDispatcher(boolean markedAsDispatcher) {
    this.markedAsDispatcher = markedAsDispatcher;
  }

  @Override
  public boolean isMarkedAbstract() {
    return markedAbstract;
  }

  public void setMarkedAbstract(boolean markedAbstract) {
    this.markedAbstract = markedAbstract;
  }

  @Override
  public boolean isInjectable() {
    return injectable;
  }

  public void setInjectable(boolean injectable) {
    this.injectable = injectable;
  }

  /**
   * Is this aggregate itself a generic type that has been used as a parameter.
   */
  @Override
  public boolean isGenericTypeParameter() {
    return genericTypeParameter;
  }

  /**
   * Set this aggregate to be a generic type parameter.
   */
  public void setGenericTypeParameter(boolean genericTypeParameter) {
    this.genericTypeParameter = genericTypeParameter;
  }

  /**
   * Make this a template/generic type by adding one or more Parameterised types.
   * This now becomes a TEMPLATE_TYPE, rather than just a plain TYPE.
   */
  @Override
  public void addParameterisedType(AggregateSymbol parameterisedType) {
    super.addParameterisedType(parameterisedType);
    super.setCategory(SymbolCategory.TEMPLATE_TYPE);
  }

  public List<IAggregateSymbol> getSubAggregateScopedSymbols() {
    return subAggregateScopedSymbols;
  }

  /**
   * We note down any subtypes this type is used with.
   */
  public void addSubAggregateScopedSymbol(IAggregateSymbol sub) {
    if (!subAggregateScopedSymbols.contains(sub)) {
      subAggregateScopedSymbols.add(sub);
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
    superAggregateScopedSymbol.ifPresent(
        aggregateSymbol -> rtn.addAll(aggregateSymbol.getAllAbstractMethods()));

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
    superAggregateScopedSymbol.ifPresent(
        aggregateSymbol -> rtn.addAll(aggregateSymbol.getAllNonAbstractMethods()));

    rtn.addAll(getAllNonAbstractMethodsInThisScopeOnly());
    return rtn;
  }

  public List<ISymbol> getAllPropertyFieldsInThisScopeOnly() {
    return getSymbolsForThisScope().stream().filter(sym -> !sym.isMethod()).toList();
  }

  @Override
  public List<MethodSymbol> getAllNonAbstractMethodsInThisScopeOnly() {
    return filterMethods(MethodSymbol::isNotMarkedAbstract);
  }

  private List<MethodSymbol> filterMethods(Predicate<MethodSymbol> predicate) {
    return getSymbolsForThisScope().stream().filter(ISymbol::isMethod)
        .map(MethodSymbol.class::cast).filter(predicate).toList();
  }

  @Override
  public boolean isImplementingInSomeWay(IAggregateSymbol aggregate) {
    return superAggregateScopedSymbol.map(
        aggregateSymbol -> aggregateSymbol.isImplementingInSomeWay(aggregate)).orElse(false);
  }


  @Override
  public List<AggregateWithTraitsSymbol> getAllExtensionConstrainedTraits() {
    return new ArrayList<>();
  }

  @Override
  public boolean isExtensionConstrained() {
    return false;
  }

  /**
   * Any traits this type has.
   */
  @Override
  public List<IAggregateSymbol> getTraits() {
    return new ArrayList<>();
  }

  @Override
  public List<AggregateWithTraitsSymbol> getAllTraits() {
    //This has no traits but its super might.
    List<AggregateWithTraitsSymbol> rtn = new ArrayList<>();
    getSuperAggregateScopedSymbol().ifPresent(theSuper -> {
      List<AggregateWithTraitsSymbol> superTraits = theSuper.getAllTraits();
      superTraits.forEach(trait -> {
        if (!rtn.contains(trait)) {
          rtn.add(trait);
        }
      });
    });

    return rtn;
  }

  public IScope getModuleScope() {
    return moduleScope;
  }

  public void setModuleScope(IScope moduleScope) {
    this.moduleScope = moduleScope;
  }

  @Override
  public boolean isExtensionOfInjectable() {
    if (injectable) {
      return true;
    }
    return superAggregateScopedSymbol.map(ISymbol::isExtensionOfInjectable).orElse(false);
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
  public double getAssignableWeightTo(ISymbol s) {
    //Here be dragons - looks simple but there are complexities in here with super
    //types and generics might not be able to be directly assignable - but not end of the world!
    //We might get a coercion match so weight might not be 0.0.

    double canAssign = super.getAssignableWeightTo(s);

    //Check if assignable as super
    if (superAggregateScopedSymbol.isPresent() && canAssign < 0.0) {
      //now we can check superclass matches. but add some weight because this did not match
      double weight = superAggregateScopedSymbol.get().getAssignableWeightTo(s);
      canAssign = 0.05 + weight;

      if (canAssign < 0.0) {
        return canAssign;
      }
    }

    if (canAssign >= 0.0 && s instanceof AggregateSymbol toCheck) {
      double parameterisedWeight = checkParameterisedTypesAssignable(getParameterisedTypes(),
          toCheck.getParameterisedTypes());
      canAssign += parameterisedWeight;
    }

    return canAssign;
  }

  @Override
  public double getUnCoercedAssignableWeightTo(ISymbol s) {
    double canAssign = super.getUnCoercedAssignableWeightTo(s);

    if (superAggregateScopedSymbol.isPresent() && canAssign < 0.0) {
      //now we can check superclass matches. but add some weight because this did not match
      double weight = superAggregateScopedSymbol.get().getUnCoercedAssignableWeightTo(s);
      canAssign = 0.05 + weight;

      if (canAssign < 0.0) {
        return canAssign;
      }
    }

    if (canAssign >= 0.0 && s instanceof AggregateSymbol toCheck) {
      double parameterisedWeight = checkParameterisedTypesAssignable(getParameterisedTypes(),
          toCheck.getParameterisedTypes());
      canAssign += parameterisedWeight;
    }

    return canAssign;
  }

  public Optional<IAggregateSymbol> getSuperAggregateScopedSymbol() {
    return superAggregateScopedSymbol;
  }

  /**
   * Set the 'super' of this type.
   */
  public void setSuperAggregateScopedSymbol(Optional<IAggregateSymbol> superAggregateScopedSymbol) {
    AssertValue.checkNotNull("Optional superAggregateScopedSymbol cannot be null",
        superAggregateScopedSymbol);
    this.superAggregateScopedSymbol = superAggregateScopedSymbol;
    superAggregateScopedSymbol.ifPresent(
        aggregateSymbol -> aggregateSymbol.addSubAggregateScopedSymbol(this));
  }

  public void setSuperAggregateScopedSymbol(IAggregateSymbol baseSymbol) {
    setSuperAggregateScopedSymbol(Optional.ofNullable(baseSymbol));
  }

  public boolean hasImmediateSuper(IAggregateSymbol theSuper) {
    return superAggregateScopedSymbol.map(
        aggregateSymbol -> aggregateSymbol.isExactSameType(theSuper)).orElse(false);
  }

  @Override
  public ISymbol setType(Optional<ISymbol> type) {
    //Used when cloning so type can be set if not already populated.
    if (this.isGenericTypeParameter() && type.isPresent()) {
      this.setSuperAggregateScopedSymbol((IAggregateSymbol) type.get());
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
  public MethodSymbolSearchResult resolveMatchingMethods(MethodSymbolSearch search,
                                                         MethodSymbolSearchResult result) {
    MethodSymbolSearchResult buildResult = new MethodSymbolSearchResult(result);
    //Do supers first then do own
    //So if we have a super then this will bee a peer of anything we already have passed
    //in could be traits/interfaces

    if (superAggregateScopedSymbol.isPresent()) {
      buildResult = buildResult.mergePeerToNewResult(superAggregateScopedSymbol.get()
          .resolveMatchingMethods(search, new MethodSymbolSearchResult()));
    }

    MethodSymbolSearchResult res =
        resolveMatchingMethodsInThisScopeOnly(search, new MethodSymbolSearchResult());
    buildResult = buildResult.overrideToNewResult(res);

    return buildResult;
  }

  /**
   * Just try and resolve a member in this or super scopes.
   */
  public Optional<ISymbol> resolveMember(SymbolSearch search) {
    //For members; we only look to our aggregate and super (unless we introduce traits later)
    Optional<ISymbol> rtn = resolveInThisScopeOnly(search);
    if (rtn.isEmpty() && superAggregateScopedSymbol.isPresent()) {
      rtn = superAggregateScopedSymbol.get().resolveMember(search);
    }
    return rtn;
  }

  /**
   * So this is where we alter the mechanism of normal symbol resolution.
   */
  @Override
  public Optional<ISymbol> resolve(SymbolSearch search) {
    Optional<ISymbol> rtn = resolveFromParameterisedTypes(search);

    if (rtn.isEmpty()) {
      rtn = resolveInThisScopeOnly(search);
    }

    if (rtn.isEmpty()) {
      rtn = resolveWithParentScope(search);
    }
    return rtn;
  }

  private String doGetFriendlyName() {
    return super.getFriendlyName() + getAnyGenericParamsAsFriendlyNames();
  }
}
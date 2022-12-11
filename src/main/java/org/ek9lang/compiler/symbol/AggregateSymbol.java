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
 * public statics on a class. For example module the.mod that defines Constants
 * could have a class called the.mod._Constants with public final statics
 * defined.
 * So there are two ways to resolve references first is module scope and second
 * is super class type scope.
 */
public class AggregateSymbol extends ScopedSymbol implements IAggregateSymbol {

  /**
   * Also keep a back pointer to the direct subclasses.
   * This is really useful for analysing a class or a trait.
   */
  private final List<IAggregateSymbol> subAggregateScopedSymbols = new ArrayList<>();

  //This is the module this aggregate has been defined in.
  private IScope moduleScope;

  /**
   * Just used in commented output really - just handy to understand the origin of the aggregate
   * as some are synthetic.
   */
  private String aggregateDescription;

  /**
   * This is actually a 'T' itself - we will need to know this.
   */
  private boolean genericTypeParameter;

  /**
   * If there is a method that acts as a dispatcher then this aggregate is also a dispatcher.
   */
  private boolean markedAsDispatcher = false;
  /**
   * Might always be null if a base 'class' or 'interface'.
   */
  private Optional<IAggregateSymbol> superAggregateScopedSymbol = Optional.empty();

  /**
   * Was the aggregate marked as abstract in the source code.
   */
  private boolean markedAbstract = false;

  /**
   * Now an aggregate might not have been marked as abstract.
   * But it might have on inherit some methods that are abstract.
   * In such cases we force this aggregate to be marked as virtual.
   * We can then do simple analysis and indicate the aggregate must
   * be marked as abstract by the developer.
   */
  private boolean forceVirtual = false;

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
  private Optional<String> pipeSinkType = Optional.empty();

  /**
   * When used in pipeline processing, what is the type this aggregate could
   * support to create types.
   */
  private Optional<String> pipeSourceType = Optional.empty();

  /**
   * For dynamic classes we can capture variables from the enclosing scope(s) and pull them in
   * We can then hold and access them in the dynamic class even when the class has moved out of
   * the original scope. i.e. a sort of closure over a variable - but has to be declared and is
   * not implicit like java/other languages.
   */
  private Optional<LocalScope> capturedVariables = Optional.empty();

  /*
   * For built-in or reverse engineered aggregates we need to know if "_get()" of object as itself
   * is supported; so we can create the IR output.
   * This is typically needed if we want to use these aggregates in pipelines.
   * Though this will be rationalised with an iterator that just returns an iterator or one item
   * of itself. getOfSelfSupported = false
   */

  /**
   * A simple straight forward aggregate type, like a class or record.
   */
  public AggregateSymbol(String name, IScope enclosingScope) {
    super(name, enclosingScope);
    super.setCategory(SymbolCategory.TYPE);
    super.setProduceFullyQualifiedName(true);
  }

  /**
   * A simple straight forward aggregate type, like a class or record.
   */
  public AggregateSymbol(String name, Optional<ISymbol> type, IScope enclosingScope) {
    super(name, type, enclosingScope);
    super.setCategory(SymbolCategory.TYPE);
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
    newCopy.forceVirtual = forceVirtual;
    newCopy.injectable = injectable;
    newCopy.openForExtension = openForExtension;

    pipeSinkType.ifPresent(s -> newCopy.pipeSinkType = Optional.of(s));

    pipeSourceType.ifPresent(s -> newCopy.pipeSourceType = Optional.of(s));

    if (capturedVariables.isPresent()) {
      LocalScope newCaptureScope = new LocalScope("CaptureScope", getEnclosingScope());
      capturedVariables.get().cloneIntoLocalScope(newCaptureScope);
      newCopy.setCapturedVariables(newCaptureScope);
    }

    return newCopy;
  }

  /**
   * If used in dynamic form, this will return the scope with any dynamic variables
   * that we captured at definition.
   *
   * @return An optional of a scope containing zero or more captured variables.
   */
  @Override
  public Optional<LocalScope> getCapturedVariables() {
    return capturedVariables;
  }

  /**
   * When used in a dynamic style aggregate to set the variables that have been captured.
   *
   * @param capturedVariables The scope with the captured variables in.
   */
  @Override
  public void setCapturedVariables(LocalScope capturedVariables) {
    this.capturedVariables = Optional.ofNullable(capturedVariables);
  }

  /**
   * Attempt to resolve a named variable but exclude looking any captured variables.
   *
   * @param search The symbol search to use to find the variable.
   * @return An optional match if one is found.
   */
  @Override
  public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search) {
    return super.resolveInThisScopeOnly(search);
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

  public void setPipeSinkType(Optional<String> pipeSinkType) {
    this.pipeSinkType = pipeSinkType;
  }

  public Optional<String> getPipeSourceType() {
    return pipeSourceType;
  }

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
  public boolean isVirtual() {
    return forceVirtual;
  }

  @Override
  public void setVirtual(boolean virtual) {
    this.forceVirtual = virtual;
  }

  @Override
  public boolean isInjectable() {
    return injectable;
  }

  public void setInjectable(boolean injectable) {
    this.injectable = injectable;
  }

  /**
   * Is this aggregate based of generic sort of aggregate, and it has been parameterized.
   */
  @Override
  public boolean isGenericInNature() {
    return !this.getParameterisedTypes().isEmpty();
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
   * Parameterize this with an aggregate type.
   */
  @Override
  public AggregateSymbol addParameterisedType(AggregateSymbol parameterisedType) {
    super.addParameterisedType(parameterisedType);
    super.setCategory(SymbolCategory.TEMPLATE_TYPE);
    return this;
  }

  public List<IAggregateSymbol> getSubAggregateScopedSymbols() {
    return subAggregateScopedSymbols;
  }

  /**
   * We note down any sub types this type is used with.
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
    return getSymbolsForThisScope().parallelStream().filter(ISymbol::isMethod)
        .map(MethodSymbol.class::cast).filter(predicate).toList();
  }

  @Override
  public boolean isTraitImplemented(AggregateWithTraitsSymbol thisTraitSymbol) {
    return superAggregateScopedSymbol.map(
        aggregateSymbol -> aggregateSymbol.isTraitImplemented(thisTraitSymbol)).orElse(false);
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

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
    //But note we limit the search in the captured vars to that scope only.
    //no looking up the enclosing scopes just the capture scope!
    if (rtn.isEmpty() && capturedVariables.isPresent()) {
      rtn = capturedVariables.get().resolveInThisScopeOnly(search);
    }

    return rtn;
  }

  /**
   * Resolve using super aggregate.
   */
  protected Optional<ISymbol> resolveWithParentScope(SymbolSearch search) {
    Optional<ISymbol> rtn = Optional.empty();

    //So we keep going back up the class hierarchy until no more supers then.
    //When we get to the final aggregate we use the scopedSymbol local and back up to
    //global symbol table.
    if (superAggregateScopedSymbol.isPresent()) {
      rtn = superAggregateScopedSymbol.get().resolve(search);
    }

    if (rtn.isEmpty()) {
      rtn = super.resolve(search);
    }

    return rtn;
  }

  /**
   * So this is where we alter the mechanism of normal symbol resolution.
   */
  @Override
  public Optional<ISymbol> resolve(SymbolSearch search) {
    Optional<ISymbol> rtn = Optional.empty();
    //Now if this is a generic type class we might need to resolve the name of the type 'T' or 'S'
    //or whatever for example
    if (isGenericInNature() && search.getSearchType().equals(SymbolCategory.TYPE)) {
      for (ISymbol parameterisedType : getParameterisedTypes()) {
        if (parameterisedType.isAssignableTo(search.getAsSymbol())) {
          rtn = Optional.of(parameterisedType);
        }
      }
    }

    //Now see if it is defined in this aggregate.
    if (rtn.isEmpty()) {
      rtn = resolveInThisScopeOnly(search);
    }
    //if not then resolve with parent scope which could be super or back up scope tree
    if (rtn.isEmpty()) {
      rtn = resolveWithParentScope(search);
    }
    return rtn;
  }

  private String doGetFriendlyName() {
    return super.getFriendlyName() + getAnyGenericParamsAsFriendlyNames();
  }
}
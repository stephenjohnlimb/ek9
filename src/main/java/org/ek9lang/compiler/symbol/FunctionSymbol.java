package org.ek9lang.compiler.symbol;

import java.util.List;
import java.util.Optional;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Scope for functions that are part of a module.
 * While in ek9 these are just functions, when mapped to java we can implement in any
 * way we like i.e. classes.
 * We need to ensure that any functions we extend have the same method signature.
 */
public class FunctionSymbol extends MethodSymbol {

  //This is the module this function has been defined in.
  private IScope moduleScope;

  /**
   * To be used when this function extends an abstract function.
   * So we want the same method signature as the abstract function but this provides the
   * implementation.
   * It is sort of object-oriented but for functions.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<FunctionSymbol> superFunctionSymbol = Optional.empty();

  /**
   * For Functions symbols we keep a handle on the context where the returning param (if any)
   * was defined. We do this because with functions we allow the function name to be defined
   * when implementing an abstract function without the need to refine all the parameter and returns.
   * Clearly this would not make sense for methods where you have overloading but for functions
   * there is only one name for that function just the parameters and returns alter.
   */
  private EK9Parser.ReturningParamContext returningParamContext;

  /**
   * Create a new Function Symbol with a specific unique name (in the enclosing scope).
   */
  public FunctionSymbol(String name, IScope enclosingScope) {
    super(name, enclosingScope);
    super.setCategory(SymbolCategory.FUNCTION);
    super.setScopeType(ScopeType.NON_BLOCK);
    super.setProduceFullyQualifiedName(true);
  }

  /**
   * A function that can be parameterised, i.e. like a 'List of T'.
   * So the name would be 'List' and the parameterTypes would be a single aggregate of
   * a conceptual T.
   */
  public FunctionSymbol(String name, IScope enclosingScope, List<AggregateSymbol> parameterTypes) {
    this(name, enclosingScope);
    parameterTypes.forEach(this::addParameterisedType);
  }

  @Override
  public FunctionSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoFunctionSymbol(new FunctionSymbol(this.getName(), withParentAsAppropriate));
  }

  protected FunctionSymbol cloneIntoFunctionSymbol(FunctionSymbol newCopy) {
    super.cloneIntoMethodSymbol(newCopy);
    newCopy.setCategory(SymbolCategory.FUNCTION);
    newCopy.setProduceFullyQualifiedName(this.getProduceFullyQualifiedName());
    newCopy.moduleScope = this.moduleScope;
    newCopy.returningParamContext = this.returningParamContext;
    superFunctionSymbol.ifPresent(
        functionSymbol -> newCopy.superFunctionSymbol = Optional.of(functionSymbol));

    return newCopy;
  }

  /**
   * Does this function directly implement or through its hierarch implement the function passed in.
   */
  public boolean isImplementingInSomeWay(final FunctionSymbol function) {
    if (function == this) {
      return true;
    }
    return superFunctionSymbol.map(functionSymbol -> functionSymbol.isImplementingInSomeWay(function)).orElse(false);
  }

  /**
   * By adding a parameterised type this Function stops being a FUNCTION and becomes a TEMPLATE_FUNCTION.
   */
  @Override
  public void addParameterisedType(AggregateSymbol parameterisedType) {
    super.addParameterisedType(parameterisedType);
    super.setCategory(SymbolCategory.TEMPLATE_FUNCTION);
  }

  public EK9Parser.ReturningParamContext getReturningParamContext() {
    return returningParamContext;
  }

  public void setReturningParamContext(EK9Parser.ReturningParamContext returningParamContext) {
    this.returningParamContext = returningParamContext;
  }

  public IScope getModuleScope() {
    return moduleScope;
  }

  public void setModuleScope(IScope moduleScope) {
    this.moduleScope = moduleScope;
  }

  @Override
  protected Optional<IScope> getAnySuperTypeOrFunction() {
    if (this.superFunctionSymbol.isPresent()) {
      return Optional.of(superFunctionSymbol.get());
    }
    return Optional.empty();
  }

  public Optional<FunctionSymbol> getSuperFunctionSymbol() {
    return superFunctionSymbol;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setSuperFunctionSymbol(Optional<FunctionSymbol> superFunctionSymbol) {
    this.superFunctionSymbol = superFunctionSymbol;
  }

  @Override
  public void setReturningSymbol(ISymbol returningSymbol) {
    justSetReturningSymbol(returningSymbol);
  }

  @Override
  public double getAssignableWeightTo(ISymbol s) {
    return getUnCoercedAssignableWeightTo(s);
  }

  @Override
  public double getUnCoercedAssignableWeightTo(ISymbol s) {
    double canAssign = super.getUnCoercedAssignableWeightTo(s);
    if (canAssign >= 0.0) {
      return canAssign;
    }

    //now we can check superclass matches. but add some weight because this did not match
    return superFunctionSymbol.map(value -> 0.05 + value.getUnCoercedAssignableWeightTo(s))
        .orElse(-1.0);
  }

  @Override
  public String getFriendlyScopeName() {
    return getFriendlyName();
  }

  @Override
  public String getFriendlyName() {
    Optional<ISymbol> returningSymbolType =
        getReturningSymbol() != null ? getReturningSymbol().getType() : Optional.empty();
    var prefix = getName().isEmpty() ? "dynamic function" : getName();
    var name =
        doGetFriendlyName(prefix + getPrivateVariablesForDisplay(), returningSymbolType)
            + getAnyGenericParamsAsFriendlyNames();

    return superFunctionSymbol.map(s -> name + " is " + s.getName()).orElse(name);
  }

  @Override
  public Optional<ISymbol> getType() {
    //Treat this as a type. To get result of call need to use:
    return Optional.of(this);
  }

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
}

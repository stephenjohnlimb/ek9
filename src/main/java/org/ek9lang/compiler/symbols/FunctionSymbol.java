package org.ek9lang.compiler.symbols;

import java.util.List;
import java.util.Optional;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.symbols.search.SymbolSearch;
import org.ek9lang.compiler.symbols.support.SymbolMatcher;
import org.ek9lang.compiler.symbols.support.ToCommaSeparated;

/**
 * Scope for functions that are part of a module.
 * While in ek9 these are just functions, when mapped to java we can implement in any
 * way we like i.e. classes.
 * We need to ensure that any functions we extend have the same method signature.
 */
public class FunctionSymbol extends PossibleGenericSymbol {

  //Just used internally to check for method signature matching
  private final SymbolMatcher matcher = new SymbolMatcher();

  /**
   * Keep separate variable for what we are returning because we need its name and type.
   */
  private ISymbol returningSymbol;

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
    super.setGenus(ISymbol.SymbolGenus.FUNCTION);
    super.setScopeType(ScopeType.NON_BLOCK);
    super.setProduceFullyQualifiedName(true);
  }

  /**
   * A function that can be parameterised, i.e. like a 'List of T'.
   * So the name would be 'List' and the parameterTypes would be a single aggregate of
   * a conceptual T.
   */
  public FunctionSymbol(String name, IScope enclosingScope, List<AggregateSymbol> typeParameterOrArguments) {
    this(name, enclosingScope);
    typeParameterOrArguments.forEach(this::addTypeParameterOrArgument);
  }

  @Override
  public FunctionSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoFunctionSymbol(new FunctionSymbol(this.getName(), withParentAsAppropriate));
  }

  protected FunctionSymbol cloneIntoFunctionSymbol(FunctionSymbol newCopy) {
    super.cloneIntoPossibleGenericSymbol(newCopy);
    if (isReturningSymbolPresent()) {
      newCopy.returningSymbol = this.returningSymbol.clone(newCopy);
    }

    newCopy.setCategory(this.getCategory());
    newCopy.setProduceFullyQualifiedName(this.getProduceFullyQualifiedName());
    newCopy.returningParamContext = this.returningParamContext;
    superFunctionSymbol.ifPresent(
        functionSymbol -> newCopy.superFunctionSymbol = Optional.of(functionSymbol));

    return newCopy;
  }

  /**
   * Added convenience method to make the parameters a bit more obvious.
   */
  public List<ISymbol> getCallParameters() {
    return super.getSymbolsForThisScope();
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

  public EK9Parser.ReturningParamContext getReturningParamContext() {
    return returningParamContext;
  }

  public void setReturningParamContext(EK9Parser.ReturningParamContext returningParamContext) {
    this.returningParamContext = returningParamContext;
  }

  @Override
  protected Optional<IScope> getAnySuperTypeOrFunction() {
    if (this.superFunctionSymbol.isPresent()) {
      return Optional.of(superFunctionSymbol.get());
    }
    return Optional.empty();
  }

  /**
   * Check if the parameter types and return types match.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public boolean isSignatureMatchTo(Optional<ISymbol> theirReturnType, final List<ISymbol> theirParams) {
    List<ISymbol> ourParams = this.getSymbolsForThisScope();
    double weight = matcher.getWeightOfParameterMatch(theirParams, ourParams);
    if (weight < 0.0) {
      return false;
    }
    weight = matcher.getWeightOfMatch(this.getType(), theirReturnType);

    return weight >= 0.0;
  }

  public Optional<FunctionSymbol> getSuperFunctionSymbol() {
    return superFunctionSymbol;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setSuperFunctionSymbol(Optional<FunctionSymbol> superFunctionSymbol) {
    this.superFunctionSymbol = superFunctionSymbol;
  }

  public void setSuperFunctionSymbol(FunctionSymbol superFunctionSymbol) {
    setSuperFunctionSymbol(Optional.ofNullable(superFunctionSymbol));
  }

  /**
   * Some functions have a named return symbol 'like rtn as String' for example.
   * In other cases a function will not return anything (We use 'Void') in the
   * case as the 'type'.
   * So when a Returning Symbol is set we use the type of the returning variable as the type
   * return on the function.
   */
  public boolean isReturningSymbolPresent() {
    return returningSymbol != null;
  }

  /**
   * Provide a symbol that is returned from this function.
   * Note in EK9 this is not just a type but actually a variable symbol (that has a type).
   */
  public ISymbol getReturningSymbol() {
    return returningSymbol;
  }

  public void setReturningSymbol(ISymbol returningSymbol) {
    justSetReturningSymbol(returningSymbol);
  }

  protected void justSetReturningSymbol(ISymbol returningSymbol) {
    this.returningSymbol = returningSymbol;
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


  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  protected String doGetFriendlyName(String withName, Optional<ISymbol> theType) {
    var toCommaSeparated = new ToCommaSeparated(this, true);
    StringBuilder buffer = new StringBuilder();

    buffer.append(getSymbolTypeAsString(theType));

    buffer.append(" <- ").append(withName);
    buffer.append(toCommaSeparated.apply(getSymbolsForThisScope()));
    if (isMarkedAbstract()) {
      buffer.append(" as abstract");
    }

    return buffer.toString();
  }

  @Override
  public Optional<ISymbol> getType() {
    //Treat this as a type. To get result of call need to use:
    return Optional.of(this);
  }


  @Override
  public ISymbol setType(ISymbol type) {
    return this;
  }

  @Override
  public ISymbol setType(Optional<ISymbol> type) {
    return this;
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    //This will now also check the returning symbol (if present)
    if (this.isReturningSymbolPresent() && returningSymbol.getName().equals(search.getName())) {
      return Optional.of(returningSymbol);
    }
    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FunctionSymbol that)) {
      return false;
    }

    boolean result = getReturningSymbol() != null ? getReturningSymbol().equals(that.getReturningSymbol()) :
        that.getReturningSymbol() == null;

    return result
        && super.equals(o)
        && isMarkedPure() == that.isMarkedPure()
        && getSuperFunctionSymbol().equals(that.getSuperFunctionSymbol());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getReturningSymbol() != null ? getReturningSymbol().hashCode() : 0);
    result = 31 * result + (isMarkedPure() ? 1 : 0);
    result = 31 * result + getSuperFunctionSymbol().hashCode();
    return result;
  }
}

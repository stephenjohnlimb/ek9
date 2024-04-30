package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.SymbolMatcher;
import org.ek9lang.compiler.support.ToCommaSeparated;

/**
 * Represents some type of method that exists on an aggregate type scope.
 * Or it could just be a function type concept at the module level.
 * So it really is some type of callable 'thing' that has a scope of its own.
 * This scope is used to hold incoming parameters.
 * It can also have some form of visibility/access modifiers and holds the concept
 * of being overridable in terms of inheritance. In some cases it may also be
 * defined as one of the 'operators' such as '+' or '$' for example.
 */
public class MethodSymbol extends ScopedSymbol implements IMayReturnSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  //Just used internally to check for method signature matching
  //But has to be lazily created for serialisation.
  private final SymbolMatcher matcher = new SymbolMatcher();

  /**
   * Keep separate variable for what we are returning because we need its name and type.
   */
  private ISymbol returningSymbol;

  /**
   * So has the developer indicated that this method is an overriding method.
   */
  private boolean override = false;

  /**
   * By default, access to methods is public unless otherwise modified.
   */
  private String accessModifier = "public";

  /**
   * Is this a constructor method or just a normal method.
   */
  private boolean constructor = false;

  /**
   * Is this an operator like := or &lt; etc.
   */
  private boolean operator = false;

  /**
   * Was it marked abstract in the source code.
   */
  private boolean markedAbstract = false;

  private boolean markedAsDispatcher = false;

  /**
   * Should this method be cloned during a clone operation like for type defines.
   */
  private boolean markedNoClone = false;

  /**
   * Is this method a synthetic one, ie typically for constructors the compiler can indicate
   * this method should be created in code generation.
   */
  private boolean synthetic = false;

  /**
   * Really just used for reverse engineered methods from Java we need to know if the method
   * returns this or not.
   */
  private boolean ek9ReturnsThis = false;

  /**
   * This is the name of the delegate in the aggregate.
   * DelegatingProcessor with trait of Processor by proc
   * proc as Processor?
   * This is held at method level, so some methods can be delegated and others implemented.
   * In effect the compiler needs to add synthetic methods in for the methods on the trait.
   * But the developer can decide to implement them if they elect to.
   */
  private String usedAsProxyForDelegate = null;

  /**
   * Create  new method symbol of a specific name with an enclosing scope (i.e. a class).
   */
  public MethodSymbol(final String name, final IScope enclosingScope) {

    this(name, Optional.empty(), enclosingScope);

  }

  /**
   * Typically used for cloning constructors.
   */
  public MethodSymbol(final String name, final ISymbol type, final IScope enclosingScope) {

    this(name, Optional.of(type), enclosingScope);

  }

  /**
   * Create  new method symbol of a specific name with an enclosing scope (i.e. a class).
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public MethodSymbol(final String name, final Optional<ISymbol> type, final IScope enclosingScope) {

    super(name, type, enclosingScope);
    super.setCategory(SymbolCategory.METHOD);
    super.setGenus(SymbolGenus.VALUE);

  }

  private SymbolMatcher getSymbolMatcher() {

    return matcher;
  }

  /**
   * For actual methods this will be the aggregate they are part of.
   */
  public IScope getParentScope() {

    return this.getEnclosingScope();
  }

  public MethodSymbol clone(final ISymbol withType, final IScope withParentAsAppropriate) {

    return cloneIntoMethodSymbol(
        new MethodSymbol(this.getName(), withType, withParentAsAppropriate));
  }

  @Override
  public MethodSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoMethodSymbol(
        new MethodSymbol(this.getName(), this.getType(), withParentAsAppropriate));
  }

  protected MethodSymbol cloneIntoMethodSymbol(final MethodSymbol newCopy) {

    super.cloneIntoScopeSymbol(newCopy);

    return copyMethodProperties(newCopy);
  }

  /**
   * Just copies the properties over.
   */
  public MethodSymbol copyMethodProperties(final MethodSymbol newCopy) {

    if (this.returningSymbol != null) {
      newCopy.returningSymbol = this.returningSymbol.clone(newCopy);
    }
    newCopy.override = this.override;
    newCopy.accessModifier = this.accessModifier;
    newCopy.constructor = this.constructor;
    newCopy.operator = this.operator;
    newCopy.markedAbstract = this.markedAbstract;
    newCopy.markedAsDispatcher = this.markedAsDispatcher;
    newCopy.markedNoClone = this.markedNoClone;
    newCopy.synthetic = this.synthetic;
    newCopy.ek9ReturnsThis = this.ek9ReturnsThis;
    newCopy.usedAsProxyForDelegate = this.usedAsProxyForDelegate;

    return newCopy;
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(final SymbolSearch search) {

    //This will now also check the returning symbol (if present)
    if (this.isReturningSymbolPresent() && returningSymbol.getName().equals(search.getName())) {
      return Optional.of(returningSymbol);
    }

    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolve(final SymbolSearch search) {

    final var rtn = resolveInThisScopeOnly(search);
    if (rtn.isEmpty()) {
      return super.resolve(search);
    }

    return rtn;
  }

  public boolean isSynthetic() {

    return synthetic;
  }

  public void setSynthetic(final boolean synthetic) {

    this.synthetic = synthetic;
  }

  public boolean isOverride() {

    return override;
  }

  public void setOverride(final boolean override) {

    this.override = override;
  }

  public String getAccessModifier() {

    return accessModifier;
  }

  public void setAccessModifier(final String accessModifier) {

    this.accessModifier = accessModifier;
  }

  @Override
  public boolean isPrivate() {

    return accessModifier.equals("private");
  }

  @Override
  public boolean isProtected() {

    return accessModifier.equals("protected");
  }

  @Override
  public boolean isPublic() {

    return accessModifier.equals("public");
  }

  public boolean isUsedAsProxyForDelegate() {

    return usedAsProxyForDelegate != null;
  }

  public String getUsedAsProxyForDelegate() {

    return this.usedAsProxyForDelegate;
  }

  /**
   * Set this method to be marked so at it delegates any calls to a delegate.
   */
  public void setUsedAsProxyForDelegate(final String delegateName) {

    this.usedAsProxyForDelegate = delegateName;
    //now this also means a couple of other things
    this.setOverride(true);
    this.setMarkedAbstract(false);
  }

  public boolean isEk9ReturnsThis() {

    return ek9ReturnsThis;
  }

  public void setEk9ReturnsThis(final boolean ek9ReturnsThis) {

    this.ek9ReturnsThis = ek9ReturnsThis;
  }

  public boolean isMarkedNoClone() {

    return markedNoClone;
  }

  public void setMarkedNoClone(final boolean markedNoClone) {

    this.markedNoClone = markedNoClone;
  }

  public boolean isMarkedAsDispatcher() {

    return markedAsDispatcher;
  }

  public void setMarkedAsDispatcher(final boolean markedAsDispatcher) {

    this.markedAsDispatcher = markedAsDispatcher;
  }

  @Override
  public boolean isMarkedAbstract() {

    return markedAbstract;
  }

  public void setMarkedAbstract(final boolean markedAbstract) {

    this.markedAbstract = markedAbstract;
  }

  public boolean isNotMarkedAbstract() {

    return !markedAbstract;
  }

  @Override
  public boolean isConstant() {

    return true;
  }

  public boolean isConstructor() {

    return constructor;
  }

  public boolean isNotConstructor() {

    return !isConstructor();
  }

  public MethodSymbol setConstructor(final boolean constructor) {

    this.constructor = constructor;

    return this;
  }

  public boolean isOperator() {

    return operator;
  }

  public MethodSymbol setOperator(final boolean operator) {

    this.operator = operator;

    return this;
  }

  /**
   * Some methods have a named return symbol 'like rtn as String' for example.
   * In other cases a method will not return anything (We use 'Void') in the
   * case as the 'type'.
   * So when a Returning Symbol is set we use the type of the returning variable as the type
   * return on the method.
   */
  public boolean isReturningSymbolPresent() {

    return returningSymbol != null;
  }

  /**
   * Provide a symbol that is returned from this method.
   * Note in EK9 this is not just a type but actually a variable symbol (that has a type).
   */
  @Override
  public ISymbol getReturningSymbol() {

    return returningSymbol;
  }

  /**
   * Sets the returning symbol (variable not just type).
   */
  @Override
  public void setReturningSymbol(final VariableSymbol returningSymbol) {

    returningSymbol.setReturningParameter(true);
    justSetReturningSymbol(returningSymbol);
    setNullAllowed(returningSymbol.isNullAllowed());
    setType(returningSymbol.getType());

  }

  protected void justSetReturningSymbol(final ISymbol returningSymbol) {

    this.returningSymbol = returningSymbol;

  }

  /**
   * Does the signature of this method match that of the method passed in.
   * Not the name of the method just the signature of the parameter types
   * and special treatment for the return type - this can be coerced back or be a super type.
   * This also takes into account polymorphism and promotions.
   */
  public boolean isSignatureMatchTo(final MethodSymbol toMethod) {

    double weight = getWeightOfParameterMatches(toMethod.getSymbolsForThisScope(), getSymbolsForThisScope());

    if (weight < 0.0) {
      return false;
    }

    weight += getSymbolMatcher().getWeightOfMatch(this.getType(), toMethod.getType());

    return weight >= 0.0;
  }

  /**
   * This method just checks that the signature has exactly the same types as parameters.
   * So this is 'exact' in addition it does not check the return type at all, nor the method name.
   * It just focuses on the number order and type of the parameters matching.
   */
  public boolean isExactSignatureMatchTo(final MethodSymbol toMethod) {

    double weight = getWeightOfParameterMatches(toMethod.getSymbolsForThisScope(), getSymbolsForThisScope());
    return Math.abs(weight) < 1e-6;
  }

  /**
   * Very important method; that checks if the parameter list provided can match
   * the parameters declared for this method.
   * But this takes into account polymorphism and promotions.
   */
  public boolean isParameterSignatureMatchTo(final List<ISymbol> params) {

    return getWeightOfParameterMatches(params, getSymbolsForThisScope()) >= 0.0;
  }

  private double getWeightOfParameterMatches(final List<ISymbol> fromSymbols, final List<ISymbol> toSymbols) {

    return getSymbolMatcher().getWeightOfParameterMatch(fromSymbols, toSymbols);
  }

  /**
   * Added convenience method to make the parameters a bit more obvious.
   */
  public List<ISymbol> getCallParameters() {

    return super.getSymbolsForThisScope();
  }

  /**
   * Typically used when making synthetic methods you want to add in all the params from another
   * method or something.
   *
   * @param params The parameters to pass to the method.
   */
  public void setCallParameters(final List<ISymbol> params) {

    for (ISymbol param : params) {
      define(param);
    }
  }

  @Override
  public String getFriendlyName() {

    return doGetFriendlyName(super.getName(), this.getType());
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  protected String doGetFriendlyName(final String withName, final Optional<ISymbol> theType) {

    final var toCommaSeparated = new ToCommaSeparated(true);
    final var buffer = new StringBuilder();

    if (this.isOverride()) {
      buffer.append("override ");
    }
    buffer.append(accessModifier).append(" ");
    buffer.append(getSymbolTypeAsString(theType));
    buffer.append(" <- ").append(withName);
    buffer.append(toCommaSeparated.apply(getSymbolsForThisScope()));
    if (this.markedAbstract) {
      buffer.append(" as abstract");
    }

    return buffer.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    return (o instanceof MethodSymbol that)
        && super.equals(o)
        && isOverride() == that.isOverride()
        && isConstructor() == that.isConstructor()
        && isOperator() == that.isOperator()
        && isMarkedAbstract() == that.isMarkedAbstract()
        && isMarkedAsDispatcher() == that.isMarkedAsDispatcher()
        && isMarkedNoClone() == that.isMarkedNoClone()
        && isSynthetic() == that.isSynthetic()
        && isEk9ReturnsThis() == that.isEk9ReturnsThis()
        && getAccessModifier().equals(that.getAccessModifier());
  }

  @Override
  public int hashCode() {

    //Altered to use hashcode of friendly name, because otherwise if the method had parameters
    //That we the same type as the aggregate the method was on, then we'd get an endless loop of calling hash code.
    int result = getFriendlyName().hashCode();
    result = 31 * result + (getSourceToken() != null ? getSourceToken().hashCode() : 0);
    result = 31 * result + (isOverride() ? 1 : 0);
    result = 31 * result + (isConstructor() ? 1 : 0);
    result = 31 * result + (isOperator() ? 1 : 0);
    result = 31 * result + (isMarkedAbstract() ? 1 : 0);
    result = 31 * result + (isMarkedAsDispatcher() ? 1 : 0);
    result = 31 * result + (isMarkedNoClone() ? 1 : 0);
    result = 31 * result + (isSynthetic() ? 1 : 0);
    result = 31 * result + (isEk9ReturnsThis() ? 1 : 0);
    result = 31 * result + getAccessModifier().hashCode();

    return result;
  }

  @Override
  public String toString() {

    return getFriendlyName();
  }
}

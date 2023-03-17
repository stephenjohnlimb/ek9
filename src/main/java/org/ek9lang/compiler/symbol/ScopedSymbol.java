package org.ek9lang.compiler.symbol;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Represents a symbol that also has a scope. Typically, this means it can have variables,
 * methods and function declared within it. But also includes block scoped constructs like
 * for loops for example.
 * It's turning into a bit of a 'bitbucket' because I don't really want duplication at the FunctionSymbol
 * and AggregateSymbol level. So some things like CaptureVariables are not really applicable (yet).
 * But I may use them later for other constructs and controls.
 * I may refactor the generic bits out to a new Symbol type between ScopedSymbol and Symbol
 */
public class ScopedSymbol extends Symbol implements IScopedSymbol {

  /**
   * This is the scope where any symbols actually get defined.
   */
  private final LocalScope actualScope;

  /**
   * If we encounter an exception within a scope we need to note the line number.
   */
  private Token encounteredExceptionToken = null;

  /**
   * For some scoped symbols - dynamic functions and classes it is important to keep a
   * reference to the outermost enclosing aggregate or function.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<IScopedSymbol> outerMostTypeOrFunction = Optional.empty();

  public ScopedSymbol(String name, IScope enclosingScope) {
    super(name);
    actualScope = new LocalScope(ScopeType.BLOCK, name, enclosingScope);
  }

  public ScopedSymbol(IScope.ScopeType scopeType, String scopeName, IScope enclosingScope) {
    super(scopeName);
    actualScope = new LocalScope(scopeType, scopeName, enclosingScope);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public ScopedSymbol(String name, Optional<ISymbol> type, IScope enclosingScope) {
    super(name, type);
    actualScope = new LocalScope(ScopeType.BLOCK, name, enclosingScope);
  }

  @Override
  public int hashCode() {

    return Objects.hash(super.hashCode(), actualScope.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    var rtn = super.equals(obj);
    if (rtn && obj instanceof ScopedSymbol scopedSymbol) {
      rtn = actualScope.equals(scopedSymbol.actualScope);
    } else {
      rtn = false;
    }
    return rtn;
  }

  @Override
  public ScopedSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoScopeSymbol(new ScopedSymbol(this.getName(), withParentAsAppropriate));
  }

  protected ScopedSymbol cloneIntoScopeSymbol(ScopedSymbol newCopy) {
    cloneIntoSymbol(newCopy);
    actualScope.cloneIntoLocalScope(newCopy.actualScope);
    newCopy.encounteredExceptionToken = this.encounteredExceptionToken;
    getOuterMostTypeOrFunction().ifPresent(newCopy::setOuterMostTypeOrFunction);

    return newCopy;
  }

  public Optional<IScopedSymbol> getOuterMostTypeOrFunction() {
    return outerMostTypeOrFunction;
  }

  public void setOuterMostTypeOrFunction(IScopedSymbol outerMostTypeOrFunction) {
    this.outerMostTypeOrFunction = Optional.of(outerMostTypeOrFunction);
  }

  public LocalScope getActualScope() {
    return actualScope;
  }

  @Override
  public IScope.ScopeType getScopeType() {
    return actualScope.getScopeType();
  }

  public void setScopeType(final IScope.ScopeType scopeType) {
    actualScope.setScopeType(scopeType);
  }


  /**
   * Resolve with super type/function or via enclosing scope.
   */
  public Optional<ISymbol> resolveWithParentScope(SymbolSearch search) {
    Optional<ISymbol> rtn = Optional.empty();

    //So we keep going back up the class hierarchy until no more supers then.
    //When we get to the final aggregate we use the scopedSymbol local and back up to
    //global symbol table.
    var theSuper = getAnySuperTypeOrFunction();
    if (theSuper.isPresent()) {
      rtn = theSuper.get().resolve(search);
    }

    if (rtn.isEmpty()) {
      return getEnclosingScope().resolve(search);
    }

    return rtn;
  }


  @Override
  public boolean isMarkedPure() {
    return actualScope.isMarkedPure();
  }

  @Override
  public void setMarkedPure(boolean markedPure) {
    actualScope.setMarkedPure(markedPure);
  }

  @Override
  public boolean isTerminatedNormally() {
    return getEncounteredExceptionToken() == null;
  }

  @Override
  public Token getEncounteredExceptionToken() {
    return encounteredExceptionToken;
  }

  @Override
  public void setEncounteredExceptionToken(Token encounteredExceptionToken) {
    this.encounteredExceptionToken = encounteredExceptionToken;
  }

  @Override
  public String getScopeName() {
    return actualScope.getScopeName();
  }

  @Override
  public String getFriendlyScopeName() {
    return this.getFriendlyName();
  }

  @Override
  public void define(ISymbol symbol) {
    actualScope.define(symbol);
  }

  protected IScope getEnclosingScope() {
    return actualScope.getEnclosingScope();
  }

  protected Optional<IScope> getAnySuperTypeOrFunction() {
    return Optional.empty();
  }

  @Override
  public List<ISymbol> getSymbolsForThisScope() {
    return actualScope.getSymbolsForThisScope();
  }

  @Override
  public MethodSymbolSearchResult resolveMatchingMethods(MethodSymbolSearch search,
                                                         MethodSymbolSearchResult result) {
    return actualScope.resolveMatchingMethods(search, result);
  }

  @Override
  public MethodSymbolSearchResult resolveMatchingMethodsInThisScopeOnly(
      MethodSymbolSearch search, MethodSymbolSearchResult result) {
    return actualScope.resolveMatchingMethodsInThisScopeOnly(search, result);
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    return actualScope.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolve(SymbolSearch search) {
    return actualScope.resolve(search);
  }

  @Override
  public Optional<ScopedSymbol> findNearestNonBlockScopeInEnclosingScopes() {
    if (getScopeType().equals(ScopeType.NON_BLOCK)) {
      return Optional.of(this);
    }
    return actualScope.findNearestNonBlockScopeInEnclosingScopes();
  }

  @Override
  public Optional<ScopedSymbol> findNearestDynamicBlockScopeInEnclosingScopes() {
    if (getScopeType().equals(ScopeType.DYNAMIC_BLOCK)) {
      return Optional.of(this);
    }

    return actualScope.findNearestDynamicBlockScopeInEnclosingScopes();
  }

  @Override
  public boolean isScopeAMatchForEnclosingScope(IScope toCheck) {
    return actualScope.isScopeAMatchForEnclosingScope(toCheck);
  }
}

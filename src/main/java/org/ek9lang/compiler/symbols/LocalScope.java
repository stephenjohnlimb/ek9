package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Optional;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.core.AssertValue;

/**
 * Used in many ways via composition.
 * The local scope can be either just a block (if, where, etc.)
 * or aggregate (class, component, etc.).
 */
public class LocalScope extends SymbolTable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final IScope enclosingScope;
  private IScope.ScopeType scopeType = IScope.ScopeType.BLOCK;

  /**
   * Create a new named local scope with an outer enclosing scope.
   * Scope type default to BLOCK.
   */
  public LocalScope(String scopeName, IScope enclosingScope) {
    super(scopeName);
    AssertValue.checkNotNull("EnclosingScope cannot be null", enclosingScope);
    this.enclosingScope = enclosingScope;
  }

  /**
   * Create a new named local scope with an outer enclosing scope.
   * But will set the scope type to the type passed in.
   */
  public LocalScope(IScope.ScopeType scopeType, String scopeName, IScope enclosingScope) {
    super(scopeName);
    AssertValue.checkNotNull("EnclosingScope cannot be null", enclosingScope);
    this.enclosingScope = enclosingScope;
    this.scopeType = scopeType;
  }

  public LocalScope(IScope enclosingScope) {
    this("localScope", enclosingScope);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof LocalScope that)
        && super.equals(o)
        && getScopeType() == that.getScopeType();
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getScopeType().hashCode();
    return result;
  }

  @Override
  public LocalScope clone(IScope withParentAsAppropriate) {
    return cloneIntoLocalScope(
        new LocalScope(this.scopeType, this.getScopeName(), withParentAsAppropriate));
  }

  /**
   * Clones the content of this into the new copy.
   */
  public LocalScope cloneIntoLocalScope(LocalScope newCopy) {
    super.cloneIntoSymbolTable(newCopy, this.enclosingScope);
    //properties set at construction.
    return newCopy;
  }

  @Override
  public IScope.ScopeType getScopeType() {
    return scopeType;
  }

  public void setScopeType(final IScope.ScopeType scopeType) {
    this.scopeType = scopeType;
  }

  public IScope getEnclosingScope() {
    return enclosingScope;
  }

  /**
   * Useful to be able to check if the scope you have in hand is the same the enclosing scope
   * for this.
   * Typically, used when looking at access modifiers.
   * Find the scope of the aggregate (assuming not a function) where the call is being made
   * from then call this to see if that scope is the same enclosing scope.
   * Then you can determine if access should be allowed.
   *
   * @param toCheck The scope to check
   * @return true if a match false otherwise
   */
  @Override
  public boolean isScopeAMatchForEnclosingScope(IScope toCheck) {
    return enclosingScope == toCheck;
  }

  /**
   * Traverses up the scope tree of enclosing scopes to find the first scope type that is an
   * aggregate.
   * It might not find anything - if you call this in  a local scope in a function or program
   * there is no aggregate up the enclosing scopes only in class/component type stuff is there
   * an aggregate scope for things.
   *
   * @return An Optional scope of the first encounter with and scope that is an aggregate or empty.
   */
  @Override
  public Optional<ScopedSymbol> findNearestNonBlockScopeInEnclosingScopes() {
    if (enclosingScope.getScopeType().equals(ScopeType.NON_BLOCK)) {
      return Optional.of((ScopedSymbol) enclosingScope);
    }

    return enclosingScope.findNearestNonBlockScopeInEnclosingScopes();
  }

  @Override
  public Optional<ScopedSymbol> findNearestDynamicBlockScopeInEnclosingScopes() {
    if (enclosingScope.getScopeType().equals(ScopeType.DYNAMIC_BLOCK)) {
      return Optional.of((ScopedSymbol) enclosingScope);
    }

    return enclosingScope.findNearestDynamicBlockScopeInEnclosingScopes();
  }

  @Override
  protected Optional<ISymbol> resolveWithEnclosingScope(SymbolSearch search) {
    if (search.isLimitToBlocks() && !enclosingScope.getScopeType().equals(IScope.ScopeType.BLOCK)) {
      return Optional.empty();
    }

    return enclosingScope.resolve(search);
  }

  @Override
  protected MethodSymbolSearchResult resolveForAllMatchingMethodsInEnclosingScope(
      MethodSymbolSearch search, MethodSymbolSearchResult result) {
    return enclosingScope.resolveMatchingMethods(search, result);
  }
}
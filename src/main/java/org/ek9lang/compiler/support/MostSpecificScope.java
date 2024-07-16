package org.ek9lang.compiler.support;

import java.util.function.Supplier;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Used when trying to locate the current class or dynamic class, to be able
 * to check if access to fields or methods should be allowed, or even resolve methods without 'this' prefix.
 */
public final class MostSpecificScope implements Supplier<IScope> {
  private final SymbolsAndScopes symbolsAndScopes;

  public MostSpecificScope(final SymbolsAndScopes symbolsAndScopes) {

    this.symbolsAndScopes = symbolsAndScopes;

  }

  @Override
  public IScope get() {

    final var possibleDynamicBlockScope =
        symbolsAndScopes.getTopScope().findNearestDynamicBlockScopeInEnclosingScopes();
    if (possibleDynamicBlockScope.isPresent()) {
      return possibleDynamicBlockScope.get();
    }

    final var possibleNonBlockScope =
        symbolsAndScopes.getTopScope().findNearestNonBlockScopeInEnclosingScopes();
    if (possibleNonBlockScope.isPresent()) {
      return possibleNonBlockScope.get();
    }

    return symbolsAndScopes.getTopScope();
  }
}
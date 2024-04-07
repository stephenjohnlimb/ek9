package org.ek9lang.compiler.support;

import java.util.function.Supplier;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Used when trying to locate the current class or dynamic class, to be able
 * to check if access to fields or methods should be allowed, or even resolve methods without 'this' prefix.
 */
public final class MostSpecificScope implements Supplier<IScope> {
  private final SymbolAndScopeManagement symbolAndScopeManagement;

  public MostSpecificScope(final SymbolAndScopeManagement symbolAndScopeManagement) {

    this.symbolAndScopeManagement = symbolAndScopeManagement;

  }

  @Override
  public IScope get() {

    final var possibleDynamicBlockScope =
        symbolAndScopeManagement.getTopScope().findNearestDynamicBlockScopeInEnclosingScopes();
    if (possibleDynamicBlockScope.isPresent()) {
      return possibleDynamicBlockScope.get();
    }

    final var possibleNonBlockScope =
        symbolAndScopeManagement.getTopScope().findNearestNonBlockScopeInEnclosingScopes();
    if (possibleNonBlockScope.isPresent()) {
      return possibleNonBlockScope.get();
    }

    return symbolAndScopeManagement.getTopScope();
  }
}
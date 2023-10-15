package org.ek9lang.compiler.phase3;

import java.util.function.Supplier;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Used when trying to locate the a current class or dynamic class, so as to be able
 * to check if access to fields or methods should be allowed.
 */
final class MostSpecificScope implements Supplier<IScope> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  MostSpecificScope(final SymbolAndScopeManagement symbolAndScopeManagement) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
  }

  @Override
  public IScope get() {
    var fromScope = symbolAndScopeManagement.getTopScope().findNearestDynamicBlockScopeInEnclosingScopes();
    if (fromScope.isEmpty()) {
      fromScope = symbolAndScopeManagement.getTopScope().findNearestNonBlockScopeInEnclosingScopes();
      if (fromScope.isEmpty()) {
        return symbolAndScopeManagement.getTopScope();
      }
    }
    return fromScope.get();
  }
}
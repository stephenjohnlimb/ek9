package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Supplier;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.IScope;

/**
 * Used when trying to locate the a current class or dynamic class, so as to be able
 * to check if access to fields or methods should be allowed.
 */
public class MostSpecificScope implements Supplier<IScope> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  public MostSpecificScope(final SymbolAndScopeManagement symbolAndScopeManagement) {
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
package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Supplier;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Used when trying to locate the a current class or dynamic class, so as to be able
 * to check if access to fields or methods should be allowed.
 */
public class MostSpecificScope extends RuleSupport implements Supplier<IScope> {


  public MostSpecificScope(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
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
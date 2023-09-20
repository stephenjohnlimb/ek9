package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Only use when code being checked prohibits use in outer generic context.
 */
final class CheckOuterGenericsUse extends RuleSupport implements Consumer<IToken> {

  private final ErrorListener.SemanticClassification errorClassification;

  /**
   * Checks if this token is being used within a generic type/function - directly or indirectly.
   * If it is being run within a generic type/function the configured error is issued.
   */
  CheckOuterGenericsUse(final SymbolAndScopeManagement symbolAndScopeManagement,
                        final ErrorListener errorListener,
                        final ErrorListener.SemanticClassification errorClassification) {
    super(symbolAndScopeManagement, errorListener);
    this.errorClassification = errorClassification;
  }

  @Override
  public void accept(IToken token) {
    var currentScope = symbolAndScopeManagement.getTopScope();

    //Might not be in any sort of dynamic scope at all - but check
    var possibleDynamicScope = currentScope.findNearestDynamicBlockScopeInEnclosingScopes();
    possibleDynamicScope.ifPresentOrElse(dynamicScope -> {
      var possibleGenericScope = dynamicScope.getOuterMostTypeOrFunction();
      possibleGenericScope.ifPresent(scope -> errorIfGenericInNature(token, scope));
    }, () -> {
      var possibleGenericScope = currentScope.findNearestNonBlockScopeInEnclosingScopes();
      possibleGenericScope.ifPresent(scope -> errorIfGenericInNature(token, scope));
    });
  }

  private void errorIfGenericInNature(final IToken token, final IScopedSymbol scope) {
    if (scope.isGenericInNature()) {
      errorListener.semanticError(token, "", errorClassification);
    }
  }
}

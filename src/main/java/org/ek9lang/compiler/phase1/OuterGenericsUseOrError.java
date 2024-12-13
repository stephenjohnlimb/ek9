package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Only use when code being checked prohibits use in outer generic context.
 */
final class OuterGenericsUseOrError extends RuleSupport implements Consumer<IToken> {

  private final ErrorListener.SemanticClassification errorClassification;

  /**
   * Checks if this token is being used within a generic type/function - directly or indirectly.
   * If it is being run within a generic type/function the configured error is issued.
   */
  OuterGenericsUseOrError(final SymbolsAndScopes symbolsAndScopes,
                          final ErrorListener errorListener,
                          final ErrorListener.SemanticClassification errorClassification) {

    super(symbolsAndScopes, errorListener);
    this.errorClassification = errorClassification;

  }

  @Override
  public void accept(final IToken token) {

    var currentScope = symbolsAndScopes.getTopScope();

    //Might not be in any sort of dynamic scope at all - but check
    final var possibleDynamicScope = currentScope.findNearestDynamicBlockScopeInEnclosingScopes();
    possibleDynamicScope.ifPresentOrElse(dynamicScope -> {
      final var possibleGenericScope = dynamicScope.getOuterMostTypeOrFunction();
      possibleGenericScope.ifPresent(scope -> errorIfGenericInNature(token, scope));
    }, () -> {
      final var possibleGenericScope = currentScope.findNearestNonBlockScopeInEnclosingScopes();
      possibleGenericScope.ifPresent(scope -> errorIfGenericInNature(token, scope));
    });

  }

  private void errorIfGenericInNature(final IToken token, final IScopedSymbol scope) {

    if (scope.isGenericInNature()) {
      errorListener.semanticError(token, "", errorClassification);
    }

  }
}

package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Just check is a scope has normal termination.
 * This means does the scope always end up with a guaranteed Exception.
 */
final class CheckNormalTermination implements BiConsumer<Token, IScope> {
  private final ErrorListener errorListener;

  CheckNormalTermination(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final Token token, final IScope scope) {

    if (!scope.isTerminatedNormally()) {
      errorListener.semanticError(token, "",
          ErrorListener.SemanticClassification.RETURN_UNREACHABLE);
    }
  }
}

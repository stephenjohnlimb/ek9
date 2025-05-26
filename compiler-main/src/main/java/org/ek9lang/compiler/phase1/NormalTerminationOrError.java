package org.ek9lang.compiler.phase1;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Just check is a scope has normal termination.
 * This means does the scope always end up with a guaranteed Exception.
 */
final class NormalTerminationOrError implements BiConsumer<IToken, IScope> {
  private final ErrorListener errorListener;

  NormalTerminationOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final IToken token, final IScope scope) {

    if (!scope.isTerminatedNormally()) {
      errorListener.semanticError(token, "",
          ErrorListener.SemanticClassification.RETURN_UNREACHABLE);
    }

  }
}

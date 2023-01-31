package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.IScope;

/**
 * Just check is a scope has normal termination.
 * This means does the scope always end up with a guaranteed Exception.
 */
public class CheckNormalTermination implements BiConsumer<Token, IScope> {
  private final ErrorListener errorListener;

  public CheckNormalTermination(final ErrorListener errorListener) {
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

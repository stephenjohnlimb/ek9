package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;

/**
 * Checks if the pure modifier has been used correctly on methods and functions.
 */
public class CheckPureModifier extends RuleSupport implements Consumer<PureCheckData> {
  public CheckPureModifier(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final PureCheckData data) {

    if (data.superSymbol().isMarkedPure() && !data.thisSymbol().isMarkedPure()) {
      errorListener.semanticError(data.thisSymbol().getSourceToken(), data.errorMessage(),
          ErrorListener.SemanticClassification.SUPER_IS_PURE);
    } else if (!data.superSymbol().isMarkedPure() && data.thisSymbol().isMarkedPure()) {
      errorListener.semanticError(data.thisSymbol().getSourceToken(), data.errorMessage(),
          ErrorListener.SemanticClassification.SUPER_IS_NOT_PURE);
    }
  }
}

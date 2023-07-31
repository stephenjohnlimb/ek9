package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.symbols.support.AggregateFactory.EK9_VOID;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.common.ExternallyImplemented;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.support.LocationExtractor;

/**
 * Checks if the return value (if present) has been initialised.
 * This takes into account extern, abstract and the like,
 */
final class CheckReturn extends RuleSupport implements BiConsumer<ISymbol, ISymbol> {
  private final LocationExtractor locationExtractor = new LocationExtractor();

  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();
  private final boolean forDynamicFunction;

  CheckReturn(final boolean forDynamicFunction, final SymbolAndScopeManagement symbolAndScopeManagement,
              final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.forDynamicFunction = forDynamicFunction;
  }

  @Override
  public void accept(final ISymbol parentSymbol, final ISymbol returnSymbol) {
    //They can be null if there were ek9 code errors - duplications and the like
    if (isNotAbstractAndIsTyped(parentSymbol, returnSymbol)
        && !externallyImplemented.test(parentSymbol)) {
      checkIfInitialisedOrError(parentSymbol, returnSymbol);
    }
  }

  private boolean isNotAbstractAndIsTyped(final ISymbol parentSymbol, final ISymbol returnSymbol) {
    return parentSymbol != null && !parentSymbol.isMarkedAbstract()
        && returnSymbol != null && returnSymbol.getType().isPresent()
        && !EK9_VOID.equals(returnSymbol.getType().get().getFullyQualifiedName());
  }

  private void checkIfInitialisedOrError(final ISymbol parentSymbol, final ISymbol returnSymbol) {
    if (returnSymbol != null && !returnSymbol.isInitialised()) {

      if (forDynamicFunction) {
        var msg = "dynamic function inferred return '"
            + returnSymbol.getFriendlyName()
            + "' "
            + locationExtractor.apply(returnSymbol) + ":";
        errorListener.semanticError(parentSymbol.getSourceToken(), msg,
            ErrorListener.SemanticClassification.NEVER_INITIALISED);
      } else {
        var msg = "'" + returnSymbol.getFriendlyName() + "'";
        errorListener.semanticError(returnSymbol.getSourceToken(), msg,
            ErrorListener.SemanticClassification.NEVER_INITIALISED);
      }
    }
  }
}

package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.common.UninitialisedVariableToBeChecked;


final class ProcessIdentifierReference extends TypedSymbolAccess
    implements Consumer<EK9Parser.IdentifierReferenceContext> {

  private final UninitialisedVariableToBeChecked uninitialisedVariableToBeChecked =
      new UninitialisedVariableToBeChecked();

  ProcessIdentifierReference(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.IdentifierReferenceContext ctx) {

    final var symbol = symbolAndScopeManagement.getRecordedSymbol(ctx);

    if (uninitialisedVariableToBeChecked.test(symbol)) {
      var initialised = symbolAndScopeManagement.isVariableInitialised(symbol);
      if (!initialised) {
        errorListener.semanticError(ctx.start, "'" + symbol.getFriendlyName() + "':", USED_BEFORE_INITIALISED);
      }
    }

  }
}

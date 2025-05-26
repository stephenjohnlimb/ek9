package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.common.UninitialisedVariableToBeChecked;


/**
 * Emits an error if an identifier (typically a variable) has been used before being initialised.
 */
final class IdentifierReferenceOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.IdentifierReferenceContext> {

  private final UninitialisedVariableToBeChecked uninitialisedVariableToBeChecked =
      new UninitialisedVariableToBeChecked();

  IdentifierReferenceOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.IdentifierReferenceContext ctx) {

    final var symbol = symbolsAndScopes.getRecordedSymbol(ctx);

    if (uninitialisedVariableToBeChecked.test(symbol)) {
      var initialised = symbolsAndScopes.isVariableInitialised(symbol);
      if (!initialised) {
        errorListener.semanticError(ctx.start, "'" + symbol.getFriendlyName() + "':", USED_BEFORE_INITIALISED);
      }
    }

  }
}

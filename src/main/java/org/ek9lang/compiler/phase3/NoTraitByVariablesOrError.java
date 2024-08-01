package org.ek9lang.compiler.phase3;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;

/**
 * Used where traits are implemented 'by' a variable.
 * But this is design to emit errors because a trait with trait 'by' {variable} is not supported.
 */
final class NoTraitByVariablesOrError extends TypedSymbolAccess
    implements BiConsumer<EK9Parser.TraitsListContext, AggregateWithTraitsSymbol> {

  NoTraitByVariablesOrError(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.TraitsListContext traitsListContext, final AggregateWithTraitsSymbol aggregate) {

    if (traitsListContext == null || aggregate == null) {
      return;
    }

    //Only those traits that have been marked with 'by identifier' are candidates.
    traitsListContext.traitReference().stream()
        .filter(traitCtx -> traitCtx.identifier() != null)
        .forEach(traitCtx -> emitNotSupported(traitCtx, aggregate));

  }

  private void emitNotSupported(final EK9Parser.TraitReferenceContext traitCtx,
                                final AggregateWithTraitsSymbol aggregate) {

    errorListener.semanticError(aggregate.getSourceToken(),
        "wrt to trait '" + traitCtx.identifierReference().getText() + "', '" + traitCtx.identifier().getText() + "':",
        ErrorListener.SemanticClassification.TRAIT_BY_IDENTIFIER_NOT_SUPPORTED);

  }
}

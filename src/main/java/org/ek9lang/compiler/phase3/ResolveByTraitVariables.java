package org.ek9lang.compiler.phase3;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used where traits are implemented 'by' a variable.
 * This resolves those identifiers on the aggregate and checks the type compatibility.
 */
final class ResolveByTraitVariables extends TypedSymbolAccess
    implements BiConsumer<EK9Parser.TraitsListContext, AggregateWithTraitsSymbol> {

  ResolveByTraitVariables(final SymbolAndScopeManagement symbolAndScopeManagement, final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.TraitsListContext traitsListContext, final AggregateWithTraitsSymbol aggregate) {
    if (traitsListContext == null || aggregate == null) {
      return;
    }
    //Only those traits that have been marked with 'by identifier' are candidates.
    traitsListContext.traitReference().stream()
        .filter(traitCtx -> traitCtx.identifier() != null)
        .forEach(traitCtx -> resolveAndCheckIdentifier(traitCtx, aggregate));
  }

  private void resolveAndCheckIdentifier(final EK9Parser.TraitReferenceContext traitCtx,
                                         final AggregateWithTraitsSymbol aggregate) {
    var symbol = getRecordedAndTypedSymbol(traitCtx.identifierReference());
    if (symbol instanceof AggregateWithTraitsSymbol trait) {
      //Now need to resolve the identifier, which must be available in the aggregate.
      var variableToResolve = traitCtx.identifier().getText();
      var resolved = aggregate.resolve(new SymbolSearch(variableToResolve));

      resolved.ifPresentOrElse(variable -> {
        variableIsValidTypeOrError(variable, trait, aggregate);
        //Now make a note of what this resolved to - may be useful in later phases.
        recordATypedSymbol(variable, traitCtx.identifier());
      }, () -> emitNotResolved(variableToResolve, trait, aggregate));
    }
  }

  private void variableIsValidTypeOrError(final ISymbol variable, final AggregateWithTraitsSymbol trait,
                                          final AggregateWithTraitsSymbol aggregate) {
    variable.getType().ifPresent(varType -> {
      if (!varType.isAssignableTo(trait)) {
        errorListener.semanticError(aggregate.getSourceToken(),
            "wrt to trait '" + trait.getFriendlyName() + "', '" + variable.getFriendlyName() + "':",
            ErrorListener.SemanticClassification.INCOMPATIBLE_TYPES);
      }
    });
  }

  private void emitNotResolved(final String variableToResolve, final AggregateWithTraitsSymbol trait,
                               final AggregateWithTraitsSymbol aggregate) {

    errorListener.semanticError(aggregate.getSourceToken(),
        "wrt to trait '" + trait.getFriendlyName() + "', '" + variableToResolve + "':",
        ErrorListener.SemanticClassification.NOT_RESOLVED);
  }
}

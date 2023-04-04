package org.ek9lang.compiler.main.rules;

import java.util.Optional;
import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;

/**
 * Checks for a type is resolved and is suitable to be extended from (in basic terms).
 * This does not include the 'allows only' graph that has to be done one the whole type hierarchy has been]
 * established.
 */
public class CheckSuitableToExtend implements Function<ParserRuleContext, Optional<ISymbol>> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ISymbol.SymbolGenus genus;
  private final ErrorListener errorListener;

  private final boolean issueErrorIfNotResolved;

  /**
   * Checks that the typedef passed in (when resolved) is suitable to be extended from.
   */
  public CheckSuitableToExtend(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener,
                               final ISymbol.SymbolGenus genus,
                               final boolean issueErrorIfNotResolved) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
    this.genus = genus;
    this.issueErrorIfNotResolved = issueErrorIfNotResolved;
  }

  @Override
  public Optional<ISymbol> apply(final ParserRuleContext ctx) {
    var theTypeDef = symbolAndScopeManagement.getRecordedSymbol(ctx);
    return checkIfValidSuper(ctx, theTypeDef) ? Optional.of(theTypeDef) : Optional.empty();
  }

  private boolean checkIfValidSuper(final ParserRuleContext ctx, final ISymbol proposedSuper) {
    if (proposedSuper == null) {
      if (issueErrorIfNotResolved) {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
      }
      return false;
    }

    if (proposedSuper.getGenus() != genus) {
      var msg = "resolved as '" + proposedSuper.getGenus() + "' rather than '" + genus + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INCOMPATIBLE_GENUS);
      return false;
    }

    if (proposedSuper instanceof PossibleGenericSymbol aggregateOfFunction
        && !aggregateOfFunction.isOpenForExtension()) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_OPEN_TO_EXTENSION);
      return false;
    }
    return true;
  }
}
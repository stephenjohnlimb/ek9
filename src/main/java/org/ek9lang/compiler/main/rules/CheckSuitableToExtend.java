package org.ek9lang.compiler.main.rules;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;

/**
 * Checks for a type is resolved and is suitable to be extended from (in basic terms).
 * This does not include the 'allows only' graph that has to be done one the whole type hierarchy has been]
 * established.
 */
public class CheckSuitableToExtend implements Function<EK9Parser.TypeDefContext, Optional<ISymbol>> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ISymbol.SymbolGenus genus;
  private final ErrorListener errorListener;

  /**
   * Checks that the typedef passed in (when resolved) is suitable to be extended from.
   */
  public CheckSuitableToExtend(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener, final ISymbol.SymbolGenus genus) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
    this.genus = genus;
  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.TypeDefContext ctx) {
    var theTypeDef = symbolAndScopeManagement.getRecordedSymbol(ctx);
    return checkIfValidSuper(ctx, theTypeDef) ? Optional.of(theTypeDef) : Optional.empty();
  }

  private boolean checkIfValidSuper(final EK9Parser.TypeDefContext ctx, final ISymbol proposedSuper) {
    if (proposedSuper == null) {
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
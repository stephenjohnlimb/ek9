package org.ek9lang.compiler.phase2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks for a type is resolved and is suitable genus and category.
 */
final class SuitableGenusOrError extends RuleSupport implements Function<ParserRuleContext, Optional<ISymbol>> {

  private final List<ISymbol.SymbolGenus> supportedGenus = new ArrayList<>();

  private final boolean issueErrorIfNotResolved;

  private final boolean allowTemplates;

  /**
   * Checks that the typedef/identifierReference passed in (when resolved) is suitable genus.
   */
  SuitableGenusOrError(final SymbolsAndScopes symbolsAndScopes,
                       final ErrorListener errorListener,
                       final ISymbol.SymbolGenus genus,
                       final boolean allowTemplates,
                       final boolean issueErrorIfNotResolved) {

    this(symbolsAndScopes, errorListener, List.of(genus), allowTemplates, issueErrorIfNotResolved);

  }

  /**
   * Checks that the typedef/identifierReference passed in (when resolved) is suitable genus.
   * Accepts multiple allowed genus. i.e. FUNCTION and FUNCTION_TRAIT.
   */
  public SuitableGenusOrError(final SymbolsAndScopes symbolsAndScopes,
                              final ErrorListener errorListener,
                              final List<ISymbol.SymbolGenus> genus,
                              final boolean allowTemplates,
                              final boolean issueErrorIfNotResolved) {

    super(symbolsAndScopes, errorListener);
    this.supportedGenus.addAll(genus);
    this.issueErrorIfNotResolved = issueErrorIfNotResolved;
    this.allowTemplates = allowTemplates;

  }

  @Override
  public Optional<ISymbol> apply(final ParserRuleContext ctx) {

    var symbol = symbolsAndScopes.getRecordedSymbol(ctx);
    return validSymbolOrError(ctx, symbol) ? Optional.of(symbol) : Optional.empty();

  }

  private boolean validSymbolOrError(final ParserRuleContext ctx, final ISymbol symbol) {

    if (!resolvedOrError(ctx, symbol)) {
      return false;
    }

    if (!validGenusOrError(ctx, symbol)) {
      return false;
    }

    return validCategoryOrError(ctx, symbol);
  }

  private boolean validCategoryOrError(final ParserRuleContext ctx, final ISymbol symbol) {

    if (!allowTemplates && (symbol.isTemplateType() || symbol.isTemplateFunction())) {
      final var msg = "is a '"
          + symbol.getCategory().getDescription()
          + "', which is not supported";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INCOMPATIBLE_CATEGORY);
      return false;
    }

    return true;
  }

  private boolean resolvedOrError(final ParserRuleContext ctx, final ISymbol symbol) {

    if (symbol == null) {
      if (issueErrorIfNotResolved) {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
      }
      return false;
    }

    return true;
  }

  private boolean validGenusOrError(final ParserRuleContext ctx, final ISymbol symbol) {

    if (!supportedGenus.contains(symbol.getGenus())) {
      final var msg = "resolved as a '"
          + symbol.getGenus().getDescription()
          + "' which is a '"
          + symbol.getCategory().getDescription()
          + "' rather than a '"
          + supportedGenus.stream().map(ISymbol.SymbolGenus::getDescription).collect(Collectors.joining(", "))
          + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.INCOMPATIBLE_GENUS);
      return false;
    }

    return true;
  }
}
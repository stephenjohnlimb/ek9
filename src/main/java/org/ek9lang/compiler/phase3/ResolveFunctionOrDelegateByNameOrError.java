package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used for resolving a function (by just name) or a variable that is a function delegate.
 */
final class ResolveFunctionOrDelegateByNameOrError extends RuleSupport
    implements Consumer<EK9Parser.IdentifierContext> {

  /**
   * Searches for an identifier and issues an error if it is not resolved.
   * So identifiers are a little ambiguous unlike identifierReferences (which must be present).
   * identifiers can be optional in some contexts (like named argument when calling methods and functions).
   * Also in dynamic classes and functions with variable capture.
   */
  ResolveFunctionOrDelegateByNameOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                         final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.IdentifierContext ctx) {
    var toResolve = ctx.getText();

    var resolved = testForFunctionDelegate(toResolve);
    if (resolved.isEmpty()) {
      resolved = testForFunctionByName(toResolve);
      if (resolved.isEmpty()) {
        var msg = "'" + toResolve + "':";
        errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.NOT_RESOLVED);
        return;
      }
    }

    var symbol = resolved.get();
    symbol.setReferenced(true);
    symbolAndScopeManagement.recordSymbol(symbol, ctx);
  }

  private Optional<ISymbol> testForFunctionDelegate(final String name) {
    return symbolAndScopeManagement.getTopScope().resolve(new SymbolSearch(name));
  }

  private Optional<ISymbol> testForFunctionByName(final String name) {
    var possibleMatches = symbolAndScopeManagement.getModuleScope().getAllSymbolsMatchingName(name);
    if (!possibleMatches.isEmpty()) {
      return Optional.of(possibleMatches.get(0));
    }
    return Optional.empty();
  }
}
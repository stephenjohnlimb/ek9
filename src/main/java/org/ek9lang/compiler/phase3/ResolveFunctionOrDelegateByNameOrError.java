package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_REQUIRED;

import java.util.Optional;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used for resolving a function (by just name) or a variable that is a function delegate.
 */
final class ResolveFunctionOrDelegateByNameOrError extends TypedSymbolAccess
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

    final var toResolve = ctx.getText();

    var resolved = getFunction(ctx, toResolve);
    if (resolved.isEmpty()) {
      var msg = "'" + toResolve + "':";
      errorListener.semanticError(ctx.start, msg, ErrorListener.SemanticClassification.NOT_RESOLVED);
      return;
    }

    final var symbol = resolved.get();
    symbol.setReferenced(true);
    recordATypedSymbol(symbol, ctx);
  }

  private Optional<ISymbol> getFunction(final EK9Parser.IdentifierContext ctx, final String functionNameToResolve) {

    final var resolved = resolveFunctionDelegate(functionNameToResolve);
    if (resolved.isPresent()) {
      return resolved;
    }

    return resolveFunctionByName(ctx, functionNameToResolve);
  }


  private Optional<ISymbol> resolveFunctionDelegate(final String name) {

    return symbolAndScopeManagement.getTopScope().resolve(new SymbolSearch(name));
  }

  private Optional<ISymbol> resolveFunctionByName(final EK9Parser.IdentifierContext ctx, final String name) {

    //While it may seem strange - we resolve like this so that it is possible to give more meaningful error messages.
    //EK9 stops the same name being used at module scope for multiple 'types'/'constant' so there should only be one
    //But it might be the wrong type - that is checked later in its context.
    final var possibleMatches = symbolAndScopeManagement.getModuleScope().getAllSymbolsMatchingName(name);
    if (!possibleMatches.isEmpty()) {
      final var resolved = possibleMatches.get(0);
      if (!resolved.isFunction() && !resolved.isTemplateFunction()) {
        final var msg = "'" + name + "':";
        errorListener.semanticError(ctx.start, msg, FUNCTION_OR_DELEGATE_REQUIRED);
      }
      return Optional.of(resolved);
    }

    return Optional.empty();
  }

}
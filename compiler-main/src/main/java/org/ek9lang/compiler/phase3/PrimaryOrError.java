package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Ensures that 'primary' is now resolved and 'typed' or a not resolved error.
 * There was a placeholder in for primary, that can now be replaced with the real resolved symbol.
 */
final class PrimaryOrError extends TypedSymbolAccess implements Consumer<EK9Parser.PrimaryContext> {

  private final SymbolFromContextOrError symbolFromContextOrError;

  /**
   * Check Primary resolves and attempt to 'type' it.
   */
  PrimaryOrError(final SymbolsAndScopes symbolsAndScopes,
                 final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError = new SymbolFromContextOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.PrimaryContext ctx) {

    final var symbol = determineSymbolToRecordOrError(ctx);
    if (symbol != null) {
      recordATypedSymbol(symbol, ctx);
    }

  }

  /**
   * Gets the appropriate symbol to register against this context.
   */
  private ISymbol determineSymbolToRecordOrError(final EK9Parser.PrimaryContext ctx) {

    if (ctx.literal() != null) {
      return getSymbolFromContextOrError(ctx.literal());
    } else if (ctx.identifierReference() != null) {
      return getSymbolFromContextOrError(ctx.identifierReference());
    } else if (ctx.primaryReference() != null) {
      return getSymbolFromContextOrError(ctx.primaryReference());
    } else if (ctx.expression() != null) {
      return getSymbolFromContextOrError(ctx.expression());
    } else {
      AssertValue.fail("Expecting finite set of operations");
    }

    return null;
  }

  private ISymbol getSymbolFromContextOrError(final ParserRuleContext ctx) {

    return symbolFromContextOrError.apply(ctx);

  }
}
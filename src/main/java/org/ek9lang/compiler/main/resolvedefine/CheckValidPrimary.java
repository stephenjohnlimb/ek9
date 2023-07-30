package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.exception.AssertValue;

/**
 * Ensures that 'primary' is now resolved and 'typed' or a not resolved error.
 * There was a placeholder in for primary, that can now be replaced with the real resolved symbol.
 */
public class CheckValidPrimary extends RuleSupport implements Consumer<EK9Parser.PrimaryContext> {


  /**
   * Check Primary resolves and attempt to 'type' it.
   */
  public CheckValidPrimary(final SymbolAndScopeManagement symbolAndScopeManagement,
                           final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.PrimaryContext ctx) {
    var symbol = determineSymbolToRecord(ctx);
    if (symbol != null) {
      symbolAndScopeManagement.recordSymbol(symbol, ctx);
    }
  }

  /**
   * Gets the appropriate symbol to register against this context.
   */
  private ISymbol determineSymbolToRecord(final EK9Parser.PrimaryContext ctx) {
    ISymbol symbol = null;
    if (ctx.literal() != null) {
      symbol = getSymbolFromContext(ctx.literal());
    } else if (ctx.identifierReference() != null) {
      symbol = getSymbolFromContext(ctx.identifierReference());
    } else if (ctx.primaryReference() != null) {
      symbol = getSymbolFromContext(ctx.primaryReference());
    } else if (ctx.expression() != null) {
      symbol = getSymbolFromContext(ctx.expression());
    } else {
      AssertValue.fail("Expecting finite set of operations");
    }
    return symbol;
  }

  private ISymbol getSymbolFromContext(final ParserRuleContext ctx) {
    var resolved = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (resolved == null) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
    }
    return resolved;
  }
}
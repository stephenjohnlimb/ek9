package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Ensures that 'expression' is now resolved and 'typed' or a not resolved error.
 * This is a beast and will have to delegate parts, as there are just some many types of expression.
 * TODO lot's of splitting of this logic up.
 */
public class CheckValidExpression implements Consumer<EK9Parser.ExpressionContext> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final SymbolFactory symbolFactory;
  private final ErrorListener errorListener;

  /**
   * Check Primary resolves and attempt to 'type' it.
   */
  public CheckValidExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                              final SymbolFactory symbolFactory,
                              final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.symbolFactory = symbolFactory;
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ExpressionContext ctx) {
    var symbol = determineSymbolToRecord(ctx);
    if (symbol != null) {
      symbolAndScopeManagement.recordSymbol(symbol, ctx);
      if (symbol.getType().isEmpty()) {
        emitTypeNotResolvedError(ctx.start, symbol);
      }
    }
  }

  /**
   * TODO this will need to be pulled out to separate methods because it will be too much to grok in one go.
   */
  private ISymbol determineSymbolToRecord(final EK9Parser.ExpressionContext ctx) {
    if (ctx.QUESTION() != null) {
      var expressionInQuestion = symbolAndScopeManagement.getRecordedSymbol(ctx.expression(0));
      if (expressionInQuestion != null) {
        expressionInQuestion.setReferenced(true);
        symbolFactory.newExpressionSymbol(expressionInQuestion).setType(symbolAndScopeManagement.getTopScope().resolve(
            new TypeSymbolSearch("Boolean")));
      }
    } else if (ctx.primary() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.primary());
    } else if (ctx.call() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.call());
    } else if (ctx.list() != null) {
      return symbolAndScopeManagement.getRecordedSymbol(ctx.list());
    } else if (ctx.expression() != null && !ctx.expression().isEmpty()) {
      System.out.println("Expression to expression(s) next - but need to work out the type");
      return symbolFactory.newExpressionSymbol(ctx.start, ctx.getText(), symbolAndScopeManagement.getTopScope().resolve(
          new TypeSymbolSearch("Boolean")));
    } else if (ctx.objectAccessExpression() != null) {
      System.out.println("Object access expression - but need to work out the type");
      return symbolFactory.newExpressionSymbol(ctx.start, ctx.getText());
    } else {
      AssertValue.fail(
          "Expecting finite set of operations for expression [" + ctx.getText() + "] line: " + ctx.start.getLine());
    }
    return null;
  }

  private void setFromExpressions(final EK9Parser.ExpressionContext ctx, ISymbol thisExpressionSymbol) {
    if (ctx.expression().size() > 1) {
      //TODO lots of working out what the type will be depending on the expression.
    } else {
      setTypeFromSymbol(ctx.expression(0), thisExpressionSymbol);
    }
  }

  private void setTypeFromSymbol(final ParserRuleContext ctx, ISymbol thisExpressionSymbol) {
    var resolved = symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (resolved != null) {
      thisExpressionSymbol.setType(resolved.getType());
    }
  }

  private void emitTypeNotResolvedError(final Token lineToken, final ISymbol argument) {
    var msg = "'" + argument.getName() + "':";
    errorListener.semanticError(lineToken, msg,
        ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
  }
}
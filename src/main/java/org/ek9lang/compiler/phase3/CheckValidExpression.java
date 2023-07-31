package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.support.SymbolFactory;
import org.ek9lang.core.AssertValue;

/**
 * Ensures that 'expression' is now resolved and 'typed' or a not resolved error.
 * This is a beast and will have to delegate parts, as there are just some many types of expression.
 * TODO lot's of splitting of this logic up, because this is the 'beast'.
 */
final class CheckValidExpression extends RuleSupport implements Consumer<EK9Parser.ExpressionContext> {

  private final SymbolFactory symbolFactory;


  /**
   * Check Primary resolves and attempt to 'type' it.
   */
  CheckValidExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final SymbolFactory symbolFactory,
                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFactory = symbolFactory;
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
      //TODO implement this.
      System.out.println("Expression to expression(s) next - but need to work out the type");
      return symbolFactory.newExpressionSymbol(ctx.start, ctx.getText(), symbolAndScopeManagement.getTopScope().resolve(
          new TypeSymbolSearch("Boolean")));
    } else if (ctx.objectAccessExpression() != null) {
      var maybeResolved = symbolAndScopeManagement.getRecordedSymbol(ctx.objectAccessExpression());
      if (maybeResolved != null && maybeResolved.getType().isPresent()) {
        return symbolFactory.newExpressionSymbol(ctx.start, ctx.getText()).setType(maybeResolved.getType());
      }
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
    var msg = "'" + argument.getName() + "' :";
    errorListener.semanticError(lineToken, msg,
        ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
  }
}